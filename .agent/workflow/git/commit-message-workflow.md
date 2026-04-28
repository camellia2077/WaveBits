---
description: Agent 专用 Git commit message 生成工作流
---

# Commit Message Workflow

本文件定义 agent 生成 Git commit message 时的推荐工作流。目标不是替代 `.agent/guides/git/git-message-styles.md` 的格式规则，而是把“先收集什么、优先信什么、什么时候可以退回 git fallback”这套执行顺序固定下来。

## Goal

- 把 commit message 生成链路统一为 `history -> message`
- 让 agent 优先消费已落盘的 release history，而不是每次重新从 `git diff` 猜语义
- 把 `message prep`、git fallback 和最终人工判断之间的边界说清楚

## Default Workflow

1. 先确认本次改动是否已经有对应的 history 文件：
   - `docs/libs/...`
   - `docs/presentation/android/...`
   - `docs/presentation/cli/...`
2. 若 history 已明确，优先运行：
   `python tools/run.py message prep --history <history-file.md>`
3. 若当前 working tree 中只有一个变更中的 history 文件，也可以直接运行：
   `python tools/run.py message prep`
4. 读取生成的 `temp/message.txt`
5. 参照 `.agent/guides/git/git-message-styles.md` 重写草稿：
   - 改掉 `TODO(agent)`
   - 合并低价值噪音项
   - 补齐真实的 `[Summary]` 与 `[Verification]`
   - 校正 `type`、`subject` 与 `Release-Version`
6. 如有必要，再结合当前变更文件做一次 sanity check：
   - `git status --short --untracked-files=all`
   - 仅用于校对，不用于推翻已明确的 history 语义
7. 最终再执行 `git commit`

## Tool Contract

`python tools/run.py message prep` 当前链路由 `tools/repo_tooling/message/` 下的几个模块组成：

- `collect.py`
  - 执行 `git status --short --untracked-files=all`
  - 读取当前 working tree 中的变更文件
  - 识别是否存在变更中的 history markdown
- `prep.py`
  - 作为主入口决定走 `history` 还是 `git-fallback`
  - 将草稿写到 `temp/message.txt`
- `infer.py`
  - 当存在明确 history 文件时，提取 `Added / Changed / Fixed / release version / component versions`
  - 当没有 history 文件时，退回 git fallback，按变更文件拼出保守草稿
- `render.py`
  - 将结构化草稿渲染成最终 commit message 文本格式
  - 自动补出 `[Summary]`、`[Verification]`、`Release-Version` 等段落骨架

## Preferred Source Order

commit message 的语义来源优先级固定如下：

1. 已落盘且与本次提交直接对应的 history 文件
2. 当前 working tree 中唯一明确的 history 文件
3. git working tree 的变更文件清单

换句话说：

- 有 history 时，不要重新从 git 改动独立发明另一套 commit 语义
- 只有没有 history，或者当前提交就是纯工具/脚本级改动、暂时没有 history 可写时，才允许使用 git fallback 作为草稿起点

## Hard Rules

- commit message 的最终格式必须继续遵守 `.agent/guides/git/git-message-styles.md`
- `temp/message.txt` 是可改写草稿，不是可直接提交的最终结果
- 对代码行为、接口、主题规则、构建入口、工作流有直接影响的文档修改，应与对应代码改动同提交更新
- 不要把这类配套文档变更拆成后补的独立 `docs` 提交；只有纯文档修正、补注释或纠正文档遗漏时，才允许单独提交
- 有多个变更中的 history 文件时，不要让工具自动合并不同发布线；必须显式传 `--history`
- `git-fallback` 只能作为临时兜底，不应覆盖已经明确的 history 口径
- `[Verification]` 必须由 agent 根据本次真实执行过的命令补齐，不能保留占位符
- `Release-Version` 必须由 agent 最终确认；如果工具给出 `TODO(agent)`，必须在提交前解决
- 如果 staged changes 包含 `libs/...`，但 history、用户指令或已落盘版本文件没有明确 `libs` 版本状态，必须先停止并询问用户确认 `libs` 应写具体版本、`changed` 还是 `unchanged`，不得自行推断
- 纯文档改动才允许 `docs`；只要包含代码改动，就回到 `feat / fix / refactor / chore / perf`

## Agent Judgment Rules

- 优先写“为什么这次提交成立”，再写文件层面的细碎变化
- 如果 history 已经按用户可感知能力归纳过，commit message 不要退化成文件清单
- 如果一次提交覆盖多个子系统，优先在 `[Component Versions]` 中补充版本线，而不是把 `Release-Version` 写多次
- 如果 git fallback 只列出了 `Changed file: ...`，必须由 agent 手动归纳成真实语义后才能提交
- 如果 working tree 很脏，但本次只准备提交其中一部分内容，agent 需要先确认 message 仅描述将被提交的那部分，而不是整个 `git status`

## Recommended Commands

```powershell
python tools/run.py message prep --history docs/presentation/android/v0.3/0.3.1.md
python tools/run.py message prep --history docs/presentation/cli/v0.2/0.2.1.md
python tools/run.py message prep --history docs/libs/v0.4/0.4.2.md
python tools/run.py message prep
Get-Content temp/message.txt
```

## Minimal Review Checklist

- `type` 是否正确
- `subject` 是否简短直接
- `[Summary]` 是否已去掉占位符
- `[Verification]` 是否只写本次真实执行过的验证
- `Release-Version` 是否唯一且正确
- staged changes 若包含 `libs/...`，`[Component Versions]` 是否已明确写出 `libs` 版本状态，或已向用户确认
- 是否错误混入了未提交部分的语义

## Usage Note

当本次改动已经先完成 history 落盘时，推荐链路永远是：

```text
history 落盘 -> message prep -> agent 改写 -> git commit
```

只有在 history 还不存在、或当前提交本身就是对工具链/脚手架的早期整理时，才使用 git fallback 作为临时起点。
