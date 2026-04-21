# Tooling Workflow Overview

更新时间：2026-04-19

## 目标

- 把仓库命令入口统一到 `python tools/run.py`
- 让总说明、工具入口和专题细节各自只承担一层职责
- 避免在多个文档里重复维护超长命令清单

## 分层

- 总说明：
  - `<repo-root>/.agent/AGENTS.md`
  - 只保留工作方式、命令分层原则和高频入口
- 工具入口：
  - `<repo-root>/tools/README.md`
  - 只保留命令组地图、少量代表例子和专题文档索引
- 详细专题：
  - 构建/验证：`<repo-root>/docs/notes/build-commands.md`
  - 产物工作流：`<repo-root>/docs/notes/artifact-workflow.md`
  - history 工作流：`<repo-root>/docs/notes/history-workflow.md`
  - Android 专题：`<repo-root>/docs/notes/android/android-compile.md`
  - clang 专题：`<repo-root>/docs/notes/clang/cmds.md`

## 命令组

- `build / test / verify`
  - host 构建、单测、正式验证
- `clang`
  - native 代码质量工具
- `android`
  - Android Gradle 构建与 Kotlin 质量 gate
- `artifact`
  - 可见测试产物与最终交付产物
- `history`
  - release-history 草稿与结构校验

## 使用方式

先看顶层：

```powershell
python tools/run.py --help
```

再按命令组下钻：

```powershell
python tools/run.py android --help
python tools/run.py artifact --help
python tools/run.py history --help
```

需要更具体的流程、约束和示例时，再看对应 docs 专题，而不是继续扩充总说明文档。
