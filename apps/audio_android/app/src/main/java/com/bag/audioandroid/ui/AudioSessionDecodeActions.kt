package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.SavedAudioPlaybackSelection
import kotlinx.coroutines.flow.update
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AudioSessionDecodeActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val audioCodecGateway: AudioCodecGateway,
    private val sessionStateStore: AudioSessionStateStore,
    private val uiTextMapper: BagUiTextMapper,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val workerDispatcher: CoroutineDispatcher
) {
    fun onDecode() {
        val current = uiState.value
        if (current.currentSession.isCodecBusy) {
            return
        }
        when (val source = current.currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> onDecodeGenerated(current, source.mode)
            is AudioPlaybackSource.Saved -> onDecodeSaved(current, source.itemId)
        }
    }

    private fun onDecodeGenerated(current: AudioAppUiState, mode: TransportModeOption) {
        val session = current.sessions.getValue(mode)
        if (session.generatedPcm.isEmpty()) {
            sessionStateStore.updateSession(mode) {
                it.copy(statusText = UiText.Resource(R.string.status_no_audio_for_mode))
            }
            return
        }
        val request = DecodeRequest(
            mode = mode,
            generatedPcm = session.generatedPcm,
            flashPreset = resolveGeneratedFlashPreset(current, mode)
        )
        sessionStateStore.updateSession(request.mode) {
            it.copy(
                isCodecBusy = true,
                encodeProgress = null,
                encodePhase = null,
                statusText = UiText.Resource(
                    R.string.status_mode_audio_decoding,
                    listOf(request.mode.wireName)
                )
            )
        }

        scope.launch {
            val result = withContext(workerDispatcher) {
                performDecode(request)
            }
            applyGeneratedDecodeResult(request.mode, result)
        }
    }

    private fun onDecodeSaved(current: AudioAppUiState, itemId: String) {
        val selectedSavedAudio = current.selectedSavedAudio
            ?.takeIf { it.item.itemId == itemId }
            ?: return
        val mode = TransportModeOption.fromWireName(selectedSavedAudio.item.modeWireName)
        if (mode == null) {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_saved_audio_load_failed))
            }
            return
        }
        val request = DecodeRequest(
            mode = mode,
            generatedPcm = selectedSavedAudio.pcm,
            flashPreset = resolveSavedFlashPreset(current, selectedSavedAudio)
        )

        markSavedDecodeBusy(mode)
        scope.launch {
            val result = withContext(workerDispatcher) {
                performDecode(request)
            }
            applySavedDecodeResult(itemId, mode, result)
        }
    }

    private fun resolveGeneratedFlashPreset(
        current: AudioAppUiState,
        mode: TransportModeOption
    ): FlashVoicingStyleOption =
        if (mode == TransportModeOption.Flash) {
            current.sessions.getValue(mode).generatedFlashVoicingStyle ?: current.selectedFlashVoicingStyle
        } else {
            FlashVoicingStyleOption.CodedBurst
        }

    private fun resolveSavedFlashPreset(
        current: AudioAppUiState,
        savedAudio: SavedAudioPlaybackSelection
    ): FlashVoicingStyleOption =
        if (savedAudio.item.modeWireName == TransportModeOption.Flash.wireName) {
            savedAudio.item.flashVoicingStyle ?: current.selectedFlashVoicingStyle
        } else {
            FlashVoicingStyleOption.CodedBurst
        }

    private fun performDecode(request: DecodeRequest): DecodeResult {
        val validationIssue = audioCodecGateway.validateDecodeConfig(
            sampleRateHz,
            frameSamples,
            request.mode.nativeValue,
            request.flashPreset.signalProfileValue,
            request.flashPreset.voicingFlavorValue
        )
        if (validationIssue != BagApiCodes.VALIDATION_OK) {
            return DecodeResult.ValidationFailure(validationIssue)
        }

        val decoded = audioCodecGateway.decodeGeneratedPcm(
            request.generatedPcm,
            sampleRateHz,
            frameSamples,
            request.mode.nativeValue,
            request.flashPreset.signalProfileValue,
            request.flashPreset.voicingFlavorValue
        )
        return DecodeResult.Success(decoded)
    }

    private fun applyGeneratedDecodeResult(mode: TransportModeOption, result: DecodeResult) {
        when (result) {
            is DecodeResult.ValidationFailure -> {
                sessionStateStore.updateSession(mode) {
                    it.copy(
                        statusText = uiTextMapper.validationIssue(result.validationIssue),
                        isCodecBusy = false,
                        encodeProgress = null,
                        encodePhase = null,
                        isEncodeCancelling = false
                    )
                }
            }

            is DecodeResult.Success -> {
                val status = if (result.decoded.isEmpty()) {
                    uiTextMapper.errorCode(BagApiCodes.ERROR_INTERNAL)
                } else {
                    UiText.Resource(
                        R.string.status_mode_decode_completed,
                        listOf(mode.wireName)
                    )
                }
                sessionStateStore.updateSession(mode) {
                    it.copy(
                        resultText = result.decoded,
                        statusText = status,
                        isCodecBusy = false,
                        encodeProgress = null,
                        encodePhase = null,
                        isEncodeCancelling = false
                    )
                }
            }
        }
    }

    private fun markSavedDecodeBusy(mode: TransportModeOption) {
        sessionStateStore.updateCurrentSession {
            it.copy(
                isCodecBusy = true,
                encodeProgress = null,
                encodePhase = null,
                statusText = UiText.Resource(
                    R.string.status_mode_audio_decoding,
                    listOf(mode.wireName)
                )
            )
        }
    }

    private fun applySavedDecodeResult(itemId: String, mode: TransportModeOption, result: DecodeResult) {
        when (result) {
            is DecodeResult.ValidationFailure -> {
                sessionStateStore.updateCurrentSession {
                    it.copy(
                        statusText = uiTextMapper.validationIssue(result.validationIssue),
                        isCodecBusy = false,
                        encodeProgress = null,
                        encodePhase = null,
                        isEncodeCancelling = false
                    )
                }
            }

            is DecodeResult.Success -> {
                val status = if (result.decoded.isEmpty()) {
                    uiTextMapper.errorCode(BagApiCodes.ERROR_INTERNAL)
                } else {
                    UiText.Resource(
                        R.string.status_mode_decode_completed,
                        listOf(mode.wireName)
                    )
                }
                uiState.update { state ->
                    val selected = state.selectedSavedAudio
                        ?.takeIf { it.item.itemId == itemId }
                        ?: return@update state
                    state.copy(
                        selectedSavedAudio = selected.copy(decodedText = result.decoded)
                    )
                }
                sessionStateStore.updateCurrentSession {
                    it.copy(
                        statusText = status,
                        isCodecBusy = false,
                        encodeProgress = null,
                        encodePhase = null,
                        isEncodeCancelling = false
                    )
                }
            }
        }
    }

    private data class DecodeRequest(
        val mode: TransportModeOption,
        val generatedPcm: ShortArray,
        val flashPreset: FlashVoicingStyleOption
    )

    private sealed interface DecodeResult {
        data class ValidationFailure(val validationIssue: Int) : DecodeResult
        data class Success(val decoded: String) : DecodeResult
    }
}
