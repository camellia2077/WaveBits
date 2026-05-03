# Android Native Strategy

更新时间：2026-04-30

## 当前固定路线
- Android native 当前固定继续使用：
  - `externalNativeBuild + CMake 4.1.2 + C++23 + bag_api.h + audio_runtime.h + package-private audio_io wrapper`
  - app `CMake` 通过 `apps/audio_android/native_package/` 消费 `bag_android_native`
- `native_package` 当前只编译 `audio_core` package-owned implementation sources、`bag_api` / `audio_runtime` package-owned boundary implementation、`audio_io` package-private wrapper 与 `android_bag/**` / `android_audio_io/**` 私有声明层，不再直接 source-own 主仓原始实现文件
- 当前 `flash` 路径下，Android package lane 已补齐 `signal + voicing + phy_clean` 的 package-owned source owner；Android 继续通过 `bag_api.h` 复用 formal `flash` 的 signal profile + voicing emotion 语义，并通过同一份稳定 config 选择 `Steady / Hostile / Litany / Collapse / Zeal / Void` preset。
- 当前 `mini` 路径下，Android package lane 已补齐 `codec + phy_clean` 的 package-owned source owner；Android 继续通过同一 `bag_api.h` encode/decode/validate 入口访问 Morse code 模式，并用稳定的 `frame_samples` 字段承接 `Slow / Standard / Fast` speed preset。
- 这条路线是当前长期例外路径，不属于 host modules 主路径的一部分。
- 当前不把 Android native 直接并入 named modules 迁移主线。

## 当前实验入口
- 当前额外保留一条 opt-in 实验线：
  - `python tools/run.py android modules-smoke`
- 这条线当前只证明两件事：
  - Android packaging graph 可以直接编译并消费主仓 `bag.common.version` module/source
  - `audio_core_common_version.cpp` 可以在 opt-in lane 下退出 Android package-owned source list
- 这条线当前不证明：
  - Android 已正式具备 host-style `import std;`
  - Android 已可直接承接完整 `bag_core` target
- 当前本机实测阻塞是：
  - 基线环境里存在 `msys64` provider 污染
  - 但 clean PATH + fresh `.cxx` 目录后的真实状态仍是：
    - `blocked-missing-libcxx-modules-json`
  - 因此 Android `import std;` 仍应继续作为后续独立 toolchain gate
- 当前正式状态文档见：
  - `docs/notes/android-import-std-toolchain-status.md`
- 如需判断未来是否值得升级 Android `NDK/CMake`，见：
  - `docs/notes/android-ndk-cmake-upgrade-decision.md`

## 现阶段的硬规则
- Android JNI 编解码能力继续通过 `bag_api.h` 访问
- Android JNI 播放运行时能力继续通过 `audio_runtime.h` 访问
- Android JNI 的 WAV bytes 能力只通过 `native_package` 私有 `audio_io` wrapper 访问
- Android `flash` emotion preset 继续通过稳定 `bag_api.h` 配置面进入 core，不新增 flash-only 私有 encode/decode JNI 入口，也不回退到 Android-only 参数分叉
- Android `mini` Morse mode 继续通过稳定 `bag_api.h` 配置面进入 core，不新增 mini-only 私有 encode/decode JNI 入口；speed preset 只改变传入 core 的 `frame_samples`
- Android JNI 不直接 `#include "bag/..."`
- Android JNI 不直接 `import bag.*`
- Android app `CMake` 不再直接 `add_subdirectory(libs/audio_core)` 或 `add_subdirectory(libs/audio_api)`
- Android native packaging target 不直接对 app 暴露 `audio_core/include` 或 `audio_io/include`
- Android native packaging target 不直接编译 repo 原始 `bag_api.cpp + audio_core/src/*.cpp` source list
- Android app 层不直接 `#include "wav_io.h"`；如需 `wav <-> pcm` 逻辑，必须经由 package-private wrapper

## 为什么当前不直接切 Android modules
- Android `externalNativeBuild` 当前已固定到 `CMake 4.1.2 + C++23`，但这只解决 app-side native baseline，不等于 Android 已具备 host named modules 工作流
- 当前 host modules 路径依赖的 named modules 工作流，不适合直接原样搬到这条 Android 工具链
- JNI 层本身是平台边界，优先级应是稳定消费 `bag_api.h`，而不是直接碰内部实现模块

## 与 host modules 主线的关系
- host 默认路径继续走：
  - `clang++`
  - `Ninja`
  - named modules
- Android 不直接跟随这条主路径推进：
  - 不直接 `import bag.*`
  - app JNI 只消费 `bag_android_native -> bag_api.h / audio_runtime.h / package-private audio_io wrapper`
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
