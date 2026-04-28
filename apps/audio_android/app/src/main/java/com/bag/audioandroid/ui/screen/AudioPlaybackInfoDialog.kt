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
    val userRows: List<AudioInfoRowModel>,
    val technicalRows: List<AudioInfoRowModel>,
    val dismissLabel: String,
)

internal data class AudioInfoRowModel(
    val label: String,
    val value: String,
    val testTag: String,
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
                    model.userRows.forEach { row ->
                        AudioInfoRow(
                            label = row.label,
                            value = row.value,
                            modifier = Modifier.testTag(row.testTag),
                        )
                    }
                }
                HorizontalDivider()
                AudioInfoSection(
                    title = model.technicalSectionTitle,
                    modifier = Modifier.testTag("audio-info-technical-section"),
                ) {
                    model.technicalRows.forEach { row ->
                        AudioInfoRow(
                            label = row.label,
                            value = row.value,
                            modifier = Modifier.testTag(row.testTag),
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
