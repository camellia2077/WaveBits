package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
    waveformPcm: ShortArray,
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
    modifier: Modifier = Modifier,
) {
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
                when (transportMode) {
                    TransportModeOption.Flash -> {
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
                            pcm = waveformPcm,
                            sampleRateHz = sampleRateHz,
                            displayedSamples = displayedSamples,
                            isPlaying = isPlaying,
                            mode = flashVisualizationMode,
                            flashVoicingStyle = flashVoicingStyle,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    TransportModeOption.Pro, TransportModeOption.Ultra ->
                        if (transportMode == TransportModeOption.Pro) {
                            ProEncodingExplanationVisualizer(
                                followData = followData,
                                displayedSamples = displayedSamples,
                                frameSamples = frameSamples,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            UltraSymbolStepVisualizer(
                                pcm = waveformPcm,
                                sampleRateHz = sampleRateHz,
                                displayedSamples = displayedSamples,
                                frameSamples = frameSamples,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                    null ->
                        if (isFlashMode) {
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
                                pcm = waveformPcm,
                                sampleRateHz = sampleRateHz,
                                displayedSamples = displayedSamples,
                                isPlaying = isPlaying,
                                mode = flashVisualizationMode,
                                flashVoicingStyle = flashVoicingStyle,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            AudioPcmWaveform(
                                pcm = waveformPcm,
                                sampleRateHz = sampleRateHz,
                                displayedSamples = displayedSamples,
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                }
            }
        } else {
            // The playback area mirrors a music player: visual mode works like album art,
            // while lyrics mode hands off to the formal line-timeline lyric page.
            PlaybackDataFollowSection(
                followData = followData,
                displayedSamples = displayedSamples,
            )
        }
        PlaybackTokenContextTape(
            followData = followData,
            displayedSamples = displayedSamples,
            visibleLineCount = if (playbackDisplayMode == PlaybackDisplayMode.Visual) 5 else 3,
        )
    }
}
