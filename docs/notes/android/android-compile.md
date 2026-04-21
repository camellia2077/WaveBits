# Android 编译命令

优先从仓库根目录使用 `tools/run.py android`，不要再默认直接调用旧的 `tools/build_android.py`。

## Debug APK

```powershell
python tools/run.py android assemble-debug
```

## Release APK

```powershell
python tools/run.py android assemble-release
```

## Release APK（先 clean）

```powershell
python tools/run.py android assemble-release --clean
```

## Native Debug 构建

如果只想验证 Android native/CMake 链路，可运行：

```powershell
python tools/run.py android native-debug
```
