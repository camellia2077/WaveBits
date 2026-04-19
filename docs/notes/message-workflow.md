# Message Workflow

更新时间：2026-04-19

## 入口

- 主命令组：`python tools/run.py message --help`
- 子命令：
  - `python tools/run.py message prep --help`

## 推荐链路

```text
history 落盘 -> message prep -> agent 润色 -> git commit
```

## 职责

- `message prep`
  - 优先读取已落盘 history markdown
  - 从 history section 提取 `[Summary]`、`[Added]`、`[Changed & Refactored]`、`[Fixed]` 的草稿内容
  - 根据 history 路径推断 `[Component Versions]`
  - 把草稿写到 `temp/message.txt`
  - 仅当无法确定 history 文件时，才退回 git-based scaffold

## 高频示例

```powershell
python tools/run.py message prep --history docs/presentation/cli/v0.2/0.2.0.md
python tools/run.py message prep --history docs/presentation/android/v0.3/0.3.0.md
python tools/run.py message prep
```

## 说明

- 默认口径是“先写 history，再写 message”；message 不应重新从 git 改动独立重建完整语义。
- 如果当前 working tree 里恰好只有一个变更中的 history 文件，`message prep` 可以直接自动选中它。
- 如果存在多个变更中的 history 文件，显式传 `--history`，避免工具擅自合并不同发布线语义。
- `temp/message.txt` 是 agent 可继续改写的草稿，不是最终 commit message。
