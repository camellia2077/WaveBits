# WaveBits Tools

`tools/` 只负责开发编排，不替代 `CMake` / `Gradle` 本身。

Host 侧根目录 `CMake` 工程当前要求 `CMake 3.28+`。

## 目标
- 固定仓库内的标准构建入口
- 减少手工记忆命令成本
- 让验证流程可以稳定复用

## 入口
- 对外只保留一个入口：`python tools/run.py <command>`
- 内部按职责拆分到 `tools/wavebits_tools/`
- CLI 帮助保持分层：根命令只列主要子命令，详细参数通过 `python tools/run.py <command> --help` 查看

## 说明
- `configure`：执行根目录 `CMake` 配置
- `build`：执行 `cmake --build`
- `clean`：清理默认 host 构建目录，或按 scope 清理 Android、测试产物与 Python cache
- `test`：执行 `ctest`，默认同时输出机器可读 `summary.json` 和人工可读 `run.log`
- `verify`：执行 `configure + build + test`，在未传 `--skip-android` 时再跑根目录 `Gradle` 的 `:app:assembleDebug`
  - 在进入构建前，会先执行 6 组长期语义的静态检查：
    - `module_structure`
    - `boundary`
    - `host_import_std`
    - `audio_io_boundary`
    - `compatibility`
    - `retirement`
  - 运行过程中会输出分步 banner，并直接透传 configure/build/test/Android 的实时日志，避免长时间构建看起来像卡死
  - 可使用 `python tools/run.py verify --list-checks` 查看当前分组与子检查项
  - 当前正式 gate 口径已经拆成：
    - host 默认 modules 主路径：`python tools/run.py verify --build-dir build/dev --skip-android`
    - Android 独立 gate：`python tools/run.py android native-debug` 与 `python tools/run.py android assemble-debug`
  - CI 当前通过 `.github/workflows/verify-host-and-legacy.yml` 持续运行：
    - `verify --list-checks`
    - `verify --build-dir build/dev --skip-android`
- `android`：执行仓库根目录的标准 `Gradle` 任务，模块源码位于 `apps/audio_android/app`
  - `python tools/run.py android modules-smoke` 会在 `externalNativeBuildDebug` 前打开 opt-in 的 Android named-modules smoke target
  - 这条实验线当前先验证 `bag.common.version` 的 direct module ownership shift；Android `import std;` 仍单独视为后续 toolchain gate
- `export-apk`：读取 Gradle 的 APK metadata，并将最终 APK 复制到根目录 `dist/android/`
- `roundtrip`：生成真实 `WAV` 产物并回读为文本
- `smoke`：批量生成 `flash / pro / ultra` 可见测试产物
- host 根目录当前只保留一条正式主线：
  - `clang++ + Ninja + build/dev`
- host 根目录 CMake 构建默认编译器为 `clang++`
- root host 不再名义支持 GNU / MSVC / Visual Studio / Ninja Multi-Config 组合
  - 如果同一个 `build/` 目录之前是用别的编译器配置的，`configure` 会自动做一次 `cmake --fresh`
- 当前长期保留的非-module 边界为：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_io/include/wav_io.h`

## Android 入口
- Android 官方构建入口固定为仓库根目录：
  - Windows：`.\gradlew.bat :app:assembleDebug`
  - macOS/Linux：`./gradlew :app:assembleDebug`
- `apps/audio_android` 不再是独立 `Gradle` root，只保留源码与模块资源。
- Android Studio / IntelliJ 导入项目时，应直接打开仓库根目录 `C:\code\WaveBits`。
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

## 示例

```powershell
python tools/run.py configure --build-dir build/dev
python tools/run.py build --build-dir build/dev
python tools/run.py clean
python tools/run.py clean --scope all --dry-run
python tools/run.py test --build-dir build/dev
python tools/run.py test --build-dir build/dev --report-dir build/test-artifacts/reports/latest
python tools/run.py verify --build-dir build/dev --skip-android
python tools/run.py android native-debug
python tools/run.py android assemble-debug
python tools/run.py android modules-smoke
python tools/run.py export-apk
python tools/run.py roundtrip --build-dir build/dev --mode ultra --text "你好，WaveBits"
python tools/run.py smoke --build-dir build/dev
```

## 产物目录
- `build/`：保留 CMake / Gradle 原生构建输出和测试可见产物
- `dist/`：保留 Python 复制出的最终交付物
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


