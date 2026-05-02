package com.bag.audioandroid.ui.screen

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal data class FlashSignalViewport(
    val startSample: Float,
    val endSample: Float,
    val playheadSample: Float,
) {
    val sampleCount: Float = (endSample - startSample).coerceAtLeast(1f)
}

internal fun DrawScope.drawToneTrackSegments(
    segments: List<FlashSignalToneSegment>,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
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
    visibleFlashSegments(segments, viewport).forEach { segment ->
        val startX = sampleToViewportX(segment.startSample.toFloat(), viewport, leftPadding, innerWidth)
        val endX = sampleToViewportX(segment.endSample.toFloat(), viewport, leftPadding, innerWidth)
        val segmentWidth = (endX - startX).coerceAtLeast(1.6f)
        val top =
            when (segment.tone) {
                FskDominantTone.High -> upperTop
                FskDominantTone.Low -> lowerTop
                FskDominantTone.Unknown -> return@forEach
            }
        drawMicroBarsAcrossSegment(
            segment = segment,
            viewport = viewport,
            startX = startX,
            endX = endX,
            minBarHeight = laneHeight * 0.48f,
            maxBarHeight = laneHeight * 0.82f,
            centerY = top + laneHeight / 2f,
            activeToneColor = activeToneColor,
            inactiveToneColor = inactiveToneColor,
            glowPulse = glowPulse,
            minVisualWidth = segmentWidth,
        )
    }
}

internal fun DrawScope.drawToneEnergySegments(
    segments: List<FlashSignalToneSegment>,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
) {
    val centerY = topPadding + innerHeight / 2f
    val upperGap = 3.dp.toPx()
    val lowerGap = 3.dp.toPx()
    val maxEnergyHeight = innerHeight * 0.42f

    drawLine(
        color = centerLineColor,
        start = Offset(leftPadding, centerY),
        end = Offset(leftPadding + innerWidth, centerY),
        strokeWidth = 1.dp.toPx(),
    )
    visibleFlashSegments(segments, viewport).forEach { segment ->
        val startX = sampleToViewportX(segment.startSample.toFloat(), viewport, leftPadding, innerWidth)
        val endX = sampleToViewportX(segment.endSample.toFloat(), viewport, leftPadding, innerWidth)
        val segmentWidth = (endX - startX).coerceAtLeast(1.8f)
        when (segment.tone) {
            FskDominantTone.High ->
                drawEnergyPulseClusterAcrossSegment(
                    segment = segment,
                    viewport = viewport,
                    startX = startX,
                    endX = endX,
                    baselineY = centerY - upperGap,
                    direction = -1f,
                    minBarHeight = maxEnergyHeight * 0.28f,
                    maxBarHeight = maxEnergyHeight,
                    activeToneColor = activeToneColor,
                    inactiveToneColor = inactiveToneColor,
                    glowPulse = glowPulse,
                    minVisualWidth = segmentWidth,
                )

            FskDominantTone.Low ->
                drawEnergyPulseClusterAcrossSegment(
                    segment = segment,
                    viewport = viewport,
                    startX = startX,
                    endX = endX,
                    baselineY = centerY + lowerGap,
                    direction = 1f,
                    minBarHeight = maxEnergyHeight * 0.28f,
                    maxBarHeight = maxEnergyHeight,
                    activeToneColor = activeToneColor,
                    inactiveToneColor = inactiveToneColor,
                    glowPulse = glowPulse,
                    minVisualWidth = segmentWidth,
                )

            FskDominantTone.Unknown -> Unit
        }
    }
}

internal fun DrawScope.drawPitchLadderSegments(
    segments: List<FlashSignalToneSegment>,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
) {
    val highY = topPadding + innerHeight * 0.12f
    val lowY = topPadding + innerHeight * 0.88f
    val activeStrokeWidth = 4.8.dp.toPx()
    val inactiveStrokeWidth = 3.6.dp.toPx()

    drawLine(
        color = centerLineColor.copy(alpha = 0.48f),
        start = Offset(leftPadding, highY),
        end = Offset(leftPadding + innerWidth, highY),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = centerLineColor.copy(alpha = 0.48f),
        start = Offset(leftPadding, lowY),
        end = Offset(leftPadding + innerWidth, lowY),
        strokeWidth = 1.dp.toPx(),
    )

    visibleFlashSegments(segments, viewport).forEach { segment ->
        val y =
            when (segment.tone) {
                FskDominantTone.High -> highY
                FskDominantTone.Low -> lowY
                FskDominantTone.Unknown -> return@forEach
            }
        val startX = sampleToViewportX(segment.startSample.toFloat(), viewport, leftPadding, innerWidth)
        val endX = sampleToViewportX(segment.endSample.toFloat(), viewport, leftPadding, innerWidth)
        drawPitchSegmentedBar(
            segment = segment,
            viewport = viewport,
            startX = startX,
            endX = endX,
            y = y,
            activeBarHeight = activeStrokeWidth,
            inactiveBarHeight = inactiveStrokeWidth,
            activeToneColor = activeToneColor,
            inactiveToneColor = inactiveToneColor,
            glowPulse = glowPulse,
        )
    }
}

internal fun DrawScope.drawToneTracks(
    buckets: List<FskEnergyBucket>,
    activeThresholdBucketIndex: Float,
    activeWindowBucketCount: Int,
    bucketOffset: Float,
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
        val isActiveBucket = index.toFloat() <= activeThresholdBucketIndex
        val x = leftPadding + bucketWidth * (index.toFloat() - bucketOffset)
        val contentWidth = (bucketWidth - 1.dp.toPx()).coerceAtLeast(1.6f)
        val heldHighStrength = bucket.heldToneStrength(buckets, index, FskDominantTone.High)
        val heldLowStrength = bucket.heldToneStrength(buckets, index, FskDominantTone.Low)
        val highAlpha =
            when (bucket.dominantTone) {
                FskDominantTone.High -> 0.30f + bucket.highStrength * (0.54f + 0.08f * glowPulse)
                FskDominantTone.Unknown -> 0.06f + bucket.highStrength * 0.20f
                FskDominantTone.Low -> 0.08f + bucket.highStrength * 0.18f
            }
        val lowAlpha =
            when (bucket.dominantTone) {
                FskDominantTone.Low -> 0.30f + bucket.lowStrength * (0.54f + 0.08f * glowPulse)
                FskDominantTone.Unknown -> 0.06f + bucket.lowStrength * 0.20f
                FskDominantTone.High -> 0.08f + bucket.lowStrength * 0.18f
            }
        val highHoldAlpha = if (heldHighStrength > bucket.highStrength) heldHighStrength * FlashToneHoldAlphaBoost else 0f
        val lowHoldAlpha = if (heldLowStrength > bucket.lowStrength) heldLowStrength * FlashToneHoldAlphaBoost else 0f
        val highHeight = laneHeight * (0.24f + 0.66f * heldHighStrength)
        val lowHeight = laneHeight * (0.24f + 0.66f * heldLowStrength)
        val highColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.High,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = highAlpha + highHoldAlpha,
                strength = heldHighStrength,
            )
        val lowColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.Low,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = lowAlpha + lowHoldAlpha,
                strength = heldLowStrength,
            )

        drawRoundRect(
            color = highColor,
            topLeft = Offset(x, upperTop + (laneHeight - highHeight) / 2f),
            size = Size(contentWidth, highHeight),
            cornerRadius = CornerRadius(contentWidth / 2f, contentWidth / 2f),
        )
        drawRoundRect(
            color = lowColor,
            topLeft = Offset(x, lowerTop + (laneHeight - lowHeight) / 2f),
            size = Size(contentWidth, lowHeight),
            cornerRadius = CornerRadius(contentWidth / 2f, contentWidth / 2f),
        )
    }
}

internal fun DrawScope.drawToneEnergy(
    buckets: List<FskEnergyBucket>,
    activeThresholdBucketIndex: Float,
    activeWindowBucketCount: Int,
    bucketOffset: Float,
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
    val upperGap = 3.dp.toPx()
    val lowerGap = 3.dp.toPx()
    val maxEnergyHeight = innerHeight * 0.42f
    val strokeWidth = (bucketWidth * 0.58f).coerceAtLeast(1.8f)

    drawLine(
        color = centerLineColor,
        start = Offset(leftPadding, centerY),
        end = Offset(leftPadding + innerWidth, centerY),
        strokeWidth = 1.dp.toPx(),
    )
    buckets.forEachIndexed { index, bucket ->
        val isActiveBucket = index.toFloat() <= activeThresholdBucketIndex
        val x = leftPadding + bucketWidth * (index.toFloat() - bucketOffset) + bucketWidth / 2f
        val heldHighStrength = bucket.heldToneStrength(buckets, index, FskDominantTone.High)
        val heldLowStrength = bucket.heldToneStrength(buckets, index, FskDominantTone.Low)
        val highHeight = maxEnergyHeight * heldHighStrength
        val lowHeight = maxEnergyHeight * heldLowStrength
        val highAlpha =
            if (bucket.dominantTone == FskDominantTone.High) {
                0.42f + 0.42f * glowPulse + bucket.confidence * 0.12f
            } else {
                0.18f + bucket.highStrength * 0.24f
            }
        val lowAlpha =
            if (bucket.dominantTone == FskDominantTone.Low) {
                0.42f + 0.42f * glowPulse + bucket.confidence * 0.12f
            } else {
                0.18f + bucket.lowStrength * 0.24f
            }
        val highHoldAlpha = if (heldHighStrength > bucket.highStrength) heldHighStrength * FlashToneHoldAlphaBoost else 0f
        val lowHoldAlpha = if (heldLowStrength > bucket.lowStrength) heldLowStrength * FlashToneHoldAlphaBoost else 0f
        val highColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.High,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = highAlpha + highHoldAlpha,
                strength = heldHighStrength,
            )
        val lowColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.Low,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = lowAlpha + lowHoldAlpha,
                strength = heldLowStrength,
            )

        drawLine(
            color = highColor,
            start = Offset(x, centerY - upperGap),
            end = Offset(x, centerY - upperGap - highHeight),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = lowColor,
            start = Offset(x, centerY + lowerGap),
            end = Offset(x, centerY + lowerGap + lowHeight),
            strokeWidth = strokeWidth,
        )
    }
}

internal fun DrawScope.drawPitchLadder(
    buckets: List<FskEnergyBucket>,
    activeThresholdBucketIndex: Float,
    activeWindowBucketCount: Int,
    bucketOffset: Float,
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
    val highY = topPadding + innerHeight * 0.12f
    val lowY = topPadding + innerHeight * 0.88f
    val strokeWidth = 3.dp.toPx()
    val pointRadius = (bucketWidth * 0.34f).coerceIn(1.6f, 4.8f)

    drawLine(
        color = centerLineColor.copy(alpha = 0.48f),
        start = Offset(leftPadding, highY),
        end = Offset(leftPadding + innerWidth, highY),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = centerLineColor.copy(alpha = 0.48f),
        start = Offset(leftPadding, lowY),
        end = Offset(leftPadding + innerWidth, lowY),
        strokeWidth = 1.dp.toPx(),
    )

    fun yForTone(bucket: FskEnergyBucket): Float? =
        when (bucket.dominantTone) {
            FskDominantTone.High -> highY
            FskDominantTone.Low -> lowY
            FskDominantTone.Unknown -> null
        }

    val pastPath = Path()
    val futurePath = Path()
    var pastStarted = false
    var futureStarted = false
    var pastSegmentOpen = false
    var futureSegmentOpen = false
    buckets.forEachIndexed { index, bucket ->
        val y = yForTone(bucket)
        if (y == null) {
            if (index.toFloat() <= activeThresholdBucketIndex) {
                pastSegmentOpen = false
            } else {
                futureSegmentOpen = false
            }
            return@forEachIndexed
        }

        val segmentStartX = leftPadding + bucketWidth * (index.toFloat() - bucketOffset)
        val segmentEndX = segmentStartX + bucketWidth
        val isPastBucket = index.toFloat() <= activeThresholdBucketIndex
        val targetPath = if (isPastBucket) pastPath else futurePath
        val segmentOpen = if (isPastBucket) pastSegmentOpen else futureSegmentOpen
        if (!segmentOpen) {
            targetPath.moveTo(segmentStartX, y)
            if (isPastBucket) {
                pastStarted = true
                pastSegmentOpen = true
            } else {
                futureStarted = true
                futureSegmentOpen = true
            }
        } else {
            targetPath.lineTo(segmentStartX, y)
        }
        targetPath.lineTo(segmentEndX, y)
    }

    if (futureStarted) {
        drawPath(
            path = futurePath,
            color = inactiveToneColor.copy(alpha = 0.42f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    if (pastStarted) {
        drawPath(
            path = pastPath,
            color = activeToneColor.copy(alpha = 0.72f + 0.16f * glowPulse),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }

    buckets.forEachIndexed { index, bucket ->
        val y = yForTone(bucket) ?: return@forEachIndexed
        val isActiveBucket = index.toFloat() <= activeThresholdBucketIndex
        val centerX = leftPadding + bucketWidth * (index.toFloat() - bucketOffset + 0.5f)
        val color =
            if (isActiveBucket) {
                activeToneColor.copy(alpha = 0.72f + 0.16f * glowPulse)
            } else {
                inactiveToneColor.copy(alpha = 0.34f + 0.18f * bucket.confidence)
            }
        drawCircle(
            color = color,
            radius = pointRadius,
            center = Offset(centerX, y),
        )
    }
}

private fun toneBucketColor(
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    alpha: Float,
    strength: Float,
): Color =
    if (isActive) {
        activeColor.copy(alpha = alpha.coerceIn(0f, 1f))
    } else {
        inactiveColor.copy(
            alpha = (0.18f + 0.28f * strength + 0.12f * alpha.coerceIn(0f, 1f)).coerceIn(0f, 0.55f),
        )
    }

private fun visibleFlashSegments(
    segments: List<FlashSignalToneSegment>,
    viewport: FlashSignalViewport,
): List<FlashSignalToneSegment> =
    segments.filter { segment ->
        segment.endSample.toFloat() >= viewport.startSample &&
            segment.startSample.toFloat() <= viewport.endSample
    }

private fun DrawScope.drawMicroBarsAcrossSegment(
    segment: FlashSignalToneSegment,
    viewport: FlashSignalViewport,
    startX: Float,
    endX: Float,
    minBarHeight: Float,
    maxBarHeight: Float,
    centerY: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    glowPulse: Float,
    minVisualWidth: Float,
) {
    val visualWidth = (endX - startX).coerceAtLeast(minVisualWidth)
    val spacing = FlashSegmentMicroBarSpacingDp.dp.toPx()
    val barWidth = FlashSegmentMicroBarWidthDp.dp.toPx().coerceAtMost(visualWidth)
    val barCount = (visualWidth / spacing).toInt().coerceAtLeast(1)
    val visibleStartSample = maxOf(segment.startSample.toFloat(), viewport.startSample)
    val visibleEndSample = minOf(segment.endSample.toFloat(), viewport.endSample)
    repeat(barCount) { barIndex ->
        val fraction = (barIndex + 0.5f) / barCount.toFloat()
        val x = startX + visualWidth * fraction
        val sample = visibleStartSample + (visibleEndSample - visibleStartSample) * fraction
        val isActive = sample <= viewport.playheadSample
        val edgeGlow = activeEdgeGlow(sample, viewport, segment.sampleCount)
        val texture = deterministicSignalTexture(segment, barIndex)
        val height = minBarHeight + (maxBarHeight - minBarHeight) * texture
        val color =
            flashSegmentColor(
                isActive = isActive,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                confidence = 0.78f + 0.22f * texture,
                glowPulse = glowPulse,
                edgeGlow = edgeGlow,
            )
        drawRoundRect(
            color = color,
            topLeft = Offset(x - barWidth / 2f, centerY - height / 2f),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
    }
}

private fun DrawScope.drawEnergyPulseClusterAcrossSegment(
    segment: FlashSignalToneSegment,
    viewport: FlashSignalViewport,
    startX: Float,
    endX: Float,
    baselineY: Float,
    direction: Float,
    minBarHeight: Float,
    maxBarHeight: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    glowPulse: Float,
    minVisualWidth: Float,
) {
    val visualWidth = (endX - startX).coerceAtLeast(minVisualWidth)
    val visibleStartSample = maxOf(segment.startSample.toFloat(), viewport.startSample)
    val visibleEndSample = minOf(segment.endSample.toFloat(), viewport.endSample)
    val pulseCount =
        when {
            visualWidth < 10.dp.toPx() -> 1
            visualWidth < 22.dp.toPx() -> 2
            visualWidth < 42.dp.toPx() -> 3
            else -> 5
        }
    val strokeWidth = (visualWidth / (pulseCount * 2.4f)).coerceIn(2.2.dp.toPx(), 7.dp.toPx())
    repeat(pulseCount) { pulseIndex ->
        val fraction = (pulseIndex + 0.5f) / pulseCount.toFloat()
        val x = startX + visualWidth * fraction
        val sample = visibleStartSample + (visibleEndSample - visibleStartSample) * fraction
        val edgeGlow = activeEdgeGlow(sample, viewport, segment.sampleCount)
        val texture = deterministicSignalTexture(segment, pulseIndex)
        val envelope = 1f - kotlin.math.abs(fraction - 0.5f) * 0.72f
        val height = minBarHeight + (maxBarHeight - minBarHeight) * (0.42f + 0.58f * texture * envelope)
        val color =
            flashSegmentColor(
                isActive = sample <= viewport.playheadSample,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                confidence = 0.72f + 0.28f * texture,
                glowPulse = glowPulse,
                edgeGlow = edgeGlow,
            )
        drawLine(
            color = color,
            start = Offset(x, baselineY),
            end = Offset(x, baselineY + direction * height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawPitchSegmentedBar(
    segment: FlashSignalToneSegment,
    viewport: FlashSignalViewport,
    startX: Float,
    endX: Float,
    y: Float,
    activeBarHeight: Float,
    inactiveBarHeight: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    glowPulse: Float,
) {
    val availableWidth = endX - startX
    if (availableWidth <= 0f) {
        return
    }

    val gapInset = FlashPitchSegmentGapDp.dp.toPx() / 2f
    val minVisibleWidth = FlashPitchSegmentMinVisibleWidthDp.dp.toPx()
    val canApplyGap = availableWidth > gapInset * 2f + minVisibleWidth
    val drawStartX = if (canApplyGap) startX + gapInset else startX
    val drawEndX = if (canApplyGap) endX - gapInset else endX
    val visualWidth = (drawEndX - drawStartX).coerceAtLeast(1f)
    val visibleStartSample = maxOf(segment.startSample.toFloat(), viewport.startSample)
    val visibleEndSample = minOf(segment.endSample.toFloat(), viewport.endSample)
    val centerSample = (visibleStartSample + visibleEndSample) / 2f
    val edgeGlow = activeEdgeGlow(centerSample, viewport, segment.sampleCount)
    val isActive = centerSample <= viewport.playheadSample
    val barHeight = if (isActive) activeBarHeight else inactiveBarHeight
    val color =
        flashSegmentColor(
            isActive = isActive,
            activeColor = activeToneColor,
            inactiveColor = inactiveToneColor,
            confidence = 1f,
            glowPulse = glowPulse,
            edgeGlow = edgeGlow,
        )
    // Draw Pitch as real segmented bars. Round line caps visually bridge short
    // gaps, so the bar geometry owns the cut between adjacent low/high symbols.
    drawRoundRect(
        color = color,
        topLeft = Offset(drawStartX, y - barHeight / 2f),
        size = Size(visualWidth, barHeight),
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
    )
}

private fun sampleToViewportX(
    sample: Float,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    innerWidth: Float,
): Float =
    leftPadding +
        ((sample - viewport.startSample) / viewport.sampleCount)
            .coerceIn(0f, 1f) * innerWidth

private fun flashSegmentColor(
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    confidence: Float,
    glowPulse: Float,
    edgeGlow: Float = 0f,
): Color =
    if (isActive) {
        activeColor.copy(alpha = (0.58f + 0.20f * glowPulse + 0.20f * edgeGlow).coerceIn(0f, 1f))
    } else {
        inactiveColor.copy(alpha = (0.26f + 0.18f * confidence.coerceIn(0f, 1f) + 0.16f * edgeGlow).coerceIn(0f, 0.76f))
    }

private fun activeEdgeGlow(
    sample: Float,
    viewport: FlashSignalViewport,
    segmentSampleCount: Int,
): Float {
    val glowWindow = maxOf(segmentSampleCount.toFloat() * 0.42f, viewport.sampleCount * 0.018f)
    return (1f - kotlin.math.abs(sample - viewport.playheadSample) / glowWindow)
        .coerceIn(0f, 1f)
}

private fun deterministicSignalTexture(
    segment: FlashSignalToneSegment,
    index: Int,
): Float {
    val seed =
        segment.startSample * 31 +
            segment.endSample * 17 +
            index * 13 +
            if (segment.tone == FskDominantTone.High) 7 else 3
    val folded = ((seed xor (seed ushr 7)) and 0xF).toFloat() / 15f
    return 0.58f + 0.42f * folded
}

private fun FskEnergyBucket.heldToneStrength(
    buckets: List<FskEnergyBucket>,
    index: Int,
    tone: FskDominantTone,
): Float {
    val ownStrength = strengthForTone(tone)
    var heldStrength = ownStrength
    for (offset in FlashToneHoldDecayByBucketOffset.indices) {
        val previousBucket = buckets.getOrNull(index - offset - 1) ?: break
        if (previousBucket.dominantTone == tone) {
            heldStrength =
                maxOf(
                    heldStrength,
                    previousBucket.strengthForTone(tone) * FlashToneHoldDecayByBucketOffset[offset],
                )
        }
    }
    return heldStrength.coerceIn(0f, 1f)
}

private fun FskEnergyBucket.strengthForTone(tone: FskDominantTone): Float =
    when (tone) {
        FskDominantTone.High -> highStrength
        FskDominantTone.Low -> lowStrength
        FskDominantTone.Unknown -> 0f
    }

private val FlashToneHoldDecayByBucketOffset = floatArrayOf(0.52f, 0.26f)
private const val FlashToneHoldAlphaBoost = 0.18f
private const val FlashSegmentMicroBarSpacingDp = 4
private const val FlashSegmentMicroBarWidthDp = 2
private const val FlashPitchSegmentGapDp = 3
private const val FlashPitchSegmentMinVisibleWidthDp = 2
