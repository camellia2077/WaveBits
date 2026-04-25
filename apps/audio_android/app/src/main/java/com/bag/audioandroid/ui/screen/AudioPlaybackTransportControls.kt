package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.playerChromeColors

@Composable
internal fun AudioPlaybackTransportControls(
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    canExportGeneratedAudio: Boolean,
    transportMode: TransportModeOption,
    durationMs: Long,
    sampleRateHz: Int,
    frameSamples: Int,
    flashVoicingStyle: FlashVoicingStyleOption?,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onExportGeneratedAudio: () -> Unit,
    onOpenSavedAudioSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerColors = playerChromeColors()
    var showAudioInfo by remember { mutableStateOf(false) }
    val audioInfoDialogModel =
        AudioInfoDialogModel(
            title = stringResource(R.string.audio_info_dialog_title),
            userSectionTitle = stringResource(R.string.audio_info_user_section),
            technicalSectionTitle = stringResource(R.string.audio_info_technical_section),
            encodingTypeLabel = stringResource(R.string.audio_info_encoding_type),
            encodingTypeValue = stringResource(transportMode.labelResId),
            durationLabel = stringResource(R.string.audio_info_duration),
            durationValue = formatDurationMillis(durationMs),
            sampleRateLabel = stringResource(R.string.audio_info_sample_rate),
            sampleRateValue = stringResource(R.string.audio_info_sample_rate_value, sampleRateHz),
            frameSamplesLabel = stringResource(R.string.audio_info_frame_samples),
            frameSamplesValue = stringResource(R.string.audio_info_frame_samples_value, frameSamples),
            flashVoicingStyleLabel = stringResource(R.string.audio_info_flash_voicing_style),
            flashVoicingStyleValue =
                if (transportMode == TransportModeOption.Flash && flashVoicingStyle != null) {
                    stringResource(flashVoicingStyle.labelResId)
                } else {
                    null
                },
            dismissLabel = stringResource(R.string.common_cancel),
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AudioPlaybackPrimaryControlsRow(
            isPlaying = isPlaying,
            playbackSequenceMode = playbackSequenceMode,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
            onOpenSavedAudioSheet = onOpenSavedAudioSheet,
            colors = playerColors,
        )
        if (canExportGeneratedAudio) {
            AudioPlaybackSecondaryActionsRow(
                onOpenAudioInfo = { showAudioInfo = true },
                onExportGeneratedAudio = onExportGeneratedAudio,
                contentColor = playerColors.neutralAction,
            )
        }
    }
    if (showAudioInfo) {
        AudioPlaybackInfoDialog(
            model = audioInfoDialogModel,
            onDismiss = { showAudioInfo = false },
        )
    }
}
