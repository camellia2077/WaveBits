# libs 工作指引

作用域：`libs/` 整个目录树。

## 开始前先看文档
- 处理 `libs/` 下任何代码前，先阅读：
  - `docs/README.md`
  - `docs/architecture/repo-map.md`
  - `docs/architecture/compatibility-layer-inventory.md`
  - `docs/design/transports.md`
  - `docs/testing.md`
  - `docs/notes/legacy-retirement-preconditions.md`

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
