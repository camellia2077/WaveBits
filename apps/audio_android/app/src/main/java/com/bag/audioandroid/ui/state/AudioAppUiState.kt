package com.bag.audioandroid.ui.state

import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette

data class AudioAppUiState(
    val selectedTab: AppTab = AppTab.Audio,
    val selectedLanguage: AppLanguageOption = AppLanguageOption.FollowSystem,
    val showAboutPage: Boolean = false,
    val showLicensesPage: Boolean = false,
    val presentationVersion: String = "",
    val coreVersion: String = "",
    val selectedPalette: PaletteOption = DefaultMaterialPalette,
    val selectedThemeMode: ThemeModeOption = ThemeModeOption.FollowSystem,
    val selectedFlashVoicingStyle: FlashVoicingStyleOption = FlashVoicingStyleOption.CodedBurst,
    val transportMode: TransportModeOption = TransportModeOption.Flash,
    val sessions: Map<TransportModeOption, ModeAudioSessionState> = defaultModeSessions(),
    val currentPlaybackSource: AudioPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Flash),
    val playbackSequenceMode: PlaybackSequenceMode = PlaybackSequenceMode.Normal,
    val selectedSavedAudio: SavedAudioPlaybackSelection? = null,
    val savedAudioItems: List<SavedAudioItem> = emptyList(),
    val librarySelection: LibrarySelectionUiState = LibrarySelectionUiState(),
    val showSavedAudioSheet: Boolean = false,
    val showPlayerDetailSheet: Boolean = false,
    val libraryStatusText: UiText = UiText.Empty,
    val snackbarMessage: SnackbarMessage? = null
) {
    val currentSession: ModeAudioSessionState
        get() = sessions.getValue(transportMode)

    val currentPlayback: PlaybackUiState
        get() = when (val source = currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).playback
            is AudioPlaybackSource.Saved -> selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?.playback
                ?: PlaybackUiState()
        }

    val currentPlaybackSampleCount: Int
        get() = when (val source = currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).generatedPcm.size
            is AudioPlaybackSource.Saved -> selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?.pcm
                ?.size
                ?: 0
        }

    val currentPlaybackPcm: ShortArray
        get() = when (val source = currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).generatedPcm
            is AudioPlaybackSource.Saved -> selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?.pcm
                ?: shortArrayOf()
        }

    val currentSavedAudioItem: SavedAudioItem?
        get() = when (val source = currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> null
            is AudioPlaybackSource.Saved -> selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?.item
        }

    val currentPlaybackDecodedText: String?
        get() = when (val source = currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).resultText
            is AudioPlaybackSource.Saved -> selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?.decodedText
        }

    val miniPlayerModel: MiniPlayerUiModel?
        get() = when (val source = currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> {
                val session = sessions.getValue(source.mode)
                if (session.generatedPcm.isEmpty()) {
                    null
                } else {
                    MiniPlayerUiModel.Generated(
                        mode = source.mode,
                        flashVoicingStyle = session.generatedFlashVoicingStyle,
                        durationMs = samplesToDurationMillis(
                            sampleCount = currentPlayback.totalSamples.takeIf { it > 0 } ?: session.generatedPcm.size,
                            sampleRateHz = currentPlayback.sampleRateHz
                        )
                    )
                }
            }

            is AudioPlaybackSource.Saved -> currentSavedAudioItem?.let { item ->
                MiniPlayerUiModel.Saved(
                    displayName = item.displayName,
                    modeWireName = item.modeWireName,
                    flashVoicingStyle = item.flashVoicingStyle,
                    durationMs = item.durationMs
                )
            }
        }

    val canSkipPrevious: Boolean
        get() {
            val currentItemId = currentSavedAudioItem?.itemId ?: return false
            val currentIndex = savedAudioItems.indexOfFirst { it.itemId == currentItemId }
            return currentIndex > 0
        }

    val canSkipNext: Boolean
        get() {
            val currentItemId = currentSavedAudioItem?.itemId ?: return false
            val currentIndex = savedAudioItems.indexOfFirst { it.itemId == currentItemId }
            return currentIndex >= 0 && currentIndex < savedAudioItems.lastIndex
        }
}

private fun defaultModeSessions(): Map<TransportModeOption, ModeAudioSessionState> =
    TransportModeOption.entries.associateWith { ModeAudioSessionState() }

private fun samplesToDurationMillis(sampleCount: Int, sampleRateHz: Int): Long {
    if (sampleCount <= 0 || sampleRateHz <= 0) {
        return 0L
    }
    return (sampleCount.toLong() * 1000L) / sampleRateHz.toLong()
}
