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
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.theme.appThemeAccentTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioInputEditorDialog(
    inputText: String,
    placeholderText: String,
    sampleInputLength: SampleInputLengthOption,
    randomizeEnabled: Boolean,
    onInputTextChange: (String) -> Unit,
    onRandomizeSampleInput: (SampleInputLengthOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val accentTokens = appThemeAccentTokens()
    val topAppBarColors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = accentTokens.disclosureAccentTint,
            navigationIconContentColor = accentTokens.actionAccentTint,
            actionIconContentColor = accentTokens.actionAccentTint,
        )
    val topBarActionButtonColors =
        IconButtonDefaults.iconButtonColors(
            contentColor = accentTokens.actionAccentTint,
            disabledContentColor = accentTokens.actionAccentTint.copy(alpha = 0.38f),
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
                                colors = topBarActionButtonColors,
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
                                colors = topBarActionButtonColors,
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
                            Text(
                                text = stringResource(R.string.audio_input_editor_count, inputText.length),
                            )
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                    )
                }
            }
        }
    }
}
