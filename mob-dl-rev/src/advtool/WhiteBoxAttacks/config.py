import numpy as np


class AdvAttackConfig:
    def __init__(self) -> None:
        self.eps = 1.0 / 255.0
        self.nb_iter = 10
        self.target = None
        self.norm = np.inf
        self.clip_range = -1, 1
        self.loss_fn = None
