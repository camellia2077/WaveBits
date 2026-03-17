package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class AudioSessionEncodeActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val audioCodecGateway: AudioCodecGateway,
    private val sessionStateStore: AudioSessionStateStore,
    private val uiTextMapper: BagUiTextMapper,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val stopPlayback: () -> Unit,
    private val workerDispatcher: CoroutineDispatcher
) {
    private var encodeJob: Job? = null

    fun onEncode() {
        val current = uiState.value
        val session = current.currentSession
        if (session.isCodecBusy) {
            return
        }

        val request = EncodeRequest(
            mode = current.transportMode,
            inputText = session.inputText,
            selectedFlashVoicingStyle = current.selectedFlashVoicingStyle,
            flashPreset = resolveFlashPreset(current),
            appVersion = current.presentationVersion.ifBlank { "unknown" },
            coreVersion = current.coreVersion.ifBlank { "unknown" }
        )

        stopPlayback()
        sessionStateStore.updateSession(request.mode) {
            it.copy(
                isCodecBusy = true,
                encodeProgress = 0f,
                encodePhase = AudioEncodePhase.PreparingInput,
                isEncodeCancelling = false,
                statusText = uiTextMapper.encodePhaseStatus(
                    request.mode.wireName,
                    AudioEncodePhase.PreparingInput
                )
            )
        }

        val launchedJob = scope.launch {
            val runningJob = currentCoroutineContext()[Job]
            try {
                val result = withContext(workerDispatcher) {
                    performEncode(request)
                }
                applyEncodeResult(request, result)
            } catch (cancelled: CancellationException) {
                val sessionAfterCancel = uiState.value.sessions.getValue(request.mode)
                if (sessionAfterCancel.isEncodeCancelling) {
                    applyEncodeCancelled(request.mode)
                } else {
                    throw cancelled
                }
            } finally {
                if (encodeJob === runningJob) {
                    encodeJob = null
                }
            }
        }
        encodeJob = launchedJob
    }

    fun onCancelEncode() {
        val mode = uiState.value.transportMode
        val session = uiState.value.sessions.getValue(mode)
        if (!session.isCodecBusy || session.encodeProgress == null || session.isEncodeCancelling) {
            return
        }

        sessionStateStore.updateSession(mode) {
            it.copy(
                isEncodeCancelling = true,
                encodePhase = it.encodePhase,
                statusText = UiText.Resource(
                    R.string.status_mode_audio_canceling,
                    listOf(mode.wireName)
                )
            )
        }
        encodeJob?.cancel()
    }

    private fun resolveFlashPreset(current: AudioAppUiState): FlashVoicingStyleOption =
        if (current.transportMode == TransportModeOption.Flash) {
            current.selectedFlashVoicingStyle
        } else {
            FlashVoicingStyleOption.CodedBurst
        }

    private suspend fun performEncode(request: EncodeRequest): EncodeResult {
        val validationIssue = audioCodecGateway.validateEncodeRequest(
            request.inputText,
            sampleRateHz,
            frameSamples,
            request.mode.nativeValue,
            request.flashPreset.signalProfileValue,
            request.flashPreset.voicingFlavorValue
        )
        if (validationIssue != BagApiCodes.VALIDATION_OK) {
            return EncodeResult.ValidationFailure(validationIssue)
        }

        return when (
            val gatewayResult = audioCodecGateway.encodeTextToPcm(
                request.inputText,
                sampleRateHz,
                frameSamples,
                request.mode.nativeValue,
                request.flashPreset.signalProfileValue,
                request.flashPreset.voicingFlavorValue,
                onProgress = { update ->
                    applyEncodeProgress(request.mode, update)
                }
            )
        ) {
            is EncodeAudioResult.Success -> EncodeResult.Success(gatewayResult.pcm)
            EncodeAudioResult.Cancelled -> EncodeResult.Cancelled
            is EncodeAudioResult.Failed -> EncodeResult.Failure(gatewayResult.errorCode)
        }
    }

    private fun applyEncodeProgress(mode: TransportModeOption, update: EncodeProgressUpdate) {
        val clampedProgress = update.progress0To1.coerceIn(0f, 1f)
        sessionStateStore.updateSession(mode) { session ->
            if (session.isEncodeCancelling) {
                return@updateSession session
            }
            if (session.encodePhase == update.phase) {
                session.copy(
                    encodeProgress = clampedProgress
                )
            } else {
                session.copy(
                    encodeProgress = clampedProgress,
                    encodePhase = update.phase,
                    statusText = uiTextMapper.encodePhaseStatus(mode.wireName, update.phase)
                )
            }
        }
    }

    private fun applyEncodeResult(request: EncodeRequest, result: EncodeResult) {
        when (result) {
            is EncodeResult.ValidationFailure -> {
                sessionStateStore.updateSession(request.mode) {
                    it.copy(
                        generatedPcm = shortArrayOf(),
                        generatedAudioMetadata = null,
                        generatedFlashVoicingStyle = null,
                        resultText = "",
                        statusText = uiTextMapper.validationIssue(result.validationIssue),
                        isCodecBusy = false,
                        encodeProgress = null,
                        encodePhase = null,
                        isEncodeCancelling = false,
                        playback = playbackRuntimeGateway.cleared()
                    )
                }
            }

            EncodeResult.Cancelled -> {
                applyEncodeCancelled(request.mode)
            }

            is EncodeResult.Failure -> {
                sessionStateStore.updateSession(request.mode) {
                    it.copy(
                        resultText = "",
                        statusText = uiTextMapper.errorCode(result.errorCode),
                        isCodecBusy = false,
                        encodeProgress = null,
                        encodePhase = null,
                        isEncodeCancelling = false,
                        playback = playbackRuntimeGateway.cleared()
                    )
                }
            }

            is EncodeResult.Success -> {
                val pcm = result.pcm
                val status = UiText.Resource(
                    R.string.status_mode_audio_generated,
                    listOf(request.mode.wireName, pcm.size)
                )

                sessionStateStore.updateSession(request.mode) {
                    it.copy(
                        generatedPcm = pcm,
                        generatedAudioMetadata = GeneratedAudioMetadata(
                            mode = request.mode,
                            flashVoicingStyle = if (request.mode == TransportModeOption.Flash && pcm.isNotEmpty()) {
                                request.selectedFlashVoicingStyle
                            } else {
                                null
                            },
                            createdAtIsoUtc = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                            durationMs = (pcm.size.toLong() * 1000L) / sampleRateHz.toLong(),
                            frameSamples = frameSamples,
                            pcmSampleCount = pcm.size,
                            appVersion = request.appVersion,
                            coreVersion = request.coreVersion
                        ),
                        generatedFlashVoicingStyle = if (request.mode == TransportModeOption.Flash && pcm.isNotEmpty()) {
                            request.selectedFlashVoicingStyle
                        } else {
                            null
                        },
                        resultText = "",
                        statusText = status,
                        isCodecBusy = false,
                        encodeProgress = null,
                        encodePhase = null,
                        isEncodeCancelling = false,
                        playback = playbackRuntimeGateway.load(pcm.size, sampleRateHz)
                    )
                }
                if (uiState.value.transportMode == request.mode) {
                    uiState.update {
                        it.copy(currentPlaybackSource = AudioPlaybackSource.Generated(request.mode))
                    }
                }
            }
        }
    }

    private fun applyEncodeCancelled(mode: TransportModeOption) {
        sessionStateStore.updateSession(mode) {
            it.copy(
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
                statusText = UiText.Resource(
                    R.string.status_mode_audio_cancelled,
                    listOf(mode.wireName)
                )
            )
        }
    }

    private data class EncodeRequest(
        val mode: TransportModeOption,
        val inputText: String,
        val selectedFlashVoicingStyle: FlashVoicingStyleOption,
        val flashPreset: FlashVoicingStyleOption,
        val appVersion: String,
        val coreVersion: String
    )

    private sealed interface EncodeResult {
        data class ValidationFailure(val validationIssue: Int) : EncodeResult
        data object Cancelled : EncodeResult
        data class Failure(val errorCode: Int) : EncodeResult
        data class Success(val pcm: ShortArray) : EncodeResult
    }
}
