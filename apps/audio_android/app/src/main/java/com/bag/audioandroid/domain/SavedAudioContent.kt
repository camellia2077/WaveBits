package com.bag.audioandroid.domain

data class SavedAudioContent(
    val item: SavedAudioItem,
    val pcm: ShortArray,
    val waveformPcm: ShortArray = pcm,
    val pcmFilePath: String? = null,
    val sampleRateHz: Int,
    val metadata: GeneratedAudioMetadata? = null,
)
