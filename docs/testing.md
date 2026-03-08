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
  - 正式协议模式，输入必须是 ASCII；文本先转固定 3 位十进制 ASCII 串，再进入 frame。
- `ultra`
  - 正式协议模式，输入先转 UTF-8 字节，再进入 frame。

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
  - 最大单帧成功语料：`170` 个 ASCII 字符（当前固定为 `170` 个 `'A'`）
- 失败语料：
  - 任意非 ASCII 文本，例如 `u8"中文"`
  - 超长文本：`171` 个 ASCII 字符，展开后得到 `513` 字节 payload

### `ultra`
- 成功语料：
  - `"Hello-123"`
  - `u8"你好，WaveBits"`
  - `u8"WaveBits 🚀"`
  - `u8"WaveBits 超级模式 🚀"`
  - 最大单帧成功语料：恰好 `512` 字节 UTF-8（当前固定为 `170` 个 `u8"你"` 加 `"AB"`）
- 失败语料：
  - 超长文本：`513` 字节 UTF-8（当前固定为 `171` 个 `u8"你"`）

## 成功 / 失败矩阵

| 模式 | ASCII | 中文 / Emoji / 混合 UTF-8 | 最大边界成功 | 超长失败 |
| --- | --- | --- | --- | --- |
| `flash` | 必须成功 | 必须成功 | 不适用 | 不适用 |
| `pro` | 必须成功 | 必须失败 | `170` ASCII 字符必须成功 | `171` ASCII 字符必须失败 |
| `ultra` | 必须成功 | 必须成功 | `512` UTF-8 字节必须成功 | `513` UTF-8 字节必须失败 |

## 当前门禁要求
- `unit_tests`
  - `flash`：ASCII、UTF-8、snapshot、pipeline 生命周期。
  - `pro`：ASCII decimal codec、ASCII-only 约束、`170` 成功 / `171` 失败。
  - `ultra`：中文、emoji、混合 UTF-8、`512` 字节成功 / `513` 字节失败。
- `api_tests`
  - 三模式 roundtrip 必须通过。
  - `pro` 非 ASCII 必须返回 `BAG_INVALID_ARGUMENT`。
  - `pro/ultra` 超长必须返回 `BAG_INVALID_ARGUMENT`。
  - 解码结果的 `mode` 必须与配置一致。
- `artifact_tests`
  - 三模式 direct/WAV roundtrip 必须通过。
  - `ultra` 的中文 / emoji / 混合 UTF-8 必须在产物级往返一致。
- `cli_smoke_tests`
  - `encode/decode --mode flash|pro|ultra` 代表性语料必须通过。
  - `pro` 非 ASCII 失败提示必须包含 `ASCII`。
  - `pro/ultra` 超长失败提示必须包含 `512-byte`。

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
