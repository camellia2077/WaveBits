# WaveBits 测试说明

更新时间：2026-03-08

## 测试分层
- `unit`
  - 核心算法、text codec、frame codec、pipeline 最小状态机。
- `api contract`
  - `bag_api` 的 C ABI、错误语义、mode 透传与校验规则。
- `artifact roundtrip`
  - `text -> PCM/WAV -> text` 的产品主链路。
- `cli smoke`
  - 真实执行 `binary_audio_cpp`，验证命令行参数、产物文件与 roundtrip。

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
  - `flash`：ASCII、UTF-8、snapshot、pipeline 生命周期。
  - `pro`：ASCII byte codec、nibble symbol 映射、DTMF-like clean PHY、ASCII-only 约束、`171` ASCII 回归成功。
  - `ultra`：UTF-8 byte codec、nibble symbol 映射、clean `16-FSK`、中文/emoji/混合 UTF-8 往返、`512` / `513` 字节回归成功。
- `api_tests`
  - 三模式 roundtrip 必须通过。
  - `pro` 非 ASCII 必须返回 `BAG_INVALID_ARGUMENT`。
  - `pro` 的 `171` ASCII 回归语料必须成功，证明不再继承 compat frame 限制。
  - `ultra` 的 `513` UTF-8 回归语料必须成功，证明不再继承 compat frame 限制。
  - 解码结果的 `mode` 必须与配置一致。
- `artifact_tests`
  - 三模式 direct/WAV roundtrip 必须通过。
  - `pro` 的 PCM 长度必须按 `ASCII byte * 2 nibble symbol * frame_samples` 断言。
  - `ultra` 的 PCM 长度必须按 `UTF-8 byte * 2 nibble symbol * frame_samples` 断言。
  - `ultra` 的中文 / emoji / 混合 UTF-8 必须在产物级往返一致。
- `cli_smoke_tests`
  - `encode/decode --mode flash|pro|ultra` 代表性语料必须通过。
  - `pro` 非 ASCII 失败提示必须包含 `ASCII`。
  - `pro` 的 extended ASCII 文本文件 roundtrip 必须通过。
  - `ultra` 的 extended UTF-8 文本文件 roundtrip 必须通过。

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
