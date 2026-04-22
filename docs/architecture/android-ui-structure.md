# Android UI Structure

## Purpose

This document explains where Android presentation responsibilities live inside `apps/audio_android`.
Use it when deciding where a new change belongs before editing large files.

## Main Layers

### App shell

Primary files:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidApp.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidAppShell.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidMainScaffold.kt`

Responsibilities:

- choose theme family and active color scheme
- provide app-wide composition locals
- host top-level sheets, tabs, and player scaffold

### ViewModel and action split

Primary files:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidViewModel.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidChromeActions.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidSessionActions.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidLibraryActions.kt`

Responsibilities:

- own UI state mutations
- collect preferences and repository results
- route user intent into session, library, and chrome flows

### Screen layer

Primary directory:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/`

Responsibilities:

- render state
- emit callbacks
- avoid owning business policy that belongs in actions, gateways, or shared theme helpers

### Theme mappings and shared UI tokens

Primary files:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidThemeMappings.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/theme/AppThemeAccentTokens.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/theme/BrandThemeCatalog.kt`

Responsibilities:

- convert selected theme mode into reusable UI color logic
- expose shared helpers for dock, player controls, and navigation
- prevent component-level color drift

## Player-Specific Ownership

### Player scaffold and dock

Primary files:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/PlayerScaffold.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/MiniPlayerBar.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidBottomBar.kt`

Responsibilities:

- host bottom playback dock
- keep mini player and bottom nav visually aligned
- provide layout padding so page content can coexist with the dock

### Player detail and playback area

Primary files:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlayerDetailSheet.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/AudioPlaybackProgressSection.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/AudioPlaybackTransportControls.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlaybackDataFollowSection.kt`

Responsibilities:

- render waveform / flash visualizer / lyrics follow
- render playback transport chrome
- render text/raw follow UI

### Shared player color helpers

Primary file:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidThemeMappings.kt`

Current helpers:

- `playerDockContainerColor(uiState)`
- `playerSegmentedButtonColors()`
- `playerChromeColors()`

Rule:

- new player controls should reuse these helpers before introducing local color logic

## What Belongs Outside UI

Do not place these in screen files when a shared or lower layer exists:

- decode/encode business policy
- metadata parsing rules
- transport-mode payload logic
- JNI mapping details
- theme-family color policy that affects multiple player components

Those belong in:

- action files
- data gateways
- JNI/native bridge files
- shared theme mapping helpers

## Documentation Split

For Android docs:

- `apps/audio_android/AGENTS.md`
  - thin index and hard rules for agents
- `apps/audio_android/README.md`
  - human-facing entrypoint and navigation
- `docs/design/android/...`
  - UI/visual/design intent
- `docs/architecture/...`
  - structure, ownership, and flow boundaries
