package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
                transportMode = TransportModeOption.Flash,
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
                transportMode = TransportModeOption.Flash,
            )
        }

        composeRule.onNodeWithTag("follow-token-strip").assertIsDisplayed()
        composeRule.onNodeWithText("BELL").assertIsDisplayed()
        composeRule.onNodeWithText("42").assertIsDisplayed()
        composeRule.onAllNodesWithText("4C").assertCountEquals(1)
    }

    @Test
    fun `binary mode shows current token binary annotation`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData = sampleFollowData(),
                displayedSamples = 7,
                transportMode = TransportModeOption.Flash,
                initialAnnotationMode = PlaybackFollowViewMode.Binary,
            )
        }

        composeRule.onNodeWithText("01000010").assertIsDisplayed()
        composeRule.onAllNodesWithText("01001100", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun `flash mode keeps hex and binary annotation choices`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData = sampleFollowData(),
                displayedSamples = 7,
                transportMode = TransportModeOption.Flash,
            )
        }

        composeRule.onNodeWithText(string(R.string.audio_follow_view_hex)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.audio_follow_view_binary)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.audio_follow_view_morse)).assertCountEquals(0)
    }

    @Test
    fun `mini mode only shows morse annotation choice`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData = sampleFollowData(),
                displayedSamples = 7,
                transportMode = TransportModeOption.Mini,
                initialAnnotationMode = PlaybackFollowViewMode.Binary,
            )
        }

        composeRule.onNodeWithText(string(R.string.audio_follow_view_morse)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.audio_follow_view_hex)).assertCountEquals(0)
        composeRule.onAllNodesWithText(string(R.string.audio_follow_view_binary)).assertCountEquals(0)
        composeRule.onNodeWithText("-...").assertIsDisplayed()
    }

    @Test
    fun `first active token starts centered inside lyrics strip`() {
        composeRule.setContent {
            Box(
                modifier =
                    androidx.compose.ui.Modifier
                        .width(360.dp),
            ) {
                PlaybackDataFollowSection(
                    followData = sampleFollowData(),
                    displayedSamples = 0,
                    transportMode = TransportModeOption.Flash,
                )
            }
        }
        composeRule.waitForIdle()

        val stripBounds = composeRule.onNodeWithTag("follow-token-strip").getUnclippedBoundsInRoot()
        val activeBounds =
            composeRule
                .onNodeWithTag("follow-token-active", useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
        val stripCenterX = (stripBounds.left.value + stripBounds.right.value) / 2f
        val activeCenterX = (activeBounds.left.value + activeBounds.right.value) / 2f

        assertEquals(stripCenterX, activeCenterX, 2f)
        assertTrue(activeBounds.left >= stripBounds.left)
        assertTrue(activeBounds.right <= stripBounds.right)
    }

    @Test
    fun `main token strip card is preview only and does not seek`() {
        var seekTarget: Int? = null
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData = sampleFollowData(),
                displayedSamples = 7,
                transportMode = TransportModeOption.Flash,
                onSeekToSample = { seekTarget = it },
            )
        }

        composeRule.onNodeWithTag("follow-token-active", useUnmergedTree = true).assertHasNoClickAction()

        assertEquals(null, seekTarget)
    }

    @Test
    fun `tokenizer opens token list without stealing follow strip state`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData = sampleFollowData(),
                displayedSamples = 7,
                transportMode = TransportModeOption.Flash,
            )
        }

        composeRule
            .onNodeWithContentDescription(string(R.string.audio_action_open_tokenizer))
            .performClick()

        composeRule.onNodeWithTag("follow-tokenizer-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("follow-tokenizer-card-0", useUnmergedTree = true).assertIsDisplayed()
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
                transportMode = TransportModeOption.Flash,
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
                transportMode = TransportModeOption.Flash,
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

    @Test
    fun `morse mode chooses dot dash annotation`() {
        val annotation =
            annotationByteGroupsForMode(
                PlaybackFollowViewMode.Morse,
                listOf(
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 0,
                        sampleCount = 4,
                        byteIndexWithinToken = 0,
                        byteOffset = 0,
                        byteCount = 1,
                        hexText = "50",
                        binaryText = "01010000",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 4,
                        sampleCount = 4,
                        byteIndexWithinToken = 1,
                        byteOffset = 1,
                        byteCount = 1,
                        hexText = "52",
                        binaryText = "01010010",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 8,
                        sampleCount = 4,
                        byteIndexWithinToken = 2,
                        byteOffset = 2,
                        byteCount = 1,
                        hexText = "41",
                        binaryText = "01000001",
                    ),
                ),
            )

        assertEquals(listOf(".--.", ".-.", ".-"), annotation)
    }

    @Test
    fun `morse display groups keep letters paired with morse patterns`() {
        val groups =
            morseLetterDisplayGroups(
                token = "ASH",
                rawDisplayUnits =
                    listOf(
                        TextFollowRawDisplayUnitViewData(0, 0, 1, 0, 0, 1, "41", "01000001"),
                        TextFollowRawDisplayUnitViewData(0, 1, 1, 1, 1, 1, "53", "01010011"),
                        TextFollowRawDisplayUnitViewData(0, 2, 2, 2, 2, 1, "48", "01001000"),
                    ),
            )

        assertEquals(listOf("A", "S", "H"), groups.map(MorseLetterDisplayGroup::text))
        assertEquals(listOf(".-", "...", "...."), groups.map(MorseLetterDisplayGroup::morse))
    }

    @Test
    fun `active bit index uses follow timeline bit offset from libs`() {
        val rawDisplayUnit = TextFollowRawDisplayUnitViewData(0, 0, 80, 0, 0, 1, "41", "01000001")
        val activeBitIndex =
            activeBitIndexWithinByte(
                activeTextIndex = 0,
                activeByteIndexWithinToken = 0,
                displayedSamples = 15,
                followData =
                    PayloadFollowViewData(
                        binaryGroupTimeline =
                            listOf(
                                PayloadFollowBinaryGroupTimelineEntry(
                                    startSample = 10,
                                    sampleCount = 10,
                                    groupIndex = 99,
                                    bitOffset = 3,
                                    bitCount = 1,
                                ),
                            ),
                    ),
                rawDisplayUnitsByToken = mapOf(0 to listOf(rawDisplayUnit)),
            )

        assertEquals(3, activeBitIndex)
    }

    @Test
    fun `active bit position keeps completed bit during silence gap`() {
        val rawDisplayUnit = TextFollowRawDisplayUnitViewData(0, 0, 80, 0, 0, 1, "41", "01000001")
        val position =
            activeBitPositionWithinByte(
                activeTextIndex = 0,
                activeByteIndexWithinToken = 0,
                displayedSamples = 25,
                followData =
                    PayloadFollowViewData(
                        binaryGroupTimeline =
                            listOf(
                                PayloadFollowBinaryGroupTimelineEntry(
                                    startSample = 10,
                                    sampleCount = 10,
                                    groupIndex = 0,
                                    bitOffset = 3,
                                    bitCount = 1,
                                ),
                                PayloadFollowBinaryGroupTimelineEntry(
                                    startSample = 40,
                                    sampleCount = 10,
                                    groupIndex = 1,
                                    bitOffset = 4,
                                    bitCount = 1,
                                ),
                            ),
                    ),
                rawDisplayUnitsByToken = mapOf(0 to listOf(rawDisplayUnit)),
            )

        assertEquals(3, position.bitIndexWithinByte)
        assertFalse(position.isToneActive)
    }

    @Test
    fun `active bit position reports tone active during current bit`() {
        val rawDisplayUnit = TextFollowRawDisplayUnitViewData(0, 0, 80, 0, 0, 1, "41", "01000001")
        val position =
            activeBitPositionWithinByte(
                activeTextIndex = 0,
                activeByteIndexWithinToken = 0,
                displayedSamples = 45,
                followData =
                    PayloadFollowViewData(
                        binaryGroupTimeline =
                            listOf(
                                PayloadFollowBinaryGroupTimelineEntry(
                                    startSample = 10,
                                    sampleCount = 10,
                                    groupIndex = 0,
                                    bitOffset = 3,
                                    bitCount = 1,
                                ),
                                PayloadFollowBinaryGroupTimelineEntry(
                                    startSample = 40,
                                    sampleCount = 10,
                                    groupIndex = 1,
                                    bitOffset = 4,
                                    bitCount = 1,
                                ),
                            ),
                    ),
                rawDisplayUnitsByToken = mapOf(0 to listOf(rawDisplayUnit)),
            )

        assertEquals(4, position.bitIndexWithinByte)
        assertTrue(position.isToneActive)
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
