# exporter.py
# 职责：将已绘制的雷达图加上标题后保存为 PNG
# 依赖：config.py (颜色)，config_fonts.py (字体路径/大小)

import os
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties

import config
import config_fonts


# Flash 模式统一输出到 {lang}/flash/ 子目录
_FLASH_MODES = {"Steady", "Hostile", "Litany", "Collapse", "Zeal", "Void"}


def _resolve_out_path(mode_key, lang, script_dir):
    """根据模式和语言确定输出文件的完整路径。"""
    base_dir = os.path.join(script_dir, lang)
    if mode_key in _FLASH_MODES:
        out_dir = os.path.join(base_dir, "flash")
    else:
        out_dir = base_dir
    os.makedirs(out_dir, exist_ok=True)
    safe_name = mode_key.replace(" ", "_")
    return os.path.join(out_dir, f"radar_panel_{safe_name}.png")


def save_and_title(fig, mode_key, lang='en', script_dir=None):
    """
    为已有 figure 添加居中标题，并保存为透明背景 PNG。
    """
    if script_dir is None:
        here = os.path.dirname(os.path.abspath(__file__))
        script_dir = os.path.normpath(os.path.join(here, "..", "..", "..", "temp"))

    ax = fig.gca()

    # 清理旧标题 text artist（仅移除带 _is_title 标记的，保留维度标签）
    for t in list(ax.texts):
        if getattr(t, '_is_title', False):
            t.remove()

    # 标题配置选择
    if lang == 'zh':
        title_font = FontProperties(fname=config_fonts.FONT_ZH_PATH,
                                    weight='bold', size=config_fonts.SIZE_RADAR_TITLE_ZH)
        title_y = config_fonts.POS_Y_RADAR_TITLE_ZH
    else:
        title_font = FontProperties(fname=config_fonts.FONT_IBM_PLEX_BOLD,
                                    size=config_fonts.SIZE_RADAR_TITLE_EN)
        title_y = config_fonts.POS_Y_RADAR_TITLE_EN

    # 从数据配置中直接读取显示名称
    from data import DATA_DICT
    mode_data = DATA_DICT[mode_key]
    display_title = mode_data["name_zh"] if lang == 'zh' else mode_data["name_en"]

    # 居中顶部标题（向上偏移，避免与雷达图重叠）
    title_artist = plt.text(0.5, title_y, display_title,
                            transform=ax.transAxes,
                            fontproperties=title_font,
                            color=config.TEXT_COLOR,
                            ha='center', va='top')
    title_artist._is_title = True  # 标记为标题，供下次清理时识别

    # 保存
    out_path = _resolve_out_path(mode_key, lang, script_dir)
    plt.savefig(out_path, facecolor='none', transparent=True, dpi=config.EXPORT_DPI_RADAR)
    print(f"[Generated ({lang.upper()})] -> {out_path}")
