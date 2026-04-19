---
description: Agent 专用文件命名模板
---

# File Name Template

本文件只定义 agent 新建或重命名文件时必须遵守的最小规则与模板。

如需先做保守的机械检查，可使用：

```powershell
python tools/run.py file-name prep
```

该命令只生成 agent 可继续改写的草稿，默认输出到 `temp/file-name/message.txt`，不替 agent 做最终命名决策。

## Mechanical Rules

- 默认使用 `snake_case`
- 避免使用 `utils`、`common`、`temp`、`misc` 等模糊命名
- 优先使用 `<domain>_<role>.<ext>` 结构
- 可直接命中的模板优先按模板识别，不必强行重写成别的形状

## Agent Judgment

- 文件名必须表达职责，而不只是表达实现细节
- 接口、数据载体、schema、registry、config 等“职责清晰的小文件”允许独立存在，不因文件短小而强行合并
- 若文件同时承担多个职责，应拆分而不是继续叠加命名
- 模板命中后，仍需判断该模板是否掩盖了更准确的职责语义

## Naming Templates

- 接口：`i_<capability>.hpp`
- 配置：`<domain>_config.hpp`
- 类型集合：`<domain>_types.hpp`
- 请求/响应：`<domain>_requests.hpp` / `<domain>_responses.hpp`
- DTO：`<domain>_dto.hpp`
- 模型：`<domain>_model.hpp` / `<domain>_models.hpp`
- Schema：`<domain>_schema.<ext>`
- Registry：`<domain>_registry.<ext>`
- Manifest：`<domain>_manifest.<ext>`
- Aliases：`<domain>_aliases.<ext>`

## Split Rules

- 仅声明接口时，保留独立头文件
- 仅承载结构体、枚举、配置、schema 时，保留独立文件
- 出现流程控制、I/O、数据库读写或复杂业务逻辑时，不应继续塞进“types/config/schema”类文件

## Generic Example

```text
i_formatter.hpp
report_config.hpp
query_requests.hpp
storage_schema.hpp
plugin_registry.cpp
```
