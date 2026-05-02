# Android Translation Tooling Agent Index

这是一页给 agent / 开发者的短索引。

它不解释 translate 工具内部实现，只回答三类问题：

1. 现在该不该看翻译工具文档
2. 现在该跑哪个命令
3. 现在该去看哪篇更详细的文档

如果你要看完整工具地图，再去：

- [tools/scripts/android/translate/docs/architecture.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/architecture.md)

如果你要看 Android app 里的日常落地流程，再去：

- [android-translation-workflow.md](/C:/code/WaveBits/docs/design/android/android-translation-workflow.md)

## Start Here

只有在下面这些场景里，才建议展开 translate 工具文档：

- 你改了 `res/values*` 下的可见 XML 文案
- 你改了 `audio_samples_*.xml`
- 你改了样例文本、默认文案、语言切换后的样例内容
- Android 构建因为 translation / localization / key alignment 失败
- 你想生成翻译 review markdown
- 你想把 LLM 或人工给出的翻译修订安全写回 XML

如果这次改动只是 Kotlin、Compose、JNI、播放逻辑，不碰文本资源，通常不用继续往下看。

## Fast Decisions

### 1. 我只是改了 XML 文案，先做什么

先看：

- [android-translation-workflow.md](/C:/code/WaveBits/docs/design/android/android-translation-workflow.md)

然后至少知道这两件事：

- 新增 key 必须走 `python tools/run.py android strings-add ...`
- `strings-add` 默认只写英文基线，并生成缺译任务报告
- Android 构建会自动触发 key alignment

### 2. 我想先检查本地化结构有没有漂移

跑：

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py key-alignment"
```

说明文档：

- [check_translation_key_alignment.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/check_translation_key_alignment.md)

### 3. 构建失败了，提示 translation key alignment

先看：

- `temp/translation_key_alignment_reports/`

重点读：

- `temp/translation_key_alignment_reports/<locale>/<locale>_translation_tasks.md`

然后再回：

- [android-translation-workflow.md](/C:/code/WaveBits/docs/design/android/android-translation-workflow.md)

### 4. 我刚新增了英文 key，要补其他语言

默认先跑：

```powershell
pwsh -NoLogo -Command "python tools/run.py android strings-add --file strings_audio.xml --key sample_key --en 'Sample text'"
```

然后按报告补齐：

- `temp/translation_key_alignment_reports/<locale>/<locale>_translation_tasks.md`
- [tools/scripts/android/translate/AGENTS.md](/C:/code/WaveBits/tools/scripts/android/translate/AGENTS.md)

### 5. 我想生成给 LLM / 人工审校的翻译对照 Markdown

先看：

- [compare_translation_quality.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/compare_translation_quality.md)

再决定是否需要：

- [sop.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/sop.md)

### 6. 我已经拿到翻译修订 JSON，想安全写回 XML

先看：

- [apply_translation_replacements.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/apply_translation_replacements.md)

如果 JSON 质量不稳，再补看：

- [sop.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/sop.md)

### 7. 我怀疑某个语言文件里混进了别的语言

看：

- [check_mixed_language.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/check_mixed_language.md)

### 8. 我是来改工具本身，不是用工具

直接看完整地图：

- [architecture.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/architecture.md)

## Commands You Will Actually Use

最常用的通常只有这几个：

### Key alignment

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py key-alignment"
```

### Key alignment quiet

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py key-alignment --quiet"
```

### Add Android XML key

```powershell
pwsh -NoLogo -Command "python tools/run.py android strings-add --file strings_audio.xml --key sample_key --en 'Sample text'"
```

### Compare

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py compare"
```

### Replace

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py replace --json <path-to-json>"
```

## Most Useful Documents By Task

按任务选文档：

- Android 改文案时的日常流程：
  - [android-translation-workflow.md](/C:/code/WaveBits/docs/design/android/android-translation-workflow.md)
- 翻译语义、术语、长度规则：
  - [android-localization-guidelines.md](/C:/code/WaveBits/docs/design/android/android-localization-guidelines.md)
- 英文拆分资源如何对应到多语言文件：
  - [android-split-strings-translation-guide.md](/C:/code/WaveBits/docs/design/android/translation/android-split-strings-translation-guide.md)
- 想生成 review markdown：
  - [compare_translation_quality.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/compare_translation_quality.md)
- 想把 JSON 写回 XML：
  - [apply_translation_replacements.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/apply_translation_replacements.md)
- 想查 key 对齐问题：
  - [check_translation_key_alignment.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/check_translation_key_alignment.md)
- 想查工具代码结构：
  - [architecture.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/architecture.md)

## One-Line Memory

可以把它记成一句话：

> 改文案先看 workflow，查结构先跑 key-alignment，改工具本身再看 architecture。
