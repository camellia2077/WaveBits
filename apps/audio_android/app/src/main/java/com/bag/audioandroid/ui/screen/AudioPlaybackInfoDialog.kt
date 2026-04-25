package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal data class AudioInfoDialogModel(
    val title: String,
    val userSectionTitle: String,
    val technicalSectionTitle: String,
    val encodingTypeLabel: String,
    val encodingTypeValue: String,
    val durationLabel: String,
    val durationValue: String,
    val sampleRateLabel: String,
    val sampleRateValue: String,
    val frameSamplesLabel: String,
    val frameSamplesValue: String,
    val flashVoicingStyleLabel: String,
    val flashVoicingStyleValue: String?,
    val dismissLabel: String,
)

@Composable
internal fun AudioPlaybackInfoDialog(
    model: AudioInfoDialogModel,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = model.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AudioInfoSection(
                    title = model.userSectionTitle,
                    modifier = Modifier.testTag("audio-info-user-section"),
                ) {
                    AudioInfoRow(
                        label = model.encodingTypeLabel,
                        value = model.encodingTypeValue,
                        modifier = Modifier.testTag("audio-info-row-encoding-type"),
                    )
                    AudioInfoRow(
                        label = model.durationLabel,
                        value = model.durationValue,
                        modifier = Modifier.testTag("audio-info-row-duration"),
                    )
                }
                HorizontalDivider()
                AudioInfoSection(
                    title = model.technicalSectionTitle,
                    modifier = Modifier.testTag("audio-info-technical-section"),
                ) {
                    AudioInfoRow(
                        label = model.sampleRateLabel,
                        value = model.sampleRateValue,
                        modifier = Modifier.testTag("audio-info-row-sample-rate"),
                    )
                    AudioInfoRow(
                        label = model.frameSamplesLabel,
                        value = model.frameSamplesValue,
                        modifier = Modifier.testTag("audio-info-row-frame-samples"),
                    )
                    model.flashVoicingStyleValue?.let { flashVoicingStyleValue ->
                        AudioInfoRow(
                            label = model.flashVoicingStyleLabel,
                            value = flashVoicingStyleValue,
                            modifier = Modifier.testTag("audio-info-row-flash-voicing-style"),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = model.dismissLabel)
            }
        },
    )
}

@Composable
private fun AudioInfoSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        content()
    }
}

@Composable
private fun AudioInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
