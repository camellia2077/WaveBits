package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.flow.MutableStateFlow

internal class AudioAndroidLibraryActions(
    uiState: MutableStateFlow<AudioAppUiState>,
    sessionStateStore: AudioSessionStateStore,
    audioCodecGateway: AudioCodecGateway,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    savedAudioRepository: SavedAudioRepository,
    stopPlayback: () -> Unit,
    generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) {
    private val setCurrentStatusText: (UiText) -> Unit = { statusText ->
        sessionStateStore.updateCurrentSession {
            it.copy(statusText = statusText)
        }
    }
    private val selectionActions =
        AudioSavedAudioSelectionActions(
            uiState = uiState,
            audioCodecGateway = audioCodecGateway,
            playbackRuntimeGateway = playbackRuntimeGateway,
            savedAudioRepository = savedAudioRepository,
            stopPlayback = stopPlayback,
            setCurrentStatusText = setCurrentStatusText,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
        )
    private val mutationActions =
        AudioSavedAudioMutationActions(
            uiState = uiState,
            savedAudioRepository = savedAudioRepository,
            stopPlayback = stopPlayback,
            setCurrentStatusText = setCurrentStatusText,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
        )

    fun onSavedAudioSelected(itemId: String) {
        selectionActions.onSavedAudioSelected(itemId)
    }

    fun onShellSavedAudioSelected(itemId: String) {
        selectionActions.onShellSavedAudioSelected(itemId)
    }

    fun prepareSavedAudioSelection(
        itemId: String,
        switchToAudioTab: Boolean = false,
        clearLibrarySelection: Boolean = false,
    ): Boolean =
        selectionActions.prepareSavedAudioSelection(
            itemId = itemId,
            switchToAudioTab = switchToAudioTab,
            clearLibrarySelection = clearLibrarySelection,
        )

    fun onEnterLibrarySelection(itemId: String) {
        selectionActions.onEnterLibrarySelection(itemId)
    }

    fun onToggleLibrarySelection(itemId: String) {
        selectionActions.onToggleLibrarySelection(itemId)
    }

    fun onSelectAllLibraryItems(itemIds: Collection<String>? = null) {
        selectionActions.onSelectAllLibraryItems(itemIds)
    }

    fun onClearLibrarySelection() {
        selectionActions.onClearLibrarySelection()
    }

    fun onDeleteSelectedSavedAudio() {
        mutationActions.onDeleteSelectedSavedAudio()
    }

    fun onDeleteSavedAudio(itemId: String) {
        mutationActions.onDeleteSavedAudio(itemId)
    }

    fun onRenameSavedAudio(
        itemId: String,
        newBaseName: String,
    ) {
        mutationActions.onRenameSavedAudio(itemId, newBaseName)
    }

    fun onImportAudio(uriString: String) {
        mutationActions.onImportAudio(uriString)
    }

    fun onShareCurrentSavedAudio() {
        mutationActions.onShareCurrentSavedAudio()
    }

    fun onShareSavedAudio(item: SavedAudioItem) {
        mutationActions.onShareSavedAudio(item)
    }

    fun onCreateSavedAudioFolder(name: String) {
        mutationActions.onCreateSavedAudioFolder(name)
    }

    fun onRenameSavedAudioFolder(
        folderId: String,
        name: String,
    ) {
        mutationActions.onRenameSavedAudioFolder(folderId, name)
    }

    fun onDeleteSavedAudioFolder(folderId: String) {
        mutationActions.onDeleteSavedAudioFolder(folderId)
    }

    fun onMoveSavedAudioToFolder(
        itemIds: Collection<String>,
        folderId: String?,
    ) {
        mutationActions.onMoveSavedAudioToFolder(itemIds, folderId)
    }

    fun refreshSavedAudioItems() {
        mutationActions.refreshSavedAudioItems()
    }
}
