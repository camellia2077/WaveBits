# Android Translation Workflow

这篇文档只讲一件事：

- 当 Android 改动涉及 XML 文案、样例文本或本地化结构时，agent / 开发者应该什么时候看翻译文档、什么时候跑翻译工具、失败后去哪里看报告。

它的目标不是代替翻译规则本身，而是把“工具怎么接进日常改动流程”讲清楚。

相关文档：

- agent 速查索引：
  - [android-translation-tooling-agent-index.md](/C:/code/WaveBits/docs/design/android/android-translation-tooling-agent-index.md)
- 翻译语义与术语规则：
  - [android-localization-guidelines.md](/C:/code/WaveBits/docs/design/android/android-localization-guidelines.md)
- 英文拆分资源的翻译说明：
  - [android-split-strings-translation-guide.md](/C:/code/WaveBits/docs/design/android/translation/android-split-strings-translation-guide.md)
- translate 工具总览：
  - [tools/scripts/android/translate/docs/architecture.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/architecture.md)
- key 对齐检查说明：
  - [tools/scripts/android/translate/docs/check_translation_key_alignment.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/check_translation_key_alignment.md)

## When To Read

只有在本次改动确实碰到下面这些内容时，才建议展开看这篇文档：

- 新增或修改 `res/values*` 下的可见 XML 文案
- 新增或修改 `audio_samples_*.xml`
- 调整默认样例、随机样例、语言切换后的样例内容
- 处理构建里和翻译对齐相关的失败
- 想手动使用 `tools/scripts/android/translate/run.py`

如果这次改动只是 Kotlin/JNI/播放逻辑，不碰文本资源，通常不用读这篇。

## Source Of Truth

- 英文 `res/values/` 是结构真源。
- 英文现在按职责拆在：
  - `strings_common.xml`
  - `strings_audio.xml`
  - `strings_saved.xml`
  - `strings_settings.xml`
  - `strings_about.xml`
  - `strings_validation.xml`
- 本地化目录应跟随英文文件结构，不要靠猜测去找“应该改哪个文件”。
- 真正不确定时，优先跑 key alignment 工具，而不是手工目测目录结构。

## Default Workflow

推荐顺序：

1. 新增 key 时必须用 `python tools/run.py android strings-add --file <strings_*.xml> --key <name> --en "<English text>"`
2. `strings-add` 默认只写英文 `values/` 基线，并自动生成 translation key alignment 报告
3. 根据 `temp/translation_key_alignment_reports/` 里的语言任务补齐 `values-*`
4. 补齐本地化时优先使用 `tools/scripts/android/translate/AGENTS.md` 的 agent job 流程，不要手工把英文复制成 localized fallback
5. 修改已有 key 时先改英文基线 XML，再用 translate 工具生成 review / task / replacement 产物修订对应语言
6. 如果改动涉及样例文本，再检查：
   - `apps/audio_android/app/src/main/java/com/bag/audioandroid/data/AndroidSampleInputTextProvider.kt`
   - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/SampleInputSessionUpdater.kt`
7. 运行 Android 验证
8. 如果翻译检查失败，去看自动生成的报告再补齐

例外：

- 只有品牌名、协议 token、不可翻译 UI 符号等明确全语言共享的文本，才允许传 `--localized`
- `values-la` 是 Dog Latin / High Gothic 风格，不是真正的 classical Latin
- `values-uk` 必须按乌克兰语处理，不要照搬俄语资源

## Tool Entry

统一入口：

- [tools/scripts/android/translate/run.py](/C:/code/WaveBits/tools/scripts/android/translate/run.py)

最常用命令：

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py key-alignment"
```

新增 key 脚手架：

```powershell
pwsh -NoLogo -Command "python tools/run.py android strings-add --file strings_audio.xml --key sample_key --en 'Sample text'"
```

这条命令会：

- 写入英文 `res/values/<file>`
- 生成 `temp/translation_key_alignment_reports/`
- 不会默认写入 `values-*`

静默模式：

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py key-alignment --quiet"
```

它会检查：

- localized key 是否都来自英文基线
- localized 文件是否和英文文件结构对齐

更多子命令见：

- [android-translation-tooling-agent-index.md](/C:/code/WaveBits/docs/design/android/android-translation-tooling-agent-index.md)
- [tools/scripts/android/translate/docs/architecture.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/architecture.md)

## Gradle Integration

Android app 的 Gradle 已经把翻译结构检查接进默认构建链路。

定义位置：

- [apps/audio_android/app/build.gradle.kts](/C:/code/WaveBits/apps/audio_android/app/build.gradle.kts)

当前行为：

- `preBuild` 依赖 `checkTranslationKeyAlignment`
- `check` 也依赖 `checkTranslationKeyAlignment`
- `checkTranslationKeyAlignment` 会调用：
  - `tools/scripts/android/translate/run.py key-alignment --quiet`

这意味着：

- 跑 `python tools/run.py android assemble-debug`
- 或直接跑 `gradlew :app:assembleDebug`

时，都会自动触发 key 对齐检查。

## Failure Output

如果 key alignment 失败：

- 命令会返回非零退出码
- Gradle 里的 `checkTranslationKeyAlignment` 会失败
- 工具会生成按语言拆分的任务报告

输出目录：

- `temp/translation_key_alignment_reports/`

典型文件形态：

- `temp/translation_key_alignment_reports/fr/fr_translation_tasks.md`
- `temp/translation_key_alignment_reports/ja/ja_translation_tasks.md`

这些 `*_translation_tasks.md` 报告会告诉你：

- 缺的是哪个文件
- 缺的是哪个 key
- 英文基线是什么
- 当前目标语言应该去补哪个资源文件

## How To Use The Reports

建议读法：

1. 先看失败语言目录
2. 打开对应 `*_translation_tasks.md`
3. 根据 `DIR / FILE / KEY / EN / CONTEXT` 补翻译
4. 再重新跑：

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py key-alignment --quiet"
```

如果这里只是结构缺失：

- 不要先去大面积改术语
- 先把缺文件、缺 key、文件对齐问题修平

## What This Tool Is Good At

这套工具最适合做：

- 英文新增 key 后，批量发现哪些语言没跟上
- 英文拆分文件后，发现哪些语言目录结构还没对齐
- 生成适合人工或 LLM 补翻译的任务清单

它不是：

- 自动把所有翻译都做完的黑盒
- 语义质量的最终裁判

语义、术语、长度与产品口径，还是要回到：

- [android-localization-guidelines.md](/C:/code/WaveBits/docs/design/android/android-localization-guidelines.md)

## Agent Guidance

对 agent 来说，最实用的规则是：

- 不碰 XML 文案时，不要主动展开这条文档
- 一旦碰到 XML 文案或样例文本，就应该优先意识到：
  - 这次改动可能要同步多语言
  - 构建时会自动触发 key alignment
  - 失败后先看 `temp/translation_key_alignment_reports`

可以把它记成一句话：

> 改文案时先想多语言，构建失败先看 translation key alignment 报告。
