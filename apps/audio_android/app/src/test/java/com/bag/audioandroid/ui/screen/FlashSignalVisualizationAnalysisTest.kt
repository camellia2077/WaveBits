package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashSignalVisualizationAnalysisTest {
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
}
