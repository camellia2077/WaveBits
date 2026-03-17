package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.SavedAudioModeFilter

@Composable
internal fun MiniPlayerBar(
    model: MiniPlayerUiModel,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onOpenSavedAudioSheet: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniPlayerLeadingIcon(model = model)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = miniPlayerTitle(model),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = miniPlayerSubtitle(model),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onOpenSavedAudioSheet) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = stringResource(R.string.audio_action_open_saved_audio_list)
                )
            }
            FilledIconButton(
                onClick = onTogglePlayback,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.audio_action_pause else R.string.audio_action_play
                    )
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerLeadingIcon(model: MiniPlayerUiModel) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (model) {
                    is MiniPlayerUiModel.Generated -> Icons.Rounded.GraphicEq
                    is MiniPlayerUiModel.Saved -> Icons.Rounded.LibraryMusic
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun miniPlayerTitle(model: MiniPlayerUiModel): String =
    when (model) {
        is MiniPlayerUiModel.Generated -> stringResource(
            R.string.audio_mini_player_generated_title,
            stringResource(model.mode.labelResId)
        )

        is MiniPlayerUiModel.Saved -> model.displayName
    }

@Composable
private fun miniPlayerSubtitle(model: MiniPlayerUiModel): String =
    when (model) {
        is MiniPlayerUiModel.Generated -> {
            val duration = formatDurationMillis(model.durationMs)
            if (model.mode == com.bag.audioandroid.ui.model.TransportModeOption.Flash &&
                model.flashVoicingStyle != null) {
                stringResource(
                    R.string.audio_mini_player_generated_flash_subtitle,
                    stringResource(model.flashVoicingStyle.labelResId),
                    duration
                )
            } else {
                stringResource(R.string.audio_mini_player_duration_only, duration)
            }
        }

        is MiniPlayerUiModel.Saved -> stringResource(
            if (model.modeWireName == "flash" && model.flashVoicingStyle != null) {
                R.string.audio_mini_player_generated_flash_subtitle
            } else {
                R.string.audio_mini_player_saved_subtitle
            },
            if (model.modeWireName == "flash" && model.flashVoicingStyle != null) {
                stringResource(model.flashVoicingStyle.labelResId)
            } else {
                stringResource(SavedAudioModeFilter.labelResIdForModeWireName(model.modeWireName))
            },
            formatDurationMillis(model.durationMs)
        )
    }
