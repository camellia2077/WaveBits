# Tools Docs

`docs/tools/` 只记录仓库内部开发工具与工作流的长期文档，不承载 `libs` / `cli` / `android` 的产品版本历史。

## 目录口径

- `history/`
  - 记录 `tools/`、agent workflow、repo-internal tooling 相关的重要演进
  - 文件名固定使用日期：`YYYY-MM-DD.md`
  - 不使用 `vX.Y.Z` 版本树

## 适合写进 `docs/tools/history/` 的内容

- `python tools/run.py` 的命令入口、分组、默认工作流变化
- `tools/repo_tooling/` 内部架构重构，只要它改变了 agent / 开发者可感知的使用方式
- `history` / `message` / `file-name` 这类辅助工作流的新增、重构或口径变化
- 静态 policy、validate、artifact、Android build orchestration 等工具侧重要收口

## 不适合写进 `docs/tools/history/` 的内容

- 纯产品功能变化，且 tooling 本身没有用户可感知变化
- 只影响单个组件实现、但没有改变工具入口或开发工作流的内部重排
- 临时调试脚本、一次性草稿、未正式采用的实验命令

## 约定

- 同一天内如果有多轮 tooling 改动，优先合并到同一个日期文件，而不是按批次继续细分文件名
- 如需写作规范，统一参考：
  - `C:\code\WaveBits\.agent\workflow\docs\tools-history-style-guide.md`
