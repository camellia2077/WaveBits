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
[![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg)]()
[![CI Android Assemble](https://github.com/camellia2077/WaveBits/actions/workflows/ci-android-assemble.yml/badge.svg)](https://github.com/camellia2077/WaveBits/actions/workflows/ci-android-assemble.yml)
[![CI Android Quality](https://github.com/camellia2077/WaveBits/actions/workflows/ci-android-quality.yml/badge.svg)](https://github.com/camellia2077/WaveBits/actions/workflows/ci-android-quality.yml)
[![CI Host Verify](https://github.com/camellia2077/WaveBits/actions/workflows/ci-host-verify.yml/badge.svg)](https://github.com/camellia2077/WaveBits/actions/workflows/ci-host-verify.yml)

## Project Overview
FlipBits is an experimental audio project built around retro-futurist communication aesthetics. It encodes text into audible signal patterns and decodes those patterns back into text, combining DSP experimentation with a deliberately stylized industrial-tech presentation.

The project provides multiple transport modes that map text into waveform structures through different frequency organizations, then recover text from those waveforms. It does not provide cryptographic encryption.

- **Core modes**: the project currently includes `flash`, `pro`, `ultra`, and `mini`.
- **Mode positioning**: `flash` favors ritualistic atmosphere and stylized presentation, `pro` favors a cleaner and more structured signaling path, `ultra` favors denser mapping for UTF-8 text, and `mini` favors clear Morse code dot/dash rhythm.
- **Design intent**: the goal is not just to transmit content, but to make the generated audio feel deliberate, dramatic, and technologically evocative.
- **Entertainment-first focus**: some modes intentionally sacrifice efficiency in exchange for stronger atmosphere and a more distinctive listening experience.

## Naming Map
For fast orientation, the project-specific mode names roughly map to these more familiar technical categories:

- `mini` -> Morse code
- `flash` -> BFSK / bit-by-bit FSK-style signaling
- `pro` -> DTMF-like dual-tone mapping
- `ultra` -> `16-FSK` frequency mapping

These names are product-facing labels, not a simple ladder of "basic to advanced" versions of the same protocol. Each mode emphasizes a different listening character, expressive goal, and transport structure.

## Design Boundary
The current project focus is the controlled loop of "text -> stylized audio -> in-project decoding", with particular emphasis on Android app workflows for audio generation, conversion, sharing, and export.

It is not primarily designed around real-time over-speaker playback decoding by another device, nor around noise robustness, echo resistance, far-field reception, or complex real-world synchronization. In this project, atmosphere, recognizable stylistic expression, and controlled mode behavior take priority over real-world acoustic communication robustness.

## Android App Footprint
The Android app is intentionally lightweight and native-leaning, with fast cold start behavior and a small package footprint for audio generation, conversion, sharing, and export.

In the current reference build, the install package is about `5.5 MB`, and the installed size is about `5.9 MB`. These numbers may change across versions, ABIs, and build configurations.

## Modes

### `flash`
`flash` is intentionally the most stylized mode. It uses two high / low Hz states to represent bits, then shapes bit duration, frequency choices, and pause spacing to simulate a more human-like emotional tone and stylized delivery. The same input text may produce audio close to a minute long in `flash`, while `ultra` may finish in only a few seconds. That gap is intentional: the project values the feeling of "being played like a ritual signal" more than raw throughput.

`flash` currently provides six styles. Each style uses a low / high Hz pair to define bit states, then combines bit duration, frequency shaping, and pause spacing to create a distinct emotional "speaking tone":

| Style | Low / High | Listening target |
| --- | --- | --- |
| [Litany](docs/design/modes/flash/litany.md) | `220 / 440 Hz` | low, solemn, chant-like |
| [Collapse](docs/design/modes/flash/collapse.md) | `280 / 560 Hz` | hushed, panicked, stuttering |
| [Steady](docs/design/modes/flash/steady.md) | `300 / 600 Hz` | everyday, precise, stable |
| [Hostile](docs/design/modes/flash/hostile.md) | `450 / 900 Hz` | sharp, urgent, aggressive |
| [Zeal](docs/design/modes/flash/zeal.md) | variable `560-900 / 1120-1800 Hz` | bright, variable-speed, dense |
| [Void](docs/design/modes/flash/void.md) | `240 / 480 Hz` | low, trailing, sparse |

For deeper `flash` voicing-style semantics, emotional intent, and preset design notes, see:
- [`docs/design/modes/flash/README.md`](docs/design/modes/flash/README.md)
- [`docs/design/modes/flash/voicing-emotions.md`](docs/design/modes/flash/voicing-emotions.md)
- [`docs/design/modes/flash/`](docs/design/modes/flash/)

### `mini`
`mini` is the Morse code mode. Input is normalized through Morse-compatible text rules, emphasizing clear rhythm, visual readability, and follow-along playback. The current speed presets are:

| Speed | Role |
| --- | --- |
| Slow | slower and easier to inspect in dot/dash visuals and lyrics follow |
| Standard | default Morse rhythm |
| Fast | shorter, more compact Morse output |

For deeper `mini` rules, follow/visual behavior, and implementation notes, see:
- [`docs/design/modes/mini.md`](docs/design/modes/mini.md)

### `pro`
`pro` is the more formal ASCII-only mode: input text is first converted into ASCII bytes, then each byte is split into a high nibble and a low nibble, each of which maps to a `DTMF-like` dual-tone symbol, so `1 byte = 2 symbol`. It favors a cleaner and more regular acoustic signaling structure.

For deeper `pro` mode positioning and implementation notes, see:
- [`docs/design/modes/pro.md`](docs/design/modes/pro.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

### `ultra`
`ultra` is the denser UTF-8-oriented mode: input text is processed directly as UTF-8 bytes, each byte is split into two nibbles, and each nibble maps to a fixed frequency in clean `16-FSK`, so `1 byte = 2 symbol`, with each symbol emitting only one frequency. It favors higher information density and a more formal UTF-8 text transport path.

For deeper `ultra` mode positioning and implementation notes, see:
- [`docs/design/modes/ultra.md`](docs/design/modes/ultra.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

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
- **No affiliation statement**: this project is an original independent work and is not affiliated with, endorsed by, sponsored by, licensed by, or otherwise associated with Games Workshop or Warhammer 40,000.
- **Aesthetic references only**: the project's visual and writing style draws from retro-futurism, industrial aesthetics, and ritualized presentation as broad creative influences, without relying on protected setting-specific terminology as its foundation.
- **Content correction policy**: if any asset, naming, or wording in the repository feels misleading, inappropriate, or legally risky, please report it through [GitHub Issues](../../issues).

---

## Quick Start

> If you notice wording, assets, or stylistic material in this repository that feels inaccurate or potentially misleading, please report it through [GitHub Issues](../../issues).

If you are an AI / agent, start with [`.agent/AGENTS.md`](.agent/AGENTS.md) and then read the relevant subsystem `AGENTS.md` files for a faster overview of repository structure, tooling entry points, and editing conventions.

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

- Agent / AI entry point: [`.agent/AGENTS.md`](.agent/AGENTS.md)
- Core libraries and shared logic: [`libs/AGENTS.md`](libs/AGENTS.md)
- CLI presentation layer: [`apps/audio_cli/AGENTS.md`](apps/audio_cli/AGENTS.md)
- Android application: [`apps/audio_android/AGENTS.md`](apps/audio_android/AGENTS.md)

For broader repository structure and tooling details, see:
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)
- [`tools/README.md`](tools/README.md)
