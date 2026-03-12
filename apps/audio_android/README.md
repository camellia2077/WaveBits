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

## 说明

- Android 官方 `Gradle` 入口固定在仓库根目录，不在 `apps/audio_android/`。
- Android Studio / IntelliJ 应直接打开仓库根目录，而不是单独打开 `apps/audio_android/`。
