# Compatibility Layer Inventory

更新时间：2026-03-13

## 目标
- 盘点当前仍保留在 `include/` 下的公开头文件。
- 说明它们为什么还存在，以及哪些属于长期边界，哪些只是兼容层。
- 避免误删稳定接口，并把长期边界与兼容层清楚分开。

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
  - 当前 shared-header implementation chain 已退休；真正的 `sndfile` / 文件系统 owner 收回到 `libs/audio_io/src/wav_io_backend.cpp`
  - `wav_io.h` 现在只保留稳定声明，不再承担 module/header 双入口共享实现

## 当前 `audio_core` 兼容层结论
- `audio_core` 面向 host / Android / no-modules 的 shared bridge headers 已完成退休。
- host 默认路径现在直接消费 modules。
- root host `WAVEBITS_HOST_MODULES=OFF` 已退休，主仓剩余实现链现在按 modules-only 收口。
- Android 当前通过 `apps/audio_android/native_package/ -> bag_android_native` 独立消费 package-private `C++17` 包装层。
- Android gate 与 host modules 主线已经彻底拆开，不再共享同一个 fallback 口径。
- 主仓里的 `bag/internal/**` owner 已归零。
- 预留接口头当前通过 `bag/interface/common/{config,error_code,types}.h` 维持独立 `C++17` 声明层，并按长期保留的 reserved-interface boundary 管理。
- 当前留在 `include/bag/` 主树下的头，已经不再承担 `audio_core` 主线 bridge 语义。

## 当前 post-legacy 状态
- `bag/legacy/**` 已从 `libs/audio_core/include/` 删除，不再属于当前 compatibility surface。
- 当前批准的 `bag/legacy/**` direct include owner 集合保持为空。
- Android package-private `C++17` native exception 继续作为 Android 独立平台偏差跟踪，但不再与已删除的 legacy surface 绑定。
- `retirement_policy.py` 当前同时阻止两类回流：
  - deleted legacy header path 回流
  - direct `#include "bag/legacy/..."` token 回流

## 已删除的 legacy surface
- `libs/audio_core/include/bag/legacy/**`
- 说明：
  - 原 `16` 个 residual headers 已全部删除
  - 当前仓库中不再保留 legacy declaration surface
  - 如 legacy 路径或 include token 回流，由 `retirement_policy.py` 阻止

## 长期保留的 reserved interface declaration layer
- `libs/audio_core/include/bag/interface/common/config.h`
- `libs/audio_core/include/bag/interface/common/error_code.h`
- `libs/audio_core/include/bag/interface/common/types.h`
- 说明：
  - 这不是对外 consumer boundary，也不是 shared bridge headers 或 `bag/internal/**` 的回流
  - 这层是 intentional、长期保留的仓库内部声明边界，只承接预留接口头仍需保留的 `common/*` 声明
  - 主仓 `bag/internal/**` owner 已为 `0`
  - `bag.common.version`、`bag.transport.facade`、`flash/pro/ultra` clean/codec、`bag.pipeline`、`bag.pro.phy_compat`、`bag.ultra.phy_compat` 与 `bag.transport.compat.frame_codec` 已切到 modules-only host 主线，不再通过主仓 `bag/internal/**` 暴露 fallback declarations
  - `compatibility_policy.py` 当前锁定它的存在性、allowed-owner 集合、include-based 形态，以及不新增 `bag.interface.*` module mirror
  - 当前直接使用它的路径包括：
    - `libs/audio_core/include/bag/link/link_layer.h`
    - `libs/audio_core/include/bag/phy/fun/fun_phy.h`
    - `libs/audio_core/include/bag/phy/pro/pro_phy.h`

## 已退休的 compatibility-only wrappers
- `libs/audio_core/include/bag/pro/text_codec.h`
- `libs/audio_core/src/pro/text_codec.cpp`
- `libs/audio_core/include/bag/pro/frame_codec.h`
- `libs/audio_core/src/pro/frame_codec.cpp`
- `libs/audio_core/include/bag/fsk/fsk_codec.h`
- 说明：
  - 这批文件只承担历史命名包装或 host 过渡桥接，不再属于当前 inventory
  - `modules_phase2_leaf_smoke` 已直接覆盖 `bag.flash.phy_clean`、`bag.pro.codec` 与 `bag.transport.compat.frame_codec`
  - `src/fsk/fsk_codec.cpp` 仍保留为 `bag.fsk.codec` 的 module implementation，但不再通过 header wrapper 暴露

## 已退休的 shared bridge headers
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
  - 这批头已完成 owner 切分、consumer evacuation 与最终退休
  - host 默认路径继续直接 `import bag.*`
  - 主仓实现链已改为 modules-only；Android 相关声明入口改由 `apps/audio_android/native_package/private_include/android_bag/**` 私有层承接

## 预留 / 接口层头
- `libs/audio_core/include/bag/link/link_layer.h`
- `libs/audio_core/include/bag/phy/fun/fun_phy.h`
- `libs/audio_core/include/bag/phy/pro/pro_phy.h`
- 说明：
  - 这些头代表接口层或预留概念，不是当前产品主链路
  - 其 `C++17` fallback 已切到 `bag/interface/common/*`
  - `bag/interface/common/*` 是这组接口头的长期保留声明边界，不是下一笔 retirement 或 `import std;` 扩面目标
  - 不建议把它们和已删除 legacy surface 或主线 compatibility header 混为一谈

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
  - `apps/audio_android/native_package/CMakeLists.txt`
- 当前 allowed surface：
  - `bag_api.h`
- 当前不允许：
  - 直接 `#include "bag/..."`
  - 直接 `#include "bag/legacy/..."`
  - 直接依赖 `audio_core/include`
  - 直接依赖 `audio_io/include`
  - 直接 `import bag.*`
- 说明：
  - Android 当前是 `bag_api.h` 的边界消费方，不是 `bag/legacy/**` 的直接 owner
  - Android app `CMake` 当前只消费 `native_package -> bag_android_native`
  - Android native package 现在只编译 package-private wrapper 与 `android_bag/**` 私有声明层，不再直接 source-own 主仓原始实现文件

### Host tests
- 文件：
  - `Test/**`
- 说明：
  - 测试属于内部验证面，不纳入 consumer-boundary 限制
  - `unit_tests.cpp` 当前只保留 `wav_io.h` header-boundary smoke，不再直接持有 `bag/internal/**` owner
  - `modules_phase2_leaf_smoke` 继续承担 `bag.flash.phy_clean`、`bag.pro.codec` 与 `bag.transport.compat.frame_codec` 的低层 module-first 覆盖
  - modules 路径仍可继续直接 `import bag.*`
  - 当前测试不再有批准的 `bag/legacy/**` 直接 owner；其余测试同样不应新增 legacy include

## 当前 legacy 退休结论
- `bag/legacy/**` 的删除前置条件与最终删除执行均已完成。
- 当前不再有这条主题下的 remaining work；若后续出现 legacy path 或 include token，即视为 regression。
- `bag/interface/common/*` 已定稿为长期保留的 reserved-interface declaration boundary，不再是 open question。

## 收口原则
- 不要把“compatibility layer inventory”理解成“立刻删头文件”
- 长期边界头优先保证稳定，不追求 module-only
- Android 当前仍是平台例外，但它的 `C++17` 包装层已被限制在 `native_package` 私有目录下
- 主仓 `bag/internal/**` owner 已为 `0`；预留接口头通过 `bag/interface/common/*` 显式表达其非-modules 声明责任
- `bag/interface/common/*` 保持 include-based header boundary，不进入 host-only `import std;` audited expansion
- `bag/legacy/**` 保持已删除状态，既不属于现役 surface，也不应再回流任何 owner、路径或 include token
- `audio_core` 的 shared bridge headers 已完成收口；不应再把它们带回主线
