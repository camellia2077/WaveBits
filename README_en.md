<p align="center">
  <img src="ui/app/icon-foreground.svg" alt="FlipBits icon" width="128" />
</p>

<p align="center">
  <em>Icon designed by camellia2077 (FlipBits Project)</em>
</p>

<h1 align="center">FlipBits</h1>

<p align="center">
  <a href="README.md">中文</a> | English
</p>

<p align="center">
  <strong>A stylized text/audio signaling and decoding toolkit that uses FSK rhythm, tone, and pause to make encoded audio feel spoken</strong><br />
  <em>Text-to-audio signaling with emotional FSK pacing, visual follow, and readable encoding structure</em>
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

## Quick Overview
- Native Android app for the visualization and sonification of text encoding.
- Supports Morse code (`mini`), bit-by-bit BFSK / FSK (`flash`), DTMF-like dual-tone mapping (`pro`), and `16-FSK` (`ultra`).
Offers "Visual" and "Lyrics" modes for inspecting audio signal and text encoding layers.
- Localized UI: English, German, Spanish, French, Italian, Japanese, Korean, Polish, Brazilian Portuguese, Russian, Ukrainian, Simplified Chinese, Traditional Chinese, and dog Latin for a solemn, liturgical, techno-futurist space-opera atmosphere.

## Download / Install
Android APKs will be published through GitHub Releases.

In the current reference build, the install package is about `5.64 MB`, and the installed size is about `6.10 MB`. These numbers may change across versions, ABIs, and build configurations.

## Mode Overview
| Mode | Technical category | Best for |
| --- | --- | --- |
| `mini` | Morse code | short, clear, readable dot/dash rhythm |
| `flash` | bit-by-bit BFSK / FSK | expressive audio and Visual/Lyrics learning |
| `pro` | DTMF-like dual-tone mapping | compact dual-tone structure |
| `ultra` | `16-FSK` frequency mapping | shorter audio, faster generation and decoding |

These names are product-facing labels, not a simple ladder of "basic to advanced" versions of the same protocol. Each mode emphasizes a different listening character, expressive goal, and transport structure.

## Project Overview
FlipBits is a toolkit for encoding text into audible signal patterns and decoding those generated patterns back into text. It does more than convert text into sound: in its bit-by-bit FSK path, it shapes bit duration, pause spacing, frequency choices, and playback rhythm so generated audio can resemble different emotional tones or speaking styles.

It maps text into waveform structures and can recover text from project-generated waveforms. It does not provide cryptographic encryption.

- **Expressive focus**: bit-by-bit BFSK / FSK (`flash`) intentionally sacrifices encoding efficiency, using longer bits, pauses, and frequency changes to create stronger emotional tone and ritual-like listening character.
- **Efficient alternatives**: if shorter audio or more formal transport is needed, Morse code (`mini`), DTMF-like dual-tone mapping (`pro`), and `16-FSK` (`ultra`) provide more compact paths. `16-FSK` (`ultra`) is not only shorter than bit-by-bit BFSK / FSK (`flash`) for the same input; generation cost and decode time are usually much lower as well. Speed is available, but it is not the only goal.
- **Visual learning value**: Android provides two complementary follow views. Visual focuses on the signal layer, showing how encoded text becomes FSK low/high bits, tone segments, and playback timing. Lyrics focuses on the text-encoding layer, using tokens to show how text becomes UTF-8 bytes, hex/bin, and bits, then highlighting them during playback.

## Android App Footprint
The Android app is intentionally lightweight and native-leaning, with fast cold start behavior and a small package footprint for audio generation, conversion, sharing, and export.

## Design Boundary
The current project focus is the controlled loop of "text -> stylized audio -> in-project decoding", with particular emphasis on Android app workflows for audio generation, conversion, sharing, and export.

It is not primarily designed around real-time over-speaker playback decoding by another device, nor around noise robustness, echo resistance, far-field reception, or complex real-world synchronization. In this project, atmosphere, recognizable stylistic expression, and controlled mode behavior take priority over real-world acoustic communication robustness.

## Modes

### Bit-by-bit BFSK / FSK (`flash`)
Bit-by-bit BFSK / FSK (`flash`) is intentionally the most stylized mode. It uses two high / low Hz states to represent bits, then shapes bit duration, frequency choices, and pause spacing to simulate a more human-like emotional tone and stylized delivery. The same input text may produce audio close to a minute long in bit-by-bit BFSK / FSK (`flash`), while `16-FSK` (`ultra`) may finish in only a few seconds. That gap is intentional: the project values the feeling of "being played like a ritual signal" more than raw throughput.

The feel of bit-by-bit BFSK / FSK (`flash`) comes from the binary nature of the signal itself: each bit switches between only two frequency states, low and high, similar to 0 and 1 in binary. Instead of sounding like continuous natural speech, it turns text into an audible chain of low/high pulses, giving the audio a mechanical, ritual-like communication character.

The low efficiency of bit-by-bit BFSK / FSK (`flash`) is intentional. It does not encode multiple bits into one simultaneous symbol; low and high tones appear one after another in bit order. Style-specific bit duration, pauses, and frequency changes make the audio much longer. The goal is not throughput, but audibility and explainability: a listener can use the sound, Visual, and Lyrics views together to understand or even write down the corresponding low/high bit sequence.

Bit-by-bit BFSK / FSK (`flash`) currently provides six styles. Each style uses a low / high Hz pair to define bit states, then combines bit duration, frequency shaping, and pause spacing to create a distinct emotional "speaking tone":



<p align="center">
  <video src="https://github.com/user-attachments/assets/e9f2c2bb-0e1c-4a81-a5d2-9f8872324b2d" width="400" controls muted autoplay loop style="border-radius: 8px;"></video>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/5b1f1b40-a4e2-4891-9800-8052f304baba" width="180" title="1" />
  <img src="https://github.com/user-attachments/assets/1eee2bb5-d696-48a9-94b5-dbf72c2442f9" width="180" title="2" />
  <img src="https://github.com/user-attachments/assets/c9d593f5-a330-4f7a-9f56-eb823abdf1c5" width="180" title="3" />
  <img src="https://github.com/user-attachments/assets/7bd93db0-b2b5-4c85-83eb-299dde7bd4b1" width="180" title="4" />
</p>


| Style | Low / High | Listening target |
| --- | --- | --- |
| [Litany](docs/design/modes/flash/litany.md) | `220 / 440 Hz` | low, solemn, chant-like |
| [Collapse](docs/design/modes/flash/collapse.md) | `280 / 560 Hz` | hushed, panicked, stuttering |
| [Standard](docs/design/modes/flash/standard.md) | `300 / 600 Hz` | everyday, precise, stable |
| [Hostile](docs/design/modes/flash/hostile.md) | `450 / 900 Hz` | sharp, urgent, aggressive |
| [Zeal](docs/design/modes/flash/zeal.md) | variable `560-900 / 1120-1800 Hz` | bright, variable-speed, dense |
| [Void](docs/design/modes/flash/void.md) | `240 / 480 Hz` | low, trailing, sparse |

For deeper `flash` voicing-style semantics, emotional intent, and preset design notes, see:
- [`docs/design/modes/flash/README.md`](docs/design/modes/flash/README.md)
- [`docs/design/modes/flash/voicing-emotions.md`](docs/design/modes/flash/voicing-emotions.md)
- [`docs/design/modes/flash/`](docs/design/modes/flash/)

### Morse code (`mini`)
Morse code (`mini`) normalizes input through Morse-compatible text rules, emphasizing clear rhythm, visual readability, and follow-along playback. The current speed presets are:

| Speed | Role |
| --- | --- |
| Slow | slower and easier to inspect in dot/dash visuals and lyrics follow |
| Standard | default Morse rhythm |
| Fast | shorter, more compact Morse output |




<p align="center">
  <video src="https://github.com/user-attachments/assets/7d4b6ba9-8102-4122-9518-854ee63a1bc8" width="400" controls muted autoplay loop style="border-radius: 8px;"></video>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/61618706-dacb-44cc-ba31-149d5d68f971" width="180" title="1" />
  <img src="https://github.com/user-attachments/assets/b68d1c27-34ef-450c-ae75-77c1be9708c3" width="180" title="2" />
  <img src="https://github.com/user-attachments/assets/72bcba95-cb29-4d39-a271-11c7cdc33602" width="180" title="3" />
</p>



For deeper `mini` rules, follow/visual behavior, and implementation notes, see:
- [`docs/design/modes/mini.md`](docs/design/modes/mini.md)

### DTMF-like dual-tone mapping (`pro`)
DTMF-like dual-tone mapping (`pro`) is the more formal ASCII-only mode: input text is first converted into ASCII bytes, then each byte is split into a high nibble and a low nibble, each of which maps to a dual-tone symbol, so `1 byte = 2 symbol`. It favors a cleaner and more regular acoustic signaling structure.



<p align="center">
  <video src="https://github.com/user-attachments/assets/e64a710d-12fe-44c0-bb6f-79dceed10d68" width="400" controls muted autoplay loop style="border-radius: 8px;"></video>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/ebfc6f96-ccf4-4d17-ace9-794704151515" width="180" title="1" />
  <img src="https://github.com/user-attachments/assets/1f436a22-cbff-4681-bd62-343875c3bce4" width="180" title="2" />
  <img src="https://github.com/user-attachments/assets/dc1144a3-fcd6-4e81-a258-a4f7a7380bf5" width="180" title="3" />
  <img src="https://github.com/user-attachments/assets/fb63021c-c32f-4d10-8a5e-a2e44ecbc6e8" width="180" title="4" />
</p>


For deeper `pro` mode positioning and implementation notes, see:
- [`docs/design/modes/pro.md`](docs/design/modes/pro.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

### `16-FSK` frequency mapping (`ultra`)
`16-FSK` frequency mapping (`ultra`) is the denser UTF-8-oriented mode: input text is processed directly as UTF-8 bytes, each byte is split into two nibbles, and each nibble maps to a fixed frequency in clean `16-FSK`, so `1 byte = 2 symbol`, with each symbol emitting only one frequency. It favors higher information density and a more formal UTF-8 text transport path.

Compared with bit-by-bit BFSK / FSK (`flash`), `16-FSK` (`ultra`) is not only shorter for the same input; its generation and decoding paths are also lighter. In one reference test with 7000 chars / 7000 bytes of text, bit-by-bit BFSK / FSK (`flash`) took about 1 min 56 sec to generate about 43 min of audio, while `16-FSK` (`ultra`) generated in about 2 sec and produced about 11 min 40 sec of audio. Exact numbers vary by device, style, and parameters, but the scale difference reflects the mode split: `flash` prioritizes audibility, explainability, and emotional delivery, while `ultra` prioritizes higher throughput and faster processing.


<p align="center">
  <video src="https://github.com/user-attachments/assets/444a37c7-af57-46a0-b34a-4a8e26a34db9" width="400" controls muted autoplay loop style="border-radius: 8px;"></video>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/2cf006b1-a4fe-4af1-bfce-2c738ed26129" width="180" title="1" />
  <img src="https://github.com/user-attachments/assets/cf5d6e62-d4d6-44f4-92ce-fd22335686c8" width="180" title="2" />
  <img src="https://github.com/user-attachments/assets/79d8854a-14d2-40d2-9672-e958b0a174fd" width="180" title="3" />
</p>


For deeper `ultra` mode positioning and implementation notes, see:
- [`docs/design/modes/ultra.md`](docs/design/modes/ultra.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

The implementation direction of the `ultra` mode was informed by the public engineering ideas and acoustic transport practice in [ggerganov/ggwave](https://github.com/ggerganov/ggwave), and we gratefully acknowledge that reference.  
This repository remains an independent implementation; except for third-party components explicitly marked in-tree, no affiliation, endorsement, or official relationship is claimed.

---

## Usage & Liability

### Icon Usage (Public Resource)
FlipBits icon assets (including source and component SVG files) are provided to the community as a public resource.

Subject to this repository's license and applicable law, you may use these icons for badges, derivative creations, videos, and commercial distribution (including sales) without requesting additional one-off permission from the project.

For public distribution, attribution is recommended:
`Icon designed by FlipBits Project`

Limits and boundaries (legally conservative):
- This permission covers the icon assets only and does not grant trademark rights, patent rights, personality/publicity rights, or any rights not explicitly granted.
- The assets are provided "as is" without express or implied warranties; users are responsible for their own compliance and risk assessment.
- Do not privatize, exclusively claim, or re-license these icon assets as proprietary/exclusive resources.
- Do not imply official representation, endorsement, or exclusive authorization from the FlipBits project or its author.

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
- The official Android project root is `C:\code\FlipBits\apps\audio_android`.
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
