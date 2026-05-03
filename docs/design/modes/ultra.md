# `ultra` Mode Design

更新时间：2026-05-04

## 定位
- `ultra` 是面向 UTF-8 文本字节忠实传输的正式模式。
- 它强调更高的信息密度和更纯粹的频点映射结构，适合作为后续更高阶多频模式的 clean 基础。
- 从命名上看它不是 `pro` 的线性升级，而是并列的 UTF-8 / `16-FSK` mode。

## 输入与字符集边界
- 输入文本直接按 UTF-8 byte 处理。
- 面向 UTF-8 文本，不再沿用 `pro` 的 ASCII-only 约束。
- 不额外做人类语义转换；核心语义是“把 UTF-8 字节按 nibble 拆开，再映射到固定频点”。

## 信息层
- 每个 byte 拆成两个 nibble。
- 每个 nibble 映射成一个独立 symbol。
- 因此 `1 byte = 2 symbol`。
- 它和 `flash` 的差别在于：
  - `flash` 逐 bit 发 low/high BFSK
  - `ultra` 逐 nibble 发单频 `16-FSK` symbol
- 它和 `pro` 的差别在于：
  - `pro` 每个 symbol 是一对低/高双音
  - `ultra` 每个 symbol 只发一个固定频点

## 音频层
- 使用 clean `16-FSK`。
- 每个 nibble 映射到一个固定频点。
- 每个 symbol 只发一个频点。
- decode 时按相同频点表回收 nibble，再重组为原始 UTF-8 byte 序列。

## 听感与工程意图
- `ultra` 比 `pro` 更偏高密度频点映射，而不是正式双音。
- 它的目标不是风格化情绪表达，而是更直接地把 UTF-8 文本字节组织成 `16-FSK` clean payload。
- 对 LLM / agent 来说，最稳定的理解方式是：
  - `ultra = UTF-8 byte -> two nibbles -> clean 16-FSK`

## 主链路文件
- `libs/audio_core/src/ultra/codec.cpp`
- `libs/audio_core/src/ultra/phy_clean.cpp`
- transport 分发入口：
  - `libs/audio_core/src/transport/transport.cpp`
- C API 边界：
  - `libs/audio_api/src/bag_api.cpp`

## 相关入口
- 总览 / 对比见 [`../transports.md`](../transports.md)
- 文件地图见 [`../../architecture/repo-map.md`](../../architecture/repo-map.md)
