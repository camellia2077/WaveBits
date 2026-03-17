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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

@Composable
internal fun AudioPcmWaveform(
    pcm: ShortArray,
    sampleRateHz: Int,
    displayedSamples: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    if (pcm.isEmpty()) {
        return
    }

    val density = LocalDensity.current
    val totalSamples = pcm.size.coerceAtLeast(1)
    val visualTransition = rememberInfiniteTransition(label = "pcmWaveform")
    val glowPulseAnimated by visualTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pcmWaveformGlowPulse"
    )
    val driftAnimated by visualTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pcmWaveformDrift"
    )
    val clampedDisplayedSamples = displayedSamples.coerceIn(0, totalSamples)
    val animatedDisplayedSamples by animateFloatAsState(
        targetValue = clampedDisplayedSamples.toFloat(),
        animationSpec = tween(durationMillis = if (isPlaying) 120 else 0, easing = FastOutSlowInEasing),
        label = "pcmWaveformDisplayedSamples"
    )
    val glowPulse = if (isPlaying) glowPulseAnimated else 0.82f
    val driftPhase = if (isPlaying) driftAnimated else 0.2f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val targetBucketCount = remember(widthPx) {
            val bucketSpacingPx = with(density) { 3.dp.toPx() }
            ceil((widthPx / bucketSpacingPx).toDouble())
                .toInt()
                .coerceIn(PcmWaveformMinBucketCount, PcmWaveformMaxBucketCount)
        }
        val windowSampleCount = remember(sampleRateHz, totalSamples) {
            sampleRateHz.coerceAtLeast(1).coerceAtMost(max(totalSamples, 1))
        }
        val buckets = remember(pcm, targetBucketCount, windowSampleCount, animatedDisplayedSamples) {
            buildPcmWaveBuckets(
                pcm = pcm,
                currentSample = animatedDisplayedSamples,
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount
            )
        }

        val primaryColor = MaterialTheme.colorScheme.primary
        val glowColor = MaterialTheme.colorScheme.onPrimaryContainer
        val baseBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        val centerLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        val ambientBrush = Brush.horizontalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f + 0.02f * driftPhase),
                primaryColor.copy(alpha = 0.10f + 0.02f * glowPulse)
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
            val centerY = topPadding + innerHeight / 2f
            val maxAmplitudePx = innerHeight * 0.48f
            val playheadX = leftPadding + innerWidth * PcmWaveformPlayheadAnchorRatio
            val strokeWidth = (innerWidth / buckets.size.toFloat()).coerceAtLeast(1.2f)
            val minBarHeight = 1.4.dp.toPx()
            val playheadBucket = buckets.minByOrNull { bucket ->
                abs(
                    bucketCenterX(
                        bucket = bucket,
                        windowCenterSample = animatedDisplayedSamples,
                        windowSampleCount = windowSampleCount.toFloat(),
                        leftPadding = leftPadding,
                        innerWidth = innerWidth
                    ) - playheadX
                )
            }

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
            drawLine(
                color = centerLineColor,
                start = Offset(leftPadding, centerY),
                end = Offset(size.width - rightPadding, centerY),
                strokeWidth = 1.dp.toPx()
            )

            buckets.forEach { bucket ->
                val x = bucketCenterX(
                    bucket = bucket,
                    windowCenterSample = animatedDisplayedSamples,
                    windowSampleCount = windowSampleCount.toFloat(),
                    leftPadding = leftPadding,
                    innerWidth = innerWidth
                )
                val (topY, bottomY) = bucketVerticalSpan(
                    bucket = bucket,
                    centerY = centerY,
                    maxAmplitudePx = maxAmplitudePx,
                    minHeightPx = minBarHeight
                )
                val emphasis = 0.24f + bucket.peakAmplitude * 0.56f

                drawLine(
                    color = primaryColor.copy(alpha = (0.12f + emphasis * 0.24f) * glowPulse),
                    start = Offset(x, topY),
                    end = Offset(x, bottomY),
                    strokeWidth = strokeWidth + 1.6.dp.toPx()
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.34f + emphasis * 0.24f),
                    start = Offset(x, topY),
                    end = Offset(x, bottomY),
                    strokeWidth = strokeWidth
                )
            }

            playheadBucket?.let { bucket ->
                val (topY, bottomY) = bucketVerticalSpan(
                    bucket = bucket,
                    centerY = centerY,
                    maxAmplitudePx = maxAmplitudePx,
                    minHeightPx = minBarHeight
                )
                val focusY = (topY + bottomY) / 2f

                drawLine(
                    color = glowColor.copy(alpha = 0.82f),
                    start = Offset(playheadX, topPadding),
                    end = Offset(playheadX, size.height - bottomPadding),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.14f * glowPulse),
                    radius = 12.dp.toPx(),
                    center = Offset(playheadX, focusY)
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.26f * glowPulse),
                    radius = 6.dp.toPx(),
                    center = Offset(playheadX, focusY)
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.96f),
                    radius = 2.6.dp.toPx(),
                    center = Offset(playheadX, focusY)
                )
            }
        }
    }
}

private fun bucketCenterX(
    bucket: PcmWaveBucket,
    windowCenterSample: Float,
    windowSampleCount: Float,
    leftPadding: Float,
    innerWidth: Float
): Float {
    val safeWindowSampleCount = windowSampleCount.coerceAtLeast(1f)
    val windowStart = windowCenterSample - safeWindowSampleCount * PcmWaveformPlayheadAnchorRatio
    val bucketCenterSample = (bucket.startSample + bucket.endSample) / 2f
    val positionRatio = ((bucketCenterSample - windowStart) / safeWindowSampleCount).coerceIn(0f, 1f)
    return leftPadding + innerWidth * positionRatio
}

private fun bucketVerticalSpan(
    bucket: PcmWaveBucket,
    centerY: Float,
    maxAmplitudePx: Float,
    minHeightPx: Float
): Pair<Float, Float> {
    var topY = centerY - maxAmplitudePx * bucket.maxAmplitude
    var bottomY = centerY - maxAmplitudePx * bucket.minAmplitude
    val currentHeight = bottomY - topY
    if (currentHeight < minHeightPx) {
        val halfPadding = (minHeightPx - currentHeight) / 2f
        topY -= halfPadding
        bottomY += halfPadding
    }
    return topY to bottomY
}

private const val PcmWaveformMinBucketCount = 72
private const val PcmWaveformMaxBucketCount = 220
