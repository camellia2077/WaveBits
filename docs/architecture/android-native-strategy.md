# Android Native Strategy

更新时间：2026-03-13

## 当前固定路线
- Android native 当前固定继续使用：
  - `externalNativeBuild + CMake 3.22.1 + C++17 + bag_api.h`
  - app `CMake` 通过 `apps/audio_android/native_package/` 消费 `bag_android_native`
- `native_package` 当前只编译 package-private wrapper 与 `android_bag/**` 私有声明层，不再直接 source-own `bag_api.cpp + 8` 个 `audio_core` 原始实现文件
- 这条路线是当前长期例外路径，不属于 host modules 主路径的一部分。
- 当前不把 Android native 直接并入 named modules 迁移主线。

## 现阶段的硬规则
- Android JNI 源码只通过 `bag_api.h` 访问内核能力
- Android JNI 不直接 `#include "bag/..."`
- Android JNI 不直接 `import bag.*`
- Android app `CMake` 不再直接 `add_subdirectory(libs/audio_core)` 或 `add_subdirectory(libs/audio_api)`
- Android native packaging target 不直接对 app 暴露 `audio_core/include` 或 `audio_io/include`
- Android native packaging target 不直接编译 repo 原始 `bag_api.cpp + audio_core/src/*.cpp` source list

## 为什么当前不直接切 Android modules
- Android `externalNativeBuild` 仍固定在 `CMake 3.22.1`
- 当前 host modules 路径依赖的 named modules 工作流，不适合直接原样搬到这条 Android 工具链
- JNI 层本身是平台边界，优先级应是稳定消费 `bag_api.h`，而不是直接碰内部实现模块

## 与 host modules 主线的关系
- host 默认路径继续走：
  - `WAVEBITS_HOST_MODULES=ON`
  - `clang++`
  - named modules
- Android 不直接跟随这条主路径推进：
  - 不直接 `import bag.*`
  - app JNI 只消费 `bag_android_native -> bag_api.h`
  - 不直接暴露 `audio_core/include`
  - 不直接暴露 `audio_io/include`
- 这两条路线当前是明确分开的，而不是“暂时混用、以后再决定”。

## 如果未来要继续推进 Android native
- Android named modules
- Android 消费预编译 native 产物
- Android 与 host 产物分发 / ABI / packaging 解耦

以上都应作为新的独立议题单开，而不是当前主线的隐含下一步。

## 验收口径
- Android focused gate：
  - `python tools/run.py android native-debug`
  - `python tools/run.py android assemble-debug`
- Android JNI 继续稳定依赖 `bag_api.h`
- Android native 构建不因 host modules 收口而被破坏
- Android 路线保持为明确、稳定、可重复验证的例外路径
