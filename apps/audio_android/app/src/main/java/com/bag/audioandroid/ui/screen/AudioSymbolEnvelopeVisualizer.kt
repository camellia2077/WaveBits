package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.TransportModeOption
import kotlin.math.ceil

@Composable
internal fun AudioSymbolEnvelopeVisualizer(
    pcm: ShortArray,
    sampleRateHz: Int,
    displayedSamples: Int,
    isPlaying: Boolean,
    transportMode: TransportModeOption,
    frameSamples: Int,
    modifier: Modifier = Modifier,
) {
    if (pcm.isEmpty() || sampleRateHz <= 0 || frameSamples <= 0) {
        return
    }

    val density = LocalDensity.current
    val totalSamples = pcm.size.coerceAtLeast(1)
    val clampedDisplayedSamples = displayedSamples.coerceIn(0, totalSamples)
    val snappedDisplayedSamples =
        remember(clampedDisplayedSamples, frameSamples, totalSamples) {
            snapDisplayedSampleToSymbol(
                displayedSample = clampedDisplayedSamples,
                symbolSamples = frameSamples,
                totalSamples = totalSamples,
            )
        }
    val glowPulse = if (isPlaying) 0.86f else 0.82f
    val sweepPhase = 0.24f

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(112.dp)
                .testTag("symbol-envelope-visualizer"),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val targetBucketCount =
            remember(widthPx) {
                val bucketSpacingPx = with(density) { 6.dp.toPx() }
                ceil((widthPx / bucketSpacingPx).toDouble()).toInt().coerceIn(SymbolEnvelopeMinBucketCount, SymbolEnvelopeMaxBucketCount)
            }
        val buckets =
            remember(pcm, sampleRateHz, snappedDisplayedSamples, frameSamples, targetBucketCount, transportMode) {
                buildSymbolEnvelopeBuckets(
                    pcm = pcm,
                    sampleRateHz = sampleRateHz,
                    currentSample = snappedDisplayedSamples.toFloat(),
                    symbolSamples = frameSamples,
                    targetBucketCount = targetBucketCount,
                    transportMode = transportMode,
                )
            }

        val activeToneColor = MaterialTheme.colorScheme.primary
        val inactiveToneColor = MaterialTheme.colorScheme.outlineVariant
        val glowColor = MaterialTheme.colorScheme.onPrimaryContainer
        val baseBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        val centerLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        val ambientBrush =
            Brush.horizontalGradient(
                colors =
                    listOf(
                        inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                        activeToneColor.copy(alpha = 0.12f + 0.02f * sweepPhase),
                        inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                    ),
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
            val scanHeadBucketIndex =
                (buckets.size * SymbolEnvelopePlayheadAnchorRatio)
                    .toInt()
                    .coerceIn(0, buckets.lastIndex)
            val playheadX = leftPadding + bucketWidth * scanHeadBucketIndex.toFloat()

            drawRoundRect(
                color = baseBackground,
                size = size,
                cornerRadius = corner,
            )
            drawRoundRect(
                brush = ambientBrush,
                size = size,
                cornerRadius = corner,
            )

            when (transportMode) {
                TransportModeOption.Pro ->
                    drawProSymbolEnvelope(
                        buckets = buckets,
                        leftPadding = leftPadding,
                        topPadding = topPadding,
                        innerWidth = innerWidth,
                        innerHeight = innerHeight,
                        bucketWidth = bucketWidth,
                        activeToneColor = activeToneColor,
                        inactiveToneColor = inactiveToneColor,
                        centerLineColor = centerLineColor,
                        glowPulse = glowPulse,
                    )

                TransportModeOption.Ultra ->
                    drawUltraSymbolEnvelope(
                        buckets = buckets,
                        leftPadding = leftPadding,
                        topPadding = topPadding,
                        innerWidth = innerWidth,
                        innerHeight = innerHeight,
                        bucketWidth = bucketWidth,
                        activeToneColor = activeToneColor,
                        inactiveToneColor = inactiveToneColor,
                        centerLineColor = centerLineColor,
                        glowPulse = glowPulse,
                    )

                else -> Unit
            }

            drawLine(
                color = glowColor.copy(alpha = 0.80f),
                start = Offset(playheadX, topPadding),
                end = Offset(playheadX, size.height - bottomPadding),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

private fun DrawScope.drawProSymbolEnvelope(
    buckets: List<SymbolEnvelopeBucket>,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    bucketWidth: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
) {
    val laneGap = 10.dp.toPx()
    val laneHeight = (innerHeight - laneGap).coerceAtLeast(1f) / 2f
    val upperTop = topPadding
    val lowerTop = topPadding + laneHeight + laneGap
    drawLine(
        color = centerLineColor,
        start = Offset(leftPadding, upperTop + laneHeight + laneGap / 2f),
        end = Offset(leftPadding + innerWidth, upperTop + laneHeight + laneGap / 2f),
        strokeWidth = 1.dp.toPx(),
    )

    buckets.forEachIndexed { index, bucket ->
        val x = leftPadding + bucketWidth * index.toFloat()
        val contentWidth = (bucketWidth - 1.dp.toPx()).coerceAtLeast(2.2f)
        val upperHeight = laneHeight * (0.08f + 0.82f * bucket.upperEnergy.coerceIn(0f, 1f))
        val lowerHeight = laneHeight * (0.08f + 0.82f * bucket.lowerEnergy.coerceIn(0f, 1f))
        val upperColor =
            inactiveToneColor.copy(alpha = (0.16f + 0.36f * bucket.peakAmplitude + 0.24f * glowPulse).coerceIn(0f, 0.72f))
                .compositeOver(activeToneColor.copy(alpha = (0.22f + 0.44f * bucket.upperEnergy).coerceIn(0f, 0.88f)))
        val lowerColor =
            inactiveToneColor.copy(alpha = (0.16f + 0.36f * bucket.peakAmplitude + 0.24f * glowPulse).coerceIn(0f, 0.72f))
                .compositeOver(activeToneColor.copy(alpha = (0.22f + 0.44f * bucket.lowerEnergy).coerceIn(0f, 0.88f)))

        drawRoundRect(
            color = upperColor,
            topLeft = Offset(x, upperTop + (laneHeight - upperHeight) / 2f),
            size = Size(contentWidth, upperHeight),
            cornerRadius = CornerRadius(contentWidth / 2f, contentWidth / 2f),
        )
        drawRoundRect(
            color = lowerColor,
            topLeft = Offset(x, lowerTop + (laneHeight - lowerHeight) / 2f),
            size = Size(contentWidth, lowerHeight),
            cornerRadius = CornerRadius(contentWidth / 2f, contentWidth / 2f),
        )
    }
}

private fun DrawScope.drawUltraSymbolEnvelope(
    buckets: List<SymbolEnvelopeBucket>,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    bucketWidth: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
) {
    val centerY = topPadding + innerHeight / 2f
    val maxEnvelopeHeight = innerHeight * 0.44f
    drawLine(
        color = centerLineColor,
        start = Offset(leftPadding, centerY),
        end = Offset(leftPadding + innerWidth, centerY),
        strokeWidth = 1.dp.toPx(),
    )

    buckets.forEachIndexed { index, bucket ->
        val x = leftPadding + bucketWidth * index.toFloat()
        val contentWidth = (bucketWidth - 1.dp.toPx()).coerceAtLeast(2.4f)
        val energy = bucket.upperEnergy.coerceIn(0f, 1f)
        val halfHeight = maxEnvelopeHeight * (0.10f + 0.84f * energy)
        val color =
            inactiveToneColor.copy(alpha = (0.18f + 0.24f * bucket.peakAmplitude).coerceIn(0f, 0.40f))
                .compositeOver(activeToneColor.copy(alpha = (0.28f + 0.42f * energy + 0.10f * glowPulse).coerceIn(0f, 0.92f)))

        drawRoundRect(
            color = color,
            topLeft = Offset(x, centerY - halfHeight),
            size = Size(contentWidth, halfHeight * 2f),
            cornerRadius = CornerRadius(contentWidth / 2f, contentWidth / 2f),
        )
    }
}

private const val SymbolEnvelopeMinBucketCount = 40
private const val SymbolEnvelopeMaxBucketCount = 88
