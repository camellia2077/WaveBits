# `mini / flash / pro / ultra` 模式设计

更新时间：2026-04-30

## 总体原则
- 四种模式按 mode-first 架构解耦。
- 每种模式内部至少区分：
  - 信息层：文本 / 字节 / symbol 的变换
  - clean PHY：symbol 与 PCM 的互转
- 当前先保证“生成音频 -> 解析生成音频”闭环。
- 当前不做复杂风格层、抗干扰、FEC、自动拆帧、录音环境 decode 或复杂同步搜索。

## 字符集约束一览
- `flash`
  - 不限字符集。
  - 输入按原始字节直通处理，不额外要求必须是 ASCII 或 UTF-8。
  - 当前公共 API 入口仍是字符串接口，不是通用二进制 blob 接口。
- `pro`
  - 仅允许 ASCII。
  - 任何非 ASCII 输入都应视为不合法。
- `ultra`
  - 面向 UTF-8 文本使用。
  - 输入文本按 UTF-8 byte 进入后续 nibble / PHY 链路。
- `mini`
  - Morse code 模式。
  - 仅支持 `A-Z / 0-9 / space / 常见 Morse 标点`。
  - 小写会在信息层规范化为大写；连续空格会折叠为单个空格，首尾空格不进入 payload。
  - 不做录音 decode 鲁棒性承诺；当前目标是生成音频与程序内 decode 的闭环。

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
- 但当前还需要补一条边界说明：
  - `flash` 的核心语义是“字节透明传输”，不等于“公共 API 可直接承接任意二进制 blob”
  - 现有 `bag_api.h` / CLI / Android 主链路入口使用的仍是字符串语义输入
  - 因此最稳定、最明确的使用方式仍然是普通文本
  - 含内嵌 `NUL` 的数据不属于当前公共输入边界

### 音频层
- 使用 clean `BFSK`
- `0` 对应低频
- `1` 对应高频
- `1 byte = 8 bit symbol`
- `bag.flash.signal` 负责 clean payload render / decode 与 payload layout
- `bag.flash.signal` 当前通过 `flash_signal_profile` 派生 payload timing：
  - `Steady`: `0.9375x frame_samples`
  - `Hostile`: `0.875x frame_samples`
  - `Litany`: `6x frame_samples`，可跳过 silence slot 为 `1x frame_samples`
  - `Collapse`: `1x frame_samples`
  - `Zeal`: variable `0.5x` / `0.625x` / `0.75x` / `1x frame_samples` bit windows, with `1x frame_samples` silence slots for punctuation pauses
  - `Void`: `2.5x frame_samples`
- `bag.flash.voicing` 负责 emotion voicing、固定 preamble / epilogue、payload texture、可跳过 silence、trim descriptor 与 trim payload。
- formal `flash` 当前对外暴露六个用户可见 emotion preset：`Steady / Hostile / Litany / Collapse / Zeal / Void`。
- Android 仍使用一个 flash voicing 选择器，但每个 preset 内部同时指定 `signalProfileValue` 与 `voicingFlavorValue` 两轴。
- decode 入口当前会先按 voicing trim descriptor 去掉非 payload 区域，再进入 `bag.flash.signal` 解调；`Litany` / `Collapse` 的变长 silence payload 使用 gap-aware decode 跳过静音，`Zeal` 使用自己的确定性变速 / 变频 gap-aware decode，`Void` 继续使用普通 low/high window 判定。
- flash emotion 的总览见 `docs/design/flash-voicing-emotions.md`；具体 preset 的当前声效方法与后续调音方向见 `docs/design/flash-voicing/<preset>.md`。

### 主链路文件
- `libs/audio_core/src/flash/codec.cpp`
- `libs/audio_core/src/flash/signal.cpp`
- `libs/audio_core/src/flash/voicing.cpp`
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

## `mini`

### 定位
- Morse code 文本音频模式。
- 它不是 `pro` 的更强版本，而是面向“点 / 划 / 静音间隔”这种经典电报码听感的独立 transport。
- `mini` 当前优先追求清晰、可解释、可视化友好的 Morse 表达；不做录音环境 decode、抗噪同步搜索或纠错。
- Android 使用 `pro` 的示例文本族作为 `mini` 示例来源，但 `mini` 会按 Morse 规则校验和规范化输入。

### 输入规范
- 支持：
  - `A-Z`
  - `0-9`
  - space
  - 常见 International Morse punctuation：`.,?'!/()&:;=+-_"$@`
- 小写输入会自动转成大写。
- 多个连续空格会折叠为一个空格。
- 开头和结尾的空格不会进入 payload。
- 非支持字符应在校验阶段失败，公共 API 通过 `BAG_VALIDATION_MINI_MORSE_ONLY` 暴露失败语义。
- Android 输入区会显示 normalization 预览、Morse 点划预览与 unsupported character 提示；这些 UI 提示是编辑辅助，不改变 core 的最终校验权威。

### 信息层
- 输入文本先规范化为 Morse-compatible text。
- 规范化后的每个字符仍以 byte 形式保存到 payload 中：
  - 字母 / 数字 / 标点保存其 ASCII byte
  - space 保存为 `0x20`
- 与 `flash` 不同，`mini` 不是原始 byte 透明传输；它只接受 Morse 表中存在的字符。
- 与 `pro` 不同，`mini` 的 payload byte 不是再拆成 nibble symbol，而是通过 Morse 表映射到 dot / dash pattern。
- decode 后返回的是规范化文本：
  - 例如 `"praise   the omnissiah"` roundtrip 后返回 `"PRAISE THE OMNISSIAH"`。

### 音频层
- 使用 clean Morse tone PHY。
- 基本单位为 `frame_samples`：
  - dot：`1` unit tone
  - dash：`3` unit tone
  - 同一字符内部 dot/dash 之间：`1` unit silence
  - 字符之间：`3` unit silence
  - 单词之间：`7` unit silence
- `mini` 的静音是协议语义的一部分，不是装饰层；visual 和 follow 都应该把 silence 留空，而不是画成 tone。
- Android speed preset 通过改变 `frame_samples` 控制 unit 长度：
  - `Slow`: `1.5x`
  - `Standard`: `1.0x`
  - `Fast`: `0.5x`
- WAV metadata 保存实际使用的 `frame_samples`，这样保存后的音频重新加载时仍能按当时的 Morse speed 对齐播放、visual 与 decode。

### Follow / Visual
- `BuildPayloadFollowData` 对 `mini` 发布两层 timeline：
  - `byte_timeline`：按规范化字符跟随，字符跨度包含该字符 tone 与后续协议 silence。
  - `binary_group_timeline`：按 Morse tone element 跟随，只发布 dot / dash，不发布 silence。
- Text lyrics 使用规范化后的文本，因此小写和连续空格输入不会导致 lyrics token 与 payload byte 错位。
- Morse lyrics 显示 dot/dash pattern，例如 `.--. .-. .- .. ... .`，按 dot / dash element 高亮。
- Android 的 Morse Timeline Visual 使用朴素 block：
  - dot / dash 是实心块
  - dash 比 dot 更长
  - silence gap 保持空白
  - 不显示无意义背景竖线

### 主链路文件
- `libs/audio_core/src/mini/codec.cpp`
- `libs/audio_core/src/mini/phy_clean.cpp`
- `libs/audio_core/src/transport/follow.cpp`
- `libs/audio_core/src/transport/transport.cpp`
- Android package lane 对应：
  - `apps/audio_android/native_package/src/audio_core_mini_codec.cpp`
  - `apps/audio_android/native_package/src/audio_core_mini_phy_clean.cpp`

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
