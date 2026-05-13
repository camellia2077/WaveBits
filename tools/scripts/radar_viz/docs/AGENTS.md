# AGENTS.md — radar_viz 代码索引

## 这是什么

radar_viz 是 WaveBits 项目的雷达图可视化工具。它将音频编码模式的 8 维技术指标渲染为正八边形雷达图，输出透明背景 PNG，支持中英双语。

## 快速导航

| 主题 | 文件 | 说明 |
|------|------|------|
| 标签布局 | [layout.md](layout.md) | 8 维度位置、label/value 上下规则、方法调用链 |
| 渲染流程 | [rendering.md](rendering.md) | RadarChart 类、输出规则 |
| 常见修改 | [modifications.md](modifications.md) | 新增模式、调数值、调位置、调颜色 |

## 目录结构

```
radar_viz/
├── main.py              入口，调用 cli.run()
├── cli.py               CLI 参数解析，分发到各子命令
├── config.py            维度常量、颜色方案、导出尺寸
├── config_fonts.py      字体路径、字号、标签位置微调偏移
├── data.py              DATA_DICT 数据集定义（所有模式的 8 维数值）
├── radar.py             RadarChart 类：坐标系、装甲、网格、多边形、标签
├── exporter.py          标题渲染 + PNG 导出（含 Flash 模式子目录逻辑）
├── legend.py            独立图例说明书生成
├── animator.py          两模式间的过渡动画（PNG 序列帧，ease-in-out 插值）
├── scripts/             批处理脚本（双击运行）
├── docs/                参考文档
│   ├── AGENTS.md        本文件（索引）
│   ├── layout.md        标签布局与定位逻辑
│   ├── rendering.md     渲染流程
│   ├── modifications.md 常见修改场景
│   ├── metrics.py       从物理参数计算客观维度分数
│   ├── arousal_reference.md   解析难度维度参照标准
│   └── stability_reference.md 传输稳定维度参照标准
└── legacy/              废弃的旧版单体渲染器（不参与主流程）
```

## 注意事项

- 所有 .py 模块在同级目录，通过直接 import 互相引用，不要拆到子目录
- `config_fonts.py` 中的字体路径是本地绝对路径，换机器需要修改
- `renderer.py`（在 `legacy/`）是旧版单体渲染器，不参与主流程

## 修改后必做

**修改 `radar.py` 后必须跑测试，确认标签布局未被破坏：**

```bash
cd tools/scripts/radar_viz
python -m pytest test_radar.py -v
```

测试覆盖 `_shift_polar` 的零偏移、径向偏移、法线偏移（四个基角 + 对角线）。若测试失败，说明坐标转换逻辑被破坏，不要提交。
