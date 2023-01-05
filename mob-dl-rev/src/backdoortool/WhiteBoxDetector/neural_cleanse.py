from decimal import Decimal
from keras import backend as K
import numpy as np

from keras.models import Model
from loguru import logger

from keras.losses import categorical_crossentropy
from keras.metrics import categorical_accuracy

# from keras.optimizers import adam_v2
from keras.optimizer_v1 import Adam
from keras.utils.all_utils import to_categorical


class NeuralCleanse:
    def __init__(
        self,
        nb_class,
        img_shape=(224, 224, 3),
        lr=0.01,
        opt_steps=1000,
        init_cost=1e-3,
        regularization="l1",
        attack_thres=0.99,
        early_stop=True,
        early_stop_thres=1.0,
        batch_size=4,
        early_stop_patience=10,
        counter_patience=10,
    ) -> None:
        self.nb_class = nb_class
        self.img_shape = img_shape
        self.lr = lr
        self.opt_steps = opt_steps
        self.init_cost = init_cost
        self.regularization = regularization
        self.attack_thres = attack_thres
        self.early_stop = early_stop
        self.early_stop_thres = early_stop_thres
        self.early_stop_patience = early_stop_patience
        self.batch_size = batch_size
        self.counter_patience = counter_patience

    def run_keras(self, model, imgs, y_target=None):
        pattern_raw_tensor = K.variable(np.zeros(self.img_shape))
        pattern_raw_tensor = K.expand_dims(pattern_raw_tensor, axis=0)
        # pattern_raw_tensor = pattern_raw_tensor + 0.5*255

        mask_tensor = K.zeros([self.img_shape[0], self.img_shape[1], 1])
        mask_tensor = mask_tensor + 0.5
        mask_tensor = K.repeat_elements(mask_tensor, rep=3, axis=2)
        mask_tensor = K.expand_dims(mask_tensor, axis=0)

        reverse_mask_tensor = K.ones_like(mask_tensor) - mask_tensor

        input_raw_tensor = K.placeholder(self.img_shape)
        # Reshape to [1, H, W, C]
        input_raw_tensor = K.expand_dims(input_raw_tensor, axis=0)

        X_adv_raw_tensor = (
            reverse_mask_tensor * input_raw_tensor + mask_tensor * pattern_raw_tensor
        )

        output_tensor = model(X_adv_raw_tensor)
        y_true_tensor = K.placeholder(model.output_shape)
        loss_acc = categorical_accuracy(output_tensor, y_true_tensor)
        loss_ce = categorical_crossentropy(output_tensor, y_true_tensor)

        if self.regularization is None:
            loss_reg = K.constant(0)
        elif self.regularization == "l1":
            loss_reg = K.sum(K.abs(mask_tensor)) / 3
        elif self.regularization == "l2":
            loss_reg = K.sqrt(K.sum(K.square(mask_tensor)) / 3)

        cost = self.init_cost
        cost_tensor = K.variable(cost)
        loss = loss_ce + loss_reg * cost_tensor
        opt = Adam(lr=self.lr, beta_1=0.5, beta_2=0.9)
        updates = opt.get_updates(params=[pattern_raw_tensor, mask_tensor], loss=loss)
        train = K.function(
            [input_raw_tensor, y_true_tensor],
            [loss_ce, loss_reg, loss, loss_acc],
            updates=updates,
        )

        if len(imgs) % self.batch_size != 0:
            logger.warning("Batch Size can not match the Input!")
            logger.warning("Batch Size: {}".format(self.batch_size))
            logger.warning("Input Size: {}".format(len(imgs)))

        # best optimization results
        mask_best = None
        pattern_best = None
        reg_best = float("inf")

        # logs and counters for adjusting balance cost
        logs = []
        cost_set_counter = 0
        cost_up_counter = 0
        cost_down_counter = 0
        cost_up_flag = False
        cost_down_flag = False

        # counter for early stop
        early_stop_counter = 0
        early_stop_reg_best = reg_best

        for i in range(self.opt_steps):
            # record loss for all mini-batches
            loss_ce_list = []
            loss_reg_list = []
            loss_list = []
            loss_acc_list = []

            for j in range(len(imgs) // self.batch_size):
                Y = to_categorical([y_target] * self.batch_size, self.nb_class)
                X = K.variable(imgs[j * self.batch_size : (j + 1) * self.batch_size])

                (loss_ce_value, loss_reg_value, loss_value, loss_acc_value) = train(
                    [X, Y]
                )
                loss_ce_list.extend(list(loss_ce_value.flatten()))
                loss_reg_list.extend(list(loss_reg_value.flatten()))
                loss_list.extend(list(loss_value.flatten()))
                loss_acc_list.extend(list(loss_acc_value.flatten()))

            avg_loss_ce = np.mean(loss_ce_list)
            avg_loss_reg = np.mean(loss_reg_list)
            avg_loss = np.mean(loss_list)
            avg_loss_acc = np.mean(loss_acc_list)

            # save log
            logs.append(
                (
                    i,
                    avg_loss_ce,
                    avg_loss_reg,
                    avg_loss,
                    avg_loss_acc,
                    reg_best,
                    cost,
                )
            )
            # check to save best mask or not
            if avg_loss_acc >= self.attack_thres and avg_loss_reg < reg_best:
                mask_best = K.eval(mask_tensor)
                mask_best = mask_best[0, ..., 0]
                pattern_best = K.eval(pattern_raw_tensor)
                reg_best = avg_loss_reg

            if self.early_stop:
                # only terminate if a valid attack has been found
                if reg_best < float("inf"):
                    if reg_best >= self.early_stop_thres * early_stop_reg_best:
                        early_stop_counter += 1
                    else:
                        early_stop_counter = 0
                early_stop_reg_best = min(reg_best, early_stop_reg_best)

                if (
                    cost_down_flag
                    and cost_up_flag
                    and early_stop_counter >= self.early_stop_patience
                ):
                    logger.info("early stop")
                    break

            # check cost modification
            if cost == 0 and avg_loss_acc >= self.attack_thres:
                cost_set_counter += 1
                if cost_set_counter >= self.patience:
                    cost = self.init_cost
                    K.set_value(cost_tensor, cost)
                    cost_up_counter = 0
                    cost_down_counter = 0
                    cost_up_flag = False
                    cost_down_flag = False
                    print("initialize cost to %.2E" % Decimal(self.cost))
            else:
                cost_set_counter = 0

            if avg_loss_acc >= self.attack_thres:
                cost_up_counter += 1
                cost_down_counter = 0
            else:
                cost_up_counter = 0
                cost_down_counter += 1

            if cost_up_counter >= self.patience:
                cost_up_counter = 0
                if self.verbose == 2:
                    print(
                        "up cost from %.2E to %.2E"
                        % (
                            Decimal(self.cost),
                            Decimal(self.cost * self.cost_multiplier_up),
                        )
                    )
                self.cost *= self.cost_multiplier_up
                K.set_value(self.cost_tensor, self.cost)
                cost_up_flag = True
            elif cost_down_counter >= self.patience:
                cost_down_counter = 0
                if self.verbose == 2:
                    print(
                        "down cost from %.2E to %.2E"
                        % (
                            Decimal(cost),
                            Decimal(cost / self.cost_multiplier_down),
                        )
                    )
                cost /= self.cost_multiplier_down
                K.set_value(cost_tensor, cost)
                cost_down_flag = True

    def run(self, model, img, y_target=None):
        if isinstance(model, Model):
            return self.run_keras(model, img, y_target=y_target)
        else:
            logger.error("No Implementation!")
