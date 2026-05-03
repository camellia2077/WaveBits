package com.bag.audioandroid.domain

import androidx.annotation.Keep

@Keep
data class FlashSignalInfo(
    val lowCarrierHz: String = "",
    val highCarrierHz: String = "",
    val bitDurationSamples: String = "",
    val payloadSilence: String = "",
    val decodePath: String = "",
    val available: Boolean = false,
) {
    companion object {
        val Empty = FlashSignalInfo()
    }
}
