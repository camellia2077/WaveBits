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

## 说明
- `configure`：执行根目录 `CMake` 配置
- `build`：执行 `cmake --build`
- `test`：执行 `ctest`，默认同时输出机器可读 `summary.json` 和人工可读 `run.log`
- `verify`：执行 `configure + build + test`，默认再跑根目录 `Gradle` 的 `:app:assembleDebug`
  - 在进入构建前，会先执行 5 组长期语义的静态检查：
    - `module_structure`
    - `boundary`
    - `host_import_std`
    - `compatibility`
    - `retirement`
  - 可使用 `python tools/run.py verify --list-checks` 查看当前分组与子检查项
- `android`：执行仓库根目录的标准 `Gradle` 任务，模块源码位于 `apps/audio_android/app`
- `export-apk`：读取 Gradle 的 APK metadata，并将最终 APK 复制到根目录 `dist/android/`
- `roundtrip`：生成真实 `WAV` 产物并回读为文本
- `smoke`：批量生成 `flash / pro / ultra` 可见测试产物
- host 根目录 CMake 构建默认配置 `WAVEBITS_HOST_MODULES=ON`
- host 根目录 CMake 构建默认编译器为 `clang++`
  - 如果同一个 `build/` 目录之前是用别的编译器配置的，`configure` 会自动做一次 `cmake --fresh`
- `--no-modules`：只作用于 host CMake 链路，显式回退到 `WAVEBITS_HOST_MODULES=OFF` 的 retirement baseline 路径，不再代表长期正式目标
  - 当前 `bag_api.cpp`、`unit_tests` 与 `bag_core` 的 no-modules 实现入口已显式切到 `libs/audio_core/include/bag/legacy/**`
- `--experimental-modules`：兼容别名；当前 host 默认已经开启 modules

## Android 入口
- Android 官方构建入口固定为仓库根目录：
  - Windows：`.\gradlew.bat :app:assembleDebug`
  - macOS/Linux：`./gradlew :app:assembleDebug`
- `apps/audio_android` 不再是独立 `Gradle` root，只保留源码与模块资源。
- Android Studio / IntelliJ 导入项目时，应直接打开仓库根目录 `C:\code\WaveBits`。

## 示例

```powershell
python tools/run.py configure --build-dir build/dev
python tools/run.py build --build-dir build/dev
python tools/run.py test --build-dir build/dev
python tools/run.py test --build-dir build/dev --report-dir build/test-artifacts/reports/latest
python tools/run.py verify --build-dir build/dev --skip-android
python tools/run.py verify --build-dir build/legacy-host --skip-android --no-modules
python tools/run.py android assemble-debug
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


