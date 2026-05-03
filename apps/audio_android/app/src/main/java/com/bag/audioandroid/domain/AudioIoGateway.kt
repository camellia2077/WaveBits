package com.bag.audioandroid.domain

interface AudioIoGateway {
    fun encodeMonoPcm16ToWavBytes(
        sampleRateHz: Int,
        pcm: ShortArray,
        metadata: GeneratedAudioMetadata? = null,
    ): ByteArray

    fun decodeMonoPcm16WavBytes(wavBytes: ByteArray): DecodedAudioData

    fun probeMonoPcm16WavBytes(wavBytes: ByteArray): WavAudioInfo
}
