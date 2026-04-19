# History Workflow

更新时间：2026-04-19

## 入口

- 主命令组：`python tools/run.py history --help`
- 子命令：
  - `python tools/run.py history prep --help`
  - `python tools/run.py history validate --help`

## 推荐链路

```text
history prep -> agent 重写 -> history validate -> 落盘 -> message prep
```

## 职责

- `history prep`
  - 读取 `git status --short`
  - 扫描 `docs/libs` 与 `docs/presentation/*` 的已落盘版本口径
  - 输出可改写的 history scaffold
- `history validate`
  - 用 `markdown-it-py` 解析 markdown
  - 校验 release heading、section heading、bullet marker、版本顺序和 `TODO(agent)` 残留

## 高频示例

```powershell
python tools/run.py history prep
python tools/run.py history prep --target docs/presentation/cli/v0.2/0.2.0.md
python tools/run.py history prep --format json
python tools/run.py history prep --format plain
python tools/run.py history prep --scope libs --view relevant
python tools/run.py history prep --scope apps/audio_android --target docs/presentation/android/v0.3/0.3.0.md --view relevant
python tools/run.py history prep --scope libs --format markdown --out-dir temp/history-prep --split-by bucket
python tools/run.py history validate docs/presentation/cli
python tools/run.py history validate docs/presentation/android
```

## 说明

- `history prep` 只负责采集和预填，不负责替 agent 做最终语义判断。
- 当只想写某一类 history 时，优先使用 `--scope` 缩小上下文；例如只写 libs history 时，可用 `python tools/run.py history prep --scope libs`。
- 当目标 history 文件已经确定时，优先传 `--target`；工具会优先从目标文件名推断版本号，例如 `docs/presentation/cli/v0.2/0.2.0.md -> v0.2.0`。
- `history prep` 中的日期默认来自当前工作日期，而不是 git commit 时间或旧 history 日期。
- `history prep` 现在会给出 Android / CLI / libs 的 candidate topics 与代表文件提示，方便 agent 快速定位“应该写什么”和“先读哪几个入口文件”。
- 当 agent 只需要“可写 history 的重点”而不是完整文件清单时，可用 `--view relevant`。
- 如果输出只给 agent 阅读，`--format plain` 往往比 `json` 更轻。
- 如果需要把上下文拆给 agent 分块读取，可配合 `--out-dir` 与 `--split-by bucket` 写到根目录 `temp/`。
- `history validate` 只负责结构与硬规则，不负责判断文案质量。
- 更完整的 agent 工作流说明见：
  - `C:\code\WaveBits\.agent\workflow\docs\history-style-guide.md`
  - `C:\code\WaveBits\docs\notes\message-workflow.md`
