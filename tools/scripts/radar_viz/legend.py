# legend.py
# 职责：生成独立的维度说明图例（Legend Panel）
# 依赖：config.py (颜色/标签/描述)，config_fonts.py (字体路径/大小)

import os
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties

import config
import config_fonts


def generate_legend(lang='en', out_dir=None):
    """
    生成独立的图例说明书（PNG），左脑区/右脑区分组，双语支持。

    参数
    ----
    lang    : 'en' 或 'zh'
    out_dir : 输出目录；为 None 时默认输出到脚本同级的 {lang}/ 子目录
    """
    labels = config.LABELS_EN if lang == 'en' else config.LABELS_ZH
    descriptions = config.DESC_EN if lang == 'en' else config.DESC_ZH
    title = "Technical Specification Guide" if lang == 'en' else "技术规格维度说明"

    fig, ax = plt.subplots(figsize=config.FIG_SIZE_LEGEND)
    fig.patch.set_facecolor(config.PRIMARY_COLOR)
    ax.axis('off')

    # 字体选择
    if lang == 'zh':
        title_font = FontProperties(fname=config_fonts.FONT_ZH_PATH, weight='bold',
                                    size=config_fonts.SIZE_LEGEND_TITLE_ZH)
        label_font = FontProperties(fname=config_fonts.FONT_ZH_PATH, weight='bold',
                                    size=config_fonts.SIZE_LEGEND_LABEL_ZH)
        desc_font  = FontProperties(fname=config_fonts.FONT_ZH_PATH, weight='regular',
                                    size=config_fonts.SIZE_LEGEND_DESC_ZH)
    else:
        title_font = FontProperties(fname=config_fonts.FONT_IBM_PLEX_BOLD,
                                    size=config_fonts.SIZE_LEGEND_TITLE_EN)
        label_font = FontProperties(fname=config_fonts.FONT_IBM_PLEX_BOLD,
                                    size=config_fonts.SIZE_LEGEND_LABEL_EN)
        desc_font  = FontProperties(fname=config_fonts.FONT_IBM_PLEX_REGULAR,
                                    size=config_fonts.SIZE_LEGEND_DESC_EN)

    title_x  = config_fonts.POS_X_LEGEND_LABEL_EN if lang == 'en' else config_fonts.POS_X_LEGEND_LABEL_ZH
    label_x  = title_x
    desc_x   = config_fonts.POS_X_LEGEND_DESC_EN  if lang == 'en' else config_fonts.POS_X_LEGEND_DESC_ZH

    # 主标题
    plt.text(title_x, 0.90, title, ha='left', fontproperties=title_font, color=config.SECONDARY_COLOR)

    # 各维度条目
    y_start = 0.82
    y_step  = 0.07
    bullet  = ">"
    current_y = y_start

    for i, label in enumerate(labels):
        # 左脑区分组标题
        if i == 0:
            group = "[ 左脑机理区 ]" if lang == 'zh' else "[ RATIONAL METRICS (LEFT BRAIN) ]"
            plt.text(title_x, current_y, group, va='center', ha='left',
                     fontproperties=desc_font, color='#888888')
            current_y -= y_step
        # 右脑区分组标题（插入小间距）
        elif i == 4:
            current_y -= y_step * 0.3
            group = "[ 右脑感知区 ]" if lang == 'zh' else "[ EMOTIONAL METRICS (RIGHT BRAIN) ]"
            plt.text(title_x, current_y, group, va='center', ha='left',
                     fontproperties=desc_font, color='#888888')
            current_y -= y_step

        plt.text(label_x, current_y, f"{bullet} {label}",
                 va='center', ha='left', fontproperties=label_font, color=config.SECONDARY_COLOR)
        plt.text(desc_x, current_y, f":  {descriptions[label]}",
                 va='center', ha='left', fontproperties=desc_font, color=config.TEXT_COLOR)

        current_y -= y_step

    # 边框
    rect = plt.Rectangle((0.02, 0.02), 0.96, 0.96,
                          transform=ax.transAxes,
                          color=config.OUTLINE_COLOR, fill=False, linewidth=12)
    ax.add_patch(rect)

    # 保存
    if out_dir is None:
        here = os.path.dirname(os.path.abspath(__file__))
        base = os.path.normpath(os.path.join(here, "..", "..", "..", "temp"))
        out_dir = os.path.join(base, lang)

    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "legend.png")

    plt.savefig(out_path, facecolor=config.PRIMARY_COLOR, dpi=config.EXPORT_DPI_LEGEND,
                bbox_inches='tight', pad_inches=0.1)
    plt.close()
    print(f"[Generated Legend ({lang.upper()})] -> {out_path}")
