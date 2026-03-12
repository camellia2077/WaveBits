# WaveBits Host Module Topology

更新时间：2026-03-12

## 目标
- 说明 host 默认 modules 路径下的层级拓扑。
- 标明哪些头文件只是 compatibility layer。
- 标明当前哪些目标仍然不能直接依赖内部 modules。

## 默认规则
- host 根目录 CMake 构建默认开启 `WAVEBITS_HOST_MODULES=ON`
- `libs/` 内部新增实现默认应优先 `import` 已有 modules
- 不再新增横向公开 `#include` 依赖来替代已存在的 module 边界
- 如需排障或兼容回退，可显式使用 `--no-modules`

## 模块分层

### Layer 1：基础数据与错误模型
- `bag.common.config`
- `bag.common.error_code`
- `bag.common.types`
- `bag.common.version`

### Layer 2：叶子能力
- `audio_io.wav`
- `bag.flash.codec`
- `bag.pro.codec`
- `bag.ultra.codec`
- `bag.transport.compat.frame_codec`

### Layer 3：中层能力
- `bag.transport.decoder`
- `bag.flash.phy_clean`
- `bag.fsk.codec`
- `bag.pro.phy_clean`
- `bag.pro.phy_compat`
- `bag.ultra.phy_clean`
- `bag.ultra.phy_compat`

### Layer 4：汇聚层
- `bag.transport.facade`
- `bag.pipeline`

### Layer 5：C ABI 消费边界
- `libs/audio_api/src/bag_api.cpp`
  - host modules 路径下改为 `import`
- `libs/audio_api/include/bag_api.h`
  - 保持稳定 C ABI，不改为 module-only

## Compatibility Layer
- `libs/audio_core/include/bag/legacy/**/*.h`
- `libs/audio_core/include/bag/link/*.h`
- `libs/audio_core/include/bag/phy/**/*.h`
- `libs/audio_io/include/wav_io.h`

这些头文件当前的职责是：
- `audio_core` 主线 bridge 语义已经在 `Phase 19` 结束
- `Phase 19` 起，legacy header fallback 与 Android native 需要的声明，优先通过 `bag/legacy/**` 提供
- `link/*` 与 `phy/*` 继续只是预留接口层，不属于当前主线 bridge surface

它们不是优先新增能力的主入口；新增内部实现应优先落在：
- `libs/audio_core/modules/`
- `libs/audio_io/modules/`
- 或对应 `.cpp` named module implementation unit

更细的分类与消费端边界说明见：
- `docs/architecture/compatibility-layer-inventory.md`
- `docs/architecture/android-native-strategy.md`

## 当前不能直接走 modules 的目标
- Android native `externalNativeBuild`
  - 入口：`apps/audio_android/app/src/main/cpp/CMakeLists.txt`
  - 约束：`CMake 3.22.1 + C++17`
- `bag_api.h`
  - 原因：它是稳定 C ABI，不适合改成 module-only 对外接口
- CLI / JNI 消费端
  - `apps/audio_cli/windows/src/main.cpp`
  - `apps/audio_android/app/src/main/cpp/jni_bridge.cpp`
  - 这些入口应继续消费 `bag_api.h`，不要直接 import `audio_core` 内部 modules

## 推荐验证命令
- 默认 host modules 路径：
  - `python tools/run.py verify --build-dir build/dev --skip-android`
- legacy header fallback：
  - `python tools/run.py verify --build-dir build/legacy-host --skip-android --no-modules`
- Android Gradle 配置检查：
  - `./gradlew :app:help`
