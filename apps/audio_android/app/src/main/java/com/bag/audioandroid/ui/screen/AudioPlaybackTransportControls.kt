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
    totalSamples: Int,
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
    val infoDialogTitle = stringResource(R.string.audio_info_dialog_title)
    val infoUserSectionTitle = stringResource(R.string.audio_info_user_section)
    val infoTechnicalSectionTitle = stringResource(R.string.audio_info_technical_section)
    val encodingTypeLabel = stringResource(R.string.audio_info_encoding_type)
    val encodingTypeValue = stringResource(transportMode.labelResId)
    val durationLabel = stringResource(R.string.audio_info_duration)
    val durationValue = formatDurationMillis(durationMs)
    val savedTimeLabel = stringResource(R.string.audio_player_detail_saved_time)
    val generatedTimeLabel = stringResource(R.string.audio_player_detail_saved_generated_time)
    val inputSourceLabel = stringResource(R.string.audio_player_detail_saved_input_source)
    val inputSourceValue = savedAudioItem?.inputSourceKind?.let { stringResource(it.labelResId) }
    val sampleRateLabel = stringResource(R.string.audio_info_sample_rate)
    val sampleRateValue = stringResource(R.string.audio_info_sample_rate_value, sampleRateHz)
    val frameSamplesLabel = stringResource(R.string.audio_info_frame_samples)
    val frameSamplesValue = stringResource(R.string.audio_info_frame_samples_value, frameSamples)
    val fileSizeLabel = stringResource(R.string.audio_player_detail_saved_file_size)
    val displayFileSizeBytes =
        savedAudioItem?.fileSizeBytes
            ?: estimatedMonoPcm16WavSizeBytes(totalSamples)
    val fileSizeValue =
        displayFileSizeBytes?.let { fileSizeBytes ->
            stringResource(R.string.audio_player_detail_saved_payload_bytes_value, fileSizeBytes)
        }
    val payloadBytesLabel = stringResource(R.string.audio_player_detail_saved_payload_bytes)
    val payloadBytesValue =
        savedAudioItem
            ?.payloadByteCount
            ?.let { payloadByteCount ->
                stringResource(R.string.audio_player_detail_saved_payload_bytes_value, payloadByteCount)
            }
    val flashVoicingStyleLabel = stringResource(R.string.audio_info_flash_voicing_style)
    val flashVoicingStyleValue = flashVoicingStyle?.let { stringResource(it.labelResId) }
    val dismissLabel = stringResource(R.string.common_cancel)
    val audioInfoDialogModel =
        remember(
            infoDialogTitle,
            infoUserSectionTitle,
            infoTechnicalSectionTitle,
            encodingTypeLabel,
            encodingTypeValue,
            durationLabel,
            durationValue,
            savedTimeLabel,
            generatedTimeLabel,
            inputSourceLabel,
            inputSourceValue,
            sampleRateLabel,
            sampleRateValue,
            frameSamplesLabel,
            frameSamplesValue,
            fileSizeLabel,
            fileSizeValue,
            payloadBytesLabel,
            payloadBytesValue,
            flashVoicingStyleLabel,
            flashVoicingStyleValue,
            dismissLabel,
            savedAudioItem,
            transportMode,
        ) {
            AudioInfoDialogModel(
                title = infoDialogTitle,
                userSectionTitle = infoUserSectionTitle,
                technicalSectionTitle = infoTechnicalSectionTitle,
                userRows =
                    buildList {
                        add(
                            AudioInfoRowModel(
                                label = encodingTypeLabel,
                                value = encodingTypeValue,
                                testTag = "audio-info-row-encoding-type",
                            ),
                        )
                        add(
                            AudioInfoRowModel(
                                label = durationLabel,
                                value = durationValue,
                                testTag = "audio-info-row-duration",
                            ),
                        )
                        fileSizeValue?.let {
                            add(
                                AudioInfoRowModel(
                                    label = fileSizeLabel,
                                    value = it,
                                    testTag = "audio-info-row-file-size",
                                ),
                            )
                        }
                        savedAudioItem?.let { item ->
                            add(
                                AudioInfoRowModel(
                                    label = savedTimeLabel,
                                    value = formatSavedAudioTime(item.savedAtEpochSeconds),
                                    testTag = "audio-info-row-saved-time",
                                ),
                            )
                            item.generatedAtEpochSeconds?.let { generatedAtEpochSeconds ->
                                add(
                                    AudioInfoRowModel(
                                        label = generatedTimeLabel,
                                        value = formatSavedAudioTime(generatedAtEpochSeconds),
                                        testTag = "audio-info-row-generated-time",
                                    ),
                                )
                            }
                            inputSourceValue?.let {
                                add(
                                    AudioInfoRowModel(
                                        label = inputSourceLabel,
                                        value = it,
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
                                label = sampleRateLabel,
                                value = sampleRateValue,
                                testTag = "audio-info-row-sample-rate",
                            ),
                        )
                        add(
                            AudioInfoRowModel(
                                label = frameSamplesLabel,
                                value = frameSamplesValue,
                                testTag = "audio-info-row-frame-samples",
                            ),
                        )
                        payloadBytesValue?.let {
                            add(
                                AudioInfoRowModel(
                                    label = payloadBytesLabel,
                                    value = it,
                                    testTag = "audio-info-row-payload-bytes",
                                ),
                            )
                        }
                        if (transportMode == TransportModeOption.Flash && flashVoicingStyleValue != null) {
                            add(
                                AudioInfoRowModel(
                                    label = flashVoicingStyleLabel,
                                    value = flashVoicingStyleValue,
                                    testTag = "audio-info-row-flash-voicing-style",
                                ),
                            )
                        }
                    },
                dismissLabel = dismissLabel,
            )
        }

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

private fun estimatedMonoPcm16WavSizeBytes(totalSamples: Int): Long? {
    if (totalSamples <= 0) {
        return null
    }
    // Generated and fallback saved playback use mono PCM16 WAV, so this gives a
    // stable display value even when MediaStore has not returned SIZE yet.
    return WavHeaderByteCount + totalSamples.toLong() * Pcm16BytesPerSample
}

private const val WavHeaderByteCount = 44L
private const val Pcm16BytesPerSample = 2L
