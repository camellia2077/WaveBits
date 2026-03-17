package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.FlashVoicingStyleOption

data class SavedAudioItem(
    val itemId: String,
    val displayName: String,
    val uriString: String,
    val modeWireName: String,
    val durationMs: Long,
    val savedAtEpochSeconds: Long,
    val flashVoicingStyle: FlashVoicingStyleOption? = null
)
