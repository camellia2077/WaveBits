package com.bag.audioandroid.domain

import androidx.annotation.Keep

@Keep
data class EncodedAudioPayloadResult(
    val pcm: ShortArray = shortArrayOf(),
    val rawBytesHex: String = "",
    val rawBitsBinary: String = "",
    val followData: PayloadFollowViewData = PayloadFollowViewData.Empty,
    val terminalCode: Int = BagApiCodes.ERROR_OK,
)
