package com.bag.audioandroid

import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.EncodedAudioPayloadResult
import com.bag.audioandroid.domain.FlashSignalInfo

object NativeBagBridge {
    init {
        System.loadLibrary("audio_android_jni")
    }

    external fun nativeEncodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): ShortArray

    external fun nativeValidateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int

    external fun nativeStartEncodeTextJob(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Long

    external fun nativePollEncodeTextJob(handle: Long): FloatArray

    external fun nativeTakeEncodeTextJobResult(handle: Long): EncodedAudioPayloadResult

    external fun nativeBuildEncodeFollowData(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): EncodedAudioPayloadResult

    external fun nativeDescribeFlashSignal(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): FlashSignalInfo

    external fun nativeCancelEncodeTextJob(handle: Long): Int

    external fun nativeDestroyEncodeTextJob(handle: Long)

    external fun nativeDecodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): DecodedAudioPayloadResult

    external fun nativeValidateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int

    external fun nativeGetCoreVersion(): String
}
