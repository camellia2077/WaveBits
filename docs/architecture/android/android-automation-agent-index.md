# Android Automation Agent Index

这是一页给 agent 的自动化薄索引：先判断你要解决的是哪一类自动化问题，再只展开对应文档，不要一次性把所有 Android 自动化文档都读完。

总览与分层判断先看：

- `docs/architecture/android/android-automation-coverage.md`

如果已经知道是具体场景问题，直接按下面分支下钻。

## Pick One Branch

### Flash Playback Automation

适用场景：

- Flash 真机 UI 回归
- Flash `lyrics / visual / mix` 页面切换
- Flash `lanes / pulse / pitch` 可视化 adb 采集
- Flash `Visual / 8 bits / token` 对齐
- `FlashAlignmentPerf` / `FlashVisualPerf` / `FlashLyricsPerf` 分析
- `Lanes` overlay、`Pulse` geometry、稳定 test tag

先读：

- `docs/architecture/android/android-flash-automation.md`

相关但按需再读：

- `docs/architecture/android/android-flash-visual.md`

### Mini Playback Automation

适用场景：

- Mini 真机 UI 回归
- `slow / standard / fast` Morse speed 采集
- Mini `visual / lyrics` 页面切换
- expanded lyrics 采集
- `MiniAlignmentPerf` / `FlashLyricsPerf` / `PlaybackLyricsLayout` 分析

先读：

- `docs/architecture/android/android-mini-automation.md`

### Encode Progress Automation

适用场景：

- `mini / pro / ultra` 生成音频时的编码进度条
- `Preparing input / Rendering PCM / Finalizing` phase 切换
- `capture-encode-progress` / `encode-progress-summary`

先读：

- `docs/architecture/android/android-encode-progress-automation.md`

### Saved Audio Automation

适用场景：

- Saved 列表选择
- player detail 打开时延
- long-audio hydration / file-backed load
- `SavedAudioAutomation` / `SavedAudioPerf`

先读：

- `docs/architecture/android/android-saved-automation.md`

### Settings-Aware Automation

适用场景：

- `wb.lang` 语言覆盖
- measurement baseline reset
- 判断哪些 Settings 状态能通过 adb scenario 驱动
- 判断 `capture-*` wrapper 和 raw adb extras 的差异

先读：

- `docs/architecture/android/android-settings-automation.md`

## Minimal Read Combos

- 判断新增回归放 JVM / instrumentation / adb 哪层：
  - `android-automation-coverage.md`
- Flash 真机对齐或 Visual 调试：
  - `android-automation-agent-index.md` -> `android-flash-automation.md`
- Mini 真机对齐或歌词布局调试：
  - `android-automation-agent-index.md` -> `android-mini-automation.md`
- 编码进度条采集：
  - `android-automation-agent-index.md` -> `android-encode-progress-automation.md`
- Saved 选择与 hydration 时延：
  - `android-automation-agent-index.md` -> `android-saved-automation.md`
- 语言切换或 wrapper/extras 差异：
  - `android-automation-agent-index.md` -> `android-settings-automation.md`

## When To Stop Reading

如果任务只是：

- 跑一次已有 `capture-*` 命令
- 改一个 summary 字段
- 补一个现有 scenario 文档里的小说明

通常读完本页和对应分支的第一篇文档就够了。

## Goal

目标是让 agent：

- 先知道 Android 自动化有哪些分支
- 再按问题类型只读一个分支文档
- 只有在跨场景时才补读第二篇或回到 `android-automation-coverage.md`
