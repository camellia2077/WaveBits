package com.bag.audioandroid.ui.model

sealed interface MiniPlayerUiModel {
    val durationMs: Long

    data class Generated(
        val mode: TransportModeOption,
        val flashVoicingStyle: FlashVoicingStyleOption?,
        override val durationMs: Long
    ) : MiniPlayerUiModel

    data class Saved(
        val displayName: String,
        val modeWireName: String,
        val flashVoicingStyle: FlashVoicingStyleOption?,
        override val durationMs: Long
    ) : MiniPlayerUiModel
}
