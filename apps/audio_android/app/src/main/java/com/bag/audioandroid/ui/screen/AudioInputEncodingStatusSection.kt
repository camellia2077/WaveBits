package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
