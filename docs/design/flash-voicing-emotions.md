# Flash Voicing Emotions Design

更新时间：2026-05-03

## 目标
`flash` 模式的设计目的不是高效率通信，而是把文本和音频互相转换成一种可解码的风格化声效。因此 `flash` 的风格层要同时满足两个目标：
- 仍然能把原始文本 byte 可靠地映射为 `low/high` BFSK payload，并在同一配置下 decode 回原文。
- 让同一段文本在不同 voicing emotion 下呈现不同情绪：日常、敌意、诵唱、崩溃、热忱、低沉疏离。

## 文档索引
按 voicing/preset 拆分后的详细设计：
- [Steady](flash-voicing/steady.md)
- [Hostile](flash-voicing/hostile.md)
- [Litany](flash-voicing/litany.md)
- [Collapse](flash-voicing/collapse.md)
- [Zeal](flash-voicing/zeal.md)
- [Void](flash-voicing/void.md)

## 情绪模型
`flash` 当前是一次破坏性重构后的六情绪模型：用户可见和代码配置只保留 `Steady / Hostile / Litany / Collapse / Zeal / Void`。每个 emotion 同时决定 payload timing 与 voicing 声效，不再保留旧 flavor 或旧兼容映射。

- `flash_signal_profile`
  - 负责 payload 中每个 bit 的基础时长和 silence slot 尺寸。
  - 当前只包括 `Steady`、`Hostile`、`Litany`、`Collapse`、`Zeal`、`Void`。
  - `Steady` 使用 `0.9375x frame_samples`，carrier 为 `300 / 600 Hz`。
  - `Hostile` 使用 `0.875x frame_samples`，carrier 为更有攻击性的 `450 / 900 Hz`。
  - `Collapse` 使用 `1x frame_samples`，carrier 为更低、更胆怯的 `280 / 560 Hz`。
  - `Litany` 使用 `6x frame_samples`，carrier 为低沉的 `220 / 440 Hz`；它的可跳过 silence slot 仍是 `1x frame_samples`。
  - `Zeal` 使用确定性变速 timing：`0.5x` / `0.625x` / `0.75x` / `1x frame_samples`；carrier 随 timing 在 `560-900 / 1120-1800 Hz` 间切换，强标点后插入可跳过长停顿并从 burst bit 重新进入。
  - `Void` 使用 `2.5x frame_samples`，carrier 为低沉的 `220 / 440 Hz`；v2 仍不插入额外 payload silence，优先保证 decode 稳定。
- `flash_voicing_flavor`
  - 负责情绪音色、preamble / epilogue、payload texture、停顿与边界 accent。
  - 当前只包括 `Steady`、`Hostile`、`Litany`、`Collapse`、`Zeal`、`Void`。

Android 当前仍然只暴露一个用户选择器，但每个 preset 内部同时指定这两轴：
- `Steady`: `signal=Steady`, `voicing=Steady`
- `Hostile`: `signal=Hostile`, `voicing=Hostile`
- `Litany`: `signal=Litany`, `voicing=Litany`
- `Collapse`: `signal=Collapse`, `voicing=Collapse`
- `Zeal`: `signal=Zeal`, `voicing=Zeal`
- `Void`: `signal=Void`, `voicing=Void`

这样做的目的，是让 UI 保持简单，同时让代码上可以分别调整“low/high 持续多久”和“这些 low/high 听起来像什么情绪”。

## Decode 边界
`flash` payload 的根本语义仍是原始 byte 透明传输：
- 每个输入 byte 拆成 8 个 bit。
- bit `0` 渲染为 low carrier。
- bit `1` 渲染为 high carrier。
- decode 时按同一 signal profile 的 slot 大小读取 low/high，再拼回 byte。

所有 emotion 的声效设计都必须遵守这个边界：
- 不把语义信息藏进 texture / drone / tremor。
- 不让 preamble / epilogue 参与 payload decode。
- 可变静音只能以整数 slot 插入，decode 通过 gap-aware 逻辑跳过这些 silence。
- `Litany` / `Collapse` 的 silence chunk 必须是真正的零样本静音，不在 silence 里加入残响，否则跳过静音的能量阈值会变得不稳定。
- `Zeal` 使用确定性变速 / 变频 layout 和专用 gap-aware decode；强标点 silence 仍是真正零样本静音。`Void` v2 不插入 payload silence，decode 仍走普通 low/high window 判定。

## Emotion 对比
| Emotion | Signal profile | 主要节奏 | 主要音色 | silence 策略 | 目标听感 |
| --- | --- | --- | --- | --- | --- |
| Steady | Steady | 连续 `0.9375x` bit，`300 / 600 Hz` | 低音稳态、轻金属 | 无额外 silence | 日常、冷静、精准 |
| Hostile | Hostile | 连续 `0.875x` bit，`450 / 900 Hz`，短 attack / release | 强 click、金属边缘、软削波 | 无额外 silence | 愤怒、敌意、命令 |
| Litany | Litany | `6x` bit，`220 / 440 Hz`，逐拍停顿 | 低频 drone、swell、机械喉腔、句尾收束 | 每 bit / 空格 / 标点 / 周期呼吸 | 崇敬、祷告、慢诵 |
| Collapse | Collapse | `1x` bit，`280 / 560 Hz`，局部 cluster | tremor、hesitation、failure shell | 固定 `2 slot` 结巴 cluster，少量 `5 slot` panic break | 恐惧、崩溃、结巴 |
| Zeal | Zeal | 变速 `0.5x-1x` bit，`560-900 / 1120-1800 Hz` | 亮 harmonic、快速 tremolo、上行 shell | 强标点长停顿，停顿后 burst | 热忱、变速、密集 |
| Void | Void | 连续 `2.5x` bit，`220 / 440 Hz` | 低频 drone、更弱 accent、更长 release、下行 shell | 无额外 silence | 低沉、拖尾、疏离 |
