# Android Flash Automation

## Purpose

This document explains the current Flash device automation surface for real-device UI regression checks and agent/adb-driven debugging. It is intentionally separate from the Flash Visual rendering architecture so agents can quickly find the automation contract and its current coverage.

For the full Android automation matrix across JVM tests, instrumentation, and adb debug scenarios, see `docs/architecture/android/android-automation-coverage.md`.

## Current Coverage

The automation path covers generated Flash playback only. It does not depend on sample catalog entries.

Covered:

- Fast-regression input text: `flash sync test`
- Built-in sample text selection through `wb.sample.length` / `wb.sample.id`
- Scenario kinds:
  - `ui`: real foreground UI regression through player detail + Compose visual rendering.
  - `headless`: generated-audio diagnostics without opening player detail or depending on the visual Canvas.
- Flash voicing styles: `standard`, `hostile`, `litany`, `collapse`, `zeal`, `void`
- Flash visual modes accepted by the adb scenario: `lanes`, `pulse`, `pitch`
- Playback detail display modes accepted by the adb scenario: `lyrics`, `visual`, `mix`
- UI scenario actions driven through the normal ViewModel path:
  - select Flash mode
  - select the requested Flash voicing style
  - replace input text
  - encode generated Flash audio
  - open the player detail sheet
  - switch the player detail sheet to the requested display page
  - start playback
  - stop playback after the configured short capture window
- Debug log confirmation through `FlashAutomation`
- Encode/result/follow diagnostics through `FlipBitsLongAudio`
- Generated PCM cache diagnostics through `GeneratedAudioCache`
- Playback target diagnostics through `PlaybackAutomation`
- Visual playback diagnostics through `FlashVisualPerf`
- Unified Visual/readout/Lyrics timing diagnostics through `FlashAlignmentPerf`
- Headless diagnostics through `FlashHeadless`
- Lyrics/token alignment diagnostics through `FlashLyricsPerf`
- Lyrics layout diagnostics through `PlaybackLyricsLayout`
- Vertical player-detail layout diagnostics through `PlaybackVerticalLayout`
- Pulse overlay diagnostics through `PlaybackPulseLayout`
- Mix-mode shared-sample diagnostics through `FlashAlignmentPerf`
- Lanes playhead-to-bit boundary observation through the debug-only Visual perf overlay
- On-screen Lanes alignment summary overlay for `lane / 8 bits / token` current bit comparison

Not covered yet:

- Saved-audio library playback
- Import/export flows
- Decode validation after playback
- Visual pixel assertions or screenshot comparison
- Running every visual mode for every style in the instrumentation test matrix
- Release/staging build automation entry points

Longer text is still supported through `wb.input`, but it is not the default for full style sweeps. For pangram or UTF-8/readout investigations, pass the text explicitly and usually run one style at a time.

## Scenario Contract

Debug builds support a dedicated action:

```text
com.bag.audioandroid.DEBUG_FLASH_SCENARIO
```

Fresh install on a dedicated test device:

```powershell
python tools/run.py android install-debug-fresh --clean
```

This command builds the standard debug artifact, verifies that `apps/audio_android/app/build/outputs/apk/debug/app-debug.apk` is present and not suspiciously small, runs `adb uninstall com.bag.audioandroid`, then installs the debug APK. Debug automation should use the standard `app-debug.apk`; `FlipBits-*` APK copies are kept for staging/release artifacts only.

UI scenario command:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.scenario ui --es wb.flash.style litany --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Visual-page lyrics-layout scenario:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.scenario ui --es wb.flash.style standard --es wb.display visual --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Lyrics-page layout scenario:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.scenario ui --es wb.flash.style standard --es wb.display lyrics --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Mix-page alignment scenario:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.scenario ui --es wb.flash.style standard --es wb.display mix --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Headless scenario command:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.scenario headless --es wb.flash.style litany --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Supported extras:

- `wb.scenario`
  - `ui` by default.
  - `headless` skips player-detail opening and logs generated playback/follow timing through `FlashHeadless`.
- `wb.input`
  - Optional text override.
  - Defaults to `flash sync test`.
  - Takes priority over `wb.sample.length` and `wb.sample.id` when present.
- `wb.sample.length`
  - Optional built-in sample selector.
  - Accepted values: `short`, `long`.
  - Uses the current app language and current sample flavor, then picks the first matching Flash sample from the catalog.
  - Useful for reproducing long-text behavior without passing long shell-escaped text through adb.
- `wb.sample.id`
  - Optional built-in sample id selector.
  - Uses the current app language or `wb.lang` override, plus the current sample flavor.
  - Takes priority over `wb.sample.length` when `wb.input` is absent.
- `wb.lang`
  - Optional app-language override for the scenario.
  - Accepted values follow app locale tags such as `zh`, `en`, `ru`.
  - Applied before sample resolution and before the player detail capture.
  - Useful when the same scenario should be replayed across multiple languages without manually opening Settings first.
- `wb.flash.style`
  - `standard`, `hostile`, `litany`, `collapse`, `zeal`, `void`
- `wb.display`
  - `lyrics` by default.
  - Supports `lyrics`, `visual`, and `mix`.
  - When `visual`, the debug scenario switches player detail to the Visual page before capture.
  - When `mix`, the debug scenario switches player detail to the combined Visual + token-strip page before capture.
- `wb.visual`
  - `lanes`, `pulse`, `pitch`
- `wb.encode`
  - `true` by default.
- `wb.play`
  - `true` by default.
- `wb.play.ms`
  - Playback duration before the debug scenario stops playback.
  - Defaults to `6000`.
  - Use `0` to leave playback running.

`python tools/run.py android-debug capture-flash` is a convenience wrapper around the same debug scenario, but it does not expose every adb extra yet.

Current `capture-flash` CLI surface:

- exposed:
  - `--scenario`
  - `--style`
  - `--display`
  - `--visual`
  - `--input`
  - `--sample-length`
  - `--sample-id`
  - `--play-ms`
  - `--no-encode`
  - `--no-play`
- not exposed yet:
  - `wb.lang`

So:

- use `capture-flash` for the common `ui/headless + style + display + visual + sample/input` path
- use raw `adb shell am start ...` when you need language override or another extra that is still outside the wrapper surface

Recommended `1.0x` style-alignment baseline:

- first gate every Flash voicing style with `Mix + Pulse @ 1.0x`
- use that sweep to judge whether `visual`, `8 bits`, and token `bin/hex` agree before widening into `Pitch` or `Lanes`
- current reference sweep artifacts live under:
  - `temp/android-debug/flash_mix_pulse_style_sweep_20260514/`

## Shared Flow

All Flash adb scenarios reuse the same core flow:

- Optionally apply `wb.lang` to switch the app language before scenario setup.
- Select Flash mode.
- Select the requested Flash voicing style.
- Resolve input text from `wb.input`, `wb.sample.length`, or `wb.sample.id`.
- Encode generated Flash audio.
- For `ui`, open player detail.
- For `ui`, optionally switch player detail to the requested `wb.display` page.
- For `ui`, optionally switch the Flash visualizer to the requested `wb.visual` mode.
- Start playback.
- Stop playback after the configured short capture window unless `wb.play.ms=0`.

This shared flow is the default contract. Extra diagnostics should be added around it, not by creating a separate manual tapping path.

For lyrics layout measurement, the UI scenario now forces a clean debug baseline before encode/playback:

- `Demo mode = off`
- `Visual perf overlay = off`

This reset is intentional. Demo mode only adds tap-feedback animation, but measurement captures still disable it to avoid inheriting any debug-only visual noise from device-local settings state.

## Mode-specific Observability

Visual mode:

- `FlashAutomation displayModeApplied ... mode=visual` confirms the sheet switched to Visual.
- `FlashVisualPerf` is the primary rendering/perf stream.
- `FlashAlignmentPerf` remains the preferred sync stream for Visual, readout, and Lyrics together.
- `PlaybackLyricsLayout mode=visual surface=preview` measures the compact lyrics preview shown below the Visual page.

Mix mode:

- `FlashAutomation displayModeApplied ... mode=mix` confirms the sheet switched to Mix.
- `FlashAlignmentPerf` is the preferred sync stream for Mix because the token strip reuses the same shared Flash playback sample as the Visual page.
- `lane / row / token` on-screen alignment summary is available when `Settings > Debug > Visual perf overlay` is enabled and the Flash visual mode is `Lanes`.
- The `Lanes` overlay also draws the current bit span boundary so agents can visually check whether the playhead is touching the bit's left edge when `8 bits` and token `hex/bin` advance.

Lyrics mode:

- `FlashAutomation displayModeApplied ... mode=lyrics` confirms the sheet switched to Lyrics.
- `FlashLyricsPerf` remains the primary token-follow stream.
- `PlaybackLyricsLayout mode=lyrics surface=token-strip` measures the main Lyrics tokenizer strip.
- `PlaybackLyricsLayout mode=lyrics surface=preview` measures the compact preview that still remains below the Lyrics page.

Headless mode:

- `FlashHeadless` remains the non-UI diagnostic stream.
- `wb.display` is still parsed, but there is no player-detail display page to switch in headless captures.

## Layout Measurement

`PlaybackLyricsLayout` is the shared layout-measurement log for Flash detail pages.

`PlaybackVerticalLayout` is the shared vertical-chain measurement log for Flash player detail. It is emitted by the existing Flash UI scenarios, including `wb.display=visual`, so agents can quantify the remaining gap between the display block and the bottom playback dock before changing lyrics height behavior.

`PlaybackPulseLayout` measures the `Visual + Pulse` overlay geometry. Use it when changing Pulse lane spacing or height without changing the outer visual card size.

The Visual perf overlay now serves two purposes:

- `FlashVisualPerf` FPS/motion metrics in the white debug card.
- `Lanes`-specific alignment observation when the selected visual mode is `Lanes`:
  - current active bit left/right boundary overlay inside the visual card
  - summary text for `lane`, `row`, and `token` current `globalBit:value`
  - current token `hex/bin` snapshot for Mix diagnosis

Before any Flash lyrics-size capture, the debug scenario logs:

- `FlashAutomation languageApplied ... language=<tag>` when `wb.lang` is present.
- `FlashAutomation measurementBaselineReset ... demoMode=false visualPerfOverlay=false`

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

`PlaybackVerticalLayout` currently reports:

- `rootHeightDp`
- `scrollHeightDp`
- `displayHeightDp`
- `bottomDockHeightDp`
- `displayBottomToScrollBottomDp`
- `displayBottomToDockTopDp`
- `scrollTopDp`
- `displayTopDp`
- `dockTopDp`

Recommended device prep:

```powershell
python tools/run.py android-debug device-prep
```

Default `capture-flash` artifacts:

- `raw.log`
- `summary.md`
- `crash-summary.txt`

If `--output-dir` is omitted, the tool creates:

- `temp/android-debug/<timestamp>-flash-<scenario>-<style>/`

USB debugging alone is not enough for visual/perf validation. The device must be unlocked and able to draw the foreground app; otherwise the scenario can receive the intent and encode audio without producing visual draw logs.

Recommended UI log capture in a second shell:

```powershell
adb logcat -c
adb logcat -v time FlashAutomation:D FlipBitsLongAudio:D GeneratedAudioCache:D PlaybackAutomation:D FlashAlignmentPerf:D FlashVisualPerf:D FlashLyricsPerf:D AndroidRuntime:E libc:E *:S > temp\log.txt
```

`FlashAutomation` confirms the scenario was received. `FlashVisualPerf` provides the playback and visual timing metrics used for diagnosis.
`FlashLyricsPerf` records the real Lyrics token-tape state derived from `textTokenTimeline` and the current displayed sample.
`FlashAlignmentPerf` is the preferred sync diagnostic: it samples Visual, 8-bit readout, and Lyrics token state through one debug-only throttled trace, so agents do not need to pair separate log rows manually.
`PlaybackLyricsLayout` records the actual lyrics container size for each measured surface.

Note the difference between the wrapper and fully custom logcat capture:

- `capture-flash` uses a fixed filter set and then builds `summary.md` from the parsed Flash events
- logs such as `PlaybackPulseLayout` or future one-off debug tags are not automatically included unless the tool filter list is updated
- for geometry/layout investigations outside the default filter set, prefer a manual `adb logcat ... > temp/...` capture

Useful `FlashAlignmentPerf` fields for current Flash alignment work:

- `readoutGlobalBit` / `readoutCurrentBitValue`
- `visualGlobalBit` / `visualCurrentBitValue`
- `cardGlobalBitOffset` / `cardCurrentBitValue`
- `cardByteHex` / `cardByteBinary`
- `globalBitMinusCardBit`

These fields are the quickest way to tell whether the current disagreement is:

- true timing/offset drift
- bit-value mapping mismatch
- or only a visual-language mismatch between `Visual`, `8 bits`, and token `hex/bin`

For quick regression, do not wait for complete playback. Stop after the first few `FlashVisualPerf` rows show `playing=true`, `bitReadout=true`, and `fallback=false`.

## Success Criteria

General capture success:

- `FlashAutomation` confirms the scenario was received.
- `FlashAutomation state` shows the generated Flash source became active.
- `FlashAutomation displayModeApplied` matches the requested `wb.display` for UI scenarios.

Lyrics/token success criteria:

- `FlashLyricsPerf` rows show `playing=true`.
- `sample` advances with playback.
- `token` and `tokenText` change as `sample` crosses `tokenStart` / `tokenEnd`.
- `tokenProgress` stays in `0.00..1.00` while a token is active.
- `byte` / `bit` move forward inside the active token for Flash payloads.
- Prefer `FlashAlignmentPerf visualSample/lyricsSample/sampleDelta/readoutBit/visualBit/lyricBit/bitDelta` when checking visual, 8-bit row, and Lyrics alignment.
- Compare with `FlashVisualPerf readoutSample/readoutBit/visualBit` only when investigating rendering performance or when `FlashAlignmentPerf` is missing.
- In `Mix + Lanes`, also confirm the on-screen overlay summary shows the same `bit:value` for `lane`, `row`, and `token` when the vertical playhead touches the highlighted bit's left boundary.

Layout-measurement success:

- `PlaybackLyricsLayout` appears for the requested page/surface.
- `mode=visual surface=preview` appears when `wb.display=visual`.
- `mode=lyrics surface=token-strip` appears when `wb.display=lyrics`.
- `mode=lyrics surface=preview` appears when `wb.display=lyrics`.
- The measured dimensions are taken from the current real device/layout, not inferred from screenshots.

Filtered visual + Lyrics summary:

```powershell
adb logcat -d -v time FlashAutomation:D FlashAlignmentPerf:D FlashVisualPerf:D FlashLyricsPerf:D *:S > temp\flash_alignment_raw.log
python tools/run.py android-debug flash-summary temp\flash_alignment_raw.log --output temp\flash_alignment_summary.md
```

Agents should read the generated summary first. It keeps the useful `FlashAutomation`, `FlashAlignmentPerf`, `FlashVisualPerf`, and `FlashLyricsPerf` rows. When `FlashAlignmentPerf` is present, the summary uses those unified samples first; otherwise it falls back to pairing each Lyrics sample with the nearest visual readout sample.

Recommended headless log capture:

```powershell
adb logcat -c
adb logcat -v time FlashAutomation:D FlashHeadless:D FlipBitsLongAudio:D GeneratedAudioCache:D PlaybackAutomation:D AndroidRuntime:E libc:E *:S > temp\log.txt
```

Headless success criteria:

- `FlashAutomation` shows `scenario=headless`.
- `FlashAutomation inputResolved` shows `source`, `sampleId`, `chars`, and `payloadBytes`.
- `FlipBitsLongAudio applySuccess:begin` shows `pcmSamples > 0`.
- `FlashAutomation state` shows `source=generated:flash`, `miniPlayer=true`, and a non-zero `miniDurationMs`.
- `FlashHeadless start` shows `follow=true` and `binaryGroups > 0`.
- `FlashHeadless tick` shows `playing=true`, advancing `sample`, and non-negative `bit` / `revealed` values after playback enters the bit timeline.
- `FlashHeadless stop` appears after the configured `wb.play.ms`.

Headless does not validate Compose drawing, edge fades, visual bar spacing, clipping, or layout. Use `ui` for those.

All-style fast sweep example:

```powershell
$styles = @("standard", "hostile", "litany", "collapse", "zeal", "void")
foreach ($style in $styles) {
    adb logcat -c
    adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.scenario ui --es wb.flash.style $style --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
    Start-Sleep -Seconds 12
    adb logcat -d -v time FlashAutomation:D FlashAlignmentPerf:D FlashVisualPerf:D FlashLyricsPerf:D *:S
}
```

Pangram single-style example:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.input "The quick brown fox jumps over the lazy dog." --es wb.flash.style litany --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Built-in long-sample Litany diagnostic:

```powershell
python tools/run.py android-debug capture-flash --scenario headless --style litany --sample-length long --play-ms 6000 --wait-ms 90000
```

Use `ui` instead of `headless` when validating the mini player, player detail sheet, Visual, and Lyrics UI together:

```powershell
python tools/run.py android-debug capture-flash --scenario ui --style litany --sample-length long --visual lanes --wait-ms 90000
```

`capture-flash --scenario ui` defaults to `--play-ms 30000` so Litany's longer lead-in has time to enter Visual/Lyrics token activity. Headless captures keep the shorter `6000` ms default unless `--play-ms` is passed explicitly.

## Instrumentation Test

The real-device regression entry is:

```text
apps/audio_android/app/src/androidTest/java/com/bag/audioandroid/ui/FlashDebugScenarioInstrumentedTest.kt
```

It launches `MainActivity` with the debug Flash scenario and parameterizes across all Flash voicing styles. The test waits for:

- `player-detail-sheet-content`
- `playback-display-section`
- `flash-visualization-mode-switcher`
- `flash-visualization-mode-lanes`

Compile validation:

```powershell
cd apps\audio_android
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

Run on a connected device:

```powershell
cd apps\audio_android
.\gradlew.bat :app:connectedDebugAndroidTest --tests com.bag.audioandroid.ui.FlashDebugScenarioInstrumentedTest
```

## Stable UI Tags

Current stable tags added for Flash automation:

- `audio-mode-flash`
- `audio-input-text-field`
- `audio-encode-button`
- `flash-voicing-style-selector`
- `flash-voicing-style-<styleId>`
- `audio-playback-toggle`
- `player-detail-sheet-content`
- `playback-display-section`
- `playback-display-lyrics`
- `playback-display-visual`
- `playback-display-mix`
- `flash-visualization-mode-switcher`
- `flash-visualization-mode-lanes`
- `flash-visualization-mode-pulse`
- `flash-visualization-mode-pitch`
- `flash-visual-pulse-tape`
- `flash-visual-lanes-alignment-overlay`
- `flash-visual-lanes-alignment-summary`

Prefer these tags in instrumentation tests. For agent/adb debugging, prefer the debug scenario action instead of coordinate taps or accessibility-service automation.

## Maintenance Rules

- Keep the fast-regression default text short unless the test target changes intentionally.
- Add new Flash styles to both `FlashVoicingStyleOption` and the automation coverage notes.
- If the visual mode names change, update the accepted `wb.visual` aliases and stable tags together.
- Debug scenario behavior must stay behind `BuildConfig.DEBUG`.
- When a visual bug is timing-related, capture `FlashAutomation`, `FlashAlignmentPerf`, `FlashVisualPerf`, and `FlashLyricsPerf` together before widening the fix.
