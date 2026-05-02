package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.audioInputTextFieldColors
import com.bag.audioandroid.ui.component.ActionButton
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.analyzeAudioInputEncoding
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
    isFlashVoicingEnabled: Boolean,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
    selectedMorseSpeed: MorseSpeedOption,
    onMorseSpeedSelected: (MorseSpeedOption) -> Unit,
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
    modifier: Modifier = Modifier,
) {
    val isEncodingBusy = isCodecBusy && encodeProgress != null
    val accentTokens = appThemeAccentTokens()
    val inputEncodingAnalysis =
        remember(transportMode, inputText) {
            analyzeAudioInputEncoding(transportMode, inputText)
        }
    val canEncodeInput = !inputEncodingAnalysis.isBlockingInvalid

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                        isFlashVoicingEnabled = isFlashVoicingEnabled,
                        selectedFlashVoicingStyle = selectedFlashVoicingStyle,
                        onFlashVoicingStyleSelected = onFlashVoicingStyleSelected,
                    )
                }
                if (transportMode == TransportModeOption.Mini) {
                    MorseSpeedSelectorSection(
                        enabled = !isCodecBusy,
                        selectedMorseSpeed = selectedMorseSpeed,
                        onMorseSpeedSelected = onMorseSpeedSelected,
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

                var isInputFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    enabled = !isCodecBusy,
                    label = {
                        Text(
                            text = stringResource(R.string.audio_input_label),
                            fontWeight = if (isInputFocused) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                    placeholder = { Text(inputPlaceholderText) },
                    supportingText = {
                        val inputMetrics = measureAudioInputText(inputText)
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
                            fontWeight = if (isInputFocused) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                    colors = audioInputTextFieldColors(selectedThemeStyle),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { isInputFocused = it.isFocused },
                )

                if (inputText.isNotBlank()) {
                    InputEncodingStatusSection(
                        transportMode = transportMode,
                        analysis = inputEncodingAnalysis,
                    )
                }

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
                        val encodePercent = ((encodeProgress ?: 0f).coerceIn(0f, 1f) * 100f).roundToInt()
                        ActionButton(
                            text = stringResource(R.string.audio_action_encode_busy_progress, encodePercent),
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
                        enabled = !isCodecBusy && canEncodeInput,
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
private fun MorseSpeedSelectorSection(
    enabled: Boolean,
    selectedMorseSpeed: MorseSpeedOption,
    onMorseSpeedSelected: (MorseSpeedOption) -> Unit,
) {
    SingleChoiceSegmentedButtonRow {
        MorseSpeedOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selectedMorseSpeed == option,
                onClick = { onMorseSpeedSelected(option) },
                enabled = enabled,
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = MorseSpeedOption.entries.size,
                    ),
                colors = appSegmentedButtonColors(),
                label = { Text(text = stringResource(option.labelResId)) },
            )
        }
    }
}

@Composable
private fun InputEncodingStatusSection(
    transportMode: TransportModeOption,
    analysis: com.bag.audioandroid.ui.model.AudioInputEncodingAnalysis,
) {
    var showRules by remember { mutableStateOf(false) }
    if (showRules) {
        InputEncodingRulesDialog(
            transportMode = transportMode,
            onDismiss = { showRules = false },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InputEncodingMessage(
                transportMode = transportMode,
                analysis = analysis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showRules = true }) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                )
                Text(text = stringResource(R.string.audio_input_encoding_rules))
            }
        }
        if (transportMode == TransportModeOption.Mini && !analysis.isBlockingInvalid && analysis.morseNotation.isNotBlank()) {
            SelectionContainer {
                Text(
                    text = analysis.morseNotation,
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InputEncodingMessage(
    transportMode: TransportModeOption,
    analysis: com.bag.audioandroid.ui.model.AudioInputEncodingAnalysis,
    modifier: Modifier = Modifier,
) {
    val message =
        when {
            transportMode == TransportModeOption.Mini && analysis.isBlockingInvalid ->
                stringResource(
                    R.string.audio_morse_unsupported_characters,
                    analysis.unsupportedCharacters.joinToString(" "),
                )
            transportMode == TransportModeOption.Pro && analysis.isBlockingInvalid ->
                stringResource(
                    R.string.audio_pro_unsupported_characters,
                    analysis.unsupportedCharacters.joinToString(" "),
                )
            transportMode == TransportModeOption.Mini -> stringResource(R.string.audio_input_encoding_valid_mini)
            transportMode == TransportModeOption.Pro -> stringResource(R.string.audio_input_encoding_valid_pro)
            else -> stringResource(transportMode.charsetHintResId)
        }
    Text(
        text = message,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color =
            if (analysis.isBlockingInvalid) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

@Composable
private fun InputEncodingRulesDialog(
    transportMode: TransportModeOption,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.audio_input_encoding_rules_title))
        },
        text = {
            Text(
                text =
                    stringResource(
                        when (transportMode) {
                            TransportModeOption.Mini -> R.string.audio_input_encoding_rules_mini
                            TransportModeOption.Pro -> R.string.audio_input_encoding_rules_pro
                            TransportModeOption.Flash,
                            TransportModeOption.Ultra,
                            -> R.string.audio_input_encoding_rules_unrestricted
                        },
                    ),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.audio_input_encoding_rules_done))
            }
        },
    )
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
