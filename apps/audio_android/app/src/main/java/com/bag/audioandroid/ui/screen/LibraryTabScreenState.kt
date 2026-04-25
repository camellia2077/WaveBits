package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.bag.audioandroid.domain.SavedAudioFolder
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.LibraryFolderFilter
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.state.LibrarySelectionUiState

@Composable
internal fun rememberLibraryTabScreenState(
    savedAudioItems: List<SavedAudioItem>,
    savedAudioFolders: List<SavedAudioFolder>,
    savedAudioFolderAssignments: Map<String, String>,
    librarySelection: LibrarySelectionUiState,
): LibraryTabScreenState {
    var renameTarget by remember { mutableStateOf<SavedAudioItem?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedAudioItem?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    var showBulkDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }
    var createFolderValue by rememberSaveable { mutableStateOf("") }
    var showRenameFolderDialog by rememberSaveable { mutableStateOf(false) }
    var renameFolderValue by rememberSaveable { mutableStateOf("") }
    var showDeleteFolderDialog by rememberSaveable { mutableStateOf(false) }
    var moveRequest by remember { mutableStateOf<LibraryMoveRequest?>(null) }
    var moveTargetFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFilterName by rememberSaveable { mutableStateOf(SavedAudioModeFilter.All.name) }
    var selectedFolderFilterKey by rememberSaveable { mutableStateOf(LibraryFolderFilter.All.key) }
    val selectedFilter = remember(selectedFilterName) { SavedAudioModeFilter.valueOf(selectedFilterName) }
    val selectedFolderFilter =
        remember(selectedFolderFilterKey, savedAudioFolders) {
            LibraryFolderFilter.fromKey(selectedFolderFilterKey, savedAudioFolders)
        }
    val selectedCustomFolder =
        remember(selectedFolderFilter, savedAudioFolders) {
            val folderId = (selectedFolderFilter as? LibraryFolderFilter.Folder)?.folderId ?: return@remember null
            savedAudioFolders.firstOrNull { it.folderId == folderId }
        }
    val filteredItems =
        remember(savedAudioItems, selectedFilter, selectedFolderFilter, savedAudioFolderAssignments) {
            savedAudioItems.filter { item ->
                selectedFilter.matches(item) &&
                    selectedFolderFilter.matches(item.itemId, savedAudioFolderAssignments)
            }
        }
    val filteredItemIds =
        remember(filteredItems) {
            filteredItems.map { it.itemId }
        }
    val filteredSelectedItemIds =
        remember(librarySelection.selectedItemIds, filteredItemIds) {
            filteredItemIds.filter { it in librarySelection.selectedItemIds }
        }
    val filteredSelectedCount =
        remember(filteredSelectedItemIds) {
            filteredSelectedItemIds.size
        }
    val displayFolderNames =
        remember(savedAudioFolders) {
            savedAudioFolders.associateBy(SavedAudioFolder::folderId, SavedAudioFolder::name)
        }

    return remember(
        renameTarget,
        deleteTarget,
        renameValue,
        showBulkDeleteDialog,
        showCreateFolderDialog,
        createFolderValue,
        showRenameFolderDialog,
        renameFolderValue,
        showDeleteFolderDialog,
        moveRequest,
        moveTargetFolderId,
        selectedFilter,
        selectedFolderFilter,
        selectedCustomFolder,
        filteredItems,
        filteredItemIds,
        filteredSelectedItemIds,
        filteredSelectedCount,
        displayFolderNames,
        savedAudioFolders,
        savedAudioFolderAssignments,
    ) {
        LibraryTabScreenState(
            renameTarget = renameTarget,
            deleteTarget = deleteTarget,
            renameValue = renameValue,
            showBulkDeleteDialog = showBulkDeleteDialog,
            showCreateFolderDialog = showCreateFolderDialog,
            createFolderValue = createFolderValue,
            showRenameFolderDialog = showRenameFolderDialog,
            renameFolderValue = renameFolderValue,
            showDeleteFolderDialog = showDeleteFolderDialog,
            moveRequest = moveRequest,
            moveTargetFolderId = moveTargetFolderId,
            selectedFilter = selectedFilter,
            selectedFolderFilter = selectedFolderFilter,
            selectedCustomFolder = selectedCustomFolder,
            savedAudioFolders = savedAudioFolders,
            savedAudioFolderAssignments = savedAudioFolderAssignments,
            displayFolderNames = displayFolderNames,
            filteredItems = filteredItems,
            filteredItemIds = filteredItemIds,
            filteredSelectedItemIds = filteredSelectedItemIds,
            filteredSelectedCount = filteredSelectedCount,
            onRenameStarted = { item ->
                renameTarget = item
                renameValue = item.displayName.removeSuffix(".wav")
            },
            onRenameValueChange = { renameValue = it },
            onDismissRename = {
                renameTarget = null
                renameValue = ""
            },
            onRenameCompleted = {
                renameTarget = null
                renameValue = ""
            },
            onDeleteStarted = { deleteTarget = it },
            onDismissDelete = { deleteTarget = null },
            onDeleteCompleted = { deleteTarget = null },
            onBulkDeleteRequested = { showBulkDeleteDialog = true },
            onDismissBulkDelete = { showBulkDeleteDialog = false },
            onBulkDeleteCompleted = { showBulkDeleteDialog = false },
            onCreateFolderStarted = {
                showCreateFolderDialog = true
                createFolderValue = ""
            },
            onCreateFolderValueChange = { createFolderValue = it },
            onDismissCreateFolder = {
                showCreateFolderDialog = false
                createFolderValue = ""
            },
            onCreateFolderCompleted = {
                showCreateFolderDialog = false
                createFolderValue = ""
            },
            onRenameFolderStarted = {
                selectedCustomFolder?.let { folder ->
                    showRenameFolderDialog = true
                    renameFolderValue = folder.name
                }
            },
            onRenameFolderValueChange = { renameFolderValue = it },
            onDismissRenameFolder = {
                showRenameFolderDialog = false
                renameFolderValue = ""
            },
            onRenameFolderCompleted = {
                showRenameFolderDialog = false
                renameFolderValue = ""
            },
            onDeleteFolderStarted = {
                if (selectedCustomFolder != null) {
                    showDeleteFolderDialog = true
                }
            },
            onDismissDeleteFolder = { showDeleteFolderDialog = false },
            onDeleteFolderCompleted = {
                showDeleteFolderDialog = false
                selectedFolderFilterKey = LibraryFolderFilter.All.key
            },
            onMoveStarted = { item ->
                moveTargetFolderId = savedAudioFolderAssignments[item.itemId]
                moveRequest =
                    LibraryMoveRequest(
                        itemIds = setOf(item.itemId),
                        displayName = item.displayName,
                    )
            },
            onMoveSelectedStarted = {
                if (filteredSelectedItemIds.isNotEmpty()) {
                    moveTargetFolderId = null
                    moveRequest =
                        LibraryMoveRequest(
                            itemIds = filteredSelectedItemIds.toSet(),
                            displayName = null,
                        )
                }
            },
            onMoveTargetFolderSelected = { moveTargetFolderId = it },
            onDismissMove = {
                moveRequest = null
                moveTargetFolderId = null
            },
            onMoveCompleted = {
                moveRequest = null
                moveTargetFolderId = null
            },
            onFilterSelected = { filter ->
                selectedFilterName = filter.name
            },
            onFolderFilterSelected = { filter ->
                selectedFolderFilterKey = filter.key
            },
        )
    }
}

@Immutable
internal data class LibraryMoveRequest(
    val itemIds: Set<String>,
    val displayName: String?,
)

@Immutable
internal data class LibraryTabScreenState(
    val renameTarget: SavedAudioItem?,
    val deleteTarget: SavedAudioItem?,
    val renameValue: String,
    val showBulkDeleteDialog: Boolean,
    val showCreateFolderDialog: Boolean,
    val createFolderValue: String,
    val showRenameFolderDialog: Boolean,
    val renameFolderValue: String,
    val showDeleteFolderDialog: Boolean,
    val moveRequest: LibraryMoveRequest?,
    val moveTargetFolderId: String?,
    val selectedFilter: SavedAudioModeFilter,
    val selectedFolderFilter: LibraryFolderFilter,
    val selectedCustomFolder: SavedAudioFolder?,
    val savedAudioFolders: List<SavedAudioFolder>,
    val savedAudioFolderAssignments: Map<String, String>,
    val displayFolderNames: Map<String, String>,
    val filteredItems: List<SavedAudioItem>,
    val filteredItemIds: List<String>,
    val filteredSelectedItemIds: List<String>,
    val filteredSelectedCount: Int,
    val onRenameStarted: (SavedAudioItem) -> Unit,
    val onRenameValueChange: (String) -> Unit,
    val onDismissRename: () -> Unit,
    val onRenameCompleted: () -> Unit,
    val onDeleteStarted: (SavedAudioItem) -> Unit,
    val onDismissDelete: () -> Unit,
    val onDeleteCompleted: () -> Unit,
    val onBulkDeleteRequested: () -> Unit,
    val onDismissBulkDelete: () -> Unit,
    val onBulkDeleteCompleted: () -> Unit,
    val onCreateFolderStarted: () -> Unit,
    val onCreateFolderValueChange: (String) -> Unit,
    val onDismissCreateFolder: () -> Unit,
    val onCreateFolderCompleted: () -> Unit,
    val onRenameFolderStarted: () -> Unit,
    val onRenameFolderValueChange: (String) -> Unit,
    val onDismissRenameFolder: () -> Unit,
    val onRenameFolderCompleted: () -> Unit,
    val onDeleteFolderStarted: () -> Unit,
    val onDismissDeleteFolder: () -> Unit,
    val onDeleteFolderCompleted: () -> Unit,
    val onMoveStarted: (SavedAudioItem) -> Unit,
    val onMoveSelectedStarted: () -> Unit,
    val onMoveTargetFolderSelected: (String?) -> Unit,
    val onDismissMove: () -> Unit,
    val onMoveCompleted: () -> Unit,
    val onFilterSelected: (SavedAudioModeFilter) -> Unit,
    val onFolderFilterSelected: (LibraryFolderFilter) -> Unit,
)
