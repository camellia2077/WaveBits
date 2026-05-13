package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.AudioInputEncodingAnalysis
import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioInputEncodingStatusSectionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `mini morse preview can collapse and expand under input text`() {
        composeRule.setContent {
            InputEncodingStatusSection(
                transportMode = TransportModeOption.Mini,
                analysis =
                    AudioInputEncodingAnalysis(
                        isBlockingInvalid = false,
                        unsupportedCharacters = emptyList(),
                        morseNotation = ".- -...",
                    ),
            )
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_follow_view_morse)).assertIsDisplayed()
        composeRule.onAllNodesWithTag("mini-input-morse-preview").assertCountEquals(1)

        composeRule.onNodeWithTag("mini-input-morse-disclosure").performClick()
        composeRule.onAllNodesWithTag("mini-input-morse-preview").assertCountEquals(0)

        composeRule.onNodeWithTag("mini-input-morse-disclosure").performClick()
        composeRule.onAllNodesWithTag("mini-input-morse-preview").assertCountEquals(1)
    }
}
