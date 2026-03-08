# WaveBits 文档索引

更新时间：2026-03-08

## 先看这里
- 修改 `libs/` 下的共享库代码前，优先阅读：
  - `docs/architecture/repo-map.md`
  - `docs/design/transports.md`
  - `docs/testing.md`
- 若只想快速定位文件，不要先全量扫描 `libs/`，先按上面的索引文档跳到对应模块。

## 文档分层
- `docs/architecture/`
  - 记录仓库结构、模块边界、文件地图与建议阅读顺序。
- `docs/design/`
  - 记录模式设计、编码方式、协议/PHY 方向与实现口径。
- `docs/core/`
  - 记录内核版本发布说明。
- `docs/presentation/`
  - 记录 CLI / Android 表现层版本发布说明。
- `docs/core.md`
  - 记录当前内核状态总览。
- `docs/presentation.md`
  - 记录当前表现层发布说明入口。
- `docs/testing.md`
  - 记录测试分层、语料和验证口径。
- `docs/future.md`
  - 记录未来规划与待做事项。

## 常见任务应该看哪里
- 改 `flash / pro / ultra` 模式实现
  - 先看 `docs/design/transports.md`
  - 再看 `docs/architecture/repo-map.md`
- 改 `bag_api`
  - 先看 `docs/architecture/repo-map.md`
  - 再看 `docs/core.md`
- 改 WAV 读写或 I/O
  - 先看 `docs/architecture/repo-map.md`
  - 再看 `docs/testing.md`
- 改测试
  - 先看 `docs/testing.md`
  - 再看 `docs/architecture/repo-map.md`

## 给 agent 的建议
- 优先从文档跳转到目标文件，不要先递归扫描整个 `libs/audio_core/`。
- 若任务只涉及 clean 主链路，通常不需要先打开 `phy_compat.*`、`frame_codec.*`、`text_codec.*` 或 `include/bag/phy/*`。
- 若任务只涉及 CLI / Android 集成，通常先看 `libs/audio_api` 与 `docs/design/transports.md`，再决定是否深入 `audio_core`。
