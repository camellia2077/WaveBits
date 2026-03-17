package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun PlayerDetailSummarySection(
    miniPlayerModel: MiniPlayerUiModel,
    canExportGeneratedAudio: Boolean,
    onExportGeneratedAudio: () -> Unit,
    onShareSavedAudio: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.audio_player_detail_now_playing),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            PlayerDetailHeaderActions(
                canExportGeneratedAudio = canExportGeneratedAudio,
                onExportGeneratedAudio = onExportGeneratedAudio,
                onShareSavedAudio = onShareSavedAudio
            )
        }
        Text(
            text = when (miniPlayerModel) {
                is MiniPlayerUiModel.Generated -> stringResource(
                    R.string.audio_mini_player_generated_title,
                    stringResource(miniPlayerModel.mode.labelResId)
                )
                is MiniPlayerUiModel.Saved -> miniPlayerModel.displayName
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = when (miniPlayerModel) {
                is MiniPlayerUiModel.Generated -> {
                    val duration = formatDurationMillis(miniPlayerModel.durationMs)
                    if (miniPlayerModel.mode == TransportModeOption.Flash &&
                        miniPlayerModel.flashVoicingStyle != null) {
                        stringResource(
                            R.string.audio_mini_player_generated_flash_subtitle,
                            stringResource(miniPlayerModel.flashVoicingStyle.labelResId),
                            duration
                        )
                    } else {
                        stringResource(R.string.audio_mini_player_duration_only, duration)
                    }
                }
                is MiniPlayerUiModel.Saved -> stringResource(
                    if (miniPlayerModel.modeWireName == "flash" && miniPlayerModel.flashVoicingStyle != null) {
                        R.string.audio_mini_player_generated_flash_subtitle
                    } else {
                        R.string.audio_mini_player_saved_subtitle
                    },
                    if (miniPlayerModel.modeWireName == "flash" && miniPlayerModel.flashVoicingStyle != null) {
                        stringResource(miniPlayerModel.flashVoicingStyle.labelResId)
                    } else {
                        stringResource(SavedAudioModeFilter.labelResIdForModeWireName(miniPlayerModel.modeWireName))
                    },
                    formatDurationMillis(miniPlayerModel.durationMs)
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlayerDetailHeaderActions(
    canExportGeneratedAudio: Boolean,
    onExportGeneratedAudio: () -> Unit,
    onShareSavedAudio: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (canExportGeneratedAudio) {
            HeaderActionButton(
                onClick = onExportGeneratedAudio,
                contentDescription = stringResource(R.string.audio_action_export)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        onShareSavedAudio?.let { shareSavedAudio ->
            HeaderActionButton(
                onClick = shareSavedAudio,
                contentDescription = stringResource(R.string.library_action_share)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        icon()
    }
}
