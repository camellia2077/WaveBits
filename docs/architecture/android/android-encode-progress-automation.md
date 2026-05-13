# Android Encode Progress Automation

## Purpose

This document describes the debug-only adb scenario for capturing how the Audio page encode progress UI behaves while Mini, Pro, and Ultra audio is being generated. Use this when the visible progress label or bar appears to jump between phases such as `Finalizing` and `Preparing input`.

For the full Android automation matrix across JVM tests, instrumentation, and adb debug scenarios, see `docs/architecture/android/android-automation-coverage.md`.

## Current Coverage

Covered:

- Transport modes: `mini`, `pro`, `ultra`
- Built-in sample text selection through `wb.sample.length` / `wb.sample.id`
- Optional input amplification through `wb.repeat`
- Mini Morse speed selection through `wb.mini.speed`
- UI scenario actions driven through the normal ViewModel path:
  - select the requested transport mode
  - select the requested Mini speed when mode is `mini`
  - replace input text
  - encode generated audio
- Debug log confirmation through `EncodeProgressAutomation`
- State-change UI snapshots through `EncodeProgressAutomation ui`
- Periodic progress-display sampling through `EncodeProgressAutomation uiTick`
- Encode/result/cache diagnostics through `FlipBitsLongAudio` and `GeneratedAudioCache`

Not covered:

- Flash voicing progress; Flash uses the separate Flash scenario for voicing and playback diagnostics.
- Saved-audio playback, import/export flows, and decode validation.
- Screenshot or pixel assertions for the progress bar.
- Release/staging build automation entry points.

## ADB Scenario

Debug builds support:

```text
com.bag.audioandroid.DEBUG_ENCODE_PROGRESS_SCENARIO
```

Single-mode capture:

```powershell
python tools/run.py android-debug capture-encode-progress --mode pro --sample-length long --repeat 3 --capture-ms 120000 --poll-ms 33 --wait-ms 140000
```

Supported extras:

- `wb.mode`
  - `mini` by default.
  - Accepted values: `mini`, `pro`, `ultra`.
- `wb.input`
  - Optional text override.
  - Defaults to `encode progress test`.
  - Takes priority over `wb.sample.length` and `wb.sample.id`.
- `wb.sample.length`
  - Optional built-in sample selector.
  - Accepted values: `short`, `long`.
  - Uses the current app language or `wb.lang` override, plus the current sample flavor.
- `wb.sample.id`
  - Optional built-in sample id selector.
  - Takes priority over `wb.sample.length` when `wb.input` is absent.
- `wb.lang`
  - Optional app-language override for the scenario.
  - Accepted values follow app locale tags such as `zh`, `en`, `ru`.
  - Applied before sample resolution and encode capture.
- `wb.repeat`
  - Repeats the resolved input text.
  - Accepted range: `1..50`.
  - Useful for making Mini/Pro/Ultra generation long enough to observe progress transitions.
- `wb.mini.speed`
  - Mini only.
  - Accepted values: `slow`, `standard`, `fast`.
- `wb.encode`
  - `true` by default.
- `wb.capture.ms`
  - Maximum periodic UI capture duration.
  - Defaults to `120000`.
- `wb.poll.ms`
  - Periodic UI sampling interval.
  - Defaults to `33`.
  - Accepted range: `16..1000`.

`python tools/run.py android-debug capture-encode-progress` is the main wrapper for this scenario and currently exposes the whole commonly used contract:

- `--mode`
- `--speed`
- `--input`
- `--sample-length`
- `--sample-id`
- `--repeat`
- `--capture-ms`
- `--poll-ms`
- `--no-encode`

Language override still exists at the raw adb scenario layer through `wb.lang`, but the wrapper does not surface it yet.

Recommended device prep:

```powershell
python tools/run.py android-debug device-prep
```

Default `capture-encode-progress` artifacts:

- `raw.log`
- `summary.md`
- `crash-summary.txt`

If `--output-dir` is omitted, the tool creates:

- `temp/android-debug/<timestamp>-encode-progress-<mode>/`

## Three-Mode Sweep

```powershell
$modes = @("mini", "pro", "ultra")
foreach ($mode in $modes) {
    if ($mode -eq "mini") {
        python tools/run.py android-debug capture-encode-progress --mode $mode --speed slow --sample-length long --repeat 3 --capture-ms 120000 --poll-ms 33 --wait-ms 140000 --output-dir "temp\android-debug\encode_progress_$mode"
    } else {
        python tools/run.py android-debug capture-encode-progress --mode $mode --sample-length long --repeat 3 --capture-ms 120000 --poll-ms 33 --wait-ms 140000 --output-dir "temp\android-debug\encode_progress_$mode"
    }
}
```

Read the generated summary first. The most useful fields are:

- `barVisible`
  - Whether the progress bar should be visible on the Audio page.
- `labelVisible`
  - Whether the phase label should be visible.
- `phase`
  - UI-facing phase backing the visible label.
- `percent` / `progress`
  - UI-facing progress bar value.
- `Preparing/Finalizing bounces`
  - Count of adjacent phase transitions between `PreparingInput` and `Finalizing`.

When you already have a captured log, the paired summary command is:

```powershell
python tools/run.py android-debug encode-progress-summary temp\android-debug\<capture-dir>\raw.log --output temp\android-debug\<capture-dir>\summary.md
```

## Success Criteria

- `EncodeProgressAutomation received` confirms the requested mode and capture interval.
- `EncodeProgressAutomation inputResolved` shows `source`, `sampleId`, `chars`, and `payloadBytes`.
- `EncodeProgressAutomation uiTick` rows are present while `busy=true`.
- `barVisible=true` and `labelVisible=true` appear during generation.
- The summary's phase transition table shows whether the UI is actually alternating between `PreparingInput` and `Finalizing`, or whether the issue is coming from native progress emission, state reduction, or observation timing.

## Maintenance Rules

- Keep this scenario debug-only.
- Do not use coordinate taps or accessibility automation for encode progress checks.
- Prefer `wb.sample.length long` plus `wb.repeat` over manual long shell-escaped text when the built-in sample corpus is sufficient.
- If the progress UI changes its phase labels or visibility conditions, update the log fields and summary script together.
