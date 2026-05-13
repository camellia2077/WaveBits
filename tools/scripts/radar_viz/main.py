# main.py
# 职责：程序入口——记录耗时、调用 cli.run()
#
# 模块职责一览：
#   config.py       — 维度常量、标签、颜色方案
#   config_fonts.py — 字体路径与字号配置
#   data.py         — DATA_DICT 数据集 + ALIASES 数字别名 + resolve_mode()
#   radar.py        — RadarChart 类（坐标系、装甲、网格、多边形、标签）
#   animator.py     — 过渡动画生成（PNG 序列帧，ease-in-out 插值）
#   legend.py       — 独立图例说明书生成
#   exporter.py     — 标题渲染 + PNG 文件导出
#   metrics.py      — 物理参数 → 归一化分数（可独立运行）
#   cli.py          — CLI 解析与子命令实现

import os
import time
import cli

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# 默认输出到仓库根目录的 temp/ 文件夹
REPO_ROOT = os.path.normpath(os.path.join(SCRIPT_DIR, "..", "..", ".."))
DEFAULT_OUTPUT = os.path.join(REPO_ROOT, "temp")

if __name__ == "__main__":
    t_start = time.perf_counter()

    cli.run(default_dir=DEFAULT_OUTPUT)

    t_elapsed = time.perf_counter() - t_start
    print(f"Done. Total time: {t_elapsed:.2f}s ({t_elapsed * 1000:.0f}ms)")
