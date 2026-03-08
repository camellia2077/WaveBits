# Future Plan

## 总体方向
当前方向正确：采用“输入解耦 + 统一解释 + 通用处理”的分层架构。

核心目标：先把 `音频 -> bitstream -> 文本` 在 C++ 中稳定跑通，再逐步扩展到光、震动等输入形式。

## 架构建议

### 1. 输入适配层（InputAdapter）
- 定义统一输入接口，屏蔽不同硬件/信号源差异。
- 各输入类型独立实现：`AudioAdapter`、`LightAdapter`、`VibrationAdapter`。
- 职责仅限采样与基础预处理，不耦合解码逻辑。

### 2. 解释层（Interpreter）
- 将不同信号统一转换为中间表示（IR）。
- 推荐 IR：`bitstream + timestamp + confidence`。
- 保持可替换，便于迭代不同解释算法。

### 3. 处理层（Processor）
- 输入统一 IR，输出业务结果（文本）。
- 负责分帧、纠错、解码、文本还原。
- 与输入类型无关，保证复用性。

### 4. 编排层（Pipeline）
- 负责连接 `adapter -> interpreter -> processor`。
- 支持组件替换与配置化组合，便于测试和扩展。

## 实施原则
- KISS：先实现最小可用闭环，不提前做复杂抽象。
- YAGNI：在未引入第二种输入前，接口只保留当前必需能力。
- DRY：公共逻辑放入 Processor/Utils，避免在不同输入实现中重复。
- SOLID：每层职责单一，通过接口隔离依赖，降低耦合。

## MVP 暂不做（持续补充）

### `pro/ultra` 长文本处理
- [x] MVP 固定采用“单帧上限，超长报错”策略：`pro/ultra` 当前只支持单帧消息，`payload` 上限为 `512` 字节，超出直接返回错误。
- [ ] 不做自动拆帧、多帧顺序控制与重组。
- [ ] 不做流式长文本会话与跨帧状态管理。

## 近期里程碑
1. 打通 C++ 最小闭环：`AudioAdapter + BasicInterpreter + TextProcessor`。
2. 增加基础测试数据与回归测试，保证结果稳定。
3. 固化 IR 结构与模块边界。
4. 再引入 `LightAdapter` 或 `VibrationAdapter` 验证架构扩展性。

## 风险与规避
- 风险：过早抽象“通用解释器”导致复杂度上升。
- 规避：先单输入跑通并沉淀边界，再逐步泛化。
