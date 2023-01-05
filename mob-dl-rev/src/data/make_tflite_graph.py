# -*- coding: utf-8 -*-
import pickle as pk
import os

import shutil
import click
from pathlib import Path
from dotenv import find_dotenv, load_dotenv
from loguru import logger
import numpy as np
import tensorflow as tf
from src.abs_structure.grpah import Graph
from src.abs_structure.node import Node
import sys
from src.utils.value_util import ValueUtil

# import numpy as np

segment_error_filenames = [
    "data/raw/tflite_model/com.voltmemo.xz_cidao_386/5000-colorv4.2-fifty-104x2-p96x96-cdraw_model.tflite"
]


def load_ops(interpreter, graph, op_nodes):
    for idx, op in enumerate(interpreter._get_ops_details()):
        op_name = op["op_name"]
        # Remove delegate node
        if op_name == "DELEGATE":
            continue
        inp_num = len(op["inputs"])
        out_shape = interpreter.get_tensor_details()[op["outputs"][0]]["shape"]
        tmp_node = Node("{}-{}".format(op_name, idx), inp_num, out_shape)
        tmp_node.input_tensor_node_index_arr = op["inputs"]
        tmp_node.output_tensor_node_index_arr = op["outputs"]
        op_nodes.append(tmp_node)
        graph.add_node(tmp_node)
    return graph, op_nodes


def load_tflite_model(file_path):
    logger.debug("Processing {} ...".format(file_path))
    interpreter = None
    try:
        interpreter = tf.lite.Interpreter(model_path=file_path)
    except ValueError:
        return "", ValueUtil.INTERPRET_ERROR
    try:
        interpreter.allocate_tensors()
    except RuntimeError as re:
        logger.error("Runtime Error: {}".format(re.args))
        if "custom op" in re.args[0]:
            return re.args[0].split(":")[1].split(".")[0], ValueUtil.CUSTOM_OP_ERROR

    logger.info("Operator Count: {}".format(len(interpreter._get_ops_details())))

    weights = {}
    # Init Computation Grpah
    graph = Graph()
    op_nodes = []
    graph, op_nodes = load_ops(interpreter, graph, op_nodes)

    # Build a dict <index, node> for memorizing all op nodes
    tensor_node_index_dict = {}
    for tensor in interpreter.get_tensor_details():
        tensor_index = tensor["index"]
        tensor_name = "{}-{}".format(tensor["name"], tensor_index)
        out_shape = tensor["shape"]
        tmp_tensor_node = Node(tensor_name, 0, out_shape)
        tmp_tensor_node.is_extra = True
        # Save all weights
        try:
            weights[tensor_name] = interpreter.get_tensor(tensor_index)
            # logger.debug(weights[tensor_name].dtype)
            if (
                weights[tensor_name].dtype == np.uint8
                or weights[tensor_name].dtype == np.int32
            ):
                quant_mean = 0
                quant_std = 1
                weights[tensor_name] = weights[tensor_name].astype(np.float32)

                if (
                    "quantization_parameters" in tensor
                    and "quantized_dimension" in tensor
                ):
                    if tensor["quantized_dimension"] == 3:
                        weights -= tensor["quantization_parameters"]["zero_points"]
                        weights *= tensor["quantization_parameters"]["scales"]
                    else:
                        logger.warning("Special Quantized Dimension!")
                else:
                    if "quantization" in tensor:
                        quant_mean = tensor["quantization"][1]
                        quant_std = tensor["quantization"][0]
                    else:
                        quant_mean = tensor["quantization_parameters"]["quantization"][
                            1
                        ]
                        quant_std = tensor["quantization_parameters"]["quantization"][0]
                    if quant_mean == 0 and quant_std == 0:
                        quant_std = 1.0
                    # logger.info("Mean: {}, Std: {}".format(quant_mean, quant_std))
                    weights[tensor_name] -= quant_mean
                    weights[tensor_name] *= quant_std

                tmp_tensor_node.is_quant = True
                tmp_tensor_node.quant_params = [quant_mean, quant_std]

        except ValueError:
            pass
        tensor_node_index_dict[tensor_index] = tmp_tensor_node

    for k in tensor_node_index_dict:
        graph.add_node(tensor_node_index_dict[k])

    # Init all links
    for op_node in op_nodes:
        cur_node = graph.find_node_by_name(op_node.name)
        # Handle Input Nodes
        for inp_node_idx in cur_node.input_tensor_node_index_arr:
            if inp_node_idx not in tensor_node_index_dict:
                continue
            inp_node_name = tensor_node_index_dict[inp_node_idx].name
            inp_node = graph.find_node_by_name(inp_node_name)
            inp_node.add_next_node(cur_node)
            cur_node.add_father_node(inp_node)

        # Handle Output Nodes
        for out_node_idx in cur_node.output_tensor_node_index_arr:
            out_node_name = tensor_node_index_dict[out_node_idx].name
            out_node = graph.find_node_by_name(out_node_name)
            out_node.add_father_node(cur_node)
            cur_node.add_next_node(out_node)

    # Delete all intermediate extra nodes
    # For example, [conv2d -> tensor -> conv2d] to [conv2d -> conv2d]
    graph.remove_useless_extra_nodes()

    # for node in graph.node_set:
    #     if node.is_extra or "DEQUANTIZE" in node.name:
    #         continue
    #     has_input = False
    #     for x in node.father_nodes:
    #         if (
    #             "default" in x.name
    #             or "input" in x.name
    #             or "sub_" in x.name
    #             or "Placeholder" in x.name
    #             or "image" in x.name
    #             or "Image" in x.name
    #             or "import" in x.name
    #             or "test_domain" in x.name
    #             or "waveform" in x.name
    #         ):
    #             has_input = True
    #     if has_input:
    #         continue
    #     if sum([x.is_extra for x in node.father_nodes]) == len(node.father_nodes):
    #         logger.warning(node.name)
    #         logger.warning([x.name for x in node.father_nodes])
    #         logger.warning([x.out_shape for x in node.father_nodes])

    return graph, weights


@click.command()
@click.argument("input_filepath", type=click.Path(exists=True))
@click.argument("output_filepath", type=click.Path())
# @click.option("--force_cover", "-f", is_flag=True)
def main(input_filepath, output_filepath):
    """Runs data processing scripts to turn raw data from (../raw) into
    cleaned data ready to be analyzed (saved in ../processed).
    """
    # logger = logging.getLogger(__name__)
    logger.info("making final data set from raw data")

    error_custom_ops = set()
    error_custom_model = []
    error_interpret_model = set()
    total_tflite_model = set()
    for apk_folder in os.listdir(input_filepath):
        apk_folder_path = "{}/{}".format(input_filepath, apk_folder)

        if os.path.isdir(apk_folder_path):
            save_folder_path = "{}/{}".format(output_filepath, apk_folder)
            # if os.path.exists(save_folder_path) and not force_cover:
            #     continue
            if os.path.exists(save_folder_path):
                shutil.rmtree(save_folder_path)
            os.makedirs(save_folder_path)
            for filename in os.listdir(apk_folder_path):
                file_path = "{}/{}".format(apk_folder_path, filename)
                if ".tflite" not in filename or file_path in segment_error_filenames:
                    continue
                total_tflite_model.add(file_path)
                graph, weights = load_tflite_model(file_path)
                # Handle Error
                if isinstance(weights, int):
                    if weights == ValueUtil.CUSTOM_OP_ERROR:
                        error_custom_ops.add(graph)
                        error_custom_model.append(file_path)
                    elif weights == ValueUtil.INTERPRET_ERROR:
                        error_interpret_model.add(file_path)
                    continue
                # Save Results
                pk.dump(
                    graph,
                    open("{}/{}-graph.pkl".format(save_folder_path, filename), "wb"),
                )
                pk.dump(
                    weights,
                    open("{}/{}-weights.pkl".format(save_folder_path, filename), "wb"),
                )
    logger.info("Total TF-Lite Model Number: {}".format(len(total_tflite_model)))
    if len(error_interpret_model) > 0:
        logger.warning(
            "Error Interpret TF-Lite Model Number: {}".format(
                len(error_interpret_model)
            )
        )
        logger.warning("Error Interpret Model List: {}".format(error_interpret_model))
        logger.warning(
            "Error Interpret Rate: {:.2f}".format(
                len(error_interpret_model)
                / (len(total_tflite_model) + len(segment_error_filenames))
            )
        )
    if len(error_custom_ops) > 0:
        logger.warning("Error Custom Operators: {}".format(error_custom_ops))
        logger.warning("Error Custom Op Model: {}".format(error_custom_model))
        logger.warning(
            "Error Custom Op Rate: {:.2f}".format(
                len(error_custom_model)
                / (len(total_tflite_model) + len(segment_error_filenames))
            )
        )
    if len(segment_error_filenames) > 0:
        logger.warning("Segment Error Model: {}".format(segment_error_filenames))
        logger.warning(
            "Segment Error Rate: {:.2f}".format(
                len(segment_error_filenames)
                / (len(total_tflite_model) + len(segment_error_filenames))
            )
        )


if __name__ == "__main__":
    sys.setrecursionlimit(100000)
    # not used in this stub but often useful for finding various files
    project_dir = Path(__file__).resolve().parents[2]
    # find .env automagically by walking up directories until it's found, then
    # load up the .env entries as environment variables
    load_dotenv(find_dotenv())

    main()
