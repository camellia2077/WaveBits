# Android 基础设施升级清单

更新时间：2026-05-11

## 文档定位

- 这是一份给 `apps/audio_android` 的基础设施升级执行清单。
- 本清单的目标不是“最小改动保守升级”，而是：
  - 尽可能对齐 Android 当前主线基础设施；
  - 主动清理历史构建胶水；
  - 为后续业务迭代减少工具链阻塞。
- 本清单明确不把“向后兼容旧版本组合”当成本轮约束。

## 本轮原则

1. 优先最新稳定版，不为保留旧 pin 停留在历史组合。
2. 不做半升级状态；一旦启动，就把同一层的相关 pin 一次对齐。
3. 允许为新工具链改代码、改构建脚本、改文档、改 CI。
4. 第三方插件如果挡升级，优先换接线方式，不优先保留旧插件用法。
5. 升级目标是“仓库显式 pin 可复现”，不是“本机环境碰巧能过”。

## 当前仓库基线

- `AGP`: `9.0.1`
- `Gradle Wrapper`: `9.2.1`
- `compileSdk`: `35`
- `targetSdk`: `34`
- `AboutLibraries plugin`: `12.2.4`
- `ktlint Gradle plugin`: `14.1.0`
- `detekt`: `1.23.8`，当前已改为 CLI 任务接线
- `NDK`: `28.2.13676358`
- `CMake`: `4.1.2`

对应入口：

- [apps/audio_android/build.gradle.kts](/C:/code/WaveBits/apps/audio_android/build.gradle.kts)
- [apps/audio_android/app/build.gradle.kts](/C:/code/WaveBits/apps/audio_android/app/build.gradle.kts)
- [apps/audio_android/gradle/wrapper/gradle-wrapper.properties](/C:/code/WaveBits/apps/audio_android/gradle/wrapper/gradle-wrapper.properties)

## 目标口径

- Android 构建链路对齐到“执行当日最新稳定组合”。
- `compileSdk` 和主依赖不再被旧 API level 卡住。
- Kotlin/Compose/AndroidX/Gradle plugin 版本不再跨代错位。
- AboutLibraries、detekt、ktlint 等构建插件不再依赖已知过时接线。
- Android 文档、命令说明、CI 入口与新基线一致。

## 执行顺序

### 1. 固定目标版本

- 确定并记录以下“执行当日最新稳定版”：
  - `AGP`
  - `Gradle`
  - `JDK`
  - `compileSdk`
  - `targetSdk`
  - `Build Tools`
  - `Kotlin`
  - `Compose Compiler plugin`
  - `AndroidX Compose BOM` 或直接版本组
  - `AndroidX core/activity/lifecycle/appcompat/datastore/profileinstaller`
  - `AboutLibraries`
  - `ktlint`
  - `detekt`
  - `NDK`
  - `CMake`
- 不要先改文件再临时查版本；先把目标矩阵写清楚。

### 2. 升级 Gradle 与 Android 构建主轴

- 升级 `gradle-wrapper.properties`。
- 升级 root `build.gradle.kts` 中的：
  - `com.android.application`
  - `org.jetbrains.kotlin.plugin.compose`
  - `org.jlleitschuh.gradle.ktlint`
- 如目标 AGP 要求，切换仓库统一 `JDK` 基线。
- 清理任何只为旧 `Gradle` / 旧 `AGP` 留下的绕路配置。

### 3. 升级 Android SDK 基线

- `compileSdk` 升到当前最新稳定 API level。
- `targetSdk` 同步拉到当前发布目标口径。
- 如有需要，显式补 `buildToolsVersion` 或确认继续走 AGP 默认值。
- 复核 `localeFilters`、manifest、权限、前台服务、导出组件声明是否需要跟进新 SDK 规则。

### 4. 升级 Kotlin 与 Compose 组

- 对齐：
  - Kotlin
  - Compose Compiler plugin
  - Compose UI/material/material3/activity-compose
- 处理由新版 Kotlin/Compose 带来的：
  - API 废弃
  - 编译警告升级为错误
  - preview/test helper 变更
  - 编译器 flag 变化

### 5. 升级 AndroidX 与 app 级依赖

- 升级以下主依赖到最新稳定版：
  - `androidx.core`
  - `androidx.appcompat`
  - `androidx.activity`
  - `androidx.lifecycle`
  - `androidx.datastore`
  - `androidx.profileinstaller`
  - `androidx.test`
  - `robolectric`
  - `kotlinx-coroutines-test`
- 遇到“依赖要求更高 compileSdk”的情况，优先继续提升基线，不回退依赖。

### 6. 重做质量工具接线

- `detekt`
  - 继续优先 CLI task 方案，除非官方 Gradle 插件已完全消除新的废弃 API。
  - 升级规则集和 CLI 版本。
- `ktlint`
  - 升级插件版本。
  - 复核 `additionalEditorconfig` 里旧规则 key 是否已变更。
- `verify` / `android quality`
  - 保持入口不变；
  - 允许内部任务名和实现方式随新工具链调整。

### 7. 重新决定 AboutLibraries 接线

- 先评估当前最新稳定版 AboutLibraries 官方推荐路径。
- 如果主插件仍是官方推荐默认方案：
  - 继续保留主插件；
  - 但要清理不再需要的手工胶水。
- 如果 Android 专用插件已经稳定且对当前 AGP 生效：
  - 切到 `plugin.android`；
  - 同步删掉旧的 `sourceSets` / `preBuild.dependsOn(...)` 手工补线。
- 无论选哪条路径，都要求：
  - 开源许可页可编译；
  - 导出 JSON/资源流程清晰；
  - 配置期无新的高噪音警告。

### 8. 升级 native toolchain

- 升级 `NDK` 与 `CMake` pin。
- 重跑并复核：
  - `externalNativeBuildDebug`
  - `modules-smoke`
  - `native-debug`
- 如果新版 NDK/CMake 改变了 `externalNativeBuild` 行为，同步更新：
  - [android-native-strategy.md](/C:/code/WaveBits/docs/architecture/android/android-native-strategy.md)
  - [android-ndk-cmake-upgrade-decision.md](/C:/code/WaveBits/docs/notes/android/android-ndk-cmake-upgrade-decision.md)

### 9. 清理旧时代遗留配置

- 检查并删除：
  - 已无效的 Gradle workaround
  - 已废弃的 plugin DSL 兼容代码
  - 旧 API level 特判
  - 只为旧 AboutLibraries / 旧 detekt 保留的胶水任务
  - 过时依赖的临时 suppress / ignore
- 原则是“升级后收敛”，不是“新旧配置并存”。

### 10. 更新自动化与文档

- 更新：
  - [apps/audio_android/README.md](/C:/code/WaveBits/apps/audio_android/README.md)
  - [apps/audio_android/AGENTS.md](/C:/code/WaveBits/apps/audio_android/AGENTS.md)
  - [docs/notes/android/detekt.md](/C:/code/WaveBits/docs/notes/android/detekt.md)
  - `docs/presentation/android/...` 中对应版本 history
  - `docs/legal/third_party_inventory.md`
- 如 `tools/run.py android ...` 行为变化，同步更新命令说明和 CI 文档。

## 强制验证门槛

升级完成后，至少串行跑完：

1. `python tools/run.py android test-debug`
2. `python tools/run.py android assemble-debug`
3. `python tools/run.py android assemble-staging`
4. `python tools/run.py android quality`
5. `python tools/run.py android native-debug`

如本轮触及 release 相关构建接线，再补：

1. `python tools/run.py android assemble-release`
2. translation lint / key alignment 相关入口

## 验收标准

- 所有版本 pin 已显式落盘。
- Android 主构建、质量入口、native 入口均通过。
- 不再存在“因为旧 `compileSdk` 卡住新依赖”的问题。
- 不再存在已确认的 Gradle 高版本废弃 API 警告。
- AboutLibraries/静态检查/翻译校验接线都能被后续 agent 直接理解。
- 文档与仓库实际基线一致。

## 不要做的事

- 不要为了保住旧依赖而拒绝提升 `compileSdk`。
- 不要把“本机能过一次”当成升级完成。
- 不要把构建失败简单归因给代理或网络，先确认是否已经进入真实编译/依赖兼容问题。
- 不要在升级提交里混入无关业务功能。
- 不要保留一套“以后也许用得上”的旧插件接线。

## 推荐交付拆分

建议按下面 4 个提交批次推进：

1. `Gradle / AGP / JDK / compileSdk / targetSdk`
2. `Kotlin / Compose / AndroidX / app dependencies`
3. `detekt / ktlint / AboutLibraries / translation / tooling glue`
4. `NDK / CMake / docs / CI / history`

这样失败时更容易定位，也更容易回顾哪一层引入了问题。
