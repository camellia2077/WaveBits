# apps/audio_android Agent Rules

- `apps/audio_android` 只存放 Android 模块源码，不作为独立 `Gradle` root 使用。
- Android 官方 `Gradle` 入口固定在仓库根目录 `C:\code\WaveBits`。
- 修改 `apps/audio_android` 下的代码后，最小验证优先运行：
  - Windows: `.\gradlew.bat :app:assembleDebug`
  - macOS/Linux: `./gradlew :app:assembleDebug`
- 修改 Android `Gradle` / `CMake` / JNI / 依赖接线后，建议运行：
  - Windows: `.\gradlew.bat clean :app:assembleDebug`
  - macOS/Linux: `./gradlew clean :app:assembleDebug`
- 需要 release 验证时，运行：
  - Windows: `.\gradlew.bat :app:assembleRelease`
  - macOS/Linux: `./gradlew :app:assembleRelease`
- 需要导出 APK 时，优先运行：
  - `python tools/run.py export-apk`
  - `python tools/run.py export-apk release`
- 需要排查 Android 构建失败时，优先运行：
  - Windows: `.\gradlew.bat :app:assembleDebug --stacktrace`
