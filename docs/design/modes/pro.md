# `pro` Mode Design

更新时间：2026-05-04

## 定位
- `pro` 是 ASCII-only 的正式模式。
- 它强调结构清晰、职责单纯和规则明确，适合作为正式协议 / 链路层表达的 clean 基线。
- 从命名上看它不是“更高级的通用版本”，而是与 `flash / mini / ultra` 并列的一种 mode。

## 输入与字符集边界
- 输入必须是 ASCII 文本。
- 文本先转成 ASCII byte。
- 任何非 ASCII 输入都必须在校验阶段被拒绝。
- 它不承接 UTF-8 任意字节文本；如果目标是 UTF-8 byte 忠实传输，应使用 `ultra`。

## 信息层
- 每个 ASCII byte 拆成：
  - 高 `4` bit
  - 低 `4` bit
- 每个 nibble 都会被映射成一个独立 symbol。
- 因此 `1 byte = 2 symbol`。
- `pro` 当前不额外引入 frame header、CRC、FEC 或多帧流式协议。

## 音频层
- 使用 `DTMF-like` 双音 clean PHY。
- 每个 nibble 映射为一个 symbol。
- 每个 symbol 同时包含：
  - 一组低频
  - 一组高频
- decode 时按同一映射表回收 nibble，再重组为原始 ASCII byte。

## 听感与工程意图
- `pro` 的重点不是情绪化表达，而是正式、干净、规则化的结构。
- 它比 `flash` 更少依赖风格化 voicing，也不像 `mini` 那样把 silence 本身当成协议语义主角。
- 对 LLM / agent 来说，最稳定的理解方式是：
  - `pro = ASCII byte -> nibble -> DTMF-like dual-tone symbol`

## 主链路文件
- `libs/audio_core/src/pro/codec.cpp`
- `libs/audio_core/src/pro/phy_clean.cpp`
- transport 分发入口：
  - `libs/audio_core/src/transport/transport.cpp`
- C API 边界：
  - `libs/audio_api/src/bag_api.cpp`

## 相关入口
- 总览 / 对比见 [`../transports.md`](../transports.md)
- 文件地图见 [`../../architecture/repo-map.md`](../../architecture/repo-map.md)
