# -*- coding: utf-8 -*-

import os
import pickle
import shutil
from loguru import logger
from pathlib import Path
from dotenv import find_dotenv, load_dotenv
import imp

from src.advtool.attack import AdvTool
from src.advtool.WhiteBoxAttacks.config import AdvAttackConfig
import keras.backend as K
import numpy as np
from PIL import Image

EVAL_CLS_MODELS = []
TEST_NUM = 2


def getRandomImage():
    imgDir = "/data_2/Anonymous/data/imagenet/labelled"
    length = len(os.listdir(imgDir))
    idx = np.random.randint(low=0, high=length)
    img = Image.open("{}/{}".format(imgDir, os.listdir(imgDir)[idx]))
    # img.show()
    return img


def load_test_dataset():
    test_dataset = []
    for _ in range(TEST_NUM):
        img = getRandomImage()
        img = np.array(img)
        img = K.variable(img)
        img = K.expand_dims(img, axis=0)
        test_dataset.append(img)
    return test_dataset


# dynamic import
def dynamic_imp(name, class_name):
    # find_module() method is used
    # to find the module and return
    # its description and path
    try:
        fp, path, desc = imp.find_module(name)

    except ImportError:
        print("module not found: " + name)

    try:
        # load_modules loads the module
        # dynamically ans takes the filepath
        # module and description as parameter
        example_package = imp.load_module(name, fp, path, desc)

    except Exception as e:
        print(e)

    try:
        myclass = imp.load_module("% s.% s" % (name, class_name), fp, path, desc)
    except Exception as e:
        print(e)

    return example_package, myclass


def load_model(model_path):

    class_name = "model"
    mod, modCl = dynamic_imp(model_path, class_name=class_name)
    return modCl.model


def main():
    """Runs data processing scripts to turn raw data from (../raw) into
    cleaned data ready to be analyzed (saved in ../processed).
    """

    logger.info("Start Evaluating Classification Models [AE] ...")
    test_dataset = []
    for model_path in EVAL_CLS_MODELS:
        if not os.path.exists(model_path):
            logger.warning("Not Found Model [{}] !".format(model_path))
            continue

        model = load_model(model_path)

        config = AdvAttackConfig()
        # TODO: The config should be changed with different models!
        config.clip_range = (-2.118, 2.64)
        config.eps = 8.0 / 255.0 * (config.clip_range[1] - config.clip_range[0])
        config.nb_iter = 10
        pgd_attack = AdvTool.getWhiteBoxAttack(attack_name="pgd", config=config)

        adv_x_arr = []

        for x in test_dataset:
            # Make AE Attack!
            adv_x = pgd_attack.run(model, x)
            adv_x_arr.append(adv_x)

        # TODO: Record Computation Time

        # Saving Results
        parent_path = Path(model_path).parent
        if os.path.exists("{}/adv_results".format(parent_path)):
            shutil.rmtree("{}/adv_results".format(parent_path))
        os.makedirs("{}/adv_results".format(parent_path))
        with open(
            "{}/adv_results/adv_img.pkl".format(
                parent_path,
            ),
            "wb",
        ) as f:
            pickle.dump(adv_x_arr, f)


if __name__ == "__main__":

    # not used in this stub but often useful for finding various files
    project_dir = Path(__file__).resolve().parents[2]

    # find .env automagically by walking up directories until it's found, then
    # load up the .env entries as environment variables
    load_dotenv(find_dotenv())

    main()
