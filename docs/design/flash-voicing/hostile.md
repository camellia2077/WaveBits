# Flash Voicing: Hostile

更新时间：2026-05-03

## 情绪目标
`Hostile` 表达愤怒、敌意、威胁和攻击性。它不通过变慢或停顿表达情绪，而是通过更硬的边界、更短的 release、更强的金属边缘和软削波，让文本像被强行喷射出来。

## Signal / Timing
- 使用 `Hostile` signal profile。
- 每个 low/high bit 使用 `0.875x frame_samples`。
- low/high carrier 为 `450 / 900 Hz`。
- 不插入额外可跳过 silence。
- 不插入 payload silence，但比 Steady 更短，强调“快、硬、压迫”。

## Voicing 方法
- 更强的 byte / nibble accent。
  - `byte_boundary_click_scale` 明显高于 Steady。
  - `nibble_boundary_accent_factor` 也更强，让 payload 有切割感。
- attack / release 更短。
  - `payload_release_scale` 小于 Steady。
  - 每个 low/high 更像短促命令，而不是吟唱或拖尾。
- 更强金属层。
  - `metallic_layer_gain_scale` 明显提高。
  - 让声效带有机械刃口和侵略性。
- hostile edge layer。
  - 在主载波之外叠加尖锐边缘层。
  - 目标是增加“咬字”和电气怒意。
- 更明显软削波。
  - 提高 softclip drive 和 mix。
  - 让整体听起来被压缩、过载、硬朗，但仍限制在 PCM16 范围。
- 快速 tremolo。
  - 频率约 `9.8 Hz`，深度比 Steady 高。
  - 表达激动和威胁，但不做 Collapse 那种失控颤抖。
- preamble / epilogue 使用 hostile challenge shell。
  - preamble 用短促多段 burst 和更硬的高频边缘，像威胁式锁定。
  - epilogue 用快速闭合 burst 和硬门限尾部，像攻击协议的断然确认。
  - 这些声音只存在于 preamble / epilogue；payload 仍保持可解码 BFSK，不用 shell 变化承载 bit。

## 听感关键词
短促、硬、尖、过载、敌意、命令式。

## 后续调音方向
- 可以继续打磨 attack、边界 click 和失真比例。
- 不建议增加随机 silence，否则敌意会变成慌张。
