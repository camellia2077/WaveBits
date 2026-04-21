# 编译命令速记

更新时间：2026-03-14

## 约定
- 仓库根目录：`<repo-root>`
- `build/` 只保留 CMake / Gradle 的原生构建输出与测试产物
- `dist/` 只保留 Python 复制出的最终导出物
- 推荐统一使用：`python tools/run.py <command>`
- host 唯一正式主线：`clang++ + Ninja + build/dev`

## Host 本地构建

### host 默认 modules 主路径
- 默认直接走当前唯一受支持 host 主线：

```powershell
python tools/run.py configure --build-dir build/dev
python tools/run.py build --build-dir build/dev
python tools/run.py test --build-dir build/dev
```

- root host 不再名义支持 GNU / MSVC / Visual Studio / Ninja Multi-Config 组合。

## 一键验证

### host 默认主路径

```powershell
python tools/run.py verify --build-dir build/dev --skip-android
```

## Android 验证与编译

### Android focused gate

```powershell
python tools/run.py android native-debug
python tools/run.py android assemble-debug
```

### Release

```powershell
python tools/run.py android assemble-release
```

### 如需先清理再编

```powershell
python tools/run.py android assemble-debug --clean
```

## 导出 APK

导出 Debug APK 到 `dist/android/`：

```powershell
python tools/run.py artifact export-apk
```

导出 Release APK 到 `dist/android/`：

```powershell
python tools/run.py artifact export-apk release
```

如果 APK 还没编译出来，可以让脚本自动先编译：

```powershell
python tools/run.py artifact export-apk --assemble-if-missing
python tools/run.py artifact export-apk release --assemble-if-missing
```

## Gradle 原生命令

默认不要把下面这些命令当成仓库标准入口；优先使用上面的 `python tools/run.py android ...`。

只有在排查 Gradle wrapper / IDE / `--stacktrace` 细节时，才直接在 `apps/audio_android/` 执行：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat clean :app:assembleDebug
```
