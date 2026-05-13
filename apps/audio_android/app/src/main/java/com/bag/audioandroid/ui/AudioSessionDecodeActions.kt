package com.bag.audioandroid.ui

import android.util.Log
import com.bag.audioandroid.R
import com.bag.audioandroid.data.readPcmSegmentsFromFile
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.SavedAudioPlaybackSelection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.LinkedHashSet

internal class AudioSessionDecodeActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val audioCodecGateway: AudioCodecGateway,
    sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val workerDispatcher: CoroutineDispatcher,
    private val savedAudioDecodeCacheGateway: SavedAudioDecodeCacheGateway,
) {
    private val requestFactory = DecodeRequestFactory(sampleRateHz = sampleRateHz, frameSamples = frameSamples)
    private val stateReducer =
        DecodeStateReducer(
            uiState = uiState,
            sessionStateStore = sessionStateStore,
            uiTextMapper = uiTextMapper,
        )
    private val decodeRunner =
        DecodeRunner(
            audioCodecGateway = audioCodecGateway,
            workerDispatcher = workerDispatcher,
        )

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

    fun ensureCurrentPlaybackDecodedForLyrics() {
        val current = uiState.value
        if (current.currentSession.isCodecBusy) {
            return
        }
        val source = current.currentPlaybackSource as? AudioPlaybackSource.Saved ?: return
        val selectedSavedAudio =
            current.selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?: return
        if (selectedSavedAudio.isLoadingContent) {
            return
        }
        if (selectedSavedAudio.isDecodingContent) {
            return
        }
        val alreadyDecodedForLyrics =
            selectedSavedAudio.decodedPayload.textDecodeStatusCode !=
                BagDecodeContentCodes.STATUS_UNAVAILABLE ||
                selectedSavedAudio.followData.textFollowAvailable
        if (alreadyDecodedForLyrics) {
            return
        }
        onDecodeSaved(current, source.itemId)
    }

    private fun onDecodeGenerated(
        current: AudioAppUiState,
        mode: TransportModeOption,
    ) {
        val session = current.sessions.getValue(mode)
        if (session.generatedPcm.isEmpty() && session.generatedPcmFilePath.isNullOrBlank()) {
            stateReducer.applyNoGeneratedAudio(mode)
            return
        }
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "decodeGenerated:request mode=${mode.wireName} " +
                "inMemorySamples=${session.generatedPcm.size} " +
                "waveformSamples=${session.generatedWaveformPcm.size} " +
                "fileBacked=${!session.generatedPcmFilePath.isNullOrBlank()} " +
                "metadataSamples=${session.generatedAudioMetadata?.pcmSampleCount ?: 0}",
        )
        val request =
            requestFactory.buildGenerated(
                current = current,
                mode = mode,
                generatedPcm = session.generatedPcm,
                generatedPcmFilePath = session.generatedPcmFilePath,
                metadata = session.generatedAudioMetadata,
                fallbackFlashStyle = current.selectedFlashVoicingStyle,
            )
        launchGeneratedDecode(request)
    }

    private fun onDecodeSaved(
        current: AudioAppUiState,
        itemId: String,
    ) {
        val selectedSavedAudio =
            current.selectedSavedAudio
                ?.takeIf { it.item.itemId == itemId }
                ?: return
        if (selectedSavedAudio.isLoadingContent || selectedSavedAudio.isDecodingContent) {
            return
        }
        val mode = TransportModeOption.fromWireName(selectedSavedAudio.item.modeWireName)
        if (mode == null) {
            stateReducer.applySavedLoadFailure()
            return
        }
        val request = requestFactory.buildSaved(current, mode, selectedSavedAudio, current.selectedFlashVoicingStyle)
        launchSavedDecode(itemId, request, selectedSavedAudio)
    }

    private fun launchGeneratedDecode(request: DecodeRequest) {
        stateReducer.markBusy(request.mode)
        scope.launch {
            stateReducer.reduceGeneratedResult(
                request.mode,
                decodeRunner.execute(request),
            )
        }
    }

    private fun launchSavedDecode(
        itemId: String,
        request: DecodeRequest,
        selectedSavedAudio: SavedAudioPlaybackSelection,
    ) {
        stateReducer.markBusy(request.mode)
        stateReducer.markSavedDecodeStarted(itemId)
        scope.launch {
            val result = decodeRunner.execute(request)
            when (result) {
                is DecodeResult.ValidationFailure ->
                    stateReducer.reduceSavedValidationFailure(itemId, result.validationIssue)

                is DecodeResult.Success -> {
                    val flashSignalInfo = describeSavedFlashSignal(selectedSavedAudio, result.decoded.decodedPayload)
                    kotlinx.coroutines.withContext(workerDispatcher) {
                        savedAudioDecodeCacheGateway.write(
                            item = selectedSavedAudio.item,
                            metadata = selectedSavedAudio.metadata,
                            decodedPayload = result.decoded.decodedPayload,
                            followData = result.decoded.followData,
                            flashSignalInfo = flashSignalInfo,
                        )
                    }
                    stateReducer.reduceSavedSuccess(
                        itemId = itemId,
                        mode = request.mode,
                        decoded = result.decoded,
                        flashSignalInfo = flashSignalInfo,
                    )
                }
            }
        }
    }

    private fun describeSavedFlashSignal(
        savedAudio: SavedAudioPlaybackSelection,
        decodedPayload: DecodedPayloadViewData,
    ): FlashSignalInfo {
        val metadata = savedAudio.metadata ?: return FlashSignalInfo.Empty
        if (metadata.mode != TransportModeOption.Flash) {
            return FlashSignalInfo.Empty
        }
        val resolvedText = decodedPayload.text.takeIf { decodedPayload.hasTextResult && it.isNotBlank() } ?: return FlashSignalInfo.Empty
        val style = savedAudio.item.flashVoicingStyle ?: metadata.flashVoicingStyle ?: return FlashSignalInfo.Empty
        return audioCodecGateway.describeFlashSignal(
            resolvedText,
            savedAudio.sampleRateHz,
            metadata.frameSamples,
            style.signalProfileValue,
            style.voicingFlavorValue,
        )
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

private class DecodeRequestFactory(
    private val sampleRateHz: Int,
    private val frameSamples: Int,
) {
    fun buildGenerated(
        current: AudioAppUiState,
        mode: TransportModeOption,
        generatedPcm: ShortArray,
        generatedPcmFilePath: String?,
        metadata: com.bag.audioandroid.domain.GeneratedAudioMetadata?,
        fallbackFlashStyle: FlashVoicingStyleOption,
    ): DecodeRequest {
        val segmentedPcm =
            when {
                generatedPcm.isNotEmpty() ->
                    metadata?.segmentSampleCounts?.let { splitPcmIntoSegments(generatedPcm, it) }
                !generatedPcmFilePath.isNullOrBlank() ->
                    metadata?.let {
                        readPcmSegmentsFromFile(
                            generatedPcmFilePath,
                            it.segmentSampleCounts.takeIf { counts -> counts.isNotEmpty() } ?: listOf(it.pcmSampleCount),
                        )
                    }
                else -> null
            }
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "decodeRequest:generated mode=${mode.wireName} inMemorySamples=${generatedPcm.size} fileBacked=${!generatedPcmFilePath
                .isNullOrBlank()} metadataSamples=${metadata?.pcmSampleCount ?: 0} segmentedCount=${segmentedPcm?.size ?: 0}",
        )
        return DecodeRequest(
            mode = mode,
            generatedPcm = generatedPcm,
            generatedPcmFilePath = generatedPcmFilePath,
            sampleRateHz = sampleRateHz,
            frameSamples = metadata?.frameSamples ?: frameSamples,
            segmentedPcm = segmentedPcm,
            flashPresets =
                flashPresetCandidates(
                    mode = mode,
                    preferred = current.sessions.getValue(mode).generatedFlashVoicingStyle,
                    fallback = fallbackFlashStyle,
                ),
            expectedPayloadByteCount = metadata?.payloadByteCount,
        )
    }

    fun buildSaved(
        current: AudioAppUiState,
        mode: TransportModeOption,
        savedAudio: SavedAudioPlaybackSelection,
        fallbackFlashStyle: FlashVoicingStyleOption,
    ): DecodeRequest {
        val segmentedPcm =
            when {
                savedAudio.pcm.isNotEmpty() ->
                    savedAudio.metadata?.segmentSampleCounts?.let {
                        splitPcmIntoSegments(savedAudio.pcm, it)
                    }
                !savedAudio.pcmFilePath.isNullOrBlank() ->
                    savedAudio.metadata?.let {
                        readPcmSegmentsFromFile(
                            savedAudio.pcmFilePath,
                            it.segmentSampleCounts.takeIf { counts -> counts.isNotEmpty() } ?: listOf(it.pcmSampleCount),
                        )
                    }
                else -> null
            }
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "decodeRequest:saved mode=${mode.wireName} " +
                "itemId=${savedAudio.item.itemId} " +
                "inMemorySamples=${savedAudio.pcm.size} " +
                "fileBacked=${!savedAudio.pcmFilePath.isNullOrBlank()} " +
                "metadataSamples=${savedAudio.metadata?.pcmSampleCount ?: 0} " +
                "segmentedCount=${segmentedPcm?.size ?: 0}",
        )
        return DecodeRequest(
            mode = mode,
            generatedPcm = savedAudio.pcm,
            generatedPcmFilePath = savedAudio.pcmFilePath,
            sampleRateHz = savedAudio.sampleRateHz,
            frameSamples = savedAudio.metadata?.frameSamples ?: frameSamples,
            segmentedPcm = segmentedPcm,
            flashPresets =
                flashPresetCandidates(
                    mode = mode,
                    preferred = savedAudio.item.flashVoicingStyle,
                    fallback = fallbackFlashStyle,
                ),
            expectedPayloadByteCount = savedAudio.metadata?.payloadByteCount,
        )
    }

    private fun flashPresetCandidates(
        mode: TransportModeOption,
        preferred: FlashVoicingStyleOption?,
        fallback: FlashVoicingStyleOption,
    ): List<FlashVoicingStyleOption> {
        if (mode != TransportModeOption.Flash) {
            return listOf(FlashVoicingStyleOption.Standard)
        }
        val ordered = LinkedHashSet<FlashVoicingStyleOption>()
        preferred?.let(ordered::add)
        ordered += fallback
        ordered += FlashVoicingStyleOption.entries
        return ordered.toList()
    }
}

private class DecodeRunner(
    private val audioCodecGateway: AudioCodecGateway,
    private val workerDispatcher: CoroutineDispatcher,
) {
    suspend fun execute(request: DecodeRequest): DecodeResult =
        kotlinx.coroutines.withContext(workerDispatcher) {
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "decodeRunner:start mode=${request.mode.wireName} " +
                    "inMemorySamples=${request.generatedPcm.size} " +
                    "fileBacked=${!request.generatedPcmFilePath.isNullOrBlank()} " +
                    "segmentedCount=${request.segmentedPcm?.size ?: 0} " +
                    "sampleRate=${request.sampleRateHz} " +
                    "frameSamples=${request.frameSamples}",
            )
            val validationIssue =
                audioCodecGateway.validateDecodeConfig(
                    request.sampleRateHz,
                    request.frameSamples,
                    request.mode.nativeValue,
                    request.flashPresets.first().signalProfileValue,
                    request.flashPresets.first().voicingFlavorValue,
                )
            if (validationIssue != BagApiCodes.VALIDATION_OK) {
                return@withContext DecodeResult.ValidationFailure(validationIssue)
            }

            val decoded = decodeWithFallback(request)
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "decodeRunner:done mode=${request.mode.wireName} decodedStatus=${decoded.decodedPayload.textDecodeStatusCode} followAvailable=${decoded.followData.followAvailable}",
            )

            DecodeResult.Success(decoded)
        }

    private fun decodeWithFallback(request: DecodeRequest): DecodedAudioPayloadResult {
        if (request.mode != TransportModeOption.Flash) {
            return decodeWithPreset(request, request.flashPresets.first())
        }
        val attempts = mutableListOf<DecodeAttempt>()
        request.flashPresets.forEach { preset ->
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "decodeRunner:attempt mode=${request.mode.wireName} preset=${preset.id} expectedPayloadBytes=${request.expectedPayloadByteCount ?: -1}",
            )
            val attempt =
                DecodeAttempt(
                    preset = preset,
                    result = decodeWithPreset(request, preset),
                )
            attempts += attempt
            if (isStrongDecodeMatch(attempt, request.expectedPayloadByteCount)) {
                return attempt.result
            }
        }
        return attempts
            .maxWithOrNull(compareBy<DecodeAttempt> { scoreDecodeAttempt(it, request.expectedPayloadByteCount) })
            ?.result
            ?: decodeWithPreset(request, FlashVoicingStyleOption.Standard)
    }

    private fun decodeWithPreset(
        request: DecodeRequest,
        preset: FlashVoicingStyleOption,
    ): DecodedAudioPayloadResult =
        request.segmentedPcm?.let { segmentedPcm ->
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "decodeRunner:segmented mode=${request.mode.wireName} segments=${segmentedPcm.size} preset=${preset.id}",
            )
            mergeSegmentedDecodedPayloadResults(
                segmentedPcm.map { segmentPcm ->
                    audioCodecGateway.decodeGeneratedPcm(
                        segmentPcm,
                        request.sampleRateHz,
                        request.frameSamples,
                        request.mode.nativeValue,
                        preset.signalProfileValue,
                        preset.voicingFlavorValue,
                    )
                },
            )
        }
            ?: audioCodecGateway.decodeGeneratedPcm(
                request.generatedPcm,
                request.sampleRateHz,
                request.frameSamples,
                request.mode.nativeValue,
                preset.signalProfileValue,
                preset.voicingFlavorValue,
            )

    private fun scoreDecodeAttempt(
        attempt: DecodeAttempt,
        expectedPayloadByteCount: Int?,
    ): Int {
        val payloadByteCount =
            attempt.result.decodedPayload.rawBytesHex
                .split(' ')
                .filter { it.isNotBlank() }
                .size
        val payloadMatchBonus =
            if (expectedPayloadByteCount != null && payloadByteCount == expectedPayloadByteCount) {
                100
            } else {
                0
            }
        val textBonus =
            if (attempt.result.decodedPayload.hasTextResult) {
                10
            } else {
                0
            }
        val rawBonus =
            if (attempt.result.decodedPayload.rawPayloadAvailable) {
                1
            } else {
                0
            }
        return payloadMatchBonus + textBonus + rawBonus
    }

    private fun isStrongDecodeMatch(
        attempt: DecodeAttempt,
        expectedPayloadByteCount: Int?,
    ): Boolean {
        if (!attempt.result.decodedPayload.hasTextResult) {
            return false
        }
        if (expectedPayloadByteCount == null) {
            return true
        }
        val payloadByteCount =
            attempt.result.decodedPayload.rawBytesHex
                .split(' ')
                .count { it.isNotBlank() }
        return payloadByteCount == expectedPayloadByteCount
    }
}

private class DecodeStateReducer(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val uiTextMapper: BagUiTextMapper,
) {
    fun applyNoGeneratedAudio(mode: TransportModeOption) {
        sessionStateStore.updateSession(mode) {
            it.copy(statusText = UiText.Resource(R.string.status_no_audio_for_mode))
        }
    }

    fun applySavedLoadFailure() {
        sessionStateStore.updateCurrentSession {
            it.copy(statusText = UiText.Resource(R.string.status_saved_audio_load_failed))
        }
    }

    fun markBusy(mode: TransportModeOption) {
        sessionStateStore.updateCurrentSession {
            it.copy(
                isCodecBusy = true,
                encodeProgress = null,
                encodePhase = null,
                statusText =
                    UiText.Resource(
                        R.string.status_mode_audio_decoding,
                        listOf(mode.wireName),
                    ),
            )
        }
    }

    fun reduceGeneratedResult(
        mode: TransportModeOption,
        result: DecodeResult,
    ) {
        when (result) {
            is DecodeResult.ValidationFailure -> applyValidationFailure(result.validationIssue)
            is DecodeResult.Success -> applyGeneratedSuccess(mode, result.decoded)
        }
    }

    fun reduceSavedResult(
        itemId: String,
        mode: TransportModeOption,
        result: DecodeResult,
    ) {
        when (result) {
            is DecodeResult.ValidationFailure -> applyValidationFailure(result.validationIssue)
            is DecodeResult.Success -> {
                val status = decodeStatusText(mode, result.decoded.decodedPayload)
                applySavedSuccess(itemId, result.decoded, FlashSignalInfo.Empty, status)
            }
        }
    }

    fun markSavedDecodeStarted(itemId: String) {
        uiState.update { state ->
            val selected =
                state.selectedSavedAudio
                    ?.takeIf { it.item.itemId == itemId }
                    ?: return@update state
            state.copy(
                selectedSavedAudio =
                    selected.copy(
                        isDecodingContent = true,
                    ),
            )
        }
    }

    fun reduceSavedValidationFailure(
        itemId: String,
        validationIssue: Int,
    ) {
        uiState.update { state ->
            val selected =
                state.selectedSavedAudio
                    ?.takeIf { it.item.itemId == itemId }
                    ?: return@update state
            state.copy(
                selectedSavedAudio =
                    selected.copy(
                        isDecodingContent = false,
                    ),
            )
        }
        applyValidationFailure(validationIssue)
    }

    fun reduceSavedSuccess(
        itemId: String,
        mode: TransportModeOption,
        decoded: DecodedAudioPayloadResult,
        flashSignalInfo: FlashSignalInfo,
    ) {
        val status = decodeStatusText(mode, decoded.decodedPayload)
        applySavedSuccess(itemId, decoded, flashSignalInfo, status)
    }

    private fun applyGeneratedSuccess(
        mode: TransportModeOption,
        decoded: DecodedAudioPayloadResult,
    ) {
        val status = decodeStatusText(mode, decoded.decodedPayload)
        sessionStateStore.updateSession(mode) {
            it.copy(
                decodedPayload = decoded.decodedPayload,
                followData = decoded.followData,
                statusText = status,
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
            )
        }
    }

    private fun applySavedSuccess(
        itemId: String,
        decoded: DecodedAudioPayloadResult,
        flashSignalInfo: FlashSignalInfo,
        status: UiText,
    ) {
        uiState.update { state ->
            val selected =
                state.selectedSavedAudio
                    ?.takeIf { it.item.itemId == itemId }
                    ?: return@update state
            state.copy(
                selectedSavedAudio =
                    selected.copy(
                        decodedPayload = decoded.decodedPayload,
                        followData = decoded.followData,
                        flashSignalInfo = flashSignalInfo,
                        needsDecodedContent = false,
                        isDecodingContent = false,
                    ),
            )
        }
        applyIdleStatus(status)
    }

    private fun applyValidationFailure(validationIssue: Int) {
        applyIdleStatus(uiTextMapper.validationIssue(validationIssue))
    }

    private fun applyIdleStatus(statusText: UiText) {
        sessionStateStore.updateCurrentSession {
            it.copy(
                statusText = statusText,
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
            )
        }
    }

    private fun decodeStatusText(
        mode: TransportModeOption,
        decodedPayload: DecodedPayloadViewData,
    ): UiText =
        if (
            decodedPayload.textDecodeStatusCode == BagDecodeContentCodes.STATUS_INTERNAL_ERROR ||
            (!decodedPayload.rawPayloadAvailable && !decodedPayload.hasTextResult)
        ) {
            uiTextMapper.errorCode(BagApiCodes.ERROR_INTERNAL)
        } else {
            UiText.Resource(
                R.string.status_mode_decode_completed,
                listOf(mode.wireName),
            )
        }
}

private data class DecodeRequest(
    val mode: TransportModeOption,
    val generatedPcm: ShortArray,
    val generatedPcmFilePath: String? = null,
    val sampleRateHz: Int,
    val frameSamples: Int,
    val flashPresets: List<FlashVoicingStyleOption>,
    val segmentedPcm: List<ShortArray>? = null,
    val expectedPayloadByteCount: Int? = null,
)

private data class DecodeAttempt(
    val preset: FlashVoicingStyleOption,
    val result: DecodedAudioPayloadResult,
)

private sealed interface DecodeResult {
    data class ValidationFailure(
        val validationIssue: Int,
    ) : DecodeResult

    data class Success(
        val decoded: DecodedAudioPayloadResult,
    ) : DecodeResult
}
