# Audio Core 现状（v0.1.1）

更新时间：2026-03-08

## 版本更新说明
- [v0.1.1](./core/v0.1.1.md)
- [v0.1.0](./core/v0.1.0.md)

## 相关文档
- [测试说明](./testing.md)

## 版本信息
- 内核版本：`0.1.1`
- C API 可通过 `bag_core_version()` 查询版本字符串。

## 目录与职责
- `libs/audio_core/include/bag/common`
  - `config.h`：核心配置（采样率、帧长、诊断开关）。
  - `types.h`：跨模块数据结构（`PcmBlock`、`IrPacket`、`TextResult`）。
  - `error_code.h`：统一错误码。
  - `version.h`：核心版本接口。
- `libs/audio_core/include/bag/fsk`
  - `fsk_codec.h`：FSK 文本编解码接口（`EncodeTextToPcm16` / `DecodePcm16ToText`）。
- `libs/audio_core/include/bag/pipeline`
  - `pipeline.h`：`IPipeline` 抽象与 `CreatePipeline` 工厂。
- `libs/audio_core/include/bag/phy`、`libs/audio_core/include/bag/link`
  - 预留接口（`IFunPhy`、`IProPhy`、`ILinkLayer`），当前未接入完整实现。
- `libs/audio_api`
  - `bag_api.h/.cpp`：稳定 C API（供 JNI/CLI 等外部调用）。
- `libs/audio_io`
  - `wav_io.h/.cpp`：WAV 读写与文件 I/O 边界。

## 当前已实现能力
1. 文本与音频（PCM16）互转的基础 FSK 链路。
2. Pipeline 最小闭环：`PushPcm -> PollTextResult -> Reset`。
3. C API 解码器生命周期与推流接口：
   - `bag_create_decoder`
   - `bag_push_pcm`
   - `bag_poll_result`
   - `bag_reset`
   - `bag_destroy_decoder`
   - `bag_core_version`
4. C API 编码接口：
   - `bag_encode_text`
   - `bag_free_pcm16_result`

## 当前边界（v0.1.1）
1. 以最小可用为目标，重点在单链路打通。
2. `phy/pro`、`phy/fun`、`link` 仍是接口层，未形成完整协议栈实现。
3. 尚未包含前向纠错、重传策略、复杂同步与抗噪优化。
4. 结果置信度目前为简化值，后续可演进为真实质量估计模型。

## MVP 暂不做（持续补充）
- [x] `pro/ultra` 长文本处理固定为“单帧上限，超长报错”；当前 `payload` 上限为 `512` 字节。
- [ ] 自动拆帧、多帧重组、长文本流式会话管理不在本轮 MVP 范围内。

## 对外集成建议
1. Android/Windows 等平台通过 `bag_api` 调用 core，避免直接依赖内部实现细节。
2. WAV/文件输入输出放在 `audio_io`，平台层仅组合 `bag_api` 与 `audio_io`。
3. 新能力优先加在 `audio_core`，平台层保持壳层职责，便于跨平台复用。
