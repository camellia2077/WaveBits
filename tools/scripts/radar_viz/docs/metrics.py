# metrics.py
# 职责：从各模式的物理参数计算并归一化四个客观维度的分数
# 可独立运行 (python metrics.py)，结果同时打印到终端并导出至 calc_results.md
#
# 参考文档：
#   - temp/stability_reference.md  — 传输稳定维度计算规则

import json
import math  # noqa: F401 — 保留以备后续公式扩展
import os

# ==========================================
# [ 原始物理参数 ]
# 字段说明：
#   fs      : frame_samples 倍率（符号持续时间）
#   bps     : bits per symbol（物理层调制效率）
#   charset : 支持的有效字符数（满额 256）
#   silence : 静默重置加成（消除 ISI）
#   jitter  : 抖动惩罚（故意引入的颤音 / 变频）
#   dtmf    : 双音多频物理层加成
# ==========================================
RAW_DATA = {
    # Flash 模式 (基准 BFSK)
    "Steady":   {"fs": 0.9375, "bps": 1,    "charset": 256, "silence": 0, "jitter": 0, "dtmf": 0},
    "Hostile":  {"fs": 0.875,  "bps": 1,    "charset": 256, "silence": 0, "jitter": 0, "dtmf": 0},
    "Litany":   {"fs": 6.0,    "bps": 1,    "charset": 256, "silence": 2, "jitter": 0, "dtmf": 0},
    "Collapse": {"fs": 1.0,    "bps": 1,    "charset": 256, "silence": 0, "jitter": 5, "dtmf": 0},
    "Zeal":     {"fs": 0.625,  "bps": 1,    "charset": 256, "silence": 0, "jitter": 2, "dtmf": 0},  # 变速均值
    "Void":     {"fs": 2.5,    "bps": 1,    "charset": 256, "silence": 0, "jitter": 0, "dtmf": 0},
    # 古老协议
    "Mini":     {"fs": 5.0,    "bps": 0.25, "charset": 40,  "silence": 4, "jitter": 0, "dtmf": 0},  # Morse，低单符密度
    # 高阶协议
    "Pro":      {"fs": 1.5,    "bps": 4,    "charset": 128, "silence": 0, "jitter": 0, "dtmf": 6},  # DTMF 双频抗干扰
    "Ultra":    {"fs": 0.5,    "bps": 4,    "charset": 256, "silence": 0, "jitter": 0, "dtmf": 0},  # 16-FSK
}


def _calc_raw(data: dict) -> dict:
    """
    将原始物理参数映射为四个连续量：
      TRANSMISSION_SPEED      = bps / fs          (信息吞吐率)
      ENCODING_EFFICIENCY     = bps               (单符密度)
      TRANSMISSION_STABILITY  = fs + silence + dtmf - jitter   (物理层鲁棒性)
      COMPATIBILITY           = 1 - charset/256   (字符集限制度，值越高越闭塞，此逻辑仅供归一化参考)
    """
    results = {}
    for mode, d in data.items():
        results[mode] = {
            "TRANSMISSION_SPEED":     d["bps"] / d["fs"],
            "ENCODING_EFFICIENCY":    d["bps"],
            "TRANSMISSION_STABILITY": d["fs"] + d["silence"] + d["dtmf"] - d["jitter"],
            "COMPATIBILITY":          1.0 - (d["charset"] / 256.0),
        }
    return results


def _min_max_normalize(raw_results: dict, metric_key: str) -> dict:
    """Min-Max 归一化，映射到 0~10 分制。"""
    vals = [r[metric_key] for r in raw_results.values()]
    min_v, max_v = min(vals), max(vals)
    if max_v == min_v:
        return {m: 5.0 for m in raw_results}
    return {
        m: round(10.0 * (r[metric_key] - min_v) / (max_v - min_v), 1)
        for m, r in raw_results.items()
    }


def compute_scores(raw_data: dict = None) -> dict:
    """
    对外接口：输入原始物理参数，返回各模式的归一化分数字典。
    返回格式：{mode: {全名: score, ...}}
    """
    if raw_data is None:
        raw_data = RAW_DATA

    raw_results = _calc_raw(raw_data)

    mapping = {
        "TRANSMISSION_SPEED":     "传输速度",
        "ENCODING_EFFICIENCY":    "编码效率",
        "TRANSMISSION_STABILITY": "传输稳定",
        "COMPATIBILITY":          "兼容性",
    }

    final_scores = {key: _min_max_normalize(raw_results, key) for key in mapping}

    return {
        mode: {mapping[key]: final_scores[key][mode] for key in mapping}
        for mode in raw_data
    }


def export_to_markdown(scores: dict, md_path: str):
    """将分数字典以 JSON 代码块格式导出到 Markdown 文件。"""
    json_output = json.dumps(scores, indent=4, ensure_ascii=False)
    with open(md_path, "w", encoding="utf-8") as f:
        f.write("```json\n")
        f.write(json_output)
        f.write("\n```\n")
    return json_output


# ==========================================
# 独立运行入口
# ==========================================
if __name__ == "__main__":
    scores = compute_scores()
    json_str = json.dumps(scores, indent=4, ensure_ascii=False)

    print("=" * 50)
    print("计算结果 (JSON 格式):")
    print(json_str)
    print("=" * 50)

    md_path = os.path.join(os.path.dirname(__file__), "calc_results.md")
    export_to_markdown(scores, md_path)
    print(f"\n[Exported] -> {md_path}")
