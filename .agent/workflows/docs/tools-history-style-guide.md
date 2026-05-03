---
description: Agent 专用 tools-history 工作流
---

# Tools History Workflow

用于编写 `docs/tools/history/`。目标：记录 repo-internal tooling 的长期演进，不引入产品版本号。

## Default Workflow

1. 检查 `tools/`、`.agent/`、`docs/notes/` 的改动。
2. 判断是否改变了命令入口、默认工作流、校验口径、agent 使用路径。
3. 若值得沉淀，写入 `docs/tools/history/YYYY-MM-DD.md`。
4. 同一天多轮改动合并到同一文件。

## Scope

适合写入：
- `python tools/run.py` 命令组变化。
- `tools/repo_tooling/` 架构或路径变化。
- `history`/`message`/`file-name` 等 agent workflow 变化。
- `verify`/policy/validate/artifact/Android build orchestration 体验变化。
- `.agent` 下与 repo workflow 直接相关的正式规则变化。

不适合写入：
- 纯产品功能变化且 tooling 无可感知变化。
- 只改内部实现、不改入口或 workflow。
- 临时脚本、一次性草稿、未采用方案。

## Hard Rules

- 路径固定：`docs/tools/history/YYYY-MM-DD.md`。
- 文件名必须是 `YYYY-MM-DD.md`。
- 标题固定：`## YYYY-MM-DD`。
- 列表统一 `* `，空分类删除。
- 文件名/命令/路径/配置键使用反引号。

## Preferred Sections

- `### 新增命令`
- `### 工作流调整`
- `### 校验与策略`
- `### 重构`
- `### 修复`

按需保留，不要求全写。

## Judgment Rules

- 只写 agent/开发者可感知的 tooling 变化。
- 合并同类项，不抄文件清单。
- 若仅内部搬家且入口不变，谨慎落盘。
- 变更默认入口/输出路径/工作流/验证方式时，写清旧口径与新口径。
- 变更 `history prep` 这类 agent workflow 命令时，写清推荐调用方式，例如 `history prep --format markdown --scope <repo-path> --target <history-file.md>`。
- 同时改 `tools/` 与 `.agent`/`docs/notes/` 时，按“工具入口 -> 工作流 -> 规则口径”归纳。

## Usage Note

`docs/tools/history/` 是 tooling 演进记录，不等于 release history，也不绑定 `Release-Version`。
