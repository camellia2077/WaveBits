---
description: Agent 专用 Git 提交消息模板
---

# Git Commit Message Template

本文件只定义 agent 生成或重写 Git commit message 时必须遵守的最小规则与模板。

## Allowed Types

- `feat`
- `feat!`
- `fix`
- `refactor`
- `docs`
- `perf`
- `chore`

## Hard Rules

- 代码改动禁止使用 `docs`
- 纯文档改动才允许使用 `docs`
- `subject` 必须简短直接，不写表情，不写句号
- `Release-Version: <version>` 必须为最后一行
- `Release-Version` 只能出现一次，且只能表示本次对外发布版本
- 存在 breaking changes 时，使用 `feat!` 或添加 `[Breaking Changes]`
- 空 section 不要保留
- `squash` 或 `reword` 后，不要保留 `Squashed commits:` 或原 commit 列表
- 涉及中文 commit message 的生成、落盘、amend、reword 或 `--file` 提交时，优先使用 `pwsh` / `pwsh.exe`
- 需要将提交信息写入文件时，使用 `pwsh` 的 UTF-8 输出（如 `Set-Content -Encoding utf8`），避免中文乱码

## Workflow

- 默认工作流是：`history -> message`
- 生成 commit message 草稿时，优先读取已落盘 history，而不是重新从 git 改动重建一遍语义
- 可先用 `python tools/run.py message prep --history <path>` 生成 `temp/message.txt`
- 只有当当前变更还没有明确 history 文件时，才允许退回 git-based scaffold 作为临时兜底

## Template

```text
<type>: <subject>

[Summary]
<1-3 行摘要>

[Component Versions]
- <component-name>: <version-or-status>

[Breaking Changes]
- <breaking change>

[Added]
- <added item>

[Changed & Refactored]
- <changed item>

[Fixed]
- <fixed item>

[Verification]
- <verification step>

Release-Version: vX.Y.Z
```

## Section Rules

- `[Summary]` 必填
- `[Component Versions]` 可选；仅在需要说明 libs / cli / android presentation 等子系统独立版本线或版本状态时出现
- `[Verification]` 必填
- `[Breaking Changes]` 仅在存在 breaking changes 时出现
- `[Added]`、`[Changed & Refactored]`、`[Fixed]` 按实际改动保留
- 列表项统一使用 `- `

## Release-Version Semantics

- `Release-Version` 表示本次对外发布版本，不等于某单一组件的内部实现版本
- 当 core / cli / android 在同一发布批次内共同交付时，commit message 中的 `Release-Version` 必须统一为同一个发布版本
- 组件内部版本（如构建号、内部协议号）可以独立演进，但不得替代 `Release-Version`
- 组件内部版本、子系统版本线或“未变化/仅文档同步”等信息，应写在 `[Component Versions]` 中，而不是追加第二个 `Release-Version`
- `[Component Versions]` 的职责是帮助区分 `libs`、`presentation`、`cli` 等子系统状态；它是补充说明，不是发布版本来源
- 补写 `[Component Versions]` 时，优先以 `docs/presentation/android` 或 `docs/presentation/cli` 中已落盘的 history/version 口径为准；若 docs 未明确，再参考代码中的模块版本号；仍不明确时使用 `changed` / `unchanged` 之类的状态描述

## Generic Example

```text
refactor: simplify module structure

[Summary]
整理模块边界并统一默认入口，减少重复实现。

[Component Versions]
- android-presentation: v0.4.2
- libs/tracer_core: unchanged

[Changed & Refactored]
- 调整目录结构
- 合并重复逻辑
- 同步更新相关脚本

[Verification]
- 运行项目构建
- 运行相关测试

Release-Version: vX.Y.Z
```
