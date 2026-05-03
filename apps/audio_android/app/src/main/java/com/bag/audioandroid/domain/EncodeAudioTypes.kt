package com.bag.audioandroid.domain

sealed interface EncodeAudioResult {
    data class Success(
        val pcm: ShortArray,
        val rawBytesHex: String = "",
        val rawBitsBinary: String = "",
        val followData: PayloadFollowViewData = PayloadFollowViewData.Empty,
        val flashSignalInfo: FlashSignalInfo = FlashSignalInfo.Empty,
    ) : EncodeAudioResult

    data object Cancelled : EncodeAudioResult

    data class Failed(
        val errorCode: Int,
    ) : EncodeAudioResult
}

enum class AudioEncodePhase(
    val nativeValue: Int,
) {
    PreparingInput(0),
    RenderingPcm(1),
    Postprocessing(2),
    Finalizing(3),
    ;

    companion object {
        fun fromNative(value: Int): AudioEncodePhase = entries.firstOrNull { it.nativeValue == value } ?: Finalizing
    }
}

data class EncodeProgressUpdate(
    val phase: AudioEncodePhase,
    val progress0To1: Float,
)
