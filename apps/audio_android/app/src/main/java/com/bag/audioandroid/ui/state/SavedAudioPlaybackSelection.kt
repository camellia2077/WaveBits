package com.bag.audioandroid.ui.state

import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.DecodedPayloadViewData

data class SavedAudioPlaybackSelection(
    val item: SavedAudioItem,
    val pcm: ShortArray,
    val waveformPcm: ShortArray = pcm,
    val pcmFilePath: String? = null,
    val sampleRateHz: Int,
    val metadata: GeneratedAudioMetadata? = null,
    val playback: PlaybackUiState,
    val playbackSpeed: Float = com.bag.audioandroid.ui.model.PlaybackSpeedOption.default.speed,
    val decodedPayload: DecodedPayloadViewData = DecodedPayloadViewData.Empty,
    val followData: PayloadFollowViewData = PayloadFollowViewData.Empty,
)
