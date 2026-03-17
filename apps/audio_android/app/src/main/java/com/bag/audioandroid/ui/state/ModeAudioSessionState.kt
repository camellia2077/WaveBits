package com.bag.audioandroid.ui.state

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.UiText

data class ModeAudioSessionState(
    val inputText: String = "",
    val sampleInputId: String? = null,
    val generatedPcm: ShortArray = shortArrayOf(),
    val generatedAudioMetadata: GeneratedAudioMetadata? = null,
    val generatedFlashVoicingStyle: FlashVoicingStyleOption? = null,
    val resultText: String = "",
    val statusText: UiText = UiText.Resource(R.string.status_ready_to_encode),
    val isCodecBusy: Boolean = false,
    val encodeProgress: Float? = null,
    val encodePhase: AudioEncodePhase? = null,
    val isEncodeCancelling: Boolean = false,
    val playback: PlaybackUiState = PlaybackUiState()
)
