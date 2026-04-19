# WaveBits Tools

`tools/` 只负责开发编排，不替代 `CMake` / `Gradle` 本身。

Host 侧根目录 `CMake` 工程当前要求 `CMake 3.28+`。

## 目标
- 固定仓库内的标准构建入口
- 减少手工记忆命令成本
- 让验证流程可以稳定复用

## 入口
- 对外只保留一个入口：`python tools/run.py <command>`
- 内部按职责拆分到 `tools/repo_tooling/`
- 如需运行依赖第三方 Python 库的工具命令，先执行：`python -m pip install -r tools/requirements.txt`
- CLI 帮助保持分层：根命令只列主要子命令，详细参数通过 `python tools/run.py <command> --help` 或 `python tools/run.py <command> <subcommand> --help` 查看
- 这份文档只保留工具入口地图与少量代表例子；更细流程统一下沉到 `docs/notes/`

## 命令组
- `build / test / verify`
  - 用于 host 构建、单测与正式验证
  - 常用入口：`build`、`test`、`test-lib`、`verify`
- `clang`
  - 用于 native 代码质量工具
  - 子命令：`format`、`tidy`
- `android`
  - 用于 Android Gradle 构建与 Kotlin 质量 gate
- `artifact`
  - 用于可见测试产物和最终交付产物
  - 子命令：`roundtrip`、`smoke`、`export-apk`
- `history`
  - 用于 release-history 草稿与结构校验
  - 子命令：`prep`、`validate`
- `message`
  - 用于基于 history 的 git commit message 草稿生成
  - 子命令：`prep`
- `file-name`
  - 用于文件命名的保守机械检查与 agent 草稿生成
  - 子命令：`prep`

## 详细文档

- 工具工作流总览：
  - `C:\code\WaveBits\docs\notes\tooling-overview.md`
- 构建与验证：
  - `C:\code\WaveBits\docs\notes\build-commands.md`
- 产物工作流：
  - `C:\code\WaveBits\docs\notes\artifact-workflow.md`
- History 工作流：
  - `C:\code\WaveBits\docs\notes\history-workflow.md`
- Message 工作流：
  - `C:\code\WaveBits\docs\notes\message-workflow.md`
- Android 编译专题：
  - `C:\code\WaveBits\docs\notes\android\android-compile.md`
- clang 工具专题：
  - `C:\code\WaveBits\docs\notes\clang\cmds.md`

## 说明
- `build / test / verify`
  - 对应 host 构建、单测、正式验证
  - 具体 gate、build 目录和 Android 联动规则见 `docs/notes/build-commands.md`
- `android`
  - 对应 Android Gradle 构建与 Kotlin 质量 gate
  - 具体编译入口与 Android 专题见 `docs/notes/android/android-compile.md`
- `artifact`
  - 对应可见测试产物与最终导出物
  - 具体输出位置和示例见 `docs/notes/artifact-workflow.md`
- `history`
  - 对应 release-history 草稿与结构校验
  - 具体流程见 `docs/notes/history-workflow.md`
- `message`
  - 对应 git commit message 草稿生成
  - 具体流程见 `docs/notes/message-workflow.md`
- `clang`
  - 对应 `format` / `tidy`
  - 具体参数与 clang 约束见 `docs/notes/clang/cmds.md`

## Android 入口
- Android 官方构建入口固定为 `apps/audio_android/`：
  - Windows：`cd apps/audio_android; .\gradlew.bat :app:assembleDebug`
  - macOS/Linux：`cd apps/audio_android && ./gradlew :app:assembleDebug`
- `apps/audio_android` 是独立 Android `Gradle` root。
- Android Studio / IntelliJ 导入项目时，应直接打开 `apps/audio_android`。
- Android native 当前固定继续走 `CMake 4.1.2 + C++23 + bag_api.h` 的独立 packaging lane，不直接跟随 host modules 主路径。
- Android app `CMake` 当前通过 `apps/audio_android/native_package/` 消费 `bag_android_native`，不再直接 `add_subdirectory(libs/audio_core)` 或 `add_subdirectory(libs/audio_api)`。
- Android native package 当前只编译 `audio_core` package-owned implementation sources、`bag_api` package-owned boundary implementation 与 `android_bag/**` 私有声明层，不再直接 source-own `bag_api.cpp + 8` 个 `audio_core` 原始实现文件。
- Android focused gate：
  - `python tools/run.py android native-debug`
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py android modules-smoke`
- `modules-smoke` 当前代表：
  - Android package lane 对主仓 `bag.common.version` module/source 的直接消费
  - `audio_core_common_version.cpp` 在这条 opt-in lane 下已退出 Android package-owned source list
  - 不代表 Android 已正式具备 host-style `import std;`
- 仓库内 `bag/legacy/**` 已删除；Android JNI 自身也不直接消费任何 legacy declaration surface，Android packaging lane 也不再直接消费主仓 `bag/internal/**` fallback 头。
- 相关 gate 与追踪文档见：
  - `docs/notes/legacy-release-gates.md`
  - `docs/notes/legacy-retirement-preconditions.md`

## 常用入口

先看主命令：

```powershell
python tools/run.py --help
```

再按命令组下钻：

```powershell
python tools/run.py clang --help
python tools/run.py android --help
python tools/run.py artifact --help
python tools/run.py file-name --help
python tools/run.py history --help
python tools/run.py message --help
```

高频示例只保留少量代表项：

```powershell
python tools/run.py build --build-dir build/dev
python tools/run.py verify --build-dir build/dev --skip-android
python tools/run.py android native-debug
python tools/run.py android assemble-debug
python tools/run.py file-name prep
python tools/run.py artifact export-apk
python tools/run.py history prep
python tools/run.py message prep --history docs/presentation/cli/v0.2/0.2.0.md
python tools/run.py history validate docs/presentation/cli
python tools/run.py artifact roundtrip --build-dir build/dev --mode ultra --text "你好，WaveBits"
python tools/run.py artifact smoke --build-dir build/dev
```

更细的参数与变体，统一通过对应命令组的 `--help` 查看，不在这里继续展开成长清单。

## 产物目录
- `build/`：保留 CMake / Gradle 原生构建输出和测试可见产物
- `dist/`：保留 Python 复制出的最终交付物
- `build/reports/clang-tidy/libs/<build-dir>/`：保留最小 clang-tidy 自动化产物
  - `clang-tidy.log`
  - `tasks/patch_001/001.log`
  - `tasks/patch_001/010.log`
  - `tasks/patch_002/011.log`
  - `.clang-tidy`
  - `.clang-format`
  - `summary.json`
  - `run_summary.json`
- 运行 `test` 时，默认报告输出到 `build/test-artifacts/reports/<timestamp>/`
- 测试报告目录包含：
  - `summary.json`：仅保留总数、通过数、失败数、耗时、各测试项状态，方便 agent 读取
  - `run.log`：完整 stdout/stderr、命令、时间戳、退出码，方便人工排查
- 运行 `roundtrip` / `smoke` 时，输出默认落到 `build/test-artifacts/`
- 运行 `export-apk` 时，默认输出到 `dist/android/`
- 单个 roundtrip 目录包含：
  - `input.txt`
  - `encoded.wav`
  - `decoded.txt`
  - `encode.stdout.txt`
  - `encode.stderr.txt`
  - `decode.stdout.txt`
  - `decode.stderr.txt`
  - `meta.json`


