<p align="center">
  <img src="ui/app/icon-foreground.svg" alt="FlipBits icon" width="128" />
</p>

<h1 align="center">FlipBits</h1>

<p align="center">
  <strong>面向娱乐表达的文本声学编码工具，生成具有科技氛围感的转换音频</strong><br />
  <em>Entertainment-first text-to-audio signaling with retro-tech atmosphere</em>
</p>

<p align="center">
  中文 | <a href="README_en.md">English</a>
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-red.svg)](LICENSE)
[![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg)]()
[![CI Android Assemble](https://github.com/camellia2077/WaveBits/actions/workflows/ci-android-assemble.yml/badge.svg)](https://github.com/camellia2077/WaveBits/actions/workflows/ci-android-assemble.yml)
[![CI Android Quality](https://github.com/camellia2077/WaveBits/actions/workflows/ci-android-quality.yml/badge.svg)](https://github.com/camellia2077/WaveBits/actions/workflows/ci-android-quality.yml)
[![CI Host Verify](https://github.com/camellia2077/WaveBits/actions/workflows/ci-host-verify.yml/badge.svg)](https://github.com/camellia2077/WaveBits/actions/workflows/ci-host-verify.yml)

## 🛠️ 项目定位 (Project Overview)
本项目是一个面向复古未来主义通信体验的实验性音频工具，用于在文本与可听音频信号之间进行多模式编码与解码。它聚焦于数字信号处理（DSP）实验、声学调制解调验证，以及带有工业风格气质的人机交互表达。

它提供多种传输模式，用不同的频率组织方式把文本内容映射为波形，或从波形中还原文本；项目本身不提供任何形式的密码学加密。
* **技术核心**：当前包含基于高低频切换的 `flash`、基于双音映射的 `pro`、基于 `16-FSK` 的 `ultra`，以及基于 Morse code 的 `mini` 四类模式。
* **模式定位**：`flash` 偏娱乐化与仪式感表达，`pro` 偏结构清晰的正式链路，`ultra` 偏面向 UTF-8 文本的高密度频点映射，`mini` 偏点划节奏清晰的 Morse code 表达。
* **设计理念**：以现代处理器的性能余量，稳定生成低速、可辨识、风格明确的声学编码体验，在工程实现与风格化表达之间取得平衡。
* **项目意图**：这是一个以娱乐性、氛围感和风格化表达为优先目标的项目。某些模式会刻意牺牲效率来换取更强的听感特征与仪式感，而不是追求最短时长或最高吞吐。

## 命名映射
为方便快速理解，可先把项目内部命名对应到更常见的技术分类：

- `mini` -> Morse code
- `flash` -> BFSK / 逐 bit FSK 风格
- `pro` -> DTMF-like 双音映射
- `ultra` -> `16-FSK` 频点映射

这些名字不是“强弱等级”，而是项目内部的产品化命名。它们分别强调不同的听感、表达气质和传输结构，而不是同一协议从基础版到高级版的线性升级关系。

## 设计边界
本项目当前重点是“文本 -> 风格化音频 -> 项目内解码”的受控闭环，尤其强调 Android app 内的音频生成、转换、分享与导出体验。

它不以“外放后被另一设备直接实时解析”为主要交互目标，也不以真实环境下的抗噪、抗回声、远场接收或复杂同步鲁棒性为设计优先级。对本项目来说，氛围感、可辨识的风格表达和可控的模式体验，优先于现实声学环境中的通信稳健性。

## Android App 特性
Android app 当前保持轻量原生取向：冷启动速度快，包体控制较小，适合直接生成、转换、分享与导出音频。

当前参考构建下，安装包约 `5.5 MB`，安装后占用约 `5.9 MB`。这些数字会随版本、ABI 与构建配置变化。

## 模式说明

### `flash`
`flash` 是最偏风格化的模式：它用高频 / 低频两种 Hz 作为 bit 状态，再通过调整 bit 持续时间、频率配置与停顿间隔，模拟出更接近人类说话情绪与语气的风格化表达。相同文本在 `flash` 下可能生成接近一分钟的音频，而在 `ultra` 下只需几秒。这种差异是有意设计的，目标是保留“像仪式一样被播放出来”的质感，而不是单纯压缩传输时间。

`flash` 当前提供六种 style。每种 style 都以 low / high 两种 Hz 决定 bit 状态，并通过 bit 持续时间、频率组织与停顿间隔的组合，塑造出不同的情绪化“说话语气”：

| Style | Low / High | 听感目标 |
| --- | --- | --- |
| [Litany](docs/design/modes/flash/litany.md) | `220 / 440 Hz` | 低沉、肃穆、吟诵 |
| [Collapse](docs/design/modes/flash/collapse.md) | `280 / 560 Hz` | 低声、慌张、结巴 |
| [Steady](docs/design/modes/flash/steady.md) | `300 / 600 Hz` | 日常、精确、平稳 |
| [Hostile](docs/design/modes/flash/hostile.md) | `450 / 900 Hz` | 尖锐、急促、攻击 |
| [Zeal](docs/design/modes/flash/zeal.md) | variable `560-900 / 1120-1800 Hz` | 明亮、变速、密集 |
| [Void](docs/design/modes/flash/void.md) | `240 / 480 Hz` | 低沉、拖尾、稀疏 |

更多 `flash` voicing style 的情绪定位、命名语义与 preset 设计见：
- [`docs/design/modes/flash/README.md`](docs/design/modes/flash/README.md)
- [`docs/design/modes/flash/voicing-emotions.md`](docs/design/modes/flash/voicing-emotions.md)
- [`docs/design/modes/flash/`](docs/design/modes/flash/)

### `mini`
`mini` 是 Morse code 模式，输入会按 Morse 规则规范化，强调清晰、可视化友好和点划节奏；当前提供三种 speed preset：

| Speed | 定位 |
| --- | --- |
| Slow | 更慢、更适合观察 dot / dash 和 lyrics follow |
| Standard | 默认 Morse 节奏 |
| Fast | 更短、更紧凑的 Morse 输出 |

更多 `mini` 的输入规范、follow / visual 与实现说明见：
- [`docs/design/modes/mini.md`](docs/design/modes/mini.md)

### `pro`
`pro` 是更正式的 ASCII-only 模式：输入文本先转成 ASCII byte，再把每个 byte 拆成高 4 bit / 低 4 bit，并分别映射为 `DTMF-like` 双音 symbol，因此 `1 byte = 2 symbol`。它偏向结构清晰、职责单纯的正式声学链路表达。

更多 `pro` 的模式定位与实现说明见：
- [`docs/design/modes/pro.md`](docs/design/modes/pro.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

### `ultra`
`ultra` 是面向 UTF-8 文本的更高密度模式：输入文本直接按 UTF-8 byte 处理，每个 byte 拆成两个 nibble，再映射到 clean `16-FSK` 的固定频点，因此 `1 byte = 2 symbol`，但每个 symbol 只发送一个频点。它偏向更高的信息密度和更正式的 UTF-8 文本传输。

更多 `ultra` 的模式定位与实现说明见：
- [`docs/design/modes/ultra.md`](docs/design/modes/ultra.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

---

## 使用边界与责任说明 (Usage & Liability)

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
- Android 官方工程入口在 `C:\code\WaveBits\apps\audio_android`。
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
