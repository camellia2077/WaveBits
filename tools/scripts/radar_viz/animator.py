# animator.py
# 职责：生成两个雷达图模式之间的过渡动画（PNG 序列帧，透明背景）
#
# 设计说明：
#   - 动画本质是对 8 维数值做逐帧插值，用 ease-in-out 曲线控制速度节奏
#   - 输出为 PNG 序列帧（透明背景），直接导入 Premiere / DaVinci / FCPX 使用
#   - 输出为 PNG 序列帧（透明背景），直接导入 Premiere / DaVinci / FCPX 使用
#   - 动画帧本身只渲染纯几何形状（无标签），保持视觉干净，文字可在 NLE 中叠加

import os
import numpy as np
import matplotlib
matplotlib.use('Agg')   # 无头渲染，避免弹出窗口
import matplotlib.pyplot as plt

from radar import RadarChart

# ==========================================
# [ 插值工具 ]
# ==========================================

def _ease_in_out(t: float) -> float:
    """三次 ease-in-out 曲线：两头慢、中间快。t ∈ [0, 1]"""
    return t * t * (3.0 - 2.0 * t)


def _get_anim_vals(mode_data: dict) -> np.ndarray:
    """
    从模式数据中提取用于动画的单层代表值（浮点数组）。
    返回 (start_vals, end_vals) 元组，两者相同（单层模式）。
    """
    v = np.array(mode_data["vals"], dtype=float)
    return v, v


def _build_frame_sequence(
    start_vals: np.ndarray,
    end_vals:   np.ndarray,
    frames:     int
) -> list[np.ndarray]:
    """
    生成插值帧序列（含 ease-in-out）。
    返回长度为 frames 的列表，每项为 8 维 float 数组。
    """
    sequence = []
    for i in range(frames):
        t = i / max(frames - 1, 1)
        t_e = _ease_in_out(t)
        sequence.append(start_vals + t_e * (end_vals - start_vals))
    return sequence


# ==========================================
# [ 渲染器 ]
# ==========================================

def _render_frame(vals: np.ndarray, out_path: str, dpi: int = 150):
    """渲染单帧：装甲 + 网格 + 数据多边形（无标签），保存透明 PNG。"""
    chart = RadarChart(vals)
    chart.draw_armor()
    chart.draw_grids()
    chart.draw_data(list(vals))

    chart.fig.savefig(out_path, facecolor='none', transparent=True, dpi=dpi)
    chart.close()


# ==========================================
# [ 主接口 ]
# ==========================================

def generate_transition(
    start_key:  str,
    end_key:    str,
    data_dict:  dict,
    lang:       str  = 'en',
    frames:     int  = 60,
    dpi:        int  = 150,
    script_dir: str  = None,
) -> str:
    """
    生成从 start_key 到 end_key 的过渡动画（PNG 序列帧）。

    参数
    ----
    start_key  : 起始模式名（DATA_DICT 中的 key）
    end_key    : 终止模式名
    data_dict  : 完整数据集（data.DATA_DICT）
    lang       : 'en' 或 'zh'（影响输出子目录路径）
    frames     : 总帧数（默认 60）
    dpi        : 输出分辨率（默认 150，动画够用；静态图用 300）
    script_dir : 输出根目录；None 时使用本文件所在目录

    返回
    ----
    str: 帧序列所在目录的绝对路径
    """
    if script_dir is None:
        here = os.path.dirname(os.path.abspath(__file__))
        script_dir = os.path.normpath(os.path.join(here, "..", "..", "..", "temp"))

    start_data = data_dict[start_key]
    end_data   = data_dict[end_key]

    # --- 解析起点终点值 ---
    start_vals, _ = _get_anim_vals(start_data)
    end_vals,   _ = _get_anim_vals(end_data)

    # 直接插值
    seq = _build_frame_sequence(start_vals, end_vals, frames)

    # --- 输出目录 ---
    safe_start = start_key.replace(" ", "_").replace("(", "").replace(")", "")
    safe_end   = end_key.replace(" ", "_").replace("(", "").replace(")", "")
    anim_dir   = os.path.join(script_dir, lang, "animations",
                              f"{safe_start}_to_{safe_end}")
    os.makedirs(anim_dir, exist_ok=True)

    # --- 渲染帧 ---
    from tqdm import tqdm
    total = len(seq)
    print(f"[Animate ({lang.upper()})] {start_key} → {end_key} ({total} frames @ {dpi}dpi)")

    for i, frame_vals in enumerate(tqdm(seq, desc="  Rendering", leave=False)):
        out_path = os.path.join(anim_dir, f"frame_{i:04d}.png")
        _render_frame(frame_vals, out_path, dpi=dpi)

    print(f"  Saved → {anim_dir}")
    return anim_dir
