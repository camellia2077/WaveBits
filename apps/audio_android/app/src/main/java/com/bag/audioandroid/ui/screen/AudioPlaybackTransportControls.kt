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
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.playerChromeColors

@Composable
internal fun AudioPlaybackTransportControls(
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    playbackSpeed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    canExportGeneratedAudio: Boolean,
    transportMode: TransportModeOption,
    durationMs: Long,
    sampleRateHz: Int,
    frameSamples: Int,
    flashVoicingStyle: FlashVoicingStyleOption?,
    savedAudioItem: SavedAudioItem?,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onExportGeneratedAudio: () -> Unit,
    onExportGeneratedAudioToDocument: () -> Unit,
    onOpenSavedAudioSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerColors = playerChromeColors()
    var showAudioInfo by remember { mutableStateOf(false) }
    var showSpeedAdjustment by remember { mutableStateOf(false) }
    val audioInfoDialogModel =
        AudioInfoDialogModel(
            title = stringResource(R.string.audio_info_dialog_title),
            userSectionTitle = stringResource(R.string.audio_info_user_section),
            technicalSectionTitle = stringResource(R.string.audio_info_technical_section),
            userRows =
                buildList {
                    add(
                        AudioInfoRowModel(
                            label = stringResource(R.string.audio_info_encoding_type),
                            value = stringResource(transportMode.labelResId),
                            testTag = "audio-info-row-encoding-type",
                        ),
                    )
                    add(
                        AudioInfoRowModel(
                            label = stringResource(R.string.audio_info_duration),
                            value = formatDurationMillis(durationMs),
                            testTag = "audio-info-row-duration",
                        ),
                    )
                    savedAudioItem?.let { item ->
                        add(
                            AudioInfoRowModel(
                                label = stringResource(R.string.audio_player_detail_saved_time),
                                value = formatSavedAudioTime(item.savedAtEpochSeconds),
                                testTag = "audio-info-row-saved-time",
                            ),
                        )
                        item.generatedAtEpochSeconds?.let { generatedAtEpochSeconds ->
                            add(
                                AudioInfoRowModel(
                                    label = stringResource(R.string.audio_player_detail_saved_generated_time),
                                    value = formatSavedAudioTime(generatedAtEpochSeconds),
                                    testTag = "audio-info-row-generated-time",
                                ),
                            )
                        }
                        item.inputSourceKind?.let { inputSourceKind ->
                            add(
                                AudioInfoRowModel(
                                    label = stringResource(R.string.audio_player_detail_saved_input_source),
                                    value = stringResource(inputSourceKind.labelResId),
                                    testTag = "audio-info-row-input-source",
                                ),
                            )
                        }
                    }
                },
            technicalRows =
                buildList {
                    add(
                        AudioInfoRowModel(
                            label = stringResource(R.string.audio_info_sample_rate),
                            value = stringResource(R.string.audio_info_sample_rate_value, sampleRateHz),
                            testTag = "audio-info-row-sample-rate",
                        ),
                    )
                    add(
                        AudioInfoRowModel(
                            label = stringResource(R.string.audio_info_frame_samples),
                            value = stringResource(R.string.audio_info_frame_samples_value, frameSamples),
                            testTag = "audio-info-row-frame-samples",
                        ),
                    )
                    savedAudioItem?.payloadByteCount?.let { payloadByteCount ->
                        add(
                            AudioInfoRowModel(
                                label = stringResource(R.string.audio_player_detail_saved_payload_bytes),
                                value = stringResource(R.string.audio_player_detail_saved_payload_bytes_value, payloadByteCount),
                                testTag = "audio-info-row-payload-bytes",
                            ),
                        )
                    }
                    if (transportMode == TransportModeOption.Flash && flashVoicingStyle != null) {
                        add(
                            AudioInfoRowModel(
                                label = stringResource(R.string.audio_info_flash_voicing_style),
                                value = stringResource(flashVoicingStyle.labelResId),
                                testTag = "audio-info-row-flash-voicing-style",
                            ),
                        )
                    }
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
        AudioPlaybackSecondaryActionsRow(
            playbackSpeed = playbackSpeed,
            showSpeedAdjustment = showSpeedAdjustment,
            onOpenAudioInfo = { showAudioInfo = true },
            onCyclePlaybackSpeed = {
                onPlaybackSpeedSelected(PlaybackSpeedOption.nextSpeed(playbackSpeed))
            },
            onToggleSpeedAdjustment = { showSpeedAdjustment = !showSpeedAdjustment },
            onPlaybackSpeedSelected = onPlaybackSpeedSelected,
            onExportGeneratedAudio = onExportGeneratedAudio,
            onExportGeneratedAudioToDocument = onExportGeneratedAudioToDocument,
            canExportGeneratedAudio = canExportGeneratedAudio,
            contentColor = playerColors.neutralAction,
        )
    }
    if (showAudioInfo) {
        AudioPlaybackInfoDialog(
            model = audioInfoDialogModel,
            onDismiss = { showAudioInfo = false },
        )
    }
}

private val GeneratedAudioInputSourceKind.labelResId: Int
    get() =
        when (this) {
            GeneratedAudioInputSourceKind.Manual -> R.string.audio_generated_input_source_manual
            GeneratedAudioInputSourceKind.Sample -> R.string.audio_generated_input_source_sample
        }
