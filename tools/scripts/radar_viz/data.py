# data.py
# 职责：定义所有模式的雷达图量化数据集 (DATA_DICT)
# 依赖：config.py 中的维度索引常量

from config import (
    TRANSMISSION_SPEED, ENCODING_EFFICIENCY, TRANSMISSION_STABILITY,
    COMPATIBILITY, PARSING_DIFFICULTY, ELECTRONIC_FEEL,
    EMOTIONAL_RICHNESS, SENSE_OF_RITUAL
)


def build_metrics(
    transmission_speed=1,
    encoding_efficiency=1,
    transmission_stability=1,
    compatibility=1,
    parsing_difficulty=1,
    electronic_feel=1,
    emotional_richness=1,
    sense_of_ritual=1
):
    """
    构建 8 维数据数组，字段顺序与 LABELS_EN / LABELS_ZH 严格对齐。
    顺序：TRANSMISSION_SPEED, ENCODING_EFFICIENCY, TRANSMISSION_STABILITY,
         COMPATIBILITY, PARSING_DIFFICULTY, ELECTRONIC_FEEL,
         EMOTIONAL_RICHNESS, SENSE_OF_RITUAL
    """
    return [
        transmission_speed,
        encoding_efficiency,
        transmission_stability,
        compatibility,
        parsing_difficulty,
        electronic_feel,
        emotional_richness,
        sense_of_ritual
    ]


# ==========================================
# [ 圣典模式数据定义 ]
# ==========================================
DATA_DICT = {
    # 项目作者
    "The Author": {
        "name_en": "Project Creator",
        "name_zh": "项目作者",
        "vals": build_metrics(
            transmission_speed=1, encoding_efficiency=3, transmission_stability=2,
            compatibility=2, parsing_difficulty=0, electronic_feel=0,
            emotional_richness=6, sense_of_ritual=2
        ),
    },

    # Flash BFSK 模式集 (物理极限 10)
    "Steady": {
        "name_en": "Steady",
        "name_zh": "普通",
        "vals": build_metrics(
            transmission_speed=2, encoding_efficiency=3, transmission_stability=6,
            compatibility=1, parsing_difficulty=4, electronic_feel=5,
            emotional_richness=4, sense_of_ritual=5
        ),
    },
    "Hostile": {
        "name_en": "Hostile",
        "name_zh": "敌意",
        "vals": build_metrics(
            transmission_speed=3, encoding_efficiency=3, transmission_stability=6,
            compatibility=1, parsing_difficulty=8, electronic_feel=8,
            emotional_richness=7, sense_of_ritual=2
        ),
    },
    "Litany": {
        "name_en": "Litany",
        "name_zh": "热忱",
        "vals": build_metrics(
            transmission_speed=1, encoding_efficiency=3, transmission_stability=10,
            compatibility=1, parsing_difficulty=1, electronic_feel=9,
            emotional_richness=8, sense_of_ritual=10
        ),
    },
    "Collapse": {
        "name_en": "Collapse",
        "name_zh": "崩溃",
        "vals": build_metrics(
            transmission_speed=2, encoding_efficiency=3, transmission_stability=1,
            compatibility=1, parsing_difficulty=7, electronic_feel=8,
            emotional_richness=6, sense_of_ritual=4
        ),
    },
    "Zeal": {
        "name_en": "Zeal",
        "name_zh": "狂热",
        "vals": build_metrics(
            transmission_speed=4, encoding_efficiency=3, transmission_stability=3,
            compatibility=1, parsing_difficulty=10, electronic_feel=10,
            emotional_richness=8, sense_of_ritual=7
        ),
    },
    "Void": {
        "name_en": "Void",
        "name_zh": "虚空",
        "vals": build_metrics(
            transmission_speed=1, encoding_efficiency=3, transmission_stability=8,
            compatibility=1, parsing_difficulty=2, electronic_feel=9,
            emotional_richness=5, sense_of_ritual=9
        ),
    },

    # 高阶协议 (数值呈现碾压态势，最高限 16)
    "Pro": {
        "name_en": "Pro",
        "name_zh": "Pro",
        "vals": build_metrics(
            transmission_speed=5, encoding_efficiency=16, transmission_stability=14,
            compatibility=10, parsing_difficulty=9, electronic_feel=16,
            emotional_richness=2, sense_of_ritual=10
        ),
    },
    "Ultra": {
        "name_en": "Ultra",
        "name_zh": "Ultra",
        "vals": build_metrics(
            transmission_speed=16, encoding_efficiency=16, transmission_stability=6,
            compatibility=1, parsing_difficulty=15, electronic_feel=16,
            emotional_richness=1, sense_of_ritual=5
        ),
    },

    # Mini Morse — 三种速度档位，各自独立出图
    "Mini Slow": {
        "name_en": "Mini Slow",
        "name_zh": "Mini Slow",
        "vals": build_metrics(
            transmission_speed=1, encoding_efficiency=1, transmission_stability=16,
            compatibility=16, parsing_difficulty=2, electronic_feel=12,
            emotional_richness=1, sense_of_ritual=16
        ),
    },
    "Mini Standard": {
        "name_en": "Mini Standard",
        "name_zh": "Mini Standard",
        "vals": build_metrics(
            transmission_speed=2, encoding_efficiency=1, transmission_stability=14,
            compatibility=16, parsing_difficulty=4, electronic_feel=15,
            emotional_richness=1, sense_of_ritual=16
        ),
    },
    "Mini Fast": {
        "name_en": "Mini Fast",
        "name_zh": "Mini Fast",
        "vals": build_metrics(
            transmission_speed=3, encoding_efficiency=1, transmission_stability=12,
            compatibility=16, parsing_difficulty=7, electronic_feel=16,
            emotional_richness=1, sense_of_ritual=16
        ),
    },
}

# ==========================================
# [ 数字别名映射 ] — 用于 CLI 快速输入
# ==========================================
ALIASES: dict[int, str] = {i: key for i, key in enumerate(DATA_DICT)}


def resolve_mode(token: str) -> str:
    """
    将用户输入（数字或名字）解析为 DATA_DICT 中的合法 key。
    - 输入数字字符串 → 查 ALIASES
    - 输入名字字符串 → 精确匹配（大小写不敏感）
    抛出 KeyError 表示无法识别。
    """
    if token.isdigit():
        idx = int(token)
        if idx not in ALIASES:
            raise KeyError(f"别名 [{idx}] 不存在，有效范围 0~{len(ALIASES)-1}")
        return ALIASES[idx]

    # 名字匹配（大小写不敏感）
    token_lower = token.lower()
    for key in DATA_DICT:
        if key.lower() == token_lower:
            return key

    raise KeyError(f"模式 \"{token}\" 不存在，运行 --list 查看全部可用模式")
