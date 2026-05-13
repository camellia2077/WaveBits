# Android Mini Automation

## Purpose

This document describes the debug-only Mini scenario used for real-device adb capture. The current goal is observability only: gather precise Visual/Lyrics timing data for Mini slow, standard, and fast generated playback before changing sync behavior.

For the full Android automation matrix across JVM tests, instrumentation, and adb debug scenarios, see `docs/architecture/android/android-automation-coverage.md`.

## Current Coverage

Covered:

- Fast-regression input text: `mini sync test`
- Scenario kind: `ui`
- Mini Morse speeds: `slow`, `standard`, `fast`
- UI scenario actions driven through the normal ViewModel path:
  - select Mini mode
  - select the requested Morse speed
  - replace input text
  - encode generated Mini audio
  - open the player detail sheet
  - start playback
  - stop playback after the configured short capture window
- Debug log confirmation through `MiniAutomation`
- Unified Mini visual/Lyrics timing diagnostics through `MiniAlignmentPerf`
- Token-tape diagnostics through `FlashLyricsPerf`
- Lyrics layout diagnostics through `PlaybackLyricsLayout`

Not covered yet:

- Headless Mini diagnostics
- Saved-audio library playback
- Decode validation after playback
- Visual pixel assertions or screenshot comparison
- Release/staging build automation entry points

## Scenario Contract

Debug builds support:

```text
com.bag.audioandroid.DEBUG_MINI_SCENARIO
```

Fresh install on a dedicated test device:

```powershell
python tools/run.py android install-debug-fresh
```

Single-speed UI scenario:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_MINI_SCENARIO --es wb.scenario ui --es wb.mini.speed standard --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Expanded-lyrics UI scenario:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_MINI_SCENARIO --es wb.scenario ui --es wb.mini.speed standard --ez wb.lyrics.expand true --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Visual-page lyrics layout scenario:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_MINI_SCENARIO --es wb.scenario ui --es wb.mini.speed standard --es wb.display visual --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Supported extras:

- `wb.scenario`
  - `ui` by default.
- `wb.input`
  - Optional text override.
  - Defaults to `mini sync test`.
- `wb.lang`
  - Optional app-language override for the scenario.
  - Accepted values follow app locale tags such as `zh`, `en`, `ru`.
  - Applied before Mini mode selection and before player-detail capture.
- `wb.mini.speed`
  - `slow`, `standard`, `fast`
- `wb.lyrics.expand`
  - `false` by default.
  - When `true`, the debug scenario opens player detail and forces Lyrics into the full scrollable list state.
- `wb.display`
  - `lyrics` by default.
  - Supports `lyrics` and `visual`.
  - When `visual`, the debug scenario switches player detail to the Visual page before capture.
- `wb.encode`
  - `true` by default.
- `wb.play`
  - `true` by default.
- `wb.play.ms`
  - Playback duration before the debug scenario stops playback.
  - Defaults to `6000`.
  - Use `0` to leave playback running.

`python tools/run.py android-debug capture-mini` is a convenience wrapper around the same debug scenario, but it exposes only the common Mini path today.

Current `capture-mini` CLI surface:

- exposed:
  - `--speed`
  - `--input`
  - `--play-ms`
  - `--no-encode`
  - `--no-play`
- not exposed yet:
  - `wb.lang`
  - `wb.display`
  - `wb.lyrics.expand`

So:

- use `capture-mini` for the common speed sweep and baseline sync capture
- use raw `adb shell am start ...` when you need Visual-page capture, expanded lyrics, or language override without extending the CLI first

## Shared Flow

All Mini UI adb scenarios reuse the same core flow:

- Optionally apply `wb.lang` to switch the app language before scenario setup.
- Select Mini mode.
- Select the requested Morse speed.
- Resolve input text from `wb.input` or the fast-regression default.
- Encode generated Mini audio.
- Open player detail.
- Optionally switch player detail to the requested `wb.display` page.
- Optionally force expanded full-lyrics mode through `wb.lyrics.expand=true`.
- Start playback.
- Stop playback after the configured short capture window unless `wb.play.ms=0`.

This shared flow is the default contract. Extra diagnostics should be added around it, not by creating a separate tapping path.

For lyrics layout measurement, the UI scenario now forces a clean debug baseline before encode/playback:

- `Demo mode = off`
- `Visual perf overlay = off`

This reset is intentional. Demo mode only adds tap-feedback animation, but measurement captures still disable it to avoid inheriting any debug-only visual noise from device-local settings state.

## Mode-specific Observability

Visual mode:

- `MiniAutomation displayModeApplied ... mode=visual` confirms the sheet switched to Visual.
- `MiniAlignmentPerf` remains the preferred sync stream for Mini visual and lyrics together.
- `PlaybackLyricsLayout mode=visual surface=preview` measures the compact lyrics preview shown below the Visual page.

Lyrics mode:

- `MiniAutomation displayModeApplied ... mode=lyrics` confirms the sheet switched to Lyrics.
- `FlashLyricsPerf` remains the token-follow stream used by the compact lyrics preview.
- `PlaybackLyricsLayout mode=lyrics surface=token-strip` measures the main Lyrics-page tokenizer strip.
- `PlaybackLyricsLayout mode=lyrics surface=preview` measures the compact preview that still remains below the Lyrics page.

Expanded lyrics mode:

- `MiniAutomation lyricsExpanded` confirms the full lyrics list was forced open.
- `MiniAutomation lyricsFullListRendered` confirms the scrollable full-lyrics list actually composed.
- `MiniAutomation lyricsExpandToggle` confirms a manual tap changed the expanded state.

## Layout Measurement

`PlaybackLyricsLayout` is the shared layout-measurement log for Mini detail pages.

Before any Mini lyrics-size capture, the debug scenario logs:

- `MiniAutomation languageApplied ... language=<tag>` when `wb.lang` is present.
- `MiniAutomation measurementBaselineReset ... demoMode=false visualPerfOverlay=false`

Current surfaces:

- `mode=visual surface=preview`
  - Compact lyrics preview shown below the Visual page.
- `mode=lyrics surface=token-strip`
  - Main Lyrics-page tokenizer strip.
- `mode=lyrics surface=preview`
  - Compact preview that still remains below the Lyrics page.

Current fields:

- `containerHeightPx` / `containerHeightDp`
- `widthPx` / `widthDp` when the measured surface is horizontally constrained
- `visibleLineCount`
- `lineHeightDp`
- `spacingDp`
- `maxPossibleLines`
- `displayLineRanges`
- `wrapped`

Recommended device prep:

```powershell
python tools/run.py android-debug device-prep
```

Recommended log capture:

```powershell
python tools/run.py android-debug capture-mini --speed standard --play-ms 6000 --wait-ms 20000
```

Default `capture-mini` artifacts:

- `raw.log`
- `summary.md`
- `crash-summary.txt`

If `--output-dir` is omitted, the tool creates:

- `temp/android-debug/<timestamp>-mini-<speed>/`

`MiniAlignmentPerf` is the preferred Mini sync diagnostic. It records the UI samples used for the Mini Morse timeline visual and the Lyrics token tape in the same row, plus active Morse group and active token state.

Wrapper limitation to remember:

- `capture-mini` summary is built from the parsed `MiniAutomation` / `MiniAlignmentPerf` / `FlashLyricsPerf` rows only
- if you are investigating `PlaybackLyricsLayout` or other extra debug tags, use manual `adb logcat` capture instead of assuming the wrapper collected them

## Success Criteria

General capture success:

- `MiniAutomation` confirms the scenario was received.
- `MiniAutomation displayModeApplied` confirms the sheet switched to the requested display page.

Expanded-lyrics success:

- `MiniAutomation lyricsExpanded` confirms the sheet forced the expanded lyrics state.
- `MiniAutomation lyricsFullListRendered` confirms the full scrollable lyrics list was actually composed.
- `MiniAutomation lyricsExpandToggle` confirms a manual toggle tap changed the state.

Timing and follow success:

- `MiniAlignmentPerf` rows show `playing=true`.
- `sampleDelta` is visible for comparing Mini visual input sample and Lyrics input sample.
- `visualGroup` / `visualBitOffset` change as the Morse visual advances.
- `token`, `tokenText`, `tokenProgress`, and `lyricBitOffset` advance as Lyrics follows playback.

Layout-measurement success:

- `PlaybackLyricsLayout` reports the actual lyrics-container size for the current display mode and measured surface.
- `mode=visual surface=preview` appears when `wb.display=visual`.
- `mode=lyrics surface=token-strip` appears when `wb.display=lyrics`.
- `mode=lyrics surface=preview` appears when `wb.display=lyrics`.

All-speed fast sweep:

```powershell
$speeds = @("slow", "standard", "fast")
foreach ($speed in $speeds) {
    python tools/run.py android-debug capture-mini --speed $speed --play-ms 6000 --wait-ms 20000 --output-dir "temp\android-debug\mini_$speed"
}
```

## Maintenance Rules

- Keep this flow debug-only.
- Do not use coordinate taps or accessibility automation for Mini generated playback checks.
- Keep Mini sync fixes separate from this capture prework unless a later task explicitly asks to change behavior.
