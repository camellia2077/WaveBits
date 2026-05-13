package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackDisplaySectionExpandedLyricsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `expanded lyrics switches from compact tape to full scrollable list`() {
        composeRule.setContent {
            var lyricsExpanded by remember { mutableStateOf(false) }
            PlaybackDisplaySection(
                displayedSamples = 7,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                transportMode = TransportModeOption.Mini,
                frameSamples = 2205,
                isFlashMode = false,
                flashVoicingStyle = null,
                followData = multiLineFollowData(),
                isPlaying = false,
                playbackSpeed = 1.0f,
                isScrubbing = false,
                playbackDisplayMode = PlaybackDisplayMode.Lyrics,
                flashVisualizationModeName = FlashSignalVisualizationMode.Lanes.name,
                lyricsExpanded = lyricsExpanded,
                onDisplayModeSelected = {},
                onFlashVisualizationModeSelected = {},
                onLyricsExpandedChanged = { lyricsExpanded = it },
            )
        }

        composeRule.onNodeWithTag("playback-token-context-tape-list", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("playback-lyrics-expand-toggle", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("playback-lyrics-expand-toggle", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("playback-lyrics-full-list", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("playback-lyrics-full-line-active", useUnmergedTree = true).assertExists()
        composeRule.onAllNodesWithTag("playback-token-context-tape-list", useUnmergedTree = true).assertCountEquals(0)
    }

    private fun multiLineFollowData(): PayloadFollowViewData =
        PayloadFollowViewData(
            textTokens = listOf("ASH", "BELL", "RITE", "LAMP", "TIDE", "HUSH"),
            textTokenTimeline =
                listOf(
                    TextFollowTimelineEntry(0, 4, 0),
                    TextFollowTimelineEntry(4, 4, 1),
                    TextFollowTimelineEntry(8, 4, 2),
                    TextFollowTimelineEntry(12, 4, 3),
                    TextFollowTimelineEntry(16, 4, 4),
                    TextFollowTimelineEntry(20, 4, 5),
                ),
            textRawDisplayUnits =
                listOf(
                    TextFollowRawDisplayUnitViewData(0, 0, 1, 0, 0, 1, "41", "01000001"),
                    TextFollowRawDisplayUnitViewData(1, 1, 1, 0, 1, 1, "42", "01000010"),
                    TextFollowRawDisplayUnitViewData(2, 2, 1, 0, 2, 1, "52", "01010010"),
                    TextFollowRawDisplayUnitViewData(3, 3, 1, 0, 3, 1, "4C", "01001100"),
                    TextFollowRawDisplayUnitViewData(4, 4, 1, 0, 4, 1, "54", "01010100"),
                    TextFollowRawDisplayUnitViewData(5, 5, 1, 0, 5, 1, "48", "01001000"),
                ),
            lyricLines = listOf("ASH BELL", "RITE LAMP", "TIDE HUSH"),
            lineTokenRanges =
                listOf(
                    TextFollowLineTokenRangeViewData(0, 0, 2),
                    TextFollowLineTokenRangeViewData(1, 2, 2),
                    TextFollowLineTokenRangeViewData(2, 4, 2),
                ),
            textFollowAvailable = true,
            lyricLineFollowAvailable = true,
            followAvailable = true,
        )
}
