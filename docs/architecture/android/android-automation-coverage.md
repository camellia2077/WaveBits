# Android Automation Coverage

## Purpose

This document is the Android automation index. Use it before choosing a device scenario or adding new regression coverage. It summarizes what is covered today, what each automation layer is meant to prove, and what is still outside the current suite.

## Automation Layers

### JVM State Regression

Run with:

```powershell
python tools/run.py android test-debug
```

This layer is the fastest and most stable place to lock ViewModel/action state transitions. It does not draw Compose UI and does not use adb.

Covered:

- Encode state transitions, cancellation, validation failure, native failure, and success.
- Generated PCM, metadata, follow data, playback state, and mini player model after encode.
- Long-text segmentation, file-backed generated PCM, segment metadata, and follow hydration.
- Segmented encode progress phase aggregation, including the `PreparingInput` / `Finalizing` no-bounce regression.
- Failed encode clearing stale generated audio instead of exposing stale/zero-duration mini player state.
- Saved/Generated playback source switching: saved playback remains selected after a later generated encode failure.
- Generated encode/decode roundtrip through UI actions for `flash`, `mini`, `pro`, and `ultra`.
- Decode result storage for generated and saved playback sources, including segmented generated audio.
- Saved library mutation basics: import status, folder creation, folder assignment, rename/delete status paths.
- Playback runtime state logic: progress, scrub, completion, playback speed, and source coordination.
- Screen-level component tests for selected player controls and detail-sheet behavior.

Not covered:

- Actual Android process lifecycle, Activity recreation, background/foreground transitions, or permission UI.
- Real MediaStore reads/writes, share sheet, system picker, or document-provider behavior.
- Compose rendering pixels, real animations, or screenshot comparisons.
- Real native encode/decode performance timing on a device.

### Instrumentation UI Smoke

Run from `apps/audio_android` on a connected device:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

This layer launches `MainActivity` and checks stable Compose test tags. It is for foreground UI smoke, not deep log diagnosis.

Covered:

- Flash debug scenario creates generated playback and opens player detail for every Flash voicing style.
- Mini debug scenario creates generated playback and opens player detail for every Mini Morse speed.
- Player detail sheet smoke coverage for selected rendering paths.

Not covered:

- Pixel assertions or screenshot comparisons.
- Full saved library workflows.
- Import/export/share system UI.
- Release/staging build variants.

### ADB Debug Scenarios

These are debug-only, agent-driven scenarios for collecting focused device logs. They use the normal ViewModel path and avoid coordinate taps.

Covered scenarios:

- Flash generated playback: `docs/architecture/android/android-flash-automation.md`
- Mini generated playback alignment: `docs/architecture/android/android-mini-automation.md`
- Mini/Pro/Ultra encode progress display: `docs/architecture/android/android-encode-progress-automation.md`
- Saved library selection + player-detail latency: `docs/architecture/android/android-saved-automation.md`
- Settings-owned automation surface: `docs/architecture/android/android-settings-automation.md`

Common coverage:

- Device foreground app path.
- Debug intent extras for input text and scenario configuration.
- Debug intent extras for selected Settings-owned state such as app language.
- Logcat capture and summaries through `python tools/run.py android-debug capture-*` and `python tools/run.py android-debug ...-summary`.
- Generated playback and progress diagnostics with stable debug tags.
- Debug-only on-screen overlays for selected playback diagnosis, including Flash Visual perf metrics and `Lanes` alignment observation.

Not covered:

- Release/staging automation entry points.
- Saved library import/export/share flows.
- Screenshot/pixel validation.
- Automated assertion pass/fail beyond the generated summary and manual success criteria.

## Current Feature Matrix

| Area | JVM state tests | Instrumentation UI | ADB debug scenario | Notes |
| --- | --- | --- | --- | --- |
| Flash encode | Covered | Covered | Covered | ADB covers styles, short/long/sample id/custom input, UI/headless. |
| Flash playback Visual/Lyrics | Partial | Covered smoke | Covered diagnostics | ADB now covers `lyrics/visual/mix`, `lanes/pulse/pitch`, shared-sample alignment logs, and debug overlay observation; pixel correctness is still not asserted. |
| Flash voicing styles | Covered through config and selected flows | Covered | Covered | Full visual-mode x style matrix is not in instrumentation. |
| Mini encode | Covered | Covered | Covered | ADB Mini scenario covers playback alignment; encode-progress scenario covers progress UI. |
| Mini Morse speeds | Covered through config and selected flows | Covered | Covered | ADB Mini scenario sweeps slow/standard/fast. |
| Pro encode | Covered | Not mode-specific | Covered | Encode-progress scenario covers progress UI. |
| Ultra encode | Covered | Not mode-specific | Covered | Encode-progress scenario caught and verifies the no-bounce regression. |
| Generated mini player | Covered | Covered smoke | Covered diagnostics | Failure cleanup is covered in JVM. |
| Saved playback source | Covered | Partial through UI surfaces | Covered diagnostics | ADB scenario covers selection/detail timing and long-audio hydration. |
| Decode/Roundtrip | Covered | Not covered | Not covered | Generated `flash/mini/pro/ultra` roundtrip is JVM-only. |
| Long text segmentation | Covered | Not covered | Covered indirectly | ADB progress captures long generated input; JVM validates metadata/state. |
| File-backed generated PCM | Covered | Not covered | Covered by logs | Real file size/perf is device-observed, not asserted. |
| Import/export/share | Partial mutation status only | Not covered | Not covered | Real MediaStore/document/share behavior remains a gap. |
| Saved folders/rename/delete | Partial mutation state | Not covered | Not covered | Product-level UI workflow remains a gap. |
| Playback controls | Covered state logic | Partial UI | Partial logs | Full scrub/speed/repeat/next/previous UI matrix remains a gap. |

## Recommended Next Coverage

Highest priority gaps:

- Saved library product workflow on device:
  - play/pause
  - delete selected/current item
  - verify current playback source fallback
- Decode/Roundtrip instrumentation smoke:
  - generate short text for each mode
  - decode from the UI
  - assert decoded text is displayed
- Import/export MediaStore/document workflow:
  - export generated WAV
  - import it back
  - assert metadata, duration, mode, and playback source.

Lower priority but useful:

- Screenshot or pixel assertions for Flash/Mini visual surfaces.
- Staging/release-like device automation for JNI/R8/resource-shrinker regressions.
- Activity recreation and app background/foreground playback-state recovery.

## Current Agent Notes

Useful things a new agent should know before debugging Android playback alignment:

- `python tools/run.py android-debug capture-flash` is the main entry for real-device Flash capture.
- All `capture-*` wrappers write the same three artifact files:
  - `raw.log`
  - `summary.md`
  - `crash-summary.txt`
- `python tools/run.py android-debug crash-summary <raw.log>` can be reused on any existing adb dump; it is not Flash-specific.
- Flash adb scenarios support:
  - `--scenario ui|headless`
  - `--display lyrics|visual|mix`
  - `--visual lanes|pulse|pitch`
  - `--style <flash-style>`
  - `--sample-length short|long` or explicit `--input`
- Current Flash `1.0x` real-device baseline:
  - sweep every `FlashVoicingStyleOption`
  - first gate is `Mix + Pulse`
  - current reference capture is `temp/android-debug/flash_mix_pulse_style_sweep_20260514/`
- Important wrapper limitation:
  - the raw adb Flash scenario supports `wb.display` and `wb.lang`
  - `capture-flash` now exposes `--display`
  - `capture-flash` still does not expose `wb.lang`
  - use raw `adb shell am start ...` when you need language override from the wrapper path
- Important Mini wrapper limitation:
  - the raw adb Mini scenario supports `wb.display`, `wb.lyrics.expand`, and `wb.lang`
  - `capture-mini` does not expose them yet
- `FlashAlignmentPerf` is the preferred unified sync stream for Flash:
  - it carries Visual, `8 bits`, and token-card state in one throttled row
  - current fields are enough to distinguish offset drift from bit-value mismatch
- `Settings > Debug > Visual perf overlay` is not only an FPS overlay anymore:
  - in `Flash + Lanes`, it also shows current bit boundaries and an on-screen `lane / row / token` alignment summary
- `Pulse` has its own geometry trace:
  - `PlaybackPulseLayout`
- Stable Compose tags worth preferring in tests and scripted checks:
  - `playback-display-lyrics`
  - `playback-display-visual`
  - `playback-display-mix`
  - `flash-visualization-mode-lanes`
  - `flash-visualization-mode-pulse`
  - `flash-visualization-mode-pitch`
  - `flash-visual-lanes-alignment-overlay`
  - `flash-visual-lanes-alignment-summary`
  - `flash-visual-pulse-tape`

## Maintenance Rules

- Keep debug scenarios behind `BuildConfig.DEBUG`.
- Prefer JVM tests for state-machine contracts and failure cleanup.
- Prefer instrumentation tests for stable Compose tree smoke.
- Prefer adb debug scenarios for timing, native/device behavior, long text, and log-rich diagnosis.
- Do not use coordinate taps for flows that can be driven through debug scenarios or stable test tags.
- When adding a scenario, update this matrix and the specific scenario document in the same change.
