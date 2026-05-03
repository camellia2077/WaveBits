package com.bag.audioandroid.data

import com.bag.audioandroid.NativeAudioIoBridge
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.DecodedAudioData
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.WavAudioInfo

class NativeAudioIoGateway : AudioIoGateway {
    override fun encodeMonoPcm16ToWavBytes(
        sampleRateHz: Int,
        pcm: ShortArray,
        metadata: GeneratedAudioMetadata?,
    ): ByteArray = NativeAudioIoBridge.nativeEncodeMonoPcm16ToWavBytes(sampleRateHz, pcm, metadata)

    override fun decodeMonoPcm16WavBytes(wavBytes: ByteArray): DecodedAudioData =
        NativeAudioIoBridge.nativeDecodeMonoPcm16WavBytes(wavBytes)

    override fun probeMonoPcm16WavBytes(wavBytes: ByteArray): WavAudioInfo = NativeAudioIoBridge.nativeProbeMonoPcm16WavBytes(wavBytes)
}
