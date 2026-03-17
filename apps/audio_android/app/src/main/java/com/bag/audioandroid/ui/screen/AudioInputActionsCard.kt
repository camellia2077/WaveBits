package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.ui.component.ActionButton
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import kotlin.math.roundToInt

@Composable
internal fun AudioInputActionsCard(
    transportMode: TransportModeOption,
    isCodecBusy: Boolean,
    encodeProgress: Float?,
    encodePhase: AudioEncodePhase?,
    isEncodeCancelling: Boolean,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onRandomizeSampleInput: () -> Unit,
    onEncode: () -> Unit,
    onCancelEncode: () -> Unit,
    flashVoicingExpanded: Boolean,
    onToggleFlashVoicingExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEncodingBusy = isCodecBusy && encodeProgress != null
    val encodePercent = ((encodeProgress ?: 0f).coerceIn(0f, 1f) * 100f).roundToInt()

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AudioInputCardHeader(
                title = stringResource(R.string.audio_input_label),
                transportMode = transportMode,
                enabled = !isCodecBusy,
                flashVoicingExpanded = flashVoicingExpanded,
                onToggleFlashVoicingExpanded = onToggleFlashVoicingExpanded
            )
            if (transportMode == TransportModeOption.Flash && flashVoicingExpanded) {
                FlashVoicingSelectorSection(
                    enabled = !isCodecBusy,
                    selectedFlashVoicingStyle = selectedFlashVoicingStyle,
                    onFlashVoicingStyleSelected = onFlashVoicingStyleSelected
                )
            }
            AudioInputFieldHeader(
                enabled = !isCodecBusy,
                onRandomizeSampleInput = onRandomizeSampleInput
            )
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                enabled = !isCodecBusy,
                label = { Text(stringResource(R.string.audio_input_label)) },
                placeholder = { Text(stringResource(transportMode.exampleTextResId)) },
                supportingText = { Text(stringResource(transportMode.charsetHintResId)) },
                minLines = 1,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
            )
            if (isEncodingBusy) {
                AudioEncodeProgressSection(
                    encodeProgress = requireNotNull(encodeProgress),
                    encodePhase = encodePhase
                )
            }
            if (isEncodingBusy) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        text = stringResource(
                            R.string.audio_action_encode_busy_progress,
                            encodePercent
                        ),
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = stringResource(
                            if (isEncodeCancelling) {
                                R.string.audio_action_cancel_encode_busy
                            } else {
                                R.string.audio_action_cancel_encode
                            }
                        ),
                        onClick = onCancelEncode,
                        enabled = !isEncodeCancelling,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                ActionButton(
                    text = stringResource(R.string.audio_action_encode),
                    onClick = onEncode,
                    enabled = !isCodecBusy
                )
            }
        }
    }
}

@Composable
private fun AudioInputCardHeader(
    title: String,
    transportMode: TransportModeOption,
    enabled: Boolean,
    flashVoicingExpanded: Boolean,
    onToggleFlashVoicingExpanded: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        if (transportMode == TransportModeOption.Flash) {
            IconButton(
                onClick = onToggleFlashVoicingExpanded,
                enabled = enabled
            ) {
                Icon(
                    imageVector = if (flashVoicingExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(
                        if (flashVoicingExpanded) {
                            R.string.audio_action_collapse_flash_style
                        } else {
                            R.string.audio_action_expand_flash_style
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun AudioInputFieldHeader(
    enabled: Boolean,
    onRandomizeSampleInput: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onRandomizeSampleInput,
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Rounded.Casino,
                contentDescription = stringResource(R.string.audio_action_randomize_sample_input)
            )
        }
    }
}

@Composable
private fun AudioEncodeProgressSection(
    encodeProgress: Float,
    encodePhase: AudioEncodePhase?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        encodePhase?.let { phase ->
            Text(
                text = stringResource(phase.labelResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { encodeProgress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val AudioEncodePhase.labelResId: Int
    get() = when (this) {
        AudioEncodePhase.PreparingInput -> R.string.audio_encode_phase_preparing_input_label
        AudioEncodePhase.RenderingPcm -> R.string.audio_encode_phase_rendering_pcm_label
        AudioEncodePhase.Postprocessing -> R.string.audio_encode_phase_postprocessing_label
        AudioEncodePhase.Finalizing -> R.string.audio_encode_phase_finalizing_label
    }
