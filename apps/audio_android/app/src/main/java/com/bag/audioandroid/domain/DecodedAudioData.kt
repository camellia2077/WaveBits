package com.bag.audioandroid.domain

data class DecodedAudioData(
    val statusCode: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val pcm: ShortArray,
    val metadata: GeneratedAudioMetadata? = null
) {
    val isSuccess: Boolean
        get() = statusCode == AudioIoCodes.STATUS_OK
}
