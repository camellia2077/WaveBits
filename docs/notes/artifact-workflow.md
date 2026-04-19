# Artifact Workflow

更新时间：2026-04-19

## 入口

- 主命令组：`python tools/run.py artifact --help`
- 子命令：
  - `python tools/run.py artifact roundtrip --help`
  - `python tools/run.py artifact smoke --help`
  - `python tools/run.py artifact export-apk --help`

## 职责

- `artifact roundtrip`
  - 生成单个可见 `WAV` roundtrip 产物
  - 适合手工检查单个 case
- `artifact smoke`
  - 生成 `flash / pro / ultra` 代表性产物批次
  - 适合回归观察与人工验收
- `artifact export-apk`
  - 读取 Android Gradle APK metadata
  - 把最终 APK 导出到 `dist/android/`

## 高频示例

```powershell
python tools/run.py artifact roundtrip --build-dir build/dev --mode flash --text "Hello"
python tools/run.py artifact smoke --build-dir build/dev
python tools/run.py artifact export-apk
python tools/run.py artifact export-apk release
```

## 输出位置

- roundtrip / smoke 默认输出到：
  - `build/test-artifacts/`
- APK 默认导出到：
  - `dist/android/`

## 说明

- 更细的参数不要在总说明里重复列出，统一以命令级 `--help` 为准。
- Android APK 的原生构建入口仍然在 `apps/audio_android/`；`artifact export-apk` 只负责导出，不替代 Gradle 构建本身。
