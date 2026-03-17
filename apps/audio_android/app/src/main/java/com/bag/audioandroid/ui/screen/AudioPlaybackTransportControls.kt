package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PlaybackSequenceMode

@Composable
internal fun AudioPlaybackTransportControls(
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onOpenSavedAudioSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackSequenceCycleButton(
            playbackSequenceMode = playbackSequenceMode,
            onClick = { onPlaybackSequenceModeSelected(playbackSequenceMode.next()) }
        )
        TransportIconButton(
            onClick = onSkipToPreviousTrack,
            enabled = canSkipPrevious,
            contentDescription = stringResource(R.string.audio_action_previous_track),
            size = 42.dp
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                modifier = Modifier.size(40.dp),
                contentDescription = null
            )
        }
        TransportIconButton(
            onClick = onTogglePlayback,
            contentDescription = stringResource(
                if (isPlaying) {
                    R.string.audio_action_pause
                } else {
                    R.string.audio_action_play
                }
            ),
            size = 56.dp,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                modifier = Modifier.size(54.dp),
                contentDescription = null
            )
        }
        TransportIconButton(
            onClick = onSkipToNextTrack,
            enabled = canSkipNext,
            contentDescription = stringResource(R.string.audio_action_next_track),
            size = 42.dp
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                modifier = Modifier.size(40.dp),
                contentDescription = null
            )
        }
        UtilityIconButton(
            onClick = onOpenSavedAudioSheet,
            contentDescription = stringResource(R.string.audio_action_open_saved_audio_list)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun PlaybackSequenceCycleButton(
    playbackSequenceMode: PlaybackSequenceMode,
    onClick: () -> Unit,
) {
    val icon = when (playbackSequenceMode) {
        PlaybackSequenceMode.Normal -> Icons.Rounded.Repeat
        PlaybackSequenceMode.RepeatOne -> Icons.Rounded.RepeatOne
        PlaybackSequenceMode.RepeatList -> Icons.Rounded.Repeat
        PlaybackSequenceMode.Shuffle -> Icons.Rounded.Shuffle
    }
    val contentColor = if (playbackSequenceMode == PlaybackSequenceMode.Normal) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    val contentDescription = stringResource(playbackSequenceMode.labelResId)

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .semantics { this.contentDescription = contentDescription },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun TransportIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    contentColor: androidx.compose.ui.graphics.Color? = null,
    icon: @Composable () -> Unit
) {
    val resolvedContentColor = contentColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = resolvedContentColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
        )
    ) {
        icon()
    }
}

@Composable
private fun UtilityIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .semantics { this.contentDescription = contentDescription },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        icon()
    }
}
