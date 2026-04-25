package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class SymbolEnvelopeVisualizationAnalysisTest {
    @Test
    fun `pro dual tone builds non-zero dual-lane buckets without saturating all bars`() {
        val pcm =
            buildTonePcm(
                sampleRateHz = 44_100,
                sampleCount = 44_100,
                frequenciesHz = listOf(770.0, 1336.0),
            )

        val buckets =
            buildSymbolEnvelopeBuckets(
                pcm = pcm,
                sampleRateHz = 44_100,
                currentSample = 22_050f,
                symbolSamples = 2_205,
                targetBucketCount = 24,
                transportMode = TransportModeOption.Pro,
            )

        assertEquals(24, buckets.size)
        assertTrue(buckets.maxOf { it.upperEnergy } > 0.2f)
        assertTrue(buckets.maxOf { it.lowerEnergy } > 0.2f)
        assertTrue(buckets.any { it.upperEnergy < 0.95f })
        assertTrue(buckets.any { it.lowerEnergy < 0.95f })
    }

    @Test
    fun `ultra single tone builds symmetric envelope buckets`() {
        val pcm =
            buildTonePcm(
                sampleRateHz = 44_100,
                sampleCount = 44_100,
                frequenciesHz = listOf(2260.0),
            )

        val buckets =
            buildSymbolEnvelopeBuckets(
                pcm = pcm,
                sampleRateHz = 44_100,
                currentSample = 22_050f,
                symbolSamples = 2_205,
                targetBucketCount = 24,
                transportMode = TransportModeOption.Ultra,
            )

        assertEquals(24, buckets.size)
        assertTrue(buckets.maxOf { it.upperEnergy } > 0.2f)
        assertTrue(buckets.all { kotlin.math.abs(it.upperEnergy - it.lowerEnergy) < 0.0001f })
        assertTrue(buckets.any { it.dominantLaneIndex == 9 })
        assertTrue(buckets.any { it.dominantFrequencyHz == 2260 })
    }

    @Test
    fun `ultra visualization state exposes current and next frequencies`() {
        val sampleRateHz = 44_100
        val frameSamples = 2_205
        val pcm =
            ShortArray(frameSamples * 8) { index ->
                val frequency =
                    when (index / (frameSamples * 2)) {
                        0 -> 1000.0
                        1 -> 1560.0
                        2 -> 2260.0
                        else -> 2820.0
                    }
                toneSample(index, sampleRateHz, frequency)
            }

        val state =
            buildUltraSymbolStepVisualizationState(
                pcm = pcm,
                sampleRateHz = sampleRateHz,
                currentSample = (frameSamples * 4).toFloat(),
                symbolSamples = frameSamples,
                targetBucketCount = 12,
            )

        assertNotNull(state)
        assertNotNull(state?.currentBucket?.dominantLaneIndex)
        assertNotNull(state?.currentBucket?.dominantFrequencyHz)
        assertNotNull(state?.nextBucket)
        assertNotNull(state?.nextBucket?.dominantFrequencyHz)
    }

    @Test
    fun `ultra visualization state omits next bucket at playback tail`() {
        val sampleRateHz = 44_100
        val frameSamples = 2_205
        val pcm = buildTonePcm(sampleRateHz = sampleRateHz, sampleCount = frameSamples * 4, frequenciesHz = listOf(2820.0))

        val state =
            buildUltraSymbolStepVisualizationState(
                pcm = pcm,
                sampleRateHz = sampleRateHz,
                currentSample = pcm.size.toFloat(),
                symbolSamples = frameSamples,
                targetBucketCount = 1,
            )

        assertNotNull(state)
        assertNull(state?.nextBucket)
    }

    @Test
    fun `bucket window shifts with displayed sample progress`() {
        val sampleRateHz = 44_100
        val pcm =
            ShortArray(sampleRateHz) { index ->
                if (index < sampleRateHz / 2) {
                    toneSample(index, sampleRateHz, 697.0)
                } else {
                    toneSample(index, sampleRateHz, 941.0, 1633.0)
                }
            }

        val earlierBuckets =
            buildSymbolEnvelopeBuckets(
                pcm = pcm,
                sampleRateHz = sampleRateHz,
                currentSample = 12_000f,
                symbolSamples = 2_205,
                targetBucketCount = 24,
                transportMode = TransportModeOption.Pro,
            )
        val laterBuckets =
            buildSymbolEnvelopeBuckets(
                pcm = pcm,
                sampleRateHz = sampleRateHz,
                currentSample = 32_000f,
                symbolSamples = 2_205,
                targetBucketCount = 24,
                transportMode = TransportModeOption.Pro,
        )

        assertFalse(earlierBuckets == laterBuckets)
        assertTrue(earlierBuckets.zip(laterBuckets).any { (earlier, later) -> earlier != later })
    }

    @Test
    fun `displayed sample snaps to symbol boundary for stable updates`() {
        assertEquals(0, snapDisplayedSampleToSymbol(displayedSample = 100, symbolSamples = 2205, totalSamples = 44_100))
        assertEquals(2205, snapDisplayedSampleToSymbol(displayedSample = 2205, symbolSamples = 2205, totalSamples = 44_100))
        assertEquals(2205, snapDisplayedSampleToSymbol(displayedSample = 3300, symbolSamples = 2205, totalSamples = 44_100))
        assertEquals(44_100, snapDisplayedSampleToSymbol(displayedSample = 44_900, symbolSamples = 2205, totalSamples = 44_100))
    }

    private fun buildTonePcm(
        sampleRateHz: Int,
        sampleCount: Int,
        frequenciesHz: List<Double>,
    ): ShortArray =
        ShortArray(sampleCount) { index ->
            val mixed =
                frequenciesHz.sumOf { freq ->
                    sin(2.0 * PI * freq * index.toDouble() / sampleRateHz.toDouble())
                } / frequenciesHz.size.toDouble().coerceAtLeast(1.0)
            (mixed * Short.MAX_VALUE.toDouble() * 0.7)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

    private fun toneSample(
        index: Int,
        sampleRateHz: Int,
        vararg frequenciesHz: Double,
    ): Short {
        val mixed =
            frequenciesHz.sumOf { freq ->
                sin(2.0 * PI * freq * index.toDouble() / sampleRateHz.toDouble())
            } / frequenciesHz.size.toDouble().coerceAtLeast(1.0)
        return (mixed * Short.MAX_VALUE.toDouble() * 0.7)
            .roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }
}
