package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import kotlin.math.ceil

@Composable
internal fun AudioFlashSignalVisualizer(
    input: FlashSignalVisualizationInput,
    isPlaying: Boolean,
    mode: FlashSignalVisualizationMode,
    flashVoicingStyle: FlashVoicingStyleOption?,
    modifier: Modifier = Modifier,
) {
    val pcm = input.pcm
    val sampleRateHz = input.sampleRateHz
    if (pcm.isEmpty() || sampleRateHz <= 0) {
        return
    }

    val visualTokens = appThemeVisualTokens()
    val density = LocalDensity.current
    val visualTransition = rememberInfiniteTransition(label = "flashSignalVisualizer")
    val glowPulseAnimated by visualTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "flashSignalGlowPulse",
    )
    val sweepAnimated by visualTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "flashSignalSweep",
    )
    val totalSamples = pcm.size.coerceAtLeast(1)
    val clampedDisplayedSamples = input.pcmDisplayedSamples.coerceIn(0, totalSamples)
    val followTimelineSource = input.bucketSource as? FlashSignalBucketSource.FollowTimeline
    val followTimelineTotalSamples =
        followTimelineSource
            ?.followData
            ?.totalPcmSampleCount
            ?.coerceAtLeast(followTimelineSource.displayedSamples)
            ?.coerceAtLeast(1)
            ?: 1
    val clampedFollowDisplayedSamples =
        followTimelineSource
            ?.displayedSamples
            ?.coerceIn(0, followTimelineTotalSamples)
            ?: clampedDisplayedSamples
    val displayedSamplePosition = clampedDisplayedSamples.toFloat()
    val followDisplayedSamplePosition = clampedFollowDisplayedSamples.toFloat()
    val glowPulse = if (isPlaying) glowPulseAnimated else 0.82f
    val sweepPhase = if (isPlaying) sweepAnimated else 0.24f
    val analysisCache = remember(pcm, sampleRateHz, input.bucketSource) { FlashSignalAnalysisCache() }
    val analysisSampleStep =
        remember(sampleRateHz, totalSamples) {
            visualizationAnalysisSampleStep(sampleRateHz = sampleRateHz, totalSamples = totalSamples)
        }
    val analysisDisplayedSamplePosition =
        remember(displayedSamplePosition, analysisSampleStep, totalSamples) {
            quantizeVisualizationDisplayedSamples(
                displayedSamples = displayedSamplePosition,
                sampleStep = analysisSampleStep,
                totalSamples = totalSamples,
            )
        }
    val followAnalysisSampleStep =
        remember(sampleRateHz, followTimelineTotalSamples) {
            visualizationAnalysisSampleStep(sampleRateHz = sampleRateHz, totalSamples = followTimelineTotalSamples)
        }
    val followAnalysisDisplayedSamplePosition =
        remember(followDisplayedSamplePosition, followAnalysisSampleStep, followTimelineTotalSamples) {
            quantizeVisualizationDisplayedSamples(
                displayedSamples = followDisplayedSamplePosition,
                sampleStep = followAnalysisSampleStep,
                totalSamples = followTimelineTotalSamples,
            )
        }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(170.dp),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val targetBucketCount =
            remember(widthPx) {
                val bucketSpacingPx = with(density) { 6.dp.toPx() }
                ceil((widthPx / bucketSpacingPx).toDouble())
                    .toInt()
                    .coerceIn(FlashSignalMinBucketCount, FlashSignalMaxBucketCount)
            }
        val windowSampleCount =
            remember(sampleRateHz) {
                sampleRateHz.coerceAtLeast(1)
            }
        val activeWindowBucketCount =
            remember(flashVoicingStyle) {
                flashSignalActiveWindowBucketCount(flashVoicingStyle)
            }
        val fixedTimelineFrame =
            remember(input.bucketSource) {
                (input.bucketSource as? FlashSignalBucketSource.FollowTimeline)
                    ?.followData
                    ?.let { followData ->
                        val segments = buildFlashSignalToneSegments(followData)
                        if (segments.isNotEmpty()) {
                            FlashSignalFixedTimelineFrame(
                                segments = segments,
                                totalSamples = followData.totalPcmSampleCount.coerceAtLeast(1),
                            )
                        } else {
                            null
                        }
                    }
            }
        val bitReadoutFrame =
            fixedTimelineFrame?.let { frame ->
                flashBitReadoutFrame(
                    segments = frame.segments,
                    sample = followDisplayedSamplePosition,
                )
            }
        val bucketFrame =
            remember(
                pcm,
                sampleRateHz,
                input.bucketSource,
                targetBucketCount,
                windowSampleCount,
                analysisDisplayedSamplePosition,
                followAnalysisDisplayedSamplePosition,
                fixedTimelineFrame,
            ) {
                if (fixedTimelineFrame != null) {
                    return@remember FlashSignalBucketFrame.Empty
                }
                when (val bucketSource = input.bucketSource) {
                    is FlashSignalBucketSource.FollowTimeline -> {
                        val followBuckets =
                            analysisCache
                                .followBuckets(
                                    currentSample = followAnalysisDisplayedSamplePosition,
                                    windowSampleCount = windowSampleCount,
                                    targetBucketCount = targetBucketCount,
                                ) {
                                    buildFskEnergyBucketsFromFollowData(
                                        followData = bucketSource.followData,
                                        currentSample = followAnalysisDisplayedSamplePosition,
                                        windowSampleCount = windowSampleCount,
                                        targetBucketCount = targetBucketCount,
                                    )
                                }
                        if (followBuckets.isNotEmpty()) {
                            FlashSignalBucketFrame(
                                buckets = followBuckets,
                                displayedSamplePosition = followDisplayedSamplePosition,
                                analysisDisplayedSamplePosition = followAnalysisDisplayedSamplePosition,
                            )
                        } else {
                            FlashSignalBucketFrame(
                                buckets =
                                    analysisCache.pcmBuckets(
                                        currentSample = analysisDisplayedSamplePosition,
                                        windowSampleCount = windowSampleCount,
                                        targetBucketCount = targetBucketCount,
                                    ) {
                                        buildFskEnergyBuckets(
                                            pcm = pcm,
                                            sampleRateHz = sampleRateHz,
                                            currentSample = analysisDisplayedSamplePosition,
                                            windowSampleCount = windowSampleCount,
                                            targetBucketCount = targetBucketCount,
                                        )
                                    },
                                displayedSamplePosition = displayedSamplePosition,
                                analysisDisplayedSamplePosition = analysisDisplayedSamplePosition,
                            )
                        }
                    }

                    is FlashSignalBucketSource.Pcm ->
                        FlashSignalBucketFrame(
                            buckets =
                                analysisCache.pcmBuckets(
                                    currentSample = analysisDisplayedSamplePosition,
                                    windowSampleCount = windowSampleCount,
                                    targetBucketCount = targetBucketCount,
                                ) {
                                    buildFskEnergyBuckets(
                                        pcm = pcm,
                                        sampleRateHz = sampleRateHz,
                                        currentSample = analysisDisplayedSamplePosition,
                                        windowSampleCount = windowSampleCount,
                                        targetBucketCount = targetBucketCount,
                                    )
                                },
                            displayedSamplePosition = displayedSamplePosition,
                            analysisDisplayedSamplePosition = analysisDisplayedSamplePosition,
                        )
                }
            }
        val buckets = bucketFrame.buckets

        val activeToneColor = MaterialTheme.colorScheme.primary
        val inactiveToneColor = visualTokens.visualizationInactiveToneColor
        val glowColor = MaterialTheme.colorScheme.onPrimaryContainer
        val baseBackground = visualTokens.visualizationBaseBackgroundColor
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(112.dp),
            ) {
                if (buckets.isEmpty() && fixedTimelineFrame == null) {
                    return@Canvas
                }

                val corner = CornerRadius(24f, 24f)
                val leftPadding = 12.dp.toPx()
                val rightPadding = 12.dp.toPx()
                val topPadding = 12.dp.toPx()
                val bottomPadding = 12.dp.toPx()
                val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
                val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
                val bucketWidth = if (buckets.isNotEmpty()) innerWidth / buckets.size.toFloat() else 1f
                val analysisBucketSampleWidth = if (buckets.isNotEmpty()) windowSampleCount.toFloat() / buckets.size.toFloat() else 1f
                val bucketOffset =
                    if (buckets.isNotEmpty()) {
                        ((bucketFrame.displayedSamplePosition - bucketFrame.analysisDisplayedSamplePosition) / analysisBucketSampleWidth)
                            .coerceIn(-FlashSignalMaxVisualBucketOffset, FlashSignalMaxVisualBucketOffset)
                    } else {
                        0f
                    }
                val scanHeadBucketIndex =
                    if (buckets.isNotEmpty()) {
                        (buckets.size * FlashSignalPlayheadAnchorRatio).coerceIn(0f, buckets.lastIndex.toFloat())
                    } else {
                        0f
                    }
                val activeThresholdBucketIndex =
                    if (buckets.isNotEmpty()) {
                        (scanHeadBucketIndex + bucketOffset).coerceIn(0f, buckets.lastIndex.toFloat())
                    } else {
                        0f
                    }
                val playheadX = leftPadding + innerWidth * FlashSignalPlayheadAnchorRatio
                val fixedViewport =
                    fixedTimelineFrame?.let { frame ->
                        // Follow timeline rendering is fixed in absolute sample space; playback only
                        // moves the viewport, so short Flash bits do not change shape while playing.
                        val windowStart = followDisplayedSamplePosition - windowSampleCount * FlashSignalPlayheadAnchorRatio
                        FlashSignalViewport(
                            startSample = windowStart,
                            endSample = windowStart + windowSampleCount,
                            playheadSample = followDisplayedSamplePosition,
                        )
                    }

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

                when (mode) {
                    FlashSignalVisualizationMode.ToneTracks ->
                        if (fixedTimelineFrame != null && fixedViewport != null) {
                            drawToneTrackSegments(
                                segments = fixedTimelineFrame.segments,
                                viewport = fixedViewport,
                                leftPadding = leftPadding,
                                topPadding = topPadding,
                                innerWidth = innerWidth,
                                innerHeight = innerHeight,
                                activeToneColor = activeToneColor,
                                inactiveToneColor = inactiveToneColor,
                                centerLineColor = centerLineColor,
                                glowPulse = glowPulse,
                            )
                        } else {
                            drawToneTracks(
                                buckets = buckets,
                                activeThresholdBucketIndex = activeThresholdBucketIndex,
                                activeWindowBucketCount = activeWindowBucketCount,
                                bucketOffset = bucketOffset,
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
                        }

                    FlashSignalVisualizationMode.ToneEnergy ->
                        if (fixedTimelineFrame != null && fixedViewport != null) {
                            drawToneEnergySegments(
                                segments = fixedTimelineFrame.segments,
                                viewport = fixedViewport,
                                leftPadding = leftPadding,
                                topPadding = topPadding,
                                innerWidth = innerWidth,
                                innerHeight = innerHeight,
                                activeToneColor = activeToneColor,
                                inactiveToneColor = inactiveToneColor,
                                centerLineColor = centerLineColor,
                                glowPulse = glowPulse,
                            )
                        } else {
                            drawToneEnergy(
                                buckets = buckets,
                                activeThresholdBucketIndex = activeThresholdBucketIndex,
                                activeWindowBucketCount = activeWindowBucketCount,
                                bucketOffset = bucketOffset,
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
                        }

                    FlashSignalVisualizationMode.PitchLadder ->
                        if (fixedTimelineFrame != null && fixedViewport != null) {
                            drawPitchLadderSegments(
                                segments = fixedTimelineFrame.segments,
                                viewport = fixedViewport,
                                leftPadding = leftPadding,
                                topPadding = topPadding,
                                innerWidth = innerWidth,
                                innerHeight = innerHeight,
                                activeToneColor = activeToneColor,
                                inactiveToneColor = inactiveToneColor,
                                centerLineColor = centerLineColor,
                                glowPulse = glowPulse,
                            )
                        } else {
                            drawPitchLadder(
                                buckets = buckets,
                                activeThresholdBucketIndex = activeThresholdBucketIndex,
                                activeWindowBucketCount = activeWindowBucketCount,
                                bucketOffset = bucketOffset,
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
                        }
                }

                drawLine(
                    color = glowColor.copy(alpha = 0.80f),
                    start = Offset(playheadX, topPadding),
                    end = Offset(playheadX, size.height - bottomPadding),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            if (bitReadoutFrame != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FlashBitReadoutRow(
                        cells = bitReadoutFrame.previousCells,
                        activeToneColor = activeToneColor,
                        baseBackground = baseBackground,
                        isPreviousRow = true,
                    )
                    FlashBitReadoutRow(
                        cells = bitReadoutFrame.currentCells,
                        activeToneColor = activeToneColor,
                        baseBackground = baseBackground,
                        isPreviousRow = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashBitReadoutRow(
    cells: List<FlashBitReadoutCell>,
    activeToneColor: androidx.compose.ui.graphics.Color,
    baseBackground: androidx.compose.ui.graphics.Color,
    isPreviousRow: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        cells.forEach { cell ->
            val cellBackground =
                when {
                    cell.isCurrent -> activeToneColor.copy(alpha = 0.18f)
                    cell.bit != null && isPreviousRow -> baseBackground.copy(alpha = 0.40f)
                    cell.bit != null -> baseBackground.copy(alpha = 0.52f)
                    else -> baseBackground.copy(alpha = 0.24f)
                }
            val cellColor =
                when {
                    cell.isCurrent -> activeToneColor.copy(alpha = 0.94f)
                    cell.bit != null && isPreviousRow -> activeToneColor.copy(alpha = 0.48f)
                    cell.bit != null -> activeToneColor.copy(alpha = 0.72f)
                    else -> activeToneColor.copy(alpha = 0f)
                }
            Text(
                text = cell.bit?.toString().orEmpty(),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(
                            color = cellBackground,
                            shape = RoundedCornerShape(4.dp),
                        ).padding(vertical = 1.dp),
                color = cellColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                fontWeight = if (cell.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val FlashSignalMinBucketCount = 56
private const val FlashSignalMaxBucketCount = 124
private const val FlashSignalMaxVisualBucketOffset = 3f

private data class FlashSignalBucketFrame(
    val buckets: List<FskEnergyBucket>,
    val displayedSamplePosition: Float,
    val analysisDisplayedSamplePosition: Float,
) {
    companion object {
        val Empty =
            FlashSignalBucketFrame(
                buckets = emptyList(),
                displayedSamplePosition = 0f,
                analysisDisplayedSamplePosition = 0f,
            )
    }
}

private data class FlashSignalFixedTimelineFrame(
    val segments: List<FlashSignalToneSegment>,
    val totalSamples: Int,
)

internal data class FlashBitReadoutFrame(
    val currentGroupStartIndex: Int,
    val previousCells: List<FlashBitReadoutCell>,
    val currentCells: List<FlashBitReadoutCell>,
)

internal data class FlashBitReadoutCell(
    val bit: Char?,
    val isCurrent: Boolean,
)

internal fun flashBitReadoutFrame(
    segments: List<FlashSignalToneSegment>,
    sample: Float,
): FlashBitReadoutFrame? {
    if (segments.isEmpty()) {
        return null
    }
    val playbackIndex = flashTimelinePlaybackIndex(segments = segments, sample = sample)
    val currentGroupStartIndex = (playbackIndex.revealedIndex.coerceAtLeast(0) / FlashBitReadoutGroupSize) * FlashBitReadoutGroupSize
    val previousGroupStartIndex = currentGroupStartIndex - FlashBitReadoutGroupSize
    return FlashBitReadoutFrame(
        currentGroupStartIndex = currentGroupStartIndex,
        previousCells =
            buildFlashBitReadoutCells(
                segments = segments,
                groupStartIndex = previousGroupStartIndex,
                revealThroughIndex = previousGroupStartIndex + FlashBitReadoutGroupSize - 1,
                currentIndex = null,
            ),
        currentCells =
            buildFlashBitReadoutCells(
                segments = segments,
                groupStartIndex = currentGroupStartIndex,
                revealThroughIndex = playbackIndex.revealedIndex,
                currentIndex = playbackIndex.currentIndex,
            ),
    )
}

private fun buildFlashBitReadoutCells(
    segments: List<FlashSignalToneSegment>,
    groupStartIndex: Int,
    revealThroughIndex: Int,
    currentIndex: Int?,
): List<FlashBitReadoutCell> =
    List(FlashBitReadoutGroupSize) { slot ->
        val segmentIndex = groupStartIndex + slot
        val segment = segments.getOrNull(segmentIndex)
        FlashBitReadoutCell(
            bit =
                if (segment != null && segmentIndex >= 0 && segmentIndex <= revealThroughIndex) {
                    segment.tone.toBitChar()
                } else {
                    null
                },
            isCurrent = currentIndex == segmentIndex,
        )
    }

private data class FlashTimelinePlaybackIndex(
    val currentIndex: Int?,
    val revealedIndex: Int,
)

private fun flashTimelinePlaybackIndex(
    segments: List<FlashSignalToneSegment>,
    sample: Float,
): FlashTimelinePlaybackIndex {
    var low = 0
    var high = segments.lastIndex
    var previousIndex = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        val segment = segments[mid]
        when {
            sample < segment.startSample -> high = mid - 1
            sample >= segment.endSample -> {
                previousIndex = mid
                low = mid + 1
            }
            else -> return FlashTimelinePlaybackIndex(currentIndex = mid, revealedIndex = mid)
        }
    }
    return FlashTimelinePlaybackIndex(currentIndex = null, revealedIndex = previousIndex)
}

private fun FskDominantTone.toBitChar(): Char? =
    when (this) {
        FskDominantTone.Low -> '0'
        FskDominantTone.High -> '1'
        FskDominantTone.Unknown -> null
    }

private const val FlashBitReadoutGroupSize = 8

private class FlashSignalAnalysisCache {
    private val bucketsByKey = LinkedHashMap<FlashSignalAnalysisCacheKey, List<FskEnergyBucket>>()

    fun pcmBuckets(
        currentSample: Float,
        windowSampleCount: Int,
        targetBucketCount: Int,
        build: () -> List<FskEnergyBucket>,
    ): List<FskEnergyBucket> =
        bucketsFor(
            FlashSignalAnalysisCacheKey(
                source = "pcm",
                currentSample = currentSample.toInt(),
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount,
            ),
            build,
        )

    fun followBuckets(
        currentSample: Float,
        windowSampleCount: Int,
        targetBucketCount: Int,
        build: () -> List<FskEnergyBucket>,
    ): List<FskEnergyBucket> =
        bucketsFor(
            FlashSignalAnalysisCacheKey(
                source = "follow",
                currentSample = currentSample.toInt(),
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount,
            ),
            build,
        )

    private fun bucketsFor(
        key: FlashSignalAnalysisCacheKey,
        build: () -> List<FskEnergyBucket>,
    ): List<FskEnergyBucket> {
        bucketsByKey[key]?.let { return it }
        val buckets = build()
        bucketsByKey[key] = buckets
        if (bucketsByKey.size > FlashSignalAnalysisCacheMaxEntries) {
            val eldestKey = bucketsByKey.keys.first()
            bucketsByKey.remove(eldestKey)
        }
        return buckets
    }
}

private data class FlashSignalAnalysisCacheKey(
    val source: String,
    val currentSample: Int,
    val windowSampleCount: Int,
    val targetBucketCount: Int,
)

private const val FlashSignalAnalysisCacheMaxEntries = 12
