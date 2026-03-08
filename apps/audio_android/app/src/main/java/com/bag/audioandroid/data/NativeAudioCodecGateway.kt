package com.bag.audioandroid.data

import com.bag.audioandroid.NativeBagBridge
import com.bag.audioandroid.domain.AudioCodecGateway

class NativeAudioCodecGateway : AudioCodecGateway {
    override fun validateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int
    ): String = NativeBagBridge.nativeValidateEncodeRequest(text, sampleRateHz, frameSamples, mode)

    override fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int
    ): ShortArray = NativeBagBridge.nativeEncodeTextToPcm(text, sampleRateHz, frameSamples, mode)

    override fun validateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int
    ): String = NativeBagBridge.nativeValidateDecodeConfig(sampleRateHz, frameSamples, mode)

    override fun decodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int
    ): String = NativeBagBridge.nativeDecodeGeneratedPcm(pcm, sampleRateHz, frameSamples, mode)

    override fun errorCodeMessage(code: Int): String = NativeBagBridge.nativeErrorCodeMessage(code)

    override fun getCoreVersion(): String = NativeBagBridge.nativeGetCoreVersion()
}
