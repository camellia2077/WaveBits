package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal enum class FskDominantTone {
    Low,
    High,
    Unknown,
}

internal data class FskEnergyBucket(
    val lowStrength: Float,
    val highStrength: Float,
    val amplitude: Float,
    val dominantTone: FskDominantTone,
    val confidence: Float,
)

internal fun buildFskEnergyBuckets(
    pcm: ShortArray,
    sampleRateHz: Int,
    currentSample: Float,
    windowSampleCount: Int,
    targetBucketCount: Int,
): List<FskEnergyBucket> {
    if (pcm.isEmpty() || sampleRateHz <= 0) {
        return emptyList()
    }

    val safeBucketCount = targetBucketCount.coerceAtLeast(1)
    val safeWindowSampleCount = windowSampleCount.coerceAtLeast(1)
    val pastWindowSamples = safeWindowSampleCount * FlashSignalPlayheadAnchorRatio
    val windowStart = currentSample - pastWindowSamples
    val bucketSampleWidth = safeWindowSampleCount.toFloat() / safeBucketCount.toFloat()
    val rawBuckets = ArrayList<RawFskEnergyBucket>(safeBucketCount)

    repeat(safeBucketCount) { bucketIndex ->
        val bucketStart = windowStart + bucketSampleWidth * bucketIndex.toFloat()
        val bucketEnd = bucketStart + bucketSampleWidth
        val startIndex = floor(bucketStart.toDouble()).toInt().coerceAtLeast(0)
        val endIndexExclusive = ceil(bucketEnd.toDouble()).toInt().coerceAtMost(pcm.size)

        if (endIndexExclusive - startIndex < FlashSignalMinimumAnalysisSamples) {
            rawBuckets += RawFskEnergyBucket(0f, 0f, 0f)
        } else {
            val lowPower =
                goertzelPower(
                    pcm = pcm,
                    startIndex = startIndex,
                    endIndexExclusive = endIndexExclusive,
                    sampleRateHz = sampleRateHz,
                    targetFrequencyHz = FlashSignalLowToneHz,
                )
            val highPower =
                goertzelPower(
                    pcm = pcm,
                    startIndex = startIndex,
                    endIndexExclusive = endIndexExclusive,
                    sampleRateHz = sampleRateHz,
                    targetFrequencyHz = FlashSignalHighToneHz,
                )
            val amplitude =
                peakAmplitude(
                    pcm = pcm,
                    startIndex = startIndex,
                    endIndexExclusive = endIndexExclusive,
                )
            rawBuckets +=
                RawFskEnergyBucket(
                    lowPower = lowPower,
                    highPower = highPower,
                    amplitude = amplitude,
                )
        }
    }

    val maxPower = rawBuckets.maxOfOrNull { max(it.lowPower, it.highPower) }?.coerceAtLeast(1e-6f) ?: 1f
    return rawBuckets.map { bucket ->
        val lowStrength =
            (
                sqrt((bucket.lowPower / maxPower).coerceIn(0f, 1f).toDouble()).toFloat() *
                    (0.34f + 0.66f * bucket.amplitude)
            ).coerceIn(0f, 1f)
        val highStrength =
            (
                sqrt((bucket.highPower / maxPower).coerceIn(0f, 1f).toDouble()).toFloat() *
                    (0.34f + 0.66f * bucket.amplitude)
            ).coerceIn(0f, 1f)
        val powerSum = bucket.lowPower + bucket.highPower
        val confidence =
            if (powerSum > 1e-6f) {
                abs(bucket.highPower - bucket.lowPower) / powerSum
            } else {
                0f
            }
        val dominantTone =
            when {
                bucket.amplitude < FlashSignalSilenceThreshold -> FskDominantTone.Unknown
                confidence < FlashSignalConfidenceThreshold -> FskDominantTone.Unknown
                bucket.highPower > bucket.lowPower -> FskDominantTone.High
                else -> FskDominantTone.Low
            }
        FskEnergyBucket(
            lowStrength = lowStrength,
            highStrength = highStrength,
            amplitude = bucket.amplitude,
            dominantTone = dominantTone,
            confidence = confidence.coerceIn(0f, 1f),
        )
    }
}

internal fun buildFskEnergyBucketsFromFollowData(
    followData: PayloadFollowViewData,
    currentSample: Float,
    windowSampleCount: Int,
    targetBucketCount: Int,
): List<FskEnergyBucket> {
    if (!followData.followAvailable || followData.binaryGroupTimeline.isEmpty()) {
        return emptyList()
    }

    // This path is a timeline-driven display layer, not a decoder. For flash
    // playback the libs follow data already maps each bit to its sample span,
    // so the visual can stay aligned with the payload even when the PCM is too
    // long or preview-only for cheap frequency analysis.
    val safeBucketCount = targetBucketCount.coerceAtLeast(1)
    val safeWindowSampleCount = windowSampleCount.coerceAtLeast(1)
    val pastWindowSamples = safeWindowSampleCount * FlashSignalPlayheadAnchorRatio
    val windowStart = currentSample - pastWindowSamples
    val bucketSampleWidth = safeWindowSampleCount.toFloat() / safeBucketCount.toFloat()
    val buckets = ArrayList<FskEnergyBucket>(safeBucketCount)
    var entryCursor = 0
    val entries = followData.binaryGroupTimeline

    repeat(safeBucketCount) { bucketIndex ->
        val bucketStart = windowStart + bucketSampleWidth * bucketIndex.toFloat()
        val bucketEnd = bucketStart + bucketSampleWidth
        while (
            entryCursor < entries.size &&
            entries[entryCursor].startSample + entries[entryCursor].sampleCount <= bucketStart
        ) {
            entryCursor += 1
        }

        var scanIndex = entryCursor
        var lowWeight = 0f
        var highWeight = 0f
        while (scanIndex < entries.size) {
            val entry = entries[scanIndex]
            if (entry.startSample >= bucketEnd) {
                break
            }
            val overlap =
                overlapLength(
                    firstStart = bucketStart,
                    firstEnd = bucketEnd,
                    secondStart = entry.startSample.toFloat(),
                    secondEnd = (entry.startSample + entry.sampleCount).toFloat(),
                )
            if (overlap > 0f) {
                val bitWeights = binaryTokenWeights(followData.binaryTokens.getOrNull(entry.groupIndex))
                // Add a shallow per-symbol envelope so repeated 0s or 1s still
                // read as separate payload bits instead of one continuous band.
                val displayOverlap =
                    overlap *
                        timelineSymbolDisplayEnvelope(
                            symbolStart = entry.startSample.toFloat(),
                            symbolSampleCount = entry.sampleCount,
                            overlapStart = maxOf(bucketStart, entry.startSample.toFloat()),
                            overlapEnd = minOf(bucketEnd, (entry.startSample + entry.sampleCount).toFloat()),
                        )
                lowWeight += displayOverlap * bitWeights.lowWeight
                highWeight += displayOverlap * bitWeights.highWeight
            }
            scanIndex += 1
        }

        val coveredWeight = lowWeight + highWeight
        val amplitude = (coveredWeight / bucketSampleWidth).coerceIn(0f, 1f)
        val lowStrength = (lowWeight / bucketSampleWidth).coerceIn(0f, 1f)
        val highStrength = (highWeight / bucketSampleWidth).coerceIn(0f, 1f)
        val confidence =
            if (coveredWeight > 1e-6f) {
                abs(highWeight - lowWeight) / coveredWeight
            } else {
                0f
            }
        val dominantTone =
            when {
                amplitude < FlashSignalSilenceThreshold -> FskDominantTone.Unknown
                confidence < FlashSignalConfidenceThreshold -> FskDominantTone.Unknown
                highWeight > lowWeight -> FskDominantTone.High
                else -> FskDominantTone.Low
            }
        buckets +=
            FskEnergyBucket(
                lowStrength = if (dominantTone == FskDominantTone.High) lowStrength.coerceAtLeast(0.10f * amplitude) else lowStrength,
                highStrength = if (dominantTone == FskDominantTone.Low) highStrength.coerceAtLeast(0.10f * amplitude) else highStrength,
                amplitude = amplitude,
                dominantTone = dominantTone,
                confidence = confidence.coerceIn(0f, 1f),
            )
    }
    return buckets
}

private data class BinaryTokenWeights(
    val lowWeight: Float,
    val highWeight: Float,
)

private fun binaryTokenWeights(binaryToken: String?): BinaryTokenWeights {
    val bits = binaryToken.orEmpty().filter { it == '0' || it == '1' }
    if (bits.isEmpty()) {
        return BinaryTokenWeights(lowWeight = 0f, highWeight = 0f)
    }
    val highCount = bits.count { it == '1' }.toFloat()
    val lowCount = bits.length.toFloat() - highCount
    return BinaryTokenWeights(
        lowWeight = lowCount / bits.length.toFloat(),
        highWeight = highCount / bits.length.toFloat(),
    )
}

private fun overlapLength(
    firstStart: Float,
    firstEnd: Float,
    secondStart: Float,
    secondEnd: Float,
): Float = (minOf(firstEnd, secondEnd) - maxOf(firstStart, secondStart)).coerceAtLeast(0f)

private fun timelineSymbolDisplayEnvelope(
    symbolStart: Float,
    symbolSampleCount: Int,
    overlapStart: Float,
    overlapEnd: Float,
): Float {
    if (symbolSampleCount <= 0 || overlapEnd <= overlapStart) {
        return 0f
    }

    val symbolProgress =
        (((overlapStart + overlapEnd) / 2f - symbolStart) / symbolSampleCount.toFloat())
            .coerceIn(0f, 1f)
    val edgeRatio = (1f - FlashSignalTimelineSymbolCoreRatio) / 2f
    if (edgeRatio <= 0f) {
        return 1f
    }

    val distanceToEdge = minOf(symbolProgress, 1f - symbolProgress)
    if (distanceToEdge >= edgeRatio) {
        return 1f
    }
    val ramp = (distanceToEdge / edgeRatio).coerceIn(0f, 1f)
    return FlashSignalTimelineSymbolEdgeFloor +
        (1f - FlashSignalTimelineSymbolEdgeFloor) * ramp
}

private data class RawFskEnergyBucket(
    val lowPower: Float,
    val highPower: Float,
    val amplitude: Float,
)

private fun goertzelPower(
    pcm: ShortArray,
    startIndex: Int,
    endIndexExclusive: Int,
    sampleRateHz: Int,
    targetFrequencyHz: Double,
): Float {
    val sampleCount = endIndexExclusive - startIndex
    if (sampleCount < 2 || sampleRateHz <= 0) {
        return 0f
    }

    val omega = 2.0 * PI * targetFrequencyHz / sampleRateHz.toDouble()
    val coeff = 2.0 * cos(omega)
    var q0 = 0.0
    var q1 = 0.0
    var q2 = 0.0
    for (index in startIndex until endIndexExclusive) {
        val sample = pcm[index].toDouble() / Short.MAX_VALUE.toDouble()
        q0 = coeff * q1 - q2 + sample
        q2 = q1
        q1 = q0
    }
    return max(0.0, q1 * q1 + q2 * q2 - coeff * q1 * q2).toFloat()
}

private fun peakAmplitude(
    pcm: ShortArray,
    startIndex: Int,
    endIndexExclusive: Int,
): Float {
    var peak = 0f
    for (index in startIndex until endIndexExclusive) {
        peak = max(peak, abs(pcm[index].toFloat() / Short.MAX_VALUE.toFloat()))
    }
    return peak.coerceIn(0f, 1f)
}

internal const val FlashSignalLowToneHz = 400.0
internal const val FlashSignalHighToneHz = 800.0
internal const val FlashSignalPlayheadAnchorRatio = 0.40f
internal const val FlashSignalSilenceThreshold = 0.02f
internal const val FlashSignalConfidenceThreshold = 0.12f
internal const val FlashSignalMinimumAnalysisSamples = 96
private const val FlashSignalTimelineSymbolCoreRatio = 0.62f
private const val FlashSignalTimelineSymbolEdgeFloor = 0.12f

internal fun flashSignalActiveWindowBucketCount(flashVoicingStyle: FlashVoicingStyleOption?): Int =
    flashVoicingStyle?.flashVisualActiveWindowBucketCount ?: 3

internal fun visualizationAnalysisSampleStep(
    sampleRateHz: Int,
    totalSamples: Int,
): Int =
    (sampleRateHz.coerceAtLeast(1) / VisualizationAnalysisFramesPerSecond)
        .coerceAtLeast(1)
        .coerceAtMost(totalSamples.coerceAtLeast(1))

internal fun quantizeVisualizationDisplayedSamples(
    displayedSamples: Float,
    sampleStep: Int,
    totalSamples: Int,
): Float {
    val safeStep = sampleStep.coerceAtLeast(1)
    val safeTotalSamples = totalSamples.coerceAtLeast(1)
    val clampedDisplayedSamples = displayedSamples.coerceIn(0f, safeTotalSamples.toFloat())
    return (clampedDisplayedSamples / safeStep.toFloat())
        .roundToInt()
        .times(safeStep)
        .coerceIn(0, safeTotalSamples)
        .toFloat()
}

private const val VisualizationAnalysisFramesPerSecond = 24
