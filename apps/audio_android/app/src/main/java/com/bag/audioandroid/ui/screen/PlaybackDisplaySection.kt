package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.playerSegmentedButtonColors

@Composable
internal fun PlaybackDisplaySection(
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
    isPlaying: Boolean,
    playbackDisplayMode: PlaybackDisplayMode,
    flashVisualizationModeName: String,
    onDisplayModeSelected: (PlaybackDisplayMode) -> Unit,
    onFlashVisualizationModeSelected: (FlashSignalVisualizationMode) -> Unit,
    onSeekToSample: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val visualizationRoute =
        resolvePlaybackVisualizationRoute(
            transportMode = transportMode,
            isFlashMode = isFlashMode,
            waveformPcm = waveformPcm,
            isWaveformPreview = isWaveformPreview,
            sampleRateHz = sampleRateHz,
            visualDisplayedSamples = visualDisplayedSamples,
            displayedSamples = displayedSamples,
            followData = followData,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-display-section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("playback-display-switcher"),
        ) {
            PlaybackDisplayMode.entries.forEachIndexed { index, option ->
                val optionLabel = stringResource(option.titleResId)
                SegmentedButton(
                    selected = playbackDisplayMode == option,
                    onClick = { onDisplayModeSelected(option) },
                    modifier =
                        Modifier
                            .testTag("playback-display-${option.name.lowercase()}")
                            .semantics { contentDescription = optionLabel },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PlaybackDisplayMode.entries.size,
                        ),
                    colors = playerSegmentedButtonColors(),
                    label = { Text(text = optionLabel) },
                )
            }
        }
        if (playbackDisplayMode == PlaybackDisplayMode.Visual) {
            if (waveformPcm.isNotEmpty()) {
                when (val route = visualizationRoute) {
                    PlaybackVisualizationRoute.PcmWaveform ->
                        AudioPcmWaveform(
                            pcm = waveformPcm,
                            sampleRateHz = sampleRateHz,
                            displayedSamples = visualDisplayedSamples,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxWidth(),
                        )

                    is PlaybackVisualizationRoute.SymbolEnvelope ->
                        AudioSymbolEnvelopeVisualizer(
                            pcm = waveformPcm,
                            sampleRateHz = sampleRateHz,
                            displayedSamples = visualDisplayedSamples,
                            isPlaying = isPlaying,
                            transportMode = route.transportMode,
                            frameSamples = frameSamples,
                            modifier = Modifier.fillMaxWidth(),
                        )

                    is PlaybackVisualizationRoute.FlashSignal -> {
                        val flashVisualizationMode =
                            FlashSignalVisualizationMode.values().firstOrNull { mode ->
                                mode.name == flashVisualizationModeName
                            } ?: FlashSignalVisualizationMode.ToneTracks
                        FlashSignalVisualizationModeSwitcher(
                            selectedMode = flashVisualizationMode,
                            onModeSelected = onFlashVisualizationModeSelected,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AudioFlashSignalVisualizer(
                            input = route.input,
                            isPlaying = isPlaying,
                            mode = flashVisualizationMode,
                            flashVoicingStyle = flashVoicingStyle,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    PlaybackVisualizationRoute.ProExplanation ->
                        ProEncodingExplanationVisualizer(
                            followData = followData,
                            displayedSamples = displayedSamples,
                            frameSamples = frameSamples,
                            modifier = Modifier.fillMaxWidth(),
                        )

                    PlaybackVisualizationRoute.UltraStep ->
                        UltraSymbolStepVisualizer(
                            pcm = waveformPcm,
                            sampleRateHz = sampleRateHz,
                            displayedSamples = visualDisplayedSamples,
                            frameSamples = frameSamples,
                            modifier = Modifier.fillMaxWidth(),
                        )

                    PlaybackVisualizationRoute.MorseTimeline ->
                        MorseTimelineVisualizer(
                            followData = followData,
                            displayedSamples = displayedSamples,
                            frameSamples = frameSamples,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxWidth(),
                        )
                }
            }
        } else {
            // The playback area mirrors a music player: visual mode works like album art,
            // while lyrics mode hands off to the formal line-timeline lyric page.
            PlaybackDataFollowSection(
                followData = followData,
                displayedSamples = displayedSamples,
                transportMode = transportMode,
                onSeekToSample = onSeekToSample,
            )
        }
        PlaybackTokenContextTape(
            followData = followData,
            displayedSamples = displayedSamples,
            visibleLineCount = if (playbackDisplayMode == PlaybackDisplayMode.Visual) 5 else 2,
            onSeekToSample = onSeekToSample,
            modifier =
                if (playbackDisplayMode == PlaybackDisplayMode.Lyrics) {
                    Modifier.padding(top = 12.dp)
                } else {
                    Modifier
                },
        )
    }
}
