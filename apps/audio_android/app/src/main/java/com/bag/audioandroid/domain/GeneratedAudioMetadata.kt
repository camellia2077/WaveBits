package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption

data class GeneratedAudioMetadata(
    val version: Int = CURRENT_VERSION,
    val mode: TransportModeOption,
    val flashVoicingStyle: FlashVoicingStyleOption? = null,
    val createdAtIsoUtc: String,
    val durationMs: Long,
    val frameSamples: Int,
    val pcmSampleCount: Int,
    val appVersion: String,
    val coreVersion: String
) {
    companion object {
        const val CURRENT_VERSION = 3
    }
}
