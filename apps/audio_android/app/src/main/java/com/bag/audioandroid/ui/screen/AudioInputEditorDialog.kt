package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.audioInputTextFieldColors
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.theme.appThemeAccentTokens
import com.bag.audioandroid.ui.utilityActionIconButtonColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioInputEditorDialog(
    selectedThemeStyle: ThemeStyleOption,
    transportMode: TransportModeOption,
    inputText: String,
    placeholderText: String,
    sampleInputLength: SampleInputLengthOption,
    randomizeEnabled: Boolean,
    onInputTextChange: (String) -> Unit,
    onRandomizeSampleInput: (SampleInputLengthOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val accentTokens = appThemeAccentTokens()
    val inputMetrics = measureAudioInputText(inputText)
    val topAppBarColors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = accentTokens.disclosureAccentTint,
            navigationIconContentColor = accentTokens.selectionLabelAccentTint,
            actionIconContentColor = accentTokens.selectionLabelAccentTint,
        )

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = topAppBarColors,
                        title = { Text(stringResource(R.string.audio_input_editor_title)) },
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                colors = utilityActionIconButtonColors(),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.common_back),
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { onRandomizeSampleInput(sampleInputLength) },
                                enabled = randomizeEnabled,
                                colors = utilityActionIconButtonColors(),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Casino,
                                    contentDescription = stringResource(R.string.audio_action_randomize_sample_input),
                                )
                            }
                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = stringResource(R.string.audio_input_editor_done),
                                    color = accentTokens.disclosureAccentTint,
                                )
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.audio_input_editor_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .heightIn(min = 320.dp),
                        minLines = 18,
                        maxLines = 28,
                        label = { Text(stringResource(R.string.audio_input_label)) },
                        placeholder = { Text(placeholderText) },
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
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                lineBreak = LineBreak.Paragraph,
                                hyphens = Hyphens.Auto,
                            ),
                        colors = audioInputTextFieldColors(selectedThemeStyle),
                    )
                }
            }
        }
    }
}
