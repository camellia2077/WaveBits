import math
import json
import os

# 核心物理参数录入 (基于 WaveBits 设计文档)
# 字段说明:
# - fs: frame_samples 倍率 (衡量单个符号持续时间)
# - bps: bit per symbol (物理层调制效率)
# - charset: 支持的有效字符数量 (满额256)
# - silence: 静默重置加成 (消除符号间干扰ISI)
# - jitter: 抖动惩罚 (故意引入的颤音或变频)
# - dtmf: 双音多频物理层加成

raw_data = {
    # --- Flash 模式 (基准 BFSK) ---
    "Steady":   {"fs": 0.9375, "bps": 1,    "charset": 256, "silence": 0, "jitter": 0, "dtmf": 0},
    "Hostile":  {"fs": 0.875,  "bps": 1,    "charset": 256, "silence": 0, "jitter": 0, "dtmf": 0},
    "Litany":   {"fs": 6.0,    "bps": 1,    "charset": 256, "silence": 2, "jitter": 0, "dtmf": 0},
    "Collapse": {"fs": 1.0,    "bps": 1,    "charset": 256, "silence": 0, "jitter": 5, "dtmf": 0},
    "Zeal":     {"fs": 0.625,  "bps": 1,    "charset": 256, "silence": 0, "jitter": 2, "dtmf": 0}, # 变速平均值
    "Void":     {"fs": 2.5,    "bps": 1,    "charset": 256, "silence": 0, "jitter": 0, "dtmf": 0},
    
    # --- 古老协议 ---
    "Mini":     {"fs": 5.0,    "bps": 0.25, "charset": 40,  "silence": 4, "jitter": 0, "dtmf": 0}, # 摩斯电码
    
    # --- 高阶协议 ---
    "Pro":      {"fs": 1.5,    "bps": 4,    "charset": 128, "silence": 0, "jitter": 0, "dtmf": 6}, # DTMF
    "Ultra":    {"fs": 0.5,    "bps": 4,    "charset": 256, "silence": 0, "jitter": 0, "dtmf": 0}, # 16-FSK
}

results = {}

# 1. 计算绝对值
for mode, d in raw_data.items():
    # 传输速度
    speed = d["bps"] / d["fs"]
    
    # 编码效率
    efficiency = d["bps"]
    
    # 传输稳定
    stability = d["fs"] + d["silence"] + d["dtmf"] - d["jitter"]
    
    # 兼容性
    compatibility = 1.0 - (d["charset"] / 256.0)
    
    results[mode] = {
        "TRANSMISSION_SPEED": speed,
        "ENCODING_EFFICIENCY": efficiency,
        "TRANSMISSION_STABILITY": stability,
        "COMPATIBILITY": compatibility
    }

# 2. Min-Max 归一化 (0~10分制)
def normalize(metric_key):
    vals = [r[metric_key] for r in results.values()]
    min_v, max_v = min(vals), max(vals)
    if max_v == min_v:
        return {m: 5.0 for m in results}
    
    norm_res = {}
    for m, r in results.items():
        score = 10.0 * (r[metric_key] - min_v) / (max_v - min_v)
        norm_res[m] = round(score, 1)
    return norm_res

final_scores = {
    "TRANSMISSION_SPEED":     normalize("TRANSMISSION_SPEED"),
    "ENCODING_EFFICIENCY":    normalize("ENCODING_EFFICIENCY"),
    "TRANSMISSION_STABILITY": normalize("TRANSMISSION_STABILITY"),
    "COMPATIBILITY":          normalize("COMPATIBILITY"),
}

# 3. 构建全名 JSON 数据
mapping = {
    "TRANSMISSION_SPEED":     "传输速度",
    "ENCODING_EFFICIENCY":    "编码效率",
    "TRANSMISSION_STABILITY": "传输稳定",
    "COMPATIBILITY":          "兼容性",
}

ai_data = {}
for mode in raw_data.keys():
    ai_data[mode] = {
        mapping[key]: final_scores[key][mode] for key in mapping
    }

json_output = json.dumps(ai_data, indent=4, ensure_ascii=False)

# 打印到终端
print("="*50)
print("计算结果 (JSON 格式):")
print(json_output)
print("="*50)

# 写入 Markdown 结果文件
md_path = os.path.join(os.path.dirname(__file__), "calc_results.md")
with open(md_path, "w", encoding="utf-8") as f:
    f.write("```json\n")
    f.write(json_output)
    f.write("\n```\n")

print(f"\n[Exported] -> {md_path}")
