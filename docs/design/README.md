# Design 文档

更新时间：2026-05-04

## 目的
- 记录模式设计与当前实现口径。
- 说明 `mini / flash / pro / ultra` 各自的信息层和音频层职责。
- 避免在改代码时重新从源码里反推设计意图。

## 当前文件
- `docs/design/transports.md`
  - 四种模式的总览、对比、字符集约束与跳转入口。
- `docs/design/modes/README.md`
  - mode-first 文档总入口。
- `docs/design/modes/mini.md`
  - `mini` 的输入规范、Morse 节奏、follow / visual 与主链路文件。
- `docs/design/modes/pro.md`
  - `pro` 的 ASCII-only 结构、`DTMF-like` clean PHY 与主链路文件。
- `docs/design/modes/ultra.md`
  - `ultra` 的 UTF-8 byte 结构、clean `16-FSK` 与主链路文件。
- `docs/design/modes/flash/README.md`
  - `flash` 主说明：bit-by-bit `BFSK`、emotion / style 结构、decode 边界与主链路文件。
- `docs/design/modes/flash/voicing-emotions.md`
  - `flash` voicing emotion 总览、decode 边界和 preset 对比。
- `docs/design/modes/flash/`
  - 按 preset 拆分的 `Steady / Hostile / Litany / Collapse / Zeal / Void` 详细设计。
