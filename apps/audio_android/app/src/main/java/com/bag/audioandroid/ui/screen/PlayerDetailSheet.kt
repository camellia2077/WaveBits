package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.PlaybackSequenceMode

@Composable
internal fun PlayerDetailSheetContent(
    miniPlayerModel: MiniPlayerUiModel,
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    waveformPcm: ShortArray,
    sampleRateHz: Int,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    canExportGeneratedAudio: Boolean,
    isCodecBusy: Boolean,
    decodedText: String?,
    savedAudioItem: SavedAudioItem?,
    onTogglePlayback: () -> Unit,
    onDecodeAudio: () -> Unit,
    onClearDecodedText: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onExportGeneratedAudio: () -> Unit,
    onShareSavedAudio: (() -> Unit)?,
    onOpenSavedAudioSheet: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isFlashMode = when (miniPlayerModel) {
        is MiniPlayerUiModel.Generated ->
            miniPlayerModel.mode == com.bag.audioandroid.ui.model.TransportModeOption.Flash
        is MiniPlayerUiModel.Saved ->
            miniPlayerModel.modeWireName == "flash"
    }
    val flashVoicingStyle: FlashVoicingStyleOption? = when (miniPlayerModel) {
        is MiniPlayerUiModel.Generated -> miniPlayerModel.flashVoicingStyle
        is MiniPlayerUiModel.Saved -> miniPlayerModel.flashVoicingStyle
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PlayerDetailSummarySection(
            miniPlayerModel = miniPlayerModel,
            canExportGeneratedAudio = canExportGeneratedAudio,
            onExportGeneratedAudio = onExportGeneratedAudio,
            onShareSavedAudio = onShareSavedAudio
        )
        if (miniPlayerModel is MiniPlayerUiModel.Generated ||
            (miniPlayerModel is MiniPlayerUiModel.Saved && miniPlayerModel.modeWireName == "flash")) {
            PlayerDetailDecodedSection(
                decodedText = decodedText.orEmpty(),
                isCodecBusy = isCodecBusy,
                onDecodeAudio = onDecodeAudio,
                onClearDecodedText = onClearDecodedText
            )
        }
        AudioPlaybackProgressSection(
            displayedSamples = displayedSamples,
            totalSamples = totalSamples,
            isScrubbing = isScrubbing,
            waveformPcm = waveformPcm,
            sampleRateHz = sampleRateHz,
            isFlashMode = isFlashMode,
            flashVoicingStyle = flashVoicingStyle,
            displayedTime = displayedTime,
            totalTime = totalTime,
            isPlaying = isPlaying,
            onScrubStarted = onScrubStarted,
            onScrubChanged = onScrubChanged,
            onScrubFinished = onScrubFinished
        )
        AudioPlaybackTransportControls(
            isPlaying = isPlaying,
            playbackSequenceMode = playbackSequenceMode,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
            onOpenSavedAudioSheet = onOpenSavedAudioSheet
        )
        if (miniPlayerModel is MiniPlayerUiModel.Saved) {
            savedAudioItem?.let { PlayerDetailSavedInfoSection(item = it) }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
