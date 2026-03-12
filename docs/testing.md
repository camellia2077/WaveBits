# WaveBits 测试说明

更新时间：2026-03-12

## 测试分层
- `unit`
  - `flash/pro/transport` 低层行为、WAV 读写，以及 `Phase 18` wrapper retirement 的最小回归。
- `api contract`
  - `bag_api` 的 C ABI、错误语义、mode 透传与校验规则。
- `artifact roundtrip`
  - `text -> PCM/WAV -> text` 的产品主链路。
- `modules phase smokes`
  - 直接 `import bag.*` 的 host module-first 验证，按 phase 逐步扩张覆盖面。
- `cli smoke`
  - 真实执行 `binary_audio_cpp`，验证命令行参数、产物文件与 roundtrip。

## Host 构建路径
- host 根目录 CMake 构建默认开启 `WAVEBITS_HOST_MODULES=ON`
- 默认验证命令：
  - `python tools/run.py verify --build-dir build/dev --skip-android`
- legacy header 兼容回退命令：
  - `python tools/run.py verify --build-dir build/legacy-host --skip-android --no-modules`
- `verify` 现在会先执行一轮 Phase 7 静态门禁：
  - 已审过的 `audio_core/src/*` 薄实现必须继续保留既定 `module/import` 形态，避免回退到 include-only
- `verify` 现在也会执行一轮 Phase 8 consumer-boundary 门禁：
  - CLI / JNI / Android native 边界文件不允许重新直接依赖内部 `bag/...` 头或 `import bag.*`
- `verify` 现在也会执行一轮 Phase 10 test-boundary 门禁：
  - `api_tests` / `artifact_tests` 这类 boundary-first 测试不允许重新直接依赖内部 `bag/...` 头或 `import bag.*`
  - `Test/modules/*.cpp` 这类 module-first 测试不允许回退到 `#include "bag/..."`，必须继续保持 `import bag.*`
- `verify` 现在也会执行 `Phase 11 ~ Phase 16` 的 host-only `import std;` / consumer standardization 门禁：
  - 当前会继续锁定已审过的 core interfaces、`bag.common.*`、direct consumers，以及 `flash/pro/ultra codec` bridge headers 不回退
- `verify` 现在也会执行 `Phase 17` 的长期边界附近 host-only standardization 门禁：
  - 当前会锁定 `bag_api.cpp` 与 `bag_api` target 的 `import std;` 接线，不允许 C ABI 后面的 host 实现回退
- `verify` 现在也会执行 `Phase 18` 的 compatibility eviction 门禁：
  - 当前会锁定已删除的 `pro/frame/text` 与 `fsk` wrapper 不得回流
  - `libs/audio_core/CMakeLists.txt` 中的 `src/fsk/fsk_codec.cpp` 必须继续保持 host modules-only source 形态
- `verify` 现在也会执行 `Phase 19` 的 bridge-retirement / legacy carve-out 门禁：
  - `bag_api.cpp`、`unit_tests` 与 `libs/audio_core/src/*.cpp` 的 no-modules fallback 必须继续走 `bag/legacy/**`
  - `bag/legacy/**` 不允许漂移回其他 host / consumer 路径
  - 已退休的 `audio_core` shared bridge headers 不得重新引入
- legacy header 兼容回退路径现在显式通过 `bag/legacy/**` 暴露给 `bag_api.cpp`、`unit_tests` 与 `bag_core` 的 no-modules 实现
- Android 不跟随这次默认切换：
  - 仍由 `apps/audio_android/app/src/main/cpp/CMakeLists.txt` 下的 `CMake 3.22.1 + C++17 + bag_api.h` 路线负责

## 为什么必须按模式分开测试
三种模式共用部分底层链路，但业务语义不同，不能用同一套文本机械套用：

- `flash`
  - 原始直通模式，重点验证兼容基线。
- `pro`
  - ASCII-only 正式模式，文本先转 ASCII byte，再按 nibble 进入 DTMF-like 双音 clean PHY。
- `ultra`
  - UTF-8 正式模式，文本先转 UTF-8 byte，再按 nibble 进入 clean `16-FSK`。

因此测试必须同时覆盖：
- 模式本身的成功语义
- 模式特有的失败语义
- 不同模式下的文本边界

## 正式测试语料

### `flash`
- `"A"`
- `"Hello-123"`
- `"WaveBits: encode & decode!"`
- `u8"你好，WaveBits"`
- 约 `128` 字节长文本

### `pro`
- 成功语料：
  - `"A"`
  - `"Hello-123"`
  - `"WaveBits: encode & decode!"`
  - 约 `128` 字节长文本
  - 代表性长 ASCII 语料：`170` 个 ASCII 字符（当前固定为 `170` 个 `'A'`）
  - compat 上限回归语料：`171` 个 ASCII 字符，用于验证 `pro` 不再继承旧的 `512-byte` framed 限制
- 失败语料：
  - 任意非 ASCII 文本，例如 `u8"中文"`

### `ultra`
- 成功语料：
  - `"Hello-123"`
  - `u8"你好，WaveBits"`
  - `u8"WaveBits 🚀"`
  - `u8"WaveBits 超级模式 🚀"`
  - 代表性大语料：恰好 `512` 字节 UTF-8（当前固定为 `170` 个 `u8"你"` 加 `"AB"`）
  - 扩展回归语料：`513` 字节 UTF-8（当前固定为 `171` 个 `u8"你"`）

## 成功 / 失败矩阵

| 模式 | ASCII | 中文 / Emoji / 混合 UTF-8 | 代表长文本 | 模式特有失败 |
| --- | --- | --- | --- | --- |
| `flash` | 必须成功 | 必须成功 | 可直接跑 UTF-8 长文本 | 暂无模式专属失败语义 |
| `pro` | 必须成功 | 必须失败 | `170` / `171` ASCII 字符都应成功 | 非 ASCII 必须失败 |
| `ultra` | 必须成功 | 必须成功 | `512` / `513` UTF-8 字节都应成功 | 暂无模式专属长度失败语义 |

## 当前门禁要求
- `unit_tests`
  - `flash/BFSK`：直接 `byte <-> PCM`、sample length、幅值范围、空输入与 snapshot。
  - `wav_io`：最小 mono roundtrip。
  - `pro`：`bag.pro.codec` 的 ASCII payload/symbol encode/decode 与失败语义。
  - `frame_codec`：`bag.transport.compat.frame_codec` 的成功路径与畸形帧失败语义。
- `api_tests`
  - 三模式 roundtrip 必须通过。
  - `pro` 非 ASCII 必须返回 `BAG_INVALID_ARGUMENT`。
  - `pro` 的 `171` ASCII 回归语料必须成功，证明不再继承 compat frame 限制。
  - `ultra` 的 `513` UTF-8 回归语料必须成功，证明不再继承 compat frame 限制。
  - 解码结果的 `mode` 必须与配置一致。
- `artifact_tests`
  - 三模式 direct/WAV roundtrip 必须通过。
  - 只允许通过 `bag_api.h + wav_io.h` 验证边界行为，不应反向依赖内部 `bag/...` 头。
  - `pro` 的 PCM 长度必须按 `ASCII byte * 2 nibble symbol * frame_samples` 断言。
  - `ultra` 的 PCM 长度必须按 `UTF-8 byte * 2 nibble symbol * frame_samples` 断言。
  - `ultra` 的中文 / emoji / 混合 UTF-8 必须在产物级往返一致。
- `cli_smoke_tests`
  - `encode/decode --mode flash|pro|ultra` 代表性语料必须通过。
  - `pro` 非 ASCII 失败提示必须包含 `ASCII`。
  - `pro` 的 extended ASCII 文本文件 roundtrip 必须通过。
  - `ultra` 的 extended UTF-8 文本文件 roundtrip 必须通过。
- `modules_phase0_smoke / modules_phase2_leaf_smoke / modules_phase3_mid_layer_smoke / modules_phase4_facade_pipeline_smoke / modules_phase10_internal_cutover`
  - 在默认 host modules 路径下属于正式门禁。
  - 用于验证基础模块、叶子模块、中层模块、facade/pipeline 汇聚层，以及 Phase 10 的 module-first 内部测试都可被直接 `import` 消费。

## 可见测试音频产物
- `ctest` 默认门禁不保留人工查看用的 WAV 产物。
- 如需查看真实音频文件，使用：
  - `python tools/run.py roundtrip --build-dir build/dev --mode flash --text "Hello"`
  - `python tools/run.py smoke --build-dir build/dev`
- 这些产物默认输出到 `build/test-artifacts/`，目录中会包含：
  - `input.txt`
  - `encoded.wav`
  - `decoded.txt`
  - `encode.stdout.txt`
  - `encode.stderr.txt`
  - `decode.stdout.txt`
  - `decode.stderr.txt`
  - `meta.json`

## 当前不包含的内容
- Android 自动化测试不在这套正式分层内。
- 本文档不定义抗干扰、噪声、同步搜索、FEC、重传、流式多帧等鲁棒性测试。
- `build/test-artifacts/` 属于人工查看产物目录，不属于正式门禁输出。


