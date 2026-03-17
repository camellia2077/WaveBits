# audio_android

`apps/audio_android` 只作为 Android 模块源码与局部规则入口。

## 索引

- Android 应用层架构：
  - `docs/architecture/android-app-architecture.md`
- Android native 策略：
  - `docs/architecture/android-native-strategy.md`
- Android 子项目规则：
  - `apps/audio_android/AGENTS.md`

## 当前目录职责

- `app/`
  - Android 模块源码、资源、JNI 与模块级 `build.gradle.kts`
- `AGENTS.md`
  - Android 子项目专属“改完要跑什么”规则

## 快速定位

- App 根装配与页面壳层：
  - `app/src/main/java/com/bag/audioandroid/ui/AudioAndroidApp.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/AudioAndroidAppDependencies.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/AudioAndroidAppShell.kt`
- `ViewModel` 与顶层 action 入口：
  - `app/src/main/java/com/bag/audioandroid/ui/AudioAndroidViewModel.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/AudioAndroidChromeActions.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/AudioAndroidSessionActions.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/AudioAndroidLibraryActions.kt`
- 编码/解码/导出会话逻辑：
  - `app/src/main/java/com/bag/audioandroid/ui/AudioSessionEncodeActions.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/AudioSessionDecodeActions.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/AudioSessionExportActions.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/AudioSessionEditingActions.kt`
- 主要页面与组件：
  - `app/src/main/java/com/bag/audioandroid/ui/screen/AudioTabScreen.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/LibraryTabScreen.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/ConfigTabScreen.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/MiniPlayerBar.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/PlayerDetailSheet.kt`
- Audio 页输入/结果卡片：
  - `app/src/main/java/com/bag/audioandroid/ui/screen/AudioInputActionsCard.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/AudioResultCard.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/FlashVoicingSelectorSection.kt`
- 播放进度与可视化：
  - `app/src/main/java/com/bag/audioandroid/ui/screen/AudioPlaybackProgressSection.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/AudioPlaybackTransportControls.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/AudioPcmWaveform.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/AudioPcmWaveformAnalysis.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/AudioFlashSignalVisualizer.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/FlashSignalVisualizationAnalysis.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/FlashSignalVisualizationDrawing.kt`
- 状态与 UI model：
  - `app/src/main/java/com/bag/audioandroid/ui/state/AudioAppUiState.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/state/ModeAudioSessionState.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/model/`
- 数据层入口：
  - `app/src/main/java/com/bag/audioandroid/data/`
  - 常用文件：
    - `NativeAudioCodecGateway.kt`
    - `NativeAudioIoGateway.kt`
    - `MediaStoreAudioExportGateway.kt`
    - `MediaStoreSavedAudioLibraryGateway.kt`
    - `AndroidSampleInputTextProvider.kt`
- JNI 与 native package：
  - `app/src/main/cpp/`
  - `app/src/main/cpp/jni_bridge.cpp`
  - `app/src/main/cpp/audio_io_jni.cpp`
  - `native_package/`
- Theme / palette：
  - `app/src/main/java/com/bag/audioandroid/ui/theme/PaletteCatalog.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/theme/PaletteFactory.kt`

## 常见改动入口

- 改编码进度、取消、结果落态：
  - 先看 `AudioSessionEncodeActions.kt`
  - 再看 `data/NativeAudioCodecGateway.kt`
  - 再看 `app/src/main/cpp/jni_bridge.cpp`
- 改导出 WAV、文件元数据、媒体库识别：
  - 先看 `MediaStoreAudioExportGateway.kt`
  - 再看 `MediaStoreSavedAudioLibraryGateway.kt`
  - 再看 `NativeAudioIoGateway.kt` / `audio_io_jni.cpp`
  - 最后看 `libs/audio_io/`
- 改播放区 UI：
  - 先看 `PlayerDetailSheet.kt`
  - 再看 `AudioPlaybackProgressSection.kt`
  - 再看 `AudioPlaybackTransportControls.kt`
- 改 flash / pro / ultra 可视化：
  - `AudioFlashSignalVisualizer.kt`
  - `FlashSignalVisualizationAnalysis.kt`
  - `FlashSignalVisualizationDrawing.kt`
- 改 Audio 页输入卡与随机样例：
  - `AudioInputActionsCard.kt`
  - `AudioSessionEditingActions.kt`
  - `SampleInputSessionUpdater.kt`
  - `AndroidSampleInputTextProvider.kt`
- 改媒体库导入/重命名/删除/分享：
  - `AudioSavedAudioMutationActions.kt`
  - `MediaStoreSavedAudioLibraryGateway.kt`
- 改底部抽屉摘要与已保存音频信息：
  - `PlayerDetailSummarySection.kt`
  - `PlayerDetailSavedInfoSection.kt`
  - `PlayerDetailDecodedSection.kt`

## 说明

- Android 官方 `Gradle` 入口固定在仓库根目录，不在 `apps/audio_android/`。
- Android Studio / IntelliJ 应直接打开仓库根目录，而不是单独打开 `apps/audio_android/`。
- Android XML 多语言当前以 `app/src/main/res/values/strings.xml` 为英文基线，中文与日语分别位于：
  - `app/src/main/res/values-zh/strings.xml`
  - `app/src/main/res/values-ja/strings.xml`
- 新增或修改可见 XML 文案时，需要同步更新以上三个目录，避免语言版本漂移。
