# config.py
# 职责：全局常量——维度索引、标签、描述文本、视觉颜色方案
# 不含任何业务逻辑或数据集；数据集见 data.py

import numpy as np

# ==========================================
# [ 维度常量映射 ] - 8 bits = 1 byte
# ==========================================
# 布局逻辑：左脑机理 vs 右脑感知 (Rational Left vs Emotional Right)
# 设计意图：从大众传播的文化符号出发，"左脑理性、右脑感性"能让观众以极低的理解成本
#           get 到雷达图的对抗张力。
# 左半球 (Machine/Rational): 负责客观的数据量化
# 右半球 (Spirit/Emotional): 负责主观的听感与叙事评价

TRANSMISSION_SPEED      = 0  # 传输速度 (Left-Top)
ENCODING_EFFICIENCY     = 1  # 编码效率 (Left-Mid)
TRANSMISSION_STABILITY  = 2  # 传输稳定 (Left-Mid)
COMPATIBILITY           = 3  # 兼容性 (Left-Bottom)
PARSING_DIFFICULTY      = 4  # 解析难度 (Right-Bottom)
ELECTRONIC_FEEL         = 5  # 电子感 (Right-Mid)
EMOTIONAL_RICHNESS      = 6  # 情感丰富度 (Right-Mid)
SENSE_OF_RITUAL         = 7  # 仪式感 (Right-Top)

# ==========================================
# [ 维度标题定义 ]
# ==========================================
LABELS_EN = np.array([
    'Transmission Speed',
    'Encoding Efficiency',
    'Transmission Stability',
    'Compatibility',
    'Parsing Difficulty',
    'Electronic Feel',
    'Emotional Richness',
    'Sense of Ritual'
])

LABELS_ZH = np.array([
    '传输速度',
    '编码效率',
    '传输稳定',
    '兼容性',
    '解析难度',
    '电子感',
    '情感丰富度',
    '仪式感'
])

DESC_EN = {
    'Transmission Speed': 'Throughput & Physical Layer Baud Rate',
    'Encoding Efficiency': 'Effective Bits per Symbol',
    'Transmission Stability': 'Signal Continuity, Robustness & Jitter Resistance',
    'Compatibility': 'Strictness of Charset & Payload Constraints',
    'Parsing Difficulty': 'Human or Machine Decoding Complexity',
    'Electronic Feel': 'Degree of Digitalization & Mechanical Variance',
    'Emotional Richness': 'Emotional Depth & Spectral Expression',
    'Sense of Ritual': 'Structural Rhythm, Cadence & Formatting'
}

DESC_ZH = {
    '传输速度': '吞吐量与物理层波特率',
    '编码效率': '单位符号携带的有效比特数',
    '传输稳定': '信号连续性、抗干扰能力与无抖动表现',
    '兼容性': '对字符集的严格限制与载荷挑剔程度',
    '解析难度': '人类或机器解析难度',
    '电子感': '电子感与偏离人类表达的程度',
    '情感丰富度': '情感丰富度',
    '仪式感': '结构规整度、格式顿挫与强制停顿间隔'
}

# ==========================================
# [ 视觉风格配置 ]
# ==========================================
PRIMARY_COLOR   = "#E8E2D0"   # 背景底色 (米白)
SECONDARY_COLOR = "#9E1B1B"   # 属性与网格颜色 (深红)
OUTLINE_COLOR   = "#C5A059"   # 描边颜色 (黄铜)
TEXT_COLOR   = "#2E2E2E"   # 文字颜色

# ==========================================
# [ 导出尺寸配置 ]
# ==========================================
# 1. 雷达图本体
FIG_SIZE_RADAR   = (12, 12)    # 画布尺寸 (英寸)
EXPORT_DPI_RADAR = 300         # 导出分辨率

# 2. 图例说明书
FIG_SIZE_LEGEND   = (14, 10)   # 画布尺寸 (英寸)
EXPORT_DPI_LEGEND = 300        # 导出分辨率