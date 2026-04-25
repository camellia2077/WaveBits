package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineRawSegmentViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerDetailSheetInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lyricsFollowSectionRendersInInstrumentedEnvironment() {
        composeRule.setContent {
            AudioPlaybackProgressSection(
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                transportMode = TransportModeOption.Flash,
                isFlashMode = true,
                flashVoicingStyle = null,
                followData = sampleFollowData(),
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithTag("playback-progress-section").assertIsDisplayed()
        composeRule.onNodeWithTag("playback-display-lyrics").performClick()
        composeRule.onNodeWithTag("follow-lyrics-list").assertIsDisplayed()
    }

    private fun sampleFollowData(): PayloadFollowViewData =
        PayloadFollowViewData(
            textTokens = listOf("ASH", "BELL", "RITE"),
            textTokenTimeline =
                listOf(
                    TextFollowTimelineEntry(0, 4, 0),
                    TextFollowTimelineEntry(4, 4, 1),
                    TextFollowTimelineEntry(8, 4, 2),
                ),
            textFollowAvailable = true,
            lyricLines = listOf("ASH", "BELL", "RITE"),
            lyricLineTimeline =
                listOf(
                    TextFollowLyricLineTimelineEntry(0, 4, 0),
                    TextFollowLyricLineTimelineEntry(4, 4, 1),
                    TextFollowLyricLineTimelineEntry(8, 4, 2),
                ),
            lineTokenRanges =
                listOf(
                    TextFollowLineTokenRangeViewData(0, 0, 1),
                    TextFollowLineTokenRangeViewData(1, 1, 1),
                    TextFollowLineTokenRangeViewData(2, 2, 1),
                ),
            lineRawSegments =
                listOf(
                    TextFollowLineRawSegmentViewData(
                        lineIndex = 0,
                        startSample = 0,
                        sampleCount = 4,
                        byteOffset = 0,
                        byteCount = 3,
                        hexText = "41 53 48",
                        binaryText = "01000001 01010011 01001000",
                    ),
                    TextFollowLineRawSegmentViewData(
                        lineIndex = 1,
                        startSample = 4,
                        sampleCount = 4,
                        byteOffset = 3,
                        byteCount = 4,
                        hexText = "42 45 4C 4C",
                        binaryText = "01000010 01000101 01001100 01001100",
                    ),
                    TextFollowLineRawSegmentViewData(
                        lineIndex = 2,
                        startSample = 8,
                        sampleCount = 4,
                        byteOffset = 7,
                        byteCount = 4,
                        hexText = "52 49 54 45",
                        binaryText = "01010010 01001001 01010100 01000101",
                    ),
                ),
            lyricLineFollowAvailable = true,
            followAvailable = true,
        )
}
