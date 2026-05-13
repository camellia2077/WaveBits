# Android 编译命令

优先从仓库根目录使用 `tools/run.py android`，不要再默认直接调用旧的 `tools/build_android.py`。


## Release APK

```powershell
python tools/run.py android assemble-release
```

## Release APK（先 clean）

```powershell
python tools/run.py android assemble-release --clean
```

## Release adb install 

```powershell
python tools/run.py android assemble-release

adb install -r apps\audio_android\app\build\outputs\apk\release\FlipBits-release.apk
```

如果签名不一致又想装，只能先卸载旧包再装（会丢应用数据）：

adb uninstall com.your.package
adb install path\to\app-release.apk


## Debug 

python tools/run.py android assemble-debug

adb install -r apps/audio_android/app/build/outputs/apk/debug/app-debug.apk

adb shell am start -n com.bag.audioandroid/.MainActivity
