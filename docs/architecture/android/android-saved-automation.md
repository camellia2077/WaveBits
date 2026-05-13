# Android Saved Automation

## Purpose

This document defines the debug-only Saved-audio adb scenario used to diagnose real-device library selection latency, long-audio file-backed loading, and player-detail opening without relying on coordinate taps.

For the full Android automation matrix across JVM tests, instrumentation, and adb debug scenarios, see `docs/architecture/android/android-automation-coverage.md`.

## Current Coverage

Covered:

- Saved-library selection through the normal ViewModel path
- Player-detail opening after selecting a Saved item
- Long-audio `file-backed` load diagnostics
- Main-thread vs background hydration timing for Saved selection
- Optional debug-only seeding of a long Saved WAV owned by the current app install
- Debug log confirmation through `SavedAudioAutomation`
- Stage timing through `SavedAudioPerf`
- Playback target diagnostics through `PlaybackAutomation`

Not covered yet:

- Automatic play/pause assertions after selection
- Delete/rename/share/export flows
- Pixel assertions or screenshot comparison
- Release/staging build automation entry points
- Saved decode-cache behavior, because that cache layer does not exist yet

## ADB Scenario

Debug builds support:

```text
com.bag.audioandroid.DEBUG_SAVED_AUDIO_SCENARIO
```

Fresh install on a dedicated test device:

```powershell
python tools/run.py android install-debug-fresh
```

Select a specific Saved item by MediaStore item id:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_SAVED_AUDIO_SCENARIO --es wb.saved.item_id 293
```

Select a specific Saved item by display name:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_SAVED_AUDIO_SCENARIO --es wb.saved.display_name "saved perf 181_pro_20260511_224633.wav"
```

Seed a debug-only long Saved file, then select and open it:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_SAVED_AUDIO_SCENARIO --el wb.saved.seed_duration_ms 181000 --es wb.saved.seed_mode pro
```

Supported extras:

- `wb.saved.item_id`
  - Optional exact Saved item id.
  - Takes priority over display name.
- `wb.saved.display_name`
  - Optional Saved file display name.
- `wb.saved.seed_duration_ms`
  - Optional debug-only seeding duration in milliseconds.
  - When set to a positive value and no target is found, the app generates and exports a silent Saved WAV owned by the current app install, then retries selection.
- `wb.saved.seed_mode`
  - Optional mode label for the seeded Saved file.
  - Accepted values follow the same mode ids used by other debug scenarios: `mini`, `pro`, `ultra`.
  - Defaults to `mini` when omitted.

Recommended device prep:

```powershell
python tools/run.py android-debug device-prep
```

Recommended log capture:

```powershell
adb logcat -c
adb logcat -v time SavedAudioAutomation:D SavedAudioPerf:D PlaybackAutomation:D AndroidRuntime:E libc:E *:S > temp\saved_audio_perf.log
```

## Success Criteria

For the async Saved selection flow:

- `SavedAudioAutomation received` confirms the scenario was received.
- `SavedAudioAutomation selectionResolved` shows a target item or a later seeded fallback.
- `SavedAudioPerf selectionEnd` should stay small on the main thread for long audio.
  - Current regression target is that long Saved selection stays in single-digit milliseconds on the UI thread.
- `SavedAudioAutomation openDetail` confirms the player detail sheet opened without waiting for full hydration.
- `SavedAudioPerf gatewayLoadEnd` shows the real long-audio load cost on the worker thread.
- `SavedAudioPerf selectionHydrateEnd` confirms the selected Saved item was fully hydrated after the placeholder state.

For the old blocking regression, the important comparison is:

- `selectionEnd` measures the immediate UI-path cost.
- `gatewayFileBackedExtract`, `gatewayFileBackedWaveform`, and `gatewayLoadEnd` measure the background long-audio work that used to block the click.

## Log Meanings

- `SavedAudioAutomation`
  - Scenario receipt
  - Saved target resolution
  - Optional seeding result
  - Selection application
  - Player-detail opening
- `SavedAudioPerf`
  - Selection placeholder update
  - Saved repository load timing
  - Flash-description timing when applicable
  - File-backed extraction timing
  - Waveform preview timing
  - Final hydration completion

## Maintenance Rules

- Keep this flow behind `BuildConfig.DEBUG`.
- Prefer this scenario over coordinate taps for Saved selection latency work.
- When long-audio selection behavior changes, update both this document and `docs/architecture/android/android-automation-coverage.md` in the same change.
- If a future Saved decode cache is added, document its hit/miss logs and invalidation rules here.
