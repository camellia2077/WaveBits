# 渲染流程

## 核心流程

```
main.py → cli.run()
  → cmd_generate_all()  遍历 DATA_DICT
    → RadarChart(vals)          创建极坐标画布
    → chart.draw_armor()        绘制米白背景 + 黄铜八边形边框
    → chart.draw_grids()        绘制 2/4/6/8 内部红色虚线网格
    → chart.draw_data(vals)     绘制数据多边形（单层，超过 Hex-10 变深红）
    → chart.draw_labels(...)    绘制 8 个维度的标签 + 数值
    → exporter.save_and_title()  添加居中标题，保存为 PNG
  → legend.generate_legend()    生成图例说明图
```

## RadarChart 类

| 方法 | 职责 |
|------|------|
| `__init__(vals)` | 初始化极坐标系，缓存 angles / view_limit |
| `draw_armor(limit_val)` | 背景面板 + 黄铜边框（Hex-10 上限）|
| `draw_grids()` | 2/4/6/8 红色虚线网格 |
| `draw_data(vals)` | 数据多边形（超过 10 变深红）|
| `draw_labels(vals, mode_key, lang)` | 维度标签 + 数值，中英切换 |
| `close()` | 释放 matplotlib figure |

## 输出规则

- 默认输出到仓库 `temp/` 目录（`main.py` 的 `DEFAULT_OUTPUT`）
- Flash 模式输出到 `{lang}/flash/` 子目录（`exporter.py` 的 `_FLASH_MODES`）
- 文件名空格自动替换为下划线
- 分辨率：静态图 300 DPI，动画帧 150 DPI（`config.py`）
