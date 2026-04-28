package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun PlayerDetailSheetContent(
    miniPlayerModel: MiniPlayerUiModel,
    displayedSamples: Int,
    waveformDisplayedSamples: Int = displayedSamples,
    totalSamples: Int,
    isScrubbing: Boolean,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean = false,
    sampleRateHz: Int,
    frameSamples: Int = 2205,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    playbackSpeed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    canExportGeneratedAudio: Boolean,
    followData: PayloadFollowViewData,
    savedAudioItem: SavedAudioItem?,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onExportGeneratedAudio: () -> Unit,
    onExportGeneratedAudioToDocument: () -> Unit,
    onShareSavedAudio: (() -> Unit)?,
    onOpenSavedAudioSheet: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onLyricsRequested: () -> Unit = {},
    initialDisplayMode: PlaybackDisplayMode = PlaybackDisplayMode.Visual,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val displaySectionState =
        rememberPlaybackDisplaySectionState(
            isFlashMode = miniPlayerModel.isFlashMode,
            onLyricsRequested = onLyricsRequested,
            initialDisplayMode = initialDisplayMode,
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("player-detail-sheet-content"),
    ) {
        PlayerDetailScrollContent(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
            displayedSamples = displayedSamples,
            waveformDisplayedSamples = waveformDisplayedSamples,
            waveformPcm = waveformPcm,
            isWaveformPreview = isWaveformPreview,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            transportMode = miniPlayerModel.transportMode,
            isFlashMode = miniPlayerModel.isFlashMode,
            flashVoicingStyle = miniPlayerModel.flashVoicingStyle,
            followData = followData,
            isPlaying = isPlaying,
            displaySectionState = displaySectionState,
            savedAudioItem = savedAudioItem,
        )
        PlayerDetailBottomDock(
            modifier = Modifier.fillMaxWidth(),
            displayedSamples = displayedSamples,
            totalSamples = totalSamples,
            isScrubbing = isScrubbing,
            displayedTime = displayedTime,
            totalTime = totalTime,
            isPlaying = isPlaying,
            playbackSequenceMode = playbackSequenceMode,
            playbackSpeed = playbackSpeed,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            canExportGeneratedAudio = canExportGeneratedAudio,
            transportMode = miniPlayerModel.transportMode,
            durationMs = miniPlayerModel.durationMs,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            flashVoicingStyle = miniPlayerModel.flashVoicingStyle,
            savedAudioItem = savedAudioItem,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
            onPlaybackSpeedSelected = onPlaybackSpeedSelected,
            onExportGeneratedAudio = onExportGeneratedAudio,
            onExportGeneratedAudioToDocument = onExportGeneratedAudioToDocument,
            onOpenSavedAudioSheet = onOpenSavedAudioSheet,
            onScrubStarted = onScrubStarted,
            onScrubChanged = onScrubChanged,
            onScrubFinished = onScrubFinished,
        )
    }
}

@Composable
private fun PlayerDetailScrollContent(
    displayedSamples: Int,
    waveformDisplayedSamples: Int,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean,
    sampleRateHz: Int,
    frameSamples: Int,
    transportMode: TransportModeOption,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    isPlaying: Boolean,
    displaySectionState: PlaybackDisplaySectionState,
    savedAudioItem: SavedAudioItem?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.padding(horizontal = PlayerDetailHorizontalPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AudioPlaybackDisplayBlock(
            displayedSamples = displayedSamples,
            visualDisplayedSamples = waveformDisplayedSamples,
            waveformPcm = waveformPcm,
            isWaveformPreview = isWaveformPreview,
            sampleRateHz = sampleRateHz,
            transportMode = transportMode,
            frameSamples = frameSamples,
            isFlashMode = isFlashMode,
            flashVoicingStyle = flashVoicingStyle,
            followData = followData,
            isPlaying = isPlaying,
            displaySectionState = displaySectionState,
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PlayerDetailBottomDock(
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    playbackSpeed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    canExportGeneratedAudio: Boolean,
    transportMode: TransportModeOption,
    durationMs: Long,
    sampleRateHz: Int,
    frameSamples: Int,
    flashVoicingStyle: FlashVoicingStyleOption?,
    savedAudioItem: SavedAudioItem?,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onExportGeneratedAudio: () -> Unit,
    onExportGeneratedAudioToDocument: () -> Unit,
    onOpenSavedAudioSheet: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = PlayerDetailHorizontalPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AudioPlaybackTimelineBlock(
            displayedSamples = displayedSamples,
            totalSamples = totalSamples,
            isScrubbing = isScrubbing,
            displayedTime = displayedTime,
            totalTime = totalTime,
            isPlaying = isPlaying,
            onScrubStarted = onScrubStarted,
            onScrubChanged = onScrubChanged,
            onScrubFinished = onScrubFinished,
        )
        AudioPlaybackTransportControls(
            isPlaying = isPlaying,
            playbackSequenceMode = playbackSequenceMode,
            playbackSpeed = playbackSpeed,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            canExportGeneratedAudio = canExportGeneratedAudio,
            transportMode = transportMode,
            durationMs = durationMs,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            flashVoicingStyle = flashVoicingStyle,
            savedAudioItem = savedAudioItem,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
            onPlaybackSpeedSelected = onPlaybackSpeedSelected,
            onExportGeneratedAudio = onExportGeneratedAudio,
            onExportGeneratedAudioToDocument = onExportGeneratedAudioToDocument,
            onOpenSavedAudioSheet = onOpenSavedAudioSheet,
        )
    }
}

private val PlayerDetailHorizontalPadding = 24.dp
