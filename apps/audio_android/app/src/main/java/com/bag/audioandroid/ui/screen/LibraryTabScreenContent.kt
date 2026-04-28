package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.model.asString
import com.bag.audioandroid.ui.state.LibrarySelectionUiState
import com.bag.audioandroid.ui.utilityActionIconButtonColors

@Composable
internal fun LibraryTabScreenContent(
    savedAudioItems: List<SavedAudioItem>,
    librarySelection: LibrarySelectionUiState,
    statusText: UiText,
    contentPadding: PaddingValues,
    screenState: LibraryTabScreenState,
    onImportAudio: () -> Unit,
    onSelectSavedAudio: (String) -> Unit,
    onEnterLibrarySelection: (String) -> Unit,
    onToggleLibrarySelection: (String) -> Unit,
    onSelectAllLibraryItems: (Collection<String>) -> Unit,
    onClearLibrarySelection: () -> Unit,
    onShareSavedAudio: (SavedAudioItem) -> Unit,
    onExportSavedAudioToDocument: (SavedAudioItem) -> Unit,
) {
    if (librarySelection.isSelectionMode) {
        LibrarySelectionActionsRow(
            selectedCount = screenState.filteredSelectedCount,
            canSelectAll = screenState.filteredSelectedCount < screenState.filteredItems.size,
            onSelectAll = { onSelectAllLibraryItems(screenState.filteredItemIds) },
            onMoveSelected = screenState.onMoveSelectedStarted,
            onDeleteSelected = screenState.onBulkDeleteRequested,
            onClearSelection = onClearLibrarySelection,
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onImportAudio,
                colors = utilityActionIconButtonColors(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.UploadFile,
                    contentDescription = stringResource(R.string.library_action_import),
                )
            }
            Row {
                IconButton(
                    onClick = screenState.onCreateFolderStarted,
                    colors = utilityActionIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = stringResource(R.string.library_action_new_folder),
                    )
                }
                IconButton(
                    onClick = screenState.onRenameFolderStarted,
                    enabled = screenState.selectedCustomFolder != null,
                    colors = utilityActionIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DriveFileRenameOutline,
                        contentDescription = stringResource(R.string.library_action_rename_folder),
                    )
                }
                IconButton(
                    onClick = screenState.onDeleteFolderStarted,
                    enabled = screenState.selectedCustomFolder != null,
                    colors = utilityActionIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.library_action_delete_folder),
                    )
                }
            }
        }
    }

    LibraryFolderFilterBar(
        folders = screenState.savedAudioFolders,
        selectedFilter = screenState.selectedFolderFilter,
        onFilterSelected = { filter ->
            screenState.onFolderFilterSelected(filter)
            if (librarySelection.isSelectionMode) {
                onClearLibrarySelection()
            }
        },
    )

    SavedAudioModeFilterBar(
        selectedFilter = screenState.selectedFilter,
        onFilterSelected = { filter ->
            screenState.onFilterSelected(filter)
            if (librarySelection.isSelectionMode) {
                onClearLibrarySelection()
            }
        },
    )

    val resolvedStatus = statusText.asString()
    if (resolvedStatus.isNotBlank()) {
        Text(
            text = resolvedStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (savedAudioItems.isEmpty()) {
        Text(
            text = stringResource(R.string.library_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (screenState.filteredItems.isEmpty()) {
        Text(
            text = stringResource(R.string.saved_audio_filter_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            items(screenState.filteredItems, key = { it.itemId }) { item ->
                LibrarySavedAudioRow(
                    item = item,
                    isSelectionMode = librarySelection.isSelectionMode,
                    isSelected = item.itemId in librarySelection.selectedItemIds,
                    folderName =
                        screenState.savedAudioFolderAssignments[item.itemId]
                            ?.let(screenState.displayFolderNames::get),
                    onClick = {
                        if (librarySelection.isSelectionMode) {
                            onToggleLibrarySelection(item.itemId)
                        } else {
                            onSelectSavedAudio(item.itemId)
                        }
                    },
                    onLongClick = { onEnterLibrarySelection(item.itemId) },
                    onExportToFile = { onExportSavedAudioToDocument(item) },
                    onShare = { onShareSavedAudio(item) },
                    onMove = { screenState.onMoveStarted(item) },
                    onRename = { screenState.onRenameStarted(item) },
                    onDelete = { screenState.onDeleteStarted(item) },
                )
            }
        }
    }
}
