package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.bag.audioandroid.R

@Composable
internal fun LibrarySelectionActionsRow(
    selectedCount: Int,
    canSelectAll: Boolean,
    onSelectAll: () -> Unit,
    onMoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.library_selection_count, selectedCount),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onSelectAll,
            enabled = canSelectAll,
        ) {
            Icon(
                imageVector = Icons.Rounded.DoneAll,
                contentDescription = stringResource(R.string.library_action_select_all),
            )
        }
        IconButton(
            onClick = onMoveSelected,
            enabled = selectedCount > 0,
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = stringResource(R.string.library_action_move_selected),
            )
        }
        IconButton(
            onClick = onDeleteSelected,
            enabled = selectedCount > 0,
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.library_action_delete_selected),
            )
        }
        IconButton(onClick = onClearSelection) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.library_action_clear_selection),
            )
        }
    }
}
