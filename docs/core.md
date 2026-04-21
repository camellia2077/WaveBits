# Audio Core 现状

更新时间：2026-03-15

## 版本更新说明
- [v0.4.0](./core/v0.4/v0.4.0.md)
- [v0.3.3](./core/v0.3/v0.3.3.md)
- [v0.3.2](./core/v0.3/v0.3.2.md)
- [v0.3.1](./core/v0.3/v0.3.1.md)
- [v0.3.0](./core/v0.3/v0.3.0.md)
- [v0.2.2](./core/v0.2/v0.2.2.md)
- [v0.2.1](./core/v0.2/v0.2.1.md)
- [v0.2.0](./core/v0.2/v0.2.0.md)
- [v0.1.1](./core/v0.1/v0.1.1.md)
- [v0.1.0](./core/v0.1/v0.1.0.md)

## 相关文档
- [文档索引](./README.md)
- [仓库结构与文件地图](./architecture/repo-map.md)
- [模式设计说明](./design/transports.md)
- [测试说明](./testing.md)

## 版本信息
- 当前 `bag_core_version()` 返回：`0.3.3`
- 当前仓库状态：host 主线已基本完成 `module-first` 收口；Android 例外被隔离在独立 packaging lane

## 目录与职责
- `libs/audio_core/modules/bag/**`
  - `audio_core` 当前正式内部主线；host 默认路径直接消费这些 named modules。
- `libs/audio_core/src/**`
  - 对应 module implementation units；root host 当前直接固定为单主线 modules 路径，不再暴露独立的 host 开关。
- `libs/audio_core/include/bag/interface/common/*`
  - 当前作为长期保留的 reserved interface declaration layer，供 `link/*` 与 `phy/*` 预留接口头的 `C++17` fallback 使用；不进入 `import std;` 扩面。
- `libs/audio_core/include/bag/phy/**`、`libs/audio_core/include/bag/link/**`
  - 预留接口层（`IFunPhy`、`IProPhy`、`ILinkLayer`）；尚未接入当前产品主链路。
- `libs/audio_api`
  - `bag_api.h` 继续作为稳定 C ABI；`bag_api.cpp` 的主仓实现已切到 modules-only host 形态，并已纳入当前 host-side `import std;` required baseline。
- `apps/audio_android/native_package`
  - Android 专用 packaging lane；当前通过 `audio_core` package-owned implementation sources、`bag_api` / `audio_runtime` package-owned boundary implementation、`audio_io` package-private wrapper 与 `android_bag/**` / `android_audio_io/**` 私有声明层隔离剩余 Android 平台例外，并已固定到 Android `C++23` native baseline。
- `libs/audio_io`
  - `audio_io.wav` + `wav_io.h` 的双入口 WAV 边界；同时承接“内存 bytes <-> mono PCM16 WAV”和“文件路径 <-> WAV 文件”两类能力，内部优先走 module front-end，但稳定 header boundary 与 backend containment 不强推成 pure module。

## 当前 `module-first` 程度
1. 对 host 主线来说，当前已经是“明确的 `module-first`”，而不是“module 只是可选实验路径”。
2. `audio_core` 的内部主链路已经以 named modules 为主：
   - 新增内部能力默认应先落在 `libs/audio_core/modules/bag/**`
   - `bag_api.cpp` 这类 host 边界后的实现层也已经直接消费 modules
3. 但当前并不是“所有代码都必须 module-only”：
   - 稳定 C ABI `bag_api.h` 继续保留
   - 稳定文件 I/O 边界 `wav_io.h` 继续保留
   - Android 仍是独立 `C++23` packaging lane
   - `audio_io` 的 backend owner 继续保持 include-based
4. 因此当前更准确的表述是：
   - FlipBits host 主线已经基本做到 `module-first`
   - 但长期边界、平台例外和 third-party/backend owner 不属于“必须纯 module 化”的范围

## 当前已实现能力
1. `transport facade + flash/pro/ultra` 的 mode-first 内部主线。
2. `flash` 的原始文本/字节直通 + clean `BFSK` signal 层 + `phy_clean` facade。
3. `flash` 当前已通过 `CoreConfig.flash_style` / `bag_api.h` 正式公开 `coded_burst` 与 `ritual_chant` 两种 style：`bag.flash.signal` 负责按 style 派生不同的 payload `samples_per_bit`，`bag.flash.voicing` 负责按同一 style 派生固定 preamble / epilogue、payload voicing、三段式/两段式短停顿与 trim descriptor。正式默认仍为 `coded_burst`；`ritual_chant` 则在保留 decode 自洽的前提下，同时拉长 payload timing、前后壳时长与仪式化外观。
4. `pro` 的 ASCII byte + `DTMF-like` 双音 clean PHY。
5. `ultra` 的 UTF-8 byte + clean `16-FSK` PHY。
6. Pipeline 最小闭环：`PushPcm -> PollTextResult -> Reset`。
7. 稳定 C API 编码/解码入口：
   - `bag_encode_text`
   - `bag_free_pcm16_result`
   - `bag_create_decoder`
   - `bag_push_pcm`
   - `bag_poll_result`
   - `bag_reset`
   - `bag_destroy_decoder`
   - `bag_core_version`
8. 稳定 WAV 边界能力：
   - `SerializeMonoPcm16Wav`
   - `ParseMonoPcm16Wav`
   - `WriteMonoPcm16Wav`
   - `ReadMonoPcm16Wav`

## 当前边界
1. host 默认路径就是 named modules 主路径。
2. root host 当前直接固定为 `clang++ + Ninja + build/dev` 这一条受支持主线。
3. 主仓 `bag_api.cpp` 与 `libs/audio_core` 主实现面已经完成这轮 `module-first` cutover，不再保留主仓 internal fallback headers 作为正式路径。
4. Android 仍是显式平台例外，但例外已被限制在 `apps/audio_android/native_package/` 的私有包装层里，并已固定到 Android `C++23` native baseline；当前 `audio_core` 与 `bag_api` 都已收成 package-owned implementation，不再直接 source-own 主仓原始实现文件。
5. 主仓 `bag/internal/**` owner 现为 `0`；预留接口头的 `C++17` 声明责任已独立固定到 `libs/audio_core/include/bag/interface/common/*`，并按长期保留的 reserved-interface boundary 管理。
6. `bag/legacy/**` 已删除；legacy path / include token 回流由 `retirement_policy.py` 阻止。
7. `phy/pro`、`phy/fun`、`link` 仍是接口层，不属于当前产品主路径。
8. 尚未包含前向纠错、重传策略、复杂同步与抗噪优化。
9. 结果置信度目前仍为简化值，后续可演进为真实质量估计模型。

## 为什么 `audio_io` 没有全做成 pure module
1. `audio_io` 不是“没做 modules”，而是已经采用“module front-end + stable header boundary + private backend owner”的混合结构：
   - `libs/audio_io/modules/audio_io/wav.cppm`
   - `libs/audio_io/modules/audio_io/wav_impl.cpp`
   - `libs/audio_io/include/wav_io.h`
   - `libs/audio_io/src/wav_io.cpp`
   - `libs/audio_io/src/wav_io_backend.h`
   - `libs/audio_io/src/wav_io_backend.cpp`
   - `libs/audio_io/src/wav_io_bytes_impl.inc`
2. `audio_io.wav` 已经是 host 内部优先入口，所以 `audio_io` 已经纳入当前 `module-first` 主线。
3. 没有继续强推成 pure module，主要是因为：
   - `wav_io.h` 需要继续承担稳定的 header boundary，同时对外暴露文件路径和内存 bytes 两类能力
   - Android 需要复用同一套 WAV bytes 逻辑，但又不能直接公开依赖 `audio_io/include`
   - `wav_io_backend.cpp` 仍是唯一批准的 backend owner / third-party containment 位置
   - backend declaration 需要保持在适合 global-fragment / include-based 的位置，避免把 backend 细节直接抬进 exported module interface
4. 当前设计目标不是“让 `audio_io` 看起来 100% module-only”，而是：
   - 让 host 内部消费优先走 `audio_io.wav`
   - 同时把稳定 header boundary、WAV bytes 纯逻辑和 backend containment 维持在清晰、可控的位置

## host-side `import std;` required baseline
- 当前 required baseline 覆盖：
  - `libs/audio_core/src/**` 中当前批准的 host implementation units
  - `libs/audio_core/modules/bag/common/config.cppm`
  - `libs/audio_core/modules/bag/common/types.cppm`
  - `libs/audio_core/modules/bag/flash/codec.cppm`
  - `libs/audio_core/modules/bag/flash/signal.cppm`
  - `libs/audio_core/modules/bag/flash/voicing.cppm`
  - `libs/audio_core/modules/bag/flash/phy_clean.cppm`
  - `libs/audio_core/modules/bag/fsk/codec.cppm`
  - `libs/audio_core/modules/bag/pro/codec.cppm`
  - `libs/audio_core/modules/bag/pro/phy_clean.cppm`
  - `libs/audio_core/modules/bag/pro/phy_compat.cppm`
  - `libs/audio_core/modules/bag/ultra/codec.cppm`
  - `libs/audio_core/modules/bag/ultra/phy_clean.cppm`
  - `libs/audio_core/modules/bag/ultra/phy_compat.cppm`
  - `libs/audio_core/modules/bag/transport/compat/frame_codec.cppm`
  - `libs/audio_core/modules/bag/transport/facade.cppm`
  - `libs/audio_core/modules/bag/pipeline/pipeline.cppm`
  - `libs/audio_api/src/bag_api.cpp`
  - `libs/audio_io/modules/audio_io/wav.cppm`
  - `libs/audio_io/modules/audio_io/wav_impl.cpp`
- `verify` 现在会穷尽扫描 `libs/audio_core`、`libs/audio_api` 与 `libs/audio_io` 中所有实际采用 `import std;` 的 source；任何新增 `import std;` 文件如果不先进入 required baseline policy，会直接判为 regression。
- `Gate 2` promotion list 已全部完成 closeout：
  - `bag.common.config.cppm`
  - `bag.common.types.cppm`
  - `flash/pro/ultra codec.cppm`
  - `flash signal/voicing.cppm`
  - `flash/pro/ultra phy_clean.cppm`
  - `fsk/codec.cppm`
  - `pro/ultra phy_compat.cppm`
  - `transport/compat/frame_codec.cppm`
  - `transport/facade.cppm`
  - `pipeline.cppm`
  - 以上 `16` 个 `audio_core` module interface 已收成 `import-std-only`，不再保留 fallback `#include <...>` guard
- 在 promoted set 全部收口后，当前 required baseline 只保留以下受控形态：
  - core module implementation single-path
    - `module;` + `import std;` + `module bag.*;`
    - 适用于 `libs/audio_core/src/*.cpp`
  - `audio_io.wav` module interface single-path
    - `module;` + `export module ...;` + `import std;`
    - 当前只批准 `libs/audio_io/modules/audio_io/wav.cppm`
  - boundary-host single-path
    - 非-module TU 直接使用 `import std;`，同时保持稳定 boundary include / import 关系
    - 当前只批准 `libs/audio_api/src/bag_api.cpp`
  - backend-bridge exception
    - 保留 package-private backend header include 与 global-fragment backend declaration 依赖，同时在命名模块实现面直接使用 `import std;`
    - 当前只批准 `libs/audio_io/modules/audio_io/wav_impl.cpp`
- 当前不进入这条 required baseline 的范围：
  - Android `native_package` / JNI lane
  - `bag_api.h`
  - `wav_io.h`
  - `libs/audio_io/src/wav_io.cpp`
  - `libs/audio_core/include/bag/interface/common/*`
  - `libs/audio_core/include/bag/link/**`
  - `libs/audio_core/include/bag/phy/**`

## 模式实现与编码

### 字符集规则
- `flash`
  - 不限字符集，输入按原始字节处理。
- `pro`
  - 仅允许 ASCII。
- `ultra`
  - 面向 UTF-8 文本，输入按 UTF-8 byte 处理。

### `flash`
- 定位：娱乐化、强调仪式感与二进制氛围的原始直通模式。
- 信息层：输入文本直接按原始字节处理；不限字符集，当前实现对 `std::string` 中的 UTF-8 字节不做额外协议封装。
- 编码方式：每个 bit 映射为一段固定时长的 `BFSK`，`0` 使用低频、`1` 使用高频。
- 结构特征：`1 byte = 8 bit symbol`，无帧头、无校验、无长度字段。

### `pro`
- 定位：ASCII-only 的正式模式。
- 信息层：输入必须为 ASCII 文本；文本先转为 ASCII byte；任何非 ASCII 输入都应拒绝。
- 编码方式：每个 byte 拆成高 4 bit 和低 4 bit；每个 nibble 映射为一个 `DTMF-like` 双音 symbol。
- 结构特征：`1 byte = 2 symbol`；每个 symbol 同时包含一组低频和一组高频，不使用额外 frame/CRC。

### `ultra`
- 定位：面向 UTF-8 文本字节忠实传输的正式模式。
- 信息层：输入文本直接按 UTF-8 byte 处理，不额外做人类语义转换，也不沿用 `pro` 的 ASCII-only 约束。
- 编码方式：每个 byte 拆成两个 nibble；每个 nibble 映射到 `16-FSK` 的一个固定频点。
- 结构特征：`1 byte = 2 symbol`；每个 symbol 只发一个频点。

## MVP 暂不做
- [x] `pro` 当前只做 `ASCII byte -> nibble -> DTMF-like 双音` 的 clean 闭环。
- [x] `ultra` 当前只做 `UTF-8 byte -> nibble -> clean 16-FSK` 的 clean 闭环。
- [ ] 自动拆帧、多帧重组、长文本流式会话管理不在当前 MVP 范围内。
- [x] 用户可感知的最小 style layer 已在正式 `flash` 输出中启用，当前包括安全 payload voicing 与固定 preamble / epilogue。
- [x] `bag.flash.voicing` 已接入正式 `flash` 输出，同时仍保留默认 no-op 口径供 clean 验证使用。
- [x] formal `flash` 当前已通过 `CoreConfig` / `bag_api.h` 公开 `flash_style`，并统一用同一配置同时驱动 signal timing、voicing、trim 与 decode；`coded_burst` 仍作为默认 style 和零值语义。
- [x] `flash` 当前仍不做随机化、立体声、可变长度背景层或不可预测的风格处理。

## 对外集成建议
1. Android、Windows 等平台继续通过 `bag_api.h` 调用 core，不直接依赖内部 modules。
2. WAV 读写继续通过 `wav_io.h` / `audio_io.wav` 承接，不把平台壳层拉进内部实现细节；Android 若需要 `wav <-> pcm bytes`，应继续通过 native package 私有 wrapper 间接消费，而不是直接暴露 `audio_io/include`。
3. 新能力优先加在 `audio_core` modules 与对应 implementation units，平台层继续保持壳层职责。
