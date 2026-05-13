# 常见修改场景

## 新增编码模式

1. 打开 `data.py`，在 `DATA_DICT` 中添加新条目
2. 提供：`name_en`、`name_zh`、`vals`（8 个整数，对应 8 个维度）
3. 运行 `python main.py --list` 确认别名索引
4. 运行 `python main.py --chart <新模式名> --lang both` 测试出图

## 调整维度数值

1. 打开 `data.py`，找到对应模式的 `vals`
2. 8 个数值按顺序：传输速度、编码效率、传输稳定、兼容性、解析难度、电子感、情感丰富度、仪式感
3. 从物理参数计算客观分数：运行 `docs/metrics.py`

## 调整标签位置 / 字号

| 调整项 | 位置 |
|--------|------|
| 字号 | `config_fonts.py` 的 `SIZE_RADAR_LABEL`、`SIZE_RADAR_TITLE_*` |
| 径向偏移 | `config_fonts.py` 的 `RADAR_LABEL_RADIUS_OFFSETS_*`（正=远离圆心）|
| 角度偏移 | `config_fonts.py` 的 `RADAR_LABEL_ANGLE_OFFSETS_*`（正=逆时针）|
| label/value 间距 | `radar.py` 的 `draw_labels()` 中 `HALF_GAP` |
| 布局逻辑 | `radar.py` 的 `_label_value_positions()` |

## 调整颜色 / 视觉风格

| 参数 | 文件 | 说明 |
|------|------|------|
| `PRIMARY_COLOR` | config.py | 背景底色（米白）|
| `SECONDARY_COLOR` | config.py | 数据多边形 + 网格（深红）|
| `OUTLINE_COLOR` | config.py | 八边形边框（黄铜）|
| `TEXT_COLOR` | config.py | 标签文字颜色 |

数值渲染分两层：黑色背景层（括号）+ 红色前景层（数字），见 `radar.py` 的 `draw_labels()`。

## 添加参考文档

放到 `docs/` 目录。新的维度参照标准保持与 `arousal_reference.md` / `stability_reference.md` 相同结构。
