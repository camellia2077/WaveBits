# radar.py
# 职责：雷达图核心绘制逻辑
# 以 RadarChart 类封装一次绘图会话的共享状态 (fig, ax, angles, view_limit)，
# 消除函数间反复传递相同参数的冗余。

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
from matplotlib.patches import Polygon

import config
import config_fonts

# Flash 模式使用 JetBrains Mono，其余使用 IBM Plex Mono
_FLASH_MODES = {"Steady", "Hostile", "Litany", "Collapse", "Zeal", "Void"}


class RadarChart:
    """
    一次雷达图绘制会话的封装。

    典型用法：
        chart = RadarChart(vals)
        chart.draw_armor()
        chart.draw_grids()
        chart.draw_data(vals)
        chart.draw_labels(vals, mode_key, lang='en')
        # ... 保存后切换语言 ...
        chart.draw_labels(vals, mode_key, lang='zh')
        chart.close()
    """

    VIEW_LIMIT = 10.5  # 雷达图径向上限（略大于 Hex-10 装甲）

    def __init__(self, vals):
        """初始化极坐标系并缓存会话状态。"""
        self.fig, self.ax = plt.subplots(figsize=config.FIG_SIZE_RADAR, subplot_kw=dict(polar=True))

        n = len(config.LABELS_EN)
        angles = np.linspace(0, 2 * np.pi, n, endpoint=False)
        self.angles = np.concatenate((angles, [angles[0]]))

        # Counter-Clockwise；起始偏移 112.5° → 左上顶点
        # 使 0-3 (Machine/Left) 落在左侧，4-7 (Spirit/Right) 落在右侧
        self.ax.set_theta_direction(1)
        self.ax.set_theta_offset(5 * np.pi / 8)

        self.ax.set_ylim(0, self.VIEW_LIMIT)
        self.ax.set_yticklabels([])
        self.ax.set_xticklabels([])  # 隐藏默认的角度刻度 (0, 45, 90...)
        self.ax.spines['polar'].set_visible(False)
        self.ax.grid(False)

    # ------------------------------------------------------------------
    # 背景层
    # ------------------------------------------------------------------

    def draw_armor(self, limit_val=10):
        """绘制背景面板与黄铜边框（Hex-10 上限）。"""
        octagon_v = np.full_like(self.angles, limit_val)
        self.ax.fill(self.angles, octagon_v, color=config.PRIMARY_COLOR, zorder=0)
        
        # 使用 Polygon 确保起点和终点正确闭合（miter join），解决角部线条重叠露出的穿模问题
        poly = Polygon(np.column_stack([self.angles[:-1], octagon_v[:-1]]), 
                       closed=True, fill=False, edgecolor=config.OUTLINE_COLOR, 
                       linewidth=9, joinstyle='miter', zorder=1)
        self.ax.add_patch(poly)

    def draw_grids(self):
        """绘制内部红色参考网格（2 / 4 / 6 / 8 刻度）。"""
        for g_val in [2, 4, 6, 8]:
            gv = np.full_like(self.angles, g_val)
            self.ax.plot(self.angles, gv, color=config.SECONDARY_COLOR,
                         linewidth=1.2, linestyle='--', alpha=0.25, zorder=1)

    # ------------------------------------------------------------------
    # 数据层
    # ------------------------------------------------------------------

    def draw_data(self, vals):
        """绘制单层雷达多边形。"""
        self._draw_standard(vals)

    def _draw_standard(self, values):
        """单层雷达多边形；若任意维度突破 Hex-10 则切换为深红。"""
        pv = np.concatenate((values, [values[0]]))
        color = '#CC0000' if any(v > 10 for v in values) else config.SECONDARY_COLOR

        # 使用 Polygon 确保闭合（round join）
        poly = Polygon(np.column_stack([self.angles[:-1], values]), 
                       closed=True, fill=False, edgecolor=color, 
                       linewidth=5, joinstyle='round', zorder=3, clip_on=False)
        self.ax.add_patch(poly)
        
        self.ax.fill(self.angles, pv, color=color, alpha=0.6,
                     zorder=2, clip_on=False)

    # ------------------------------------------------------------------
    # 标签层
    # ------------------------------------------------------------------

    def draw_labels(self, vals, mode_key, lang='en'):
        """绘制维度标签与数值，支持中/英文切换。"""
        # 清理旧的维度标签（防止中英文标签重叠）
        for t in list(self.ax.texts):
            if getattr(t, '_is_dim_label', False):
                t.remove()

        labels = config.LABELS_EN if lang == 'en' else config.LABELS_ZH

        # ---------------------------------------------------------------
        # 字体准备
        # ---------------------------------------------------------------
        if lang == 'zh':
            label_font = FontProperties(fname=config_fonts.FONT_ZH_PATH,
                                        weight='bold', size=config_fonts.SIZE_RADAR_LABEL)
        else:
            label_font = self._get_font_props(mode_key, weight='bold',
                                              size=config_fonts.SIZE_RADAR_LABEL)

        # ---------------------------------------------------------------
        # 逐维度渲染
        # ---------------------------------------------------------------
        if lang == 'zh':
            radius_map = config_fonts.RADAR_LABEL_RADIUS_OFFSETS_ZH
            angle_map  = config_fonts.RADAR_LABEL_ANGLE_OFFSETS_ZH
        else:
            radius_map = config_fonts.RADAR_LABEL_RADIUS_OFFSETS_EN
            angle_map  = config_fonts.RADAR_LABEL_ANGLE_OFFSETS_EN

        for i, angle in enumerate(self.angles[:-1]):
            r_offset     = radius_map.get(i, 0.0)
            a_offset_deg = angle_map.get(i, 0.0)
            theta        = angle + np.deg2rad(a_offset_deg)
            r            = self.VIEW_LIMIT + r_offset

            curr_label   = labels[i]
            curr_val_str = f"{vals[i]}"
            combined     = f"{curr_label} {curr_val_str}"

            # 双层渲染：黑色层（文字+括号），红色层（数字）
            # 中文用全角空格（U+3000）替换 CJK 字符保持宽度一致
            black_chars, red_chars = [], []
            for c in combined:
                if c.isdigit() or c == '.':
                    black_chars.append(' ')
                    red_chars.append(c)
                else:
                    black_chars.append(c)
                    red_chars.append('　' if ord(c) > 0x7F else ' ')

            t = self.ax.text(theta, r, ''.join(black_chars),
                             color=config.TEXT_COLOR, fontproperties=label_font,
                             ha='center', va='center',
                             zorder=10, clip_on=False)
            t._is_dim_label = True

            self.ax.text(theta, r, ''.join(red_chars),
                         color=config.SECONDARY_COLOR, fontproperties=label_font,
                         ha='center', va='center',
                         zorder=11, clip_on=False)._is_dim_label = True

        # 隐藏原始坐标轴刻度
        self.ax.set_xticks(self.angles[:-1])
        self.ax.set_xticklabels([])

    # ------------------------------------------------------------------
    # 生命周期
    # ------------------------------------------------------------------

    def close(self):
        """释放 matplotlib figure 资源。"""
        plt.close(self.fig)

    # ------------------------------------------------------------------
    # 私有辅助
    # ------------------------------------------------------------------

    @staticmethod
    def _get_font_props(mode_key, weight='regular', size=None):
        """根据 mode 动态选择英文字体（Flash → JetBrains Mono，其余 → IBM Plex Mono）。"""
        if size is None:
            size = config_fonts.SIZE_RADAR_LABEL

        if mode_key in _FLASH_MODES:
            path = (config_fonts.FONT_JETBRAINS_BOLD if weight == 'bold'
                    else config_fonts.FONT_JETBRAINS_REGULAR)
        else:
            path = (config_fonts.FONT_IBM_PLEX_BOLD if weight == 'bold'
                    else config_fonts.FONT_IBM_PLEX_REGULAR)

        return FontProperties(fname=path, size=size)

    @staticmethod
    def _shift_polar(angle, radius, offset, direction='perpendicular'):
        """在笛卡尔空间施加偏移后转回极坐标。

        direction: 'perpendicular'（法线方向，远离圆心）或 'radial'（径向）
        返回: (new_r, new_theta)
        """
        cx = radius * np.cos(angle)
        cy = radius * np.sin(angle)

        if direction == 'perpendicular':
            nx, ny = -np.sin(angle), np.cos(angle)
        else:  # radial
            nx, ny = np.cos(angle), np.sin(angle)

        cx += offset * nx
        cy += offset * ny

        return np.hypot(cx, cy), np.arctan2(cy, cx)
