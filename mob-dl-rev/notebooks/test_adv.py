import pickle as pk
import warnings
import tensorflow as tf
import numpy as np
from easydict import EasyDict as edict

# from matplotlib import pyplot as plt

from art.estimators.classification import KerasClassifier
from art.attacks.evasion import (
    ProjectedGradientDescent,
)

import yaml
import click

# from art.attacks.evasion.projected_gradient_descent.projected_gradient_descent_tensorflow_v2 import P
# from keras.applications.xception import preprocess_input
import os

from keras.applications.imagenet_utils import decode_predictions

from PIL import Image

tf.compat.v1.disable_eager_execution()
warnings.filterwarnings("ignore")


@click.command()
@click.option("-c", "--config_file", type=str, help="config file")
@click.option("-g", "--gpu", type=str, help="GPU config")
@click.option(
    "-vp",
    "--val_path",
    type=str,
    default="/data_2/Anonymous/data/imagenet/labelled",
    help="GPU config",
)
def run(config_file, gpu, val_path):
    config = yaml.safe_load(open(config_file, "r"))
    config = edict(config)

    os.environ["CUDA_VISIBLE_DEVICES"] = gpu

    file_list = ["{}/{}".format(val_path, x) for x in os.listdir(val_path)]

    model = None
    preprocess_input = None

    if config.model_name == "DenseNet121":
        model = tf.keras.applications.densenet.DenseNet121(weights="imagenet")
        preprocess_input = tf.keras.applications.densenet.preprocess_input
    elif config.model_name == "Xception":
        model = tf.keras.applications.xception.Xception(weights="imagenet")
        preprocess_input = tf.keras.applications.xception.preprocess_input
    elif config.model_name == "ResNet50":
        model = tf.keras.applications.resnet.ResNet50(weights="imagenet")
        preprocess_input = tf.keras.applications.resnet.preprocess_input
    elif config.model_name == "ResNet50V2":
        model = tf.keras.applications.resnet_v2.ResNet50V2(weights="imagenet")
        preprocess_input = tf.keras.applications.resnet_v2.preprocess_input
    elif config.model_name == "MobileNet":
        model = tf.keras.applications.mobilenet.MobileNet(weights="imagenet")
        preprocess_input = tf.keras.applications.mobilenet.preprocess_input

    classifier = KerasClassifier(
        model=model, clip_values=(config.clip_min, config.clip_max)
    )
    attack = ProjectedGradientDescent(
        estimator=classifier, eps=config.attack_eps, max_iter=10
    )

    gt_arr = []
    out_arr = []
    adv_out_arr = []

    for file_path in file_list[: config.test_size]:

        img = Image.open(file_path)
        img = img.convert("RGB")
        img = img.resize(config.image_shape)
        img = np.array(img).astype(np.float32)
        x = np.expand_dims(img, axis=0)

        x = preprocess_input(x)

        out = model.predict(x)
        # print(decode_predictions(out))
        out_arr.append(decode_predictions(out))

        gt = file_path.split(".")[-2]
        gt_arr.append(gt)

        x_test_adv = attack.generate(x)
        adv_out = model.predict(x_test_adv)
        # print(decode_predictions(adv_out))
        adv_out_arr.append(decode_predictions(adv_out))
        # break
    with open("{}.adv_result".format(config.model_name), "wb") as f:
        pk.dump(
            {
                "model_name": config.model_name,
                "gt": gt_arr,
                "out": out_arr,
                "adv_out": adv_out_arr,
                "config": {
                    "test_size": config.test_size,
                    "clip_min": config.clip_min,
                    "clip_max": config.clip_max,
                    "attack_eps": config.attack_eps,
                    "iter_num": 10,
                    "attack_type": "PGD",
                },
            },
            f,
        )


if __name__ == "__main__":
    run()
