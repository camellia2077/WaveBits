package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.SnackbarMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioSessionExportActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val savedAudioRepository: SavedAudioRepository,
    private val sampleRateHz: Int,
    private val refreshSavedAudioItems: () -> Unit
) {
    fun onExportAudio() {
        val current = uiState.value
        val session = current.currentSession
        if (session.generatedPcm.isEmpty()) {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_no_audio_for_mode))
            }
            return
        }
        val metadata = session.generatedAudioMetadata
        if (metadata == null) {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_audio_save_failed))
            }
            return
        }

        when (
            val result = savedAudioRepository.exportGeneratedAudio(
                mode = current.transportMode,
                inputText = session.inputText,
                pcm = session.generatedPcm,
                sampleRateHz = sampleRateHz,
                metadata = metadata
            )
        ) {
            is AudioExportResult.Success -> sessionStateStore.updateCurrentSession {
                it.copy(
                    statusText = UiText.Resource(
                        R.string.status_audio_saved,
                        listOf(result.displayName)
                    )
                )
            }.also {
                emitSnackbar(UiText.Resource(R.string.snackbar_audio_saved_to_library))
                refreshSavedAudioItems()
            }

            AudioExportResult.Failed -> sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_audio_save_failed))
            }.also {
                emitSnackbar(UiText.Resource(R.string.snackbar_audio_save_failed))
            }
        }
    }

    private fun emitSnackbar(message: UiText) {
        uiState.update { state ->
            state.copy(
                snackbarMessage = SnackbarMessage(
                    id = System.nanoTime(),
                    text = message
                )
            )
        }
    }
}
