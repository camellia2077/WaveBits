package com.bag.audioandroid

import com.bag.audioandroid.domain.DecodedAudioData
import com.bag.audioandroid.domain.GeneratedAudioMetadata

object NativeAudioIoBridge {
    init {
        System.loadLibrary("audio_android_jni")
    }

    external fun nativeEncodeMonoPcm16ToWavBytes(
        sampleRateHz: Int,
        pcm: ShortArray,
        metadata: GeneratedAudioMetadata?
    ): ByteArray

    external fun nativeDecodeMonoPcm16WavBytes(
        wavBytes: ByteArray
    ): DecodedAudioData
}
