# ⚙️ WaveBits

**基于 C++ 核心的高性能二进制音频调制解调工具 (High-Performance Binary-to-Audio Transceiver)**

[![License](https://img.shields.io/badge/License-Apache%202.0-red.svg)](LICENSE)
[![Protocol](https://img.shields.io/badge/Protocol-16--FSK%20/跨平台-gold.svg)]()

> **“肉体苦弱，逻辑永恒。凡人以言语互通，贤者以圣歌共鸣。”**

## 🛠️ 项目定位 (Project Sanctity)
本项目是为模拟复古未来主义 (Retro-futurism) 通信效果而设计的实验性音效工具。它将自然语言的文字圣化为二进制音频脉冲（Binary Cant），旨在探索数字信号处理（DSP）与工业哥特美学的结合。

仅负责将逻辑状态转化为波形，未包含任何形式的密码学加密。
* **技术核心**：采用 16-FSK 与经典 DTMF 算法。
* **工程理念**：以逻辑之高效率，行祭礼之低速率。我们调动现代处理器的巅峰性能，仅为确保每一枚比特都能以最圣洁、最冗余的声学形态被吟唱出来，从而复现科幻语境中的仪式感。

---

## ⚖️ 逻辑契约与责任界定 (Covenants & Liability)

### 1. 圣化准则与因果限制 (Principles & Limitations)
* **逻辑透明性**：本工具所使用的调制算法均为**公开、通用的通信协议**。其本质是逻辑的有序排列，不具备任何绕过安全审查的加密或隐写（Steganography）功能。
* **机魂之谕**：开发者仅提供逻辑的初态（源代码）。用户在唤醒机魂（运行程序）进行信号转换时，有责任确保其行为符合所在地法律及网络安全准则。
* **圣职定位**：本项目仅作为**音频信号处理（DSP）算法研究与声学通信原理验证工具**。所有功能仅为展示 C++ 在 NDK 环境下的编解码性能，而非设计用于隐秘通联。
* **因果自担**：开发者无法预知亦无法控制逻辑的最终流向。若用户利用此圣歌从事非法行为（如非法窃听、散布违禁信息等），其引发的亚空间回响（法律后果）由用户自行承担。
* **原样分发 (As-Is)**：本软件按逻辑原样分发。在最大法律限度内，作者不对软件的适用性、稳定性或因逻辑运行导致的数据损失承担赔偿责任。

### 2. 知识产权与异端排除 (IP Disclaimer)
* **非关联声明**：本项目为独立开发的开源工具，与任何商业化桌面游戏、特定的虚构世界观品牌或其母公司无任何关联，亦未获得其授权。
* **美学溯源**：其术语灵感来源于广义的超人类主义（Transhumanism）及工业哥特（Industrial Gothic）文学流派，属于纯粹的同人创作表达。

---

## 🚀 快速开始

> **机魂指引**：若发现本项目代码库中存在任何违背逻辑纯净性或不当的内容，请通过 [GitHub Issues] 联系处理。

## 🧭 开发入口

### Android
- Android 官方工程入口在仓库根目录 `C:\code\WaveBits`。
- 直接从根目录执行：
  - Windows：`.\gradlew.bat :app:assembleDebug`
  - macOS/Linux：`./gradlew :app:assembleDebug`
- `apps/audio_android/app` 只保留 Android 模块源码、JNI 与资源；不再作为独立 `Gradle` root。
- Android Studio 建议直接打开仓库根目录，而不是单独打开 `apps/audio_android`。

### 本地编排工具
- 推荐统一使用 `python tools/run.py <command>`。
- 常用命令：
  - `python tools/run.py build --build-dir build/dev`
  - `python tools/run.py verify --build-dir build/dev --skip-android`
  - `python tools/run.py android native-debug`
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py export-apk`
- 约定：
  - host 根目录 CMake 只支持 `WAVEBITS_HOST_MODULES=ON`；root host `OFF` 已退休。
  - `python tools/run.py verify --build-dir build/dev --skip-android` 只验证 host 默认 modules 主路径。
  - Android native 侧通过 `apps/audio_android/native_package -> bag_android_native` 独立装配；剩余 `C++17` 例外被限制在 package-private wrapper 与 `android_bag/**` 私有声明层。
  - `build/` 继续保留给 CMake / Gradle 的原生构建输出与测试产物。
  - 根目录 `dist/` 只存放 Python 导出的最终交付物；当前 Android APK 默认导出到 `dist/android/`。

更多仓库结构与入口说明见：
- `docs/architecture/repo-map.md`
- `tools/README.md`
