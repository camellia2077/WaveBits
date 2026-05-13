package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.playerSegmentedButtonColors
import com.bag.audioandroid.ui.state.FlashVisualWindowState

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
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    isPlaying: Boolean,
    isScrubbing: Boolean,
    isFlashVisualPerfOverlayEnabled: Boolean = false,
    playbackSpeed: Float = 1f,
    playbackDisplayMode: PlaybackDisplayMode,
    flashVisualizationModeName: String,
    lyricsExpanded: Boolean,
    extraLyricsRecoveryHeight: Dp = 0.dp,
    applyLyricsPreviewBonusLine: Boolean = false,
    onDisplayModeSelected: (PlaybackDisplayMode) -> Unit,
    onFlashVisualizationModeSelected: (FlashSignalVisualizationMode) -> Unit,
    onLyricsExpandedChanged: (Boolean) -> Unit,
    onSeekToSample: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var tokenStripHeightDp by remember(playbackDisplayMode, transportMode) { mutableStateOf<Float?>(null) }
    var stableTokenStripHeightDp by remember(playbackDisplayMode, transportMode) { mutableStateOf<Float?>(null) }
    val layoutModel =
        rememberPlaybackDisplayLayoutModel(
            transportMode = transportMode,
            isFlashMode = isFlashMode,
            waveformPcm = waveformPcm,
            isWaveformPreview = isWaveformPreview,
            sampleRateHz = sampleRateHz,
            visualDisplayedSamples = visualDisplayedSamples,
            displayedSamples = displayedSamples,
            followData = followData,
        )
    val sharedFlashPlaybackSampleState =
        rememberMixFlashPlaybackSampleState(
            playbackDisplayMode = playbackDisplayMode,
            visualizationRoute = layoutModel.visualizationRoute,
            displayedSamples = displayedSamples,
            isPlaying = isPlaying,
            isScrubbing = isScrubbing,
            playbackSpeed = playbackSpeed,
            sampleRateHz = sampleRateHz,
        )
    val followSectionDisplayedSamples =
        playbackFollowSectionDisplayedSamples(
            playbackDisplayMode = playbackDisplayMode,
            displayedSamples = displayedSamples,
            visualDisplayedSamples = visualDisplayedSamples,
            sharedFlashPlaybackSampleState = sharedFlashPlaybackSampleState,
        )
    if (transportMode == TransportModeOption.Mini) {
        MiniAlignmentPerfTrace.record(
            followData = followData,
            isPlaying = isPlaying,
            visualSample = displayedSamples,
            lyricsSample = visualDisplayedSamples,
            frameSamples = frameSamples,
            speed = MorseSpeedOption.fromFrameSamples(frameSamples).name.lowercase(),
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-display-section"),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("playback-display-switcher"),
        ) {
            layoutModel.displayModeOptions.forEachIndexed { index, option ->
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
                            count = layoutModel.displayModeOptions.size,
                        ),
                    colors = playerSegmentedButtonColors(),
                    label = { Text(text = optionLabel) },
                )
            }
        }
        Spacer(modifier = Modifier.height(if (playbackDisplayMode == PlaybackDisplayMode.Lyrics) 6.dp else 10.dp))
        if (playbackDisplayMode == PlaybackDisplayMode.Visual || playbackDisplayMode == PlaybackDisplayMode.Mix) {
            PlaybackVisualizationContent(
                waveformPcm = waveformPcm,
                sampleRateHz = sampleRateHz,
                visualDisplayedSamples = visualDisplayedSamples,
                displayedSamples = displayedSamples,
                frameSamples = frameSamples,
                isPlaying = isPlaying,
                flashVisualizationModeName = flashVisualizationModeName,
                flashVoicingStyle = flashVoicingStyle,
                flashVisualWindow = flashVisualWindow,
                isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
                playbackSpeed = playbackSpeed,
                isScrubbing = isScrubbing,
                followData = followData,
                visualizationRoute = layoutModel.visualizationRoute,
                sharedFlashPlaybackSampleState = sharedFlashPlaybackSampleState,
                onFlashVisualizationModeSelected = onFlashVisualizationModeSelected,
            )
        }
        if (playbackDisplayMode == PlaybackDisplayMode.Mix) {
            Spacer(modifier = Modifier.height(14.dp))
        }
        if (playbackDisplayMode == PlaybackDisplayMode.Lyrics || playbackDisplayMode == PlaybackDisplayMode.Mix) {
            // The playback area mirrors a music player: visual mode works like album art,
            // while lyrics mode hands off to the formal line-timeline lyric page.
            PlaybackDataFollowSection(
                followData = followData,
                displayedSamples = followSectionDisplayedSamples,
                transportMode = transportMode,
                contentSpacing = if (playbackDisplayMode == PlaybackDisplayMode.Mix) 6.dp else 10.dp,
                onTokenStripHeightDpChanged = { heightDp ->
                    tokenStripHeightDp = heightDp
                    stableTokenStripHeightDp =
                        when (val currentStable = stableTokenStripHeightDp) {
                            null -> heightDp
                            else -> maxOf(currentStable, heightDp)
                        }
                },
                onSeekToSample = onSeekToSample,
            )
        }
        if (playbackDisplayMode != PlaybackDisplayMode.Mix) {
            Spacer(modifier = Modifier.height(if (playbackDisplayMode == PlaybackDisplayMode.Lyrics) 6.dp else 10.dp))
            ExpandablePlaybackLyricsSection(
                followData = followData,
                displayedSamples = displayedSamples,
                isPlaying = isPlaying,
                transportMode = transportMode,
                playbackDisplayMode = playbackDisplayMode,
                lyricsExpanded = lyricsExpanded,
                tokenStripHeightDp =
                    if (transportMode?.supportsSharedTokenPage() == true) {
                        stableTokenStripHeightDp ?: tokenStripHeightDp
                    } else {
                        tokenStripHeightDp
                    },
                extraLyricsRecoveryHeight = extraLyricsRecoveryHeight,
                applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
                onLyricsExpandedChanged = onLyricsExpandedChanged,
                onSeekToSample = onSeekToSample,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal fun playbackFollowSectionDisplayedSamples(
    playbackDisplayMode: PlaybackDisplayMode,
    displayedSamples: Int,
    visualDisplayedSamples: Int,
    sharedFlashPlaybackSampleState: FlashVisualPlaybackSampleState? = null,
): Int =
    if (playbackDisplayMode == PlaybackDisplayMode.Mix) {
        sharedFlashPlaybackSampleState?.displayedSample?.toInt() ?: visualDisplayedSamples
    } else {
        displayedSamples
    }

@Composable
private fun rememberMixFlashPlaybackSampleState(
    playbackDisplayMode: PlaybackDisplayMode,
    visualizationRoute: PlaybackVisualizationRoute,
    displayedSamples: Int,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    playbackSpeed: Float,
    sampleRateHz: Int,
): FlashVisualPlaybackSampleState? {
    val flashRoute = visualizationRoute as? PlaybackVisualizationRoute.FlashSignal
    val followTimelineSource = flashRoute?.input?.bucketSource as? FlashSignalBucketSource.FollowTimeline
    val totalSamples =
        followTimelineSource
            ?.followData
            ?.totalPcmSampleCount
            ?.coerceAtLeast(displayedSamples)
            ?.coerceAtLeast(1)
            ?: return null
    if (playbackDisplayMode != PlaybackDisplayMode.Mix) {
        return null
    }
    if (isScrubbing) {
        return null
    }
    return rememberFlashVisualPlaybackSampleState(
        rawSample = followTimelineSource.displayedSamples.toFloat(),
        isPlaying = isPlaying,
        playbackSpeed = playbackSpeed,
        sampleRateHz = sampleRateHz,
        totalSamples = totalSamples,
    )
}

@Composable
private fun PlaybackVisualizationContent(
    waveformPcm: ShortArray,
    sampleRateHz: Int,
    visualDisplayedSamples: Int,
    displayedSamples: Int,
    frameSamples: Int,
    isPlaying: Boolean,
    flashVisualizationModeName: String,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState,
    isFlashVisualPerfOverlayEnabled: Boolean,
    playbackSpeed: Float,
    isScrubbing: Boolean,
    followData: PayloadFollowViewData,
    visualizationRoute: PlaybackVisualizationRoute,
    sharedFlashPlaybackSampleState: FlashVisualPlaybackSampleState?,
    onFlashVisualizationModeSelected: (FlashSignalVisualizationMode) -> Unit,
) {
    if (waveformPcm.isEmpty()) {
        return
    }
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
                } ?: FlashSignalVisualizationMode.Lanes
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
                flashVisualWindow = flashVisualWindow,
                sharedPlaybackSampleState = sharedFlashPlaybackSampleState,
                showPerfOverlay = isFlashVisualPerfOverlayEnabled,
                playbackSpeed = playbackSpeed,
                isScrubbing = isScrubbing,
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

@Composable
private fun ExpandablePlaybackLyricsSection(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    isPlaying: Boolean,
    transportMode: TransportModeOption?,
    playbackDisplayMode: PlaybackDisplayMode,
    lyricsExpanded: Boolean,
    tokenStripHeightDp: Float?,
    extraLyricsRecoveryHeight: Dp,
    applyLyricsPreviewBonusLine: Boolean,
    onLyricsExpandedChanged: (Boolean) -> Unit,
    onSeekToSample: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyricsLayoutModel =
        rememberPlaybackLyricsLayoutModel(
            followData = followData,
            displayedSamples = displayedSamples,
            transportMode = transportMode,
            playbackDisplayMode = playbackDisplayMode,
            tokenStripHeightDp = tokenStripHeightDp,
            extraLyricsRecoveryHeight = extraLyricsRecoveryHeight,
            applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
            lyricsExpanded = lyricsExpanded,
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-lyrics-section"),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            if (lyricsExpanded) {
                PlaybackLyricsFullList(
                    followData = followData,
                    displayLineRanges = lyricsLayoutModel.displayLineRanges,
                    activeLineIndex = lyricsLayoutModel.activeLineIndex,
                    onSeekToSample = onSeekToSample,
                    extraHeight = lyricsLayoutModel.effectiveExtraLyricsRecoveryHeight,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PlaybackTokenContextTape(
                    followData = followData,
                    displayedSamples = displayedSamples,
                    isPlaying = isPlaying,
                    visibleLineCount = lyricsLayoutModel.compactVisibleLineCount,
                    extraContainerHeight = lyricsLayoutModel.effectiveExtraLyricsRecoveryHeight,
                    layoutMeasurementSource =
                        when (playbackDisplayMode) {
                            PlaybackDisplayMode.Visual -> PlaybackLyricsLayoutMeasurementSource.VisualPreview
                            PlaybackDisplayMode.Mix -> PlaybackLyricsLayoutMeasurementSource.VisualPreview
                            PlaybackDisplayMode.Lyrics -> PlaybackLyricsLayoutMeasurementSource.LyricsPreview
                        },
                    onSeekToSample = onSeekToSample,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (followData.textFollowAvailable && followData.textTokens.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            LyricsExpandButton(
                expanded = lyricsExpanded,
                onExpandedChanged = onLyricsExpandedChanged,
            )
        }
    }
}

@Composable
private fun LyricsExpandButton(
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
) {
    IconButton(
        onClick = {
            val nextExpanded = !expanded
            Log.d("MiniAutomation", "lyricsExpandToggle expanded=$nextExpanded")
            onExpandedChanged(nextExpanded)
        },
        modifier =
            Modifier
                .padding(top = 4.dp)
                .size(36.dp)
                .testTag("playback-lyrics-expand-toggle")
                .semantics {
                    contentDescription =
                        if (expanded) {
                            "Collapse lyrics"
                        } else {
                            "Expand lyrics"
                        }
                },
    ) {
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription =
                stringResource(
                    if (expanded) {
                        R.string.audio_action_collapse_result_content
                    } else {
                        R.string.audio_action_expand_result_content
                    },
                ),
        )
    }
}

@Composable
private fun PlaybackLyricsFullList(
    followData: PayloadFollowViewData,
    displayLineRanges: List<DisplayTokenLineRange>,
    activeLineIndex: Int,
    onSeekToSample: (Int) -> Unit,
    extraHeight: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(displayLineRanges.size) {
        Log.d("MiniAutomation", "lyricsFullListRendered lineCount=${displayLineRanges.size}")
    }
    val lineStartSamples =
        remember(followData.textTokenTimeline) {
            followData.textTokenTimeline
                .groupBy { it.tokenIndex }
                .mapValues { (_, entries) -> entries.minOfOrNull { it.startSample } }
        }
    LazyColumn(
        modifier =
            modifier
                .height(280.dp + extraHeight)
                .testTag("playback-lyrics-full-list"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(displayLineRanges) { index, lineRange ->
            val lineText =
                resolveFullLyricsLineText(
                    followData = followData,
                    lineRange = lineRange,
                )
                    ?: resolveContinuousViewportLineForRange(
                        followData = followData,
                        tokenRange = lineRange.tokenRange,
                    ).text
            val startSample =
                lineStartSamples[lineRange.tokenBeginIndex]
                    ?.takeIf { it >= 0 }
            val isActive = index == activeLineIndex
            Text(
                text = lineText,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (startSample != null) {
                                Modifier.clickable { onSeekToSample(startSample) }
                            } else {
                                Modifier
                            },
                        ).testTag(if (isActive) "playback-lyrics-full-line-active" else "playback-lyrics-full-line"),
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                color =
                    if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

private fun resolveFullLyricsLineText(
    followData: PayloadFollowViewData,
    lineRange: DisplayTokenLineRange,
): String? =
    if (lineRange.coversFullSourceLine) {
        followData.lyricLines.getOrNull(lineRange.sourceLineIndex)
    } else {
        null
    }
