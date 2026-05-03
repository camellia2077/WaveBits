---
description: Agent 专用 Git commit message 生成工作流
---

# Commit Message Workflow

用于生成 commit message。原则：优先吃 history，缺失时再走 git fallback。

## Default Workflow

1. 先确认是否存在对应 history：`docs/libs/...`、`docs/presentation/android/...`、`docs/presentation/cli/...`。
2. 若已指定或已明确 history，运行 `python tools/run.py message prep --history <history-file.md>`，并以该 history 为首要语义来源。
3. 若仅有一个变更中的 history 文件，可直接运行 `python tools/run.py message prep`。
4. 若无可唯一识别的 history，退回 `git diff / git status`（fallback）。
5. 读取 `temp/message.txt`，按 `.agent/guides/git/git-message-styles.md` 重写：清理 `TODO(agent)`、去噪，并校正标题、sections、验证信息和版本信息。
6. 必要时用 `git status --short --untracked-files=all` 做一致性校对。
7. 执行 `git commit`。

## Source Priority

1. 显式 `--history <history-file.md>`
2. 已落盘且与本次提交对应的 history
3. working tree 中唯一明确的 history
4. git 变更清单（fallback）

## Hard Rules

- 最终格式、标题格式、allowed types、section 规则和 type 选择规则统一以 `.agent/guides/git/git-message-styles.md` 为准。
- 本 workflow 只规定 message 来源选择、草稿生成、staged scope 校对、验证补全和 commit 执行流程。
- `temp/message.txt` 是草稿，不可直接提交。
- 多个变更中的 history 文件时必须显式传 `--history`。
- `git-fallback` 仅兜底，不覆盖明确 history 口径。
- `[Verification]` 必须写本次真实执行过的验证。
- `Release-Version` 必须唯一且最终确认，不得保留 `TODO(agent)`。
- 若 staged 包含 `libs/...` 且 `libs` 版本状态不明确，必须先向用户确认 `具体版本`/`changed`/`unchanged`。

## Judgment Rules

- 先写“为什么这次提交成立”，再写细节。
- history 已有归纳时，不退化成文件清单。
- 涉及多子系统时，在 `[Component Versions]` 补版本线，不重复写多个 `Release-Version`。
- fallback 只有文件清单时，必须先做语义归纳再提交。
- working tree 很脏时，message 只描述将被提交的内容。

## Minimal Checklist

- `type` 和 `subject` 符合 `.agent/guides/git/git-message-styles.md`。
- `[Summary]` 无占位符。
- `[Verification]` 真实可追溯。
- `Release-Version` 唯一且正确。
- 若涉及 `libs/...`，`[Component Versions]` 已明确 `libs` 状态或已完成用户确认。

## Usage Note

推荐链路：`history 落盘 -> message prep -> agent 改写 -> git commit`。无 history 时才走 git fallback。
