package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioPlaybackProgressSectionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    @Test
    fun `visual mode keeps follow section hidden`() {
        composeRule.setContent {
            AudioPlaybackProgressSection(
                displayedSamples = 0,
                totalSamples = 8,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4),
                sampleRateHz = 44100,
                transportMode = TransportModeOption.Pro,
                isFlashMode = false,
                flashVoicingStyle = null,
                followData = sampleFollowData(),
                displayedTime = "0:00",
                totalTime = "0:01",
                isPlaying = false,
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
                initialDisplayMode = PlaybackDisplayMode.Visual,
            )
        }

        composeRule.onNodeWithText(string(R.string.audio_playback_view_visual)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.audio_playback_view_lyrics)).assertIsDisplayed()
        composeRule.onNodeWithTag("playback-display-section").assertIsDisplayed()
        composeRule.onAllNodesWithTag("playback-token-context-tape-list").assertCountEquals(1)
        composeRule.onAllNodesWithTag("playback-follow-section").assertCountEquals(0)
    }

    @Test
    fun `defaults to visual playback mode`() {
        composeRule.setContent {
            AudioPlaybackProgressSection(
                displayedSamples = 0,
                totalSamples = 8,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4),
                sampleRateHz = 44100,
                transportMode = TransportModeOption.Pro,
                isFlashMode = false,
                flashVoicingStyle = null,
                followData = sampleFollowData(),
                displayedTime = "0:00",
                totalTime = "0:01",
                isPlaying = false,
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithTag("playback-display-visual").assertIsDisplayed()
        composeRule.onNodeWithTag("playback-display-section").assertIsDisplayed()
        composeRule.onAllNodesWithTag("playback-token-context-tape-list").assertCountEquals(1)
        composeRule.onAllNodesWithTag("playback-follow-section").assertCountEquals(0)
    }

    @Test
    fun `lyrics mode shows follow section`() {
        composeRule.setContent {
            AudioPlaybackProgressSection(
                displayedSamples = 0,
                totalSamples = 8,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4),
                sampleRateHz = 44100,
                transportMode = TransportModeOption.Pro,
                isFlashMode = false,
                flashVoicingStyle = null,
                followData = sampleFollowData(),
                displayedTime = "0:00",
                totalTime = "0:01",
                isPlaying = false,
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
                initialDisplayMode = PlaybackDisplayMode.Lyrics,
            )
        }

        composeRule.onNodeWithTag("playback-display-lyrics").performClick()
        composeRule.onAllNodesWithTag("playback-token-context-tape-list").assertCountEquals(1)
        composeRule.onAllNodesWithTag("playback-follow-section").assertCountEquals(1)
    }

    @Test
    fun `lyrics mode stays stable inside outer vertical scroll container`() {
        composeRule.setContent {
            Column(modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) {
                AudioPlaybackProgressSection(
                    displayedSamples = 0,
                    totalSamples = 8,
                    isScrubbing = false,
                    waveformPcm = shortArrayOf(1, 2, 3, 4),
                    sampleRateHz = 44100,
                    transportMode = TransportModeOption.Pro,
                    isFlashMode = false,
                    flashVoicingStyle = null,
                    followData = sampleFollowData(),
                    displayedTime = "0:00",
                    totalTime = "0:01",
                    isPlaying = false,
                    onScrubStarted = {},
                    onScrubChanged = {},
                    onScrubFinished = {},
                    initialDisplayMode = PlaybackDisplayMode.Lyrics,
                )
            }
        }

        composeRule.onNodeWithTag("playback-display-lyrics").performClick()
        composeRule.onNodeWithTag("playback-progress-section").assertIsDisplayed()
        composeRule.onAllNodesWithTag("playback-token-context-tape-list").assertCountEquals(1)
        composeRule.onAllNodesWithTag("playback-follow-section").assertCountEquals(1)
    }

    @Test
    fun `token context tape hides when text follow is unavailable`() {
        composeRule.setContent {
            AudioPlaybackProgressSection(
                displayedSamples = 0,
                totalSamples = 8,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4),
                sampleRateHz = 44100,
                transportMode = TransportModeOption.Pro,
                isFlashMode = false,
                flashVoicingStyle = null,
                followData = PayloadFollowViewData(followAvailable = true, textFollowAvailable = false),
                displayedTime = "0:00",
                totalTime = "0:01",
                isPlaying = false,
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
                initialDisplayMode = PlaybackDisplayMode.Visual,
            )
        }

        composeRule.onAllNodesWithTag("playback-token-context-tape-list").assertCountEquals(0)
    }

    @Test
    fun `flash visual keeps visualization switcher`() {
        composeRule.setContent {
            AudioPlaybackProgressSection(
                displayedSamples = 0,
                totalSamples = 8,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4),
                sampleRateHz = 44100,
                transportMode = TransportModeOption.Flash,
                isFlashMode = true,
                flashVoicingStyle = null,
                followData = sampleFollowData(),
                displayedTime = "0:00",
                totalTime = "0:01",
                isPlaying = false,
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithTag("flash-visualization-mode-switcher").assertIsDisplayed()
    }

    @Test
    fun `pro visual uses symbol envelope without flash switcher`() {
        composeRule.setContent {
            AudioPlaybackProgressSection(
                displayedSamples = 0,
                totalSamples = 8,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4),
                sampleRateHz = 44100,
                transportMode = TransportModeOption.Pro,
                isFlashMode = false,
                flashVoicingStyle = null,
                followData = sampleFollowData(),
                displayedTime = "0:00",
                totalTime = "0:01",
                isPlaying = false,
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithTag("pro-encoding-visualizer").assertIsDisplayed()
        composeRule.onAllNodesWithTag("flash-visualization-mode-switcher").assertCountEquals(0)
    }

    @Test
    fun `ultra visual uses symbol envelope without flash switcher`() {
        composeRule.setContent {
            AudioPlaybackProgressSection(
                displayedSamples = 0,
                totalSamples = 8,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4),
                sampleRateHz = 44100,
                transportMode = TransportModeOption.Ultra,
                isFlashMode = false,
                flashVoicingStyle = null,
                followData = sampleFollowData(),
                displayedTime = "0:00",
                totalTime = "0:01",
                isPlaying = false,
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithTag("ultra-symbol-step-visualizer").assertIsDisplayed()
        composeRule.onNodeWithTag("ultra-now-next-row").assertIsDisplayed()
        composeRule.onAllNodesWithTag("flash-visualization-mode-switcher").assertCountEquals(0)
    }

    private fun sampleFollowData(): PayloadFollowViewData =
        PayloadFollowViewData(
            textTokens = listOf("ASH", "BELL", "RITE", "OF", "THE", "WEST", "GATE", "WAITS", "DAWN"),
            textTokenTimeline =
                listOf(
                    TextFollowTimelineEntry(0, 4, 0),
                    TextFollowTimelineEntry(4, 4, 1),
                    TextFollowTimelineEntry(8, 4, 2),
                    TextFollowTimelineEntry(12, 4, 3),
                    TextFollowTimelineEntry(16, 4, 4),
                    TextFollowTimelineEntry(20, 4, 5),
                    TextFollowTimelineEntry(24, 4, 6),
                    TextFollowTimelineEntry(28, 4, 7),
                    TextFollowTimelineEntry(32, 4, 8),
                ),
            textRawDisplayUnits =
                listOf(
                    TextFollowRawDisplayUnitViewData(0, 0, 4, 0, 0, 1, "41", "01000001"),
                ),
            byteTimeline = listOf(PayloadFollowByteTimelineEntry(0, 8, 0)),
            textFollowAvailable = true,
            lyricLines = listOf("ASH BELL RITE"),
            lyricLineTimeline = listOf(TextFollowLyricLineTimelineEntry(0, 12, 0)),
            lineTokenRanges = listOf(TextFollowLineTokenRangeViewData(0, 0, 3)),
            lyricLineFollowAvailable = true,
            followAvailable = true,
        )
}
