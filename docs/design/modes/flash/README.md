# `flash` Mode Design

更新时间：2026-05-04

## 定位
- `flash` 是娱乐化、仪式感优先的原始直通模式。
- 它不是为了最短时长或最高吞吐，而是刻意保留“文本像被播放、被宣读、被仪式化发声”的听感。
- 从技术映射看，它最接近 bit-by-bit `BFSK`，但实际产品语义里又叠加了 emotion voicing、payload cadence 和非 payload shell。

## 信息层
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

## 音频层
- 使用 clean `BFSK`。
- `0` 对应低频。
- `1` 对应高频。
- `1 byte = 8 bit symbol`。
- `bag.flash.signal` 负责 clean payload render / decode 与 payload layout。
- `bag.flash.voicing` 负责 emotion voicing、固定 preamble / epilogue、payload texture、可跳过 silence、trim descriptor 与 trim payload。

## Emotion / Style 结构
- formal `flash` 当前对外暴露六个用户可见 emotion preset：
  - `Steady`
  - `Hostile`
  - `Litany`
  - `Collapse`
  - `Zeal`
  - `Void`
- Android 当前仍使用一个 flash voicing 选择器，但每个 preset 内部同时指定：
  - `signalProfileValue`
  - `voicingFlavorValue`
- 也就是说，`flash` 的 style 不是单纯“换一个语气标签”，而是同时决定：
  - payload bit 的基础时长与 low/high 组织方式
  - preamble / epilogue 和 payload texture 的情绪化声效

## 当前 Signal Profile 速记
- `Steady`: `0.9375x frame_samples`
- `Hostile`: `0.875x frame_samples`
- `Litany`: `6x frame_samples`，可跳过 silence slot 为 `1x frame_samples`
- `Collapse`: `1x frame_samples`
- `Zeal`: variable `0.5x` / `0.625x` / `0.75x` / `1x frame_samples` bit windows, with `1x frame_samples` silence slots for punctuation pauses
- `Void`: `2.5x frame_samples`

## Decode 边界
- decode 入口会先按 voicing trim descriptor 去掉非 payload 区域，再进入 `bag.flash.signal` 解调。
- `Litany` / `Collapse` 的变长 silence payload 使用 gap-aware decode 跳过静音。
- `Zeal` 使用自己的确定性变速 / 变频 gap-aware decode。
- `Void` 继续使用普通 low/high window 判定。
- 所有 emotion voicing 都必须服从这个边界：
  - 不把 payload 语义藏进 texture / drone / shell
  - 不让 preamble / epilogue 参与 payload decode
  - silence chunk 必须保持可被 decode 稳定跳过

## 主链路文件
- `libs/audio_core/src/flash/codec.cpp`
- `libs/audio_core/src/flash/signal.cpp`
- `libs/audio_core/src/flash/voicing.cpp`
- `libs/audio_core/src/flash/phy_clean.cpp`
- transport 分发入口：
  - `libs/audio_core/src/transport/transport.cpp`
- C API 边界：
  - `libs/audio_api/src/bag_api.cpp`

## 相关入口
- emotion 总览见 [`voicing-emotions.md`](voicing-emotions.md)
- preset 细节见当前目录下的逐 preset 文档
- 总览 / 对比见 [`../../transports.md`](../../transports.md)
- 文件地图见 [`../../../architecture/repo-map.md`](../../../architecture/repo-map.md)
