package com.bag.audioandroid

object NativeBagBridge {
    init {
        System.loadLibrary("audio_android_jni")
    }

    external fun nativeEncodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int
    ): ShortArray

    external fun nativeValidateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int
    ): String

    external fun nativeDecodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int
    ): String

    external fun nativeValidateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int
    ): String

    external fun nativeErrorCodeMessage(code: Int): String

    external fun nativeGetCoreVersion(): String
}
