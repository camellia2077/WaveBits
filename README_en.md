<p align="center">
  <img src="ui/app/icon-foreground.svg" alt="FlipBits icon" width="128" />
</p>

<h1 align="center">FlipBits</h1>

<p align="center">
  <strong>An entertainment-first text-to-audio signaling toolkit for generating retro-tech atmospheric audio</strong><br />
  <em>Stylized acoustic text encoding with a strong science-fiction mood</em>
</p>

<p align="center">
  <a href="README.md">中文</a> | English
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-red.svg)](LICENSE)
[![Protocol](https://img.shields.io/badge/Protocol-Multi--Mode%20Audio%20Signaling-gold.svg)]()

> **"Language can be written. Logic can also be heard."**

## Project Overview
FlipBits is an experimental audio project built around retro-futurist communication aesthetics. It encodes text into audible signal patterns and decodes those patterns back into text, combining DSP experimentation with a deliberately stylized industrial-tech presentation.

The project provides multiple transport modes that map text into waveform structures through different frequency organizations, then recover text from those waveforms. It does not provide cryptographic encryption.

- **Core modes**: the project currently includes `flash`, `pro`, and `ultra`.
- **Mode positioning**: `flash` favors ritualistic atmosphere and stylized presentation, `pro` favors a cleaner and more structured signaling path, and `ultra` favors denser mapping for UTF-8 text.
- **Design intent**: the goal is not just to transmit content, but to make the generated audio feel deliberate, dramatic, and technologically evocative.
- **Entertainment-first focus**: some modes intentionally sacrifice efficiency in exchange for stronger atmosphere and a more distinctive listening experience.

Mode summary:
- `flash`: transmits bit-by-bit through high / low frequency switching, emphasizing atmosphere, audibility, and stylized presentation.
- `pro`: uses dual-tone combinations for each nibble, with a more regular structure suited to a more formal signaling path.
- `ultra`: uses `16-FSK` frequency mapping for nibbles, targeting UTF-8 text with higher information density.

`flash` is intentionally the least efficient mode. The same input text may produce audio close to a minute long in `flash`, while `ultra` may finish in only a few seconds. That gap is intentional: the project values the feeling of "being played like a ritual signal" more than raw throughput.

---

## Usage & Liability

### 1. Principles & Limitations
- **Transparent signaling methods**: the project uses open and conventional acoustic signaling methods, including high/low frequency switching, dual-tone mapping, and multi-frequency mapping. These are structured encoding schemes, not encryption or steganography.
- **Clear scope**: this project is intended for DSP experimentation, acoustic signaling studies, and encode/decode performance exploration. It is not designed for covert communication.
- **Entertainment over efficiency**: some modes intentionally preserve long duration, low speed, and heavy stylistic coloration as part of the intended experience.
- **No real-world robustness guarantee**: the current focus is the controlled loop of "generate audio -> decode generated audio". The project does not guarantee reliable decoding under real playback, recording, noise, echo, clipping, frequency-response shifts, or distance propagation conditions.
- **Use at your own responsibility**: users are responsible for ensuring that any use, modification, or deployment complies with local law, platform rules, and network-security requirements.
- **As-Is distribution**: this software is provided as-is. To the maximum extent permitted by law, the authors disclaim liability for suitability, stability, or losses resulting from its use.

### 2. Style & IP
- **Independent open-source project**: this is an independent, unofficial open-source project and is not affiliated with any film, game, or commercial brand.
- **Aesthetic references only**: the project's visual and writing style draws from retro-futurism, industrial aesthetics, and ritualized presentation as broad creative influences, without relying on protected setting-specific terminology as its foundation.
- **Content correction policy**: if any asset, naming, or wording in the repository feels misleading, inappropriate, or legally risky, please report it through [GitHub Issues](../../issues).

---

## Quick Start

> If you notice wording, assets, or stylistic material in this repository that feels inaccurate or potentially misleading, please report it through [GitHub Issues](../../issues).

## Development Entry

### Android
- The official Android project root is `C:\code\WaveBits\apps\audio_android`.
- Run these commands from the repository root:
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py android assemble-release`
  - `python tools/run.py android native-debug`
- `apps/audio_android` is the Android Gradle root, and `apps/audio_android/app` is the actual app module.
- In Android Studio, open `apps/audio_android` directly.

### Local Tooling
- Prefer using `python tools/run.py <command>` as the unified entry point.
- Common commands:
  - `python tools/run.py build --build-dir build/dev`
  - `python tools/run.py clean`
  - `python tools/run.py verify --build-dir build/dev --skip-android`
  - `python tools/run.py android native-debug`
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py artifact export-apk`
- Notes:
  - Use `python tools/run.py --help` for the top-level overview; use `python tools/run.py <command> --help` for detailed arguments.
  - The host-side default path is currently the mainline `clang++ + Ninja + build/dev`.
  - `python tools/run.py verify --build-dir build/dev --skip-android` validates the default host module path only.
  - The Android native side is assembled through `apps/audio_android/native_package -> bag_android_native`; the remaining `C++17` exceptions are restricted to the package-private wrapper layer and `android_bag/**`.
  - `build/` remains the home for native CMake / Gradle build outputs and test artifacts.
  - Root-level `dist/` is reserved for Python-exported deliverables; Android APKs are currently exported to `dist/android/`.

### Development Navigation
When reading or modifying the codebase by area, these are good starting points:

- Core libraries and shared logic: [`libs/AGENTS.md`](libs/AGENTS.md)
- CLI presentation layer: [`apps/audio_cli/AGENTS.md`](apps/audio_cli/AGENTS.md)
- Android application: [`apps/audio_android/AGENTS.md`](apps/audio_android/AGENTS.md)

For broader repository structure and tooling details, see:
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)
- [`tools/README.md`](tools/README.md)
