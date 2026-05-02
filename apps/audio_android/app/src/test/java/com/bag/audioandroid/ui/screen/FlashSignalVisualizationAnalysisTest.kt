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
    fun `follow timeline segments preserve fixed bit geometry`() {
        val followData =
            PayloadFollowViewData(
                binaryTokens = listOf("0", "1", "0", "1"),
                binaryGroupTimeline =
                    listOf(
                        PayloadFollowBinaryGroupTimelineEntry(100, 20, 0, 0, 1),
                        PayloadFollowBinaryGroupTimelineEntry(120, 20, 1, 1, 1),
                        PayloadFollowBinaryGroupTimelineEntry(140, 20, 2, 2, 1),
                        PayloadFollowBinaryGroupTimelineEntry(160, 20, 3, 3, 1),
                    ),
                totalPcmSampleCount = 240,
                followAvailable = true,
            )

        val segments = buildFlashSignalToneSegments(followData)

        assertEquals(4, segments.size)
        assertEquals(100, segments[0].startSample)
        assertEquals(120, segments[0].endSample)
        assertEquals(FskDominantTone.Low, segments[0].tone)
        assertEquals(120, segments[1].startSample)
        assertEquals(140, segments[1].endSample)
        assertEquals(FskDominantTone.High, segments[1].tone)
        assertEquals(160, segments[3].startSample)
        assertEquals(180, segments[3].endSample)
        assertEquals(FskDominantTone.High, segments[3].tone)
    }

    @Test
    fun `flash bit readout reveals current eight bit group by playback`() {
        val segments =
            listOf(
                FskDominantTone.Low,
                FskDominantTone.High,
                FskDominantTone.High,
                FskDominantTone.Low,
                FskDominantTone.High,
                FskDominantTone.Low,
                FskDominantTone.Low,
                FskDominantTone.High,
            ).mapIndexed { index, tone ->
                FlashSignalToneSegment(
                    startSample = index * 10,
                    endSample = index * 10 + 10,
                    tone = tone,
                )
            }

        val frame = flashBitReadoutFrame(segments = segments, sample = 35f)

        requireNotNull(frame)
        assertEquals(0, frame.currentGroupStartIndex)
        assertEquals(List(8) { null }, frame.previousCells.map { it.bit })
        assertEquals(listOf('0', '1', '1', '0', null, null, null, null), frame.currentCells.map { it.bit })
        assertEquals(listOf(false, false, false, true, false, false, false, false), frame.currentCells.map { it.isCurrent })
    }

    @Test
    fun `flash bit readout shows previous and current groups after eight revealed bits`() {
        val segments =
            List(10) { index ->
                FlashSignalToneSegment(
                    startSample = index * 10,
                    endSample = index * 10 + 10,
                    tone = if (index % 2 == 0) FskDominantTone.Low else FskDominantTone.High,
                )
            }

        val frame = flashBitReadoutFrame(segments = segments, sample = 85f)

        requireNotNull(frame)
        assertEquals(8, frame.currentGroupStartIndex)
        assertEquals(listOf('0', '1', '0', '1', '0', '1', '0', '1'), frame.previousCells.map { it.bit })
        assertEquals(listOf('0', null, null, null, null, null, null, null), frame.currentCells.map { it.bit })
        assertEquals(listOf(true, false, false, false, false, false, false, false), frame.currentCells.map { it.isCurrent })
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
