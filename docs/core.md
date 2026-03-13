# Audio Core 现状

更新时间：2026-03-13

## 版本更新说明
- [v0.3.2](./core/v0.3.2.md)
- [v0.3.1](./core/v0.3.1.md)
- [v0.3.0](./core/v0.3.0.md)
- [v0.2.2](./core/v0.2.2.md)
- [v0.2.1](./core/v0.2.1.md)
- [v0.2.0](./core/v0.2.0.md)
- [v0.1.1](./core/v0.1.1.md)
- [v0.1.0](./core/v0.1.0.md)

## 相关文档
- [文档索引](./README.md)
- [仓库结构与文件地图](./architecture/repo-map.md)
- [模式设计说明](./design/transports.md)
- [测试说明](./testing.md)

## 版本信息
- 当前 `bag_core_version()` 返回：`0.3.1`
- 当前仓库状态：host 主线已完成 modules-only 收口；Android 例外被隔离在独立 packaging lane

## 目录与职责
- `libs/audio_core/modules/bag/**`
  - `audio_core` 当前正式内部主线；host 默认路径直接消费这些 named modules。
- `libs/audio_core/src/**`
  - 对应 module implementation units；主仓 `src/*.cpp` 已不再为 root host `WAVEBITS_HOST_MODULES=OFF` 保留 fallback。
- `libs/audio_core/include/bag/interface/common/*`
  - 当前作为长期保留的 reserved interface declaration layer，供 `link/*` 与 `phy/*` 预留接口头的 `C++17` fallback 使用；不进入 `import std;` 扩面。
- `libs/audio_core/include/bag/phy/**`、`libs/audio_core/include/bag/link/**`
  - 预留接口层（`IFunPhy`、`IProPhy`、`ILinkLayer`）；尚未接入当前产品主链路。
- `libs/audio_api`
  - `bag_api.h` 继续作为稳定 C ABI；`bag_api.cpp` 的主仓实现已切到 modules-only host 形态。
- `apps/audio_android/native_package`
  - Android 专用 packaging lane；通过 package-private wrapper 与 `android_bag/**` 私有声明层隔离剩余 `C++17` 平台例外。
- `libs/audio_io`
  - `audio_io.wav` + `wav_io.h` 的双入口文件 I/O 边界。

## 当前已实现能力
1. `transport facade + flash/pro/ultra` 的 mode-first 内部主线。
2. `flash` 的原始文本/字节直通 + clean `BFSK` PHY。
3. `pro` 的 ASCII byte + `DTMF-like` 双音 clean PHY。
4. `ultra` 的 UTF-8 byte + clean `16-FSK` PHY。
5. Pipeline 最小闭环：`PushPcm -> PollTextResult -> Reset`。
6. 稳定 C API 编码/解码入口：
   - `bag_encode_text`
   - `bag_free_pcm16_result`
   - `bag_create_decoder`
   - `bag_push_pcm`
   - `bag_poll_result`
   - `bag_reset`
   - `bag_destroy_decoder`
   - `bag_core_version`

## 当前边界
1. host 默认路径就是 named modules 主路径。
2. root host `WAVEBITS_HOST_MODULES=OFF` 已退休，不再是受支持的 host lane。
3. 主仓 `bag_api.cpp` 与 `libs/audio_core/src/*.cpp` 已完成这轮 modules-only cutover，不再保留主仓 internal fallback headers。
4. Android 仍是显式 `C++17` 平台例外，但例外已被限制在 `apps/audio_android/native_package/` 的私有包装层里，不再直接 source-own 主仓原始实现文件。
5. 主仓 `bag/internal/**` owner 现为 `0`；预留接口头的 `C++17` 声明责任已独立固定到 `libs/audio_core/include/bag/interface/common/*`，并按长期保留的 reserved-interface boundary 管理。
6. `bag/legacy/**` 已删除；legacy path / include token 回流由 `retirement_policy.py` 阻止。
7. `phy/pro`、`phy/fun`、`link` 仍是接口层，不属于当前产品主路径。
8. 尚未包含前向纠错、重传策略、复杂同步与抗噪优化。
9. 结果置信度目前仍为简化值，后续可演进为真实质量估计模型。

## 模式实现与编码

### `flash`
- 定位：娱乐化、强调仪式感与二进制氛围的原始直通模式。
- 信息层：输入文本直接按原始字节处理；当前实现对 `std::string` 中的 UTF-8 字节不做额外协议封装。
- 编码方式：每个 bit 映射为一段固定时长的 `BFSK`，`0` 使用低频、`1` 使用高频。
- 结构特征：`1 byte = 8 bit symbol`，无帧头、无校验、无长度字段。

### `pro`
- 定位：ASCII-only 的正式模式。
- 信息层：输入必须为 ASCII 文本；文本先转为 ASCII byte。
- 编码方式：每个 byte 拆成高 4 bit 和低 4 bit；每个 nibble 映射为一个 `DTMF-like` 双音 symbol。
- 结构特征：`1 byte = 2 symbol`；每个 symbol 同时包含一组低频和一组高频，不使用额外 frame/CRC。

### `ultra`
- 定位：面向 UTF-8 文本字节忠实传输的正式模式。
- 信息层：输入文本直接按 UTF-8 byte 处理，不额外做人类语义转换。
- 编码方式：每个 byte 拆成两个 nibble；每个 nibble 映射到 `16-FSK` 的一个固定频点。
- 结构特征：`1 byte = 2 symbol`；每个 symbol 只发一个频点。

## MVP 暂不做
- [x] `pro` 当前只做 `ASCII byte -> nibble -> DTMF-like 双音` 的 clean 闭环。
- [x] `ultra` 当前只做 `UTF-8 byte -> nibble -> clean 16-FSK` 的 clean 闭环。
- [ ] 自动拆帧、多帧重组、长文本流式会话管理不在当前 MVP 范围内。
- [ ] style layer 暂不实现。
- [x] `flash` 当前不做 style layer，不加入频率变化、立体声、节奏强化、音色设计或背景氛围处理。

## 对外集成建议
1. Android、Windows 等平台继续通过 `bag_api.h` 调用 core，不直接依赖内部 modules。
2. 文件 I/O 继续通过 `wav_io.h` / `audio_io.wav` 承接，不把平台壳层拉进内部实现细节。
3. 新能力优先加在 `audio_core` modules 与对应 implementation units，平台层继续保持壳层职责。
