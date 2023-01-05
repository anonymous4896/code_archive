# from model.net import CLSNetV7
# from my_utils.parsing_util import POOLING_SIZE
# from loguru import logger
from typing import Dict
from loguru import logger
import numpy as np
from src.abs_structure.node import Node
import math

# import torch
# import os


class ValidateUtil:
    def __init__(self) -> None:
        pass

    @staticmethod
    def validateNode(node: Node, weights: Dict) -> Node:
        # logger.debug("Validating node {} ...".format(node.name))
        # logger.debug([x.name for x in node.father_nodes])
        # logger.debug([x.out_shape for x in node.father_nodes])
        # logger.debug([x.is_extra for x in node.father_nodes])
        # logger.debug(node.out_shape)
        # logger.debug(node.in_shape)
        # if node.function_type == "Unpack":
        #     logger.debug([x.name for x in node.next_nodes])

        in_shape = node.in_shape
        out_shape = node.out_shape

        if (
            len(node.father_nodes) > 0
            and node.father_nodes[0].is_extra
            and not node.father_nodes[0].is_input
        ):
            for x in node.father_nodes:
                if not x.is_extra:
                    in_shape = [x.out_shape]
                    # logger.debug("Change InShape!")

        if node.function_type == "Cropping2D":
            # Keras shape is [B, H. W, C]
            tmp_in_shape = in_shape[0]
            if len(tmp_in_shape) == 3:
                tmp_in_shape = [1] + tmp_in_shape
            if "cropping" not in node.function_param:
                node.function_param["cropping"] = [0, 0, 0, 0]
            h_top, h_bottom, w_left, w_right = node.function_param["cropping"]
            if tmp_in_shape[1] - h_top - h_bottom != out_shape[1]:
                node.function_param["cropping"][1] = (
                    tmp_in_shape[1] - out_shape[1] - h_top
                )
            if tmp_in_shape[2] - w_left - w_right != out_shape[2]:
                node.function_param["cropping"][3] = (
                    tmp_in_shape[2] - out_shape[2] - w_left
                )
        elif node.function_type == "Mean":
            axis = []
            if len(in_shape[0]) > len(out_shape):
                node.function_param["keep_dims"] = "False"
                axis = [i for i in range(len(in_shape[0]))]
                for x in out_shape:
                    axis.remove(list(in_shape[0]).index(x))
            else:
                node.function_param["keep_dims"] = "True"
                for i in range(len(out_shape)):
                    if out_shape[i] == 1 and in_shape[0][i] != 1:
                        axis.append(i)
            node.function_param["axis"] = axis

        elif node.function_type == "Argmax":
            dimension = 0
            for i in range(len(in_shape[0])):
                if in_shape[0][i] not in out_shape:
                    dimension = i
                    break
            node.function_param["dimension"] = dimension

        elif node.function_type == "StridedSlice":
            extra_node_names = []
            for x in node.extra_input:
                if not x.is_input:
                    extra_node_names.append(x.name)
            node.function_param["begin"] = list(weights[extra_node_names[0]])
            node.function_param["end"] = list(weights[extra_node_names[1]])

        elif node.function_type == "Transpose":
            extra_node_names = []
            for x in node.extra_input:
                if not x.is_input:
                    extra_node_names.append(x.name)
            node.function_param["perm"] = list(weights[extra_node_names[0]])

        elif node.function_type == "Pack":
            input_num = len(in_shape)
            node.function_param["axis"] = list(node.out_shape).index(input_num)

        elif node.function_type == "Unpack":
            # TODO: Need Fix!
            input_num = len(in_shape)
            node.function_param["axis"] = list(node.out_shape).index(input_num)

        elif node.function_type == "Space2Depth":
            node.function_param["block_size"] = in_shape[0][1] // out_shape[1]

        elif node.function_type == "Conv2D" or node.function_type == "DepthwiseConv2D":

            # Check filter
            if (
                "filters" not in node.function_param
                or out_shape[-1] != node.function_param["filters"]
            ):
                node.function_param["filters"] = out_shape[-1]

            if len(node.in_shape[0]) == 0:
                logger.warning(node.name)

            # Check kernel size
            if "kernel_size" not in node.function_param:
                node.function_param["kernel_size"] = 1
            tmp_kernel_size = (-1, -1)
            if len(node.extra_input) == 1:
                tmp_kernel_size = (
                    node.extra_input[0].out_shape[1],
                    node.extra_input[0].out_shape[2],
                )
            else:
                for extra in node.extra_input:
                    # logger.debug(extra.out_shape)
                    if extra.is_input or len(extra.out_shape) < 4:
                        continue
                    if extra.out_shape[3] == node.in_shape[0][3]:
                        tmp_kernel_size = (extra.out_shape[1], extra.out_shape[2])
                        break
                    if node.function_type == "DepthwiseConv2D":
                        tmp_kernel_size = (extra.out_shape[1], extra.out_shape[2])
                        break

            node.function_param["kernel_size"] = tmp_kernel_size

            # Check the strides
            if "strides" not in node.function_param:
                node.function_param["strides"] = [1, 1]

            h_out, w_out = out_shape[1], out_shape[2]
            h_in, w_in = in_shape[0][1], in_shape[0][2]
            tmp_stirdes = [1, 1]
            if h_out != 1 and w_out != 1:
                tmp_stirdes = [
                    round((h_in - tmp_kernel_size[0]) / (h_out - 1)),
                    round((w_in - tmp_kernel_size[1]) / (w_out - 1)),
                ]
                if h_in == tmp_kernel_size[0]:
                    tmp_stirdes = [
                        round((h_in + 2 - tmp_kernel_size[0]) / (h_out - 1)),
                        round((w_in + 2 - tmp_kernel_size[1]) / (w_out - 1)),
                    ]
            if h_out == 1 and w_out == 1 and h_in == 2 and tmp_kernel_size[0] == 3:
                tmp_stirdes = [2, 2]

            if tmp_kernel_size[0] >= h_in:
                basic_out = h_in + 2 - tmp_kernel_size[0] + 1
                tmp_stirdes[0] = round(basic_out / h_out)

            if tmp_kernel_size[1] >= w_in:
                basic_out = w_in + 2 - tmp_kernel_size[1] + 1
                tmp_stirdes[1] = round(basic_out / w_out)

            node.function_param["strides"][0] = max(1, tmp_stirdes[0])
            node.function_param["strides"][1] = max(1, tmp_stirdes[1])

            # Check padding
            k = node.function_param["kernel_size"]
            strides = node.function_param["strides"]

            # Check the padding !
            if int((h_in - k[0]) / strides[0] + 1) == h_out:
                node.function_param["padding"] = "valid"
            else:
                node.function_param["padding"] = "same"

            # Check the DepthwiseConv2D
            if node.function_type == "DepthwiseConv2D":
                in_channel = node.in_shape[0][-1]
                out_channel = node.out_shape[-1]
                node.function_param["depth_multiplier"] = out_channel // in_channel

        elif node.function_type == "Conv2DTranspose":
            import tensorflow as tf

            # Filter
            node.function_param["filters"] = out_shape[-1]

            # Kernel Size
            if "kernel_size" not in node.function_param:
                node.function_param["kernel_size"] = 1
            tmp_kernel_size = -1
            if len(node.extra_input) == 1:
                tmp_kernel_size = node.extra_input[0].out_shape[1]
            else:
                for extra in node.extra_input:
                    # logger.debug(extra.out_shape)
                    if extra.is_input:
                        continue
                    if (
                        len(extra.out_shape) > 1
                        and extra.out_shape[3] == node.in_shape[0][3]
                    ):
                        tmp_kernel_size = extra.out_shape[1]
                        break
            if (
                tmp_kernel_size != -1
                and tmp_kernel_size != node.function_param["kernel_size"]
            ):
                node.function_param["kernel_size"] = tmp_kernel_size

            # Strides
            node.function_param["strides"] = [1, 1]
            h_out, w_out = out_shape[1], out_shape[2]
            h_in, w_in = in_shape[0][1], in_shape[0][2]
            tmp_stirdes = [1, 1]
            if h_out != 1 and w_out != 1:
                tmp_stirdes = [
                    round((h_out - tmp_kernel_size) / (h_in - 1)),
                    round((w_out - tmp_kernel_size) / (w_in - 1)),
                ]
                if h_in == tmp_kernel_size:
                    tmp_stirdes = [
                        round((h_out + 2 - tmp_kernel_size) / (h_in - 1)),
                        round((w_out + 2 - tmp_kernel_size) / (w_in - 1)),
                    ]

            node.function_param["strides"][0] = max(1, tmp_stirdes[0])
            node.function_param["strides"][1] = max(1, tmp_stirdes[1])

            # Check padding
            k = node.function_param["kernel_size"]
            strides = node.function_param["strides"]

            # Check the padding !
            if int((h_in - 1) * strides[0] + k) == h_out:
                node.function_param["padding"] = "valid"
            else:
                node.function_param["padding"] = "same"

            # run real test
            conv2dtrans = tf.keras.layers.Conv2DTranspose(
                filters=node.function_param["filters"],
                kernel_size=(tmp_kernel_size, tmp_kernel_size),
                strides=tmp_stirdes,
                padding=node.function_param["padding"],
                activation=None,
                use_bias=False,
            )
            x = tf.constant(np.random.random((in_shape[0])))
            tmp_out = conv2dtrans(x)
            while tmp_out.shape != out_shape:
                tmp_stirdes = (tmp_stirdes[0] + 1, tmp_stirdes[1] + 1)
                conv2dtrans = tf.keras.layers.Conv2DTranspose(
                    filters=node.function_param["filters"],
                    kernel_size=(tmp_kernel_size, tmp_kernel_size),
                    strides=tmp_stirdes,
                    padding=node.function_param["padding"],
                    activation=None,
                    use_bias=False,
                )
                tmp_out = conv2dtrans(x)
            node.function_param["strides"] = tmp_stirdes

        elif (
            node.function_type == "MaxPooling2D"
            or node.function_type == "MaxPooling3D"
            or node.function_type == "AveragePooling2D"
            or node.function_type == "AveragePooling3D"
        ):
            if "pool_size" not in node.function_param:
                node.function_param["pool_size"] = None

            pool_size = round(in_shape[0][1] / out_shape[1])
            if len(in_shape[0]) == 3:
                pool_size = round(in_shape[0][0] / out_shape[1])

            padding = "valid"
            if len(in_shape[0]) == 3:
                if in_shape[0][0] // (pool_size + 1e-15) < out_shape[1]:
                    padding = "same"
            elif in_shape[0][1] // (pool_size + 1e-15) < out_shape[1]:
                padding = "same"

            strides = max(pool_size, 1)
            if padding == "valid":
                while (
                    math.floor((in_shape[0][1] - pool_size) / strides) + 1
                    != out_shape[1]
                ):
                    if (
                        math.floor((in_shape[0][1] - pool_size) / strides) + 1
                        > out_shape[1]
                    ):
                        pool_size += 1
                    else:
                        pool_size -= 1
            node.function_param["strides"] = strides

            node.function_param["pool_size"] = pool_size

            if (
                "padding" not in node.function_param
                or node.function_param["padding"] != padding
            ):
                node.function_param["padding"] = padding

            # if node.function_param["padding"] == "same" and node.function_param[
            #         "strides"] == 2 and node.function_param["pool_size"] != 1:
            #     node.function_param["pool_size"] = 3

        elif node.function_type == "UpSampling" or node.function_type == "UpSampling3D":
            size = out_shape[1] // in_shape[0][1]
            if len(in_shape[0]) == 3:
                size = out_shape[0] // in_shape[0][0]
            if size != node.function_param["size"]:
                node.function_param["size"] = size
        elif node.function_type == "Pad" or node.function_type == "MirrorPad":

            node.function_param["padding"] = [
                [0, 0] for _ in range(len(node.in_shape[0]))
            ]
            tmp_in_shape = in_shape[0]
            for i in range(len(tmp_in_shape)):
                pad = (out_shape[i] - tmp_in_shape[i]) // 2
                node.function_param["padding"][i] = [
                    pad,
                    (out_shape[i] - tmp_in_shape[i]) - pad,
                ]

        elif node.function_type == "Space2Batch":
            block_size = int(out_shape[0] ** 0.5)
            node.function_param["block_shape"] = [block_size, block_size]
            paddings = [[0, 0], [0, 0]]
            paddings[0][0] = (out_shape[1] * block_size - in_shape[0][1]) // 2
            paddings[1][0] = (out_shape[2] * block_size - in_shape[0][2]) // 2
            paddings[0][1] = (out_shape[1] * block_size - in_shape[0][1]) - paddings[0][
                0
            ]
            paddings[1][1] = (out_shape[1] * block_size - in_shape[0][1]) - paddings[1][
                0
            ]
            node.function_param["paddings"] = paddings
        elif node.function_type == "Batch2Space":
            block_size = int(in_shape[0][0] ** 0.5)
            node.function_param["block_shape"] = [block_size, block_size]
            crops = [[0, 0], [0, 0]]
            crops[0][0] = (in_shape[0][1] * block_size - out_shape[1]) // 2
            crops[1][0] = (in_shape[0][2] * block_size - out_shape[2]) // 2
            crops[0][1] = (in_shape[0][1] * block_size - out_shape[1]) - crops[0][0]
            crops[1][1] = (in_shape[0][2] * block_size - out_shape[2]) - crops[1][0]
            node.function_param["crops"] = crops

        else:
            # TODO: Add more validation of these parameters
            pass
        return node
