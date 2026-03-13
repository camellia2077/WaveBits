# 编译命令速记

更新时间：2026-03-13

## 约定
- 仓库根目录：`C:\code\WaveBits`
- `build/` 只保留 CMake / Gradle 的原生构建输出与测试产物
- `dist/` 只保留 Python 复制出的最终导出物
- 推荐统一使用：`python tools/run.py <command>`
- host 默认编译器：`clang++`

## Host 本地构建

### host 默认 modules 主路径
- 默认走 `WAVEBITS_HOST_MODULES=ON`：

```powershell
python tools/run.py configure --build-dir build/dev
python tools/run.py build --build-dir build/dev
python tools/run.py test --build-dir build/dev
```

- root host `WAVEBITS_HOST_MODULES=OFF` 已退休。

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
