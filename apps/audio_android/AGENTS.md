# apps/audio_android Agent Rules

## Terminal

- 在这个仓库执行终端命令时，统一使用 `pwsh` 作为 shell 入口。
- 在这个仓库执行终端命令、编译或验证时，默认不要使用沙盒环境；尤其是 `CMake` / `Ninja` / `Gradle` / `python tools/run.py verify` 相关命令，必须在非沙盒环境执行。

## First Read

- `apps/audio_android` 是 Android 官方 `Gradle` root。
- 先读 `apps/audio_android/README.md` 的“快速定位 / 常见改动入口”。
- 如果改动涉及 UI / 配色 / 播放器结构，再按需读：
  - `docs/design/android/android-player-ui.md`
  - `docs/design/android/android-dual-tone-theme.md`
- 如果改动涉及 UI 职责拆分或入口归属，再按需读：
  - `docs/architecture/android-ui-structure.md`

## Hard Rules

- 优先按职责找入口，不要默认从最大文件开始搜。
- 修改可见 XML 文案时，必须同步检查：
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-zh/strings.xml`
  - `app/src/main/res/values-ja/strings.xml`
  - `app/src/main/res/values-zh-rTW/strings.xml`
  - `app/src/main/res/values-fr/strings.xml`
- 新增 XML 文案 key 时，不允许只落在单一语言目录。
- 改动语言切换、随机样例或默认文案时，还要检查：
  - `data/AndroidSampleInputTextProvider.kt`
  - `ui/SampleInputSessionUpdater.kt`
- For dual-tone lineup/theme color/sample changes, also check:
  - `docs/design/android/android-dual-tone-theme.md`
  - `app/src/main/java/com/bag/audioandroid/ui/theme/BrandThemeCatalog.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/theme/AudioEncodeGlyphColors.kt`
  - `app/src/main/java/com/bag/audioandroid/ui/screen/ConfigThemeAppearanceSection.kt`
  - `data/AndroidSampleInputTextProvider.kt` and matching `audio_samples_*` resources
- 如果改动涉及保存音频识别，不要再从文件名设计新解析逻辑；优先看 WAV metadata 链路。
- 如果要修改 Android presentation 版本号，优先改 `apps/audio_android/gradle.properties`。

## Player UI Rules

- Dock 系统颜色必须统一走 `playerDockContainerColor(uiState)`。
- 播放器 segmented button 必须统一走 `playerSegmentedButtonColors()`。
- 播放器 transport / chip 等子控件必须优先复用 `playerChromeColors()`。
- 不要在单个播放器组件里临时手写新的主题色分支，除非同步更新共享 helper。
- dual-tone 颜色职责必须从 `BrandThemeCatalog.kt` 的 `backgroundColor` / `accentColor` / `outlineColor` 出发，并通过共享 token/helper 进入 UI；不要在具体组件里按主题 id 硬编码颜色。

## Build And Validation

- 编译与测试优先从仓库根目录通过 `python tools/run.py android ...` 执行。
- 修改 Android Kotlin 源码后，最小验证优先运行：
  - `python tools/run.py android assemble-debug`
- 涉及 JNI / `proguard-rules.pro` / `@Keep` / 反射 / `FindClass` / `GetMethodID` /
  `NewObject` / 资源收缩 / release-only 崩溃时，不要只验证 debug；默认还要运行：
  - `python tools/run.py android assemble-staging`
- 新增会被 native 通过类名、字段名或构造器签名直接访问的 Kotlin/Java DTO 时：
  - 优先补 `@Keep`
  - 同步检查 `app/proguard-rules.pro`
- 修改 `Gradle` / `CMake` / JNI / 依赖接线后，建议运行：
  - `python tools/run.py android assemble-debug --clean`
- 需要自动格式化时，优先运行：
  - `python tools/run.py android ktlint-format`
- 需要质量检查时，优先运行：
  - `python tools/run.py android ktlint-check`
  - `python tools/run.py android detekt`
  - `python tools/run.py android quality`
