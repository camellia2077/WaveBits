package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.ui.audioInputTextFieldColors
import com.bag.audioandroid.ui.component.ActionButton
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.theme.appThemeAccentTokens
import com.bag.audioandroid.ui.utilityActionIconButtonColors
import kotlin.math.roundToInt

@Composable
internal fun AudioInputActionsCard(
    selectedThemeStyle: ThemeStyleOption,
    transportMode: TransportModeOption,
    isCodecBusy: Boolean,
    encodeProgress: Float?,
    encodePhase: AudioEncodePhase?,
    isEncodeCancelling: Boolean,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
    inputCardExpanded: Boolean,
    onToggleInputCardExpanded: () -> Unit,
    inputText: String,
    inputPlaceholderText: String,
    onInputTextChange: (String) -> Unit,
    onOpenInputEditor: () -> Unit,
    sampleInputLength: SampleInputLengthOption,
    onSampleInputLengthSelected: (SampleInputLengthOption) -> Unit,
    onRandomizeSampleInput: () -> Unit,
    onClearInput: () -> Unit,
    onEncode: () -> Unit,
    onCancelEncode: () -> Unit,
    flashVoicingExpanded: Boolean,
    onToggleFlashVoicingExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEncodingBusy = isCodecBusy && encodeProgress != null
    val encodePercent = ((encodeProgress ?: 0f).coerceIn(0f, 1f) * 100f).roundToInt()
    val inputMetrics = measureAudioInputText(inputText)
    val accentTokens = appThemeAccentTokens()

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AudioInputCardHeader(
                title = stringResource(R.string.audio_input_label),
                enabled = !isCodecBusy,
                expanded = inputCardExpanded,
                onToggleExpanded = onToggleInputCardExpanded,
            )
            if (inputCardExpanded) {
                if (transportMode == TransportModeOption.Flash) {
                    FlashVoicingSelectorSection(
                        enabled = !isCodecBusy,
                        expanded = flashVoicingExpanded,
                        onToggleExpanded = onToggleFlashVoicingExpanded,
                        selectedFlashVoicingStyle = selectedFlashVoicingStyle,
                        onFlashVoicingStyleSelected = onFlashVoicingStyleSelected,
                    )
                }
                AudioInputFieldHeader(
                    enabled = !isCodecBusy,
                    onOpenInputEditor = onOpenInputEditor,
                    sampleInputLength = sampleInputLength,
                    onSampleInputLengthSelected = onSampleInputLengthSelected,
                    onRandomizeSampleInput = onRandomizeSampleInput,
                    onClearInput = onClearInput,
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    enabled = !isCodecBusy,
                    label = { Text(stringResource(R.string.audio_input_label)) },
                    placeholder = { Text(inputPlaceholderText) },
                    supportingText = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            AudioInputMetricsSummaryRow(
                                charsetHint = stringResource(transportMode.charsetHintResId),
                                metricsText =
                                    stringResource(
                                        R.string.audio_input_metrics,
                                        inputMetrics.characterCount,
                                        inputMetrics.byteCount,
                                    ),
                            )
                        }
                    },
                    minLines = 2,
                    maxLines = 8,
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            lineBreak = LineBreak.Paragraph,
                            hyphens = Hyphens.Auto,
                        ),
                    colors = audioInputTextFieldColors(selectedThemeStyle),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.audio_input_editor_inline_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AudioEncodeStatusSection(
                    encodeProgress = encodeProgress,
                    encodePhase = encodePhase,
                    isEncodingBusy = isEncodingBusy,
                )
                if (isEncodingBusy) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ActionButton(
                            text =
                                stringResource(
                                    R.string.audio_action_encode_busy_progress,
                                    encodePercent,
                                ),
                            onClick = {},
                            enabled = false,
                            borderColor = accentTokens.selectionBorderAccentTint,
                            borderWidth = 2.dp,
                            modifier = Modifier.weight(1f),
                        )
                        ActionButton(
                            text =
                                stringResource(
                                    if (isEncodeCancelling) {
                                        R.string.audio_action_cancel_encode_busy
                                    } else {
                                        R.string.audio_action_cancel_encode
                                    },
                                ),
                            onClick = onCancelEncode,
                            enabled = !isEncodeCancelling,
                            borderColor = accentTokens.selectionBorderAccentTint,
                            borderWidth = 2.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    ActionButton(
                        text = stringResource(R.string.audio_action_encode),
                        onClick = onEncode,
                        enabled = !isCodecBusy,
                        textColor = accentTokens.disclosureAccentTint,
                        borderColor = accentTokens.selectionBorderAccentTint,
                        borderWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AudioInputMetricsSummaryRow(
    charsetHint: String,
    metricsText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = charsetHint,
            modifier = Modifier.weight(1f),
        )
        Text(text = metricsText)
    }
}

@Composable
private fun AudioInputCardHeader(
    title: String,
    enabled: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val accentTokens = appThemeAccentTokens()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = accentTokens.disclosureAccentTint,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onToggleExpanded,
            enabled = enabled,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription =
                    stringResource(
                        if (expanded) {
                            R.string.audio_action_collapse_input
                        } else {
                            R.string.audio_action_expand_input
                        },
                    ),
                tint = accentTokens.disclosureAccentTint,
            )
        }
    }
}

@Composable
private fun AudioInputFieldHeader(
    enabled: Boolean,
    onOpenInputEditor: () -> Unit,
    sampleInputLength: SampleInputLengthOption,
    onSampleInputLengthSelected: (SampleInputLengthOption) -> Unit,
    onRandomizeSampleInput: () -> Unit,
    onClearInput: () -> Unit,
) {
    val accentTokens = appThemeAccentTokens()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow {
            SampleInputLengthOption.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = sampleInputLength == option,
                    onClick = { onSampleInputLengthSelected(option) },
                    enabled = enabled,
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SampleInputLengthOption.entries.size,
                        ),
                    colors = appSegmentedButtonColors(),
                    label = {
                        Text(text = stringResource(option.labelResId))
                    },
                )
            }
        }
        IconButton(
            onClick = onClearInput,
            enabled = enabled,
            colors = utilityActionIconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.audio_action_clear),
            )
        }
        IconButton(
            onClick = onOpenInputEditor,
            enabled = enabled,
            colors = utilityActionIconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = stringResource(R.string.audio_action_open_input_editor),
            )
        }
        IconButton(
            onClick = onRandomizeSampleInput,
            enabled = enabled,
            colors = utilityActionIconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Casino,
                contentDescription = stringResource(R.string.audio_action_randomize_sample_input),
            )
        }
    }
}
