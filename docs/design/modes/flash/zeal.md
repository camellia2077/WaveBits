# Flash Voicing: Zeal

更新时间：2026-05-03

## 情绪目标
`Zeal` 表达高唤醒、兴奋、确信和快速宣告。它的重点是明亮、变速、密集和突然强调，但不是 Hostile 那种威胁和过载。v2 以稳定 decode 为前提，通过确定性 timing / carrier schedule 制造急促与突变。

## Signal / Timing
- 使用 `Zeal` signal profile。
- 每个 low/high bit 按固定 16-bit pattern 在 `0.5x`、`0.625x`、`0.75x`、`1x frame_samples` 间切换。
- low/high carrier 随 bit window 切换：`900 / 1800 Hz` 用于最短 burst，`760 / 1520 Hz` 用于 fast，`660 / 1320 Hz` 用于 drive，`560 / 1120 Hz` 用于 hold。
- 短 window 使用更高 carrier，保证同一 bit 内仍有足够波形周期供 Goertzel low/high 判定。
- 普通 payload 不插入停顿；逗号 / 分号 / 冒号插入 `1 slot`，句号 / 感叹号 / 问号插入 `4 slot`，换行插入 `5 slot`。
- pause 后第一个 bit 强制使用 burst timing，使长停顿后立即回到高能量短窗口。
- decode 使用 Zeal 专用 gap-aware path：按同一确定性 pattern 读取变长 window，遇到低能量 silence slot 时跳过并把下一个 bit 作为 burst。

## Voicing 方法
- 更短 attack / release。
  - 每个 tone 更短促、更接近快速发声，但 signal 层仍保留足够 carrier 周期供判定。
- 更亮 harmonic 与适中边界 accent。
  - 二、三次谐波和 byte / nibble accent 高于 Steady，但低于 Hostile 的攻击性口径。
- 轻度 softclip 与快速 tremolo。
  - 让整体更紧、更兴奋，但不制造 Collapse 式不稳定。
- zeal rising shell。
  - preamble / epilogue 仍保持 `3x frame_samples`，避免 trim 与 decode 复杂化。
  - shell 使用上行、短促的明亮 burst，只存在于非 payload 区域。

## 听感关键词
明亮、快速、密集、上扬、确信、高唤醒。

## 后续调音方向
- 可继续微调 carrier、bit 时长、强标点 pause slot 和 bright layer。
- 不建议把变速 pattern 做成随机值；decode 需要和 encode 使用同一确定性 schedule。
