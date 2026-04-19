---
description: Agent 专用 tools-history 工作流
---

# Tools History Workflow

本文件定义 agent 编写 `docs/tools/history/` 时的推荐工作流。

目标不是为 `tools/` 发明产品版本号，而是把 repo-internal tooling 的长期演进按日期沉淀下来，方便后续 agent 和开发者回看“工具入口、工作流、校验口径”是如何变化的。

## Goal

- 把高重复、低创造性的步骤交给工具或固定模板
- 保留 agent 对 tooling 变化的归纳、取舍和措辞能力
- 让 `tools` 这条线独立于 `libs` / `presentation` 的版本 history

## Default Workflow

1. 先看当前 `tools/`、`.agent/`、`docs/notes/` 的真实改动
2. 判断这些改动是否改变了：
   - 命令入口
   - 默认工作流
   - 验证/校验口径
   - agent 使用路径
3. 若值得沉淀，则写入：
   - `docs/tools/history/YYYY-MM-DD.md`
4. 以当天文件作为单日 tooling history 入口，不为 `tools` 单独维护版本号
5. 同一天若有多轮工具演进，优先合并到同一个日期文件

## Scope

适合写入 `docs/tools/history/` 的内容：

- `python tools/run.py` 命令组变化
- `tools/repo_tooling/` 架构或路径变化
- `history` / `message` / `file-name` 等 agent 辅助工作流变化
- `verify` / policy / validate / artifact / Android build orchestration 的工具体验变化
- `.agent` 下与 repo workflow 直接相关的正式规则变化

不适合写入 `docs/tools/history/` 的内容：

- 纯产品功能变化，且 tooling 本身无可感知变化
- 只影响单个实现文件、但不会改变命令入口或 workflow 的内部整理
- 临时脚本、一次性草稿、未采用方案

## Hard Rules

- 文件路径固定为：`docs/tools/history/YYYY-MM-DD.md`
- 文件名必须使用 ISO 8601 日期：`YYYY-MM-DD.md`
- 标题固定为：`## YYYY-MM-DD`
- 列表统一使用 `* `
- 空分类不要保留
- 文件名、命令、路径、配置键统一使用反引号

## Preferred Sections

- `### 新增命令`
- `### 工作流调整`
- `### 校验与策略`
- `### 重构`
- `### 修复`

不是每篇都必须包含全部分类，只保留实际发生的内容。

## Agent Judgment Rules

- 只写 agent / 开发者能真实感知到的 tooling 变化
- 同类改动尽量合并表达，不把文件清单直接抄进 history
- 如果改动本质是“入口没变，只是内部搬家”，谨慎判断是否值得写
- 如果改动改变了默认入口、默认输出路径、默认工作流或验证方式，应明确写出旧口径与新口径
- 当一轮改动同时触及 `tools/` 与 `docs/notes/` / `.agent/`，优先按“工具入口 -> 工作流 -> 规则口径”的顺序归纳

## Minimal Template

```md
## YYYY-MM-DD

### 新增命令
* 新增 `<command or helper>`

### 工作流调整
* 调整 `<workflow>`

### 校验与策略
* 更新 `<policy or validation rule>`

### 重构
* 重构 `<tooling surface>`

### 修复
* 修复 `<tooling issue>`
```

## Usage Note

`docs/tools/history/` 是 repo-internal tooling 的长期演进记录，不等于 release history，也不要求绑定 `Release-Version`。
