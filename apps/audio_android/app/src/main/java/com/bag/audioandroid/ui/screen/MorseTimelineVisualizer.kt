package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

internal data class MorseTimelineWindow(
    val startSample: Int,
    val sampleCount: Int,
) {
    val endSample: Int
        get() = startSample + sampleCount
}

@Composable
internal fun MorseTimelineVisualizer(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    frameSamples: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!followData.followAvailable || followData.binaryGroupTimeline.isEmpty() || frameSamples <= 0) {
        return
    }

    val visualTokens = appThemeVisualTokens()
    val toneEntries =
        remember(followData.binaryGroupTimeline) {
            followData.binaryGroupTimeline.sortedBy(PayloadFollowBinaryGroupTimelineEntry::startSample)
        }
    val totalSamples = followData.totalPcmSampleCount.coerceAtLeast(1)
    val currentSample = displayedSamples.coerceIn(0, totalSamples)
    val windowSamples =
        remember(frameSamples) {
            (frameSamples * 96).coerceAtLeast(frameSamples * 16)
        }
    val window =
        remember(currentSample, windowSamples) {
            resolveMorseTimelineWindow(
                currentSample = currentSample,
                windowSamples = windowSamples,
            )
        }

    val toneColor = MaterialTheme.colorScheme.primary
    val pastToneColor = MaterialTheme.colorScheme.secondary
    val dashColor = MaterialTheme.colorScheme.tertiary
    val playheadColor = MaterialTheme.colorScheme.onPrimaryContainer
    val backgroundColor = visualTokens.visualizationBaseBackgroundColor

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(112.dp)
                .testTag("morse-timeline-visualizer"),
    ) {
        drawRoundRect(
            color = backgroundColor,
            size = size,
            cornerRadius = CornerRadius(24f, 24f),
        )

        val leftPadding = 12.dp.toPx()
        val rightPadding = 12.dp.toPx()
        val topPadding = 16.dp.toPx()
        val bottomPadding = 16.dp.toPx()
        val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
        val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
        val visibleSamples = window.sampleCount.coerceAtLeast(1)
        val centerY = topPadding + innerHeight / 2f

        toneEntries.forEach { entry ->
            val entryStart = entry.startSample
            val entryEnd = entry.startSample + entry.sampleCount
            if (entryEnd < window.startSample || entryStart > window.endSample) {
                return@forEach
            }
            val visibleStart = entryStart.coerceAtLeast(window.startSample)
            val visibleEnd = entryEnd.coerceAtMost(window.endSample)
            val x =
                leftPadding +
                    ((visibleStart - window.startSample).toFloat() / visibleSamples.toFloat()) * innerWidth
            val width =
                (((visibleEnd - visibleStart).coerceAtLeast(1)).toFloat() / visibleSamples.toFloat()) *
                    innerWidth
            val isDash = entry.sampleCount >= frameSamples * MorseDashUnitThreshold
            val isPast = entryEnd <= currentSample
            val isActive = currentSample in entryStart until entryEnd
            val blockHeight =
                if (isDash) {
                    innerHeight * 0.68f
                } else {
                    innerHeight * 0.42f
                }
            val color =
                when {
                    isActive -> toneColor
                    isPast -> pastToneColor.copy(alpha = if (isPlaying) 0.78f else 0.62f)
                    isDash -> dashColor.copy(alpha = 0.72f)
                    else -> toneColor.copy(alpha = 0.48f)
                }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, centerY - blockHeight / 2f),
                size = Size(width.coerceAtLeast(2.dp.toPx()), blockHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
        }

        val playheadX =
            leftPadding +
                ((currentSample - window.startSample).toFloat() / visibleSamples.toFloat()) * innerWidth
        drawLine(
            color = playheadColor.copy(alpha = 0.80f),
            start = Offset(playheadX, topPadding),
            end = Offset(playheadX, size.height - bottomPadding),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

internal fun resolveMorseTimelineWindow(
    currentSample: Int,
    windowSamples: Int,
): MorseTimelineWindow {
    val safeWindowSamples = windowSamples.coerceAtLeast(1)
    val windowStart = (currentSample - (safeWindowSamples * MorseTimelinePlayheadAnchorRatio).toInt()).coerceAtLeast(0)
    return MorseTimelineWindow(
        startSample = windowStart,
        sampleCount = safeWindowSamples,
    )
}

internal fun morseTimelineSampleWidthFraction(
    sampleCount: Int,
    window: MorseTimelineWindow,
): Float = sampleCount.coerceAtLeast(1).toFloat() / window.sampleCount.coerceAtLeast(1).toFloat()

private const val MorseTimelinePlayheadAnchorRatio = 0.40f
private const val MorseDashUnitThreshold = 3
