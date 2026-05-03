# Flash Voicing: Litany

更新时间：2026-05-03

## 情绪目标
`Litany` 表达崇敬、祷告、诵经和仪式化流程。它的重点是慢、稳、句读、低频共鸣和收束感。它应该听起来像一个金属喉管在逐拍诵读，而不是普通通信声。

## Signal / Timing
- 使用 `Litany` signal profile。
- 每个 low/high bit 使用 `6x frame_samples`。
- low/high carrier 为 `220 / 440 Hz`。
- 这让单个 tone 本身变长，慢感来自“吟出每个音”，不只是 tone 之间插空白。

## Silence / Cadence
Litany 使用可跳过的整数 slot silence 来表达经文节奏：
- 每个 low/high bit 后默认插入 `1 slot` 静音。
  - visual 上每个 high/low 之间都有真正无波形区。
  - 听感上形成规整的一音一顿。
- 空格 / tab 后使用 `3 slots`。
  - 作为词边界或短呼吸。
- `, ; : ，；：` 后使用 `4 slots`。
  - 作为短句句读。
- 换行后使用 `6 slots`。
  - 作为段落呼吸。
- `. ! ? 。！？` 后使用 `8 slots`。
  - 作为句末大停顿。
- 每 12 byte 的 UTF-8 边界使用 `5 slots`。
  - 作为周期性长呼吸。
  - 会避免切在 UTF-8 continuation byte 中间。

这些 silence 是 payload layout 的一部分，但不承载 bit；decode 时会跳过它们。

## Voicing 方法
- 更长 release 和更高 envelope floor。
  - 每个 tone 不像普通脉冲那样硬断。
  - 在可解码范围内保留一点连贯的吟唱尾部。
- 低频 chant drone。
  - 当前 drone 以 `60 / 120 / 180 Hz` 为核心。
  - gain 较 Steady 更高，用来形成神龛/管腔低鸣。
- 慢速 chant swell。
  - 约 `0.65 Hz`，深度约 `10%`。
  - 表达祷文式呼吸，而不是 tremor。
- 机械喉腔 formant。
  - 叠加很弱的 `160 / 420 / 560 Hz` 共振。
  - 再叠一点 `18 Hz` motor buzz。
  - 目标是机械喉管和金属腔体，而不是人声拟真。
- chant resonance layer。
  - 在主 BFSK 之外添加很弱的共鸣，当前 gain 约 `0.010`。
  - 幅度受控，避免干扰 low/high 判定。
- phrase tail dip。
  - nibble / byte 尾部会更明显地下沉。
  - 当前 phrase tail 从较早位置开始收束，并有较深 dip，用来表达句尾低头收声。
- text-aware pause articulation。
  - 空格和标点对应的 bit 末尾会提前下沉。
  - 让“停顿”不是突然断电，而像诵唱前自然收声。
- 弱 boundary click。
  - Litany 的边界不应像 Hostile 那样攻击。
  - click scale 当前约 `0.18`，让结构存在但不刺耳。
- litany invocation shell。
  - preamble 使用约 `1.35s` 的秒级仪式壳，不再按短 frame 倍数压缩。
  - preamble 使用三次低频金属钟击，绝对时间位置固定为 `0.20s / 0.60s / 1.00s`，三次间隔严格一致，像仪式开始前的三次敲钟。
  - 三次开场钟击使用相同时间、相同基频和相同衰减口径；当前基频约 `104 Hz`，叠加短促金属冲击、非谐波 partial 和更长尾音，目标是“咚……咚……咚……”而不是短促桌面敲击。
  - epilogue 使用约 `1.15s` 的秒级闭礼壳和一长一短二次钟击：第一声约 `92 Hz`、更低、更长，表示经文收束；第二声约 `128 Hz`、更短，表示礼仪闭合。
  - epilogue 的 terminal mute 会更晚介入，避免长钟尾音过早消失。
  - shell 内还会叠加受控 drone，但仍只放在 preamble / epilogue，不进入 payload silence chunk。
  - 前后壳比 Steady / Hostile 更长，让 UI 时长和听感都能明显感到仪式化。

## 听感关键词
慢诵、肃穆、低频、句读、机械喉腔、祷文、收束。

## 后续调音方向
- 可继续打磨 preamble / epilogue 的礼拜短句感。
- 可进一步微调 formant 和 phrase tail，但要保持 silence chunk 真静音。
