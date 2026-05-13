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
@Suppress("LargeClass")
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
        composeRule.onNodeWithText("01000010").assertIsDisplayed()
        composeRule.onAllNodesWithText("01001100", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun `flash mode shows binary before hex in annotation switcher`() {
        composeRule.setContent {
            PlaybackDataFollowSection(
                followData = sampleFollowData(),
                displayedSamples = 7,
                transportMode = TransportModeOption.Flash,
            )
        }
        composeRule.waitForIdle()

        val binaryBounds = composeRule.onNodeWithTag("follow-annotation-binary").getUnclippedBoundsInRoot()
        val hexBounds = composeRule.onNodeWithTag("follow-annotation-hex").getUnclippedBoundsInRoot()

        assertTrue(binaryBounds.left < hexBounds.left)
    }

    @Test
    fun `long preview waveform lyrics must use real playback samples instead of preview samples`() {
        val followData =
            PayloadFollowViewData(
                textTokens = listOf("ASH", "BELL", "RITE"),
                textTokenTimeline =
                    listOf(
                        TextFollowTimelineEntry(0, 5_000, 0),
                        TextFollowTimelineEntry(9_000, 5_000, 1),
                        TextFollowTimelineEntry(20_000, 5_000, 2),
                    ),
                textFollowAvailable = true,
                followAvailable = true,
            )

        assertEquals(1, followActiveTextTimelineIndex(followData, displayedSamples = 10_000))
        assertEquals(0, followActiveTextTimelineIndex(followData, displayedSamples = 409))
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
    fun `wide lyrics token card stays inside strip for long ukrainian token`() {
        composeRule.setContent {
            Box(
                modifier =
                    androidx.compose.ui.Modifier
                        .width(360.dp),
            ) {
                PlaybackDataFollowSection(
                    followData = ukrainianFollowData(),
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
    fun `annotation rows fit more byte groups when lyrics card is wide`() {
        assertEquals(8, annotationByteGroupsPerRow(PlaybackFollowViewMode.Hex, availableWidthDp = 320f))
        assertEquals(6, annotationByteGroupsPerRow(PlaybackFollowViewMode.Hex, availableWidthDp = 288f))
        assertEquals(4, annotationByteGroupsPerRow(PlaybackFollowViewMode.Hex, availableWidthDp = 220f))
        assertEquals(4, annotationByteGroupsPerRow(PlaybackFollowViewMode.Binary, availableWidthDp = 320f))
        assertEquals(3, annotationByteGroupsPerRow(PlaybackFollowViewMode.Binary, availableWidthDp = 300f))
        assertEquals(3, annotationByteGroupsPerRow(PlaybackFollowViewMode.Binary, availableWidthDp = 220f))
    }

    @Test
    fun `annotation rows cap visible row counts by mode`() {
        assertEquals(3, annotationMaxVisibleRows(PlaybackFollowViewMode.Hex))
        assertEquals(4, annotationMaxVisibleRows(PlaybackFollowViewMode.Binary))
        assertTrue(annotationMaxVisibleRows(PlaybackFollowViewMode.Morse) > 1000)
    }

    @Test
    fun `annotation window keeps previous start while active byte stays in comfort zone`() {
        assertEquals(
            4,
            resolveWindowStartIndex(
                activeIndex = 10,
                previousStartIndex = 4,
                capacity = 16,
                lastPossibleStart = 20,
            ),
        )
    }

    @Test
    fun `annotation window recenters once active byte leaves comfort zone`() {
        assertEquals(
            12,
            resolveWindowStartIndex(
                activeIndex = 18,
                previousStartIndex = 4,
                capacity = 16,
                lastPossibleStart = 20,
            ),
        )
    }

    @Test
    fun `annotation window reports overflow around active byte for long tokens`() {
        val window =
            resolveAnnotationWindow(
                annotationByteGroups = (0 until 32).map { index -> "g$index" },
                byteGroupsPerRow = 8,
                maxVisibleRows = 3,
                activeByteIndexWithinToken = 18,
                centerActiveGroup = true,
                previousStartIndex = 0,
            )

        assertEquals(8, window.startIndex)
        assertEquals(24, window.groups.size)
        assertTrue(window.hasLeadingOverflow)
        assertFalse(window.hasTrailingOverflow)
    }

    @Test
    fun `tokens preview line count stays larger for shorter token strip`() {
        assertEquals(
            4,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Flash,
                tokenStripHeightDp = null,
                prefersWrappedLines = false,
            ),
        )
        assertEquals(
            4,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Flash,
                tokenStripHeightDp = 188.2f,
                prefersWrappedLines = false,
            ),
        )
    }

    @Test
    fun `tokens preview line count yields space to taller or wrapped token strip`() {
        assertEquals(
            3,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Flash,
                tokenStripHeightDp = 203.4f,
                prefersWrappedLines = false,
            ),
        )
        assertEquals(
            3,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Flash,
                tokenStripHeightDp = 188.2f,
                prefersWrappedLines = true,
            ),
        )
        assertEquals(
            2,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Flash,
                tokenStripHeightDp = 236.2f,
                prefersWrappedLines = true,
            ),
        )
    }

    @Test
    fun `tokens preview bonus line only adds one line on top of the base policy`() {
        assertEquals(
            5,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Flash,
                tokenStripHeightDp = 188.2f,
                prefersWrappedLines = false,
                applyBonusLine = true,
            ),
        )
        assertEquals(
            4,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Flash,
                tokenStripHeightDp = 203.4f,
                prefersWrappedLines = false,
                applyBonusLine = true,
            ),
        )
        assertEquals(
            3,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Flash,
                tokenStripHeightDp = 236.2f,
                prefersWrappedLines = true,
                applyBonusLine = true,
            ),
        )
    }

    @Test
    fun `mini tokens preview adds two extra lines over the base policy`() {
        assertEquals(
            7,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Mini,
                tokenStripHeightDp = null,
                prefersWrappedLines = false,
            ),
        )
        assertEquals(
            7,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Mini,
                tokenStripHeightDp = 188.2f,
                prefersWrappedLines = false,
            ),
        )
        assertEquals(
            7,
            lyricsPreviewVisibleLineCount(
                transportMode = TransportModeOption.Mini,
                tokenStripHeightDp = 203.4f,
                prefersWrappedLines = false,
            ),
        )
    }

    @Test
    fun `mini visual lyrics recovery stays conservative at one extra line`() {
        assertEquals(
            7,
            computeCompactLyricsVisibleLineCount(
                transportMode = TransportModeOption.Mini,
                playbackDisplayMode = PlaybackDisplayMode.Visual,
                prefersWrappedLines = false,
                effectiveExtraLyricsRecoveryHeight = 120.dp,
                tokenStripHeightDp = null,
                applyLyricsPreviewBonusLine = false,
            ),
        )
    }

    @Test
    fun `annotation character boundaries split non cjk token bytes`() {
        assertEquals(
            setOf(2, 4),
            annotationCharacterBoundaryByteIndexes("під"),
        )
        assertEquals(
            setOf(1, 2),
            annotationCharacterBoundaryByteIndexes("ABC"),
        )
    }

    @Test
    fun `annotation character boundaries are hidden for cjk token`() {
        assertEquals(emptySet<Int>(), annotationCharacterBoundaryByteIndexes("漢字"))
        assertEquals(emptySet<Int>(), annotationCharacterBoundaryByteIndexes("日"))
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

    private fun ukrainianFollowData(): PayloadFollowViewData =
        PayloadFollowViewData(
            textTokens = listOf("підтверджено"),
            textTokenTimeline =
                listOf(
                    TextFollowTimelineEntry(0, 12, 0),
                ),
            textRawDisplayUnits =
                listOf(
                    TextFollowRawDisplayUnitViewData(0, 0, 1, 0, 0, 1, "D0", "11010000"),
                    TextFollowRawDisplayUnitViewData(0, 1, 1, 1, 1, 1, "BF", "10111111"),
                    TextFollowRawDisplayUnitViewData(0, 2, 1, 2, 2, 1, "D1", "11010001"),
                    TextFollowRawDisplayUnitViewData(0, 3, 1, 3, 3, 1, "96", "10010110"),
                    TextFollowRawDisplayUnitViewData(0, 4, 1, 4, 4, 1, "D0", "11010000"),
                    TextFollowRawDisplayUnitViewData(0, 5, 1, 5, 5, 1, "B4", "10110100"),
                    TextFollowRawDisplayUnitViewData(0, 6, 1, 6, 6, 1, "D1", "11010001"),
                    TextFollowRawDisplayUnitViewData(0, 7, 1, 7, 7, 1, "82", "10000010"),
                    TextFollowRawDisplayUnitViewData(0, 8, 1, 8, 8, 1, "D0", "11010000"),
                    TextFollowRawDisplayUnitViewData(0, 9, 1, 9, 9, 1, "B2", "10110010"),
                    TextFollowRawDisplayUnitViewData(0, 10, 1, 10, 10, 1, "D0", "11010000"),
                    TextFollowRawDisplayUnitViewData(0, 11, 1, 11, 11, 1, "B5", "10110101"),
                    TextFollowRawDisplayUnitViewData(0, 12, 1, 12, 12, 1, "D1", "11010001"),
                    TextFollowRawDisplayUnitViewData(0, 13, 1, 13, 13, 1, "80", "10000000"),
                    TextFollowRawDisplayUnitViewData(0, 14, 1, 14, 14, 1, "D0", "11010000"),
                    TextFollowRawDisplayUnitViewData(0, 15, 1, 15, 15, 1, "B4", "10110100"),
                    TextFollowRawDisplayUnitViewData(0, 16, 1, 16, 16, 1, "D0", "11010000"),
                    TextFollowRawDisplayUnitViewData(0, 17, 1, 17, 17, 1, "B6", "10110110"),
                    TextFollowRawDisplayUnitViewData(0, 18, 1, 18, 18, 1, "D0", "11010000"),
                    TextFollowRawDisplayUnitViewData(0, 19, 1, 19, 19, 1, "B5", "10110101"),
                    TextFollowRawDisplayUnitViewData(0, 20, 1, 20, 20, 1, "D0", "11010000"),
                    TextFollowRawDisplayUnitViewData(0, 21, 1, 21, 21, 1, "BD", "10111101"),
                    TextFollowRawDisplayUnitViewData(0, 22, 1, 22, 22, 1, "D0", "11010000"),
                    TextFollowRawDisplayUnitViewData(0, 23, 1, 23, 23, 1, "BE", "10111110"),
                ),
            textFollowAvailable = true,
            followAvailable = true,
        )
}
