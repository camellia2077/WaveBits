# Android Flash Visual

## Purpose

This document explains the Android Flash Visual playback path, the data it consumes, and the diagnostic workflow for animation stutter, jumping, or density problems.

Use this before changing Flash Visual, Lyrics follow, playback smoothing, or long-audio visual performance. The goal is to identify the real bottleneck with code and debug data before widening the change to windowing, follow data, or native libraries.

## Primary Files

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlaybackDisplaySection.kt`
  - Routes the player display mode into waveform, Flash Visual, Lyrics, and other mode-specific visuals.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/AudioFlashSignalVisualizer.kt`
  - Main Flash Visual component. Owns viewport calculation, debug overlay placement, smoothed visual sample position, and Canvas composition.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/FlashSignalVisualizationInput.kt`
  - Defines the visual input boundary for PCM and follow timeline sources.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/FlashSignalVisualizationAnalysis.kt`
  - Builds bucket and segment analysis used by Flash Visual.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/FlashSignalVisualizationDrawing.kt`
  - Draws the current `Lanes`, `Pulse`, and `Pitch` Flash visual primitives.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/FlashVisualWindowActions.kt`
  - Maintains the Flash visual window state and throttles/reduces redundant window requests.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/state/FlashVisualWindowState.kt`
  - Holds the current windowed Flash timeline and drawable segment budget.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/FlashVisualPerfTrace.kt`
  - Debug-only metrics for rendering, windowing, playback position, and visual motion quality.

## Data Flow

Flash audio can reach the visualizer through two paths:

- Short/PCM path:
  - `AudioPlaybackDisplayBlock` / `PlaybackDisplaySection`
  - `FlashSignalVisualizationInput`
  - `AudioFlashSignalVisualizer`
  - PCM bucket analysis in `FlashSignalVisualizationAnalysis.kt`
  - Canvas drawing in `FlashSignalVisualizationDrawing.kt`

- Generated/follow timeline path:
  - Native encode/decode result provides follow data.
  - Android receives follow data and exposes Flash binary group timing through `FlashSignalBucketSource.FollowTimeline`.
  - `FlashVisualWindowActions` builds a current viewport window from the full follow timeline.
  - `FlashVisualWindowState` gives `AudioFlashSignalVisualizer` a pre-windowed segment list.
  - Canvas draws only the current visual window around playback.

Lyrics and Flash Visual are related but not identical:

- Lyrics focuses on text/token follow state and token cards.
- Flash Visual focuses on low/high/silence signal segments and visual motion.
- Do not assume a Lyrics fix belongs in Flash Visual, or that a Flash Visual stutter requires native/follow-data changes.

## Playback Position

There are two important positions:

- Raw playback position:
  - Comes from the player state as displayed samples.
  - It may update at a coarse cadence, for example 8-10 times per second.
- Smoothed visual position:
  - Used only by `AudioFlashSignalVisualizer` Canvas movement.
  - Advances from a frame-driven clock so the viewport can move smoothly between coarse raw player updates.
  - It should not reset on every raw anchor update.

If FPS is high but the image still jumps, check position smoothing before changing drawing, windowing, or native data.

## Windowing

The Flash Visual should behave like a sliding viewport:

- The full follow timeline may be long.
- Android should keep a current `FlashVisualWindowState` near the playback viewport.
- Canvas should draw only visible/budgeted primitives.
- Window requests should be deduped/throttled so playback does not rebuild state every frame.

Windowing is a performance boundary, not a timing source. The timing source is still playback position plus visual smoothing.

## Debug Metrics

`FlashVisualPerfTrace` is debug-only. It logs one `FlashVisualPerf` row roughly once per second and also feeds the debug overlay.

Rendering and composition:

- `draw/s`
  - Canvas draw rate.
- `drawAvgMs`, `drawMaxMs`
  - Canvas draw duration.
- `compose/s`
  - Compose recomposition rate for the visual path.
- `visible`, `visiblePrimitives`
  - How many visible segments/primitives the Canvas is drawing.

Windowing:

- `windowReq/s`
  - Window request rate.
- `windowBuild/s`
  - Window build rate.
- `windowBusySkip/s`
  - Requests skipped because a window build was already active.
- `window=[start,end)`
  - Current Flash Visual window in sample space.
- `windowShiftMaxMs`
  - Maximum window start jump during the interval.

Playback and motion:

- `rawUpdate/s`
  - Raw playback position update rate.
- `rawStepMaxMs`
  - Largest raw playback position jump.
- `smoothStepMaxMs`
  - Largest smoothed visual position step.
- `visualErrorMs`
  - Difference between raw playback position and smoothed visual position.
- `pxStepMax`
  - Largest single-frame visual movement in pixels.
- `anchorJumpMaxMs`
  - Largest raw anchor jump seen by the smoother.
- `smoothReset`
  - Number of smoother resets in the interval.
- `viewportStartStepMaxMs`
  - Largest jump in the actual rendered viewport start.
- `largePxStep`
  - Count of large visual jumps over the debug threshold.

## ADB Capture

Use ADB data when the symptom is animation stutter, jumping, flicker, or long-audio performance. Do not rely only on subjective visual feedback.

For the fixed Flash device scenario, supported styles/visual modes, Mix-mode token follow behavior, debug overlays, instrumentation coverage, and stable UI tags, use `docs/architecture/android/android-flash-automation.md`.

Recommended capture:

```powershell
adb logcat -c
adb logcat FlashVisualPerf:D *:S > temp\log.txt
```

Reproduce the issue for 10-15 seconds, then stop the command and inspect the captured `FlashVisualPerf` rows in `temp/log.txt`.

## Diagnostic Workflow

Start with the symptom, then use metrics to choose the layer:

- FPS is low or `drawAvgMs` / `drawMaxMs` is high:
  - Inspect Canvas drawing, primitive counts, object allocation, and visible segment filtering.
- `compose/s` is high:
  - Inspect state reads, `remember` keys, and whether fast-changing playback position is forcing broad recomposition.
- `windowReq/s` or `windowBuild/s` is high:
  - Inspect `FlashVisualWindowActions` request dedupe, refresh margins, and active job skipping.
- `rawUpdate/s` is low but `draw/s` is high:
  - Inspect visual smoothing. The player may provide coarse position updates while Canvas is capable of drawing smoothly.
- `smoothReset` is high:
  - Inspect `LaunchedEffect` keys and state reset logic. The smoother should not restart on every raw anchor update.
- `viewportStartStepMaxMs` or `pxStepMax` is high:
  - Inspect viewport calculation and whether raw/window changes are being applied as visible jumps.
- Metrics are healthy but users cannot follow the animation:
  - Treat it as visual density/readability. Reduce visible bit density or adjust visual scale without changing audio timing.

Only consider native/libs changes after Android metrics show that the data source itself is missing required indexed timing information or is forcing Android to rebuild large timelines repeatedly.

## Lessons From The Long-Audio Stutter Fix

The long-audio Flash Visual stutter was not caused by native data size or Canvas draw cost. Debug metrics showed:

- `draw/s` was high.
- `drawAvgMs` was low.
- `rawUpdate/s` was coarse but expected.
- `smoothReset` matched raw update cadence.
- `pxStepMax` and `viewportStartStepMaxMs` were high.

The root cause was visual smoothing restarting on raw anchor updates. The effective fix was narrow: keep the smoother alive across raw anchor changes, read the latest anchor via updated state, and only re-anchor when drift becomes too large.

## Example Diagnosis

A real long-audio Flash Visual capture produced this summary:

- Rows: `17`
- Avg FPS: `94.14`
- Avg draw ms: `0.35`
- Max draw ms: `2.01`
- Avg visible primitives: `43.41`
- Max px step: `166.43`
- Max anchor jump ms: `185.76`
- Max viewport start step ms: `169.14`
- Total smooth resets: `135`
- Total large px steps: `1428`
- Avg window builds/s: `0.88`

Interpretation:

- Drawing was fast enough, so the visible stutter was not a Canvas throughput problem.
- Raw player position updated coarsely, which is expected and must be smoothed visually.
- `smoothReset` matched raw update cadence, proving the smoother was restarting on raw anchor changes.
- `pxStepMax` and `viewportStartStepMaxMs` proved the rendered viewport was visibly jumping.
- The correct fix was to keep the smoother alive across raw anchor changes, not to widen the change into native/libs data generation.

This is the preferred pattern for future animation fixes:

1. Read the actual data path.
2. Add a minimal debug metric if the system is still a black box.
3. Capture ADB logs.
4. Decide the layer from metrics.
5. Make the narrowest code change that should move the metric.
6. Re-check the same metric after the change.

Avoid widening directly to follow data, window source, or native libraries unless the metrics prove that boundary is the bottleneck.
