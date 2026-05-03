package com.bag.audioandroid.ui.state

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.UiText

data class ModeAudioSessionState(
    val inputText: String = "",
    val sampleInputId: String? = null,
    val sampleShuffleState: SampleInputShuffleState? = null,
    val generatedPcm: ShortArray = shortArrayOf(),
    val generatedWaveformPcm: ShortArray = shortArrayOf(),
    val generatedPcmFilePath: String? = null,
    val generatedAudioMetadata: GeneratedAudioMetadata? = null,
    val generatedWavAudioInfo: WavAudioInfo = WavAudioInfo.Empty,
    val generatedFlashVoicingStyle: FlashVoicingStyleOption? = null,
    val generatedFlashSignalInfo: FlashSignalInfo = FlashSignalInfo.Empty,
    val generatedContentRevision: Long = 0L,
    val decodedPayload: DecodedPayloadViewData = DecodedPayloadViewData.Empty,
    val followData: PayloadFollowViewData = PayloadFollowViewData.Empty,
    val statusText: UiText = UiText.Resource(R.string.status_ready_to_encode),
    val isCodecBusy: Boolean = false,
    val encodeProgress: Float? = null,
    val encodePhase: AudioEncodePhase? = null,
    val isEncodeCancelling: Boolean = false,
    val playback: PlaybackUiState = PlaybackUiState(),
    val playbackSpeed: Float = PlaybackSpeedOption.default.speed,
)

data class SampleInputShuffleState(
    val flavor: SampleFlavor,
    val length: SampleInputLengthOption,
    // We keep the full shuffled round plus a cursor so each sample can be shown once
    // before the next round begins, while still letting every new round feel different.
    val shuffledSampleIds: List<String>,
    val nextSampleIndex: Int,
    val lastPresentedSampleId: String?,
) {
    fun matches(
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): Boolean = this.flavor == flavor && this.length == length
}
