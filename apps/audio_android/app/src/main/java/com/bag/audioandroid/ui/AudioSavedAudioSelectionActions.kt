package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.LibrarySelectionUiState
import com.bag.audioandroid.ui.state.SavedAudioPlaybackSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioSavedAudioSelectionActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val savedAudioRepository: SavedAudioRepository,
    private val stopPlayback: () -> Unit,
    private val setCurrentStatusText: (UiText) -> Unit,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) {
    fun onSavedAudioSelected(itemId: String) {
        stopPlayback()
        if (!prepareSavedAudioSelection(
                itemId,
                switchToAudioTab = false,
                clearLibrarySelection = true,
                closeSavedAudioSheet = false,
            )
        ) {
            setCurrentStatusText(UiText.Resource(R.string.status_saved_audio_load_failed))
            return
        }
        val savedAudio = uiState.value.selectedSavedAudio ?: return
        setCurrentStatusText(
            UiText.Resource(
                R.string.status_saved_audio_loaded,
                listOf(savedAudio.item.displayName),
            ),
        )
    }

    fun prepareSavedAudioSelection(
        itemId: String,
        switchToAudioTab: Boolean = false,
        clearLibrarySelection: Boolean = false,
        closeSavedAudioSheet: Boolean = false,
    ): Boolean {
        val previousSelection = uiState.value.selectedSavedAudio
        val savedAudio = savedAudioRepository.loadSavedAudio(itemId) ?: return false
        if (previousSelection?.item?.itemId != savedAudio.item.itemId) {
            generatedAudioCacheGateway.deleteCachedFile(previousSelection?.pcmFilePath)
        }
        uiState.update { state ->
            state.copy(
                selectedTab = if (switchToAudioTab) AppTab.Audio else state.selectedTab,
                showSavedAudioSheet = if (closeSavedAudioSheet || switchToAudioTab) false else state.showSavedAudioSheet,
                currentPlaybackSource = AudioPlaybackSource.Saved(savedAudio.item.itemId),
                selectedSavedAudio =
                    SavedAudioPlaybackSelection(
                        item = savedAudio.item,
                        pcm = savedAudio.pcm,
                        waveformPcm = savedAudio.waveformPcm,
                        pcmFilePath = savedAudio.pcmFilePath,
                        sampleRateHz = savedAudio.sampleRateHz,
                        metadata = savedAudio.metadata,
                        playback =
                            playbackRuntimeGateway.load(
                                savedAudio.metadata?.pcmSampleCount ?: savedAudio.pcm.size,
                                savedAudio.sampleRateHz,
                            ),
                        playbackSpeed =
                            state.selectedSavedAudio
                                ?.takeIf { it.item.itemId == savedAudio.item.itemId }
                                ?.playbackSpeed
                                ?: PlaybackSpeedOption.default.speed,
                    ),
                librarySelection =
                    if (clearLibrarySelection) {
                        LibrarySelectionUiState()
                    } else {
                        state.librarySelection
                    },
            )
        }
        return true
    }

    fun onShellSavedAudioSelected(itemId: String) {
        stopPlayback()
        if (!prepareSavedAudioSelection(itemId, closeSavedAudioSheet = true)) {
            uiState.update { it.copy(showSavedAudioSheet = false) }
            setCurrentStatusText(UiText.Resource(R.string.status_saved_audio_load_failed))
            return
        }
        val savedAudio = uiState.value.selectedSavedAudio ?: return
        setCurrentStatusText(
            UiText.Resource(
                R.string.status_saved_audio_loaded,
                listOf(savedAudio.item.displayName),
            ),
        )
    }

    fun onEnterLibrarySelection(itemId: String) {
        uiState.update { state ->
            if (state.savedAudioItems.none { it.itemId == itemId }) {
                return@update state
            }
            state.copy(
                librarySelection =
                    LibrarySelectionUiState(
                        isSelectionMode = true,
                        selectedItemIds = setOf(itemId),
                    ),
            )
        }
    }

    fun onToggleLibrarySelection(itemId: String) {
        uiState.update { state ->
            if (!state.librarySelection.isSelectionMode ||
                state.savedAudioItems.none { it.itemId == itemId }
            ) {
                return@update state
            }

            val updatedSelection =
                if (itemId in state.librarySelection.selectedItemIds) {
                    state.librarySelection.selectedItemIds - itemId
                } else {
                    state.librarySelection.selectedItemIds + itemId
                }
            state.copy(
                librarySelection =
                    if (updatedSelection.isEmpty()) {
                        LibrarySelectionUiState()
                    } else {
                        state.librarySelection.copy(selectedItemIds = updatedSelection)
                    },
            )
        }
    }

    fun onSelectAllLibraryItems(itemIds: Collection<String>? = null) {
        uiState.update { state ->
            val allItemIds =
                itemIds
                    ?.filter { candidateId -> state.savedAudioItems.any { it.itemId == candidateId } }
                    ?.toSet()
                    ?: state.savedAudioItems.map { it.itemId }.toSet()
            if (allItemIds.isEmpty()) {
                state
            } else {
                state.copy(
                    librarySelection =
                        LibrarySelectionUiState(
                            isSelectionMode = true,
                            selectedItemIds = allItemIds,
                        ),
                )
            }
        }
    }

    fun onClearLibrarySelection() {
        uiState.update { it.copy(librarySelection = LibrarySelectionUiState()) }
    }
}
