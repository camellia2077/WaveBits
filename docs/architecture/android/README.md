# Android Architecture Documents

This directory contains Android-specific architecture and diagnostic documents.

Use it for Android-only behavior such as player UI structure, native bridge strategy, Flash Visual animation, Lyrics follow, and device-driven debugging. Keep cross-platform library architecture in `docs/architecture/` or `docs/libs/`.

## Files

- `android-app-architecture.md`
  - Android app layering, UI state flow, JNI calls, and directory ownership.
- `android-ui-structure.md`
  - Android presentation ownership, player scaffold responsibilities, and UI placement rules.
- `android-native-strategy.md`
  - Android native consumption strategy, CMake/NDK constraints, and release-like native checks.
- `android-automation-coverage.md`
  - Android automation coverage matrix across JVM tests, instrumentation UI smoke, and adb debug scenarios.
- `android-flash-visual.md`
  - Flash Visual data flow, windowing boundary, debug metrics, ADB capture, and animation stutter diagnosis.
- `android-flash-automation.md`
  - Flash real-device UI regression coverage, debug adb scenario, stable test tags, and current automation limits.
- `android-mini-automation.md`
  - Mini real-device adb scenario and Visual/Lyrics timing capture prework for slow, standard, and fast Morse speeds.
- `android-encode-progress-automation.md`
  - Mini/Pro/Ultra real-device adb scenario for encode progress bar phase and visibility capture.
- `android-settings-automation.md`
  - Settings-owned automation surface such as app-language overrides and layout-measurement debug baseline resets.
