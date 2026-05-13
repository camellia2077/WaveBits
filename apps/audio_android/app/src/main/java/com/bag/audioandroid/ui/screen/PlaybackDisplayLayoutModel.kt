package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.ui.model.TransportModeOption

internal data class PlaybackDisplayLayoutModel(
    val visualizationRoute: PlaybackVisualizationRoute,
    val displayModeOptions: List<PlaybackDisplayMode>,
    val supportsMixMode: Boolean,
)

@Composable
internal fun rememberPlaybackDisplayLayoutModel(
    transportMode: TransportModeOption?,
    isFlashMode: Boolean,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean,
    sampleRateHz: Int,
    visualDisplayedSamples: Int,
    displayedSamples: Int,
    followData: PayloadFollowViewData,
): PlaybackDisplayLayoutModel {
    val visualizationRoute =
        remember(
            transportMode,
            isFlashMode,
            waveformPcm,
            isWaveformPreview,
            sampleRateHz,
            visualDisplayedSamples,
            displayedSamples,
            followData,
        ) {
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
        }
    val supportsMixMode =
        remember(transportMode, isFlashMode) {
            isFlashMode || transportMode == TransportModeOption.Mini
        }
    return remember(visualizationRoute, supportsMixMode) {
        val displayModeOptions =
            when {
                transportMode == TransportModeOption.Mini ->
                    listOf(PlaybackDisplayMode.Lyrics, PlaybackDisplayMode.Visual, PlaybackDisplayMode.Mix)
                supportsMixMode ->
                    listOf(PlaybackDisplayMode.Lyrics, PlaybackDisplayMode.Visual, PlaybackDisplayMode.Mix)
                else ->
                    listOf(PlaybackDisplayMode.Lyrics, PlaybackDisplayMode.Visual)
            }
        PlaybackDisplayLayoutModel(
            visualizationRoute = visualizationRoute,
            displayModeOptions = displayModeOptions,
            supportsMixMode = supportsMixMode,
        )
    }
}

internal data class PlaybackLyricsLayoutModel(
    val displayLineRanges: List<DisplayTokenLineRange>,
    val activeLineIndex: Int,
    val prefersWrappedLines: Boolean,
    val effectiveExtraLyricsRecoveryHeight: Dp,
    val compactVisibleLineCount: Int,
)

@Composable
internal fun rememberPlaybackLyricsLayoutModel(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    transportMode: TransportModeOption?,
    playbackDisplayMode: PlaybackDisplayMode,
    tokenStripHeightDp: Float?,
    extraLyricsRecoveryHeight: Dp,
    applyLyricsPreviewBonusLine: Boolean,
    lyricsExpanded: Boolean,
): PlaybackLyricsLayoutModel {
    val sourceLineRanges =
        remember(followData.lineTokenRanges, followData.textTokens) {
            followData.lineTokenRanges.ifEmpty {
                listOf(TextFollowLineTokenRangeViewData(0, 0, followData.textTokens.size))
            }
        }
    val displayLineRanges =
        remember(followData, sourceLineRanges) {
            buildDisplayTokenLineRanges(
                followData = followData,
                sourceLineRanges = sourceLineRanges,
            )
        }
    val activeTimelineIndex =
        remember(followData.textTokenTimeline, displayedSamples) {
            followActiveTextTimelineIndex(followData, displayedSamples)
        }
    val activeTokenIndex = followData.textTokenTimeline.getOrNull(activeTimelineIndex)?.tokenIndex ?: -1
    val activeLineIndex =
        remember(displayLineRanges, activeTokenIndex) {
            resolveActiveDisplayLineIndex(displayLineRanges, activeTokenIndex)
        }
    val prefersWrappedLines =
        remember(followData.textTokens, followData.lyricLines, displayLineRanges) {
            displayLineRanges.any { lineRange ->
                resolveContinuousViewportLineForRange(
                    followData = followData,
                    tokenRange = lineRange.tokenRange,
                    lyricLineText = null,
                ).shouldWrap
            }
        }
    val effectiveExtraLyricsRecoveryHeight =
        remember(lyricsExpanded, displayLineRanges.size, extraLyricsRecoveryHeight) {
            if (!lyricsExpanded && displayLineRanges.size <= 1) 0.dp else extraLyricsRecoveryHeight
        }
    val compactVisibleLineCount =
        remember(
            playbackDisplayMode,
            transportMode,
            prefersWrappedLines,
            effectiveExtraLyricsRecoveryHeight,
            tokenStripHeightDp,
            applyLyricsPreviewBonusLine,
        ) {
            computeCompactLyricsVisibleLineCount(
                transportMode = transportMode,
                playbackDisplayMode = playbackDisplayMode,
                prefersWrappedLines = prefersWrappedLines,
                effectiveExtraLyricsRecoveryHeight = effectiveExtraLyricsRecoveryHeight,
                tokenStripHeightDp = tokenStripHeightDp,
                applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
            )
        }
    return remember(
        displayLineRanges,
        activeLineIndex,
        prefersWrappedLines,
        effectiveExtraLyricsRecoveryHeight,
        compactVisibleLineCount,
    ) {
        PlaybackLyricsLayoutModel(
            displayLineRanges = displayLineRanges,
            activeLineIndex = activeLineIndex,
            prefersWrappedLines = prefersWrappedLines,
            effectiveExtraLyricsRecoveryHeight = effectiveExtraLyricsRecoveryHeight,
            compactVisibleLineCount = compactVisibleLineCount,
        )
    }
}

internal fun computeCompactLyricsVisibleLineCount(
    transportMode: TransportModeOption?,
    playbackDisplayMode: PlaybackDisplayMode,
    prefersWrappedLines: Boolean,
    effectiveExtraLyricsRecoveryHeight: Dp,
    tokenStripHeightDp: Float?,
    applyLyricsPreviewBonusLine: Boolean,
): Int =
    when (playbackDisplayMode) {
        PlaybackDisplayMode.Visual -> {
            val lineHeightDp = if (prefersWrappedLines) 56f else 36f
            val spacingDp = 4f
            val targetHeightDp = lineHeightDp * 4f + spacingDp * 3f + effectiveExtraLyricsRecoveryHeight.value
            val maxPossibleLines =
                (((targetHeightDp + spacingDp) / (lineHeightDp + spacingDp)).toInt()).coerceAtLeast(4)
            val visualLineCap =
                if (transportMode == TransportModeOption.Mini) {
                    7
                } else {
                    5
                }
            maxPossibleLines.coerceAtMost(visualLineCap)
        }
        PlaybackDisplayMode.Mix -> 0
        PlaybackDisplayMode.Lyrics ->
            lyricsPreviewVisibleLineCount(
                transportMode = transportMode,
                tokenStripHeightDp = tokenStripHeightDp,
                prefersWrappedLines = prefersWrappedLines,
                applyBonusLine = applyLyricsPreviewBonusLine,
            )
    }

internal fun lyricsPreviewVisibleLineCount(
    transportMode: TransportModeOption?,
    tokenStripHeightDp: Float?,
    prefersWrappedLines: Boolean,
    applyBonusLine: Boolean = false,
): Int {
    if (transportMode == TransportModeOption.Mini) {
        // Mini token pages use morse chips rather than variable-width binary/hex blocks.
        // Keep the lyrics preview fixed so the UI reflects the intended extra space
        // instead of being pulled back by shared token-strip height thresholds.
        return 7
    }
    val stripHeight = tokenStripHeightDp ?: return 4
    val baseCount =
        when {
            prefersWrappedLines && stripHeight >= 200f -> 2
            prefersWrappedLines -> 3
            stripHeight >= 200f -> 3
            else -> 4
        }
    val transportBonus =
        when (transportMode) {
            TransportModeOption.Mini -> 2
            else -> if (applyBonusLine) 1 else 0
        }
    return baseCount + transportBonus
}
