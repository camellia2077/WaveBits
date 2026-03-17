package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.model.asString
import com.bag.audioandroid.ui.state.LibrarySelectionUiState

@Composable
fun LibraryTabScreen(
    savedAudioItems: List<SavedAudioItem>,
    librarySelection: LibrarySelectionUiState,
    statusText: UiText,
    onImportAudio: () -> Unit,
    onSelectSavedAudio: (String) -> Unit,
    onEnterLibrarySelection: (String) -> Unit,
    onToggleLibrarySelection: (String) -> Unit,
    onSelectAllLibraryItems: (Collection<String>) -> Unit,
    onDeleteSelectedSavedAudio: () -> Unit,
    onClearLibrarySelection: () -> Unit,
    onDeleteSavedAudio: (String) -> Unit,
    onRenameSavedAudio: (String, String) -> Unit,
    onShareSavedAudio: (SavedAudioItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var renameTarget by remember { mutableStateOf<SavedAudioItem?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedAudioItem?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    var showBulkDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var selectedFilterName by rememberSaveable { mutableStateOf(SavedAudioModeFilter.All.name) }
    val selectedFilter = SavedAudioModeFilter.valueOf(selectedFilterName)
    val filteredItems = remember(savedAudioItems, selectedFilter) {
        savedAudioItems.filter(selectedFilter::matches)
    }
    val filteredItemIds = remember(filteredItems) {
        filteredItems.map { it.itemId }
    }
    val filteredSelectedCount = librarySelection.selectedItemIds.intersect(filteredItemIds.toSet()).size

    renameTarget?.let { item ->
        RenameSavedAudioDialog(
            currentDisplayName = item.displayName,
            initialBaseName = renameValue.ifBlank { item.displayName.removeSuffix(".wav") },
            onDismiss = {
                renameTarget = null
                renameValue = ""
            },
            onValueChange = { renameValue = it },
            onConfirm = {
                onRenameSavedAudio(item.itemId, renameValue)
                renameTarget = null
                renameValue = ""
            }
        )
    }

    deleteTarget?.let { item ->
        DeleteSavedAudioDialog(
            displayName = item.displayName,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeleteSavedAudio(item.itemId)
                deleteTarget = null
            }
        )
    }

    if (librarySelection.isSelectionMode && showBulkDeleteDialog) {
        DeleteSelectedSavedAudioDialog(
            selectedCount = filteredSelectedCount,
            onDismiss = { showBulkDeleteDialog = false },
            onConfirm = {
                onDeleteSelectedSavedAudio()
                showBulkDeleteDialog = false
            }
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (librarySelection.isSelectionMode) {
            LibrarySelectionActionsRow(
                selectedCount = filteredSelectedCount,
                canSelectAll = filteredSelectedCount < filteredItems.size,
                onSelectAll = { onSelectAllLibraryItems(filteredItemIds) },
                onDeleteSelected = { showBulkDeleteDialog = true },
                onClearSelection = onClearLibrarySelection
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onImportAudio) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = stringResource(R.string.library_action_import)
                    )
                }
            }
        }
        SavedAudioModeFilterBar(
            selectedFilter = selectedFilter,
            onFilterSelected = { filter ->
                selectedFilterName = filter.name
                if (librarySelection.isSelectionMode) {
                    onClearLibrarySelection()
                }
            }
        )

        val resolvedStatus = statusText.asString()
        if (resolvedStatus.isNotBlank()) {
            Text(
                text = resolvedStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (savedAudioItems.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (filteredItems.isEmpty()) {
            Text(
                text = stringResource(R.string.saved_audio_filter_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.itemId }) { item ->
                    LibrarySavedAudioRow(
                        item = item,
                        isSelectionMode = librarySelection.isSelectionMode,
                        isSelected = item.itemId in librarySelection.selectedItemIds,
                        onClick = {
                            if (librarySelection.isSelectionMode) {
                                onToggleLibrarySelection(item.itemId)
                            } else {
                                onSelectSavedAudio(item.itemId)
                            }
                        },
                        onLongClick = { onEnterLibrarySelection(item.itemId) },
                        onShare = { onShareSavedAudio(item) },
                        onRename = {
                            renameTarget = item
                            renameValue = item.displayName.removeSuffix(".wav")
                        },
                        onDelete = { deleteTarget = item }
                    )
                }
            }
        }
    }
}
