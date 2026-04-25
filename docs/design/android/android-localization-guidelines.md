# Android Localization Guidelines

本文件定义 Android 多语言文案新增、修改和翻译时的最小规则。

## Source Of Truth

- 工程基线以 `apps/audio_android/app/src/main/res/values/strings.xml` 为准。
- 新增 `string` key 时，先落 `values/strings.xml`，再同步到其他语言目录。
- 但翻译语义不以“英文逐词直译”为唯一标准；真正的语义真源是：
  - `string key` 表达的职责
  - 当前 UI 场景中的产品意图
  - 项目内已经稳定下来的术语口径

换句话说：

- **工程真源**：`values/strings.xml`
- **语义真源**：`key + 场景 + 术语口径`

## Required Locales

修改可见 XML 文案时，至少同步检查：

- `apps/audio_android/app/src/main/res/values/strings.xml`
- `apps/audio_android/app/src/main/res/values-zh/strings.xml`
- `apps/audio_android/app/src/main/res/values-zh-rTW/strings.xml`
- `apps/audio_android/app/src/main/res/values-ja/strings.xml`

当前仓库还维护：

- `apps/audio_android/app/src/main/res/values-de/strings.xml`
- `apps/audio_android/app/src/main/res/values-es/strings.xml`
- `apps/audio_android/app/src/main/res/values-pt-rBR/strings.xml`
- `apps/audio_android/app/src/main/res/values-ru/strings.xml`

如果本次改动涉及用户可见文案，默认应一起补齐，不要只改英文后留下其它语言漂移。

## Translation Rules

- 优先翻译“用户要理解的动作和结果”，不要拘泥英文表面词形。
- 同一术语在同一语言里保持稳定，不要一处叫 `Config`、另一处叫 `Settings`、再另一处叫 `Preferences`。
- UI 空间很紧时，优先保留清晰、短而稳定的产品化说法，不追求完整直译。
- 如果英文为了开发方便写得偏技术，落到其他语言时可以改成更自然的用户表达，但不能改变实际行为。
- 不要把品牌风格词翻成过度文学化的长句，避免按钮、标题、标签失去可读性。

## Term Priority

当英文原文、历史译法和当前场景冲突时，优先级如下：

1. 当前功能真实行为
2. 已在产品内稳定使用的术语
3. 当前页面的 UI 长度与可读性
4. 英文原文表面措辞

这意味着英文不是“必须逐字跟随”的最高优先级。

## Product Terms

以下术语应优先保持稳定，不要在不同语言里频繁换说法：

- `Config`
- `Dual-tone`
- `flash voicing`
- `saved audio`
- `decoded text`
- `library`
- `Input text`

如果要改这些词，应该成组检查相关页面和语言资源，而不是只改一处。

## Mode Input Encoding

`Input text` 的本地化、随机样例与默认示例，必须遵守各 transport mode 的输入编码约束；不要只按语言习惯改文案，而忽略 mode 本身的可编码范围。

- `flash`
  - 输入文本不做字符集限制。
  - 它关注的是原始输入字节的透明传输，而不是特定字符集语义。
  - 因此 `flash` 的默认示例与随机样例可以随 app language 本地化。

- `pro`
  - 输入文本必须保持 `ASCII-only`。
  - 因此 `pro` 的默认示例、随机样例和任何预填文案，都不应引入非 ASCII 字符。
  - 即使 app language 切换到中文、日文、俄文等语言，`pro` 的样例也应继续提供可直接编码的 ASCII 文本。

- `ultra`
  - 输入文本按 `UTF-8` byte 处理。
  - 因此 `ultra` 的默认示例与随机样例可以随 app language 本地化，不需要沿用 `pro` 的 ASCII-only 约束。

## Sample Text Rules

如果改动涉及 `Input text` 的默认示例、随机样例或语言切换逻辑，除了检查翻译语义，还要同时检查：

- 当前文案是否符合对应 mode 的输入编码约束
- 同一个 sample slot 在不同语言下是否保持大体语义一致
- `pro` 样例是否仍然是可直接编码的 ASCII 文本
- `flash` / `ultra` 样例是否在符合产品风格的前提下做了自然本地化

## Non-XML Text

如果改动涉及以下内容，也要同步检查是否需要跟语言一起更新：

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/data/AndroidSampleInputTextProvider.kt`
- 与随机样例、默认示例、语言切换相关的 Kotlin 文案来源

不要只改 `strings.xml`，却遗漏样例文本、默认文本或语言切换逻辑。

## Review Checklist

- 新 key 是否先落在 `values/strings.xml`
- 其它语言是否同步补齐
- 术语是否和现有页面保持一致
- 按钮/标题是否因为直译过长而破坏布局
- 是否遗漏 `AndroidSampleInputTextProvider.kt` 之类的非 XML 文案来源

## Rule Of Thumb

可以把这条规则记成一句话：

> 以英文 `values/strings.xml` 做工程基线，以产品语义和稳定术语做翻译基线。
