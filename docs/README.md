# FlipBits 文档索引

更新时间：2026-04-30

## 先看这里
- 修改 `libs/` 下的共享库代码前，优先阅读：
  - `docs/architecture/repo-map.md`
  - `docs/design/transports.md`
  - `docs/testing.md`
- 若只想快速定位文件，不要先全量扫描 `libs/`，先按上面的索引文档跳到对应模块。
- 模式字符集速记：
  - `flash` 不校验字符集，按输入字节透明传输；公共入口仍是字符串接口
  - `pro` 仅允许 ASCII
  - `ultra` 面向 UTF-8
  - `mini` 是 Morse code 模式，仅支持 `A-Z / 0-9 / space / 常见 Morse 标点`，小写会规范化为大写

## 文档分层
- `docs/architecture/`
  - 记录仓库结构、模块边界、文件地图与建议阅读顺序。
- `docs/design/`
  - 记录模式设计、编码方式、协议/PHY 方向与实现口径。
- `docs/notes/`
  - 记录构建命令、release gate 与持续追踪清单。
- `docs/libs/`
  - 记录 `libs/` 共享库版本发布说明。
- `docs/presentation/`
  - 记录 CLI / Android 表现层版本发布说明。
- `docs/core.md`
  - 记录当前内核状态总览。
- `docs/presentation/README.md`
  - 记录当前表现层发布说明入口。
- `docs/testing.md`
  - 记录测试分层、语料和验证口径。
- `docs/future.md`
  - 记录未来规划与待做事项。

## 持续门禁与追踪
- 构建命令速记：
  - `docs/notes/build-commands.md`
- Android `import std;` 当前工具链状态：
  - `1/2`
  - `docs/notes/android-import-std-toolchain-status.md`
- Android `NDK/CMake` 何时值得升级：
  - `2/2`
  - `docs/notes/android-ndk-cmake-upgrade-decision.md`
- `legacy` 相关 release gate：
  - `docs/notes/legacy-release-gates.md`
## 常见任务应该看哪里
- 只改 Android UI 文案 / 本地化 key
  - 先看 `apps/audio_android/AGENTS.md`
  - 再看 `docs/design/android/android-translation-workflow.md`
  - 只需要定位英文基线 `apps/audio_android/app/src/main/res/values/strings_*.xml` 和对应 `values-*` 文件；通常不用展开 core/native 文档。
- 只改 core follow / lyrics / visual timeline 对齐
  - 先看 `docs/design/transports.md`
  - 再看 `docs/architecture/repo-map.md` 的 `mini / flash / pro / ultra`、`播放会话 runtime / seek 语义`、测试地图相关入口
  - 重点入口通常是 `libs/audio_core/src/transport/follow.cpp`、`libs/audio_api/tests/api_sync_tests.cpp` 和 Android follow/visual 组件。
- 改 `mini / flash / pro / ultra` 模式实现
  - 先看 `docs/design/transports.md`
  - flash 情绪音色总览看 `docs/design/flash-voicing-emotions.md`
  - 具体 `Steady / Hostile / Litany / Collapse / Zeal / Void` 设计看 `docs/design/flash-voicing/<preset>.md`
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
- 若任务只涉及 clean 主链路，通常不需要先打开 `phy_compat.*`、`transport/compat/frame_codec.*`、`src/fsk/fsk_codec.cpp` 或 `include/bag/phy/*`。
- 若任务只涉及 CLI / Android 集成，通常先看 `libs/audio_api` 与 `docs/design/transports.md`，再决定是否深入 `audio_core`。
