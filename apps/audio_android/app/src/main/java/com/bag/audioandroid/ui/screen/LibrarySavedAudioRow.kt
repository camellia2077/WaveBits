package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.utilityActionIconButtonColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibrarySavedAudioRow(
    item: SavedAudioItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    folderName: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onExportToFile: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (isSelectionMode) null else onLongClick,
                ).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        "${stringResource(SavedAudioModeFilter.labelResIdForModeWireName(item.modeWireName))} • " +
                            formatDurationMillis(item.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatSavedAudioTime(item.savedAtEpochSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                folderName?.takeIf { it.isNotBlank() }?.let { resolvedFolderName ->
                    Text(
                        text = stringResource(R.string.library_folder_row_label, resolvedFolderName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (!isSelectionMode) {
                IconButton(
                    onClick = onExportToFile,
                    colors = utilityActionIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SaveAlt,
                        contentDescription = stringResource(R.string.library_action_export_to_file),
                    )
                }
                IconButton(
                    onClick = onShare,
                    colors = utilityActionIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.library_action_share),
                    )
                }
                IconButton(
                    onClick = onMove,
                    colors = utilityActionIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = stringResource(R.string.library_action_move),
                    )
                }
                IconButton(
                    onClick = onRename,
                    colors = utilityActionIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.library_action_rename),
                    )
                }
                IconButton(
                    onClick = onDelete,
                    colors = utilityActionIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.library_action_delete),
                    )
                }
            }
        }
    }
}
