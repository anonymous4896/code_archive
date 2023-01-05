from loguru import logger
from src.abs_structure.node import Node


class TypeMappingUtil:
    def __init__(self) -> None:
        pass

    @staticmethod
    def map_type(node: Node, tflite_type: str) -> str:
        """Map the tflite type to our operator type

        :param tflite_type: The operator type name in tflite
        :type tflite_type: str
        :return: Our operator type
        :rtype: str
        """
        type_map_dict = {
            "CONV_2D": "Conv2D",
            "RELU": "ReLU",
            "DEPTHWISE_CONV_2D": "DepthwiseConv2D",
            "LOGISTIC": "Logistic",
            "STRIDED_SLICE": "StridedSlice",
            "ADD": "Add",
            "CONCATENATION": "Concatenate",
            "RESHAPE": "Reshape",
            "MAX_POOL_2D": "MaxPooling2D",
            "AVERAGE_POOL_2D": "AveragePooling2D",
            "MUL": "Multiply",
            "RESIZE_NEAREST_NEIGHBOR": "Resize_nn",
            "RESIZE_BILINEAR": "Resize_bi",
            "DEQUANTIZE": "Dequantize",
            "QUANTIZE": "Quantize",
            "BATCH_TO_SPACE_ND": "Batch2Space",
            "SPACE_TO_BATCH_ND": "Space2Batch",
            "TFLite_Detection_PostProcess": "TFLiteDetectionPostProcess",
            "FULLY_CONNECTED": "Dense",
            "TRANSPOSE_CONV": "Conv2DTranspose",
            "DIV": "Divide",
            "SOFTMAX": "Softmax",
            "AVERAGE_POOL_2D": "AveragePooling2D",
            "MAX_POOL_2D": "MaxPooling2D",
            "SUB": "Subtract",
            "SPLIT": "Split",
            "PAD": "Pad",
            "PRELU": "PReLU",
            "LOG": "Log",
            "TANH": "Tanh",
            "MIRROR_PAD": "MirrorPad",
            "MAXIMUM": "Maximum",
            "MINIMUM": "Minimum",
            "MEAN": "Mean",
            "ABS": "Abs",
            "ARG_MAX": "Argmax",
            "SQRT": "Sqrt",
            "RSQRT": "RSqrt",
            "NEG": "Negative",
            "SQUARE": "Square",
            "SQUARED_DIFFERENCE": "SquaredDifference",
            "GREATER_EQUAL": "GreaterEqual",
            "TRANSPOSE": "Transpose",
            "GATHER": "Gather",
            "PACK": "Pack",
            "UNPACK": "Unpack",
            "SPACE_TO_DEPTH": "Space2Depth",
            "LEAKY_RELU": "LeakyReLU",
            "EXP": "Exp",
            "RELU6": "ReLU",
            "REDUCE_MAX": "ReduceMax",
            "LESS": "Less",
            "LOGICAL_NOT": "LogicalNot",
            "SIN": "Sin",
            "COS": "Cos",
            # To Implement Operator
            "SLICE": "Slice",  # We ignore that op since it just appears in a model with TextEncoder3 operator
            "SUM": "Sum",  # We ignore that op since it just appears in a model with TextEncoder3 operator
            "DistanceDiversification": "DistanceDiversification",  # We ignore that op since it just appears in a model with TextEncoder3 operator
            "L2_NORMALIZATION": "",  # We ignore that op since it just appears in a model with TextEncoder3 operator
            "TOPK_V2": "TopkV2",  # We ignore that op since it just appears in a model with TextEncoder3 operator
            "CAST": "Cast",  # We do not need to cast type?
        }
        res_type = "NoType"
        # if tflite_type == "SPACE_TO_DEPTH":
        #     logger.warning(node.in_shape)
        #     logger.warning(node.out_shape)
        #     logger.warning([x.out_shape for x in node.father_nodes])
        #     logger.warning([x.is_extra for x in node.father_nodes])
        #     logger.warning([x.name for x in node.father_nodes])
        if tflite_type in type_map_dict:
            res_type = type_map_dict[tflite_type]
        else:
            logger.warning("No Implement OP: {}".format(tflite_type))
        if res_type == "ZeroPadding2D" and len(node.in_shape[0]) == 5:
            res_type = "ZeroPadding3D"

        return res_type
