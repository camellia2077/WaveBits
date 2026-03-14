# Android `import std;` Toolchain Status

更新时间：2026-03-14

## 推荐阅读顺序
- `1/2`：先读本文件，确认当前 Android `import std;` 的正式状态。
- `2/2`：再读 `docs/notes/android-ndk-cmake-upgrade-decision.md`，判断未来是否值得升级 `NDK/CMake`。
- 交接给后续 agent 时，建议按以上顺序发送。

## 文档定位
- 这份文档只记录当前 Android `import std;` 的正式状态口径。
- 它回答的是“当前这条 Android lane 是否已经具备标准库模块 provider”。
- 它不重开 Android owner 迁移计划，也不替代：
  - `docs/architecture/android-native-strategy.md`
  - `docs/testing.md`
- 如需判断后续是否值得升级 `NDK/CMake`，见：
  - `docs/notes/android-ndk-cmake-upgrade-decision.md`

## 一句话结论
- 对当前这条正式 Android lane：
  - `Windows + externalNativeBuild + CMake 4.1.2 + NDK 28.2.13676358`
  - `C++23` baseline 已成立
  - 最小 Android named-modules owner shift 已成立
  - 但标准库模块 provider 仍未成立
- 因此当前不能把这条 Android lane 写成“已经支持 host-style `import std;`”。

## 当前适用范围
- 主机平台：
  - Windows
- Android native 构建入口：
  - `externalNativeBuild`
- 当前工具链组合：
  - `AGP 9.0.1`
  - `Gradle 9.2.1`
  - `CMake 4.1.2`
  - `NDK 28.2.13676358`
  - `Clang 19.0.1`
- 当前验证 lane：
  - `python tools/run.py android modules-smoke`
  - `python tools/run.py android native-debug`

## 当前成立的事实
1. Android 当前已经稳定运行在 `C++23` native baseline 上。
2. Android 当前 `modules-smoke` 已能验证最小 direct-owner shift：
   - `bag.common.version`
3. Android 当前 clean PATH + fresh `.cxx` 目录下，`modules-smoke` 与 `native-debug` 都可重复通过。
4. 这说明：
   - Android 不等于“完全没有 C++23”
   - Android 也不等于“完全不能做 named modules”

## 当前不成立的事实
1. 当前这条 Android lane 不能被写成“已经正式支持 `import std;`”。
2. 当前这条 Android lane 不能被写成“已经具备 NDK-local `libc++` standard-library module provider”。
3. 当前 `modules-smoke` 不能被解释成 Android `import std;` gate 已通过。

## 核心证据

### 1. 环境污染隔离后，provider 仍然缺失
- 在去掉 `C:\msys64\*` 的 clean PATH 下，fresh `.cxx` 目录中的最新 report 为：
  - `cxx_import_std=unavailable`
  - `libcxx_modules_json_query=libc++.modules.json`
  - `libcxx_modules_json_exists=0`
  - `libcxx_modules_json_origin=missing`
  - `import_std_status=blocked-missing-libcxx-modules-json`
- 当前可复核位置：
  - `apps/audio_android/app/.cxx/Debug/4x2ks4c4/arm64-v8a/deps/modules_smoke/android-modules-smoke-report.txt`
  - `apps/audio_android/app/.cxx/Debug/4x2ks4c4/x86_64/deps/modules_smoke/android-modules-smoke-report.txt`

### 2. 这不是“只差清 PATH”就能启用的状态
- 基线环境下，bare probe 会误命中外部：
  - `C:/msys64/ucrt64/lib/libc++.modules.json`
- 但 clean PATH + fresh `.cxx` 之后，状态会收敛成：
  - 不再命中 `msys64`
  - 同时也没有找到 Android NDK-local provider
- 因此：
  - `msys64` 污染是真问题
  - 但清掉污染后，当前 Android lane 的真实状态仍然是 provider 缺失

### 3. `C++23` 与 `import std;` 不是同一件事
- 当前 Android lane 已经证明：
  - `C++23` 编译模式可用
  - 最小 named-modules owner shift 可用
- 但这不自动推出：
  - `import std;` 可用
- 当前更准确的表述应是：
  - `C++23` 已成立
  - `import std;` provider 未成立

## 当前允许的对外表述
- “Android native 当前固定到 `CMake 4.1.2 + C++23` baseline。”
- “Android `modules-smoke` 当前证明的是最小 named-modules owner shift，不是 Android `import std;` 正式支持。”
- “当前 Windows Android lane 的标准库模块 provider 仍未成立，因此不能把它写成 host-style `import std;` lane。”

## 当前不允许的表述
- “Android 已经支持 `import std;`。”
- “Android 现在和 host 一样已经具备完整标准库模块工作流。”
- “`modules-smoke` 通过就等于 Android `import std;` gate 已通过。”

## 对当前计划的含义
1. Android `Phase 3A` 可以继续做不依赖 `import std;` 的最小 owner shift。
2. `bag.flash.codec` 这类依赖 `import std;` 的 Android direct-owner 扩面，不应在当前状态下误写成“已具备正式工具链入口”。
3. `_impl.inc` / `bag_api_impl.inc` 的最终退场，仍应继续以 Android toolchain gate 为前提，而不是仅凭 Android `C++23` baseline 判断。

## 重新评估前应满足什么
- 如果未来要重新评估 Android `import std;`，至少应同时满足：
1. clean PATH 下 bare probe 不再命中外部 provider。
2. clean PATH + fresh `.cxx` 目录下，report 不再是：
   - `blocked-missing-libcxx-modules-json`
3. `modules-smoke --clean`、`modules-smoke`、`native-debug` 在多 ABI 下可重复通过。
4. 不依赖旧 `.cxx` 缓存或历史 configure 结果。
