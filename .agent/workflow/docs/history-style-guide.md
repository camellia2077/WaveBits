---
description: Agent 专用发布历史工作流
---

# Release History Workflow

本文件定义 agent 编写版本历史时的推荐工作流。目标不是把所有情况写死，而是先用仓库内工具收集固定信息，再由 agent 完成判断、归纳与定稿。

## Goal

- 把高重复、低创造性的步骤交给工具
- 保留 agent 对版本历史的归纳、取舍和措辞能力
- 让版本口径优先来自仓库内已落盘的 history/docs，而不是临时猜测

## Default Workflow

1. 先运行：
   `python tools/run.py history prep --format markdown`
2. 如果目标 history 文件已经确定，优先补上：
   `--target <history-file.md>`
   例如：`python tools/run.py history prep --format markdown --target docs/presentation/cli/v0.2/0.2.0.md`
3. 读取输出中的：
   - `Release Hints`
   - `Relevant Summary`
   - `Draft Entry`
4. 以该输出作为草稿上下文，而不是直接原样落盘
5. 根据实际改动重写历史条目：
   - 删除无关分类
   - 合并低价值噪音项
   - 把固定模板中的 `TODO(agent)` 改成真实内容
6. 写完后运行：
   `python tools/run.py history validate <history-file.md>`
7. 若校验失败，先修格式与结构，再回头看措辞和归纳

## Tool Contract

`history prep` 负责：

- 读取当前 `git status --short`
- 归类当前变更文件
- 扫描 `docs/libs`、`docs/presentation/android`、`docs/presentation/cli` 中已落盘的最新版本口径
- 生成一份可供 agent 改写的 history 草稿模板
- 支持用 `--scope` 缩小上下文
- 支持用 `--target` 让建议版本优先来自目标 history 文件名
- 支持输出 `markdown`、`plain`、`json`
- 支持用 `--out-dir` 与 `--split-by bucket` 把上下文拆到 `temp/` 供 agent 分块读取

`history prep` 不负责：

- 判断本次改动是否值得写入 history
- 判断应该归入 `Added`、`Changed/Refactor`、`Fixed` 哪一类
- 直接替 agent 产出最终历史内容

`history validate` 负责：

- 用 `markdown-it-py` 解析 markdown 结构
- 校验 release heading、section heading、bullet marker、版本顺序
- 检查是否残留 `TODO(agent)` 占位内容

`history validate` 不负责：

- 判断历史条目是否“值得写”
- 判断文案是否足够好
- 替 agent 做语义归类

## Hard Rules

- 最新版本必须写在最前面
- 版本标题格式必须为 `## [vX.Y.Z] - YYYY-MM-DD`
- 日期必须使用 ISO 8601：`YYYY-MM-DD`
- 分类只使用以下几类：
  - `### 新增功能 (Added)`
  - `### 技术改进/重构 (Changed/Refactor)`
  - `### 修复 (Fixed)`
  - `### 安全性 (Security)`
  - `### 弃用/删除 (Deprecated/Removed)`
- 列表统一使用 `* `
- 空分类不要保留
- 条目应以动词开头，简短直接
- 文件名、命令、路径、配置键统一使用反引号

## Agent Judgment Rules

- 只写用户可感知或工程上重要的变化
- 同类改动尽量合并表达，不把文件清单直接抄进 history
- 涉及目录迁移时，明确写出路径
- 若涉及版本号、配置格式、构建方式变化，应明确写出旧口径与新口径
- 如果 `history prep` 的版本提示和实际改动不一致，以仓库中真实落盘内容和当前改动语义为准
- 写 Android history 时，优先按“用户可感知能力 + 前端边界 + 工程入口”归纳，不要按 UI/data/native 文件层次逐项抄写

## Format Guidance

- 默认推荐：`--format markdown`
  - 最适合 agent 直接阅读、提炼和改写
  - 是当前 history workflow 的首选输出格式
- 如果目标文件已经明确，优先配合 `--target`
  - 版本号优先来自目标 history 文件名，例如 `docs/libs/v0.4/0.4.1.md -> v0.4.1`
  - 日期仍然来自当前工作日期
- 轻量阅读模式：`--format plain`
  - 适合只想快速扫重点、不想看 markdown 或 json 包装时使用
- 结构化备选：`--format json`
  - 适合需要更稳定字段结构时使用
  - 不是当前 agent-only history 编写流程的默认推荐格式
- 如果只写单一范围，优先配合 `--scope`
- `history prep` 给出的 candidate topics 和 representative files 只是一组阅读入口，不是最终 bullet 清单
- 如果需要把上下文按 bucket 拆给 agent 分块读取，优先配合：
  - `--out-dir temp/history-prep`
  - `--split-by bucket`

## Minimal Template

```md
## [vX.Y.Z] - YYYY-MM-DD

### 新增功能 (Added)
* 新增 `<feature or file>`

### 技术改进/重构 (Changed/Refactor)
* 重构 `<module or workflow>`

### 修复 (Fixed)
* 修复 `<bug or regression>`

### 安全性 (Security)
* 修复 `<security issue>`

### 弃用/删除 (Deprecated/Removed)
* 删除 `<removed item>`
```

## Usage Note

当改动频繁、history 结构高度重复时，优先走 `history prep --format markdown -> agent 重写 -> history validate -> 落盘` 这条链路；不要把工具输出当作最终 history 直接提交。
