from typing import Dict, Tuple

import numpy as np
from ..config import AdvAttackConfig
from keras.models import Model
import tensorflow as tf
from loguru import logger

# tf.compat.v1.disable_eager_execution()
# import numpy as np

# import torch


class PGDAttack:
    def __init__(self, config: AdvAttackConfig) -> None:
        self.config = config

    def run_fgsm_attack_keras(self, model, x, y):

        loss = None
        with tf.GradientTape() as tape:
            tape.watch(x)
            loss = self.config.loss_fn(model(x), y)

        # logger.info(loss)
        grads = tape.gradient(loss, x)
        logger.info(grads)
        perturbation = tf.keras.backend.sign(grads)
        if np.sum(tf.math.is_nan(perturbation).numpy()) > 0:
            logger.warning(
                "NaN Perturbation! [Count: {}]".format(
                    np.sum(tf.math.is_nan(perturbation).numpy())
                )
            )
            return x + tf.keras.backend.random_uniform(
                shape=x.shape, minval=-self.config.eps, maxval=self.config.eps
            )
        return tf.keras.backend.clip(
            x + perturbation, self.config.clip_range[0], self.config.clip_range[1]
        )

    def run_fgsm_attack_tf(self, model, x, y, other_inputs=None):
        graph, graph_def, input_tensor, output_tensor = model
        sess = tf.compat.v1.Session(graph=graph)
        tf.compat.v1.import_graph_def(graph_def)
        # out = sess.run(output_tensor, feed_dict={input_tensor: x})
        loss = self.config.loss_fn(y, output_tensor)
        grad = tf.compat.v1.gradients(loss, input_tensor)
        feed_dict = {input_tensor: x}
        if other_inputs:
            for k in other_inputs:
                feed_dict[k] = other_inputs[k]
        tmp_grad = sess.run(grad, feed_dict=feed_dict)
        perturbation = np.sign(tmp_grad[0])
        return np.clip(
            x + perturbation, self.config.clip_range[0], self.config.clip_range[1]
        )

    def run(self, model, x, target=None, other_inputs=None):

        if isinstance(model, Model):
            if self.config.loss_fn is None:
                self.config.loss_fn = tf.keras.losses.MeanSquaredError()

            eta = tf.keras.backend.random_uniform(
                shape=x.shape, minval=-self.config.eps, maxval=self.config.eps
            )
            adv_x = tf.keras.backend.clip(
                x + eta, self.config.clip_range[0], self.config.clip_range[1]
            )
            if target is None:
                target = model.predict(x)
            for _ in range(self.config.nb_iter):
                adv_x = self.run_fgsm_attack_keras(model, adv_x, target)
                eta = tf.keras.backend.clip(
                    adv_x - x, min_value=-self.config.eps, max_value=self.config.eps
                )
                adv_x = tf.keras.backend.clip(
                    x + eta, self.config.clip_range[0], self.config.clip_range[1]
                )
        elif isinstance(model, Tuple) and len(model) == 4:
            if other_inputs and (not isinstance(other_inputs, Dict)):
                logger.error("Other inputs should be a dict!")
                return None
            # Tensorflow Model
            graph, graph_def, input_tensor, output_tensor = model
            sess = tf.compat.v1.Session(graph=graph)

            if self.config.loss_fn is None:
                self.config.loss_fn = tf.compat.v1.losses.mean_squared_error

            eta = np.random.uniform(
                size=x.shape, low=-self.config.eps, high=self.config.eps
            )
            adv_x = np.clip(
                x + eta,
                a_min=self.config.clip_range[0],
                a_max=self.config.clip_range[1],
            )
            if target is None:
                feed_dict = {input_tensor: x}
                if other_inputs:
                    for k in other_inputs:
                        feed_dict[k] = other_inputs[k]
                target = sess.run(output_tensor, feed_dict=feed_dict)

            for _ in range(self.config.nb_iter):
                adv_x = self.run_fgsm_attack_tf(
                    model, adv_x, target, other_inputs=other_inputs
                )
                eta = np.clip(adv_x - x, a_min=-self.config.eps, a_max=self.config.eps)
                adv_x = np.clip(
                    x + eta,
                    a_min=self.config.clip_range[0],
                    a_max=self.config.clip_range[1],
                )

        else:
            logger.error("Model is unknown!")
            exit(0)
        return adv_x


def pgd_attack(config: AdvAttackConfig):
    return PGDAttack(config)
