package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow

internal class AudioSessionCodecActions(
    uiState: MutableStateFlow<AudioAppUiState>,
    scope: CoroutineScope,
    audioCodecGateway: AudioCodecGateway,
    sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    sampleRateHz: Int,
    frameSamples: Int,
    stopPlayback: () -> Unit,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val encodeActions = AudioSessionEncodeActions(
        uiState = uiState,
        scope = scope,
        audioCodecGateway = audioCodecGateway,
        sessionStateStore = sessionStateStore,
        uiTextMapper = uiTextMapper,
        playbackRuntimeGateway = playbackRuntimeGateway,
        sampleRateHz = sampleRateHz,
        frameSamples = frameSamples,
        stopPlayback = stopPlayback,
        workerDispatcher = workerDispatcher
    )

    private val decodeActions = AudioSessionDecodeActions(
        uiState = uiState,
        scope = scope,
        audioCodecGateway = audioCodecGateway,
        sessionStateStore = sessionStateStore,
        uiTextMapper = uiTextMapper,
        sampleRateHz = sampleRateHz,
        frameSamples = frameSamples,
        workerDispatcher = workerDispatcher
    )

    fun onEncode() {
        encodeActions.onEncode()
    }

    fun onCancelEncode() {
        encodeActions.onCancelEncode()
    }

    fun onDecode() {
        decodeActions.onDecode()
    }
}
