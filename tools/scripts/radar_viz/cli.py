# cli.py
# 职责：CLI 解析与子命令实现
# 依赖：data, radar, exporter, legend, animator
# 被 main.py 调用；不直接运行。

import os
import sys
import argparse

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
plt.rcParams['axes.unicode_minus'] = False

import data
from radar import RadarChart
import exporter
import legend
import animator


# ==========================================
# [ 内部工具 ]
# ==========================================

def _resolve_output(output_arg: str | None, default_dir: str) -> str:
    """解析 --output 参数；未指定时回退到 default_dir（main.py 所在目录）。"""
    return os.path.abspath(output_arg) if output_arg else default_dir


def _resolve_langs(lang_arg: str) -> list[str]:
    """将 'both' 展开为 ['en', 'zh']；否则返回单元素列表。"""
    return ['en', 'zh'] if lang_arg == 'both' else [lang_arg]


def _render_single(mode_key: str, lang: str, out_dir: str):
    """渲染单个模式的单语言版本并保存。"""
    mode_data = data.DATA_DICT[mode_key]
    vals      = mode_data["vals"]

    chart = RadarChart(vals)
    chart.draw_armor()
    chart.draw_grids()
    chart.draw_data(vals)
    chart.draw_labels(vals, mode_key, lang=lang)
    exporter.save_and_title(chart.fig, mode_key, lang=lang, script_dir=out_dir)
    chart.close()


# ==========================================
# [ 子命令实现 ]
# ==========================================

def cmd_list():
    """--list：列出所有可用模式及其数字别名。"""
    print("Available modes:\n")
    for idx, key in data.ALIASES.items():
        print(f"  {idx:<3} {key}")
    print()


def cmd_chart(token: str, lang: str, out_dir: str):
    """--chart：生成指定单个模式的静态雷达图（支持数字别名）。"""
    try:
        mode_key = data.resolve_mode(token)
    except KeyError as e:
        print(f"[错误] {e}")
        sys.exit(1)

    for lg in _resolve_langs(lang):
        _render_single(mode_key, lg, out_dir)


def cmd_generate_all(lang: str, out_dir: str):
    """--generate：批量生成所有模式的静态雷达图 + 图例。"""
    from tqdm import tqdm
    langs = _resolve_langs(lang)
    print(f"Generating all radar charts ({', '.join(l.upper() for l in langs)})...\n")

    for mode_key, mode_data in tqdm(data.DATA_DICT.items(), desc="Generating Charts"):
        vals = mode_data["vals"]

        chart = RadarChart(vals)
        chart.draw_armor()
        chart.draw_grids()
        chart.draw_data(vals)

        for lg in langs:
            chart.draw_labels(vals, mode_key, lang=lg)
            exporter.save_and_title(chart.fig, mode_key, lang=lg, script_dir=out_dir)

        chart.close()

    for lg in langs:
        legend.generate_legend(lang=lg, out_dir=os.path.join(out_dir, lg))


def cmd_animate(start_token: str, end_token: str, lang: str, frames: int, out_dir: str):
    """--animate：生成两个模式之间的过渡动画序列帧。"""
    try:
        start_key = data.resolve_mode(start_token)
        end_key   = data.resolve_mode(end_token)
    except KeyError as e:
        print(f"[错误] {e}")
        sys.exit(1)

    if start_key == end_key:
        print("[错误] 起点和终点是同一个模式，无需生成动画。")
        sys.exit(1)

    for lg in _resolve_langs(lang):
        animator.generate_transition(
            start_key  = start_key,
            end_key    = end_key,
            data_dict  = data.DATA_DICT,
            lang       = lg,
            frames     = frames,
            script_dir = out_dir,
        )


# ==========================================
# [ Parser 构建 ]
# ==========================================

def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog        = "python main.py",
        description = "WaveBits Radar Chart Generator · 波段雷达图生成器",
        formatter_class = argparse.RawDescriptionHelpFormatter,
        epilog = """\
示例：
  python main.py --list
      查看所有模式与数字别名

  python main.py --chart Steady --lang en
  python main.py --chart 3 --lang zh --output ./out
      生成单个模式的雷达图

  python main.py --generate --lang both
  python main.py --generate --lang en --output ./out
      生成全部模式的静态雷达图与图例

  python main.py --animate Steady Litany --lang en
  python main.py --animate 1 3 --lang zh --output ./out
  python main.py --animate 0 9 --lang both --frames 90
      生成两个模式之间的过渡动画
        """
    )

    action = parser.add_mutually_exclusive_group()
    action.add_argument("--list",     action="store_true",       help="列出所有可用模式及数字别名")
    action.add_argument("--chart",    metavar="MODE",            help="生成指定单个模式的静态雷达图（支持名字或数字别名）")
    action.add_argument("--generate", action="store_true",       help="生成全部模式的静态雷达图与图例")
    action.add_argument("--animate",  nargs=2, metavar=("START", "END"), help="生成从 START 到 END 的过渡动画")

    parser.add_argument("--lang",   default="en", choices=["en", "zh", "both"], help="输出语言：en / zh / both（默认 en）")
    parser.add_argument("--output", metavar="DIR", default=None,                help="输出根目录（默认为仓库 temp/ 目录）")
    parser.add_argument("--frames", type=int, default=60,                       help="动画总帧数，仅 --animate 生效（默认 60，即 2 秒 @ 30fps）")

    return parser


# ==========================================
# [ 主分发入口 ]
# ==========================================

def run(default_dir: str):
    """
    解析命令行参数并分发到对应子命令。
    default_dir: 调用方（main.py）的所在目录，作为 --output 的回退默认值。
    """
    parser = build_parser()
    args   = parser.parse_args()

    if len(sys.argv) == 1:
        parser.print_help()
        sys.exit(0)

    out_dir = _resolve_output(args.output, default_dir)

    if args.list:
        cmd_list()
    elif args.chart:
        cmd_chart(args.chart, args.lang, out_dir)
    elif args.generate:
        cmd_generate_all(args.lang, out_dir)
    elif args.animate:
        cmd_animate(
            start_token = args.animate[0],
            end_token   = args.animate[1],
            lang        = args.lang,
            frames      = args.frames,
            out_dir     = out_dir,
        )
