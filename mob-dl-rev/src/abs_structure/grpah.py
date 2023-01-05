from typing import List

# from loguru import logger

from .node import Node


class Graph:
    def __init__(self) -> None:
        self.root = Node("root", 0, None)
        self.node_set: List[Node] = []
        self.node_dict = {}
        self.nx_graph = None
        pass

    def add_node(self, node: Node):
        if len(self.node_set) == 0:
            self.root.next_nodes = node

        self.node_set.append(node)
        self.node_dict[node.name] = node
        return self

    def find_node_by_name(self, name) -> Node:
        if name in self.node_dict:
            return self.node_dict[name]
        return None

    def remove_useless_extra_nodes(self):
        useless_nodes = []
        new_nodes = []
        for node in self.node_set:
            if not node.is_extra:
                continue
            # check hidden relu6
            raw_node_name = node.name.split("-")[-2]
            if (
                raw_node_name.endswith("relu6")
                or raw_node_name.endswith("Relu6")
                or (
                    "efficientnet-lite2" in node.name
                    and "Relu6" in node.name
                    and ";" in node.name
                )
            ):
                useless_nodes.append(node)
                # Temporally remove the node
                # Since we set the is_extra to False
                # The node will append its shape to input shape
                for next_node in node.next_nodes:
                    next_node.father_nodes.remove(node)
                    next_node.extra_input.remove(node)
                    # next_node.add_father_node(node)

                for father_node in node.father_nodes:
                    father_node.next_nodes.remove(node)
                    # father_node.add_next_node(node)
                node.function_param["a_max"] = "6f"
                node.is_extra = False
                old_name = node.name
                node.name = "RELU6-{}".format(old_name.split("-")[-1])
                for next_node in node.next_nodes:
                    next_node.add_father_node(node)

                for father_node in node.father_nodes:
                    father_node.add_next_node(node)

                new_nodes.append(node)
                continue
            elif (
                raw_node_name.split("_")[0].endswith("Relu")
                or (
                    raw_node_name.startswith("activation")
                    and raw_node_name.endswith("Relu")
                )
                or (
                    len(raw_node_name.split("_")) >= 2
                    and raw_node_name.split("_")[-1].endswith("relu/Relu")
                )
                or (
                    len(raw_node_name.split("_")) >= 2
                    and raw_node_name.split("_")[-1].endswith("/Relu")
                    and raw_node_name.startswith("decoder")
                )
            ):
                useless_nodes.append(node)
                for next_node in node.next_nodes:
                    next_node.father_nodes.remove(node)
                    next_node.extra_input.remove(node)

                for father_node in node.father_nodes:
                    father_node.next_nodes.remove(node)

                node.is_extra = False
                old_name = node.name
                node.name = "RELU-{}".format(old_name.split("-")[-1])
                for next_node in node.next_nodes:
                    next_node.add_father_node(node)

                for father_node in node.father_nodes:
                    father_node.add_next_node(node)

                new_nodes.append(node)
                continue

            if (
                len(node.father_nodes) == 1
                and len(node.next_nodes) >= 1
                and not node.father_nodes[0].is_extra
                and sum([x.is_extra for x in node.next_nodes]) == 0
            ):
                father_node_name = node.father_nodes[0].name
                father_node = self.find_node_by_name(father_node_name)
                if node in father_node.next_nodes:
                    father_node.next_nodes.remove(node)

                for next_node in node.next_nodes:
                    father_node.add_next_node(next_node)
                    next_node.father_nodes.remove(node)
                    next_node.add_father_node(father_node)
                    next_node.extra_input.remove(node)
                useless_nodes.append(node)
                # Set the quantization param
                # if node.is_quant:
                #     father_node.is_quant = True
                #     father_node.quant_params = node.quant_params

        for node in useless_nodes:
            if node in self.node_set:
                self.node_set.remove(node)
            if node.name in self.node_dict:
                del self.node_dict[node.name]

        for node in new_nodes:
            self.node_set.append(node)
            self.node_dict[node.name] = node

    def remove_quant_ops(self):
        quant_nodes = []
        for node in self.node_set:
            if node.is_extra or (
                node.function_type != "Quantize" and node.function_type != "Dequantize"
            ):
                continue
            # logger.debug("Removing Quant Node[{}] ...".format(node.name))
            # logger.debug([x.name for x in node.father_nodes])
            # logger.debug([x.name for x in node.next_nodes])

            if len(node.father_nodes) == 1 and len(node.next_nodes) >= 1:
                father_node_name = node.father_nodes[0].name
                father_node = self.find_node_by_name(father_node_name)
                # Remove link
                father_node.next_nodes.remove(node)
                for next_node in node.next_nodes:
                    # next_node = node.next_nodes[0]
                    next_node.father_nodes.remove(node)
                    # Add new link
                    father_node.add_next_node(next_node)
                    next_node.add_father_node(father_node)
                    # if len(next_node.next_nodes) > 0:
                    #     father_node.add_next_node(next_node.next_nodes[0])
                    #     next_node.add_father_node(father_node)
                    #     quant_nodes.append(node.next_nodes[0])
                quant_nodes.append(node)
            elif len(node.father_nodes) == 1 and len(node.next_nodes) == 0:
                father_node_name = node.father_nodes[0].name
                father_node = self.find_node_by_name(father_node_name)
                father_node.next_nodes.remove(node)
                quant_nodes.append(node)

        for node in quant_nodes:
            if node in self.node_set:
                self.node_set.remove(node)
            if node.name in self.node_dict:
                del self.node_dict[node.name]
