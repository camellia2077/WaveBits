# Third-Party Inventory

更新时间：2026-04-21

## 目的

这份文档记录当前仓库中已明确声明或可直接扫描到的第三方依赖线索，作为后续补齐：

- `THIRD_PARTY_NOTICES.*`
- 分发包内许可证文件
- CLI / Android 内“查看许可证”功能

的稳定来源文件。

更细的 CLI 专属落地文档已拆分到：

- `docs/legal/cli_third_party_inventory.md`
- `docs/legal/cli_third_party_notices.md`

这是一份初版 inventory，不等同于最终法律结论。对于最终是否随产品分发、具体许可证文本、静态/动态链接义务，仍需逐项确认上游项目的正式许可证与发布形式。

## 记录约定

- `范围`
  - `CLI`
  - `libs/native`
  - `Android`
- `角色`
  - `分发期依赖`：较大概率进入最终产品分发
  - `构建/质量工具`：主要用于构建、测试或质量检查
- `来源`
  - 只记录当前仓库内可直接扫描到的声明来源，例如 `Cargo.toml`、`Cargo.lock`、`CMakeLists.txt`、`build.gradle.kts`
- `状态`
  - `已确认`：仓库内已有明确声明
  - `待确认`：仍需补许可证文本、分发形态或版本口径

## CLI

### 分发期依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| clap | 4.6.1 | `apps/audio_cli/rust/Cargo.toml` / `Cargo.lock` | Rust CLI 参数解析 | 已确认 | 当前唯一直接运行时 crates.io 依赖 |
| clap_builder | 4.6.0 | `cargo tree --edges normal,build` | `clap` 传递依赖 | 已确认 | 由 `clap` 引入 |
| anstream | 1.0.0 | `cargo tree --edges normal,build` | `clap_builder` 传递依赖 | 已确认 | 由 `clap_builder` 引入 |
| anstyle | 1.0.14 | `cargo tree --edges normal,build` | `anstream` / `clap_builder` 传递依赖 | 已确认 | 由 `clap` 依赖链引入 |
| anstyle-parse | 1.0.0 | `cargo tree --edges normal,build` | `anstream` 传递依赖 | 已确认 | 由 `clap` 依赖链引入 |
| anstyle-query | 1.1.5 | `cargo tree --edges normal,build` | `anstream` 传递依赖 | 已确认 | Windows 终端相关查询 |
| anstyle-wincon | 3.0.11 | `cargo tree --edges normal,build` | `anstream` 传递依赖 | 已确认 | Windows 控制台相关支持 |
| colorchoice | 1.0.5 | `cargo tree --edges normal,build` | `anstream` 传递依赖 | 已确认 | 由 `clap` 依赖链引入 |
| is_terminal_polyfill | 1.70.2 | `cargo tree --edges normal,build` | `anstream` 传递依赖 | 已确认 | 由 `clap` 依赖链引入 |
| utf8parse | 0.2.2 | `cargo tree --edges normal,build` | `anstream` / `anstyle-parse` 传递依赖 | 已确认 | 由 `clap` 依赖链引入 |
| clap_lex | 1.1.0 | `cargo tree --edges normal,build` | `clap_builder` 传递依赖 | 已确认 | 由 `clap` 依赖链引入 |
| strsim | 0.11.1 | `cargo tree --edges normal,build` | `clap_builder` 传递依赖 | 已确认 | 由 `clap` 依赖链引入 |
| windows-sys | 0.61.2 | `cargo tree --edges normal,build` | Windows 相关传递依赖 | 已确认 | 当前通过 `anstyle-query` / `anstyle-wincon` 引入 |
| windows-link | 0.2.1 | `cargo tree --edges normal,build` | Windows 相关传递依赖 | 已确认 | 当前通过 `windows-sys` 引入 |

### 构建期依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| clap_derive | 4.6.1 | `cargo tree --edges normal,build` | `clap` proc-macro | 已确认 | 构建期代码生成 |
| heck | 0.5.0 | `cargo tree --edges normal,build` | `clap_derive` 传递依赖 | 已确认 | 构建期依赖 |
| proc-macro2 | 1.0.106 | `cargo tree --edges normal,build` | `clap_derive` 传递依赖 | 已确认 | 构建期依赖 |
| quote | 1.0.45 | `cargo tree --edges normal,build` | `clap_derive` 传递依赖 | 已确认 | 构建期依赖 |
| syn | 2.0.117 | `cargo tree --edges normal,build` | `clap_derive` 传递依赖 | 已确认 | 构建期依赖 |
| unicode-ident | 1.0.24 | `cargo tree --edges normal,build` | proc-macro 依赖链 | 已确认 | 构建期依赖 |

### 开发/测试依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| assert_cmd | 2.2.1 | `Cargo.lock` | CLI 集成测试 | 已确认 | `dev-dependencies` |
| predicates | 3.1.4 | `Cargo.lock` | CLI 测试断言 | 已确认 | `dev-dependencies` |
| tempfile | 3.27.0 | `Cargo.lock` | CLI 测试临时目录 | 已确认 | `dev-dependencies` |

## libs/native

### 分发期依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| libsndfile / sndfile | 系统提供，版本待确认 | `libs/audio_io/CMakeLists.txt` 中 `pkg_check_modules(SNDFILE REQUIRED IMPORTED_TARGET sndfile)` | `audio_io` 的 WAV 读写 backend | 已确认 | 许可证文本与最终 Windows 分发形态仍需确认；这是当前最需要重点核对的 native 第三方依赖 |

### 构建期依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| PkgConfig | 系统提供，版本待确认 | `libs/audio_io/CMakeLists.txt` 中 `find_package(PkgConfig REQUIRED)` | 发现 `sndfile` | 已确认 | 构建工具，不属于最终产品运行时库 |

## Android

### 分发期依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| androidx.appcompat:appcompat | 1.7.0 | `apps/audio_android/app/build.gradle.kts` | Android UI/runtime 支持 | 已确认 | APK 分发期依赖 |
| androidx.core:core-ktx | 1.13.1 | `apps/audio_android/app/build.gradle.kts` | Android Kotlin 扩展 | 已确认 | APK 分发期依赖 |
| androidx.datastore:datastore-preferences | 1.1.1 | `apps/audio_android/app/build.gradle.kts` | 偏好存储 | 已确认 | APK 分发期依赖 |
| androidx.activity:activity-compose | 1.9.2 | `apps/audio_android/app/build.gradle.kts` | Compose Activity 集成 | 已确认 | APK 分发期依赖 |
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.8.6 | `apps/audio_android/app/build.gradle.kts` | ViewModel 支持 | 已确认 | APK 分发期依赖 |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.6 | `apps/audio_android/app/build.gradle.kts` | Compose + ViewModel | 已确认 | APK 分发期依赖 |
| androidx.lifecycle:lifecycle-runtime-compose | 2.8.6 | `apps/audio_android/app/build.gradle.kts` | Compose runtime 生命周期支持 | 已确认 | APK 分发期依赖 |
| androidx.profileinstaller:profileinstaller | 1.4.0 | `apps/audio_android/app/build.gradle.kts` | Profile installer | 已确认 | APK 分发期依赖 |
| androidx.compose.ui:ui | 1.6.8 | `apps/audio_android/app/build.gradle.kts` | Compose UI | 已确认 | APK 分发期依赖 |
| androidx.compose.ui:ui-tooling-preview | 1.6.8 | `apps/audio_android/app/build.gradle.kts` | Compose 预览支持 | 已确认 | APK 分发期依赖 |
| androidx.compose.material:material-icons-extended | 1.6.8 | `apps/audio_android/app/build.gradle.kts` | Material icons | 已确认 | APK 分发期依赖 |
| androidx.compose.material3:material3 | 1.2.1 | `apps/audio_android/app/build.gradle.kts` | Material 3 UI | 已确认 | APK 分发期依赖 |
| com.mikepenz:aboutlibraries-core | 12.2.4 | `apps/audio_android/app/build.gradle.kts` | 第三方依赖信息展示 | 已确认 | APK 分发期依赖 |
| com.mikepenz:aboutlibraries-compose-m3 | 12.2.4 | `apps/audio_android/app/build.gradle.kts` | Compose Material 3 的 AboutLibraries UI | 已确认 | APK 分发期依赖 |

### 构建/质量工具

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| com.android.application Gradle plugin | 9.0.1 | `apps/audio_android/build.gradle.kts` | Android 构建插件 | 已确认 | 构建期工具 |
| org.jetbrains.kotlin.plugin.compose | 2.2.10 | `apps/audio_android/build.gradle.kts` | Compose 编译插件 | 已确认 | 构建期工具 |
| org.jlleitschuh.gradle.ktlint | 14.1.0 | `apps/audio_android/build.gradle.kts` | Kotlin 格式检查 | 已确认 | 构建/质量工具 |
| io.gitlab.arturbosch.detekt | 1.23.8 | `apps/audio_android/build.gradle.kts` | Kotlin 静态检查 | 已确认 | 构建/质量工具 |
| com.mikepenz.aboutlibraries.plugin | 12.2.4 | `apps/audio_android/build.gradle.kts` | 生成 AboutLibraries 资源 | 已确认 | 构建期工具 |
| org.gradle.toolchains.foojay-resolver-convention | 1.0.0 | `apps/audio_android/settings.gradle.kts` | Gradle toolchain 解析 | 已确认 | 构建期工具 |

### 开发/测试依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| junit:junit | 4.13.2 | `apps/audio_android/app/build.gradle.kts` | 单元测试 | 已确认 | `testImplementation` |
| androidx.test:core | 1.6.1 | `apps/audio_android/app/build.gradle.kts` | Android 测试支持 | 已确认 | `testImplementation` |
| org.jetbrains.kotlinx:kotlinx-coroutines-test | 1.8.1 | `apps/audio_android/app/build.gradle.kts` | 协程测试 | 已确认 | `testImplementation` |
| org.robolectric:robolectric | 4.14.1 | `apps/audio_android/app/build.gradle.kts` | Android 单元测试环境 | 已确认 | `testImplementation` |
| androidx.compose.ui:ui-tooling | 1.6.8 | `apps/audio_android/app/build.gradle.kts` | Compose 调试/预览工具 | 已确认 | `debugImplementation` |

## 当前待补项

- 为每个“分发期依赖”补正式许可证类型与上游许可证文本链接
- 确认 `libsndfile` 在 Windows CLI 分发时的实际交付形式：
  - 静态链接
  - 动态链接
  - 仅构建环境依赖
- 确认 Android release APK 的最终依赖清单是否需要从 Gradle 产物再反查一次，和这里的声明级清单做对照
- 基于本文件生成：
  - `THIRD_PARTY_NOTICES.md`
  - 分发包内 `LICENSE` / notices 文件复制逻辑
  - CLI `licenses` / Android About 页面所需的精简展示信息
