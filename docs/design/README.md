# Design 文档

更新时间：2026-03-08

## 目的
- 记录模式设计与当前实现口径。
- 说明 `flash / pro / ultra` 各自的信息层和音频层职责。
- 避免在改代码时重新从源码里反推设计意图。

## 当前文件
- `docs/design/transports.md`
  - 三种模式的定位、编码方式、主链路文件归属与当前非目标范围。
- `docs/design/flash-voicing-emotions.md`
  - `flash` voicing emotion 总览、decode 边界和 preset 对比。
- `docs/design/flash-voicing/`
  - 按 preset 拆分的 `Steady / Hostile / Litany / Collapse / Zeal / Void` 详细设计。
