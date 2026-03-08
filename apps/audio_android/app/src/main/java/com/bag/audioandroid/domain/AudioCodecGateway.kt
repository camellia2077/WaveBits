package com.bag.audioandroid.domain

interface AudioCodecGateway {
    fun validateEncodeRequest(text: String, sampleRateHz: Int, frameSamples: Int, mode: Int): String
    fun encodeTextToPcm(text: String, sampleRateHz: Int, frameSamples: Int, mode: Int): ShortArray
    fun validateDecodeConfig(sampleRateHz: Int, frameSamples: Int, mode: Int): String
    fun decodeGeneratedPcm(pcm: ShortArray, sampleRateHz: Int, frameSamples: Int, mode: Int): String
    fun errorCodeMessage(code: Int): String
    fun getCoreVersion(): String
}
