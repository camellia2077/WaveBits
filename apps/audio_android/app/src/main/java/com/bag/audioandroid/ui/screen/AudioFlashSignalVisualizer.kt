package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import kotlin.math.ceil

@Composable
internal fun AudioFlashSignalVisualizer(
    pcm: ShortArray,
    sampleRateHz: Int,
    displayedSamples: Int,
    isPlaying: Boolean,
    mode: FlashSignalVisualizationMode,
    flashVoicingStyle: FlashVoicingStyleOption?,
    modifier: Modifier = Modifier
) {
    if (pcm.isEmpty() || sampleRateHz <= 0) {
        return
    }

    val density = LocalDensity.current
    val visualTransition = rememberInfiniteTransition(label = "flashSignalVisualizer")
    val glowPulseAnimated by visualTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashSignalGlowPulse"
    )
    val sweepAnimated by visualTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flashSignalSweep"
    )
    val totalSamples = pcm.size.coerceAtLeast(1)
    val clampedDisplayedSamples = displayedSamples.coerceIn(0, totalSamples)
    val animatedDisplayedSamples by animateFloatAsState(
        targetValue = clampedDisplayedSamples.toFloat(),
        animationSpec = tween(durationMillis = if (isPlaying) 120 else 0, easing = FastOutSlowInEasing),
        label = "flashSignalDisplayedSamples"
    )
    val glowPulse = if (isPlaying) glowPulseAnimated else 0.82f
    val sweepPhase = if (isPlaying) sweepAnimated else 0.24f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val targetBucketCount = remember(widthPx) {
            val bucketSpacingPx = with(density) { 6.dp.toPx() }
            ceil((widthPx / bucketSpacingPx).toDouble())
                .toInt()
                .coerceIn(FlashSignalMinBucketCount, FlashSignalMaxBucketCount)
        }
        val windowSampleCount = remember(sampleRateHz) {
            sampleRateHz.coerceAtLeast(1)
        }
        val activeWindowBucketCount = remember(flashVoicingStyle) {
            flashSignalActiveWindowBucketCount(flashVoicingStyle)
        }
        val buckets = remember(pcm, sampleRateHz, targetBucketCount, windowSampleCount, animatedDisplayedSamples) {
            buildFskEnergyBuckets(
                pcm = pcm,
                sampleRateHz = sampleRateHz,
                currentSample = animatedDisplayedSamples,
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount
            )
        }

        val activeToneColor = MaterialTheme.colorScheme.primary
        val inactiveToneColor = MaterialTheme.colorScheme.outlineVariant
        val glowColor = MaterialTheme.colorScheme.onPrimaryContainer
        val baseBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        val centerLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        val ambientBrush = Brush.horizontalGradient(
            colors = listOf(
                inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                activeToneColor.copy(alpha = 0.12f + 0.02f * sweepPhase),
                inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse)
            )
        )

        Canvas(modifier = Modifier.fillMaxWidth().height(112.dp)) {
            if (buckets.isEmpty()) {
                return@Canvas
            }

            val corner = CornerRadius(24f, 24f)
            val leftPadding = 12.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 12.dp.toPx()
            val bottomPadding = 12.dp.toPx()
            val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val bucketWidth = innerWidth / buckets.size.toFloat()
            val scanHeadBucketIndex = ((buckets.size) * FlashSignalPlayheadAnchorRatio)
                .toInt()
                .coerceIn(0, buckets.lastIndex)
            val playheadX = leftPadding + bucketWidth * scanHeadBucketIndex.toFloat()

            drawRoundRect(
                color = baseBackground,
                size = size,
                cornerRadius = corner
            )
            drawRoundRect(
                brush = ambientBrush,
                size = size,
                cornerRadius = corner
            )

            when (mode) {
                FlashSignalVisualizationMode.ToneTracks -> drawToneTracks(
                    buckets = buckets,
                    scanHeadBucketIndex = scanHeadBucketIndex,
                    activeWindowBucketCount = activeWindowBucketCount,
                    leftPadding = leftPadding,
                    topPadding = topPadding,
                    innerWidth = innerWidth,
                    innerHeight = innerHeight,
                    bucketWidth = bucketWidth,
                    activeToneColor = activeToneColor,
                    inactiveToneColor = inactiveToneColor,
                    centerLineColor = centerLineColor,
                    glowPulse = glowPulse
                )

                FlashSignalVisualizationMode.ToneEnergy -> drawToneEnergy(
                    buckets = buckets,
                    scanHeadBucketIndex = scanHeadBucketIndex,
                    activeWindowBucketCount = activeWindowBucketCount,
                    leftPadding = leftPadding,
                    topPadding = topPadding,
                    innerWidth = innerWidth,
                    innerHeight = innerHeight,
                    bucketWidth = bucketWidth,
                    activeToneColor = activeToneColor,
                    inactiveToneColor = inactiveToneColor,
                    centerLineColor = centerLineColor,
                    glowPulse = glowPulse
                )
            }

            drawLine(
                color = glowColor.copy(alpha = 0.80f),
                start = Offset(playheadX, topPadding),
                end = Offset(playheadX, size.height - bottomPadding),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private const val FlashSignalMinBucketCount = 56
private const val FlashSignalMaxBucketCount = 124
