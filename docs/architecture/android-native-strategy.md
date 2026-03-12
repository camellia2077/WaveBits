# Android Native Strategy

更新时间：2026-03-11

## 当前结论
- 当前正式策略采用方案 A：
  - 继续保留 `externalNativeBuild + CMake 3.22.1 + C++17 + bag_api.h`
- 这不是因为 modules 不重要，而是因为 Android native toolchain 仍然是当前迁移里的现实约束点。

## 现阶段的硬规则
- Android JNI 源码只通过 `bag_api.h` 访问内核能力
- Android JNI 不直接 `#include "bag/..."`
- Android JNI 不直接 `import bag.*`
- Android native target 不直接暴露 `audio_core/include` 或 `audio_io/include`

## 为什么当前不直接切 Android modules
- Android `externalNativeBuild` 仍固定在 `CMake 3.22.1`
- 当前 host modules 路径依赖的 named modules 工作流，不适合直接原样搬到这条 Android 工具链
- JNI 层本身是平台边界，优先级应是稳定消费 `bag_api.h`，而不是直接碰内部实现模块

## 备选路线

### 方案 A：继续维持当前路线
- 继续用：
  - `externalNativeBuild`
  - `CMake 3.22.1`
  - `C++17`
  - `bag_api.h`
- 优点：
  - 改动最小
  - 平台边界最稳定
  - 不阻塞当前 Android 交付
- 缺点：
  - Android 不会直接受益于 host modules 内部组织优化

### 方案 B：改为消费预编译 native 产物
- Android 不再直接编 `libs/*`
- 改为消费仓库根目录预编译出的 native library
- 优点：
  - Android 可以彻底和 host 内部 modules 实现解耦
  - 更符合“平台层只消费稳定边界”的方向
- 缺点：
  - 需要重新设计产物分发、ABI 管理和 Gradle / packaging 流程
  - 实施成本明显高于方案 A

## 当前建议
- 短中期继续执行方案 A
- 仅在以下条件同时成立时，再认真推进方案 B：
  - Android 交付频率要求值得引入预编译 native 分发
  - ABI / packaging 方案已有明确设计
  - 团队愿意把 Android native 从“源码同编”切换到“消费产物”

## 验收口径
- Android JNI 继续稳定依赖 `bag_api.h`
- Android native 构建不因 host modules 收口而被破坏
- Android 路线不再停留在“以后再看”，而是有明确默认策略和升级条件
