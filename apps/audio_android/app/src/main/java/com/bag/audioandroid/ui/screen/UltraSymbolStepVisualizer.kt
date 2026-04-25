package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import kotlin.math.ceil

@Composable
internal fun UltraSymbolStepVisualizer(
    pcm: ShortArray,
    sampleRateHz: Int,
    displayedSamples: Int,
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

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("ultra-symbol-step-visualizer"),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val targetBucketCount =
            remember(widthPx) {
                val bucketSpacingPx = with(density) { 10.dp.toPx() }
                ceil((widthPx / bucketSpacingPx).toDouble()).toInt().coerceIn(UltraMinBucketCount, UltraMaxBucketCount)
            }
        val state =
            remember(pcm, sampleRateHz, snappedDisplayedSamples, frameSamples, targetBucketCount) {
                buildUltraSymbolStepVisualizationState(
                    pcm = pcm,
                    sampleRateHz = sampleRateHz,
                    currentSample = snappedDisplayedSamples.toFloat(),
                    symbolSamples = frameSamples,
                    targetBucketCount = targetBucketCount,
                )
            } ?: return@BoxWithConstraints

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UltraSymbolStepChart(
                buckets = state.buckets,
                modifier = Modifier.fillMaxWidth(),
            )
            UltraNowNextRow(
                currentFrequencyHz = state.currentBucket.dominantFrequencyHz,
                nextFrequencyHz = state.nextBucket?.dominantFrequencyHz,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UltraSymbolStepChart(
    buckets: List<SymbolEnvelopeBucket>,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val nextColor = MaterialTheme.colorScheme.secondary
    val inactiveColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.42f)
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val baseBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
    val ambientBrush =
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
            ),
        )

    Box(
        modifier =
            modifier
                .height(84.dp)
                .testTag("ultra-symbol-step-chart"),
    ) {
        UltraReferenceTicks(modifier = Modifier.matchParentSize())

        Canvas(
            modifier = Modifier.matchParentSize(),
        ) {
            if (buckets.isEmpty()) {
                return@Canvas
            }

            val corner = CornerRadius(22.dp.toPx(), 22.dp.toPx())
            val leftPadding = 34.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 10.dp.toPx()
            val bottomPadding = 10.dp.toPx()
            val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val bucketWidth = innerWidth / buckets.size.toFloat()
            val stepXInset = bucketWidth * 0.22f
            val activeBucketIndex =
                (buckets.size * SymbolEnvelopePlayheadAnchorRatio)
                    .toInt()
                    .coerceIn(0, buckets.lastIndex)

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

            repeat(4) { guideIndex ->
                val y = topPadding + innerHeight * guideIndex.toFloat() / 3f
                drawLine(
                    color = guideColor,
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val stepPoints =
                buckets.mapIndexedNotNull { index, bucket ->
                    val laneIndex = bucket.dominantLaneIndex ?: return@mapIndexedNotNull null
                    val normalizedLane =
                        1f - (laneIndex.toFloat() / (UltraFreqsHz.lastIndex.coerceAtLeast(1)).toFloat())
                    val x = leftPadding + bucketWidth * index.toFloat() + bucketWidth / 2f
                    val y = topPadding + normalizedLane * innerHeight
                    IndexedStepPoint(index = index, point = Offset(x, y))
                }

            if (stepPoints.size >= 2) {
                val path = Path().apply {
                    moveTo(stepPoints.first().point.x, stepPoints.first().point.y)
                    stepPoints.drop(1).forEach { indexedPoint ->
                        lineTo(indexedPoint.point.x, indexedPoint.point.y)
                    }
                }
                drawPath(
                    path = path,
                    color = inactiveColor.copy(alpha = 0.20f),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }

            val activePoint = stepPoints.firstOrNull { it.index == activeBucketIndex }?.point
            val nextPoint = stepPoints.firstOrNull { it.index == activeBucketIndex + 1 }?.point
            if (activePoint != null && nextPoint != null) {
                drawLine(
                    color = nextColor.copy(alpha = 0.88f),
                    start = activePoint,
                    end = nextPoint,
                    strokeWidth = 3.dp.toPx(),
                )
                drawCircle(
                    color = nextColor.copy(alpha = 0.26f),
                    radius = 7.dp.toPx(),
                    center = nextPoint,
                )
            }

            buckets.forEachIndexed { index, bucket ->
                val laneIndex = bucket.dominantLaneIndex ?: return@forEachIndexed
                val normalizedLane =
                    1f - (laneIndex.toFloat() / (UltraFreqsHz.lastIndex.coerceAtLeast(1)).toFloat())
                val x = leftPadding + bucketWidth * index.toFloat() + stepXInset
                val y = topPadding + normalizedLane * innerHeight
                val markerHeight = (8.dp.toPx() + 10.dp.toPx() * bucket.upperEnergy.coerceIn(0f, 1f)).coerceAtLeast(8.dp.toPx())
                val markerWidth = (bucketWidth - stepXInset * 2f).coerceAtLeast(4.dp.toPx())
                val color =
                    when (index) {
                        activeBucketIndex -> activeColor
                        activeBucketIndex + 1 -> nextColor.copy(alpha = 0.82f)
                        else -> inactiveColor.copy(alpha = 0.34f)
                    }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y - markerHeight / 2f),
                    size = Size(markerWidth, markerHeight),
                    cornerRadius = CornerRadius(markerWidth / 2f, markerWidth / 2f),
                )

                if (index == activeBucketIndex) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.22f),
                        radius = 8.dp.toPx(),
                        center = Offset(x + markerWidth / 2f, y),
                    )
                }
            }
        }
    }
}

@Composable
private fun UltraReferenceTicks(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.padding(start = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start,
    ) {
        UltraReferenceTick(stringResource(R.string.audio_ultra_visual_level_high))
        UltraReferenceTick(stringResource(R.string.audio_ultra_visual_level_mid))
        UltraReferenceTick(stringResource(R.string.audio_ultra_visual_level_low))
    }
}

@Composable
private fun UltraReferenceTick(
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .width(18.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
        )
    }
}

@Composable
private fun UltraNowNextRow(
    currentFrequencyHz: Int?,
    nextFrequencyHz: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.testTag("ultra-now-next-row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UltraFrequencyPill(
            title = stringResource(R.string.audio_ultra_visual_now),
            value =
                currentFrequencyHz?.let {
                    stringResource(R.string.audio_ultra_visual_frequency_value, it)
                } ?: stringResource(R.string.audio_ultra_visual_no_symbol),
            modifier = Modifier.weight(1f),
        )
        UltraFrequencyPill(
            title = stringResource(R.string.audio_ultra_visual_next),
            value =
                nextFrequencyHz?.let {
                    stringResource(R.string.audio_ultra_visual_frequency_value, it)
                } ?: stringResource(R.string.audio_ultra_visual_no_next_symbol),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun UltraFrequencyPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class IndexedStepPoint(
    val index: Int,
    val point: Offset,
)

private const val UltraMinBucketCount = 12
private const val UltraMaxBucketCount = 16
