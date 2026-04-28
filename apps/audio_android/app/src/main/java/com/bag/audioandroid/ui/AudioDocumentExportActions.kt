package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.AudioDocumentExportSource
import com.bag.audioandroid.ui.state.PendingAudioDocumentExportRequest
import com.bag.audioandroid.ui.state.SnackbarMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AudioDocumentExportActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val sessionStateStore: AudioSessionStateStore,
    private val savedAudioRepository: SavedAudioRepository,
    private val sampleRateHz: Int,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun onRequestGeneratedAudioExportToDocument() {
        val currentState = uiState.value
        val session = currentState.currentSession
        if (session.generatedPcm.isEmpty() && session.generatedPcmFilePath == null) {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_no_audio_for_mode))
            }
            return
        }
        val metadata = session.generatedAudioMetadata
        if (metadata == null) {
            emitSnackbar(UiText.Resource(R.string.snackbar_audio_export_to_file_failed))
            return
        }
        uiState.update { state ->
            state.copy(
                pendingDocumentExportRequest =
                    PendingAudioDocumentExportRequest(
                        id = System.nanoTime(),
                        suggestedFileName =
                            savedAudioRepository.suggestGeneratedAudioDisplayName(
                                mode = state.transportMode,
                                inputText = session.inputText,
                            ),
                        source = AudioDocumentExportSource.Generated(state.transportMode),
                    ),
            )
        }
    }

    fun onRequestSavedAudioExportToDocument(item: SavedAudioItem) {
        uiState.update { state ->
            state.copy(
                pendingDocumentExportRequest =
                    PendingAudioDocumentExportRequest(
                        id = System.nanoTime(),
                        suggestedFileName = item.displayName,
                        source = AudioDocumentExportSource.Saved(item.itemId),
                    ),
            )
        }
    }

    fun onDocumentExportPicked(uriString: String?) {
        val request = uiState.value.pendingDocumentExportRequest ?: return
        uiState.update { it.copy(pendingDocumentExportRequest = null) }
        if (uriString == null) {
            return
        }

        scope.launch {
            val success =
                withContext(workerDispatcher) {
                    when (val source = request.source) {
                        is AudioDocumentExportSource.Generated -> exportGeneratedAudioToDocument(source.mode, uriString)
                        is AudioDocumentExportSource.Saved ->
                            savedAudioRepository.exportSavedAudioToDocument(source.itemId, uriString)
                    }
                }
            emitSnackbar(
                UiText.Resource(
                    if (success) {
                        R.string.snackbar_audio_exported_to_file
                    } else {
                        R.string.snackbar_audio_export_to_file_failed
                    },
                ),
            )
        }
    }

    private fun exportGeneratedAudioToDocument(
        mode: com.bag.audioandroid.ui.model.TransportModeOption,
        destinationUriString: String,
    ): Boolean {
        val currentState = uiState.value
        val session = currentState.sessions.getValue(mode)
        val metadata = session.generatedAudioMetadata ?: return false
        if (session.generatedPcm.isEmpty() && session.generatedPcmFilePath == null) {
            return false
        }
        return savedAudioRepository.exportGeneratedAudioToDocument(
            mode = mode,
            inputText = session.inputText,
            pcm = session.generatedPcm,
            pcmFilePath = session.generatedPcmFilePath,
            sampleRateHz = sampleRateHz,
            metadata = metadata,
            destinationUriString = destinationUriString,
        )
    }

    private fun emitSnackbar(message: UiText) {
        uiState.update { state ->
            state.copy(
                snackbarMessage =
                    SnackbarMessage(
                        id = System.nanoTime(),
                        text = message,
                        durationMillis = EXPORT_SNACKBAR_DURATION_MILLIS,
                    ),
            )
        }
    }

    private companion object {
        const val EXPORT_SNACKBAR_DURATION_MILLIS = 1400L
    }
}
