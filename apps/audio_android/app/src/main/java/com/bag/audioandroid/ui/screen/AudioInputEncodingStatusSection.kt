package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.AudioInputEncodingAnalysis
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun InputEncodingStatusSection(
    transportMode: TransportModeOption,
    analysis: AudioInputEncodingAnalysis,
) {
    var isMiniMorseExpanded by rememberSaveable { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InputEncodingMessage(
            transportMode = transportMode,
            analysis = analysis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (transportMode == TransportModeOption.Mini && !analysis.isBlockingInvalid && analysis.morseNotation.isNotBlank()) {
            MiniInputMorseDisclosureRow(
                expanded = isMiniMorseExpanded,
                onExpandedChanged = { isMiniMorseExpanded = it },
            )
            if (isMiniMorseExpanded) {
                SelectionContainer {
                    Text(
                        text = analysis.morseNotation,
                        modifier = Modifier.testTag("mini-input-morse-preview"),
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
}

@Composable
private fun MiniInputMorseDisclosureRow(
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onExpandedChanged(!expanded) }
                .testTag("mini-input-morse-disclosure"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.audio_follow_view_morse),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InputEncodingMessage(
    transportMode: TransportModeOption,
    analysis: AudioInputEncodingAnalysis,
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
            else -> null
        }
    if (message == null) {
        return
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
internal fun InputEncodingRulesDialog(
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
