package com.bag.audioandroid.ui

import android.util.Log
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.AudioIoWavCodes
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.model.analyzeMorseText
import com.bag.audioandroid.ui.screen.FlashSignalToneSegment
import com.bag.audioandroid.ui.screen.buildFlashSignalToneSegments
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.FlashVisualWindowSource
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import com.bag.audioandroid.ui.state.FollowDataWindowSource
import com.bag.audioandroid.ui.state.FollowDataWindowState
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
    audioIoGateway: AudioIoGateway,
    sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val stopPlayback: () -> Unit,
    workerDispatcher: CoroutineDispatcher,
    generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) {
    private val requestFactory = EncodeRequestFactory(frameSamples)
    private val stateReducer =
        EncodeStateReducer(
            uiState = uiState,
            sessionStateStore = sessionStateStore,
            uiTextMapper = uiTextMapper,
            playbackRuntimeGateway = playbackRuntimeGateway,
            audioIoGateway = audioIoGateway,
            sampleRateHz = sampleRateHz,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
        )
    private val encodeRunner =
        EncodeRunner(
            audioCodecGateway = audioCodecGateway,
            sampleRateHz = sampleRateHz,
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
            "onEncode:start mode=${request.mode.wireName} chars=${request.inputText.length} payloadBytes=${request.payloadByteCount} frameSamples=${request.frameSamples} source=${current.currentPlaybackSource}",
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
                val windowSource = request.toFollowDataWindowSource()
                val window =
                    windowSource?.followWindowAround(0)
                        ?: FollowDataWindowState(
                            startSample = 0,
                            endSampleExclusive = initialFollowWindowEndSample(request.totalPcmSampleCount),
                        )
                val followData =
                    encodeRunner.buildFollowDataWindow(
                        request = request.encodeRequest,
                        windowStartSample = window.startSample,
                        windowEndSampleExclusive = window.endSampleExclusive,
                    )
                if (followData != null) {
                    val flashVisualWindowSource =
                        if (request.mode == TransportModeOption.Flash) {
                            encodeRunner.buildFlashVisualWindowSource(request.encodeRequest)
                        } else {
                            null
                        }
                    val flashVisualWindow = flashVisualWindowSource?.flashVisualWindowAround(0) ?: FlashVisualWindowState()
                    safeLogE(
                        LONG_AUDIO_LOG_TAG,
                        "followHydration:built mode=${request.mode.wireName} revision=${request.generatedContentRevision} followAvailable=${followData.followAvailable} textFollowAvailable=${followData.textFollowAvailable} totalSamples=${followData.totalPcmSampleCount} window=${window.startSample}-${window.endSampleExclusive}",
                    )
                    stateReducer.applyHydratedFollowData(
                        mode = request.mode,
                        revision = request.generatedContentRevision,
                        followData = followData,
                        window = window,
                        flashVisualWindowSource = flashVisualWindowSource,
                        flashVisualWindow = flashVisualWindow,
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

private const val LONG_AUDIO_LOG_TAG = "FlipBitsLongAudio"

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

private class EncodeRequestFactory(
    private val defaultFrameSamples: Int,
) {
    fun build(current: AudioAppUiState): EncodeRequest =
        EncodeRequest(
            mode = current.transportMode,
            inputText = current.currentSession.inputText,
            sampleInputId = current.currentSession.sampleInputId,
            selectedFlashVoicingStyle = current.selectedFlashVoicingStyle,
            flashPreset =
                if (current.transportMode == TransportModeOption.Flash && current.isFlashVoicingEnabled) {
                    current.selectedFlashVoicingStyle
                } else {
                    FlashVoicingStyleOption.Standard
                },
            frameSamples =
                if (current.transportMode == TransportModeOption.Mini) {
                    current.selectedMorseSpeed.frameSamples(defaultFrameSamples)
                } else {
                    defaultFrameSamples
                },
            appVersion = current.presentationVersion.ifBlank { "unknown" },
            coreVersion = current.coreVersion.ifBlank { "unknown" },
        )
}

private class EncodeRunner(
    private val audioCodecGateway: AudioCodecGateway,
    private val sampleRateHz: Int,
    private val workerDispatcher: CoroutineDispatcher,
    private val onProgress: (TransportModeOption, EncodeProgressUpdate) -> Unit,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) {
    suspend fun execute(request: EncodeRequest): EncodeResult =
        kotlinx.coroutines.withContext(workerDispatcher) {
            val payloadByteCount = request.payloadByteCount
            val validationIssue =
                audioCodecGateway.validateEncodeRequest(
                    request.inputText,
                    sampleRateHz,
                    request.frameSamples,
                    request.mode.nativeValue,
                    request.flashPreset.signalProfileValue,
                    request.flashPreset.voicingFlavorValue,
                )
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "execute:validated mode=${request.mode.wireName} " +
                    "payloadBytes=$payloadByteCount " +
                    "issue=$validationIssue " +
                    "maxSegmentPayloadBytes=${segmentedPayloadByteLimit(request)}",
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
                        request.frameSamples,
                        request.mode.nativeValue,
                        request.flashPreset.signalProfileValue,
                        request.flashPreset.voicingFlavorValue,
                        onProgress = { update -> onProgress(request.mode, update) },
                    )
            ) {
                is EncodeAudioResult.Success -> {
                    val flashSignalInfo = describeFlashSignalForRequest(request)
                    val flashVisualTimelineSegments =
                        if (request.mode == TransportModeOption.Flash) {
                            buildFlashSignalToneSegments(gatewayResult.followData)
                        } else {
                            emptyList()
                        }
                    safeLogE(
                        LONG_AUDIO_LOG_TAG,
                        "execute:singleSuccess mode=${request.mode.wireName} payloadBytes=$payloadByteCount samples=${gatewayResult.pcm.size}",
                    )
                    EncodeResult.Success(
                        pcm = gatewayResult.pcm,
                        segmentCount = 1,
                        flashSignalInfo = flashSignalInfo,
                        flashVisualTimelineSegments = flashVisualTimelineSegments,
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

    suspend fun buildFollowDataWindow(
        request: EncodeRequest,
        windowStartSample: Int,
        windowEndSampleExclusive: Int,
    ): PayloadFollowViewData? =
        kotlinx.coroutines.withContext(workerDispatcher) {
            val segmentation = request.segmentation
            if (segmentation != null) {
                val segmentSampleCounts = request.segmentSampleCounts
                if (segmentSampleCounts.size != segmentation.segmentCount) {
                    return@withContext null
                }
                val selectedSegments = ArrayList<PayloadFollowViewData>()
                var segmentStartSample = 0
                segmentation.segments.forEachIndexed { index, segmentText ->
                    val segmentSampleCount = segmentSampleCounts[index]
                    val segmentEndSample = segmentStartSample + segmentSampleCount
                    if (segmentEndSample > windowStartSample && segmentStartSample < windowEndSampleExclusive) {
                        val result =
                            audioCodecGateway.buildEncodeFollowData(
                                segmentText,
                                sampleRateHz,
                                request.frameSamples,
                                request.mode.nativeValue,
                                request.flashPreset.signalProfileValue,
                                request.flashPreset.voicingFlavorValue,
                            )
                        selectedSegments += result.followData.takeIf { it.followAvailable } ?: return@withContext null
                    }
                    segmentStartSample = segmentEndSample
                }
                return@withContext mergeSegmentedFollowDataWindow(
                    segments = selectedSegments,
                    firstSampleOffset =
                        selectedWindowSampleOffset(
                            segmentSampleCounts = segmentSampleCounts,
                            windowStartSample = windowStartSample,
                            windowEndSampleExclusive = windowEndSampleExclusive,
                        ),
                    totalPcmSampleCount = segmentSampleCounts.sum(),
                )
            }
            val result =
                audioCodecGateway.buildEncodeFollowData(
                    request.inputText,
                    sampleRateHz,
                    request.frameSamples,
                    request.mode.nativeValue,
                    request.flashPreset.signalProfileValue,
                    request.flashPreset.voicingFlavorValue,
                )
            result.followData.takeIf { it.followAvailable }
        }

    suspend fun buildFlashVisualWindowSource(request: EncodeRequest): FlashVisualWindowSource? =
        kotlinx.coroutines.withContext(workerDispatcher) {
            if (request.mode != TransportModeOption.Flash || request.totalPcmSampleCount <= 0) {
                return@withContext null
            }

            val timelineSegments = ArrayList<FlashSignalToneSegment>()
            val segmentation = request.segmentation
            if (segmentation != null) {
                val segmentSampleCounts = request.segmentSampleCounts
                if (segmentSampleCounts.size != segmentation.segmentCount) {
                    return@withContext null
                }
                var segmentStartSample = 0
                segmentation.segments.forEachIndexed { index, segmentText ->
                    val result =
                        audioCodecGateway.buildEncodeFollowData(
                            segmentText,
                            sampleRateHz,
                            request.frameSamples,
                            request.mode.nativeValue,
                            request.flashPreset.signalProfileValue,
                            request.flashPreset.voicingFlavorValue,
                        )
                    timelineSegments +=
                        buildFlashSignalToneSegments(result.followData)
                            .map { segment ->
                                segment.copy(
                                    startSample = segment.startSample + segmentStartSample,
                                    endSample = segment.endSample + segmentStartSample,
                                )
                            }
                    segmentStartSample += segmentSampleCounts[index]
                }
            } else {
                val result =
                    audioCodecGateway.buildEncodeFollowData(
                        request.inputText,
                        sampleRateHz,
                        request.frameSamples,
                        request.mode.nativeValue,
                        request.flashPreset.signalProfileValue,
                        request.flashPreset.voicingFlavorValue,
                    )
                timelineSegments += buildFlashSignalToneSegments(result.followData)
            }

            if (timelineSegments.isEmpty()) {
                null
            } else {
                FlashVisualWindowSource(
                    timelineSegments = timelineSegments,
                    totalPcmSampleCount = request.totalPcmSampleCount,
                )
            }
        }

    private suspend fun encodeSegmented(request: EncodeRequest): EncodeResult {
        val maxPayloadBytes = segmentedPayloadByteLimit(request)
        val segmentation = splitInputIntoPayloadSegments(request.inputText, maxPayloadBytes)
        val segmentByteCounts = segmentation.segments.map { it.toByteArray(UTF_8).size }
        val segmentBytesSummary = segmentByteCounts.joinToString(prefix = "[", postfix = "]")
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "encodeSegmented:start mode=${request.mode.wireName} payloadBytes=${request.payloadByteCount} segments=${segmentation.segmentCount} maxPayloadBytes=$maxPayloadBytes style=${request.selectedFlashVoicingStyle.id} segmentBytes=$segmentBytesSummary",
        )
        val totalSegments = segmentation.segmentCount.toFloat()
        val audioAssembler =
            SegmentedEncodeAudioAssembler.create(
                generatedAudioCacheGateway = generatedAudioCacheGateway,
                modeWireName = request.mode.wireName,
                previewPointCount = LONG_AUDIO_WAVEFORM_PREVIEW_POINTS,
            )
        val flashVisualTimelineSegments = ArrayList<FlashSignalToneSegment>()

        try {
            segmentation.segments.forEachIndexed { index, segmentText ->
                val segmentBytes = segmentByteCounts[index]
                safeLogE(
                    LONG_AUDIO_LOG_TAG,
                    "encodeSegmented:segmentStart mode=${request.mode.wireName} index=${index + 1}/${segmentation.segmentCount} chars=${segmentText.length} bytes=$segmentBytes maxPayloadBytes=$maxPayloadBytes",
                )
                val validationIssue =
                    audioCodecGateway.validateEncodeRequest(
                        segmentText,
                        sampleRateHz,
                        request.frameSamples,
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
                            request.frameSamples,
                            request.mode.nativeValue,
                            request.flashPreset.signalProfileValue,
                            request.flashPreset.voicingFlavorValue,
                            onProgress = { update ->
                                val segmentProgress = update.progress0To1.coerceIn(0f, 1f)
                                onProgress(
                                    request.mode,
                                    update.copy(
                                        phase =
                                            aggregateSegmentedEncodePhase(
                                                segmentIndex = index,
                                                segmentCount = segmentation.segmentCount,
                                                segmentPhase = update.phase,
                                                segmentProgress0To1 = segmentProgress,
                                            ),
                                        progress0To1 =
                                            ((index.toFloat() + segmentProgress) / totalSegments)
                                                .coerceIn(0f, 1f),
                                    ),
                                )
                            },
                        )
                ) {
                    is EncodeAudioResult.Success -> {
                        val segmentStartSample = audioAssembler.totalPcmSamples
                        audioAssembler.appendSegment(gatewayResult.pcm)
                        if (request.mode == TransportModeOption.Flash) {
                            flashVisualTimelineSegments +=
                                buildFlashSignalToneSegments(gatewayResult.followData)
                                    .map { segment ->
                                        segment.copy(
                                            startSample = segment.startSample + segmentStartSample,
                                            endSample = segment.endSample + segmentStartSample,
                                        )
                                    }
                        }
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
            flashSignalInfo =
                describeFlashSignalForRequest(
                    request,
                    text = segmentation.segments.firstOrNull() ?: request.inputText,
                ),
            flashVisualTimelineSegments = flashVisualTimelineSegments,
        )
    }

    private fun describeFlashSignalForRequest(
        request: EncodeRequest,
        text: String = request.inputText,
    ): FlashSignalInfo =
        if (request.mode == TransportModeOption.Flash) {
            audioCodecGateway.describeFlashSignal(
                text,
                sampleRateHz,
                request.frameSamples,
                request.flashPreset.signalProfileValue,
                request.flashPreset.voicingFlavorValue,
            )
        } else {
            FlashSignalInfo.Empty
        }

    private companion object {
        const val MAX_SINGLE_FRAME_PAYLOAD_BYTES = 512
        const val MAX_SEGMENT_PAYLOAD_BYTES_RITUAL = 64
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
            request.flashPreset.usesLongCadencePayload -> MAX_SEGMENT_PAYLOAD_BYTES_RITUAL
            else -> MAX_SINGLE_FRAME_PAYLOAD_BYTES
        }

    private fun aggregateSegmentedEncodePhase(
        segmentIndex: Int,
        segmentCount: Int,
        segmentPhase: AudioEncodePhase,
        segmentProgress0To1: Float,
    ): AudioEncodePhase {
        val isFirstSegment = segmentIndex == 0
        val isLastSegment = segmentIndex == segmentCount - 1
        if (isFirstSegment && segmentPhase == AudioEncodePhase.PreparingInput && segmentProgress0To1 <= 0f) {
            return AudioEncodePhase.PreparingInput
        }
        if (!isLastSegment) {
            return AudioEncodePhase.RenderingPcm
        }
        return when (segmentPhase) {
            AudioEncodePhase.PreparingInput -> AudioEncodePhase.RenderingPcm
            AudioEncodePhase.RenderingPcm,
            AudioEncodePhase.Postprocessing,
            AudioEncodePhase.Finalizing,
            -> segmentPhase
        }
    }
}

private class EncodeStateReducer(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val uiTextMapper: BagUiTextMapper,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val audioIoGateway: AudioIoGateway,
    private val sampleRateHz: Int,
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
        val previousFilePath =
            uiState.value.sessions
                .getValue(mode)
                .generatedPcmFilePath
        sessionStateStore.updateSession(mode) {
            it.copy(
                generatedPcm = shortArrayOf(),
                generatedWaveformPcm = shortArrayOf(),
                generatedPcmFilePath = null,
                generatedAudioMetadata = null,
                generatedWavAudioInfo = WavAudioInfo.Empty,
                generatedFlashVoicingStyle = null,
                generatedFlashSignalInfo = FlashSignalInfo.Empty,
                decodedPayload = DecodedPayloadViewData.Empty,
                followData = PayloadFollowViewData.Empty,
                followWindowSource = null,
                followWindow = FollowDataWindowState(),
                flashVisualWindowSource = null,
                flashVisualWindow = FlashVisualWindowState(),
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
        val previousFilePath =
            uiState.value.sessions
                .getValue(mode)
                .generatedPcmFilePath
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "applyFailure mode=${mode.wireName} error=$errorCode previousFilePath=$previousFilePath",
        )
        sessionStateStore.updateSession(mode) {
            it.copy(
                generatedPcm = shortArrayOf(),
                generatedWaveformPcm = shortArrayOf(),
                generatedPcmFilePath = null,
                generatedAudioMetadata = null,
                generatedWavAudioInfo = WavAudioInfo.Empty,
                generatedFlashVoicingStyle = null,
                generatedFlashSignalInfo = FlashSignalInfo.Empty,
                decodedPayload = DecodedPayloadViewData.Empty,
                followData = PayloadFollowViewData.Empty,
                followWindowSource = null,
                followWindow = FollowDataWindowState(),
                flashVisualWindowSource = null,
                flashVisualWindow = FlashVisualWindowState(),
                statusText = uiTextMapper.errorCode(errorCode),
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

    private fun applySuccess(
        request: EncodeRequest,
        result: EncodeResult.Success,
    ): FollowDataHydrationRequest? {
        val pcm = result.pcm
        val previousFilePath =
            uiState.value.sessions
                .getValue(request.mode)
                .generatedPcmFilePath
        val generatedFlashStyle =
            if (request.mode == TransportModeOption.Flash && result.pcmSampleCount > 0) {
                request.flashPreset
            } else {
                null
            }
        val payloadByteCount = request.payloadByteCount
        val generatedMetadata =
            GeneratedAudioMetadata(
                mode = request.mode,
                flashVoicingStyle = generatedFlashStyle,
                createdAtIsoUtc = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                durationMs = (result.pcmSampleCount.toLong() * 1000L) / sampleRateHz.toLong(),
                sampleRateHz = sampleRateHz,
                frameSamples = request.frameSamples,
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
            )
        val wavAudioInfo = probeGeneratedWavInfo(result, generatedMetadata)
        val nextRevision =
            uiState.value.sessions
                .getValue(request.mode)
                .generatedContentRevision + 1L
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "applySuccess:begin mode=${request.mode.wireName} pcmSamples=${result.pcmSampleCount} inMemorySamples=${pcm.size} waveformSamples=${result.waveformPcm.size} fileBacked=${result.generatedPcmFilePath != null} segments=${result.segmentCount} payloadBytes=$payloadByteCount nextRevision=$nextRevision",
        )
        sessionStateStore.updateSession(request.mode) {
            it.copy(
                generatedPcm = pcm,
                generatedWaveformPcm = result.waveformPcm,
                generatedPcmFilePath = result.generatedPcmFilePath,
                generatedAudioMetadata = generatedMetadata,
                generatedWavAudioInfo = wavAudioInfo,
                generatedFlashVoicingStyle = generatedFlashStyle,
                generatedFlashSignalInfo = result.flashSignalInfo,
                generatedContentRevision = nextRevision,
                decodedPayload = DecodedPayloadViewData.Empty,
                followData = PayloadFollowViewData.Empty,
                followWindowSource = result.toFollowDataWindowSource(request),
                followWindow = FollowDataWindowState(),
                flashVisualWindowSource = result.toFlashVisualWindowSource(request),
                flashVisualWindow = result.initialFlashVisualWindow(request),
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
        generatedAudioCacheGateway.enforceGeneratedAudioCachePolicy(uiState.value)
        if (!shouldHydrateFollowData(result)) {
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
            encodeRequest =
                request.copy(
                    segmentation = result.segmentation,
                    segmentSampleCounts = result.segmentSampleCounts,
                    totalPcmSampleCount = result.pcmSampleCount,
                ),
            generatedContentRevision = nextRevision,
            totalPcmSampleCount = result.pcmSampleCount,
        )
    }

    private fun shouldHydrateFollowData(result: EncodeResult.Success): Boolean = result.pcmSampleCount > 0

    private fun probeGeneratedWavInfo(
        result: EncodeResult.Success,
        metadata: GeneratedAudioMetadata,
    ): WavAudioInfo {
        if (result.pcm.isEmpty() && result.generatedPcmFilePath != null) {
            return metadata.toGeneratedWavAudioInfo()
        }
        val pcmForProbe =
            if (result.pcm.isNotEmpty()) {
                result.pcm
            } else {
                ShortArray(result.pcmSampleCount.coerceAtLeast(0))
            }
        if (pcmForProbe.isEmpty()) {
            return WavAudioInfo.Empty
        }
        val wavBytes = audioIoGateway.encodeMonoPcm16ToWavBytes(sampleRateHz, pcmForProbe, metadata)
        if (wavBytes.isEmpty()) {
            return WavAudioInfo.Empty
        }
        return audioIoGateway.probeMonoPcm16WavBytes(wavBytes)
    }

    fun applyHydratedFollowData(
        mode: TransportModeOption,
        revision: Long,
        followData: PayloadFollowViewData,
        window: FollowDataWindowState,
        flashVisualWindowSource: FlashVisualWindowSource?,
        flashVisualWindow: FlashVisualWindowState,
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
                session.copy(
                    followData = followData,
                    followWindow = window,
                    flashVisualWindowSource = flashVisualWindowSource ?: session.flashVisualWindowSource,
                    flashVisualWindow = flashVisualWindow.takeIf { it.available } ?: session.flashVisualWindow,
                )
            }
        }
    }
}

private fun initialFollowWindowEndSample(totalPcmSampleCount: Int): Int =
    minOf(totalPcmSampleCount, FollowWindowInitialSamples).coerceAtLeast(1)

private fun GeneratedAudioMetadata.toGeneratedWavAudioInfo(): WavAudioInfo =
    WavAudioInfo(
        wavStatusCode = AudioIoWavCodes.STATUS_OK,
        sampleRateHz = sampleRateHz,
        channels = 1,
        bitsPerSample = 16,
        pcmSampleCount = pcmSampleCount.toLong(),
        dataByteCount = pcmSampleCount.toLong() * 2L,
        fileByteCount = 44L + pcmSampleCount.toLong() * 2L,
        durationMs = durationMs,
    )

private fun selectedWindowSampleOffset(
    segmentSampleCounts: List<Int>,
    windowStartSample: Int,
    windowEndSampleExclusive: Int,
): Int {
    var segmentStartSample = 0
    segmentSampleCounts.forEach { segmentSampleCount ->
        val segmentEndSample = segmentStartSample + segmentSampleCount
        if (segmentEndSample > windowStartSample && segmentStartSample < windowEndSampleExclusive) {
            return segmentStartSample
        }
        segmentStartSample = segmentEndSample
    }
    return 0
}

private fun EncodeResult.Success.toFollowDataWindowSource(request: EncodeRequest): FollowDataWindowSource? {
    val segmentation = segmentation ?: return null
    val counts = segmentSampleCounts.takeIf { it.size == segmentation.segmentCount } ?: return null
    return FollowDataWindowSource(
        segmentTexts = segmentation.segments,
        segmentSampleCounts = counts,
        totalPcmSampleCount = pcmSampleCount,
        flashSignalProfile = if (request.mode == TransportModeOption.Flash) request.flashPreset.signalProfileValue else 0,
        flashVoicingFlavor = if (request.mode == TransportModeOption.Flash) request.flashPreset.voicingFlavorValue else 0,
    )
}

private fun EncodeResult.Success.toFlashVisualWindowSource(request: EncodeRequest): FlashVisualWindowSource? {
    if (request.mode != TransportModeOption.Flash) {
        return null
    }
    if (flashVisualTimelineSegments.isEmpty()) {
        return null
    }
    return FlashVisualWindowSource(
        timelineSegments = flashVisualTimelineSegments,
        totalPcmSampleCount = pcmSampleCount,
    )
}

private fun EncodeResult.Success.initialFlashVisualWindow(request: EncodeRequest): FlashVisualWindowState =
    toFlashVisualWindowSource(request)?.flashVisualWindowAround(0) ?: FlashVisualWindowState()

private fun FollowDataHydrationRequest.toFollowDataWindowSource(): FollowDataWindowSource? {
    val segmentation = encodeRequest.segmentation ?: return null
    val counts = encodeRequest.segmentSampleCounts.takeIf { it.size == segmentation.segmentCount } ?: return null
    return FollowDataWindowSource(
        segmentTexts = segmentation.segments,
        segmentSampleCounts = counts,
        totalPcmSampleCount = totalPcmSampleCount,
        flashSignalProfile = if (encodeRequest.mode == TransportModeOption.Flash) encodeRequest.flashPreset.signalProfileValue else 0,
        flashVoicingFlavor = if (encodeRequest.mode == TransportModeOption.Flash) encodeRequest.flashPreset.voicingFlavorValue else 0,
    )
}

private data class EncodeRequest(
    val mode: TransportModeOption,
    val inputText: String,
    val sampleInputId: String?,
    val selectedFlashVoicingStyle: FlashVoicingStyleOption,
    val flashPreset: FlashVoicingStyleOption,
    val frameSamples: Int,
    val appVersion: String,
    val coreVersion: String,
    val segmentation: SegmentedInputPlan? = null,
    val segmentSampleCounts: List<Int> = emptyList(),
    val totalPcmSampleCount: Int = 0,
) {
    val payloadByteCount: Int
        get() =
            if (mode == TransportModeOption.Mini) {
                analyzeMorseText(inputText).normalizedText.toByteArray(UTF_8).size
            } else {
                inputText.toByteArray(UTF_8).size
            }
}

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
        val flashSignalInfo: FlashSignalInfo = FlashSignalInfo.Empty,
        val flashVisualTimelineSegments: List<FlashSignalToneSegment> = emptyList(),
    ) : EncodeResult
}

private data class FollowDataHydrationRequest(
    val mode: TransportModeOption,
    val encodeRequest: EncodeRequest,
    val generatedContentRevision: Long,
    val totalPcmSampleCount: Int,
)

private const val LONG_AUDIO_WAVEFORM_PREVIEW_POINTS = 4096
private const val FollowWindowInitialSamples = 44_100 * 60
