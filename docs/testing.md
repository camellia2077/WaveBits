# WaveBits 测试说明

更新时间：2026-03-15

## 测试分层
- `unit`
  - `wav_io.h` header-boundary smoke、bytes roundtrip 与非法/不支持 WAV 失败语义。
  - 当前源码位置：`libs/audio_io/tests/unit_tests.cpp`
- `api contract`
  - `bag_api` 的 C ABI、错误语义、mode 透传与校验规则。
  - 当前源码位置：`libs/audio_api/tests/api_tests.cpp`
- `runtime`
  - `audio_runtime` 的状态迁移、scrub 语义与时间换算。
  - 当前源码位置：`libs/audio_runtime/tests/runtime_tests.cpp`
- `artifact roundtrip`
  - `text -> PCM/WAV -> text` 的产品主链路。
- `flash voicing focused`
  - 独立验证 `bag.flash.voicing` 的 no-op、长度保持、范围安全与确定性。
- `module smokes`
  - 直接 `import bag.*` / `import audio_io.wav` 的 host module-first 验证，覆盖基础模块、叶子模块、中层模块与汇聚层。
- `cli smoke`
  - 真实执行 `binary_audio_cpp`，验证命令行参数、产物文件与 roundtrip。

## 当前门禁路径

### Host 默认主路径
- 默认验证命令：
  - `python tools/run.py verify --build-dir build/dev --skip-android`
- 当前含义：
  - 验证 host 默认 modules 主路径
  - 不再借默认 `verify` 步骤隐式表达 Android gate
- 单库定向测试命令：
  - `python tools/run.py test-lib audio_runtime --build-dir build/dev`
  - `python tools/run.py test-lib audio_api --build-dir build/dev`
  - `python tools/run.py test-lib audio_io --build-dir build/dev`
- 当前约定：
  - 根层 `ctest -R runtime_tests|api_tests|unit_tests` 不再作为正式工作流兼容目标
  - `runtime_tests` / `api_tests` / `unit_tests` 应通过各自库级测试子树运行

### Android 独立 gate
- focused gate：
  - `python tools/run.py android native-debug`
  - `python tools/run.py android assemble-debug`
- 当前含义：
  - Android 通过 `apps/audio_android/native_package/CMakeLists.txt -> bag_android_native` 独立装配
  - Android JNI 编解码继续消费 `bag_api.h`，播放运行时消费 `audio_runtime.h`，WAV bytes 能力消费 package-private `audio_io` wrapper
  - Android native package 当前只编译 `audio_core` package-owned implementation sources、`bag_api` / `audio_runtime` package-owned boundary implementation、`audio_io` package-private wrapper 与 `android_bag/**` / `android_audio_io/**` 私有声明层

### Host 支持政策
- host 根目录当前只保留一条正式主线：
  - `clang++ + Ninja + build/dev`

## `verify` 静态检查
- `verify` 在构建前会执行 4 组静态检查：
  - `module_structure`
    - 锁定 named-module implementation units 与 module-first wiring 不回退
  - `boundary`
    - 锁定 CLI / JNI / Android native / tests 的 allowed surface，不允许重新直接依赖内部 `bag/...` 头或 `import bag.*`
  - `audio_io_boundary`
    - 锁定 `wav_io.h` 与 `audio_io.wav` 的双入口模型、private backend 拆分、`sndfile` containment 与 `wav_impl.cpp` 的 global-fragment backend 声明位置
  - `retirement`
    - 锁定 retired wrappers、shared bridge headers、Android 私有头 self-contained 形态、已删除的 `bag/legacy/**` no-reintroduction 状态，以及 boundary-adjacent host wiring 不回退
- 当前主仓 `bag/internal/**` owner 已为 `0`
- `bag/interface/common/*` 当前按长期保留的 reserved-interface declaration boundary 管理：
  - 只允许预留接口头与该目录内部消费
  - 保持 include-based header 形态，不新增 `import std;`
  - 不新增 `bag.interface.*` module mirror
- 当前不再有批准的 `bag/legacy/**` 直接消费者，legacy headers 也已从仓库删除

## CI 与发布前门禁
- CI 当前通过 `.github/workflows/verify-host-and-legacy.yml` 持续执行：
  - `verify --list-checks`
  - `verify --build-dir build/dev --skip-android`
- 发布前门禁与退休前置条件追踪见：
  - `docs/notes/legacy-release-gates.md`
  - `docs/notes/legacy-retirement-preconditions.md`

## 为什么必须按模式分开测试
三种模式共用部分底层链路，但业务语义不同，不能用同一套文本机械套用：
- `flash`
  - 不限字符集的原始直通模式，重点验证 clean raw-byte 语义。
- `pro`
  - ASCII-only 正式模式，文本先转 ASCII byte，再按 nibble 进入 `DTMF-like` 双音 clean PHY。
- `ultra`
  - UTF-8 正式模式，文本先转 UTF-8 byte，再按 nibble 进入 clean `16-FSK`。

因此测试必须同时覆盖：
- 模式本身的成功语义
- 模式特有的失败语义
- 不同模式下的文本边界

## 字符集规则
- `flash`
  - 不限字符集。
  - 测试上应覆盖 ASCII 与中文 / emoji / 混合 UTF-8。
- `pro`
  - 仅允许 ASCII。
  - 任何非 ASCII 文本都应走失败语义。
- `ultra`
  - 面向 UTF-8 文本。
  - 测试上应覆盖 ASCII、中文、emoji 与混合 UTF-8。

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
  - 代表性长 ASCII 语料：`170` 个 ASCII 字符
  - 回归语料：`171` 个 ASCII 字符
- 失败语料：
  - 任意非 ASCII 文本，例如 `u8"中文"`

### `ultra`
- 成功语料：
  - `"Hello-123"`
  - `u8"你好，WaveBits"`
  - `u8"WaveBits 🚀"`
  - `u8"WaveBits 超级模式 🚀"`
  - 代表性大语料：恰好 `512` 字节 UTF-8
  - 扩展回归语料：`513` 字节 UTF-8

## 成功 / 失败矩阵

| 模式 | ASCII | 中文 / Emoji / 混合 UTF-8 | 代表长文本 | 模式特有失败 |
| --- | --- | --- | --- | --- |
| `flash` | 必须成功 | 必须成功 | 可直接跑 UTF-8 长文本 | 不限字符集，暂无模式专属失败语义 |
| `pro` | 必须成功 | 必须失败 | `170` / `171` ASCII 字符都应成功 | 非 ASCII 必须失败 |
| `ultra` | 必须成功 | 必须成功 | `512` / `513` UTF-8 字节都应成功 | 暂无模式专属长度失败语义 |

## 当前门禁要求
- `unit_tests`
  - `wav_io.h` header boundary 的多组 mono roundtrip contract、bytes roundtrip、缺失文件失败语义，以及 invalid header / unsupported format 的失败语义。
- `api_tests`
  - 三模式 roundtrip 必须通过。
  - `pro` 非 ASCII 必须返回 `BAG_INVALID_ARGUMENT`。
  - `pro` 的 `171` ASCII 回归语料必须成功。
  - `ultra` 的 `513` UTF-8 回归语料必须成功。
  - 解码结果的 `mode` 必须与配置一致。
- `artifact_tests`
  - 三模式 direct/WAV roundtrip 必须通过。
  - 只允许通过 `bag_api.h + wav_io.h` 验证边界行为，不应反向依赖内部 `bag/...` 头。
  - `pro` 的 PCM 长度必须按 `ASCII byte * 2 nibble symbol * frame_samples` 断言。
  - `ultra` 的 PCM 长度必须按 `UTF-8 byte * 2 nibble symbol * frame_samples` 断言。
  - `ultra` 的中文 / emoji / 混合 UTF-8 必须在产物级往返一致。
- `flash_voicing_tests`
  - `bag.flash.voicing` 的 no-op、envelope-only、harmonic-only、click-only、trim descriptor / trim payload、固定 preamble / epilogue 与 styled 确定性测试。
- `cli_smoke_tests`
  - `encode/decode --mode flash|pro|ultra` 代表性语料必须通过。
  - `pro` 非 ASCII 失败提示必须包含 `ASCII`。
  - `pro` 的 extended ASCII 文本文件 roundtrip 必须通过。
  - `ultra` 的 extended UTF-8 文本文件 roundtrip 必须通过。
  - 属于额外产品 smoke，不在默认 `verify` 最小集里。
- `modules_foundation_smoke / modules_leaf_smoke / modules_mid_layer_smoke / modules_facade_pipeline_smoke / modules_end_to_end_smoke`
  - 在默认 host modules 路径下属于正式门禁。
  - 用于验证基础模块、叶子模块、中层模块、facade/pipeline 汇聚层，以及当前 module-first 内部测试都可被直接 `import` 消费。
  - 其中 `modules_leaf_smoke` 继续承担：
    - `audio_io.wav` 的多组 roundtrip、bytes roundtrip 与 invalid header 失败语义
    - `bag.flash.signal` 的 payload layout、`byte <-> PCM`、sample length、幅值范围、空输入与 snapshot 覆盖
    - `bag.flash.voicing` 的 no-op 输出、safe styled 输出与 descriptor 覆盖
    - `bag.flash.phy_clean` 的 text facade roundtrip 覆盖
    - `bag.pro.codec` 的 ASCII payload/symbol encode/decode 与失败语义覆盖
    - `bag.transport.compat.frame_codec` 的成功路径、畸形帧失败语义与单帧长度上限覆盖

## 当前 post-legacy 结论
- `bag/legacy/**` 已从 `libs/audio_core/include/` 删除。
- Android package-private native exception 继续作为独立平台偏差跟踪，但当前已固定到 Android `C++23` baseline，不再与 legacy surface 耦合。
- 当前 testing / verify 口径会同时阻止 legacy 路径回流与 direct legacy include token 回流。

## 可见测试音频产物
- `ctest` 默认门禁不保留人工查看用的 WAV 产物。
- 如需查看真实音频文件，使用：
  - `python tools/run.py artifact roundtrip --build-dir build/dev --mode flash --text "Hello"`
  - `python tools/run.py artifact smoke --build-dir build/dev`
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
