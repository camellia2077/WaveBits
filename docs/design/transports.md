# `flash / pro / ultra` 模式设计

更新时间：2026-03-08

## 总体原则
- 三种模式按 mode-first 架构解耦。
- 每种模式内部至少区分：
  - 信息层：文本 / 字节 / symbol 的变换
  - clean PHY：symbol 与 PCM 的互转
- 当前先保证“生成音频 -> 解析生成音频”闭环。
- 当前不做风格层、抗干扰、FEC、自动拆帧或复杂同步搜索。

## `flash`

### 定位
- 娱乐化、仪式感优先的原始直通模式。
- 不强调效率，不引入协议帧。

### 信息层
- 输入文本直接按原始字节处理。
- 当前不对 UTF-8 再做额外协议转换。

### 音频层
- 使用 clean `BFSK`
- `0` 对应低频
- `1` 对应高频
- `1 byte = 8 bit symbol`

### 主链路文件
- `libs/audio_core/src/flash/codec.cpp`
- `libs/audio_core/src/flash/phy_clean.cpp`

## `pro`

### 定位
- ASCII-only 的正式模式。
- 目标是结构清晰、职责单纯，适合作为正式协议/链路层的后续基础。

### 信息层
- 输入必须是 ASCII 文本。
- 文本先转 ASCII byte。
- 每个 byte 拆成：
  - 高 4 bit
  - 低 4 bit

### 音频层
- 使用 `DTMF-like` 双音 clean PHY。
- 每个 nibble 映射为一个 symbol。
- 每个 symbol 同时包含：
  - 一组低频
  - 一组高频
- `1 byte = 2 symbol`

### 主链路文件
- `libs/audio_core/src/pro/codec.cpp`
- `libs/audio_core/src/pro/phy_clean.cpp`

## `ultra`

### 定位
- 面向 UTF-8 文本字节忠实传输的正式模式。
- 适合作为后续更高阶多频模式的基础。

### 信息层
- 输入文本直接按 UTF-8 byte 处理。
- 不额外做人类语义转换。
- 每个 byte 拆成两个 nibble。

### 音频层
- 使用 clean `16-FSK`
- 每个 nibble 映射到一个固定频点
- `1 byte = 2 symbol`
- 每个 symbol 只发一个频点

### 主链路文件
- `libs/audio_core/src/ultra/codec.cpp`
- `libs/audio_core/src/ultra/phy_clean.cpp`

## 外部统一入口
- transport 分发：
  - `libs/audio_core/src/transport/transport.cpp`
- C API：
  - `libs/audio_api/src/bag_api.cpp`

## 当前明确不做
- style layer
- FEC / CRC / 重传
- 多帧拆分与长文本协议
- 真实环境同步搜索
- 噪声、衰减、截断下的鲁棒性承诺
