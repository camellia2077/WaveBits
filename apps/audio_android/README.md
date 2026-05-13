# audio_android

`apps/audio_android` 是 Android 工程的 `Gradle` root，同时承载 Android 模块源码、资源、JNI 与局部规则入口。

## 文档导航

- Android 应用层架构：
  - `docs/architecture/android/android-app-architecture.md`
- Android agent read-order：
  - `docs/design/android/android-agent-read-order.md`
- Android UI 结构与职责：
  - `docs/architecture/android/android-ui-structure.md`
- Android native 策略：
  - `docs/architecture/android/android-native-strategy.md`
- Android 基础设施升级清单：
  - `docs/notes/android/android-infra-upgrade-checklist.md`
- Android 自动化覆盖总览：
  - `docs/architecture/android/android-automation-coverage.md`
- Android 自动化 agent 索引：
  - `docs/architecture/android/android-automation-agent-index.md`
- Android 双色主题规则：
  - `docs/design/android/android-dual-tone-theme.md`
- Android 播放器 UI：
  - `docs/design/android/android-player-ui.md`
- Android Flash Visual 数据流与性能诊断：
  - `docs/architecture/android/android-flash-visual.md`
- Android 本地化规则：
  - `docs/design/android/android-localization-guidelines.md`
  - `docs/design/android/android-translation-workflow.md`
  - `docs/design/android/android-translation-tooling-agent-index.md`
  - `docs/design/android/translation/android-split-strings-translation-guide.md`
- Android 子项目规则：
  - `apps/audio_android/AGENTS.md`

## 当前目录职责

- `app/`
  - Android 模块源码、资源、JNI 与模块级 `build.gradle.kts`
- `AGENTS.md`
  - Android 子项目专属薄索引与硬约束
- `README.md`
  - 人类入口与快速定位

## 文案资源布局

- 英文基线资源现在按职责拆在 `app/src/main/res/values/` 下：
  - `strings_common.xml`
  - `strings_audio.xml`
  - `strings_saved.xml`
  - `strings_settings.xml`
  - `strings_about.xml`
  - `strings_validation.xml`
- 其他语言当前仍主要保留在各自目录下的单文件 `strings.xml`，例如：
  - `app/src/main/res/values-zh/strings_audio.xml`
  - `app/src/main/res/values-zh-rTW/strings_audio.xml`
  - `app/src/main/res/values-ja/strings_audio.xml`
  - `app/src/main/res/values-fr/strings_audio.xml`
- 修改用户可见文案时，先按职责定位英文基线文件，再同步检查对应语言目录下的 `strings*.xml`。
- `audio_samples_*` 一类示例文本资源仍保持独立 XML，不在上述 `strings_*.xml` 中。

## 使用方式

- 想快速找入口：先看本文件的“代码地图 / 常见起点”
- 想先判断这次改动还要不要展开别的 Android 文档：去 `docs/design/android/android-agent-read-order.md`
- 想看 UI 设计原则：去 `docs/design/android/`
- 想看 UI 职责和代码归属：去 `docs/architecture/android/android-ui-structure.md`
- 想看 agent 必须遵守的硬规则：去 `apps/audio_android/AGENTS.md`
- 想按需展开 Android 自动化文档：先去 `docs/architecture/android/android-automation-agent-index.md`

## 代码地图

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
  - `app/src/main/java/com/bag/audioandroid/ui/screen/FlashVisualPerfTrace.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/FlashVisualWindowActions.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/state/FlashVisualWindowState.kt`
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
  - `app/src/main/java/com/bag/audioandroid/ui/theme/BrandThemeCatalog.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/theme/AppThemeAccentTokens.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/theme/AppThemeVisualTokens.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/theme/AudioEncodeGlyphColors.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/AudioAndroidThemeMappings.kt`

## 常见起点

- 编码进度、取消、结果落态：
  - `AudioSessionEncodeActions.kt` -> `data/NativeAudioCodecGateway.kt` -> `app/src/main/cpp/jni_bridge.cpp`
  - adb 采集与进度条 phase 诊断看 `docs/architecture/android/android-encode-progress-automation.md`
- 导出 WAV、文件元数据、媒体库识别：
  - `MediaStoreAudioExportGateway.kt` -> `MediaStoreSavedAudioLibraryGateway.kt` -> `NativeAudioIoGateway.kt` / `audio_io_jni.cpp` -> `libs/audio_io/`
- 播放区 UI：
  - `PlayerDetailSheet.kt` -> `AudioPlaybackProgressSection.kt` -> `AudioPlaybackTransportControls.kt`
- mini / flash / pro / ultra 可视化：
  - `AudioFlashSignalVisualizer.kt` -> `FlashSignalVisualizationAnalysis.kt` -> `FlashSignalVisualizationDrawing.kt`
  - `MorseTimelineVisualizer.kt` 负责 `mini` 的 dot/dash block timeline；silence gap 保持空白，不画成 tone。
- Flash Visual 卡顿、跳动、长音频性能或 bit 密度问题：
  - 先读 `docs/architecture/android/android-flash-visual.md`。
  - 先用 debug 指标和 `adb logcat FlashVisualPerf:D *:S` 判断是 draw、Compose、window、raw playback position、smoother reset 还是视觉密度问题，再决定改哪个层级。
- 主题、palette、glyph：
  - `BrandThemeCatalog.kt` -> `AppThemeAccentTokens.kt` -> `AppThemeVisualTokens.kt` -> `AudioAndroidThemeMappings.kt` -> `AudioEncodeGlyphColors.kt`
- Audio 页输入卡、随机样例、示例文本：
  - `AudioInputActionsCard.kt` -> `AudioSessionEditingActions.kt` -> `SampleInputSessionUpdater.kt` -> `AndroidSampleInputTextProvider.kt`
  - `mini` 输入辅助由 `ui/model/MorseCode.kt` 提供 normalization、unsupported character 提示、dot/dash 预览与 `Slow / Standard / Fast` speed preset。
- XML 文案与本地化资源：
  - `app/src/main/res/values/strings_*.xml` -> `app/src/main/res/values-*/strings*.xml` -> `app/src/main/res/values*/audio_samples_*.xml`
- 媒体库导入、重命名、删除、分享：
  - `AudioSavedAudioMutationActions.kt` -> `MediaStoreSavedAudioLibraryGateway.kt`
- 底部抽屉摘要与已保存音频信息：
  - `PlayerDetailSummarySection.kt` -> `PlayerDetailSavedInfoSection.kt`

## 说明

- Android 官方 `Gradle` 入口固定在 `apps/audio_android/`。
- Android Studio / IntelliJ 应直接打开 `apps/audio_android/`。
- 当前应用有两套主题实现：
  - `Material`
    - 继续走单色 `ColorScheme` 语义。
  - `dual-tone`
    - 从 `BrandThemeCatalog.kt` 的 `backgroundColor` / `accentColor` / `outlineColor` 出发。
    - 通过 `AppThemeAccentTokens.kt`、`AppThemeVisualTokens.kt` 和 `AudioAndroidThemeMappings.kt` 显式映射到 UI。
    - `Material` 组件继续提供结构、状态与可读性基础，但不再负责决定 dual-tone 的最终视觉语义。
- Android XML 多语言当前以 `app/src/main/res/values/strings*.xml` 为英文基线。
- 新增或修改可见 XML 文案时，需要同步更新对应语言目录下的 `strings*.xml`，避免语言版本漂移。
- UI 设计细则不要继续堆在 `README.md` 或 `AGENTS.md` 里；优先写进 `docs/design/android/`，再由这里做导航。
- 代码职责、颜色入口、共享 helper 等结构性说明，优先写进 `docs/architecture/`，再由这里做导航。
- 当前底部 tab 用户文案以 `Audio / Saved / Settings` 为准；代码入口文件仍沿用历史命名，如 `LibraryTabScreen.kt`、`ConfigTabScreen.kt`。

## Build Variants

- `python tools/run.py android assemble-debug`
  - 日常开发与基础功能验证入口。
- `python tools/run.py android assemble-staging`
  - 面向 release-only 问题排查的本地验证入口。
  - `staging` 继承 release 的 `minify + shrink` 行为，但保持 `debuggable = true` 并使用 debug 签名，更适合真机安装、连接 Android Studio 和抓日志。
  - 优先用于排查：
    - JNI / R8 / `@Keep`
    - 资源收缩误删
    - 只在 release-like 构建里出现的闪退
- `python tools/run.py android assemble-release`
  - 正式签名与发布产物入口。

## Release 签名与可更新安装

- Android 能否“直接覆盖安装更新”，关键不在代码逻辑，而在于：
  - 新 APK 与已安装 APK 使用同一个签名证书
  - 新 APK 的 `versionCode` 更大
- 本项目的 release 构建会在检测到签名配置时自动启用正式签名；未配置时仍可构建 debug，但 release 不会带上你的正式签名。
- 推荐把真实签名材料保存在仓库外，并通过下面任一方式提供给 `app/build.gradle.kts`：
  - 在 `apps/audio_android/app/release-signing.properties` 填写配置
  - 或传入 `-Pflipbits.android.releaseSigningPropertiesFile=<properties 文件绝对路径>`
  - 或直接传入 Gradle 属性 / 环境变量：
    - `flipbits.android.releaseSigning.storeFile`
    - `flipbits.android.releaseSigning.storePassword`
    - `flipbits.android.releaseSigning.keyAlias`
    - `flipbits.android.releaseSigning.keyPassword`
- 可参考模板文件：
  - `apps/audio_android/app/release-signing.properties.example`
- 常见流程：
  - 1. 生成或拿到固定的 release keystore
  - 2. 填写签名配置
  - 3. 每次发新版前递增 `apps/audio_android/gradle.properties` 里的 `flipbits.android.versionCode`
  - 4. 执行 `python tools/run.py android assemble-release`
- 一旦首个对外安装包是用你的正式 release key 签出来的，之后只要保持同一把 key，并持续递增 `versionCode`，安装新 APK 时就不需要先卸载旧版。
