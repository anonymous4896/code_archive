import os
import pickle as pk
import click
from pathlib import Path
from dotenv import find_dotenv, load_dotenv
from loguru import logger
from src.abs_structure.grpah import Graph
from src.utils.type_mapping_util import TypeMappingUtil
from src.utils.validation_util import ValidateUtil
import sys


@click.command()
@click.argument("input_filepath", type=click.Path(exists=True))
@click.argument("output_filepath", type=click.Path())
def main(input_filepath, output_filepath):
    """Completing our computation graphs.
    In this step, we convert the tf-lite operator type to our operator type.
    Then, we compute all necessary attributes of all operators.
    """
    logger.info("Completing our computation graph for [{}]...".format(input_filepath))
    graph: Graph = pk.load(open(input_filepath, "rb"))
    weights = pk.load(open(input_filepath.replace("-graph", "-weights"), "rb"))
    op_set = set()
    for node in graph.node_set:
        if not node.is_extra:
            # if len(node.in_shape) == 0:
            in_shape = []
            for x in node.father_nodes:
                if (
                    "default" in x.name
                    or "input" in x.name
                    or "Placeholder" in x.name
                    or (
                        "image" in x.name
                        and "pooling" not in x.name
                        and "y" not in x.name
                    )
                    or "Image" in x.name
                    or "import/x" in x.name
                    or "test_domain" in x.name
                    or "waveform" in x.name
                    or "sub_2-93" == x.name
                    or "sub_7-278" == x.name
                    or "sub_7-183" == x.name
                    or "style_image" in x.name
                    or "mobilenet_conv/Conv/BiasAdd-1" == x.name
                ):
                    logger.warning(x.name)
                    x.is_input = True
                    in_shape = x.out_shape
                    node.in_shape = [in_shape] + node.in_shape

            op_set.add(node.name.split("-")[0])
            node.function_type = TypeMappingUtil.map_type(node, node.name.split("-")[0])
            if node.function_type == "NoType":
                logger.warning("Skip Model {}".format(input_filepath))
                logger.warning("Deleting intermediate results ...")
                os.remove(input_filepath)
                os.remove(input_filepath.replace("-graph", "-weights"))
                return

    graph.remove_quant_ops()
    for node in graph.node_set:
        if node.is_extra:
            continue
        node = ValidateUtil.validateNode(node, weights)
    pk.dump(graph, open(output_filepath, "wb"))
    logger.info("Op Set: {}".format(op_set))


if __name__ == "__main__":
    sys.setrecursionlimit(100000)
    # not used in this stub but often useful for finding various files
    project_dir = Path(__file__).resolve().parents[2]
    # find .env automagically by walking up directories until it's found, then
    # load up the .env entries as environment variables
    load_dotenv(find_dotenv())

    main()
