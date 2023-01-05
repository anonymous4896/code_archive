from src.advtool.WhiteBoxAttacks.config import AdvAttackConfig

ATTACK_DICT = {}


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
