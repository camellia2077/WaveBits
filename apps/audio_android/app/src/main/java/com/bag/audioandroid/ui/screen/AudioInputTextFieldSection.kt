package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.audioInputTextFieldColors
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun AudioInputTextFieldSection(
    selectedThemeStyle: ThemeStyleOption,
    transportMode: TransportModeOption,
    enabled: Boolean,
    inputText: String,
    inputPlaceholderText: String,
    onInputTextChange: (String) -> Unit,
) {
    var isInputFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = inputText,
        onValueChange = onInputTextChange,
        enabled = enabled,
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
}
