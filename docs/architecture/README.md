# Architecture 文档

更新时间：2026-03-11

## 目的
- 说明仓库结构与模块边界。
- 提供“修改某类功能时先看哪些文件”的地图。
- 帮助人类和 agent 减少无效扫描。

## 当前文件
- `docs/architecture/android-app-architecture.md`
  - Android 应用层职责、分层结构、JNI 调用链与目录定位。
- `docs/architecture/repo-map.md`
  - 仓库结构、`libs/` 文件分布、建议阅读顺序、通常可后看的兼容文件。
- `docs/architecture/module-topology.md`
  - host 默认 modules 拓扑、compatibility layer、以及当前不能直接走 modules 的目标说明。
- `docs/architecture/compatibility-layer-inventory.md`
  - compatibility header 分级、长期边界头、消费端 allowed surface 盘点。
- `docs/architecture/android-native-strategy.md`
  - Android native 当前默认路线、边界规则、以及后续升级条件。
