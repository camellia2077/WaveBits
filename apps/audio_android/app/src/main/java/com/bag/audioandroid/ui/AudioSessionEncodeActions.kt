package com.bag.audioandroid.ui

import android.util.Log
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PayloadFollowViewData
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
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class AudioSessionEncodeActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    audioCodecGateway: AudioCodecGateway,
    sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val stopPlayback: () -> Unit,
    workerDispatcher: CoroutineDispatcher,
    generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) {
    private val requestFactory = EncodeRequestFactory()
    private val stateReducer =
        EncodeStateReducer(
            uiState = uiState,
            sessionStateStore = sessionStateStore,
            uiTextMapper = uiTextMapper,
            playbackRuntimeGateway = playbackRuntimeGateway,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
        )
    private val encodeRunner =
        EncodeRunner(
            audioCodecGateway = audioCodecGateway,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            workerDispatcher = workerDispatcher,
            onProgress = stateReducer::applyProgress,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
        )

    private var encodeJob: Job? = null
    private val followDataJobs = mutableMapOf<TransportModeOption, Job>()

    fun onEncode() {
        val current = uiState.value
        if (current.currentSession.isCodecBusy) {
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "onEncode:ignoredBusy mode=${current.transportMode.wireName}",
            )
            return
        }

        val request = requestFactory.build(current)
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "onEncode:start mode=${request.mode.wireName} chars=${request.inputText.length} payloadBytes=${request.inputText.toByteArray(UTF_8).size} source=${current.currentPlaybackSource}",
        )
        stopPlayback()
        cancelFollowDataJob(request.mode)
        stateReducer.markBusy(request)
        launchEncode(request)
    }

    fun onCancelEncode() {
        val mode = uiState.value.transportMode
        val session = uiState.value.sessions.getValue(mode)
        if (!session.isCodecBusy || session.encodeProgress == null || session.isEncodeCancelling) {
            return
        }

        stateReducer.markCancelling(mode)
        encodeJob?.cancel()
    }

    private fun launchEncode(request: EncodeRequest) {
        encodeJob =
            scope.launch {
                val runningJob = currentCoroutineContext()[Job]
                try {
                    val followHydration =
                        stateReducer.reduceResult(request, encodeRunner.execute(request))
                    if (followHydration != null) {
                        launchFollowDataHydration(followHydration)
                    }
                } catch (cancelled: CancellationException) {
                    handleEncodeCancellation(request.mode, cancelled)
                } finally {
                    clearEncodeJob(runningJob)
                }
            }
    }

    private fun handleEncodeCancellation(
        mode: TransportModeOption,
        cancelled: CancellationException,
    ) {
        val sessionAfterCancel = uiState.value.sessions.getValue(mode)
        if (sessionAfterCancel.isEncodeCancelling) {
            stateReducer.applyCancelled(mode)
        } else {
            throw cancelled
        }
    }

    private fun clearEncodeJob(runningJob: Job?) {
        if (encodeJob === runningJob) {
            encodeJob = null
        }
    }

    private fun launchFollowDataHydration(request: FollowDataHydrationRequest) {
        cancelFollowDataJob(request.mode)
        followDataJobs[request.mode] =
            scope.launch {
                // Keep encode completion on the fast PCM path; hydrate the
                // heavier follow-data view models after playback is ready.
                safeLogE(
                    LONG_AUDIO_LOG_TAG,
                    "followHydration:start mode=${request.mode.wireName} revision=${request.generatedContentRevision} segments=${request.encodeRequest.segmentation?.segmentCount ?: 1}",
                )
                val followData = encodeRunner.buildFollowData(request.encodeRequest)
                if (followData != null) {
                    safeLogE(
                        LONG_AUDIO_LOG_TAG,
                        "followHydration:built mode=${request.mode.wireName} revision=${request.generatedContentRevision} followAvailable=${followData.followAvailable} textFollowAvailable=${followData.textFollowAvailable} totalSamples=${followData.totalPcmSampleCount}",
                    )
                    stateReducer.applyHydratedFollowData(
                        mode = request.mode,
                        revision = request.generatedContentRevision,
                        followData = followData,
                    )
                } else {
                    safeLogE(
                        LONG_AUDIO_LOG_TAG,
                        "followHydration:unavailable mode=${request.mode.wireName} revision=${request.generatedContentRevision}",
                    )
                }
            }
    }

    private fun cancelFollowDataJob(mode: TransportModeOption) {
        followDataJobs.remove(mode)?.cancel()
    }
}

private const val LONG_AUDIO_LOG_TAG = "WaveBitsLongAudio"

private fun safeLogE(
    tag: String,
    message: String,
) {
    try {
        Log.e(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.e is not implemented.
    }
}

private fun safeLogE(
    tag: String,
    message: String,
    throwable: Throwable,
) {
    try {
        Log.e(tag, message, throwable)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.e is not implemented.
    }
}

private class EncodeRequestFactory {
    fun build(current: AudioAppUiState): EncodeRequest =
        EncodeRequest(
            mode = current.transportMode,
            inputText = current.currentSession.inputText,
            sampleInputId = current.currentSession.sampleInputId,
            selectedFlashVoicingStyle = current.selectedFlashVoicingStyle,
            flashPreset =
                if (current.transportMode == TransportModeOption.Flash) {
                    current.selectedFlashVoicingStyle
                } else {
                    FlashVoicingStyleOption.CodedBurst
                },
            appVersion = current.presentationVersion.ifBlank { "unknown" },
            coreVersion = current.coreVersion.ifBlank { "unknown" },
        )
}

private class EncodeRunner(
    private val audioCodecGateway: AudioCodecGateway,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val workerDispatcher: CoroutineDispatcher,
    private val onProgress: (TransportModeOption, EncodeProgressUpdate) -> Unit,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) {
    suspend fun execute(request: EncodeRequest): EncodeResult =
        kotlinx.coroutines.withContext(workerDispatcher) {
            val payloadByteCount = request.inputText.toByteArray(UTF_8).size
            val validationIssue =
                audioCodecGateway.validateEncodeRequest(
                    request.inputText,
                    sampleRateHz,
                    frameSamples,
                    request.mode.nativeValue,
                    request.flashPreset.signalProfileValue,
                    request.flashPreset.voicingFlavorValue,
                )
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "execute:validated mode=${request.mode.wireName} payloadBytes=$payloadByteCount issue=$validationIssue maxSegmentPayloadBytes=${segmentedPayloadByteLimit(request)}",
            )
            if (
                validationIssue != BagApiCodes.VALIDATION_OK &&
                validationIssue != BagApiCodes.VALIDATION_PAYLOAD_TOO_LARGE
            ) {
                return@withContext EncodeResult.ValidationFailure(validationIssue)
            }
            if (shouldEncodeSegmented(request, validationIssue, payloadByteCount)) {
                safeLogE(
                    LONG_AUDIO_LOG_TAG,
                    "execute:routeSegmented mode=${request.mode.wireName} payloadBytes=$payloadByteCount issue=$validationIssue",
                )
                return@withContext encodeSegmented(request)
            }

            when (
                val gatewayResult =
                    audioCodecGateway.encodeTextToPcm(
                        request.inputText,
                        sampleRateHz,
                        frameSamples,
                        request.mode.nativeValue,
                        request.flashPreset.signalProfileValue,
                        request.flashPreset.voicingFlavorValue,
                        onProgress = { update -> onProgress(request.mode, update) },
                    )
            ) {
                is EncodeAudioResult.Success -> {
                    safeLogE(
                        LONG_AUDIO_LOG_TAG,
                        "execute:singleSuccess mode=${request.mode.wireName} payloadBytes=$payloadByteCount samples=${gatewayResult.pcm.size}",
                    )
                    EncodeResult.Success(
                        pcm = gatewayResult.pcm,
                        segmentCount = 1,
                    )
                }
                EncodeAudioResult.Cancelled -> EncodeResult.Cancelled
                is EncodeAudioResult.Failed -> {
                    safeLogE(
                        LONG_AUDIO_LOG_TAG,
                        "execute:singleFailed mode=${request.mode.wireName} payloadBytes=$payloadByteCount error=${gatewayResult.errorCode}",
                    )
                    if (gatewayResult.errorCode == BagApiCodes.ERROR_ENCODED_AUDIO_TOO_LARGE) {
                        safeLogE(
                            LONG_AUDIO_LOG_TAG,
                            "execute:retrySegmentedAfterEncodedTooLarge mode=${request.mode.wireName} payloadBytes=$payloadByteCount",
                        )
                        encodeSegmented(request)
                    } else {
                        EncodeResult.Failure(gatewayResult.errorCode)
                    }
                }
            }
        }

    suspend fun buildFollowData(request: EncodeRequest): PayloadFollowViewData? =
        kotlinx.coroutines.withContext(workerDispatcher) {
            val segmentation = request.segmentation
            if (segmentation != null) {
                val followSegments =
                    segmentation.segments.map { segmentText ->
                        val result =
                            audioCodecGateway.buildEncodeFollowData(
                                segmentText,
                                sampleRateHz,
                                frameSamples,
                                request.mode.nativeValue,
                                request.flashPreset.signalProfileValue,
                                request.flashPreset.voicingFlavorValue,
                            )
                        result.followData.takeIf { it.followAvailable } ?: return@withContext null
                    }
                return@withContext mergeSegmentedFollowData(followSegments)
            }
            val result =
                audioCodecGateway.buildEncodeFollowData(
                    request.inputText,
                    sampleRateHz,
                    frameSamples,
                    request.mode.nativeValue,
                    request.flashPreset.signalProfileValue,
                    request.flashPreset.voicingFlavorValue,
            )
            result.followData.takeIf { it.followAvailable }
        }

    private suspend fun encodeSegmented(request: EncodeRequest): EncodeResult {
        val maxPayloadBytes = segmentedPayloadByteLimit(request)
        val segmentation = splitInputIntoPayloadSegments(request.inputText, maxPayloadBytes)
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "encodeSegmented:start mode=${request.mode.wireName} payloadBytes=${request.inputText.toByteArray(UTF_8).size} segments=${segmentation.segmentCount} maxPayloadBytes=$maxPayloadBytes style=${request.selectedFlashVoicingStyle.id}",
        )
        val totalSegments = segmentation.segmentCount.toFloat()
        val audioAssembler =
            SegmentedEncodeAudioAssembler.create(
                generatedAudioCacheGateway = generatedAudioCacheGateway,
                modeWireName = request.mode.wireName,
                previewPointCount = LONG_AUDIO_WAVEFORM_PREVIEW_POINTS,
            )

        try {
            segmentation.segments.forEachIndexed { index, segmentText ->
                val validationIssue =
                    audioCodecGateway.validateEncodeRequest(
                        segmentText,
                        sampleRateHz,
                        frameSamples,
                        request.mode.nativeValue,
                        request.flashPreset.signalProfileValue,
                        request.flashPreset.voicingFlavorValue,
                )
                if (validationIssue != BagApiCodes.VALIDATION_OK) {
                    audioAssembler.abort()
                    return EncodeResult.ValidationFailure(validationIssue)
                }
                when (
                    val gatewayResult =
                        audioCodecGateway.encodeTextToPcm(
                            segmentText,
                            sampleRateHz,
                            frameSamples,
                            request.mode.nativeValue,
                            request.flashPreset.signalProfileValue,
                            request.flashPreset.voicingFlavorValue,
                            onProgress = { update ->
                                onProgress(
                                    request.mode,
                                    update.copy(
                                        progress0To1 =
                                            ((index.toFloat() + update.progress0To1.coerceIn(0f, 1f)) / totalSegments)
                                                .coerceIn(0f, 1f),
                                    ),
                                )
                            },
                        )
                ) {
                    is EncodeAudioResult.Success -> {
                        audioAssembler.appendSegment(gatewayResult.pcm)
                        safeLogE(
                            LONG_AUDIO_LOG_TAG,
                            "encodeSegmented:segmentSuccess mode=${request.mode.wireName} index=${index + 1}/${segmentation.segmentCount} segmentSamples=${gatewayResult.pcm.size} totalSamples=${audioAssembler.totalPcmSamples}",
                        )
                    }
                    EncodeAudioResult.Cancelled -> {
                        safeLogE(
                            LONG_AUDIO_LOG_TAG,
                            "encodeSegmented:cancelled mode=${request.mode.wireName} index=${index + 1}/${segmentation.segmentCount}",
                        )
                        audioAssembler.abort()
                        return EncodeResult.Cancelled
                    }
                    is EncodeAudioResult.Failed -> {
                        safeLogE(
                            LONG_AUDIO_LOG_TAG,
                            "encodeSegmented:failed mode=${request.mode.wireName} index=${index + 1}/${segmentation.segmentCount} error=${gatewayResult.errorCode}",
                        )
                        audioAssembler.abort()
                        return EncodeResult.Failure(gatewayResult.errorCode)
                    }
                }
            }
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "encodeSegmented:fileReady mode=${request.mode.wireName} totalSamples=${audioAssembler.totalPcmSamples} filePath=${audioAssembler.filePath}",
            )
        } catch (error: Exception) {
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "encodeSegmented:exception mode=${request.mode.wireName} totalSamples=${audioAssembler.totalPcmSamples}",
                error,
            )
            audioAssembler.abort()
            return EncodeResult.Failure(BagApiCodes.ERROR_INTERNAL)
        }

        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "encodeSegmented:buildPreview mode=${request.mode.wireName} totalSamples=${audioAssembler.totalPcmSamples} targetPoints=$LONG_AUDIO_WAVEFORM_PREVIEW_POINTS",
        )
        val segmentedAudio = audioAssembler.finish()
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "encodeSegmented:previewReady mode=${request.mode.wireName} previewSamples=${segmentedAudio.waveformPcm.size} totalSamples=${segmentedAudio.totalSamples}",
        )

        return EncodeResult.Success(
            pcm = shortArrayOf(),
            pcmSampleCount = segmentedAudio.totalSamples,
            generatedPcmFilePath = segmentedAudio.filePath,
            waveformPcm = segmentedAudio.waveformPcm,
            segmentCount = segmentation.segmentCount,
            segmentation = segmentation,
            segmentSampleCounts = segmentedAudio.segmentSampleCounts,
        )
    }

    private companion object {
        const val MAX_SINGLE_FRAME_PAYLOAD_BYTES = 512
        const val MAX_SEGMENT_PAYLOAD_BYTES_RITUAL = 256
        const val MAX_SEGMENT_PAYLOAD_BYTES_DEEP_RITUAL = 128
    }

    private fun shouldEncodeSegmented(
        request: EncodeRequest,
        validationIssue: Int,
        payloadByteCount: Int,
    ): Boolean =
        validationIssue == BagApiCodes.VALIDATION_PAYLOAD_TOO_LARGE ||
            payloadByteCount > segmentedPayloadByteLimit(request)

    private fun segmentedPayloadByteLimit(request: EncodeRequest): Int =
        when {
            request.mode != TransportModeOption.Flash -> MAX_SINGLE_FRAME_PAYLOAD_BYTES
            request.flashPreset == FlashVoicingStyleOption.DeepRitual -> MAX_SEGMENT_PAYLOAD_BYTES_DEEP_RITUAL
            request.flashPreset == FlashVoicingStyleOption.RitualChant -> MAX_SEGMENT_PAYLOAD_BYTES_RITUAL
            else -> MAX_SINGLE_FRAME_PAYLOAD_BYTES
        }
}

private class EncodeStateReducer(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val uiTextMapper: BagUiTextMapper,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) {
    fun markBusy(request: EncodeRequest) {
        sessionStateStore.updateSession(request.mode) {
            it.copy(
                isCodecBusy = true,
                encodeProgress = 0f,
                encodePhase = AudioEncodePhase.PreparingInput,
                isEncodeCancelling = false,
                statusText =
                    uiTextMapper.encodePhaseStatus(
                        request.mode.wireName,
                        AudioEncodePhase.PreparingInput,
                    ),
            )
        }
    }

    fun markCancelling(mode: TransportModeOption) {
        sessionStateStore.updateSession(mode) {
            it.copy(
                isEncodeCancelling = true,
                encodePhase = it.encodePhase,
                statusText =
                    UiText.Resource(
                        R.string.status_mode_audio_canceling,
                        listOf(mode.wireName),
                    ),
            )
        }
    }

    fun applyProgress(
        mode: TransportModeOption,
        update: EncodeProgressUpdate,
    ) {
        val clampedProgress = update.progress0To1.coerceIn(0f, 1f)
        sessionStateStore.updateSession(mode) { session ->
            if (session.isEncodeCancelling) {
                return@updateSession session
            }
            if (session.encodePhase == update.phase) {
                session.copy(encodeProgress = clampedProgress)
            } else {
                session.copy(
                    encodeProgress = clampedProgress,
                    encodePhase = update.phase,
                    statusText = uiTextMapper.encodePhaseStatus(mode.wireName, update.phase),
                )
            }
        }
    }

    fun reduceResult(
        request: EncodeRequest,
        result: EncodeResult,
    ): FollowDataHydrationRequest? =
        when (result) {
            is EncodeResult.ValidationFailure -> {
                applyValidationFailure(request.mode, result.validationIssue)
                null
            }
            EncodeResult.Cancelled -> {
                applyCancelled(request.mode)
                null
            }
            is EncodeResult.Failure -> {
                applyFailure(request.mode, result.errorCode)
                null
            }
            is EncodeResult.Success -> applySuccess(request, result)
        }

    fun applyCancelled(mode: TransportModeOption) {
        sessionStateStore.updateSession(mode) {
            it.copy(
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
                statusText =
                    UiText.Resource(
                        R.string.status_mode_audio_cancelled,
                        listOf(mode.wireName),
                    ),
            )
        }
    }

    private fun applyValidationFailure(
        mode: TransportModeOption,
        validationIssue: Int,
    ) {
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "applyValidationFailure mode=${mode.wireName} issue=$validationIssue",
        )
        val previousFilePath = uiState.value.sessions.getValue(mode).generatedPcmFilePath
        sessionStateStore.updateSession(mode) {
            it.copy(
                generatedPcm = shortArrayOf(),
                generatedWaveformPcm = shortArrayOf(),
                generatedPcmFilePath = null,
                generatedAudioMetadata = null,
                generatedFlashVoicingStyle = null,
                decodedPayload = DecodedPayloadViewData.Empty,
                followData = PayloadFollowViewData.Empty,
                statusText = uiTextMapper.validationIssue(validationIssue),
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
                playback = playbackRuntimeGateway.cleared(),
            )
        }
        if (previousFilePath != null) {
            generatedAudioCacheGateway.deleteCachedFile(previousFilePath)
        }
    }

    private fun applyFailure(
        mode: TransportModeOption,
        errorCode: Int,
    ) {
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "applyFailure mode=${mode.wireName} error=$errorCode",
        )
        sessionStateStore.updateSession(mode) {
            it.copy(
                decodedPayload = DecodedPayloadViewData.Empty,
                followData = PayloadFollowViewData.Empty,
                statusText = uiTextMapper.errorCode(errorCode),
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
                playback = playbackRuntimeGateway.cleared(),
            )
        }
    }

    private fun applySuccess(
        request: EncodeRequest,
        result: EncodeResult.Success,
    ): FollowDataHydrationRequest? {
        val pcm = result.pcm
        val previousFilePath = uiState.value.sessions.getValue(request.mode).generatedPcmFilePath
        val generatedFlashStyle =
            if (request.mode == TransportModeOption.Flash && result.pcmSampleCount > 0) {
                request.selectedFlashVoicingStyle
            } else {
                null
            }
        val payloadByteCount = request.inputText.toByteArray(UTF_8).size
        val nextRevision =
            uiState.value.sessions.getValue(request.mode).generatedContentRevision + 1L
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "applySuccess:begin mode=${request.mode.wireName} pcmSamples=${result.pcmSampleCount} inMemorySamples=${pcm.size} waveformSamples=${result.waveformPcm.size} fileBacked=${result.generatedPcmFilePath != null} segments=${result.segmentCount} payloadBytes=$payloadByteCount nextRevision=$nextRevision",
        )
        sessionStateStore.updateSession(request.mode) {
            it.copy(
                generatedPcm = pcm,
                generatedWaveformPcm = result.waveformPcm,
                generatedPcmFilePath = result.generatedPcmFilePath,
                generatedAudioMetadata =
                    GeneratedAudioMetadata(
                        mode = request.mode,
                        flashVoicingStyle = generatedFlashStyle,
                        createdAtIsoUtc = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                        durationMs = (result.pcmSampleCount.toLong() * 1000L) / sampleRateHz.toLong(),
                        sampleRateHz = sampleRateHz,
                        frameSamples = frameSamples,
                        pcmSampleCount = result.pcmSampleCount,
                        payloadByteCount = payloadByteCount,
                        inputSourceKind =
                            if (request.sampleInputId != null) {
                                GeneratedAudioInputSourceKind.Sample
                            } else {
                                GeneratedAudioInputSourceKind.Manual
                            },
                        segmentCount = result.segmentCount,
                        appVersion = request.appVersion,
                        coreVersion = request.coreVersion,
                        segmentSampleCounts = result.segmentSampleCounts,
                    ),
                generatedFlashVoicingStyle = generatedFlashStyle,
                generatedContentRevision = nextRevision,
                decodedPayload = DecodedPayloadViewData.Empty,
                followData = PayloadFollowViewData.Empty,
                statusText =
                    UiText.Resource(
                        if (result.segmentCount > 1) {
                            R.string.status_mode_audio_generated_segmented
                        } else {
                            R.string.status_mode_audio_generated
                        },
                        if (result.segmentCount > 1) {
                            listOf(request.mode.wireName, result.pcmSampleCount, result.segmentCount)
                        } else {
                            listOf(request.mode.wireName, result.pcmSampleCount)
                        },
                    ),
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
                playback = playbackRuntimeGateway.load(result.pcmSampleCount, sampleRateHz),
            )
        }
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "applySuccess:sessionUpdated mode=${request.mode.wireName} filePath=${result.generatedPcmFilePath} previousFilePath=$previousFilePath",
        )
        if (previousFilePath != null && previousFilePath != result.generatedPcmFilePath) {
            generatedAudioCacheGateway.deleteCachedFile(previousFilePath)
        }
        if (uiState.value.transportMode == request.mode) {
            uiState.update {
                it.copy(currentPlaybackSource = AudioPlaybackSource.Generated(request.mode))
            }
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "applySuccess:playbackSourceUpdated mode=${request.mode.wireName} transportModeMatches=true",
            )
        }
        if (!shouldHydrateFollowData(request, result)) {
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "applySuccess:skipFollowHydration mode=${request.mode.wireName} pcmSamples=${result.pcmSampleCount} segments=${result.segmentCount} fileBacked=${result.generatedPcmFilePath != null}",
            )
            return null
        }
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "applySuccess:queueFollowHydration mode=${request.mode.wireName} revision=$nextRevision",
        )
        return FollowDataHydrationRequest(
            mode = request.mode,
            encodeRequest = request.copy(segmentation = result.segmentation),
            generatedContentRevision = nextRevision,
        )
    }

    private fun shouldHydrateFollowData(
        request: EncodeRequest,
        result: EncodeResult.Success,
    ): Boolean {
        val payloadByteCount = request.inputText.toByteArray(UTF_8).size
        return payloadByteCount <= MAX_FOLLOW_DATA_PAYLOAD_BYTES &&
            result.segmentCount <= MAX_FOLLOW_DATA_SEGMENTS
    }

    fun applyHydratedFollowData(
        mode: TransportModeOption,
        revision: Long,
        followData: PayloadFollowViewData,
    ) {
        sessionStateStore.updateSession(mode) { session ->
            if (session.generatedContentRevision != revision) {
                safeLogE(
                    LONG_AUDIO_LOG_TAG,
                    "followHydration:skipStale mode=${mode.wireName} revision=$revision currentRevision=${session.generatedContentRevision}",
                )
                session
            } else {
                safeLogE(
                    LONG_AUDIO_LOG_TAG,
                    "followHydration:applied mode=${mode.wireName} revision=$revision followAvailable=${followData.followAvailable} textFollowAvailable=${followData.textFollowAvailable}",
                )
                session.copy(followData = followData)
            }
        }
    }
}

private data class EncodeRequest(
    val mode: TransportModeOption,
    val inputText: String,
    val sampleInputId: String?,
    val selectedFlashVoicingStyle: FlashVoicingStyleOption,
    val flashPreset: FlashVoicingStyleOption,
    val appVersion: String,
    val coreVersion: String,
    val segmentation: SegmentedInputPlan? = null,
)

private sealed interface EncodeResult {
    data class ValidationFailure(
        val validationIssue: Int,
    ) : EncodeResult

    data object Cancelled : EncodeResult

    data class Failure(
        val errorCode: Int,
    ) : EncodeResult

    data class Success(
        val pcm: ShortArray,
        val pcmSampleCount: Int = pcm.size,
        val generatedPcmFilePath: String? = null,
        val waveformPcm: ShortArray = pcm,
        val segmentCount: Int,
        val segmentation: SegmentedInputPlan? = null,
        val segmentSampleCounts: List<Int> = emptyList(),
    ) : EncodeResult
}

private data class FollowDataHydrationRequest(
    val mode: TransportModeOption,
    val encodeRequest: EncodeRequest,
    val generatedContentRevision: Long,
)

private const val MAX_FOLLOW_DATA_PAYLOAD_BYTES = 4096
private const val MAX_FOLLOW_DATA_SEGMENTS = 8
private const val LONG_AUDIO_WAVEFORM_PREVIEW_POINTS = 4096
