package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioAndroidSessionActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    scope: CoroutineScope,
    audioCodecGateway: AudioCodecGateway,
    audioIoGateway: AudioIoGateway,
    sampleInputTextProvider: SampleInputTextProvider,
    private val sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    savedAudioRepository: SavedAudioRepository,
    sampleRateHz: Int,
    frameSamples: Int,
    stopPlayback: () -> Unit,
    refreshSavedAudioItems: () -> Unit,
    workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
    generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) {
    private val editingActions =
        AudioSessionEditingActions(
            uiState = uiState,
            sessionStateStore = sessionStateStore,
            sampleInputTextProvider = sampleInputTextProvider,
            stopPlayback = stopPlayback,
            refreshSavedAudioItems = refreshSavedAudioItems,
        )
    private val codecActions =
        AudioSessionCodecActions(
            uiState = uiState,
            scope = scope,
            audioCodecGateway = audioCodecGateway,
            audioIoGateway = audioIoGateway,
            sessionStateStore = sessionStateStore,
            uiTextMapper = uiTextMapper,
            playbackRuntimeGateway = playbackRuntimeGateway,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            stopPlayback = stopPlayback,
            workerDispatcher = workerDispatcher,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
        )
    private val exportActions =
        AudioSessionExportActions(
            uiState = uiState,
            scope = scope,
            sessionStateStore = sessionStateStore,
            savedAudioRepository = savedAudioRepository,
            sampleRateHz = sampleRateHz,
            refreshSavedAudioItems = refreshSavedAudioItems,
            workerDispatcher = workerDispatcher,
        )

    fun onInputTextChange(value: String) {
        editingActions.onInputTextChange(value)
    }

    fun onRandomizeSampleInput(length: SampleInputLengthOption) {
        editingActions.onRandomizeSampleInput(length)
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

    fun ensureCurrentPlaybackDecodedForLyrics() {
        codecActions.ensureCurrentPlaybackDecodedForLyrics()
    }

    fun onClear() {
        sessionStateStore.updateCurrentSession {
            it.copy(
                inputText = "",
                sampleInputId = null,
                sampleShuffleState = null,
                statusText =
                    if (it.generatedPcm.isEmpty()) {
                        UiText.Resource(R.string.status_ready_to_encode)
                    } else {
                        it.statusText
                    },
            )
        }
    }

    fun onClearResult() {
        when (val source = uiState.value.currentPlaybackSource) {
            is AudioPlaybackSource.Generated ->
                sessionStateStore.updateCurrentSession {
                    it.copy(
                        decodedPayload = DecodedPayloadViewData.Empty,
                        statusText = UiText.Resource(R.string.status_result_cleared),
                    )
                }

            is AudioPlaybackSource.Saved ->
                uiState.update { state ->
                    val selected =
                        state.selectedSavedAudio
                            ?.takeIf { it.item.itemId == source.itemId }
                            ?: return@update state
                    state.copy(
                        selectedSavedAudio = selected.copy(decodedPayload = DecodedPayloadViewData.Empty),
                        libraryStatusText = UiText.Resource(R.string.status_result_cleared),
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
