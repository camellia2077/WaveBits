# libs 工作指引

作用域：`libs/` 整个目录树。

## 开始前先看文档
- 先按任务类型选最小文档集合，不要把下面所有文档当成必读清单。
- 想快速定位入口：
  - `docs/README.md`
  - `docs/architecture/repo-map.md`
- 改 `mini / flash / pro / ultra` 编解码或 mode 参数：
  - `docs/design/transports.md`
  - `docs/architecture/repo-map.md`
- 改 `flash` 情绪音色、preset、payload cadence 或 voicing：
  - `docs/design/flash-voicing-emotions.md`
  - 具体 preset 细节看 `docs/design/flash-voicing/<preset>.md`
  - `docs/design/transports.md`
- 改 `bag_api`、稳定 ABI、Android/CLI 边界或兼容层：
  - `docs/architecture/compatibility-layer-inventory.md`
  - `docs/architecture/repo-map.md`
- 改 WAV / I/O：
  - `docs/architecture/repo-map.md`
  - `docs/testing.md`
- 改测试、验证命令或测试语料：
  - `docs/testing.md`

## 扫描策略
- 不要先全量扫描 `libs/audio_core/`。
- 先按 `docs/architecture/repo-map.md` 的“按任务快速跳转”定位入口文件。
- 若任务只涉及 clean 主链路，通常不要先打开：
  - `phy_compat.*`
  - `frame_codec.*`
  - `text_codec.*`
  - `include/bag/phy/*`
  - `include/bag/link/*`

## 模块入口
- `audio_core`
  - 分发入口：`libs/audio_core/src/transport/transport.cpp`
- `audio_api`
  - API 入口：`libs/audio_api/src/bag_api.cpp`
- `audio_io`
  - I/O 入口：`libs/audio_io/src/wav_io.cpp`
- `audio_runtime`
  - 播放运行时入口：`libs/audio_runtime/src/audio_runtime.cpp`
