package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

internal class AudioAndroidSessionActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    scope: CoroutineScope,
    audioCodecGateway: AudioCodecGateway,
    sampleInputTextProvider: SampleInputTextProvider,
    private val sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    savedAudioRepository: SavedAudioRepository,
    sampleRateHz: Int,
    frameSamples: Int,
    stopPlayback: () -> Unit,
    refreshSavedAudioItems: () -> Unit
) {
    private val editingActions = AudioSessionEditingActions(
        uiState = uiState,
        sessionStateStore = sessionStateStore,
        sampleInputTextProvider = sampleInputTextProvider,
        stopPlayback = stopPlayback,
        refreshSavedAudioItems = refreshSavedAudioItems
    )
    private val codecActions = AudioSessionCodecActions(
        uiState = uiState,
        scope = scope,
        audioCodecGateway = audioCodecGateway,
        sessionStateStore = sessionStateStore,
        uiTextMapper = uiTextMapper,
        playbackRuntimeGateway = playbackRuntimeGateway,
        sampleRateHz = sampleRateHz,
        frameSamples = frameSamples,
        stopPlayback = stopPlayback
    )
    private val exportActions = AudioSessionExportActions(
        uiState = uiState,
        sessionStateStore = sessionStateStore,
        savedAudioRepository = savedAudioRepository,
        sampleRateHz = sampleRateHz,
        refreshSavedAudioItems = refreshSavedAudioItems
    )

    fun onInputTextChange(value: String) {
        editingActions.onInputTextChange(value)
    }

    fun onRandomizeSampleInput() {
        editingActions.onRandomizeSampleInput()
    }

    fun onTransportModeSelected(mode: TransportModeOption) {
        editingActions.onTransportModeSelected(mode)
    }

    fun onEncode() {
        codecActions.onEncode()
    }

    fun onCancelEncode() {
        codecActions.onCancelEncode()
    }

    fun onDecode() {
        codecActions.onDecode()
    }

    fun onClear() {
        sessionStateStore.updateCurrentSession {
            it.copy(
                inputText = "",
                sampleInputId = null,
                statusText = if (it.generatedPcm.isEmpty()) {
                    UiText.Resource(R.string.status_ready_to_encode)
                } else {
                    it.statusText
                }
            )
        }
    }

    fun onClearResult() {
        when (val source = uiState.value.currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> sessionStateStore.updateCurrentSession {
                it.copy(
                    resultText = "",
                    statusText = UiText.Resource(R.string.status_result_cleared)
                )
            }

            is AudioPlaybackSource.Saved -> uiState.update { state ->
                val selected = state.selectedSavedAudio
                    ?.takeIf { it.item.itemId == source.itemId }
                    ?: return@update state
                state.copy(
                    selectedSavedAudio = selected.copy(decodedText = ""),
                    libraryStatusText = UiText.Resource(R.string.status_result_cleared)
                )
            }
        }
    }

    fun onExportAudio() {
        exportActions.onExportAudio()
    }

    fun onOpenSavedAudioSheet() {
        editingActions.onOpenSavedAudioSheet()
    }

    fun onCloseSavedAudioSheet() {
        editingActions.onCloseSavedAudioSheet()
    }
}
