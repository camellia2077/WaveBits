# 仓库结构与文件地图

更新时间：2026-03-15

## 目标
这份文档用于快速回答两个问题：
- 某类功能代码大概在哪些文件里？
- 改某个模块时，应该先看哪些文件，而不是把整个目录从头扫一遍？

## 仓库大区块
- 仓库根目录
  - 统一入口，包含 `CMakeLists.txt`、`tools/run.py`、Android `Gradle` root 与 CI/workflow。
- `libs/`
  - 共享库主区，包含 `audio_core`、`audio_api`、`audio_io`、`audio_runtime`。
- `apps/`
  - 平台表现层与集成，目前主要是 `audio_cli`、`audio_android`。
- `Test/`
  - integration / artifact / cli / modules 测试总入口。
- `docs/`
  - 架构、测试、发布、设计说明。
- `tools/`
  - Python 编排脚本与静态门禁实现。

## `libs/` 总览

### `libs/audio_core`
- 作用：模式逻辑、编码/解码主链路、transport 分发。
- 最常先看的文件：
  - `libs/audio_core/modules/bag/**`
  - `libs/audio_core/src/transport/transport.cpp`
  - `libs/audio_core/modules/bag/common/config.cppm`
  - `libs/audio_core/modules/bag/common/types.cppm`
  - `libs/audio_core/modules/bag/transport/facade.cppm`
- 当前边界：
  - host 默认主线直接消费 named modules
  - 主仓 `src/*.cpp` 已切到 modules-only host 形态
  - 主仓 `bag/internal/**` owner 已清零；预留接口头的声明层独立固定在 `bag/interface/common/*`

### `libs/audio_api`
- 作用：稳定 C API，供 CLI、JNI 和未来其他平台层调用。
- 最常先看的文件：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api.cpp`
- 当前边界：
  - `bag_api.h` 继续保持 C ABI
  - `bag_api.cpp` 的主仓实现已切到 modules-only host 形态

### `libs/audio_io`
- 作用：WAV 读写边界，统一承接 `wav bytes <-> mono PCM16` 与 `path <-> WAV 文件`。
- 最常先看的文件：
  - `libs/audio_io/modules/audio_io/wav.cppm`
  - `libs/audio_io/modules/audio_io/wav_impl.cpp`
  - `libs/audio_io/include/wav_io.h`
  - `libs/audio_io/src/wav_io.cpp`
  - `libs/audio_io/src/wav_io_backend.h`
  - `libs/audio_io/src/wav_io_backend.cpp`
- 当前边界：
  - `wav_io.h` 是长期保留 header boundary，同时对外暴露 bytes-based 与 path-based WAV 能力
  - `audio_io.wav` 是 host 内部优先入口
  - Android 如需复用 WAV bytes 逻辑，应通过 `native_package` 私有 wrapper 间接接入
  - `sndfile` include token 只允许停留在 `wav_io_backend.cpp`

### `libs/audio_runtime`
- 作用：平台无关的播放会话状态机、样本位置/时间换算、seek 语义。
- 最常先看的文件：
  - `libs/audio_runtime/include/audio_runtime.h`
  - `libs/audio_runtime/src/audio_runtime.cpp`
  - `libs/audio_runtime/src/audio_runtime_impl.inc`
- 当前边界：
  - `audio_runtime.h` 是独立于 `bag_api.h` 的播放运行时边界
  - 只承接纯状态迁移与纯计算，不承接 Android `AudioTrack`、UI 文案或编解码 API
  - Android 当前通过独立 playback runtime JNI bridge 消费，不和 `jni_bridge.cpp` 混职责

## `audio_core` 模块级地图

### foundation / common
- 先看：
  - `libs/audio_core/modules/bag/common/config.cppm`
  - `libs/audio_core/modules/bag/common/types.cppm`
  - `libs/audio_core/modules/bag/common/error_code.cppm`
  - `libs/audio_core/modules/bag/common/version.cppm`
  - `libs/audio_core/src/common/version.cpp`
- 兼容备注：
  - 预留接口头当前通过 `libs/audio_core/include/bag/interface/common/{config,error_code,types}.h` 取得 `C++17` 声明
  - `bag/internal/common/{config,error_code,types,version}.h` 已删除

### transport / pipeline
- 先看：
  - `libs/audio_core/modules/bag/transport/facade.cppm`
  - `libs/audio_core/src/transport/transport.cpp`
  - `libs/audio_core/modules/bag/pipeline/pipeline.cppm`
  - `libs/audio_core/src/pipeline/pipeline.cpp`
- 当前语义：
  - `transport` 负责 mode 校验、encode dispatch、decoder factory
  - `pipeline` 是内部汇聚能力，不再有主仓 fallback header

### `flash`
- 先看：
  - `libs/audio_core/modules/bag/flash/codec.cppm`
  - `libs/audio_core/src/flash/codec.cpp`
  - `libs/audio_core/modules/bag/flash/signal.cppm`
  - `libs/audio_core/src/flash/signal.cpp`
  - `libs/audio_core/modules/bag/flash/voicing.cppm`
  - `libs/audio_core/src/flash/voicing.cpp`
  - `libs/audio_core/modules/bag/flash/phy_clean.cppm`
  - `libs/audio_core/src/flash/phy_clean.cpp`
- 当前语义：
  - 原始字节直通
  - `bag.flash.signal` 负责 clean `BFSK` payload 与 payload layout
  - `bag.flash.voicing` 负责 payload voicing、固定 preamble / epilogue 与 trim descriptor
  - `bag.flash.phy_clean` 负责 text facade 与 decoder 组合
  - 无 frame / CRC / 长度字段

### `pro`
- 先看：
  - `libs/audio_core/modules/bag/pro/codec.cppm`
  - `libs/audio_core/src/pro/codec.cpp`
  - `libs/audio_core/modules/bag/pro/phy_clean.cppm`
  - `libs/audio_core/src/pro/phy_clean.cpp`
  - `libs/audio_core/modules/bag/pro/phy_compat.cppm`
  - `libs/audio_core/src/pro/phy_compat.cpp`
- 当前语义：
  - ASCII-only
  - `ASCII byte -> nibble`
  - `DTMF-like` 双音 clean PHY

### `ultra`
- 先看：
  - `libs/audio_core/modules/bag/ultra/codec.cppm`
  - `libs/audio_core/src/ultra/codec.cpp`
  - `libs/audio_core/modules/bag/ultra/phy_clean.cppm`
  - `libs/audio_core/src/ultra/phy_clean.cpp`
  - `libs/audio_core/modules/bag/ultra/phy_compat.cppm`
  - `libs/audio_core/src/ultra/phy_compat.cpp`
- 当前语义：
  - UTF-8 byte
  - `UTF-8 byte -> nibble`
  - clean `16-FSK`

### `fsk` / compat helper
- 先看：
  - `libs/audio_core/modules/bag/fsk/codec.cppm`
  - `libs/audio_core/src/fsk/fsk_codec.cpp`
  - `libs/audio_core/modules/bag/transport/compat/frame_codec.cppm`
  - `libs/audio_core/src/transport/compat/frame_codec.cpp`
- 当前语义：
  - `bag.fsk.codec` 是 host module-first helper
  - `bag.transport.compat.frame_codec` 是 modules-only internal compat capability

## 兼容/遗留文件：通常不要先看
以下内容当前不属于主链路的第一阅读入口；只有在处理预留接口、历史 carve-out 或 Android 私有包装层时再看：
- `libs/audio_core/include/bag/interface/common/**`
- `libs/audio_core/include/bag/phy/**`
- `libs/audio_core/include/bag/link/**`
- `apps/audio_android/native_package/private_include/android_bag/**`
- `apps/audio_android/native_package/src/*.cpp`
- 已删除的 `libs/audio_core/include/bag/legacy/**`
  - 当前只作为历史主题保留在 docs / policy 中，不再是可阅读入口
- 额外约束：
  - `libs/*/src` 的新实现不应回流主仓 `bag/internal/**` fallback
  - `bag/legacy/**` 已删除；legacy 路径与 include token 都不应回流
  - `verify` 的静态门禁会阻止已退休 wrapper / header 回流

## 当前不能直接走 modules 的目标
- Android native：
  - `apps/audio_android/app/src/main/cpp/CMakeLists.txt`
  - `apps/audio_android/native_package/CMakeLists.txt`
  - 使用 `CMake 4.1.2 + C++23 + bag_api.h + audio_runtime.h + package-private audio_io wrapper`
  - app `CMake` 只链接 `bag_android_native`
  - native package 只编译 `audio_core` package-owned implementation sources、`bag_api` / `audio_runtime` package-owned boundary implementation、`audio_io` package-private wrapper 与 `android_bag/**` / `android_audio_io/**` 私有声明层
- 对外 C ABI：
  - `libs/audio_api/include/bag_api.h` 不会改成 module-only 接口
- 对外文件 I/O 边界：
  - `libs/audio_io/include/wav_io.h` 继续作为稳定 header boundary
- 平台消费端：
  - `apps/audio_cli/windows/src/main.cpp`
  - `apps/audio_android/app/src/main/cpp/jni_bridge.cpp`
  - 这些文件继续只消费稳定边界，不直接 `import bag.*`

## 按任务快速跳转

### 改 mode 参数校验 / 分发
- 先看：
  - `libs/audio_core/src/transport/transport.cpp`
  - `libs/audio_api/src/bag_api.cpp`

### 改 `flash` 编解码
- 先看：
  - `libs/audio_core/src/flash/codec.cpp`
  - `libs/audio_core/src/flash/signal.cpp`
  - `libs/audio_core/src/flash/voicing.cpp`
  - `libs/audio_core/src/flash/phy_clean.cpp`
- 再看：
  - `Test/modules/leaf_module_smoke.cpp`
  - `Test/artifact/artifact_tests.cpp`

### 改 `pro` 编解码
- 先看：
  - `libs/audio_core/src/pro/codec.cpp`
  - `libs/audio_core/src/pro/phy_clean.cpp`
  - `libs/audio_core/src/transport/transport.cpp`
- 再看：
  - `Test/modules/leaf_module_smoke.cpp`
  - `libs/audio_api/tests/api_tests.cpp`
  - `Test/cli/cli_smoke_tests.cpp`

### 改 `ultra` 编解码
- 先看：
  - `libs/audio_core/src/ultra/codec.cpp`
  - `libs/audio_core/src/ultra/phy_clean.cpp`
  - `libs/audio_core/src/transport/transport.cpp`
- 再看：
  - `libs/audio_api/tests/api_tests.cpp`
  - `Test/artifact/artifact_tests.cpp`

### 改 C API / JNI / CLI 接入
- 先看：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api.cpp`
- 然后按平台看：
  - `apps/audio_cli/windows/src/main.cpp`
  - `apps/audio_android/app/src/main/cpp/jni_bridge.cpp`

### 改播放会话 runtime / seek 语义
- 先看：
  - `libs/audio_runtime/include/audio_runtime.h`
  - `libs/audio_runtime/src/audio_runtime.cpp`
  - `libs/audio_runtime/tests/runtime_tests.cpp`
- Android 集成再看：
  - `apps/audio_android/app/src/main/cpp/playback_runtime_jni.cpp`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/domain/PlaybackRuntimeGateway.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioPlaybackCoordinator.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioPlayer.kt`

### 改 Android Gradle / Studio 导入 / 构建编排
- 先看：
  - `apps/audio_android/settings.gradle.kts`
  - `apps/audio_android/build.gradle.kts`
  - `gradle.properties`
  - `tools/repo_tooling/commands/android.py`
  - `tools/repo_tooling/commands/export_apk.py`
- 再看：
  - `apps/audio_android/app/build.gradle.kts`
  - `apps/audio_android/app/src/main/cpp/CMakeLists.txt`
  - `apps/audio_android/native_package/CMakeLists.txt`
  - `docs/architecture/android-app-architecture.md`
  - `docs/architecture/android-native-strategy.md`

### 改 WAV / I/O
- 先看：
  - `libs/audio_io/modules/audio_io/wav.cppm`
  - `libs/audio_io/modules/audio_io/wav_impl.cpp`
  - `libs/audio_io/include/wav_io.h`
  - `libs/audio_io/src/wav_io.cpp`
  - `libs/audio_io/src/wav_io_backend.h`
  - `libs/audio_io/src/wav_io_backend.cpp`
- 再看：
  - `libs/audio_io/tests/unit_tests.cpp`
  - `Test/artifact/artifact_tests.cpp`

## 测试地图
- `libs/audio_io/tests/`
  - `wav_io.h` header-boundary smoke 与 bytes parse/serialize contract
- `libs/audio_api/tests/`
  - C API 契约与错误语义
- `libs/audio_runtime/tests/`
  - 播放运行时状态迁移、scrub 与时间换算
- `Test/artifact/`
  - `text -> PCM/WAV -> text` 产品主链路
- `Test/cli/`
  - 真实 CLI 子进程 smoke
- `Test/modules/`
  - `bag.*` / `audio_io.wav` 的 module-first 叶子、中层与汇聚层验证

## 当前兼容政策快照
- host 默认 modules 路径是唯一受支持的 host 主路径。
- `bag/internal/**` 主仓 direct owner 当前为 `0`。
- 预留接口头通过 `bag/interface/common/*` 保留独立 `C++17` 声明层。
- `bag/legacy/**` 已删除，且不允许通过路径或 include token 回流。
- Android 是 `bag_api.h` 的边界消费方；其剩余 native 平台例外被隔离在 `native_package` 私有包装层，并已固定到 Android `C++23` baseline。
