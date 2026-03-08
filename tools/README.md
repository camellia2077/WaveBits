# WaveBits Tools

`tools/` 只负责开发编排，不替代 `CMake` / `Gradle` 本身。

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
- `test`：执行 `ctest`
- `test`：执行 `ctest`，默认同时输出机器可读 `summary.json` 和人工可读 `run.log`
- `verify`：执行 `configure + build + test`，默认再跑 `Android assembleDebug`
- `android`：执行 `apps/audio_android` 下的标准 `Gradle` 任务
- `roundtrip`：生成真实 `WAV` 产物并回读为文本
- `smoke`：批量生成 `flash / pro / ultra` 可见测试产物

## 示例

```powershell
python tools/run.py configure --build-dir build/dev
python tools/run.py build --build-dir build/dev
python tools/run.py test --build-dir build/dev
python tools/run.py test --build-dir build/dev --report-dir build/test-artifacts/reports/latest
python tools/run.py verify --build-dir build/dev --skip-android
python tools/run.py android assemble-debug
python tools/run.py roundtrip --build-dir build/dev --mode ultra --text "你好，WaveBits"
python tools/run.py smoke --build-dir build/dev
```

## 产物目录
- 运行 `test` 时，默认报告输出到 `build/test-artifacts/reports/<timestamp>/`
- 测试报告目录包含：
  - `summary.json`：仅保留总数、通过数、失败数、耗时、各测试项状态，方便 agent 读取
  - `run.log`：完整 stdout/stderr、命令、时间戳、退出码，方便人工排查
- 运行 `roundtrip` / `smoke` 时，输出默认落到 `build/test-artifacts/`
- 单个 roundtrip 目录包含：
  - `input.txt`
  - `encoded.wav`
  - `decoded.txt`
  - `encode.stdout.txt`
  - `encode.stderr.txt`
  - `decode.stdout.txt`
  - `decode.stderr.txt`
  - `meta.json`
