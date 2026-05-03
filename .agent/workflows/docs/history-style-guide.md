---
description: Agent 专用发布历史工作流
---

# Release History Workflow

用于编写 release history。原则：工具先收集，agent 负责归纳与定稿。

## Default Workflow

1. 运行 `python tools/run.py history prep --format markdown`。
2. 若目标文件已知，优先同时加 `--scope <repo-path>` 和 `--target <history-file.md>`，例如 `python tools/run.py history prep --format markdown --scope libs --target docs/libs/v0.6/0.6.1.md`。
3. 读取 `Release Hints`、`Relevant Summary`、`Draft Entry`。
4. 基于草稿重写，不直接原样落盘。
5. 删除无关分类、合并噪音、清理 `TODO(agent)`。
6. 运行 `python tools/run.py history validate <history-file.md>`；失败先修结构再修措辞。

## Tool Boundaries

`history prep` 负责：收集 `git status`、归类变更、扫描现有版本口径、生成草稿（支持 `--scope`/`--target`/`--out-dir`/`--split-by bucket`，格式 `markdown|plain|json`）。

`history prep` 不负责：价值判断、语义归类、最终文案。

`--target` 只用于推断目标 history 版本号，不会自动过滤改动范围；`--scope` 才负责把 `git status` 限定到指定目录。`--scope` 支持 repo 相对路径和 repo 内绝对路径。

`history validate` 负责：校验标题/分类/列表/顺序/`TODO(agent)`。

`history validate` 不负责：判断是否值得写、文案质量、语义归类。

## Hard Rules

- 最新版本在最前。
- 标题格式必须是 `## [vX.Y.Z] - YYYY-MM-DD`。
- 日期必须是 `YYYY-MM-DD`。
- 分类仅可使用：
  - `### 新增功能 (Added)`
  - `### 技术改进/重构 (Changed/Refactor)`
  - `### 修复 (Fixed)`
  - `### 安全性 (Security)`
  - `### 弃用/删除 (Deprecated/Removed)`
- 列表统一 `* `，空分类删除。
- 条目动词开头，简短直接。
- 文件名/命令/路径/配置键使用反引号。

## Judgment Rules

- 只写用户可感知或工程上重要的变化。
- 同类改动合并表达，不抄文件清单。
- 涉及迁移/版本/配置/构建口径变化时，写清旧口径与新口径。
- `history prep` 提示与真实改动冲突时，以已落盘内容和实际改动为准。
- Android history 优先按“用户能力 + 前端边界 + 工程入口”归纳。

## Usage Note

推荐链路：`history prep --format markdown --scope <repo-path> --target <history-file.md> -> agent 重写 -> history validate -> 落盘`。
