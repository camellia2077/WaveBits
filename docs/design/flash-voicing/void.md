# Flash Voicing: Void

更新时间：2026-05-03

## 情绪目标
`Void` 表达低唤醒、沉重、疏离和拖尾。它的重点是低频、更慢、更空的听感，但 v2 仍以稳定 decode 为前提，不通过真实 payload silence 制造间隔。

## Signal / Timing
- 使用 `Void` signal profile。
- 每个 low/high bit 使用 `2.5x frame_samples`。
- low/high carrier 为 `220 / 440 Hz`。
- 不插入额外可跳过 silence。
- payload timeline 连续，decode 使用普通 low/high window 判定。

## Voicing 方法
- 更长 release 和较低 harmonic brightness。
  - 每个 tone 有更明显拖尾，但仍保留完整 low/high 判定窗口。
- 很弱的 byte / nibble accent。
  - 降低边界重音，避免听起来像稳定报告或强命令。
- 弱低频 drone、低速浅 tremolo 和更轻的 softclip。
  - 只作为低沉、下坠的质感，不用 texture 承载语义。
- void descent shell。
  - preamble / epilogue 仍保持 `3x frame_samples`，避免 trim 与 decode 复杂化。
  - shell 使用下行、低频、拖尾的非 payload 声效。

## 听感关键词
低沉、拖尾、疏离、下行、克制、空旷。

## 后续调音方向
- 可继续微调 carrier、release 和低频层。
- 如果需要更强的真实空白感，应单独验证 gap-aware decode、visual follow 和 saved audio signal info。
