from collections import defaultdict
from typing import List

from src.abs_structure.node import Node
from src.abs_structure.grpah import Graph
from loguru import logger


class RecoverUtilKeras:
    def __init__(self, graph: Graph, weight_name="tmp_weight.pkl") -> None:
        self.graph = graph
        self.import_codes = [
            "import tensorflow as tf",
            "import pickle as pk",
            "import numpy as np",
        ]
        self.model_code = "class RecoverModel(torch.nn.Module):"
        self.init_codes = []

        # TODO: We may need consider multi input
        self.forward_codes = []
        self.return_code = "model = tf.keras.Model(inputs={}, outputs=x_{})"
        self.name_idx_dict = defaultdict(int)
        self.weight_name = weight_name

        # TODO: Add weight init function code
        self.weight_init_code = []

        self.has_strange_quant = False

    def output_model_code(self) -> str:
        model_code = ""
        for import_code in self.import_codes:
            model_code += import_code + "\n"

        model_code += 'weight_data = pk.load(open("{}", "rb"))\n'.format(
            self.weight_name
        )

        for init_code in self.init_codes:
            model_code += init_code + "\n"

        for forward_code in self.forward_codes:
            model_code += forward_code + "\n"

        model_code += self.return_code + "\n"

        for weight_code in self.weight_init_code:
            model_code += weight_code + "\n"

        return model_code

    def get_name_idx(self, func_type):
        self.name_idx_dict[func_type] += 1
        return self.name_idx_dict[func_type]

    def generate_initcode_from_single_node(self, node: Node) -> List:
        code = []

        if node.is_extra:
            return code

        if node.function_type == "Unknown":
            logger.error(
                "There are unknown function node -- atomic_type: {}".format(
                    node.atomic_type
                )
            )
            return code

        if node.function_type == "Conv2D":
            # TODO Add weight initializer
            layer_name = "conv2d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.Conv2D(filters={}, kernel_size={}, strides=({},{}), padding="{}", activation=None, use_bias={})'.format(
                    layer_name,
                    node.function_param["filters"],
                    node.function_param["kernel_size"],
                    node.function_param["strides"][0],
                    node.function_param["strides"][1],
                    node.function_param["padding"],
                    "True"
                    if (
                        len(node.extra_input)
                        - sum([x.is_input for x in node.extra_input])
                    )
                    >= 2
                    else "False",
                )
            )

            weight_init_code_tmp = "{}.set_weights([$])".format(layer_name)
            extra_nodes = node.extra_input
            weight_node = node.extra_input[0]

            bias_node = None
            if len(extra_nodes) > 1 and node.extra_input[0].is_input:
                weight_node = node.extra_input[1]
            if len(extra_nodes) == 3 and node.extra_input[0].is_input:
                weight_node = node.extra_input[1]
                bias_node = node.extra_input[2]
                if len(bias_node.out_shape) > 1:
                    weight_node, bias_node = bias_node, weight_node
            if (
                len(node.next_nodes) > 0
                and node.next_nodes[0].function_type == "BiasAdd"
            ):
                bias_node = node.next_nodes[0].extra_input[0]
                if len(bias_node.out_shape) > 1:
                    weight_node, bias_node = bias_node, weight_node
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes=[1, 2, 3, 0]), weight_data["{}"]'.format(
                        weight_node.name, bias_node.name
                    ),
                )
            elif bias_node is not None:
                if not bias_node.is_quant and weight_node.is_quant:
                    self.has_strange_quant = True
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes=[1, 2, 3, 0]), weight_data["{}"]'.format(
                        weight_node.name, bias_node.name
                    ),
                )
            elif bias_node is None and len(node.extra_input) == 2:
                bias_node = node.extra_input[1]
                if len(bias_node.out_shape) > 1:
                    weight_node, bias_node = bias_node, weight_node
                if not bias_node.is_quant and weight_node.is_quant:
                    self.has_strange_quant = True
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes=[1, 2, 3, 0]), weight_data["{}"]'.format(
                        weight_node.name, bias_node.name
                    ),
                )
            else:
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes=[1, 2, 3, 0])'.format(
                        weight_node.name
                    ),
                )

            self.weight_init_code.append(weight_init_code_tmp)
        elif node.function_type == "Conv2DTranspose":
            layer_name = "conv2dtrans_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            useBias = "True" if len(node.extra_input) >= 2 else "False"
            if (
                sum(
                    [
                        (len(x.out_shape) == 1 and x.out_shape[0] == node.out_shape[-1])
                        for x in node.extra_input
                    ]
                )
                == 0
            ):
                useBias = "False"
            code.append(
                '{} = tf.keras.layers.Conv2DTranspose(filters={}, kernel_size={}, strides=({},{}), padding="{}", activation=None, use_bias={})'.format(
                    layer_name,
                    node.function_param["filters"],
                    node.function_param["kernel_size"],
                    node.function_param["strides"][0],
                    node.function_param["strides"][1],
                    node.function_param["padding"],
                    useBias,
                )
            )

            weight_init_code_tmp = "{}.set_weights([$])".format(layer_name)
            extra_nodes = node.extra_input
            weight_node = node.extra_input[0]

            bias_node = None
            if len(extra_nodes) > 1 and node.extra_input[0].is_input:
                weight_node = node.extra_input[1]
            if len(extra_nodes) == 3 and node.extra_input[0].is_input:
                weight_node = node.extra_input[1]
                bias_node = node.extra_input[2]
                if len(bias_node.out_shape) > 1:
                    weight_node, bias_node = bias_node, weight_node

            if bias_node is not None:
                if not bias_node.is_quant and weight_node.is_quant:
                    self.has_strange_quant = True
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes=[1, 2, 0, 3]), weight_data["{}"]'.format(
                        weight_node.name, bias_node.name
                    ),
                )
            elif bias_node is None and len(node.extra_input) == 2 and useBias == "True":
                bias_node = node.extra_input[1]
                if len(bias_node.out_shape) > 1:
                    weight_node, bias_node = bias_node, weight_node
                if not bias_node.is_quant and weight_node.is_quant:
                    self.has_strange_quant = True
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes=[1, 2, 0, 3]), weight_data["{}"]'.format(
                        weight_node.name, bias_node.name
                    ),
                )
            else:
                if len(weight_node.out_shape) != 4:
                    weight_node = node.extra_input[1]

                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes=[1, 2, 0, 3])'.format(
                        weight_node.name
                    ),
                )

            self.weight_init_code.append(weight_init_code_tmp)

            # if layer_name == "conv2dtrans_4":
            #     logger.warning(code[-1])
            #     logger.warning(node.name)
            #     logger.warning([extra.out_shape for extra in node.extra_input])
            #     logger.warning(node.in_shape)
            #     logger.warning(node.out_shape)
            #     logger.warning(node.function_param["strides"])

        elif node.function_type == "Conv3D":
            layer_name = "conv3d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            useBias = True
            # TODO useBias can be inferred by the weight number
            if len(node.extra_input) < 2:
                useBias = False
            code.append(
                '{} = tf.keras.layers.Conv3D(filters={}, kernel_size={}, strides=({},{},{}), padding="{}", activation=None, use_bias={})'.format(
                    layer_name,
                    node.function_param["filters"],
                    node.function_param["kernel_size"],
                    node.function_param["strides"][0],
                    node.function_param["strides"][1],
                    node.function_param["strides"][2],
                    node.function_param["padding"],
                    "True" if useBias else "False",
                )
            )

        elif node.function_type == "SeparableConv2D":
            # TODO Add weight initializer
            layer_name = "sp_conv2d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.SeparableConv2D(filters={}, kernel_size={}, strides=({},{}), padding="{}", activation=None, depth_multiplier={})'.format(
                    layer_name,
                    node.function_param["filters"],
                    node.function_param["kernel_size"],
                    node.function_param["strides"][0],
                    node.function_param["strides"][1],
                    node.function_param["padding"],
                    # node.function_param["dilation_rate"][0],
                    # node.function_param["dilation_rate"][1],
                    # node.function_param["activation"] if node.function_param["activation"] is not None else "None",
                    node.function_param["depth_multiplier"],
                )
            )
        elif node.function_type == "DepthwiseConv2D":
            # TODO Add weight initializer
            layer_name = "dw_conv2d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.DepthwiseConv2D(kernel_size={}, strides=({},{}), padding="{}", activation=None, depth_multiplier={}, use_bias={})'.format(
                    layer_name,
                    node.function_param["kernel_size"],
                    node.function_param["strides"][0],
                    node.function_param["strides"][1],
                    node.function_param["padding"],
                    # node.function_param["dilation_rate"][0], node.function_param["dilation_rate"][1],
                    # node.function_param["activation"] if node.function_param["activation"] is not None else "None",
                    node.function_param["depth_multiplier"],
                    "True"
                    if (
                        len(node.extra_input)
                        - sum([x.is_input for x in node.extra_input])
                    )
                    >= 2
                    else "False",
                )
            )

            # if layer_name == "dw_conv2d_20":
            # logger.warning(code[-1])
            # logger.warning(node.name)
            # logger.warning([extra.out_shape for extra in node.extra_input])
            # logger.warning(node.in_shape)
            # logger.warning(node.out_shape)
            # logger.warning(node.function_param["strides"])
            # logger.warning(len(node.extra_input))
            # logger.warning([extra.out_shape for extra in node.extra_input])

            weight_init_code_tmp = "{}.set_weights([$])".format(layer_name)
            extra_nodes = node.extra_input
            weight_node = node.extra_input[0]
            bias_node = None
            if len(extra_nodes) > 1 and node.extra_input[0].is_input:
                weight_node = node.extra_input[1]
            if len(extra_nodes) == 3 and node.extra_input[0].is_input:
                weight_node = node.extra_input[1]
                bias_node = node.extra_input[2]
            axes = [1, 2, 3, 0]
            if node.function_param["depth_multiplier"] != 1:
                axes = [1, 2, 0, 3]
            if (
                len(node.next_nodes) > 0
                and node.next_nodes[0].function_type == "BiasAdd"
            ):
                bias_node = node.next_nodes[0].extra_input[0]
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes={}), weight_data["{}"]'.format(
                        weight_node.name, axes, bias_node.name
                    ),
                )
            elif bias_node is not None:
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes={}), weight_data["{}"]'.format(
                        weight_node.name, axes, bias_node.name
                    ),
                )
            elif bias_node is None and len(node.extra_input) == 2:
                bias_node = node.extra_input[1]
                if len(bias_node.out_shape) > 1:
                    weight_node, bias_node = bias_node, weight_node
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes={}), weight_data["{}"]'.format(
                        weight_node.name, axes, bias_node.name
                    ),
                )
            else:
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'np.transpose(weight_data["{}"], axes={})'.format(
                        weight_node.name, axes
                    ),
                )

            self.weight_init_code.append(weight_init_code_tmp)

        elif node.function_type == "Cropping2D":
            layer_name = "crop2d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                "{} = tf.keras.layers.Cropping2D(cropping=(({},{}),({},{})))".format(
                    layer_name,
                    node.function_param["cropping"][0],
                    node.function_param["cropping"][1],
                    node.function_param["cropping"][2],
                    node.function_param["cropping"][3],
                )
            )

        elif node.function_type == "UpSampling2D":
            layer_name = "up2d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.UpSampling2D(size={}, interpolation="{}")'.format(
                    layer_name,
                    node.function_param["size"],
                    node.function_param["interpolation"],
                )
            )

        elif node.function_type == "UpSampling3D":
            layer_name = "up3d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                "{} = tf.keras.layers.UpSampling2D(size={})".format(
                    layer_name, node.function_param["size"]
                )
            )

        elif node.function_type == "Pad":
            extra_pad = [[0, 0]]
            if len(node.function_param["padding"]) == 4 or len(node.in_shape[0]) == 2:
                extra_pad = []
            if len(node.in_shape[0]) == len(node.out_shape) and list(
                node.in_shape[0]
            ) == list(node.out_shape):
                node.recover_layer_name = "% = $"
                return code
            node.recover_layer_name = "% = tf.pad($, paddings={})".format(
                extra_pad + node.function_param["padding"]
            )

        elif node.function_type == "MirrorPad":
            node.recover_layer_name = (
                '% = tf.pad($, paddings={}, mode="REFLECT")'.format(
                    node.function_param["padding"]
                )
            )

        elif node.function_type == "Mean":
            node.recover_layer_name = (
                "% = tf.keras.backend.mean($, axis={}, keepdims={})".format(
                    node.function_param["axis"], node.function_param["keep_dims"]
                )
            )

        elif node.function_type == "Argmax":
            node.recover_layer_name = "% = tf.keras.backend.argmax($, axis={})".format(
                node.function_param["dimension"]
            )

        elif node.function_type == "ZeroPadding3D":
            layer_name = "zp3d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                "{} = tf.keras.layers.ZeroPadding3D(padding={})".format(
                    layer_name, node.function_param["padding"]
                )
            )

        elif node.function_type == "Concatenate":
            layer_name = "concat_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            axis = 0
            # logger.info(node.in_shape)
            # logger.info(node.out_shape)

            for idx, d in enumerate(node.in_shape[0]):
                if d != node.out_shape[idx]:
                    axis = idx
                    break
            code.append(
                "{} = tf.keras.layers.Concatenate(axis={})".format(layer_name, axis)
            )

        elif node.function_type == "Add":
            if (
                len(node.extra_input)
                == 1
                # and len(node.extra_input[0].out_shape) == 1
                # and len(node.in_shape[0]) == 4
                # and node.in_shape[0][-1] == node.extra_input[0].out_shape[0]
            ):
                node.recover_layer_name = "$ = %1+%2"
                return code
            layer_name = "add_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.Add()".format(layer_name))

        elif node.function_type == "Subtract":
            # if len(node.extra_input) == 1 and len(node.extra_input[0].out_shape) == 1:
            node.recover_layer_name = "$ = %1-%2"
            return code
            # layer_name = "sub_{}".format(self.get_name_idx(node.function_type))
            # node.recover_layer_name = layer_name
            # code.append("{} = tf.keras.layers.Subtract()".format(layer_name))

        elif node.function_type == "Multiply":
            # if (
            #     len(node.extra_input) == 1
            #     # and len(node.extra_input[0].out_shape) == 1
            #     # and len(node.in_shape[0]) == 4
            #     # and node.in_shape[0][-1] == node.extra_input[0].out_shape[0]
            # ):
            node.recover_layer_name = "$ = %1*%2"
            return code
            # layer_name = "mul_{}".format(self.get_name_idx(node.function_type))
            # node.recover_layer_name = layer_name
            # code.append("{} = tf.keras.layers.Multiply()".format(layer_name))

        elif node.function_type == "Divide":
            # if len(node.extra_input) == 1 and len(node.extra_input[0].out_shape) == 1:
            node.recover_layer_name = "$ = %1/%2"
            return code

        elif node.function_type == "Dot":
            layer_name = "dot_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                "{} = tf.keras.layers.Dot(axes={})".format(
                    layer_name, node.function_param["axes"]
                )
            )

        elif node.function_type == "Log":
            node.recover_layer_name = "% = tf.math.log($)"

        elif node.function_type == "Exp":
            node.recover_layer_name = "% = tf.math.exp($)"

        elif node.function_type == "Sqrt":
            node.recover_layer_name = "% = tf.keras.backend.sqrt($)"

        elif node.function_type == "RSqrt":
            node.recover_layer_name = "% = 1./tf.keras.backend.sqrt($)"

        elif node.function_type == "Abs":
            node.recover_layer_name = "% = tf.math.abs($)"

        elif node.function_type == "Negative":
            node.recover_layer_name = "% = tf.raw_ops.Neg($)"

        elif node.function_type == "Square":
            node.recover_layer_name = "% = tf.keras.backend.square($)"

        elif node.function_type == "LogicalNot":
            node.recover_layer_name = "% = tf.math.logical_not($)"

        elif node.function_type == "Sin":
            node.recover_layer_name = "% = tf.math.sin($)"

        elif node.function_type == "Cos":
            node.recover_layer_name = "% = tf.math.cos($)"

        elif node.function_type == "SquaredDifference":
            node.recover_layer_name = "% = tf.raw_ops.SquaredDifference(x=$1, y=$2)"

        elif node.function_type == "GreaterEqual":
            node.recover_layer_name = "$ = tf.keras.backend.greater_equal(x=%1, y=%2)"
            # logger.debug(node.in_shape)
            # logger.debug([x.name for x in node.father_nodes])

        elif node.function_type == "Less":
            node.recover_layer_name = "$ = tf.math.less(x=%1, y=%2)"

        elif node.function_type == "Transpose":
            node.recover_layer_name = "% = tf.raw_ops.Transpose($, perm={})".format(
                node.function_param["perm"]
            )

        elif node.function_type == "Pack":
            node.recover_layer_name = "% = tf.stack($, axis={})".format(
                node.function_param["axis"]
            )

        elif node.function_type == "Unpack":
            node.recover_layer_name = (
                "% = tf.unstack(value=$, num={}, axis={})[0]".format(
                    len(node.next_nodes), node.function_param["axis"]
                )
            )

        elif node.function_type == "ReduceMax":
            node.recover_layer_name = "% = tf.math.reduce_max($)"

        elif node.function_type == "Space2Depth":
            node.recover_layer_name = (
                "% = tf.nn.space_to_depth(input=$, block_size={})".format(
                    node.function_param["block_size"]
                )
            )

        elif node.function_type == "Maximum":
            layer_name = "max_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.Maximum()".format(layer_name))

        elif node.function_type == "Minimum":
            layer_name = "min_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.Minimum()".format(layer_name))

        elif node.function_type == "Average":
            layer_name = "avg_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.Average()".format(layer_name))

        elif node.function_type == "ReLU":

            layer_name = "relu_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                "{} = tf.keras.layers.ReLU(max_value={})".format(
                    layer_name,
                    "None"
                    if "a_max" not in node.function_param
                    else node.function_param["a_max"][:-1],
                )
            )

        elif node.function_type == "Softmax":
            layer_name = "softmax_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.Softmax()".format(layer_name))

        elif node.function_type == "LeakyReLU":
            layer_name = "leakyrelu_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.LeakyReLU()".format(layer_name))

        elif node.function_type == "PReLU":
            layer_name = "prelu_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.PReLU()".format(layer_name))

        elif node.function_type == "ELU":
            layer_name = "elu_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.ELU()".format(layer_name))

        elif node.function_type == "ThresholdedReLU":
            layer_name = "thresholdedrelu_{}".format(
                self.get_name_idx(node.function_type)
            )
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.ThresholdedReLU()".format(layer_name))

        elif node.function_type == "Logistic":
            layer_name = "logistic_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append('{} = tf.keras.layers.Activation("sigmoid")'.format(layer_name))

        elif node.function_type == "Tanh":
            layer_name = "tanh_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append('{} = tf.keras.layers.Activation("tanh")'.format(layer_name))

        elif node.function_type == "MaxPooling2D":
            layer_name = "mp2d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.MaxPooling2D(pool_size={}, padding="{}", strides={})'.format(
                    layer_name,
                    node.function_param["pool_size"],
                    node.function_param["padding"],
                    node.function_param["strides"],
                )
            )

        elif node.function_type == "MaxPooling3D":
            layer_name = "mp3d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.MaxPooling3D(pool_size={}, padding="{}", strides={})'.format(
                    layer_name,
                    node.function_param["pool_size"],
                    node.function_param["padding"],
                    node.function_param["strides"],
                )
            )

        elif node.function_type == "AveragePooling2D":
            layer_name = "avgp2d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.AveragePooling2D(pool_size={}, padding="{}", strides={})'.format(
                    layer_name,
                    node.function_param["pool_size"],
                    node.function_param["padding"],
                    node.function_param["strides"],
                )
            )

        elif node.function_type == "AveragePooling3D":
            layer_name = "avgp3d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.AveragePooling3D(pool_size={}, padding="{}", strides={})'.format(
                    layer_name,
                    node.function_param["pool_size"],
                    node.function_param["padding"],
                    node.function_param["strides"],
                )
            )

        elif node.function_type == "GlobalMaxPooling2D":
            layer_name = "gbmp2d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.GlobalMaxPooling2D()".format(layer_name))

        elif node.function_type == "GlobalMaxPooling3D":
            layer_name = "gbmp3d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.GlobalMaxPooling3D()".format(layer_name))

        elif node.function_type == "GlobalAveragePooling2D":
            layer_name = "gbavgp2d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                "{} = tf.keras.layers.GlobalAveragePooling2D()".format(layer_name)
            )

        elif node.function_type == "GlobalAveragePooling3D":
            layer_name = "gbavgp3d_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                "{} = tf.keras.layers.GlobalAveragePooling3D()".format(layer_name)
            )

        elif node.function_type == "Dense":
            layer_name = "ds_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                "{} = tf.keras.layers.Dense(activation=None, units={}, use_bias={})".format(
                    layer_name,
                    node.out_shape[-1],
                    "True"
                    if (
                        len(node.extra_input)
                        - sum([x.is_input for x in node.father_nodes])
                    )
                    == 2
                    else "False",
                )
            )

            # Add weight initializer
            weight_init_code_tmp = "{}.set_weights([$])".format(layer_name)

            extra_nodes = node.extra_input
            weight_node = node.extra_input[0]
            if len(extra_nodes) > 1 and node.extra_input[0].is_input:
                weight_node = node.extra_input[1]
            if (
                len(node.next_nodes) > 0
                and node.next_nodes[0].function_type == "BiasAdd"
            ):
                bias_node = node.next_nodes[0].extra_input[0]
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'weight_data["{}"].T, weight_data["{}"]'.format(
                        weight_node.name, bias_node.name
                    ),
                )
            elif len(node.extra_input) == 2:
                bias_node = node.extra_input[1]
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'weight_data["{}"].T, weight_data["{}"]'.format(
                        weight_node.name, bias_node.name
                    ),
                )
            elif len(node.extra_input) == 3:
                for x in node.extra_input:
                    if x.is_input:
                        continue
                    if len(x.out_shape) == 1:
                        bias_node = x
                    else:
                        weight_node = x
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'weight_data["{}"].T, weight_data["{}"]'.format(
                        weight_node.name, bias_node.name
                    ),
                )
            else:
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$", 'weight_data["{}"].T'.format(weight_node.name)
                )

            self.weight_init_code.append(weight_init_code_tmp)

        elif node.function_type == "Reshape":
            layer_name = "rs_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            out_shape = tuple(list(node.out_shape))
            if len(node.out_shape) > 0 and node.out_shape[0] == 1:
                out_shape = tuple(list(node.out_shape[1:]))

            code.append(
                "{} = tf.keras.layers.Reshape({})".format(layer_name, out_shape)
            )

        elif node.function_type == "Flatten":
            layer_name = "flt_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append("{} = tf.keras.layers.Flatten()".format(layer_name))

        elif node.function_type == "BatchNormalization":
            layer_name = "bn_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                "{} = tf.keras.layers.BatchNormalization(center={}, scale={})".format(
                    layer_name,
                    node.function_param["center"],
                    node.function_param["scale"],
                )
            )

            # Add weight initializer
            weight_init_code_tmp = "{}.set_weights([$])".format(layer_name)
            extra_nodes = node.extra_input
            if node.function_param["center"] and node.function_param["scale"]:
                weights = []
                for x in extra_nodes:
                    if len(x.out_shape) == 0:
                        continue
                    if x.out_shape[0] == node.out_shape[-1]:
                        weights.append(x.name)
                weight_init_code_tmp = weight_init_code_tmp.replace(
                    "$",
                    'weight_data["{}"], weight_data["{}"], weight_data["{}"], weight_data["{}"]'.format(
                        weights[1], weights[2], weights[3], weights[0]
                    ),
                )
            self.weight_init_code.append(weight_init_code_tmp)
        elif node.function_type == "Resize_nn":
            layer_name = "rznn_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.Resizing(height={}, width={}, interpolation="nearest")'.format(
                    layer_name, node.out_shape[1], node.out_shape[2]
                )
            )
        elif node.function_type == "Resize_bi":
            layer_name = "rzbi_{}".format(self.get_name_idx(node.function_type))
            node.recover_layer_name = layer_name
            code.append(
                '{} = tf.keras.layers.Resizing(height={}, width={}, interpolation="bilinear")'.format(
                    layer_name, node.out_shape[1], node.out_shape[2]
                )
            )
        elif node.function_type == "Batch2Space":
            node.recover_layer_name = (
                "% = tf.batch_to_space($, block_shape={}, crops={})".format(
                    node.function_param["block_shape"], node.function_param["crops"]
                )
            )
        elif node.function_type == "Space2Batch":
            node.recover_layer_name = (
                "% = tf.space_to_batch($, block_shape={}, paddings={})".format(
                    node.function_param["block_shape"],
                    node.function_param["paddings"],
                )
            )

        elif node.function_type == "Split":
            tmp_axis = 0
            for i in range(len(node.out_shape)):
                if node.in_shape[0][i] != node.out_shape[i]:
                    tmp_axis = i
                    break
            node.recover_layer_name = (
                "% = tf.split($, num_or_size_splits={}, axis={})".format(
                    len(node.next_nodes), tmp_axis
                )
            )

        elif node.function_type == "Gather":
            weight_name = node.extra_input[0].name
            node.recover_layer_name = (
                '% = tf.raw_ops.Gather(params=$, indices=weight_data["{}"])'.format(
                    weight_name
                )
            )

        elif node.function_type == "StridedSlice":
            # Add implementation for strided slice op
            slice_string = ",".join(
                [
                    "{}:{}".format(
                        str(int(st)) if st != 0 else "", str(int(ed)) if ed != 0 else ""
                    )
                    for st, ed in zip(
                        node.function_param["begin"],
                        node.function_param["end"],
                    )
                ]
            )
            node.recover_layer_name = "% = $[{}]".format(slice_string)

        return code

    def generate_init_codes(self):
        node_set: List[Node] = self.graph.node_set
        for node in node_set:
            self.init_codes.extend(self.generate_initcode_from_single_node(node))

    def topology_sort(self) -> List[Node]:
        """Get the topology sort results for generating the code with a proper order

        :return: A ordered node list
        :rtype: List[Node]
        """
        node_list: List[Node] = self.graph.node_set
        in_degree = {}
        sorted_list = []
        for node in node_list:
            # if node.is_extra and not node.is_input:
            #     continue
            in_degree[node.name] = len(node.father_nodes)

        rest_list = []
        old_res_len = len(rest_list)
        while len(sorted_list) < len(node_list):
            rest_list = []
            for nodename in in_degree:
                # if in_degree[nodename] > 0 and all(
                #     [in_degree[x.name] < 0 for x in node.father_nodes]
                # ):
                #     in_degree[nodename] = 0

                if in_degree[nodename] > 0 and nodename not in rest_list:
                    rest_list.append(nodename)

            for nodename in in_degree:
                if in_degree[nodename] == 0:
                    # logger.debug("Removing Node[{}] ...".format(nodename))
                    tmp_node = self.graph.find_node_by_name(nodename)
                    if tmp_node is None:
                        continue
                    sorted_list.append(tmp_node)
                    in_degree[nodename] = -1
                    for nextnode in tmp_node.next_nodes:
                        if nextnode.name in in_degree:
                            # logger.debug("Reducing Indegree on Node[{}] ...".format(nextnode.name))
                            in_degree[nextnode.name] -= 1

                        if in_degree[nextnode.name] == -1:
                            logger.warning(
                                "{} indegree is reduced to -1".format(nextnode.name)
                            )

            # logger.debug(in_degree)
            if len(rest_list) == old_res_len:
                logger.error(self.graph.find_node_by_name("ADD-85") in sorted_list)
                logger.error("Deadlock!")
                logger.error(
                    [
                        (x.name, in_degree[x.name])
                        for x in (set(node_list) - set(sorted_list))
                    ]
                )
                print(rest_list)
                for tmp_node_name in rest_list:
                    tmp_node = self.graph.find_node_by_name(tmp_node_name)
                    if tmp_node is None:
                        logger.debug("{} Node is lost".format(tmp_node_name))
                        continue
                    logger.debug(
                        "[{}, {}]Father Nodes: {}".format(
                            tmp_node_name,
                            in_degree[tmp_node_name],
                            [
                                (fn.name, in_degree[fn.name])
                                for fn in tmp_node.father_nodes
                            ],
                        )
                    )

                exit(0)
            old_res_len = len(rest_list)
            # logger.debug("Rest List: {}".format(rest_list))

        return sorted_list

    def generate_forward_codes(self):
        node_arr: List[Node] = self.topology_sort()
        counter = 0
        input_counter = 0
        for node in node_arr:
            if node.is_extra and not node.is_input:
                continue

            if node.unknown:
                if len(node.father_nodes) > 0:
                    node.recover_output_name = node.father_nodes[0].recover_output_name
                    node.out_shape = node.father_nodes[0].out_shape
                continue
            if node.function_type == "BiasAdd":
                node.recover_output_name = node.father_nodes[0].recover_output_name
                node.out_shape = node.father_nodes[0].out_shape
                continue

            counter += 1
            # if len(node.father_nodes) == 0:
            #     self.forward_codes.append("x = tf.keras.Input(shape={})".format(tuple(node.in_shape[0])))
            #     self.forward_codes.append("x_{} = {}({})".format(counter, node.recover_layer_name, "x"))
            #     node.recover_output_name = "x_{}".format(counter)
            if node.is_input:
                # TODO: Change the shape
                shape = tuple(node.out_shape[1:])
                if node.out_shape[0] != 1:
                    shape = tuple(node.out_shape)
                self.forward_codes.append(
                    "input_{} = tf.keras.Input(shape={})".format(input_counter, shape)
                )
                if node.is_quant:
                    self.forward_codes.append(
                        "input_{} = {}*(input_{} - {})".format(
                            input_counter,
                            node.quant_params[1],
                            input_counter,
                            node.quant_params[0],
                        )
                    )

                node.recover_output_name = "input_{}".format(input_counter)
                input_counter += 1
            else:
                inputs = "".join(
                    [
                        tmp_node.recover_output_name + ", "
                        for tmp_node in node.father_nodes
                    ]
                )
                if node.recover_layer_name == "gbavgp2d_1":
                    logger.debug([tmp_node.name for tmp_node in node.father_nodes])

                # Hide some parameter input
                while inputs.endswith(", "):
                    inputs = inputs[:-2]
                while inputs.startswith(", "):
                    inputs = inputs[2:]

                if (
                    node.function_type
                    in ["Multiply", "Add", "Divide", "Subtract", "GreaterEqual", "Less"]
                    and "$" in node.recover_layer_name
                ):
                    codeline = node.recover_layer_name.replace(
                        "$", "x_{}".format(counter)
                    )
                    if len(node.extra_input) == 0:
                        codeline = codeline.replace("%1", inputs.split(", ")[0])
                        codeline = codeline.replace("%2", inputs.split(", ")[1])
                    elif (
                        len(node.extra_input) == 2
                        and sum([x.is_input for x in node.extra_input]) > 0
                    ):
                        codeline = codeline.replace("%1", inputs)
                        weight_name = node.extra_input[0].name
                        if node.extra_input[0].is_input:
                            weight_name = node.extra_input[1].name
                        codeline = codeline.replace(
                            "%2",
                            'tf.constant(weight_data["{}"], dtype=tf.float32)'.format(
                                weight_name
                            ),
                        )

                    else:
                        codeline = codeline.replace("%1", inputs)
                        codeline = codeline.replace(
                            "%2",
                            'tf.constant(weight_data["{}"], dtype=tf.float32)'.format(
                                node.extra_input[0].name
                            ),
                        )
                    self.forward_codes.append(codeline)
                    node.recover_output_name = "x_{}".format(counter)
                    continue

                if node.function_type in [
                    "Batch2Space",
                    "Space2Batch",
                    "Pad",
                    "Log",
                    "Exp",
                    "MirrorPad",
                    "Mean",
                    "Abs",
                    "Argmax",
                    "Sqrt",
                    "RSqrt",
                    "Negative",
                    "Square",
                    "Transpose",
                    "Gather",
                    "Unpack",
                    "Space2Depth",
                    "ReduceMax",
                    "LogicalNot",
                    "Sin",
                    "Cos",
                ]:
                    codeline = node.recover_layer_name.replace("$", inputs)
                    codeline = codeline.replace("%", "x_{}".format(counter))
                    self.forward_codes.append(codeline)
                    node.recover_output_name = "x_{}".format(counter)
                    continue
                if node.function_type == "SquaredDifference":
                    codeline = node.recover_layer_name.replace(
                        "$1", inputs.split(", ")[0]
                    )
                    codeline = codeline.replace("$2", inputs.split(", ")[1])
                    codeline = codeline.replace("%", "x_{}".format(counter))
                    self.forward_codes.append(codeline)
                    node.recover_output_name = "x_{}".format(counter)
                    continue
                if (
                    node.function_type == "Quantize"
                    or node.function_type == "Dequantize"
                ):
                    self.forward_codes.append("x_{} = {}".format(counter, inputs))
                    node.recover_output_name = "x_{}".format(counter)
                    # TODO: Need Fix
                    continue

                if node.function_type == "Split":
                    codeline = node.recover_layer_name.replace("$", inputs)
                    codeline = codeline.replace("%", "x_{}".format(counter))
                    self.forward_codes.append(codeline)
                    node.recover_output_name = "x_{}".format(counter)
                    # Insert pseudo-node
                    for idx, next_node in enumerate(node.next_nodes):
                        pseudo_node = Node("PseudoNode", 1, node.out_shape)
                        pseudo_node.recover_output_name = "{}[{}]".format(
                            node.recover_output_name, idx
                        )
                        for i, fn in enumerate(next_node.father_nodes):
                            if not fn.is_extra and fn.name == node.name:
                                next_node.father_nodes[i] = pseudo_node
                    continue

                if node.function_type == "StridedSlice":
                    codeline = node.recover_layer_name.replace("$", inputs)
                    codeline = codeline.replace("%", "x_{}".format(counter))
                    self.forward_codes.append(codeline)
                    node.recover_output_name = "x_{}".format(counter)

                    out_shape = tuple(list(node.out_shape))
                    if len(node.out_shape) > 0 and node.out_shape[0] == 1:
                        out_shape = tuple(list(node.out_shape[1:]))
                    # TODO: Fix the reshape!
                    self.forward_codes.append(
                        "{} = tf.keras.layers.Reshape({})({})".format(
                            node.recover_output_name,
                            out_shape,
                            node.recover_output_name,
                        )
                    )
                    continue
                # # Check if it need reshape
                # if len(node.father_nodes) - len(node.extra_input) == 1:
                #     father_node = (
                #         node.father_nodes[0]
                #         if not node.father_nodes[0].is_extra
                #         else node.father_nodes[1]
                #     )
                #     if len(father_node.out_shape) != len(node.in_shape[0]) and np.prod(
                #         father_node.out_shape
                #     ) == np.prod(node.in_shape[0]):
                #         self.forward_codes.append(
                #             "{} = tf.keras.layers.Reshape({})({})".format(
                #                 inputs, node.in_shape[0], inputs
                #             )
                #         )

                # if (
                #     node.function_type in ["Add", "Subtract", "Divide", "Multiply"]
                #     and len(node.extra_input) > 0
                # ):
                #     for e_node in node.extra_input:
                #         inputs += ', tf.constant(weight_data["{}"])'.format(e_node.name)

                if len(node.father_nodes) > 1 and node.function_type in [
                    "Add",
                    "Subtract",
                    "Concatenate",
                    "Average",
                    "Maximum",
                    "Minimum",
                    "Multiply",
                    "Dot",
                ]:
                    inputs = "[{}]".format(inputs)

                if node.function_type == "Pack":
                    codeline = node.recover_layer_name.replace(
                        "$", "[{}]".format(inputs)
                    )
                    codeline = codeline.replace("%", "x_{}".format(counter))
                    self.forward_codes.append(codeline)
                    node.recover_output_name = "x_{}".format(counter)
                    continue

                self.forward_codes.append(
                    "x_{} = {}({})".format(counter, node.recover_layer_name, inputs)
                )
                node.recover_output_name = "x_{}".format(counter)

        if input_counter == 1:
            self.return_code = self.return_code.format("input_0", counter)
        else:
            self.return_code = self.return_code.format(
                "[{}]".format(
                    ",".join(["input_{}".format(i) for i in range(input_counter)])
                ),
                counter,
            )


if __name__ == "__main__":
    graph = Graph()
    node_1 = Node("conv2d-1", 1, [1, 29, 198, 512])
    node_1.function_type = "Conv2D"
    node_1.function_param = {
        "filters": 16,
        "kernel_size": 3,
        "strides": [7, 1],
        "padding": "valid",
        "activation": "relu",
        "input_shape": [200, 200, 10],
        "dilation_rate": [1, 1],
        "paramsShape": [(3, 3, 10, 512), (1, 1, 1, 512)],
        "outputShape": [1, 29, 198, 512],
    }
    node_1.in_shape = [200, 200, 10]
    node_1.out_shape = [1, 29, 198, 512]
    node_2 = Node("conv2d-2", 1, [1, 27, 196, 32])
    node_2.function_type = "Conv2D"
    node_2.function_param = {
        "filters": 16,
        "kernel_size": 3,
        "strides": [1, 1],
        "padding": "same",
        "activation": "tanh",
        "input_shape": [1, 29, 198, 512],
        "dilation_rate": [1, 1],
        "paramsShape": [(3, 3, 10, 512), (1, 1, 1, 512)],
        "outputShape": [1, 29, 198, 32],
    }
    node_2.in_shape = [1, 29, 198, 512]
    node_2.out_shape = [1, 29, 198, 32]
    node_1.add_next_node(node_2)
    node_2.add_father_node(node_1)
    graph.add_node(node_1)
    graph.add_node(node_2)
    re_util = RecoverUtilKeras(graph)
    re_util.generate_init_codes()
    re_util.generate_forward_codes()
    logger.info("\n" + re_util.output_model_code())
