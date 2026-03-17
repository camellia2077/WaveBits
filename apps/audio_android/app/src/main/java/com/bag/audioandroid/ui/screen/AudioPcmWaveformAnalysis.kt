package com.bag.audioandroid.ui.screen

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal data class PcmWaveBucket(
    val startSample: Int,
    val endSample: Int,
    val minAmplitude: Float,
    val maxAmplitude: Float,
    val peakAmplitude: Float
)

internal fun buildPcmWaveBuckets(
    pcm: ShortArray,
    currentSample: Float,
    windowSampleCount: Int,
    targetBucketCount: Int
): List<PcmWaveBucket> {
    if (pcm.isEmpty()) {
        return emptyList()
    }

    val safeBucketCount = targetBucketCount.coerceAtLeast(1)
    val safeWindowSampleCount = windowSampleCount.coerceAtLeast(1)
    val pastWindowSamples = safeWindowSampleCount * PcmWaveformPlayheadAnchorRatio
    val windowStart = currentSample - pastWindowSamples
    val bucketSampleWidth = safeWindowSampleCount.toFloat() / safeBucketCount.toFloat()
    val buckets = ArrayList<PcmWaveBucket>(safeBucketCount)

    for (bucketIndex in 0 until safeBucketCount) {
        val bucketStart = windowStart + bucketSampleWidth * bucketIndex.toFloat()
        val bucketEnd = bucketStart + bucketSampleWidth
        val startIndex = floor(bucketStart.toDouble()).toInt().coerceAtLeast(0)
        val endIndexExclusive = ceil(bucketEnd.toDouble()).toInt().coerceAtMost(pcm.size)

        var minAmplitude = 0f
        var maxAmplitude = 0f
        var peakAmplitude = 0f

        if (endIndexExclusive > startIndex) {
            minAmplitude = 1f
            maxAmplitude = -1f
            for (sampleIndex in startIndex until endIndexExclusive) {
                val normalized = (pcm[sampleIndex].toFloat() / Short.MAX_VALUE.toFloat()).coerceIn(-1f, 1f)
                minAmplitude = min(minAmplitude, normalized)
                maxAmplitude = max(maxAmplitude, normalized)
                peakAmplitude = max(peakAmplitude, abs(normalized))
            }
            if (minAmplitude > maxAmplitude) {
                minAmplitude = 0f
                maxAmplitude = 0f
            }
        }

        buckets += PcmWaveBucket(
            startSample = floor(bucketStart.toDouble()).toInt(),
            endSample = ceil(bucketEnd.toDouble()).toInt(),
            minAmplitude = minAmplitude,
            maxAmplitude = maxAmplitude,
            peakAmplitude = peakAmplitude
        )
    }

    return buckets
}

internal const val PcmWaveformPlayheadAnchorRatio = 0.40f
