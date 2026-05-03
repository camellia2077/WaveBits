# Flash Voicing: Steady

更新时间：2026-05-03

## 情绪目标
`Steady` 表达日常交流、冷静状态、协议式自然语言。它不是完全无风格的 clean BFSK，而是“机械人以平稳语调输出文本”：音高偏低，节奏稳定，边界清楚，没有恐惧或敌意。

## Signal / Timing
- 使用 `Steady` signal profile。
- 每个 low/high bit 使用 `0.9375x frame_samples`。
- low/high carrier 为 `300 / 600 Hz`。
- 不插入额外可跳过 silence。
- payload timeline 连续，适合作为其他 emotion 的基线；v2 只做保守加速，让日常输出更利落但不靠近 Hostile 的短促命令感。

## Voicing 方法
- 加入很弱的 `120 Hz` low voice layer。
  - 目的：让 Steady 不像裸 BFSK 那么干，而更像稳定的发声格栅。
  - 幅度保持低，避免抢占 `300 Hz / 600 Hz` 主判定频点。
- 保留轻量金属层。
  - 金属层很弱，只提供机械质感，不制造强烈情绪。
- 使用轻微 tremolo。
  - 深度很低，频率约 `6.2 Hz`。
  - 只提供微弱设备感，不形成颤抖。
- byte / nibble 边界 accent 保守。
  - byte 边界 click 比 Hostile 弱。
  - 让二进制结构仍清楚，但不攻击。
- preamble / epilogue 使用克制的 steady protocol shell。
  - 开头和结尾仍像通信协议边界，但音高和 shell 混合比旧 protocol boundary 更低、更轻。
  - 目标是“开始传输 / 结束传输”的冷静提示，不做攻击性警告、仪式召唤或崩溃尾音。

## 听感关键词
稳定、低音、平直、精准、可预测、冷静。

## 后续调音方向
- 可以继续降低或微调低音层，让它更接近日常机械语音。
- 不建议加入明显停顿，否则会抢 Litany / Collapse 的表达空间。
