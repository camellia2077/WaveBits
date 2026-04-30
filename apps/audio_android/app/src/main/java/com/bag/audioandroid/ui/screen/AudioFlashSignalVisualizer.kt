package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
                .height(112.dp),
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
        val buckets =
            remember(
                pcm,
                sampleRateHz,
                input.bucketSource,
                targetBucketCount,
                windowSampleCount,
                analysisDisplayedSamplePosition,
                followAnalysisDisplayedSamplePosition,
            ) {
                when (val bucketSource = input.bucketSource) {
                    is FlashSignalBucketSource.FollowTimeline ->
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
                            }.ifEmpty {
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
                                }
                            }

                    is FlashSignalBucketSource.Pcm ->
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
                        }
                }
            }

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
                ((buckets.size) * FlashSignalPlayheadAnchorRatio)
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

            when (mode) {
                FlashSignalVisualizationMode.ToneTracks ->
                    drawToneTracks(
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
                        glowPulse = glowPulse,
                    )

                FlashSignalVisualizationMode.ToneEnergy ->
                    drawToneEnergy(
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
                        glowPulse = glowPulse,
                    )

                FlashSignalVisualizationMode.PitchLadder ->
                    drawPitchLadder(
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
                        glowPulse = glowPulse,
                    )
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

private const val FlashSignalMinBucketCount = 56
private const val FlashSignalMaxBucketCount = 124

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
