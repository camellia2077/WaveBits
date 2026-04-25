package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackDataFollowSectionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    @Test
    fun `shows unavailable message when follow data is missing`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData = PayloadFollowViewData.Empty,
                displayedSamples = 0,
            )
        }

        composeRule.onNodeWithText(string(R.string.audio_follow_unavailable)).assertIsDisplayed()
    }

    @Test
    fun `shows token strip and current token annotation`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData = sampleFollowData(),
                displayedSamples = 7,
            )
        }

        composeRule.onNodeWithTag("follow-token-strip").assertIsDisplayed()
        composeRule.onNodeWithText("BELL").assertIsDisplayed()
        composeRule.onNodeWithText("42 45 4C 4C").assertIsDisplayed()
    }

    @Test
    fun `binary mode shows current token binary annotation`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData = sampleFollowData(),
                displayedSamples = 7,
                initialAnnotationMode = PlaybackFollowViewMode.Binary,
            )
        }

        composeRule.onNodeWithText("01000010 01000101 01001100 01001100").assertIsDisplayed()
    }

    @Test
    fun `shows text follow empty state when token mapped view is unavailable`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData =
                    PayloadFollowViewData(
                        followAvailable = true,
                        textFollowAvailable = true,
                    ),
                displayedSamples = 0,
            )
        }

        composeRule.onNodeWithText(string(R.string.audio_follow_text_unavailable)).assertIsDisplayed()
    }

    @Test
    fun `lyrics mode keeps player style empty state when text follow is unavailable`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData =
                    PayloadFollowViewData(
                        followAvailable = true,
                        textFollowAvailable = false,
                    ),
                displayedSamples = 0,
            )
        }

        composeRule.onNodeWithText(string(R.string.audio_follow_text_unavailable)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.audio_follow_view_hex)).assertCountEquals(0)
    }

    @Test
    fun `binary mode chooses byte grouped binary annotation`() {
        val annotation =
            annotationByteGroupsForMode(
                PlaybackFollowViewMode.Binary,
                listOf(
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 0,
                        sampleCount = 4,
                        byteIndexWithinToken = 0,
                        byteOffset = 3,
                        byteCount = 1,
                        hexText = "E6",
                        binaryText = "11100110",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 4,
                        sampleCount = 4,
                        byteIndexWithinToken = 1,
                        byteOffset = 4,
                        byteCount = 1,
                        hexText = "9C",
                        binaryText = "10011100",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 8,
                        sampleCount = 4,
                        byteIndexWithinToken = 2,
                        byteOffset = 5,
                        byteCount = 1,
                        hexText = "BA",
                        binaryText = "10111010",
                    ),
                ),
            )

        assertEquals(listOf("11100110", "10011100", "10111010"), annotation)
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
            textRawDisplayUnits =
                listOf(
                    TextFollowRawDisplayUnitViewData(0, 0, 1, 0, 0, 1, "41", "01000001"),
                    TextFollowRawDisplayUnitViewData(0, 1, 1, 1, 1, 1, "53", "01010011"),
                    TextFollowRawDisplayUnitViewData(0, 2, 2, 2, 2, 1, "48", "01001000"),
                    TextFollowRawDisplayUnitViewData(1, 4, 1, 0, 3, 1, "42", "01000010"),
                    TextFollowRawDisplayUnitViewData(1, 5, 1, 1, 4, 1, "45", "01000101"),
                    TextFollowRawDisplayUnitViewData(1, 6, 1, 2, 5, 1, "4C", "01001100"),
                    TextFollowRawDisplayUnitViewData(1, 7, 1, 3, 6, 1, "4C", "01001100"),
                    TextFollowRawDisplayUnitViewData(2, 8, 1, 0, 7, 1, "52", "01010010"),
                    TextFollowRawDisplayUnitViewData(2, 9, 1, 1, 8, 1, "49", "01001001"),
                    TextFollowRawDisplayUnitViewData(2, 10, 1, 2, 9, 1, "54", "01010100"),
                    TextFollowRawDisplayUnitViewData(2, 11, 1, 3, 10, 1, "45", "01000101"),
                ),
            textFollowAvailable = true,
            followAvailable = true,
        )
}
