## Terminal

- 在这个仓库执行终端命令时，统一使用 `pwsh` 作为 shell 入口。
- 在这个仓库执行终端命令、编译或验证时，默认不要使用沙盒环境；尤其是 `CMake` / `Ninja` / `Gradle` / `python tools/run.py verify` 相关命令，必须在非沙盒环境执行。

## Tooling

- 这个项目的 Python CLI 采用“主命令 / 子命令”分层形式。
- 先用 `python tools/run.py --help` 查看主命令组；需要具体参数时，再用 `python tools/run.py <command> --help` 或 `python tools/run.py <command> <subcommand> --help` 下钻。
- 常用命令组优先记这几个：`build`、`test`、`test-lib`、`verify`、`cli`、`windows`、`android`、`artifact`、`history`、`message`
- 修改代码后，如需开始编译，优先使用 `python tools/run.py build --build-dir build/dev`。
- 修改代码后，如需完整验证，优先使用 `python tools/run.py verify --build-dir build/dev`。
- 如需只跑库级单测，优先使用 `python tools/run.py test-lib <audio_runtime|audio_api|audio_io> --build-dir build/dev`。
- 不再依赖根层 `ctest -R runtime_tests|api_tests|unit_tests` 作为正式工作流。
- Python CLI 的 `--help` 保持分层：根命令只展示主要子命令与简短摘要，详细参数放到 `python tools/run.py <command> --help`，不要把所有选项堆到顶层帮助里。
- 如需生成或导出可见产物，优先先看 `python tools/run.py artifact --help`。
- 如需处理 release history，优先先看 `python tools/run.py history --help`。
- 如需准备 git commit message，优先先看 `python tools/run.py message --help`，并默认走“先 history、后 message”的链路。
- `.agent/AGENTS.md` 只保留总原则与高频入口；工具细节统一看 `<repo-root>/tools/README.md`，专题流程统一看 `<repo-root>/docs/notes/tooling-overview.md`。

## Subsystems

- Android 相关入口与编译/测试命令看 `<repo-root>/apps/audio_android/AGENTS.md`。
- CLI 相关入口与编译/测试命令看 `<repo-root>/apps/audio_cli/AGENTS.md`。

## Temp Files

- 根目录 `temp/` 是临时文件夹，默认不会被 Git 跟踪。
- 如需生成临时分析、草稿、阶段文档或中间产物，统一放到仓库根目录的 `temp/` 下，不要散落到其他目录。
