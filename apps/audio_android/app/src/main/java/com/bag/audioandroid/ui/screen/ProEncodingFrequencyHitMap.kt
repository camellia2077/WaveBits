package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R

private val ProFrequencySectionHeight = 76.dp
private val ProFrequencyLabelColumnWidth = 78.dp
private val ProFrequencySectionGap = 12.dp
private val ProFrequencySectionLabelSpacing = 6.dp
private val ProFrequencyAxisStrokeWidth = 1.dp
private val ProFrequencyAxisEndPadding = 6.dp

@Composable
internal fun ProFrequencyHitMap(
    symbols: List<ProSymbolExplanation>,
    modifier: Modifier = Modifier,
) {
    val mapBackgroundBrush =
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
            ),
        )
    val neutralColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
    val activeColor = MaterialTheme.colorScheme.primary
    val historyColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.68f)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ProFrequencySectionGap),
    ) {
        ProFrequencySection(
            title = stringResource(R.string.audio_pro_visual_high_group),
            frequencies = ProHighFreqHz,
            symbols = symbols,
            laneFrequency = { it.highFreqHz },
            mapBackgroundBrush = mapBackgroundBrush,
            neutralColor = neutralColor,
            activeColor = activeColor,
            historyColor = historyColor,
            modifier = Modifier.fillMaxWidth(),
        )
        ProFrequencySection(
            title = stringResource(R.string.audio_pro_visual_low_group),
            frequencies = ProLowFreqHz,
            symbols = symbols,
            laneFrequency = { it.lowFreqHz },
            mapBackgroundBrush = mapBackgroundBrush,
            neutralColor = neutralColor,
            activeColor = activeColor,
            historyColor = historyColor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProFrequencySection(
    title: String,
    frequencies: List<Int>,
    symbols: List<ProSymbolExplanation>,
    laneFrequency: (ProSymbolExplanation) -> Int,
    mapBackgroundBrush: Brush,
    neutralColor: androidx.compose.ui.graphics.Color,
    activeColor: androidx.compose.ui.graphics.Color,
    historyColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.width(ProFrequencyLabelColumnWidth)) {
            // Keep the title and tick labels on the same visual axis so the left edge reads as one guide rail.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .width(ProFrequencyAxisStrokeWidth)
                        .height(ProFrequencySectionHeight + ProFrequencySectionLabelSpacing + 18.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                            shape = RoundedCornerShape(99.dp),
                        ),
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(end = ProFrequencyAxisEndPadding),
                verticalArrangement = Arrangement.spacedBy(ProFrequencySectionLabelSpacing),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
                Column(
                    modifier = Modifier.height(ProFrequencySectionHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    frequencies.forEach { hz ->
                        Text(
                            // Right-align the lane labels so mixed-width frequency values read as a tidy scale.
                            text = formatFrequencyLabel(hz),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(ProFrequencySectionHeight + ProFrequencySectionLabelSpacing + 18.dp)
                    .background(
                        brush = mapBackgroundBrush,
                        shape = RoundedCornerShape(22.dp),
                    ),
        ) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(ProFrequencySectionHeight)
                        .padding(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 12.dp)
                        .testTag("pro-frequency-hit-map"),
            ) {
                val laneCount = frequencies.size
                val rowHeight = size.height / laneCount.toFloat()
                val columnWidth = size.width / symbols.size.coerceAtLeast(1).toFloat()

                repeat(laneCount) { laneIndex ->
                    val y = rowHeight * laneIndex.toFloat() + rowHeight / 2f
                    drawLine(
                        color = neutralColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                symbols.forEachIndexed { index, symbol ->
                    val laneIndex = frequencies.indexOf(laneFrequency(symbol)).coerceAtLeast(0)
                    val columnStart = columnWidth * index.toFloat()
                    val columnInset = columnWidth * 0.18f
                    val color = if (symbol.isCurrent) activeColor else historyColor

                    drawRoundRect(
                        color = color.copy(alpha = if (symbol.isCurrent) 0.92f else 0.54f),
                        topLeft = Offset(columnStart + columnInset, rowHeight * laneIndex + rowHeight * 0.14f),
                        size = Size(columnWidth - columnInset * 2f, rowHeight * 0.72f),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    )
                }
            }
        }
    }
}

private fun formatFrequencyLabel(frequencyHz: Int): String {
    // Keep the SI unit symbol fixed as "Hz" across locales for technical readability.
    return "$frequencyHz Hz"
}
