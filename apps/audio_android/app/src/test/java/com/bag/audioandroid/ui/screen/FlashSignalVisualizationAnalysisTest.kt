package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashSignalVisualizationAnalysisTest {
    @Test
    fun `flash route uses follow timeline buckets for full pcm when available`() {
        val route =
            resolvePlaybackVisualizationRoute(
                transportMode = TransportModeOption.Flash,
                isFlashMode = true,
                waveformPcm = shortArrayOf(1, 2, 3, 4),
                isWaveformPreview = false,
                sampleRateHz = 44100,
                visualDisplayedSamples = 120,
                displayedSamples = 120,
                followData =
                    PayloadFollowViewData(
                        binaryTokens = listOf("0", "1"),
                        binaryGroupTimeline =
                            listOf(
                                PayloadFollowBinaryGroupTimelineEntry(0, 100, 0, 0, 1),
                                PayloadFollowBinaryGroupTimelineEntry(100, 100, 1, 1, 1),
                            ),
                        totalPcmSampleCount = 200,
                        followAvailable = true,
                    ),
            )

        assertTrue(route is PlaybackVisualizationRoute.FlashSignal)
        val input = (route as PlaybackVisualizationRoute.FlashSignal).input
        assertTrue(input.bucketSource is FlashSignalBucketSource.FollowTimeline)
    }

    @Test
    fun `flash route falls back to pcm buckets when follow bits are incomplete`() {
        val route =
            resolvePlaybackVisualizationRoute(
                transportMode = TransportModeOption.Flash,
                isFlashMode = true,
                waveformPcm = shortArrayOf(1, 2, 3, 4),
                isWaveformPreview = false,
                sampleRateHz = 44100,
                visualDisplayedSamples = 120,
                displayedSamples = 120,
                followData =
                    PayloadFollowViewData(
                        binaryTokens = listOf("0"),
                        binaryGroupTimeline =
                            listOf(
                                PayloadFollowBinaryGroupTimelineEntry(0, 100, 0, 0, 1),
                                PayloadFollowBinaryGroupTimelineEntry(100, 100, 1, 1, 1),
                            ),
                        totalPcmSampleCount = 200,
                        followAvailable = true,
                    ),
            )

        assertTrue(route is PlaybackVisualizationRoute.FlashSignal)
        val input = (route as PlaybackVisualizationRoute.FlashSignal).input
        assertTrue(input.bucketSource is FlashSignalBucketSource.Pcm)
    }

    @Test
    fun `follow timeline buckets move with real playback samples`() {
        val followData =
            PayloadFollowViewData(
                binaryTokens = listOf("0", "1", "0", "1"),
                binaryGroupTimeline =
                    listOf(
                        PayloadFollowBinaryGroupTimelineEntry(0, 100, 0, 0, 1),
                        PayloadFollowBinaryGroupTimelineEntry(100, 100, 1, 1, 1),
                        PayloadFollowBinaryGroupTimelineEntry(200, 100, 2, 2, 1),
                        PayloadFollowBinaryGroupTimelineEntry(300, 100, 3, 3, 1),
                    ),
                totalPcmSampleCount = 400,
                followAvailable = true,
            )

        val earlyBuckets =
            buildFskEnergyBucketsFromFollowData(
                followData = followData,
                currentSample = 100f,
                windowSampleCount = 200,
                targetBucketCount = 4,
            )
        val laterBuckets =
            buildFskEnergyBucketsFromFollowData(
                followData = followData,
                currentSample = 300f,
                windowSampleCount = 200,
                targetBucketCount = 4,
            )

        assertEquals(FskDominantTone.Low, earlyBuckets[0].dominantTone)
        assertEquals(FskDominantTone.High, earlyBuckets[2].dominantTone)
        assertEquals(FskDominantTone.Low, laterBuckets[0].dominantTone)
        assertEquals(FskDominantTone.High, laterBuckets[2].dominantTone)
        assertTrue(laterBuckets.any { it.dominantTone == FskDominantTone.High })
        assertTrue(laterBuckets.any { it.dominantTone == FskDominantTone.Low })
    }

    @Test
    fun `follow timeline buckets keep a visible dip between repeated same tone symbols`() {
        val followData =
            PayloadFollowViewData(
                binaryTokens = listOf("0", "0", "0"),
                binaryGroupTimeline =
                    listOf(
                        PayloadFollowBinaryGroupTimelineEntry(0, 100, 0, 0, 1),
                        PayloadFollowBinaryGroupTimelineEntry(100, 100, 1, 1, 1),
                        PayloadFollowBinaryGroupTimelineEntry(200, 100, 2, 2, 1),
                    ),
                totalPcmSampleCount = 300,
                followAvailable = true,
            )

        val buckets =
            buildFskEnergyBucketsFromFollowData(
                followData = followData,
                currentSample = 180f,
                windowSampleCount = 300,
                targetBucketCount = 30,
            )

        val boundaryBucket = buckets[4]
        val interiorBucket = buckets[8]
        assertEquals(FskDominantTone.Low, interiorBucket.dominantTone)
        assertTrue(boundaryBucket.lowStrength < interiorBucket.lowStrength * 0.5f)
    }

    @Test
    fun `flash visualization analysis samples quantize to twenty four fps`() {
        assertEquals(1837, visualizationAnalysisSampleStep(sampleRateHz = 44_100, totalSamples = 44_100))
        assertEquals(1, visualizationAnalysisSampleStep(sampleRateHz = 10, totalSamples = 44_100))
        assertEquals(120, visualizationAnalysisSampleStep(sampleRateHz = 44_100, totalSamples = 120))
        assertEquals(
            1837f,
            quantizeVisualizationDisplayedSamples(
                displayedSamples = 1900f,
                sampleStep = 1837,
                totalSamples = 44_100,
            ),
        )
    }
}
