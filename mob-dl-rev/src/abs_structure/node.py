from typing import List


class Node:

    WEIGHT_NUM_WRONG = 1
    INPUT_NUM_WRONG = 2
    INPUT_DIM_WRONG = 3
    OUTPUT_NUM_WRONG = 4
    WEIGHT_DIM_WRONG = 5
    OUTPUT_DIM_WRONG = 6
    SHAPE_CHANGE_WRONG = 7

    def __init__(self, name, input_num, out_shape, param_type="float32"):
        self.name = name  # The node name in the computation graph (from json file)
        self.next_nodes: List[self] = []  # Child nodes
        self.father_nodes: List[self] = []  # Father nodes
        self.out_shape = out_shape  # The output shape
        self.in_shape = []  # Input shape list [could be multi inputs]
        self.param_type = (
            param_type  # The weight type, it could be float, int, long, ...
        )
        self.input_num = input_num  # The number of input nodes
        self.output_num = 1  # Always 1 output
        self.possible_components = []
        self.extra_input: List[Node] = []  # Could the weight nodes or input nodes
        self.function_param = {}  # The attribute of each function
        self.function_type = "Unknown"  # The function type (PyTorch or Keras)
        self.function_weight = []  # Weights
        self.recover_layer_name = ""  # During the recovering process, you have to assign a variable name to each layer
        self.has_activation = False
        self.activation_name = ""
        self.recover_code = (
            ""  # For some special operator, we need a flexible code template
        )
        self.recover_output_name = ""  # The name of output variable
        self.unknown = False  # For some unknown operators
        self.is_extra = False  # Input/Weight nodes
        self.is_input = False  # Input node flag
        self.atomic_type = "unknown"  # Atomic type after the deep learning model
        self.has_wrong_pred = (
            False  # If we obtain a wrong prediction, we set that flag as False
        )
        self.wrong_pred_category = -1  # The wrong pred flag
        self.attrs = {}
        self.dfs_skip = False
        self.bb_data = None  # basic block instruction data
        self.input_tensor_node_index_arr = []
        self.output_tensor_node_index_arr = []

        self.is_quant = False
        self.quant_params = [0, 0]

    def add_next_node(self, next_node):
        """Add directional link for node and next_node

        :param next_node: The operator node after this node
        :type next_node: Node
        :return: Self
        :rtype: Node
        """
        self.next_nodes.append(next_node)
        return self

    def add_father_node(self, father_node):
        self.father_nodes.append(father_node)
        if not father_node.is_extra or father_node.is_input:
            self.in_shape.append(father_node.out_shape)
        if father_node.is_extra:
            self.extra_input.append(father_node)

        return self

    def add_extra_node(self, node):
        self.extra_input.append(node)
        if node[-1] == 1:
            self.in_shape.append(node[1])
