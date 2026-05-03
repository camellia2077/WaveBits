# `libs/` 说明

这个目录存放共享库代码：
- `audio_core`
- `audio_api`
- `audio_io`
- `audio_runtime`

查看或修改 `libs/` 下代码时，优先按任务选择相关文档，不要先递归扫描整个目录。

快速定位入口：
- `docs/README.md`
- `docs/architecture/repo-map.md`

如果只是改某个 mode：
- `mini` / `flash` / `pro` / `ultra` 的总览与入口位置，优先看 `docs/design/transports.md`
- 具体 mode 细节优先看 `docs/design/modes/README.md`
- 具体文件地图，优先看 `docs/architecture/repo-map.md`
- `flash` 情绪音色、preset、payload cadence 或 voicing，总览看 `docs/design/modes/flash/voicing-emotions.md`，具体 preset 看 `docs/design/modes/flash/<preset>.md`

如果是改 API / ABI / 平台边界：
- `bag_api` 入口看 `libs/audio_api/src/bag_api.cpp`
- 稳定边界与兼容层说明看 `docs/architecture/compatibility-layer-inventory.md`

如果是改 WAV / I/O：
- 文件地图看 `docs/architecture/repo-map.md`
- roundtrip 与 metadata 测试口径看 `docs/testing.md`

如果是改播放会话 / seek / 样本位置与时间换算：
- 优先看 `libs/audio_runtime/include/audio_runtime.h`
- 实现入口看 `libs/audio_runtime/src/audio_runtime.cpp`

如果是改测试或验证命令：
- 优先看 `docs/testing.md`
