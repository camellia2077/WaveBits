package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

internal enum class FskDominantTone {
    Low,
    High,
    Unknown
}

internal data class FskEnergyBucket(
    val lowStrength: Float,
    val highStrength: Float,
    val amplitude: Float,
    val dominantTone: FskDominantTone,
    val confidence: Float
)

internal fun buildFskEnergyBuckets(
    pcm: ShortArray,
    sampleRateHz: Int,
    currentSample: Float,
    windowSampleCount: Int,
    targetBucketCount: Int
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
            val lowPower = goertzelPower(
                pcm = pcm,
                startIndex = startIndex,
                endIndexExclusive = endIndexExclusive,
                sampleRateHz = sampleRateHz,
                targetFrequencyHz = FlashSignalLowToneHz
            )
            val highPower = goertzelPower(
                pcm = pcm,
                startIndex = startIndex,
                endIndexExclusive = endIndexExclusive,
                sampleRateHz = sampleRateHz,
                targetFrequencyHz = FlashSignalHighToneHz
            )
            val amplitude = peakAmplitude(
                pcm = pcm,
                startIndex = startIndex,
                endIndexExclusive = endIndexExclusive
            )
            rawBuckets += RawFskEnergyBucket(
                lowPower = lowPower,
                highPower = highPower,
                amplitude = amplitude
            )
        }
    }

    val maxPower = rawBuckets.maxOfOrNull { max(it.lowPower, it.highPower) }?.coerceAtLeast(1e-6f) ?: 1f
    return rawBuckets.map { bucket ->
        val lowStrength = (sqrt((bucket.lowPower / maxPower).coerceIn(0f, 1f).toDouble()).toFloat() *
            (0.34f + 0.66f * bucket.amplitude)).coerceIn(0f, 1f)
        val highStrength = (sqrt((bucket.highPower / maxPower).coerceIn(0f, 1f).toDouble()).toFloat() *
            (0.34f + 0.66f * bucket.amplitude)).coerceIn(0f, 1f)
        val powerSum = bucket.lowPower + bucket.highPower
        val confidence = if (powerSum > 1e-6f) {
            abs(bucket.highPower - bucket.lowPower) / powerSum
        } else {
            0f
        }
        val dominantTone = when {
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
            confidence = confidence.coerceIn(0f, 1f)
        )
    }
}

private data class RawFskEnergyBucket(
    val lowPower: Float,
    val highPower: Float,
    val amplitude: Float
)

private fun goertzelPower(
    pcm: ShortArray,
    startIndex: Int,
    endIndexExclusive: Int,
    sampleRateHz: Int,
    targetFrequencyHz: Double
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
    endIndexExclusive: Int
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

internal fun flashSignalActiveWindowBucketCount(
    flashVoicingStyle: FlashVoicingStyleOption?
): Int {
    // Keep ritual_chant wider because it moves more slowly and benefits from a
    // longer scan-head window. Keep coded_burst tighter because it moves fast
    // and reads more clearly as a short 3-bar burst.
    return when (flashVoicingStyle) {
        FlashVoicingStyleOption.RitualChant -> 8
        else -> 3
    }
}
