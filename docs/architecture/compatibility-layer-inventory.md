# Compatibility Layer Inventory

更新时间：2026-03-12

## 目标
- 盘点当前仍保留在 `include/` 下的公开头文件。
- 说明它们为什么还存在，以及哪些属于长期边界，哪些只是兼容层。
- 避免在后续 modules 收口时误删稳定接口。

## 长期边界头

### `libs/audio_api/include/bag_api.h`
- 类型：长期保留
- 作用：稳定 C ABI
- 允许消费端：
  - CLI
  - Android JNI
  - 未来其他平台层 / 外部语言绑定
- 说明：
  - 这是当前最稳定的跨边界入口
  - 不应改成 module-only 接口

### `libs/audio_io/include/wav_io.h`
- 类型：长期保留
- 作用：WAV / 文件 I/O 边界
- 允许消费端：
  - CLI
  - 测试
  - 其他 header-based 文件 I/O 消费端
- 说明：
  - `audio_io.wav` 已存在，但当前 GCC/libstdc++ modules-ts 路径下，不适合强推所有消费端改为 `import audio_io.wav`
  - 因此 `wav_io.h` 继续作为稳定 header boundary 保留

## Phase 19 结论
- `audio_core` 面向 host / Android / no-modules 的 shared bridge headers 已完成退休。
- host 默认路径现在直接消费 modules。
- Android / no-modules / legacy fallback 需要的声明，统一通过 `bag/legacy/**` 暴露。
- 当前留在 `include/bag/` 主树下的头，已经不再承担 `audio_core` 主线 bridge 语义。

## Phase 19 legacy carve-out surface
- `libs/audio_core/include/bag/legacy/common/config.h`
- `libs/audio_core/include/bag/legacy/common/error_code.h`
- `libs/audio_core/include/bag/legacy/common/types.h`
- `libs/audio_core/include/bag/legacy/common/version.h`
- `libs/audio_core/include/bag/legacy/flash/codec.h`
- `libs/audio_core/include/bag/legacy/flash/phy_clean.h`
- `libs/audio_core/include/bag/legacy/pipeline/pipeline.h`
- `libs/audio_core/include/bag/legacy/pro/codec.h`
- `libs/audio_core/include/bag/legacy/pro/phy_compat.h`
- `libs/audio_core/include/bag/legacy/pro/phy_clean.h`
- `libs/audio_core/include/bag/legacy/transport/decoder.h`
- `libs/audio_core/include/bag/legacy/transport/transport.h`
- `libs/audio_core/include/bag/legacy/transport/compat/frame_codec.h`
- `libs/audio_core/include/bag/legacy/ultra/codec.h`
- `libs/audio_core/include/bag/legacy/ultra/phy_compat.h`
- `libs/audio_core/include/bag/legacy/ultra/phy_clean.h`
- 说明：
  - 这是 `Phase 19` 新增并扩面的显式 declaration surface
  - 目标是把 Android / `--no-modules` 需要保留的声明，从共享 host bridge 语义里切出来
  - 当前允许直接 include 这层 surface 的路径收敛为：
    - `libs/audio_api/src/bag_api.cpp`
    - `Test/unit/unit_tests.cpp`
    - `libs/audio_core/src/*.cpp` 的 no-modules fallback

## 已在 Phase 18 退休的 compatibility-only wrappers
- `libs/audio_core/include/bag/pro/text_codec.h`
- `libs/audio_core/src/pro/text_codec.cpp`
- `libs/audio_core/include/bag/pro/frame_codec.h`
- `libs/audio_core/src/pro/frame_codec.cpp`
- `libs/audio_core/include/bag/fsk/fsk_codec.h`
- 说明：
  - 这批文件只承担历史命名包装或 host 过渡桥接，不再属于当前 inventory
  - `unit_tests` 已改为直接覆盖 `bag.flash.phy_clean`、`bag.pro.codec`、`bag.transport.compat.frame_codec`
  - `src/fsk/fsk_codec.cpp` 仍保留为 `bag.fsk.codec` 的 module implementation，但不再通过 header wrapper 暴露

## 已在 Phase 19 退休的 shared bridge headers
- `libs/audio_core/include/bag/common/config.h`
- `libs/audio_core/include/bag/common/error_code.h`
- `libs/audio_core/include/bag/common/types.h`
- `libs/audio_core/include/bag/common/version.h`
- `libs/audio_core/include/bag/flash/codec.h`
- `libs/audio_core/include/bag/flash/phy_clean.h`
- `libs/audio_core/include/bag/pipeline/pipeline.h`
- `libs/audio_core/include/bag/pro/codec.h`
- `libs/audio_core/include/bag/pro/phy_compat.h`
- `libs/audio_core/include/bag/pro/phy_clean.h`
- `libs/audio_core/include/bag/transport/decoder.h`
- `libs/audio_core/include/bag/transport/compat/frame_codec.h`
- `libs/audio_core/include/bag/transport/transport.h`
- `libs/audio_core/include/bag/ultra/codec.h`
- `libs/audio_core/include/bag/ultra/phy_compat.h`
- `libs/audio_core/include/bag/ultra/phy_clean.h`
- 说明：
  - 这批头在 `Phase 19` 中已完成 owner 切分、consumer evacuation 与最终退休
  - host 默认路径继续直接 `import bag.*`
  - no-modules / Android 相关声明入口已迁到 `bag/legacy/**`

## 预留 / 接口层头
- `libs/audio_core/include/bag/link/link_layer.h`
- `libs/audio_core/include/bag/phy/fun/fun_phy.h`
- `libs/audio_core/include/bag/phy/pro/pro_phy.h`
- 说明：
  - 这些头代表接口层或预留概念，不是当前产品主链路
  - 不建议把它们和主线 compatibility header 混为一谈

## 当前消费端边界快照

### CLI
- 文件：
  - `apps/audio_cli/windows/src/main.cpp`
  - `apps/audio_cli/windows/cmake/CMakeLists.txt`
- 当前 allowed surface：
  - `bag_api.h`
  - `wav_io.h`
- 当前不允许：
  - 直接 `#include "bag/..."`
  - 直接 `import bag.*`

### Android JNI
- 文件：
  - `apps/audio_android/app/src/main/cpp/jni_bridge.cpp`
  - `apps/audio_android/app/src/main/cpp/CMakeLists.txt`
- 当前 allowed surface：
  - `bag_api.h`
- 当前不允许：
  - 直接 `#include "bag/..."`
  - 直接依赖 `audio_core/include`
  - 直接依赖 `audio_io/include`
  - 直接 `import bag.*`

### Host tests
- 文件：
  - `Test/**`
- 说明：
  - 测试属于内部验证面，不纳入 consumer-boundary 限制
  - `Phase 19` 起，legacy branch 优先通过 `bag/legacy/**` 覆盖 Android / no-modules carve-out
  - modules 路径仍可继续直接 `import bag.*`

## 收口原则
- 不要把“compatibility layer inventory”理解成“立刻删头文件”
- 长期边界头优先保证稳定，不追求 module-only
- `Phase 19` 起，Android / no-modules 需要的声明入口优先通过 `bag/legacy/**` 显式表达
- `audio_core` 的 shared bridge headers 已完成收口；下一阶段不应再把它们带回主线
