package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.LibrarySelectionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioSavedAudioMutationActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val savedAudioRepository: SavedAudioRepository,
    private val stopPlayback: () -> Unit,
    private val setCurrentStatusText: (UiText) -> Unit
) {
    fun onDeleteSelectedSavedAudio() {
        val selectedItemIds = uiState.value.librarySelection.selectedItemIds.toList()
        if (selectedItemIds.isEmpty()) {
            return
        }
        if (selectedItemIds.size == 1) {
            onDeleteSavedAudio(selectedItemIds.first())
            return
        }

        val deletedItemIds = selectedItemIds.filterTo(mutableSetOf()) { itemId ->
            savedAudioRepository.deleteSavedAudio(itemId)
        }
        if (deletedItemIds.isEmpty()) {
            uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_delete_failed))
            }
            return
        }

        stopPlaybackIfCurrentSavedAudio(deletedItemIds)
        refreshSavedAudioItems()
        uiState.update { state ->
            state.copy(
                libraryStatusText = if (deletedItemIds.size == selectedItemIds.size) {
                    UiText.Resource(
                        R.string.library_status_deleted_multiple,
                        listOf(deletedItemIds.size)
                    )
                } else {
                    UiText.Resource(
                        R.string.library_status_delete_partial,
                        listOf(deletedItemIds.size, selectedItemIds.size)
                    )
                }
            )
        }
    }

    fun onDeleteSavedAudio(itemId: String) {
        val wasDeleted = savedAudioRepository.deleteSavedAudio(itemId)
        if (!wasDeleted) {
            uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_delete_failed))
            }
            return
        }

        stopPlaybackIfCurrentSavedAudio(itemId)
        refreshSavedAudioItems()
        uiState.update {
            it.copy(libraryStatusText = UiText.Resource(R.string.library_status_deleted))
        }
    }

    fun onRenameSavedAudio(itemId: String, newBaseName: String) {
        when (val result = savedAudioRepository.renameSavedAudio(itemId, newBaseName)) {
            is SavedAudioRenameResult.Success -> {
                refreshSavedAudioItems()
                uiState.update { state ->
                    val selectedSavedAudio = state.selectedSavedAudio
                    val updatedSelection = if (selectedSavedAudio?.item?.itemId == itemId) {
                        selectedSavedAudio.copy(item = result.updatedItem)
                    } else {
                        selectedSavedAudio
                    }
                    state.copy(
                        selectedSavedAudio = updatedSelection,
                        libraryStatusText = UiText.Resource(
                            R.string.library_status_renamed,
                            listOf(result.updatedItem.displayName)
                        )
                    )
                }
            }

            SavedAudioRenameResult.DuplicateName -> uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_rename_duplicate))
            }

            SavedAudioRenameResult.Failed -> uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_rename_failed))
            }
        }
    }

    fun onImportAudio(uriString: String) {
        when (val result = savedAudioRepository.importAudio(uriString)) {
            is SavedAudioImportResult.Success -> {
                refreshSavedAudioItems()
                uiState.update {
                    it.copy(
                        libraryStatusText = UiText.Resource(
                            R.string.library_status_imported,
                            listOf(result.importedItem.displayName)
                        )
                    )
                }
            }

            SavedAudioImportResult.UnsupportedFormat -> uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_import_unsupported))
            }

            SavedAudioImportResult.Failed -> uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_import_failed))
            }
        }
    }

    fun onShareCurrentSavedAudio() {
        val currentSavedAudio = uiState.value.currentSavedAudioItem ?: return
        if (!savedAudioRepository.shareSavedAudio(currentSavedAudio)) {
            setCurrentStatusText(UiText.Resource(R.string.library_status_share_failed))
        }
    }

    fun onShareSavedAudio(item: SavedAudioItem) {
        if (!savedAudioRepository.shareSavedAudio(item)) {
            uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_share_failed))
            }
        }
    }

    fun refreshSavedAudioItems() {
        val savedAudioItems = savedAudioRepository.listSavedAudio()
        val savedAudioItemIds = savedAudioItems.map { it.itemId }.toSet()
        uiState.update { state ->
            val selectedItemIds = state.librarySelection.selectedItemIds.intersect(savedAudioItemIds)
            val currentPlaybackSource = state.currentPlaybackSource
            state.copy(
                savedAudioItems = savedAudioItems,
                selectedSavedAudio = state.selectedSavedAudio?.takeIf { it.item.itemId in savedAudioItemIds },
                currentPlaybackSource = if (currentPlaybackSource is AudioPlaybackSource.Saved &&
                    currentPlaybackSource.itemId !in savedAudioItemIds) {
                    AudioPlaybackSource.Generated(state.transportMode)
                } else {
                    currentPlaybackSource
                },
                librarySelection = if (selectedItemIds.isEmpty()) {
                    LibrarySelectionUiState()
                } else {
                    state.librarySelection.copy(selectedItemIds = selectedItemIds)
                }
            )
        }
    }

    private fun stopPlaybackIfCurrentSavedAudio(itemId: String) {
        stopPlaybackIfCurrentSavedAudio(setOf(itemId))
    }

    private fun stopPlaybackIfCurrentSavedAudio(itemIds: Set<String>) {
        val currentSource = uiState.value.currentPlaybackSource
        if (currentSource is AudioPlaybackSource.Saved && currentSource.itemId in itemIds) {
            stopPlayback()
        }
    }
}
