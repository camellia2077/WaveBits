# 编译命令速记

更新时间：2026-03-11

## 约定
- 仓库根目录：`C:\code\WaveBits`
- `build/` 只保留 CMake / Gradle 的原生构建输出与测试产物
- `dist/` 只保留 Python 复制出的最终导出物
- 推荐统一使用：`python tools/run.py <command>`
- host 默认编译器：`clang++`

## CMake 本地构建

host 默认走 C++20 modules：

```powershell
python tools/run.py configure --build-dir build/dev
python tools/run.py build --build-dir build/dev
python tools/run.py test --build-dir build/dev
```

如需显式回退旧的 header 兼容路径：

```powershell
python tools/run.py configure --build-dir build/legacy-host --no-modules
python tools/run.py build --build-dir build/legacy-host
python tools/run.py test --build-dir build/legacy-host
```

## 一键验证

```powershell
python tools/run.py verify --build-dir build/dev
```

如需跳过 Android：

```powershell
python tools/run.py verify --build-dir build/dev --skip-android
```

如需验证 legacy header fallback：

```powershell
python tools/run.py verify --build-dir build/legacy-host --skip-android --no-modules
```

## Android 编译

Debug：

```powershell
python tools/run.py android assemble-debug
```

Release：

```powershell
python tools/run.py android assemble-release
```

只编原生部分：

```powershell
python tools/run.py android native-debug
```

如果改了 Gradle / CMake / 依赖，建议先清理再编：

```powershell
python tools/run.py android assemble-debug --clean
```

## 导出 APK

导出 Debug APK 到 `dist/android/`：

```powershell
python tools/run.py export-apk
```

导出 Release APK 到 `dist/android/`：

```powershell
python tools/run.py export-apk release
```

如果 APK 还没编译出来，可以让脚本自动先编译：

```powershell
python tools/run.py export-apk --assemble-if-missing
python tools/run.py export-apk release --assemble-if-missing
```

## Gradle 原生命令

如需直接走 Gradle，也是在仓库根目录执行：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat clean :app:assembleDebug
```
