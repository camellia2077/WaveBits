package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MiniPlayerLeadingIcon
import com.bag.audioandroid.ui.model.MiniPlayerSource
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerDetailSheetContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `player detail sheet renders playback section inside real scroll structure`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Flash"),
                        subtitle =
                            UiText.Resource(
                                R.string.audio_mini_player_generated_flash_subtitle,
                                listOf(UiText.Resource(FlashVoicingStyleOption.RitualChant.labelResId), "0:44"),
                            ),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Flash,
                        isFlashMode = true,
                        flashVoicingStyle = FlashVoicingStyleOption.RitualChant,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = false,
                followData = sampleFollowData(),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onExportGeneratedAudio = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithTag("player-detail-sheet-content").assertIsDisplayed()
        composeRule.onNodeWithTag("playback-display-section").assertIsDisplayed()
        composeRule.onNodeWithTag("playback-display-switcher").assertIsDisplayed()
        composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.audio_player_detail_now_playing)).assertCountEquals(0)
    }

    @Test
    fun `audio info button opens dialog with transport mode and duration`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Pro"),
                        subtitle = UiText.Plain("generated"),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Pro,
                        isFlashMode = false,
                        flashVoicingStyle = null,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = true,
                followData = sampleFollowData(),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onExportGeneratedAudio = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.audio_action_open_audio_info)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_info_dialog_title)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.transport_mode_pro_label)).assertIsDisplayed()
        composeRule.onNodeWithText("0:44").assertIsDisplayed()
        composeRule.onAllNodesWithTag("audio-info-user-section", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("audio-info-technical-section", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("audio-info-row-sample-rate", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("audio-info-row-frame-samples", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `flash audio info dialog shows voicing style`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Flash"),
                        subtitle = UiText.Plain("generated"),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Flash,
                        isFlashMode = true,
                        flashVoicingStyle = FlashVoicingStyleOption.RitualChant,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = true,
                followData = sampleFollowData(),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onExportGeneratedAudio = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.audio_action_open_audio_info)).performClick()
        composeRule.onAllNodesWithTag("audio-info-row-flash-voicing-style", useUnmergedTree = true).assertCountEquals(1)
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
