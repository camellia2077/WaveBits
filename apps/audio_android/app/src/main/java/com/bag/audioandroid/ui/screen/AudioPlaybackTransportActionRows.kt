package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.PlayerChromeColors
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import kotlin.math.round

@Composable
internal fun AudioPlaybackPrimaryControlsRow(
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onOpenSavedAudioSheet: () -> Unit,
    colors: PlayerChromeColors,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackSequenceCycleButton(
            playbackSequenceMode = playbackSequenceMode,
            onClick = { onPlaybackSequenceModeSelected(playbackSequenceMode.next()) },
            activeColor = colors.accent,
            inactiveColor = colors.mutedAction,
        )
        TransportIconButton(
            onClick = onSkipToPreviousTrack,
            enabled = canSkipPrevious,
            contentDescription = stringResource(R.string.audio_action_previous_track),
            size = 42.dp,
            contentColor = colors.neutralAction,
            disabledContentColor = colors.disabledAction,
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                modifier = Modifier.size(40.dp),
                contentDescription = null,
            )
        }
        TransportIconButton(
            onClick = onTogglePlayback,
            contentDescription =
                stringResource(
                    if (isPlaying) {
                        R.string.audio_action_pause
                    } else {
                        R.string.audio_action_play
                    },
                ),
            size = 56.dp,
            contentColor = colors.accent,
            disabledContentColor = colors.disabledAction,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                modifier = Modifier.size(54.dp),
                contentDescription = null,
            )
        }
        TransportIconButton(
            onClick = onSkipToNextTrack,
            enabled = canSkipNext,
            contentDescription = stringResource(R.string.audio_action_next_track),
            size = 42.dp,
            contentColor = colors.neutralAction,
            disabledContentColor = colors.disabledAction,
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                modifier = Modifier.size(40.dp),
                contentDescription = null,
            )
        }
        UtilityIconButton(
            onClick = onOpenSavedAudioSheet,
            contentDescription = stringResource(R.string.audio_action_open_saved_audio_list),
            contentColor = colors.neutralAction,
            size = 46.dp,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
internal fun AudioPlaybackSecondaryActionsRow(
    playbackSpeed: Float,
    showSpeedAdjustment: Boolean,
    onOpenAudioInfo: () -> Unit,
    onCyclePlaybackSpeed: () -> Unit,
    onToggleSpeedAdjustment: () -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onExportGeneratedAudio: () -> Unit,
    onExportGeneratedAudioToDocument: () -> Unit,
    canExportGeneratedAudio: Boolean,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UtilityIconButton(
                onClick = onOpenAudioInfo,
                contentDescription = stringResource(R.string.audio_action_open_audio_info),
                contentColor = contentColor,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
            }
            PlaybackSpeedControl(
                playbackSpeed = playbackSpeed,
                onCyclePlaybackSpeed = onCyclePlaybackSpeed,
                onToggleSpeedAdjustment = onToggleSpeedAdjustment,
                contentColor = contentColor,
            )
            if (canExportGeneratedAudio) {
                UtilityIconButton(
                    onClick = onExportGeneratedAudioToDocument,
                    contentDescription = stringResource(R.string.audio_action_export_to_file),
                    contentColor = contentColor,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SaveAlt,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                    )
                }
                UtilityIconButton(
                    onClick = onExportGeneratedAudio,
                    contentDescription = stringResource(R.string.audio_action_export),
                    contentColor = contentColor,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else {
                SpacerUtilitySlot()
            }
        }
        if (showSpeedAdjustment) {
            PlaybackSpeedAdjustmentRow(
                playbackSpeed = playbackSpeed,
                onPlaybackSpeedSelected = onPlaybackSpeedSelected,
                contentColor = contentColor,
            )
        }
    }
}

@Composable
private fun PlaybackSequenceCycleButton(
    playbackSequenceMode: PlaybackSequenceMode,
    onClick: () -> Unit,
    activeColor: androidx.compose.ui.graphics.Color,
    inactiveColor: androidx.compose.ui.graphics.Color,
) {
    val icon =
        when (playbackSequenceMode) {
            PlaybackSequenceMode.Normal -> Icons.Rounded.Repeat
            PlaybackSequenceMode.RepeatOne -> Icons.Rounded.RepeatOne
            PlaybackSequenceMode.RepeatList -> Icons.Rounded.Repeat
            PlaybackSequenceMode.Shuffle -> Icons.Rounded.Shuffle
        }
    val contentColor =
        if (playbackSequenceMode == PlaybackSequenceMode.Normal) {
            inactiveColor
        } else {
            activeColor
        }
    val contentDescription = stringResource(playbackSequenceMode.labelResId)

    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(46.dp)
                .semantics { this.contentDescription = contentDescription },
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = contentColor,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(38.dp),
        )
    }
}

@Composable
private fun TransportIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 42.dp,
    contentColor: androidx.compose.ui.graphics.Color? = null,
    disabledContentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
    icon: @Composable () -> Unit,
) {
    val resolvedContentColor = contentColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .size(size)
                .semantics { this.contentDescription = contentDescription },
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = resolvedContentColor,
                disabledContentColor = disabledContentColor,
            ),
    ) {
        icon()
    }
}

@Composable
private fun UtilityIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(size)
                .semantics { this.contentDescription = contentDescription },
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = contentColor,
            ),
    ) {
        icon()
    }
}

@Composable
private fun UtilityTextButton(
    onClick: () -> Unit,
    contentDescription: String,
    contentColor: androidx.compose.ui.graphics.Color,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(size)
                .semantics { this.contentDescription = contentDescription },
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = contentColor,
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

@Composable
private fun PlaybackSpeedControl(
    playbackSpeed: Float,
    onCyclePlaybackSpeed: () -> Unit,
    onToggleSpeedAdjustment: () -> Unit,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UtilityTextButton(
            onClick = onCyclePlaybackSpeed,
            contentDescription = stringResource(R.string.audio_action_cycle_playback_speed, PlaybackSpeedOption.format(playbackSpeed)),
            contentColor = contentColor,
            label = PlaybackSpeedOption.labelFor(playbackSpeed),
            size = 64.dp,
        )
        UtilityIconButton(
            onClick = onToggleSpeedAdjustment,
            contentDescription = stringResource(R.string.audio_action_toggle_playback_speed_slider),
            contentColor = contentColor,
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun PlaybackSpeedAdjustmentRow(
    playbackSpeed: Float,
    onPlaybackSpeedSelected: (Float) -> Unit,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.audio_action_cycle_playback_speed, PlaybackSpeedOption.format(playbackSpeed)),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
        Slider(
            value = playbackSpeed.coerceIn(PlaybackSpeedMin, PlaybackSpeedMax),
            onValueChange = { selectedSpeed ->
                onPlaybackSpeedSelected(snapPlaybackSpeed(selectedSpeed))
            },
            valueRange = PlaybackSpeedMin..PlaybackSpeedMax,
            steps = PlaybackSpeedSliderSteps,
        )
    }
}

@Composable
private fun SpacerUtilitySlot() {
    Row(
        modifier = Modifier.size(36.dp),
    ) {
        // Keep the speed control centered even when export is unavailable.
    }
}

private fun snapPlaybackSpeed(speed: Float): Float =
    (round((speed - PlaybackSpeedMin) / PlaybackSpeedStep) * PlaybackSpeedStep + PlaybackSpeedMin)
        .coerceIn(PlaybackSpeedMin, PlaybackSpeedMax)

private const val PlaybackSpeedMin = 0.1f
private const val PlaybackSpeedMax = 4.0f
private const val PlaybackSpeedStep = 0.1f
private const val PlaybackSpeedSliderSteps = 38
