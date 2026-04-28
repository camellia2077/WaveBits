package com.bag.audioandroid.data

import com.bag.audioandroid.NativeBagBridge
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.EncodedAudioPayloadResult
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

class NativeAudioCodecGateway : AudioCodecGateway {
    override fun validateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int =
        NativeBagBridge.nativeValidateEncodeRequest(
            text,
            sampleRateHz,
            frameSamples,
            mode,
            flashSignalProfile,
            flashVoicingFlavor,
        )

    override suspend fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (EncodeProgressUpdate) -> Unit,
    ): EncodeAudioResult {
        val handle =
            NativeBagBridge.nativeStartEncodeTextJob(
                text,
                sampleRateHz,
                frameSamples,
                mode,
                flashSignalProfile,
                flashVoicingFlavor,
            )
        handle.toStartFailureResultOrNull()?.let { return it }

        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                val snapshot = NativeBagBridge.nativePollEncodeTextJob(handle).toEncodeJobSnapshot()
                onProgress(
                    EncodeProgressUpdate(
                        phase = snapshot.phase,
                        progress0To1 = snapshot.progress0To1,
                    ),
                )
                when (snapshot.state) {
                    EncodeJobState.Queued,
                    EncodeJobState.Running,
                    -> delay(ENCODE_JOB_POLL_INTERVAL_MS)

                    EncodeJobState.Succeeded ->
                        return NativeBagBridge
                            .nativeTakeEncodeTextJobResult(handle)
                            .toEncodeSuccessOrFailureResult()

                    EncodeJobState.Failed -> return EncodeAudioResult.Failed(snapshot.terminalCode)
                    EncodeJobState.Cancelled -> return EncodeAudioResult.Cancelled
                }
            }
        } catch (cancelled: CancellationException) {
            NativeBagBridge.nativeCancelEncodeTextJob(handle)
            throw cancelled
        } finally {
            NativeBagBridge.nativeCancelEncodeTextJob(handle)
            NativeBagBridge.nativeDestroyEncodeTextJob(handle)
        }
    }

    override suspend fun buildEncodeFollowData(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): EncodedAudioPayloadResult {
        val result =
            NativeBagBridge.nativeBuildEncodeFollowData(
                text,
                sampleRateHz,
                frameSamples,
                mode,
                flashSignalProfile,
                flashVoicingFlavor,
            )
        return result.copy(
            followData = result.followData.normalizeDesignTokens(),
        )
    }

    override fun validateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int =
        NativeBagBridge.nativeValidateDecodeConfig(
            sampleRateHz,
            frameSamples,
            mode,
            flashSignalProfile,
            flashVoicingFlavor,
        )

    override fun decodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): DecodedAudioPayloadResult {
        val result =
            NativeBagBridge.nativeDecodeGeneratedPcm(
                pcm,
                sampleRateHz,
                frameSamples,
                mode,
                flashSignalProfile,
                flashVoicingFlavor,
            )
        return result.copy(
            followData = result.followData.normalizeDesignTokens(),
        )
    }

    override fun getCoreVersion(): String = NativeBagBridge.nativeGetCoreVersion()

    private companion object {
        const val ENCODE_JOB_POLL_INTERVAL_MS = 33L
    }
}

internal data class EncodeJobSnapshot(
    val state: EncodeJobState,
    val phase: AudioEncodePhase,
    val progress0To1: Float,
    val terminalCode: Int,
)

internal enum class EncodeJobState(
    val nativeValue: Int,
) {
    Queued(0),
    Running(1),
    Succeeded(2),
    Failed(3),
    Cancelled(4),
    ;

    companion object {
        fun fromNative(value: Int): EncodeJobState = entries.firstOrNull { it.nativeValue == value } ?: Failed
    }
}

internal fun FloatArray.toEncodeJobSnapshot(): EncodeJobSnapshot {
    val stateValue = getOrNull(0)?.toInt() ?: EncodeJobState.Failed.nativeValue
    val phaseValue = getOrNull(1)?.toInt() ?: AudioEncodePhase.Finalizing.nativeValue
    val progress = getOrNull(2)?.coerceIn(0f, 1f) ?: 0f
    val terminalCode = getOrNull(3)?.toInt() ?: BagApiCodes.ERROR_INTERNAL
    return EncodeJobSnapshot(
        state = EncodeJobState.fromNative(stateValue),
        phase = AudioEncodePhase.fromNative(phaseValue),
        progress0To1 = progress,
        terminalCode = terminalCode,
    )
}

internal fun Long.toStartFailureResultOrNull(): EncodeAudioResult? =
    if (this == 0L) {
        EncodeAudioResult.Failed(BagApiCodes.ERROR_INTERNAL)
    } else {
        null
    }

internal fun EncodedAudioPayloadResult.toEncodeSuccessOrFailureResult(): EncodeAudioResult =
    if (terminalCode != BagApiCodes.ERROR_OK) {
        EncodeAudioResult.Failed(terminalCode)
    } else if (pcm.isEmpty()) {
        EncodeAudioResult.Failed(BagApiCodes.ERROR_INTERNAL)
    } else {
        EncodeAudioResult.Success(
            pcm = pcm,
        )
    }
