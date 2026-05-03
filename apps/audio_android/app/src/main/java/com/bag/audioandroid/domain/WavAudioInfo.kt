package com.bag.audioandroid.domain

import androidx.annotation.Keep

@Keep
data class WavAudioInfo(
    val wavStatusCode: Int,
    val sampleRateHz: Int = 0,
    val channels: Int = 0,
    val bitsPerSample: Int = 0,
    val pcmSampleCount: Long = 0L,
    val dataByteCount: Long = 0L,
    val fileByteCount: Long = 0L,
    val durationMs: Long = 0L,
) {
    val isWavSuccess: Boolean
        get() = wavStatusCode == AudioIoWavCodes.STATUS_OK

    companion object {
        val Empty = WavAudioInfo(wavStatusCode = AudioIoWavCodes.STATUS_INVALID_ARGUMENT)
    }
}
