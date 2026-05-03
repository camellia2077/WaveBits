package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.SavedAudioModeFilter

@Composable
internal fun PlayerDetailSavedInfoSection(
    item: SavedAudioItem,
    currentSampleRateHz: Int,
    modifier: Modifier = Modifier,
) {
    // This section surfaces the metadata that explains how a saved file was produced,
    // not just when it was saved. That keeps playback detail useful even when the
    // original input text is no longer visible.
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.audio_player_detail_saved_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            PlayerDetailInfoRow(
                label = stringResource(R.string.audio_player_detail_saved_mode),
                value = stringResource(SavedAudioModeFilter.labelResIdForModeWireName(item.modeWireName)),
            )
            PlayerDetailInfoRow(
                label = stringResource(R.string.audio_player_detail_saved_duration),
                value = formatDurationMillis(item.durationMs),
            )
            PlayerDetailInfoRow(
                label = stringResource(R.string.audio_player_detail_saved_time),
                value = formatSavedAudioTime(item.savedAtEpochSeconds),
            )
            item.generatedAtEpochSeconds?.let { generatedAtEpochSeconds ->
                PlayerDetailInfoRow(
                    label = stringResource(R.string.audio_player_detail_saved_generated_time),
                    value = formatSavedAudioTime(generatedAtEpochSeconds),
                )
            }
            item.inputSourceKind?.let { inputSourceKind ->
                PlayerDetailInfoRow(
                    label = stringResource(R.string.audio_player_detail_saved_input_source),
                    value = stringResource(inputSourceKind.labelResId),
                )
            }
            val savedSampleRateHz = item.sampleRateHz?.takeIf { it > 0 } ?: currentSampleRateHz.takeIf { it > 0 }
            if (savedSampleRateHz != null) {
                PlayerDetailInfoRow(
                    label = stringResource(R.string.audio_player_detail_saved_sample_rate),
                    value = stringResource(R.string.audio_info_sample_rate_value, savedSampleRateHz),
                )
            }
            item.fileSizeBytes?.let { fileSizeBytes ->
                PlayerDetailInfoRow(
                    label = stringResource(R.string.audio_player_detail_saved_file_size),
                    value = formatStorageSizeMb(fileSizeBytes),
                )
            }
            item.payloadByteCount?.let { payloadByteCount ->
                PlayerDetailInfoRow(
                    label = stringResource(R.string.audio_player_detail_saved_payload_bytes),
                    value = formatStorageSizeMb(payloadByteCount.toLong()),
                )
            }
        }
    }
}

private val GeneratedAudioInputSourceKind.labelResId: Int
    get() =
        when (this) {
            GeneratedAudioInputSourceKind.Manual -> R.string.audio_generated_input_source_manual
            GeneratedAudioInputSourceKind.Sample -> R.string.audio_generated_input_source_sample
        }

@Composable
private fun PlayerDetailInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
