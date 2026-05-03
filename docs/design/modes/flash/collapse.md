# Flash Voicing: Collapse

更新时间：2026-05-03

## 情绪目标
`Collapse` 表达恐惧、慌张、崩溃和结巴。它不是“随机坏掉”，而是内在恐惧导致外在语音控制失败：多数时候仍能输出，但会突然卡住、重复、颤抖或短暂断开。

## Signal / Timing
- 使用 `Collapse` signal profile。
- 每个 low/high bit 基础时长仍为 `1x frame_samples`。
- low/high carrier 为 `280 / 560 Hz`。
- 不像 Litany 那样整体变慢；Collapse 的不稳定来自局部停顿和 tremor。

## Silence / Stutter
Collapse 使用 deterministic hash 触发局部结巴 cluster：
- 普通 bit 有较低概率触发 stutter cluster。
- byte tail 有更高概率触发 cluster，因为听起来更像词尾或音节边界卡住。
- cluster 长度为 `2-4 bits`。
- cluster 内每次停顿固定为 `2 slots`。
  - 这是“结巴”的核心：不是每次长短随机，而是同一节拍反复卡住。
- 极少数 panic break 使用 `5 slots`。
  - 表达突然彻底断一下。
- hash 基于 byte index、source byte、bit index 和全局 bit position。
  - 这样同一输入文本每次生成相同 Collapse pattern。
  - 前面插入多少 silence 不会反过来影响后面的 cluster 选择。

## Voicing 方法
- tremor layer。
  - `collapse_tremor_depth` 让 voiced payload 带颤抖。
  - 目标是胆怯和维持不了稳定输出。
- hesitation articulation。
  - bit 尾部会有 near-silent hesitation 处理。
  - 和真实 silence cluster 配合，让音频有“说到一半泄气”的感觉。
- release 更长、envelope floor 较高。
  - 让声音不是硬切，而像失控拖尾。
- 金属层很弱。
  - 避免听起来像 Hostile 的攻击。
- 轻微软削波。
  - 只给崩溃边缘一点不稳定，不做强烈敌意压缩。
- preamble / epilogue 分工。
  - preamble 仍保留较弱 protocol 边界，表达“尝试建立传输”。
  - epilogue 使用 collapse failure shell，改成下坠低频、破碎短句和更早消失的尾部。
  - 这样结尾不再像旧的普通 closure，而像输出失败后逐段塌陷。

## 听感关键词
慌张、颤抖、结巴、局部卡顿、失控、短暂断线。

## 后续调音方向
- 可继续调 stutter cluster 密度和 panic break 概率。
- 不建议让每个 bit 都停，否则会靠近 Litany 的规整吟诵。
