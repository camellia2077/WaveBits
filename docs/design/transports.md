# `mini / flash / pro / ultra` 模式总览

更新时间：2026-05-04

## 目的
- 提供四种 mode 的总览、对比和快速跳转。
- 把详细设计下沉到 `docs/design/modes/`，避免总览页继续承担所有 mode 的细节。
- 为 README、AGENTS 和 repo map 提供稳定的 mode-first 入口。

## 总体原则
- 四种模式按 mode-first 架构解耦。
- 每种模式内部至少区分：
  - 信息层：文本 / 字节 / symbol 的变换
  - clean PHY：symbol 与 PCM 的互转
- 当前先保证“生成音频 -> 解析生成音频”闭环。
- 当前不做复杂风格层、抗干扰、FEC、自动拆帧、录音环境 decode 或复杂同步搜索。

## 命名映射
- `mini` -> Morse code
- `flash` -> bit-by-bit `BFSK`
- `pro` -> `DTMF-like` dual-tone mapping
- `ultra` -> clean `16-FSK`

这些名字是项目内部的产品化命名，不是同一协议的“基础版 / 高级版 / 终极版”线性升级关系。

## 字符集约束一览
| Mode | 字符集 / 输入边界 | 主要信息结构 | 主要音频结构 |
| --- | --- | --- | --- |
| `flash` | 不限字符集；输入按原始字节直通处理；公共入口仍是字符串接口 | `1 byte -> 8 bit` | low/high `BFSK` |
| `mini` | Morse-compatible text；支持 `A-Z / 0-9 / space / 常见 Morse 标点` | text -> dot/dash pattern | Morse tone + protocol silence |
| `pro` | 仅允许 ASCII | `1 byte -> 2 nibble` | `DTMF-like` dual-tone |
| `ultra` | 面向 UTF-8 文本，按 UTF-8 byte 处理 | `1 byte -> 2 nibble` | clean `16-FSK` |

## 模式跳转

### `flash`
- 定位：娱乐化、仪式感优先的原始直通模式。
- 核心：按原始字节逐 bit 发 low/high `BFSK`，再通过 timing、carrier 和 voicing shell 制造情绪化“说话语气”。
- 详细设计：
  - [`docs/design/modes/flash/README.md`](modes/flash/README.md)
  - [`docs/design/modes/flash/voicing-emotions.md`](modes/flash/voicing-emotions.md)
  - [`docs/design/modes/flash/`](modes/flash/)

### `mini`
- 定位：Morse code 文本音频模式，强调点 / 划 / 静音间隔的可听、可视、可跟随表达。
- 核心：文本先规范化为 Morse-compatible text，再按 dot / dash / silence unit 渲染。
- 详细设计：
  - [`docs/design/modes/mini.md`](modes/mini.md)

### `pro`
- 定位：ASCII-only 的正式模式。
- 核心：ASCII byte 拆成高低 nibble，再映射成 `DTMF-like` 双音 symbol。
- 详细设计：
  - [`docs/design/modes/pro.md`](modes/pro.md)

### `ultra`
- 定位：面向 UTF-8 文本字节忠实传输的正式模式。
- 核心：UTF-8 byte 拆成两个 nibble，再映射到 clean `16-FSK` 固定频点。
- 详细设计：
  - [`docs/design/modes/ultra.md`](modes/ultra.md)

## 外部统一入口
- transport 分发：
  - `libs/audio_core/src/transport/transport.cpp`
- C API：
  - `libs/audio_api/src/bag_api.cpp`

## 当前明确不做
- 随机化或不可预测的 style layer，例如可变长度背景层、随机前导/收尾
- FEC / CRC / 重传
- 多帧拆分与长文本协议
- 真实环境同步搜索
- `mini` 的录音环境 decode 鲁棒性
- 噪声、衰减、截断下的鲁棒性承诺
