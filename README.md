<p align="center">
  <img src="ui/app/icon-foreground.svg" alt="FlipBits icon" width="128" />
</p>

<p align="center">
  <em>Icon designed by camellia2077 (FlipBits Project)</em>
</p>

<h1 align="center">FlipBits</h1>


<p align="center">
  中文 | <a href="README_en.md">English</a>
</p>

<p align="center">
  <strong>文本与音频信号之间的风格化编码/解析工具，用 FSK 节奏、频率与停顿生成可听的“语气化”编码音频</strong><br />
  <em>Stylized text-to-audio signaling and decoding with FSK rhythm, tone, pause, and visual follow</em>
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-red.svg)](LICENSE)
[![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg)]()
[![CI Android Assemble](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-assemble.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-assemble.yml)
[![CI Android Quality](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-quality.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-quality.yml)
[![CI Host Verify](https://github.com/camellia2077/FlipBits/actions/workflows/ci-host-verify.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-host-verify.yml)


<p align="center">
  <img src="https://github.com/user-attachments/assets/5c2433f5-37b8-4524-a0a0-ce65f6fe4e4d" width="160" title="Chinese" />
  <img src="https://github.com/user-attachments/assets/a288707b-1186-4083-b134-c861dd2abf1a" width="160" title="English" />
  <img src="https://github.com/user-attachments/assets/6d578c41-3487-4c89-937e-a55a575ed58e" width="160" title="Deutsch" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/21ab29a9-a81f-4f26-8770-91403984ef38" width="160" title="dog Latin" />
  <img src="https://github.com/user-attachments/assets/9d517c89-4c80-4a37-ba9d-78b4b3177f26" width="160" title="French" />
  <img src="https://github.com/user-attachments/assets/e2bf65a3-e747-4f77-bfc9-f0717c0bcec1" width="160" title="Russian" />
</p>


## 快速概览
- Android 原生应用，计算机文本编码的可视化，音频化。
- 支持 Morse code (`mini`)、逐 bit BFSK / FSK (`flash`)、DTMF-like 双音映射 (`pro`) 和 `16-FSK` (`ultra`)。
- 可视化提供 Visual 与 Lyrics 两种模式，用于观察音频信号层和文本编码层。
- 支持多语言界面：英语、德语、西班牙语、法语、意大利语、日语、韩语、波兰语、巴西葡萄牙语、俄语、乌克兰语、简体中文、繁体中文，以及用于营造庄重、宗教感、科技感与太空歌剧氛围的 dog Latin。

## 下载 / 安装
Android APK 将通过 GitHub Releases 发布。

当前参考构建下，安装包约 `5.64 MB`，安装后占用约 `6.10 MB`。这些数字会随版本、ABI 与构建配置变化。

## 模式总览
| Mode | 技术类别 | 适合用途 |
| --- | --- | --- |
| `mini` | Morse code | 短促、清晰、节奏可读的点划信号 |
| `flash` | 逐 bit BFSK / FSK | 更强情绪化听感、Visual/Lyrics 对照学习 |
| `pro` | DTMF-like 双音映射 | 更紧凑的双音结构 |
| `ultra` | `16-FSK` 频点映射 | 更短音频、更快生成与解析 |

这些名字不是“强弱等级”，而是项目内部的产品化命名。它们分别强调不同的听感、表达气质和传输结构，而不是同一协议从基础版到高级版的线性升级关系。

## 项目定位
FlipBits 是一个文本与可听音频信号之间的编码/解析工具。它不只把文本转换成声音，也尝试通过逐 bit FSK 的持续时间、停顿间隔、频率组合和播放节奏，让生成音频呈现类似人类不同情绪和语气下说话的听感。

项目可以把文本内容映射为波形，也可以从项目内生成的波形中还原文本；项目本身不提供任何形式的密码学加密。

* **表达重点**：逐 bit BFSK / FSK (`flash`) 会刻意牺牲编码效率，用更长的 bit、停顿和频率变化换取更强的情绪化听感与仪式感。
* **效率补充**：如果需要更短、更快、更正式的文本传输，Morse code (`mini`)、DTMF-like 双音映射 (`pro`) 和 `16-FSK` (`ultra`) 提供了更紧凑的编码路径。`16-FSK` (`ultra`) 不只是生成的音频更短，通常生成消耗和解析耗时也明显低于逐 bit BFSK / FSK (`flash`)；但“更快”不是项目唯一目标。
* **可视化价值**：Android app 提供两种互补的跟随视图。Visual 偏向信号层，展示文本编码后如何变成 FSK low/high bit、频率片段和播放时间轴；Lyrics 偏向文本编码层，用 token 展示文本如何被编码为 UTF-8 bytes、hex/bin 和 bit，并随音频播放高亮。

## Android App 特性
Android app 当前保持轻量原生取向：冷启动速度快，包体控制较小，适合直接生成、转换、分享与导出音频。

## 设计边界
本项目当前重点是“文本 -> 风格化音频 -> 项目内解码”的受控闭环，尤其强调 Android app 内的音频生成、转换、分享与导出体验。

它不以“外放后被另一设备直接实时解析”为主要交互目标，也不以真实环境下的抗噪、抗回声、远场接收或复杂同步鲁棒性为设计优先级。对本项目来说，氛围感、可辨识的风格表达和可控的模式体验，优先于现实声学环境中的通信稳健性。

## 模式说明

### 逐 bit BFSK / FSK (`flash`)
逐 bit BFSK / FSK (`flash`) 是最偏风格化的模式：它用高频 / 低频两种 Hz 作为 bit 状态，再通过调整 bit 持续时间、频率配置与停顿间隔，模拟出更接近人类说话情绪与语气的风格化表达。相同文本在逐 bit BFSK / FSK (`flash`) 下可能生成接近一分钟的音频，而在 `16-FSK` (`ultra`) 下只需几秒。这种差异是有意设计的，目标是保留“像仪式一样被播放出来”的质感，而不是单纯压缩传输时间。

逐 bit BFSK / FSK (`flash`) 的听感来自二进制编码的声学化表达：每个 bit 都只在 low / high 两种频率状态之间切换，对应二进制中的 0 / 1。

逐 bit BFSK / FSK (`flash`) 的低效率是有意保留的。它不是把多个 bit 同时编码到一个 symbol 中，而是让 low/high 两种频率按 bit 顺序逐个发声；再加上 style 对 bit 持续时间、停顿和频率变化的调整，音频长度会快速增长。这样做的目标不是吞吐量，而是可听性和可解释性：用户可以通过声音、Visual 和 Lyrics 对照，看到甚至手动写出对应的 low/high bit 序列。

逐 bit BFSK / FSK (`flash`) 当前提供六种 style。每种 style 都以 low / high 两种 Hz 决定 bit 状态，并通过 bit 持续时间、频率组织与停顿间隔的组合，塑造出不同的情绪化“说话语气”：

| Style | Low / High | 听感目标 |
| --- | --- | --- |
| [Litany](docs/design/modes/flash/litany.md) | `220 / 440 Hz` | 低沉、肃穆、吟诵 |
| [Collapse](docs/design/modes/flash/collapse.md) | `280 / 560 Hz` | 低声、慌张、结巴 |
| [Standard](docs/design/modes/flash/standard.md) | `300 / 600 Hz` | 日常、精确、平稳 |
| [Hostile](docs/design/modes/flash/hostile.md) | `450 / 900 Hz` | 尖锐、急促、攻击 |
| [Zeal](docs/design/modes/flash/zeal.md) | variable `560-900 / 1120-1800 Hz` | 明亮、变速、密集 |
| [Void](docs/design/modes/flash/void.md) | `240 / 480 Hz` | 低沉、拖尾、稀疏 |




<p align="center">
  <video src="https://github.com/user-attachments/assets/600c07d1-ff08-45ae-a399-efcb2de2f6f4" width="400" controls muted autoplay loop style="border-radius: 8px;"></video>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/a4c709a7-9f66-4f97-b0f9-4059f34ca24d" width="180" title="1" />
  <img src="https://github.com/user-attachments/assets/cd47cb45-d518-4c8b-8b41-8991afe76296" width="180" title="2" />
  <img src="https://github.com/user-attachments/assets/768782d2-a6b0-40b9-a032-c1ee5da6ffb3" width="180" title="3" />
  <img src="https://github.com/user-attachments/assets/4cbce966-5165-4870-b4ab-995291f5cbf3" width="180" title="4" />
</p>

<br>


更多 `flash` voicing style 的情绪定位、命名语义与 preset 设计见：
- [`docs/design/modes/flash/README.md`](docs/design/modes/flash/README.md)
- [`docs/design/modes/flash/voicing-emotions.md`](docs/design/modes/flash/voicing-emotions.md)
- [`docs/design/modes/flash/`](docs/design/modes/flash/)

### Morse code (`mini`)
Morse code (`mini`) 会按 Morse 规则规范化输入，强调清晰、可视化友好和点划节奏；当前提供三种 speed preset：

| Speed | 定位 |
| --- | --- |
| Slow | 更慢、更适合观察 dot / dash 和 lyrics follow |
| Standard | 默认 Morse 节奏 |
| Fast | 更短、更紧凑的 Morse 输出 |



<p align="center">
  <video src="https://github.com/user-attachments/assets/e472b66e-3a76-43bf-9c66-09470e721b41" width="700" controls muted autoplay loop style="border-radius: 8px;"></video>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/841e4a6b-f774-4202-8eaa-7d27350951ec" style="width: 31%; border-radius: 4px; border: 1px solid #ddd;" />
  <img src="https://github.com/user-attachments/assets/fd83cc08-cf92-410c-a382-0d54c6cae8c9" style="width: 31%; border-radius: 4px; border: 1px solid #ddd;" />
  <img src="https://github.com/user-attachments/assets/80e917ae-2d70-4d45-8960-9e850ea0a985" style="width: 31%; border-radius: 4px; border: 1px solid #ddd;" />
</p>

<br>

更多 `mini` 的输入规范、follow / visual 与实现说明见：
- [`docs/design/modes/mini.md`](docs/design/modes/mini.md)

### DTMF-like 双音映射 (`pro`)
DTMF-like 双音映射 (`pro`) 是更正式的 ASCII-only 模式：输入文本先转成 ASCII byte，再把每个 byte 拆成高 4 bit / 低 4 bit，并分别映射为双音 symbol，因此 `1 byte = 2 symbol`。它偏向结构清晰、职责单纯的正式声学链路表达。





<p align="center">
  <video src="https://github.com/user-attachments/assets/296d8794-7d5b-484b-8c73-54abbea87cdb" width="700" controls muted autoplay loop style="border-radius: 8px;"></video>
</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/bb706417-e513-4419-8a42-dc914a6f5efd" style="width: 23.5%; border-radius: 4px; border: 1px solid #ddd;" />
  <img src="https://github.com/user-attachments/assets/01662e18-e55d-4e0c-84a3-819557174836" style="width: 23.5%; border-radius: 4px; border: 1px solid #ddd;" />
  <img src="https://github.com/user-attachments/assets/09c7b762-5603-4018-94e0-f38140d7acb8" style="width: 23.5%; border-radius: 4px; border: 1px solid #ddd;" />
  <img src="https://github.com/user-attachments/assets/8dadc406-c909-48b6-9d7e-c1566b387e6e" style="width: 23.5%; border-radius: 4px; border: 1px solid #ddd;" />
</p>

<br>



更多 `pro` 的模式定位与实现说明见：
- [`docs/design/modes/pro.md`](docs/design/modes/pro.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

### `16-FSK` 频点映射 (`ultra`)
`16-FSK` 频点映射 (`ultra`) 是面向 UTF-8 文本的更高密度模式：输入文本直接按 UTF-8 byte 处理，每个 byte 拆成两个 nibble，再映射到 clean `16-FSK` 的固定频点，因此 `1 byte = 2 symbol`，但每个 symbol 只发送一个频点。它偏向更高的信息密度和更正式的 UTF-8 文本传输。

相比逐 bit BFSK / FSK (`flash`)，`16-FSK` (`ultra`) 的优势不只体现在相同输入下生成的音频更短，也体现在生成和解析路径更轻。一次 7000 chars / 7000 bytes 文本的参考测试中，逐 bit BFSK / FSK (`flash`) 生成耗时约 1 分 56 秒，生成音频约 43 分钟；`16-FSK` (`ultra`) 生成约 2 秒，音频约 11 分 40 秒。具体数值会随设备、style 和参数变化，但这个量级差异体现了两种模式的定位区别：`flash` 侧重可听、可解释和情绪化表达，`ultra` 侧重更高吞吐和更快处理。





<p align="center">
  <video src="https://github.com/user-attachments/assets/ce165029-40b1-4445-8407-8887672b918c" width="700" controls muted autoplay loop style="border-radius: 8px;"></video>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/7f53b589-635b-436e-b8d2-a2e719bea804" alt="1" style="width: 31%; border-radius: 4px; border: 1px solid #ddd;" />
  <img src="https://github.com/user-attachments/assets/3b583cc8-35ee-46a8-946f-4d681095b67a" alt="2" style="width: 31%; border-radius: 4px; border: 1px solid #ddd;" />
  <img src="https://github.com/user-attachments/assets/35866d3a-d593-4089-90e3-e560b2dea644" alt="3" style="width: 31%; border-radius: 4px; border: 1px solid #ddd;" />
</p>

<br>



更多 `ultra` 的模式定位与实现说明见：
- [`docs/design/modes/ultra.md`](docs/design/modes/ultra.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

`ultra` 模式在实现思路上参考了 [ggerganov/ggwave](https://github.com/ggerganov/ggwave) 的公开工程设计与声学传输实践，特此致谢。  
本项目为独立实现；除仓库中已明确标注的第三方组件外，不主张与该项目存在隶属、背书或官方关系。

---

## 使用边界与责任说明 (Usage & Liability)

### 图标使用说明（公共资源）
FlipBits 图标资源（包括源文件与组件 SVG 文件）作为公共资源向社区开放使用。

在遵守本仓库许可证与适用法律的前提下，你可以将这些图标用于勋章、二创、视频展示与商业发布（包括售卖），无需向项目方额外申请单独授权。

若用于公开传播，建议标注：
`Icon designed by FlipBits Project`

限制与边界（法律保守版）：
- 本授权仅覆盖图标资源本身，不授予商标权、专利权、人格权或任何未明确授予的权利。
- 图标按“现状”提供，不附带任何明示或默示担保；使用者应自行承担合规与风险责任。
- 不得将这些图标资源私有化、独占声明，或再授权为专有/排他性资源。
- 不得以任何方式暗示你对 FlipBits 项目或作者拥有官方代表、背书或唯一授权关系。

### 1. 原理与限制 (Principles & Limitations)
* **协议公开透明**：本工具使用的传输方式属于**公开、通用的声学编码方法**，包括高低频切换、双音映射和多频点映射等形式。其本质是对文本字节或符号进行有序编码，不提供绕过安全审查的加密或隐写（Steganography）能力。
* **用途定位明确**：本项目用于**音频信号处理（DSP）研究、声学通信原理验证，以及相关编解码性能实验**，并非为隐蔽通信场景设计。
* **娱乐表达优先**：部分模式会刻意保留冗长、低速和强风格化的音频外观，以服务整体氛围表达；效率不是所有模式的首要目标。
* **不承诺现实环境鲁棒性**：当前重点是“生成音频 -> 解析生成音频”的主链路闭环，不承诺在真实播放、录音、噪声、回声、削波、设备频响偏差或远距离传播条件下的稳定接收表现。
* **使用责任自负**：开发者提供的是源代码与实现方法。用户在运行、修改或部署本项目时，应自行确保其用途符合所在地法律法规、平台规则与网络安全要求。
* **原样分发 (As-Is)**：本软件按现状提供。在适用法律允许的范围内，作者不对其适用性、稳定性或因使用本软件造成的损失承担赔偿责任。

### 2. 风格与知识产权 (Style & IP)
* **独立开源项目**：本项目为独立开发的非官方开源项目，不隶属于任何影视、游戏或商业品牌，也不代表任何第三方立场。
* **无关联声明**：本项目为独立原创作品，与 Games Workshop 及 Warhammer 40,000 无任何隶属、授权、赞助、认可或其他关联关系。
* **风格来源说明**：项目在视觉与文案气质上参考了复古未来主义、工业美学、宗教式仪式感表达等通用创作方向，但不使用任何受保护世界观中的专有设定作为项目基础。
* **内容处理原则**：若仓库中出现可能引发混淆、侵权或不当联想的素材、命名或表述，欢迎通过 [GitHub Issues](../../issues) 提出，我们会及时评估与修正。

---

## 快速开始

> 若发现仓库中存在不准确、过度风格化或可能引发误解的内容，欢迎通过 [GitHub Issues](../../issues) 反馈。

若你是 AI / agent，建议先阅读 [`.agent/AGENTS.md`](.agent/AGENTS.md) 以及对应子系统下的 `AGENTS.md`，用来快速理解仓库结构、工具入口与修改约定。

### Android
- Android 官方工程入口在 `C:\code\FlipBits\apps\audio_android`。
- 从仓库根目录统一执行：
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py android assemble-release`
  - `python tools/run.py android native-debug`
- `apps/audio_android` 是 Android Gradle root，`apps/audio_android/app` 是实际应用模块。
- Android Studio 建议直接打开 `apps/audio_android`。

### 本地编排工具
- 推荐统一使用 `python tools/run.py <command>`。
- 常用命令：
  - `python tools/run.py build --build-dir build/dev`
  - `python tools/run.py clean`
  - `python tools/run.py verify --build-dir build/dev --skip-android`
  - `python tools/run.py android native-debug`
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py artifact export-apk`
- 约定：
  - `python tools/run.py --help` 只看主命令概览；详细参数用 `python tools/run.py <command> --help`。
  - host 根目录当前直接固定为一条正式主线：`clang++ + Ninja + build/dev`。
  - `python tools/run.py verify --build-dir build/dev --skip-android` 只验证 host 默认 modules 主路径。
  - Android native 侧通过 `apps/audio_android/native_package -> bag_android_native` 独立装配；剩余 `C++17` 例外被限制在 package-private wrapper 与 `android_bag/**` 私有声明层。
  - `build/` 继续保留给 CMake / Gradle 的原生构建输出与测试产物。
  - 根目录 `dist/` 只存放 Python 导出的最终交付物；当前 Android APK 默认导出到 `dist/android/`。

### 开发导航
按模块阅读或修改时，可优先从以下入口进入：

- agent / AI 总入口：[`.agent/AGENTS.md`](.agent/AGENTS.md)
- 核心库与共享业务逻辑：[`libs/AGENTS.md`](libs/AGENTS.md)
- CLI 表现层：[`apps/audio_cli/AGENTS.md`](apps/audio_cli/AGENTS.md)
- Android 应用：[`apps/audio_android/AGENTS.md`](apps/audio_android/AGENTS.md)

更多仓库结构与工具说明见：
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)
- [`tools/README.md`](tools/README.md)
