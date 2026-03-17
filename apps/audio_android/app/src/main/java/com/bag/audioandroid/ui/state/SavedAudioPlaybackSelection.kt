package com.bag.audioandroid.ui.state

import com.bag.audioandroid.domain.SavedAudioItem

data class SavedAudioPlaybackSelection(
    val item: SavedAudioItem,
    val pcm: ShortArray,
    val sampleRateHz: Int,
    val playback: PlaybackUiState,
    val decodedText: String = ""
)
