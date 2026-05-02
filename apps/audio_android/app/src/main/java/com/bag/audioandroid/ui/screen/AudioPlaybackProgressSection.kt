package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun AudioPlaybackProgressSection(
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    waveformPcm: ShortArray,
    sampleRateHz: Int,
    transportMode: TransportModeOption?,
    frameSamples: Int = 2205,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onLyricsRequested: () -> Unit = {},
    initialDisplayMode: PlaybackDisplayMode = PlaybackDisplayMode.Visual,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-progress-section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val displaySectionState =
            rememberPlaybackDisplaySectionState(
                isFlashMode = isFlashMode,
                onLyricsRequested = onLyricsRequested,
                initialDisplayMode = initialDisplayMode,
            )
        AudioPlaybackDisplayBlock(
            displayedSamples = displayedSamples,
            waveformPcm = waveformPcm,
            sampleRateHz = sampleRateHz,
            transportMode = transportMode,
            frameSamples = frameSamples,
            isFlashMode = isFlashMode,
            flashVoicingStyle = flashVoicingStyle,
            followData = followData,
            isPlaying = isPlaying,
            displaySectionState = displaySectionState,
            onSeekToSample = { targetSamples ->
                onScrubStarted()
                onScrubChanged(targetSamples)
                onScrubFinished()
            },
        )
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
    }
}
