## Terminal

- 在这个仓库执行终端命令时，统一使用 `pwsh` 作为 shell 入口，不使用 `powershell.exe`。

## Build

- 修改代码后，如需开始编译，优先使用 `python tools/run.py build --build-dir build/dev`。
- 修改代码后，如需完整验证，优先使用 `python tools/run.py verify --build-dir build/dev`。
- 如需生成可见测试音频产物，优先使用 `python tools/run.py roundtrip --build-dir build/dev --mode flash --text "Hello"` 或 `python tools/run.py smoke --build-dir build/dev`。
