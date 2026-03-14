# `flash / pro / ultra` 模式设计

更新时间：2026-03-14

## 总体原则
- 三种模式按 mode-first 架构解耦。
- 每种模式内部至少区分：
  - 信息层：文本 / 字节 / symbol 的变换
  - clean PHY：symbol 与 PCM 的互转
- 当前先保证“生成音频 -> 解析生成音频”闭环。
- 当前不做风格层、抗干扰、FEC、自动拆帧或复杂同步搜索。

## 字符集约束一览
- `flash`
  - 不限字符集。
  - 输入按原始字节直通处理，不额外要求必须是 ASCII 或 UTF-8。
- `pro`
  - 仅允许 ASCII。
  - 任何非 ASCII 输入都应视为不合法。
- `ultra`
  - 面向 UTF-8 文本使用。
  - 输入文本按 UTF-8 byte 进入后续 nibble / PHY 链路。

## `flash`

### 定位
- 娱乐化、仪式感优先的原始直通模式。
- 不强调效率，不引入协议帧。

### 信息层
- 输入文本直接按原始字节处理。
- 不限字符集；ASCII、中文、emoji 或混合文本都可进入该模式。
- 当前不对 UTF-8 再做额外协议转换。
- `flash` 传输的本质不是“字符语义”，而是“原始字节的 bit 串”：
  - 先把输入文本按当前 `std::string` 中已有的字节序列取出。
  - 再把每个 byte 拆成 `8` 个 bit。
  - 最后把每个 bit 交给 clean `BFSK`：
    - `0` 发低频
    - `1` 发高频
- 因此 `flash` 只关心“字节长什么样”，不关心这些字节在上层是不是 ASCII、UTF-8，还是别的编码表示。
- 反向解析时也一样：
  - 先从高频/低频还原 bit
  - 再每 `8` 个 bit 拼回一个 byte
  - 最后按原始 byte 序列还原文本
- 这也是为什么 `flash` 可以写成“不限字符集”：
  - 它不是理解所有字符集的语义，而是对输入 byte 做透明传输。
- 例子：
  - ASCII `"A"`
    - byte 通常是 `0x41`
    - bit 串是 `01000001`
    - `flash` 实际发送的是这 `8` 个 bit 对应的高/低频序列
  - 中文 `u8"你"`
    - UTF-8 byte 通常是 `0xE4 0xBD 0xA0`
    - 总共会拆成 `24` 个 bit
    - `flash` 不会把它当“中文字符语义”处理，只会把这 `3` 个 byte 逐 bit 发送
  - emoji `u8"🚀"`
    - UTF-8 byte 通常是 `0xF0 0x9F 0x9A 0x80`
    - 总共会拆成 `32` 个 bit
    - `flash` 仍然只是把这 `4` 个 byte 原样映射成 bit 再发出去
- 所以更准确的表述是：
  - `flash` 不限制输入字节内容
  - `flash` 按原始字节透明传输
  - `flash` 不额外承担字符集校验或字符语义转换

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
- 任何非 ASCII 输入都必须在校验阶段被拒绝。
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
- 面向 UTF-8 文本，不再沿用 `pro` 的 ASCII-only 约束。
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
