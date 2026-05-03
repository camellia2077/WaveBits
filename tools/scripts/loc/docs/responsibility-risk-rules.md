# Responsibility Risk Rules

`tools/scripts/loc` 里的职责混杂扫描目前是“启发式预警”，不是 AST 级设计审查。
它的目标是尽快把值得人工复查的文件筛出来，而不是替代架构判断。

## Kotlin 当前规则

当前 Kotlin 扫描关注 5 组信号：

- 行数
  - 超过 `responsibility_line_threshold` 加 2 分
  - 超过 `responsibility_line_threshold + 120` 再加 1 分
- 状态信号
  - 统计 `remember*`、`mutableStateOf`、`LaunchedEffect`
  - 命中数达到 `responsibility_state_signal_threshold` 开始加分
  - 再按 `+2`、`+5` 的阶梯继续加分
- 顶层 `@Composable`
  - 只统计顶层 `@Composable fun`
  - 数量达到 `responsibility_top_level_composable_threshold` 开始加分
  - 再按 `+2`、`+4` 的阶梯继续加分
- 命名角色种类
  - 统计顶层 composable 名字里是否出现：
    - `Section`
    - `Block`
    - `Card`
    - `Switcher`
    - `Timeline`
  - 命中的角色种类数达到 `responsibility_role_kind_threshold` 后开始加分
- 模式分支
  - 统计 `if/when` 中和 `Mode`、`selected*`、`viewMode`、`displayMode` 相关的分支
  - 命中数达到 `responsibility_mode_branch_threshold` 后开始加分

当前优先级分档：

- `score >= 8` -> `P0`
- `score >= 6` -> `P1`
- `score >= 4` -> `P2`
- 其他 -> `P3`

Kotlin 的设计意图：

- 把“页面入口 + 多个 section/block + 多状态 + 多模式切换”这类文件优先打出来
- 不试图判断 UI 设计本身对不对
- 更偏向“找出值得拆分讨论的文件”

## Python 第一版规则

Python 第一版也保持保守，只做“脚本/模块级职责混杂预警”。
它同样使用 5 组信号，但信号内容换成 Python 语境。

### 1. 行数

- 超过 `responsibility_line_threshold` 加 2 分
- 超过 `responsibility_line_threshold + 120` 再加 1 分

建议默认阈值：

- `responsibility_line_threshold = 220`

### 2. 状态与副作用信号

统计这些模式的命中次数：

- `self.`
- `global`
- `nonlocal`
- `os.environ`
- `threading`
- `asyncio`
- `subprocess`
- `requests`

设计意图：

- 不是说这些模式本身不好
- 而是“文件已经偏大时，又同时持有状态、环境依赖和副作用”更值得复查

建议默认阈值：

- `responsibility_state_signal_threshold = 4`

### 3. 顶层符号数量

统计顶层：

- `def`
- `async def`
- `class`

设计意图：

- 如果一个 Python 文件里堆了很多顶层定义，它更容易同时承担解析、协调、IO、格式化等多种职责

建议默认阈值：

- `responsibility_top_level_composable_threshold = 6`

说明：

- 这里沿用了现有配置字段名，虽然名字里还是 `composable`
- 在 Python 语境里，它实际代表“顶层符号数”

### 4. 命名角色种类

统计顶层符号名里是否出现这些角色词：

- `Manager`
- `Service`
- `Controller`
- `Handler`
- `Client`
- `Builder`
- `Parser`
- `Formatter`
- `Loader`
- `Writer`

设计意图：

- 一个文件里如果同时出现很多不同角色名，通常是在混搭多类职责

建议默认阈值：

- `responsibility_role_kind_threshold = 2`

### 5. 模式分支

统计这些模式分支信号：

- `if ... mode ==`
- `if ... kind ==`
- `if ... type ==`
- `elif ... mode ==`
- `elif ... kind ==`
- `elif ... type ==`
- `match`
- `case`

设计意图：

- 一个文件里如果有很多“按 mode/kind/type 切多路逻辑”的代码，往往意味着多个执行路径被揉在同一个模块里

建议默认阈值：

- `responsibility_mode_branch_threshold = 2`

## C++ 第一版规则

C++ 第一版同样保持保守，目标是先把“超大实现文件 + 桥接/规则/lifecycle 混在一起”的候选打出来。

### 1. 行数

- 超过 `responsibility_line_threshold` 加 2 分
- 超过 `responsibility_line_threshold + 140` 再加 1 分

建议默认阈值：

- `responsibility_line_threshold = 360`

### 2. 共享状态 / 线程原语

统计这些模式的命中次数：

- `std::mutex`
- `std::atomic`
- `std::condition_variable`
- `thread_local`
- `std::thread`
- `std::future`
- `std::promise`

设计意图：

- 大文件里如果同时持有共享状态和线程原语，更容易把协调、资源管理和业务逻辑揉在一起

### 3. 顶层符号数量

启发式统计顶层：

- `class / struct / enum`
- 顶层函数定义

设计意图：

- 如果一个 `.cpp/.inc` 文件同时堆了很多 helper、桥接函数和实现函数，通常已经在承担多类职责

### 4. 命名角色种类

统计顶层符号名里是否出现这些角色词：

- `Bridge`
- `Codec`
- `Api`
- `Parser`
- `Builder`
- `Factory`
- `Adapter`
- `Facade`
- `Runtime`
- `Registry`
- `Manager`
- `Support`
- `Test`

设计意图：

- 一个文件里如果同时命中很多不同角色名，往往意味着桥接、规则、runtime、测试 support 被混搭在一起

### 5. 模式分支

统计和这些词相关的 `if/switch/case`：

- `mode`
- `style`
- `profile`
- `state`
- `phase`
- `kind`
- `flavor`

设计意图：

- 多个执行路径揉在同一实现文件里时，`mode/style/state` 分支通常会显著增多

### 6. 桥接 / FFI 表面

统计一个文件同时命中的桥接类别数：

- JNI 表面
- C ABI 表面
- 封送 / marshalling helper

对应字段：

- `interop_surface_hits`

设计意图：

- `jni_bridge.cpp`、`audio_io_jni.cpp`、大 C API facade 这类文件，问题往往不是“单个 helper 太长”，而是桥接表面和规则实现同时增长

### 7. 资源生命周期信号

统计这些 ownership / 生命周期模式的命中密度：

- `new/delete`
- `unique_ptr/shared_ptr`
- `lock_guard/unique_lock`
- `Release/Destroy/Cancel/Close/Free`
- `GetStringUTFChars/ReleaseStringUTFChars`

对应字段：

- `resource_lifecycle_hits`

设计意图：

- 一旦资源获取、释放、取消、封送都混在一个超大文件里，可读性和修改风险都会显著上升

## 当前边界

当前职责混杂扫描的边界是：

- 只做文件级预警
- 不做 AST 级数据流分析
- 不判断函数是否纯函数
- 不直接给出“必须拆分”的结论
- 输出主要用于：
  - 发现可疑大文件
  - 排序拆分优先级
  - 作为后续人工 review 的入口
