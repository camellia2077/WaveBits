package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.theme.appThemeAccentTokens
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DecodedPayloadContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    @Test
    fun `defaults to text view`() {
        composeRule.setContent {
            DecodedPayloadContent(
                accentTokens = appThemeAccentTokens(),
                decodedPayload =
                    DecodedPayloadViewData(
                        text = "decoded text",
                        rawBytesHex = "41 42",
                        rawBitsBinary = "01000001 01000010",
                        textDecodeStatusCode = BagDecodeContentCodes.STATUS_OK,
                        rawPayloadAvailable = true,
                    ),
                emptyTextResId = com.bag.audioandroid.R.string.audio_result_empty,
            )
        }

        composeRule.onNodeWithText("decoded text").assertIsDisplayed()
    }

    @Test
    fun `raw tab shows binary and hex`() {
        composeRule.setContent {
            DecodedPayloadContent(
                accentTokens = appThemeAccentTokens(),
                decodedPayload =
                    DecodedPayloadViewData(
                        text = "decoded text",
                        rawBytesHex = "41 42",
                        rawBitsBinary = "01000001 01000010",
                        textDecodeStatusCode = BagDecodeContentCodes.STATUS_OK,
                        rawPayloadAvailable = true,
                    ),
                emptyTextResId = com.bag.audioandroid.R.string.audio_result_empty,
                startInRawView = true,
            )
        }

        composeRule.onNodeWithText(string(R.string.audio_decode_raw_binary_label)).assertIsDisplayed()
        composeRule.onNodeWithText("01000001 01000010").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.audio_decode_raw_hex_label)).assertIsDisplayed()
        composeRule.onNodeWithText("41 42").assertIsDisplayed()
    }

    @Test
    fun `text failure still allows raw tab`() {
        composeRule.setContent {
            DecodedPayloadContent(
                accentTokens = appThemeAccentTokens(),
                decodedPayload =
                    DecodedPayloadViewData(
                        text = "",
                        rawBytesHex = "FF 80",
                        rawBitsBinary = "11111111 10000000",
                        textDecodeStatusCode = BagDecodeContentCodes.STATUS_INVALID_TEXT_PAYLOAD,
                        rawPayloadAvailable = true,
                    ),
                emptyTextResId = com.bag.audioandroid.R.string.audio_result_empty,
                startInRawView = true,
            )
        }

        composeRule.onNodeWithText("11111111 10000000").assertIsDisplayed()
        composeRule.onNodeWithText("FF 80").assertIsDisplayed()
    }

    @Test
    fun `mini mode exposes morse code result`() {
        composeRule.setContent {
            DecodedPayloadContent(
                accentTokens = appThemeAccentTokens(),
                decodedPayload =
                    DecodedPayloadViewData(
                        text = "SOS 1",
                        rawBytesHex = "53 4F 53 20 31",
                        rawBitsBinary = "01010011 01001111 01010011 00100000 00110001",
                        textDecodeStatusCode = BagDecodeContentCodes.STATUS_OK,
                        rawPayloadAvailable = true,
                    ),
                emptyTextResId = com.bag.audioandroid.R.string.audio_result_empty,
                transportMode = TransportModeOption.Mini,
            )
        }

        composeRule.onNodeWithText(string(R.string.audio_follow_view_morse)).performClick()
        composeRule.onNodeWithText("... --- ... / .----").assertIsDisplayed()
    }

    @Test
    fun `non mini mode keeps morse result hidden`() {
        composeRule.setContent {
            DecodedPayloadContent(
                accentTokens = appThemeAccentTokens(),
                decodedPayload =
                    DecodedPayloadViewData(
                        text = "SOS",
                        rawBytesHex = "53 4F 53",
                        rawBitsBinary = "01010011 01001111 01010011",
                        textDecodeStatusCode = BagDecodeContentCodes.STATUS_OK,
                        rawPayloadAvailable = true,
                    ),
                emptyTextResId = com.bag.audioandroid.R.string.audio_result_empty,
                transportMode = TransportModeOption.Flash,
            )
        }

        composeRule.onAllNodesWithText(string(R.string.audio_follow_view_morse)).assertCountEquals(0)
    }

    @Test
    fun `text view shows fallback message when payload is not valid text`() {
        composeRule.setContent {
            DecodedPayloadContent(
                accentTokens = appThemeAccentTokens(),
                decodedPayload =
                    DecodedPayloadViewData(
                        text = "",
                        rawBytesHex = "FF 80",
                        rawBitsBinary = "11111111 10000000",
                        textDecodeStatusCode = BagDecodeContentCodes.STATUS_INVALID_TEXT_PAYLOAD,
                        rawPayloadAvailable = true,
                    ),
                emptyTextResId = com.bag.audioandroid.R.string.audio_result_empty,
            )
        }

        composeRule.onNodeWithText(string(R.string.audio_decode_text_unavailable)).assertIsDisplayed()
    }
}
