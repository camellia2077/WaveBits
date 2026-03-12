# 仓库结构与文件地图

更新时间：2026-03-12

## 目标
这份文档用于回答两个问题：
- 某类功能代码大概在哪些文件里？
- 修改某个模块时，应该先看哪些文件，而不是全量扫描整个目录？

## 仓库大区块
- 仓库根目录
  - 统一入口，包含 `CMakeLists.txt`、`tools/run.py`、以及 Android 官方 `Gradle` root（`settings.gradle.kts`、`gradlew*`、`gradle/`）
- `dist/`
  - Python 导出的最终交付物目录；不替代 CMake / Gradle 的原生输出目录
- `libs/`
  - 共享库代码主区，包含 `audio_core`、`audio_api`、`audio_io`
- `apps/`
  - 表现层与平台集成，目前主要是 `audio_cli`、`audio_android`
- `Test/`
  - 单元、API、产物、CLI smoke 测试
- `docs/`
  - 架构、设计、发布说明、测试说明
- `tools/`
  - Python 编排脚本，不替代 `CMake` / `Gradle`

## `libs/` 总览

### `libs/audio_core`
- 作用：核心模式逻辑、编码/解码主链路、transport 分发。
- 构建入口：
  - `libs/audio_core/CMakeLists.txt`
- 最常先看的文件：
  - `libs/audio_core/modules/`
  - `libs/audio_core/include/bag/legacy/`
  - `libs/audio_core/modules/bag/transport/facade.cppm`
  - `libs/audio_core/src/transport/transport.cpp`
  - `libs/audio_core/modules/bag/common/config.cppm`
  - `libs/audio_core/modules/bag/common/types.cppm`

### `libs/audio_api`
- 作用：稳定 C API，供 CLI、JNI 和未来其他平台层调用。
- 最常先看的文件：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api.cpp`
- 当前边界：
  - `bag_api.h` 保持 C ABI
  - `bag_api.cpp` 在 host 默认路径下消费 modules

### `libs/audio_io`
- 作用：WAV 读写与文件 I/O 边界。
- 最常先看的文件：
  - `libs/audio_io/modules/audio_io/wav.cppm`
  - `libs/audio_io/include/wav_io.h`
  - `libs/audio_io/src/wav_io.cpp`

## `audio_core` 模块级地图

host 根目录 CMake 构建默认开启 `WAVEBITS_HOST_MODULES=ON`。
`include/` 下保留下来的大部分公开头文件，现在主要承担 compatibility layer 作用：
- C++20 host 路径下，优先 `import` 对应 module
- `Phase 19` 起，`bag_api.cpp` 与 `unit_tests` 的 legacy fallback 改为优先消费 `libs/audio_core/include/bag/legacy/**`
- `audio_core` 旧的 shared bridge headers 已在 `Phase 19` 退休
- 详细分级见：
  - `docs/architecture/compatibility-layer-inventory.md`

### 公共入口与分发
- 配置与类型
  - `libs/audio_core/modules/bag/common/config.cppm`
  - `libs/audio_core/modules/bag/common/types.cppm`
  - `libs/audio_core/modules/bag/common/error_code.cppm`
  - `libs/audio_core/include/bag/legacy/common/config.h`
  - `libs/audio_core/include/bag/legacy/common/types.h`
  - `libs/audio_core/include/bag/legacy/common/error_code.h`
- 版本
  - `libs/audio_core/modules/bag/common/version.cppm`
  - `libs/audio_core/include/bag/legacy/common/version.h`
  - `libs/audio_core/src/common/version.cpp`
- transport 门面
  - `libs/audio_core/modules/bag/transport/facade.cppm`
  - `libs/audio_core/include/bag/legacy/transport/transport.h`
  - `libs/audio_core/src/transport/transport.cpp`
- pipeline 兼容适配层
  - `libs/audio_core/modules/bag/pipeline/pipeline.cppm`
  - `libs/audio_core/include/bag/legacy/pipeline/pipeline.h`
  - `libs/audio_core/src/pipeline/pipeline.cpp`

### `flash`
- 先看：
  - `libs/audio_core/modules/bag/flash/codec.cppm`
  - `libs/audio_core/src/flash/codec.cpp`
  - `libs/audio_core/modules/bag/flash/phy_clean.cppm`
  - `libs/audio_core/src/flash/phy_clean.cpp`
  - `libs/audio_core/include/bag/legacy/flash/codec.h`
  - `libs/audio_core/include/bag/legacy/flash/phy_clean.h`
- 当前语义：
  - 原始字节直通
  - clean `BFSK`
  - 无 frame / CRC / 长度字段

### `pro`
- 先看：
  - `libs/audio_core/modules/bag/pro/codec.cppm`
  - `libs/audio_core/src/pro/codec.cpp`
  - `libs/audio_core/modules/bag/pro/phy_clean.cppm`
  - `libs/audio_core/src/pro/phy_clean.cpp`
  - `libs/audio_core/include/bag/legacy/pro/codec.h`
  - `libs/audio_core/include/bag/legacy/pro/phy_clean.h`
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
  - `libs/audio_core/include/bag/legacy/ultra/codec.h`
  - `libs/audio_core/include/bag/legacy/ultra/phy_clean.h`
- 当前语义：
  - UTF-8 byte
  - `UTF-8 byte -> nibble`
  - clean `16-FSK`

### `fsk`
- 先看：
  - `libs/audio_core/modules/bag/fsk/codec.cppm`
  - `libs/audio_core/src/fsk/fsk_codec.cpp`
- 当前语义：
  - host module-first helper
  - 复用 `flash/BFSK` 的字节 <-> PCM 能力
  - `Phase 18` 已删除旧 `include/bag/fsk/fsk_codec.h` wrapper

## 兼容/遗留文件：通常不要先看
以下文件当前不属于 clean 主链路的第一阅读入口；只有在处理兼容行为、迁移遗留逻辑或专门修改旧接口时再看：
- `libs/audio_core/include/bag/legacy/pro/phy_compat.h`
- `libs/audio_core/src/pro/phy_compat.cpp`
- `libs/audio_core/include/bag/legacy/ultra/phy_compat.h`
- `libs/audio_core/src/ultra/phy_compat.cpp`
- `libs/audio_core/include/bag/phy/*`
- `libs/audio_core/include/bag/link/*`
- `libs/audio_core/src/phy/*`
- 额外约束：
  - `libs/*/src` 的新实现不应继续新增对这些 compatibility-only 头的依赖
  - `python tools/run.py verify` 已包含对应静态门禁；`Phase 18` 还会阻止已删除 wrappers 被重新引入

## 当前不能直接走 modules 的目标
- Android native：
  - 仍固定在 `apps/audio_android/app/src/main/cpp/CMakeLists.txt`
  - 使用 `CMake 3.22.1 + C++17 + bag_api.h`
  - Android 应用层分层、JNI 调用链与目录边界见：
    - `docs/architecture/android-app-architecture.md`
- 对外 C ABI：
  - `libs/audio_api/include/bag_api.h` 不能改成 module-only 接口
- 平台消费端：
  - `apps/audio_cli/windows/src/main.cpp`
  - `apps/audio_android/app/src/main/cpp/jni_bridge.cpp`
  - 这些文件应继续只依赖稳定 C ABI，而不是直接 import `libs/audio_core` 内部 modules
- Android 路线与升级条件见：
  - `docs/architecture/android-native-strategy.md`

## 按任务快速跳转

### 改 mode 参数校验 / 分发
- 先看：
  - `libs/audio_core/src/transport/transport.cpp`
  - `libs/audio_api/src/bag_api.cpp`

### 改 `flash` 编解码
- 先看：
  - `libs/audio_core/src/flash/codec.cpp`
  - `libs/audio_core/src/flash/phy_clean.cpp`
- 再看：
  - `Test/unit/unit_tests.cpp`
  - `Test/artifact/artifact_tests.cpp`

### 改 `pro` 编解码
- 先看：
  - `libs/audio_core/src/pro/codec.cpp`
  - `libs/audio_core/src/pro/phy_clean.cpp`
  - `libs/audio_core/src/transport/transport.cpp`
- 再看：
  - `Test/unit/unit_tests.cpp`
  - `Test/api/api_tests.cpp`
  - `Test/cli/cli_smoke_tests.cpp`

### 改 `ultra` 编解码
- 先看：
  - `libs/audio_core/src/ultra/codec.cpp`
  - `libs/audio_core/src/ultra/phy_clean.cpp`
  - `libs/audio_core/src/transport/transport.cpp`
- 再看：
  - `Test/unit/unit_tests.cpp`
  - `Test/api/api_tests.cpp`
  - `Test/artifact/artifact_tests.cpp`

### 改 C API / JNI / CLI 接入
- 先看：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api.cpp`
- 然后按平台看：
  - `apps/audio_cli/windows/src/main.cpp`
  - `apps/audio_android/app/src/main/cpp/jni_bridge.cpp`

### 改 Android Gradle / Studio 导入 / 构建编排
- 先看：
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `gradle.properties`
  - `tools/wavebits_tools/commands/android.py`
  - `tools/wavebits_tools/commands/export_apk.py`
- 再看：
  - `apps/audio_android/app/build.gradle.kts`
  - `apps/audio_android/AGENTS.md`
  - `docs/architecture/android-app-architecture.md`
  - `docs/architecture/android-native-strategy.md`

### Android 目录定位
- `apps/audio_android/app/`
  - Android 模块源码、JNI、资源和模块级 `build.gradle.kts`
- 仓库根目录
  - Android 官方 `Gradle` root 与 wrapper
- `dist/android/`
  - Python 从 Gradle 输出复制出的最终 APK 交付物
- 约定：
  - Android Studio 直接打开仓库根目录，不打开 `apps/audio_android`
- Android 应用层结构与调用链见：
  - `docs/architecture/android-app-architecture.md`
- Android native 默认路线与升级条件见：
  - `docs/architecture/android-native-strategy.md`

### 改 WAV / I/O
- 先看：
  - `libs/audio_io/include/wav_io.h`
  - `libs/audio_io/src/wav_io.cpp`
- 再看：
  - `Test/unit/unit_tests.cpp`
  - `Test/artifact/artifact_tests.cpp`

## 测试地图
- `Test/unit/`
  - 核心 codec / PHY / transport / pipeline 最小行为
- `Test/api/`
  - C API 契约与错误语义
- `Test/artifact/`
  - `text -> PCM/WAV -> text` 产品主链路
- `Test/cli/`
  - 真实 CLI 子进程 smoke
- 详细测试口径见：
  - `docs/testing.md`
- 详细 module 拓扑见：
  - `docs/architecture/module-topology.md`

