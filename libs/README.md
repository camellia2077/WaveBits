# `libs/` 说明

这个目录存放共享库代码：
- `audio_core`
- `audio_api`
- `audio_io`

在查看或修改 `libs/` 下代码前，优先先看文档，不要先递归扫描整个目录：
- `docs/README.md`
- `docs/architecture/repo-map.md`
- `docs/architecture/compatibility-layer-inventory.md`
- `docs/design/transports.md`
- `docs/testing.md`
- `docs/notes/legacy-retirement-preconditions.md`

如果只是改某个 mode：
- `flash` / `pro` / `ultra` 的设计与入口位置，优先看 `docs/design/transports.md`
- 具体文件地图，优先看 `docs/architecture/repo-map.md`
