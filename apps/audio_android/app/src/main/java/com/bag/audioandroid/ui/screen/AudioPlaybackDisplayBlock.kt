package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState

@Composable
internal fun AudioPlaybackDisplayBlock(
    displayedSamples: Int,
    visualDisplayedSamples: Int = displayedSamples,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean = false,
    sampleRateHz: Int,
    transportMode: TransportModeOption?,
    frameSamples: Int,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    isPlaying: Boolean,
    isScrubbing: Boolean = false,
    isFlashVisualPerfOverlayEnabled: Boolean = false,
    playbackSpeed: Float = 1f,
    displaySectionState: PlaybackDisplaySectionState,
    extraLyricsRecoveryHeight: Dp = 0.dp,
    applyLyricsPreviewBonusLine: Boolean = false,
    modifier: Modifier = Modifier,
    onSeekToSample: (Int) -> Unit = {},
) {
    PlaybackDisplaySection(
        followData = followData,
        flashVisualWindow = flashVisualWindow,
        isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
        displayedSamples = displayedSamples,
        visualDisplayedSamples = visualDisplayedSamples,
        waveformPcm = waveformPcm,
        isWaveformPreview = isWaveformPreview,
        sampleRateHz = sampleRateHz,
        transportMode = transportMode,
        frameSamples = frameSamples,
        isFlashMode = isFlashMode,
        flashVoicingStyle = flashVoicingStyle,
        isPlaying = isPlaying,
        isScrubbing = isScrubbing,
        playbackSpeed = playbackSpeed,
        playbackDisplayMode = displaySectionState.playbackDisplayMode,
        flashVisualizationModeName = displaySectionState.flashVisualizationModeName,
        lyricsExpanded = displaySectionState.lyricsExpanded,
        extraLyricsRecoveryHeight = extraLyricsRecoveryHeight,
        applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
        onDisplayModeSelected = displaySectionState.onDisplayModeSelected,
        onFlashVisualizationModeSelected = displaySectionState.onFlashVisualizationModeSelected,
        onLyricsExpandedChanged = displaySectionState.onLyricsExpandedChanged,
        onSeekToSample = onSeekToSample,
        modifier = modifier,
    )
}
