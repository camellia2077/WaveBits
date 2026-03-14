# Android App Architecture

更新时间：2026-03-15

## 目的
- 说明 `apps/audio_android` 的整体职责、目录边界与运行链路。
- 帮助人类和 agent 在不反复全量搜索代码的前提下，快速定位 Android 相关改动入口。
- 把 Android 应用层架构说明放在根目录 `docs/architecture/`，避免子目录文档和全仓架构文档分裂。

## 模块定位
- `apps/audio_android` 是 Android 表现层与平台集成层。
- 它负责：
  - Compose UI
  - ViewModel 与应用状态
  - Android 音频播放
  - JNI 桥接
  - 调用仓库内稳定 C API
- 它不负责：
  - 直接消费 `libs/audio_core` 内部 modules
  - 定义 core 编解码算法
  - 作为独立 `Gradle` root 构建

## 分层结构

### UI 层
- 路径：
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/component/`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/model/`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/state/`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/theme/`
- 作用：
  - 组织 Compose 页面、导航、组件、主题与 UI 状态模型
- 关键入口：
  - `AudioAndroidApp.kt`
  - `AudioAndroidViewModel.kt`

### Domain 层
- 路径：
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/domain/`
- 作用：
  - 定义 Android 应用层与 native 能力之间的稳定 Kotlin 接口
- 当前接口：
  - `AudioCodecGateway.kt`
  - `PlaybackRuntimeGateway.kt`
  - `AudioIoGateway.kt`
  - `SavedAudioRepository.kt`

### Data / Native Gateway 层
- 路径：
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/data/`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/NativeBagBridge.kt`
- 作用：
  - 用 Kotlin 封装 native bridge
  - 把 UI / ViewModel 需要的能力映射到 JNI 调用
- 当前实现：
  - `NativeAudioCodecGateway.kt`
  - `NativePlaybackRuntimeGateway.kt`
  - `NativeAudioIoGateway.kt`
  - `MediaStoreAudioExportGateway.kt`
  - `MediaStoreSavedAudioLibraryGateway.kt`
  - `DefaultSavedAudioRepository.kt`
  - `AndroidIntentAudioShareGateway.kt`
  - `NativeBagBridge.kt`
  - `NativePlaybackRuntimeBridge.kt`
  - `NativeAudioIoBridge.kt`

### JNI / Native 层
- 路径：
  - `apps/audio_android/app/src/main/cpp/`
  - `apps/audio_android/native_package/`
- 作用：
  - JNI 导出函数
  - Android native `CMake` 接线
  - Android 专用 native packaging target
  - 调用稳定 C API：`bag_api.h`
- 关键文件：
  - `jni_bridge.cpp`
  - `playback_runtime_jni.cpp`
  - `audio_io_jni.cpp`
  - `CMakeLists.txt`
  - `native_package/CMakeLists.txt`
  - `native_package/src/audio_io_package.cpp`

### 平台音频层
- 路径：
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/`
- 作用：
  - 播放编码后的 PCM 数据
- 关键文件：
  - `AudioPlayer.kt`

## 当前运行链路
1. UI 页面收集输入和模式配置。
2. `AudioAndroidViewModel` 按职责调用：
   - `AudioCodecGateway` 处理文本编解码
   - `PlaybackRuntimeGateway` 处理播放状态机与 seek 语义
   - `SavedAudioRepository` 处理导出、音频库读取、重命名、删除与分享
3. `NativeAudioCodecGateway` 通过 `NativeBagBridge` 进入 `jni_bridge.cpp -> bag_api.h`。
4. `NativePlaybackRuntimeGateway` 通过 `NativePlaybackRuntimeBridge` 进入 `playback_runtime_jni.cpp -> audio_runtime.h`。
5. `NativeAudioIoGateway` 通过 `NativeAudioIoBridge` 进入 `audio_io_jni.cpp -> native_package audio_io wrapper -> libs/audio_io`，统一复用 `wav bytes <-> pcm` 逻辑。
6. `apps/audio_android/native_package/` 提供 `bag_android_native`，在 Android 专用 packaging target 中编译 `audio_core` package-owned implementation sources、`bag_api` / `audio_runtime` package-owned boundary implementation、`audio_io` package-private wrapper 与 `android_bag/**` / `android_audio_io/**` 私有声明层。
7. 结果返回 Android 层后，可在 UI 中展示、播放、保存到 `MediaStore` 或从媒体库重新加载。

## 目录说明
- `apps/audio_android/app/`
  - Android 模块源码、资源、JNI 与模块级 `build.gradle.kts`
- `apps/audio_android/app/src/main/java/`
  - Kotlin / Java 应用代码
- `apps/audio_android/app/src/main/cpp/`
  - JNI 桥接与 Android native `CMake` 接线
- `apps/audio_android/native_package/`
  - Android native packaging target、`audio_core` package-owned implementation sources、`bag_api` / `audio_runtime` package-owned boundary implementation、`audio_io` package-private wrapper 与 `android_bag/**` / `android_audio_io/**` 私有声明层
- `apps/audio_android/app/src/main/res/`
  - Android 资源文件

## 构建边界
- Android 官方 `Gradle` root 在仓库根目录，不在 `apps/audio_android/`。
- Android Studio / IntelliJ 应直接打开仓库根目录。
- Android native 当前继续走：
  - `externalNativeBuild`
  - `CMake 4.1.2`
  - `C++23`
  - `bag_api.h`
  - `audio_runtime.h`
  - package-private `audio_io` wrapper
  - `app/src/main/cpp/CMakeLists.txt -> native_package/CMakeLists.txt -> bag_android_native`
  - `native_package/src/*.cpp -> android_bag/** / android_audio_io/**`
- 更详细的 native 策略见：
  - `docs/architecture/android-native-strategy.md`

## 修改建议

### 改 UI / 页面布局
- 先看：
  - `ui/`
  - `ui/screen/`
  - `ui/component/`

### 改状态流转 / 交互逻辑
- 先看：
  - `AudioAndroidViewModel.kt`
  - `ui/state/`
  - `ui/model/`

### 改 Kotlin 到 native 的调用方式
- 先看：
  - `domain/AudioCodecGateway.kt`
  - `domain/PlaybackRuntimeGateway.kt`
  - `domain/AudioIoGateway.kt`
  - `domain/SavedAudioRepository.kt`
  - `data/NativeAudioCodecGateway.kt`
  - `NativeBagBridge.kt`

### 改 JNI / native 接线
- 先看：
  - `app/src/main/cpp/jni_bridge.cpp`
  - `app/src/main/cpp/playback_runtime_jni.cpp`
  - `app/src/main/cpp/audio_io_jni.cpp`
  - `app/src/main/cpp/CMakeLists.txt`
  - `native_package/CMakeLists.txt`
  - `libs/audio_api/include/bag_api.h`

### 改 Android 构建 / 导出
- 先看：
  - 仓库根目录 `build.gradle.kts`
  - 仓库根目录 `settings.gradle.kts`
  - `apps/audio_android/app/build.gradle.kts`
  - `tools/wavebits_tools/commands/android.py`
  - `tools/wavebits_tools/commands/export_apk.py`

## 是否需要先重构再写文档
- 当前已经值得先写。
- 原因：
  - `apps/audio_android` 已有明确的 UI / domain / data / JNI 基本分层
  - 根 docs 现在缺少一份“Android 应用层整体地图”
  - 先补文档，可以先降低 agent 和人类接手时的搜索成本
- 当前不需要为了写这份文档，先做一次大规模职责重构。
- 更合理的顺序是：
  - 先把当前已成立的边界写清
  - 后续如果 Android 层继续细分职责，再增量更新文档
