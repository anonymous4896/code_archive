import os
import click
from pathlib import Path
from dotenv import find_dotenv, load_dotenv
from loguru import logger
import sys


@click.command()
@click.argument("input_filepath", type=click.Path(exists=True))
def main(input_filepath):
    """Check the running log if there is some Errors"""
    py_counter = 0
    err_counter = 0
    failure_cases = []
    succ_cases = []
    for folder_name in os.listdir(input_filepath):
        folder_path = "{}/{}".format(input_filepath, folder_name)
        for filename in os.listdir(folder_path):
            if ".result" not in filename:
                continue
            py_counter += 1
            with open("{}/{}".format(folder_path, filename), "r") as f:
                if "Error" in "".join(f.readlines()):
                    err_counter += 1
                    failure_cases.append(folder_path + "/" + filename)
                else:
                    succ_cases.append(folder_path + "/" + filename)
    logger.info(
        "Total: {}, Succ: {}, Fail: {}".format(
            py_counter, py_counter - err_counter, err_counter
        )
    )
    logger.info("Success Rate: {:.2f}".format((py_counter - err_counter) / py_counter))
    logger.info("Failure Cases: {}".format(failure_cases))
    logger.info("Succ Cases: {}".format(succ_cases))


if __name__ == "__main__":
    sys.setrecursionlimit(100000)
    # not used in this stub but often useful for finding various files
    project_dir = Path(__file__).resolve().parents[2]
    # find .env automagically by walking up directories until it's found, then
    # load up the .env entries as environment variables
    load_dotenv(find_dotenv())

    main()
