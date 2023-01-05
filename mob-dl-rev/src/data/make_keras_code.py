import pickle as pk
import click
from pathlib import Path
from dotenv import find_dotenv, load_dotenv
from loguru import logger
from src.abs_structure.grpah import Graph
from src.utils.recover_keras_util import RecoverUtilKeras
import sys


@click.command()
@click.argument("input_filepath", type=click.Path(exists=True))
@click.argument("output_filepath", type=click.Path())
def main(input_filepath, output_filepath):
    """Generate the keras code with the complete computation graph input"""
    # logger = logging.getLogger(__name__)
    logger.info("Generating Keras Code for [{}]...".format(input_filepath))
    graph: Graph = pk.load(open(input_filepath, "rb"))
    weight_name = input_filepath.replace("cmpgraph", "weights")

    util = RecoverUtilKeras(graph=graph, weight_name=weight_name)
    util.generate_init_codes()
    util.generate_forward_codes()
    # logger.info("\n" + util.output_model_code())
    with open(output_filepath, "w") as f:
        f.write(util.output_model_code())
    if util.has_strange_quant:
        logger.warning("Starnge Quant: {}".format(input_filepath))


if __name__ == "__main__":
    sys.setrecursionlimit(100000)
    # not used in this stub but often useful for finding various files
    project_dir = Path(__file__).resolve().parents[2]
    # find .env automagically by walking up directories until it's found, then
    # load up the .env entries as environment variables
    load_dotenv(find_dotenv())

    main()
