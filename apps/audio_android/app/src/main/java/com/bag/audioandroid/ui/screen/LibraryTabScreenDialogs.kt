package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem

@Composable
internal fun LibraryTabScreenDialogs(
    renameTarget: SavedAudioItem?,
    renameValue: String,
    deleteTarget: SavedAudioItem?,
    showBulkDeleteDialog: Boolean,
    showCreateFolderDialog: Boolean,
    createFolderValue: String,
    showRenameFolderDialog: Boolean,
    renameFolderValue: String,
    selectedFolderName: String?,
    showDeleteFolderDialog: Boolean,
    moveRequest: LibraryMoveRequest?,
    savedAudioFolders: List<com.bag.audioandroid.domain.SavedAudioFolder>,
    moveTargetFolderId: String?,
    isSelectionMode: Boolean,
    filteredSelectedCount: Int,
    onRenameValueChange: (String) -> Unit,
    onDismissRename: () -> Unit,
    onConfirmRename: (String, String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    onDismissBulkDelete: () -> Unit,
    onConfirmBulkDelete: () -> Unit,
    onCreateFolderValueChange: (String) -> Unit,
    onDismissCreateFolder: () -> Unit,
    onConfirmCreateFolder: () -> Unit,
    onRenameFolderValueChange: (String) -> Unit,
    onDismissRenameFolder: () -> Unit,
    onConfirmRenameFolder: () -> Unit,
    onDismissDeleteFolder: () -> Unit,
    onConfirmDeleteFolder: () -> Unit,
    onSelectMoveFolder: (String?) -> Unit,
    onDismissMove: () -> Unit,
    onConfirmMove: () -> Unit,
) {
    renameTarget?.let { item ->
        RenameSavedAudioDialog(
            currentDisplayName = item.displayName,
            initialBaseName = renameValue.ifBlank { item.displayName.removeSuffix(".wav") },
            onDismiss = onDismissRename,
            onValueChange = onRenameValueChange,
            onConfirm = { onConfirmRename(item.itemId, renameValue) },
        )
    }

    deleteTarget?.let { item ->
        DeleteSavedAudioDialog(
            displayName = item.displayName,
            onDismiss = onDismissDelete,
            onConfirm = { onConfirmDelete(item.itemId) },
        )
    }

    if (isSelectionMode && showBulkDeleteDialog) {
        DeleteSelectedSavedAudioDialog(
            selectedCount = filteredSelectedCount,
            onDismiss = onDismissBulkDelete,
            onConfirm = onConfirmBulkDelete,
        )
    }

    if (showCreateFolderDialog) {
        CreateSavedAudioFolderDialog(
            value = createFolderValue,
            onValueChange = onCreateFolderValueChange,
            onDismiss = onDismissCreateFolder,
            onConfirm = onConfirmCreateFolder,
        )
    }

    if (showRenameFolderDialog) {
        RenameSavedAudioFolderDialog(
            value = renameFolderValue,
            onValueChange = onRenameFolderValueChange,
            onDismiss = onDismissRenameFolder,
            onConfirm = onConfirmRenameFolder,
        )
    }

    if (showDeleteFolderDialog && !selectedFolderName.isNullOrBlank()) {
        DeleteSavedAudioFolderDialog(
            folderName = selectedFolderName,
            onDismiss = onDismissDeleteFolder,
            onConfirm = onConfirmDeleteFolder,
        )
    }

    moveRequest?.let { request ->
        MoveSavedAudioToFolderDialog(
            folders = savedAudioFolders,
            selectedFolderId = moveTargetFolderId,
            titleText =
                request.displayName?.let {
                    androidx.compose.ui.res.stringResource(R.string.library_move_single_message, it)
                } ?: androidx.compose.ui.res.stringResource(
                    R.string.library_move_multiple_message,
                    request.itemIds.size,
                ),
            onSelectFolder = onSelectMoveFolder,
            onDismiss = onDismissMove,
            onConfirm = onConfirmMove,
        )
    }
}
