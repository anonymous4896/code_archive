from src.advtool.WhiteBoxAttacks.config import AdvAttackConfig
from .WhiteBoxAttacks.PGD.attack import pgd_attack

ATTACK_DICT = {"pgd": pgd_attack}


class AdvTool:
    def __init__(self) -> None:
        pass

    @staticmethod
    def getWhiteBoxAttack(attack_name, config: AdvAttackConfig):
        attack_name = attack_name.lower()
        assert attack_name in ATTACK_DICT
        return ATTACK_DICT[attack_name](config)


if __name__ == "__main__":
    pgd = AdvTool.getWhiteBoxAttack("pgd")
