# Audio Core 现状（v0.3.0）

更新时间：2026-03-12

## 版本更新说明
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
- 内核版本：`0.3.0`
- C API 可通过 `bag_core_version()` 查询版本字符串。

## 目录与职责
- `libs/audio_core/modules/bag/common`
  - `config/types/error_code/version.cppm`：当前主线基础声明模块。
- `libs/audio_core/include/bag/legacy`
  - `common/*`、`flash/*`、`pro/*`、`transport/*`、`ultra/*`：Android / no-modules / legacy fallback 的显式声明面。
- `libs/audio_core/include/bag/flash`、`libs/audio_core/include/bag/pro`、`libs/audio_core/include/bag/ultra`
  - 这层 shared bridge headers 已在 `Phase 19` 退休；当前主线请直接看对应 modules 与 `src/*.cpp`。
- `libs/audio_core/modules/bag/fsk`、`libs/audio_core/src/fsk/fsk_codec.cpp`
  - `bag.fsk.codec`：host module-first helper；实现语义已收敛为 `flash/BFSK` 的过渡包装。
- `libs/audio_core/include/bag/legacy/pro`、`libs/audio_core/include/bag/legacy/ultra`
  - `phy_compat.h`：仅保留给 legacy / no-modules 路径的历史 compat PHY 声明。
- `libs/audio_core/include/bag/phy`、`libs/audio_core/include/bag/link`
  - 预留接口（`IFunPhy`、`IProPhy`、`ILinkLayer`），当前未接入完整实现。
- `libs/audio_api`
  - `bag_api.h/.cpp`：稳定 C API（供 JNI/CLI 等外部调用）。
- `libs/audio_io`
  - `wav_io.h/.cpp`：WAV 读写与文件 I/O 边界。

## 当前已实现能力
1. `transport facade + flash/pro/ultra` 的 mode-first 内部主线。
2. `flash` 的原始文本/字节直通 + BFSK clean PHY。
3. `pro` 的 ASCII byte + DTMF-like 双音 clean PHY。
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

## 当前边界（v0.3.0）
1. 以最小可用为目标，重点在单链路打通。
2. `CreatePipeline` 现为兼容适配层，正式主线已经下沉到 `transport` 与各 mode 模块。
3. `flash` 当前固定为长期基线模式：原始直通、无 payload/frame 封装、无单帧 `512` 字节上限、只保留 `clean` 路径。
4. `pro` 当前固定为 ASCII-only：文本直接转 ASCII byte，再按 nibble 进入 DTMF-like 双音 clean PHY；不再继承 compat frame/CRC/512-byte 上限。
5. `ultra` 当前固定为 UTF-8 byte：文本直接按 UTF-8 字节进入 nibble/symbol 映射，再走 clean `16-FSK`；不再继承 compat frame/CRC/512-byte 上限。
6. `Phase 18` 已删除 `text_codec.h`、`frame_codec.h`、`fsk_codec.h` 这批 compatibility-only wrappers。
7. `Phase 19` 已退休 `audio_core` 主线 shared bridge headers；legacy 声明入口已显式切到 `bag/legacy/**`。
8. `phy/pro`、`phy/fun`、`link` 仍是接口层，未接入当前主线。
9. 尚未包含前向纠错、重传策略、复杂同步与抗噪优化。
10. 结果置信度目前为简化值，后续可演进为真实质量估计模型。

## 模式实现与编码

### `flash`
- 定位：娱乐化、强调仪式感与二进制氛围的原始直通模式。
- 信息层：输入文本直接按原始字节处理；当前实现对 `std::string` 里的 UTF-8 字节不做额外协议封装。
- 编码方式：每个 bit 映射为一段固定时长的 `BFSK`，`0` 使用低频、`1` 使用高频。
- 结构特征：`1 byte = 8 bit symbol`，无帧头、无校验、无长度字段。
- 当前重点：保证“文本 -> 音频 -> 文本”的 clean 闭环，不引入 style layer、协议层或鲁棒性逻辑。

### `pro`
- 定位：ASCII-only 的正式模式，优先保证结构清晰、职责单纯。
- 信息层：输入必须为 ASCII 文本；文本先转为 ASCII byte。
- 编码方式：每个 byte 拆成高 4 bit 和低 4 bit；每个 nibble 映射为一个 `DTMF-like` 双音 symbol。
- 结构特征：`1 byte = 2 symbol`；每个 symbol 同时包含一组低频和一组高频，不使用额外 frame/CRC。
- 当前重点：保持 byte/nibble 语义清晰，作为后续协议层、风格层或更复杂链路层的稳定基础。

### `ultra`
- 定位：面向 UTF-8 文本字节忠实传输的正式模式。
- 信息层：输入文本直接按 UTF-8 byte 处理，不额外做人类语义转换。
- 编码方式：每个 byte 拆成两个 nibble；每个 nibble 映射到 `16-FSK` 的一个固定频点。
- 结构特征：`1 byte = 2 symbol`，每个 symbol 只发一个频点；当前频点表固定为 16 个 clean 频率。
- 当前重点：先把 clean `16-FSK` 主链路稳定住，后续再考虑更高阶的协议、鲁棒性和风格化扩展。

## MVP 暂不做（持续补充）
- [x] `pro` 当前只做 `ASCII byte -> nibble -> DTMF-like 双音` 的 clean 闭环，不做 frame/CRC、风格层、鲁棒性或长文本编排策略。
- [x] `ultra` 当前只做 `UTF-8 byte -> nibble -> clean 16-FSK` 的 clean 闭环，不做 frame/CRC、风格层、鲁棒性或长文本协议编排。
- [ ] 自动拆帧、多帧重组、长文本流式会话管理不在本轮 MVP 范围内。
- [ ] style layer 暂不实现；当前只保留 mode 内部的 clean/compat 路径，不新增 style 配置或外部 API。
- [x] `flash` 当前不做 style layer，不加入频率变化、立体声、节奏强化、音色设计或背景氛围处理。

## 对外集成建议
1. Android/Windows 等平台通过 `bag_api` 调用 core，避免直接依赖内部实现细节。
2. WAV/文件输入输出放在 `audio_io`，平台层仅组合 `bag_api` 与 `audio_io`。
3. 新能力优先加在 `audio_core`，平台层保持壳层职责，便于跨平台复用。
