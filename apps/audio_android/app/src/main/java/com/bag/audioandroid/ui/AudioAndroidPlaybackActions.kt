package com.bag.audioandroid.ui

import com.bag.audioandroid.audio.AudioPlaybackCoordinator
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioAndroidPlaybackActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    scope: CoroutineScope,
    private val sessionStateStore: AudioSessionStateStore,
    private val playbackCoordinator: AudioPlaybackCoordinator,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val playbackSourceCoordinator: PlaybackSourceCoordinator,
    playbackSessionReducer: PlaybackSessionReducer,
    sampleRateHz: Int,
    onPlaybackCompleted: (AudioPlaybackSource) -> Boolean,
    followDataWindowActions: FollowDataWindowActions? = null,
    flashVisualWindowActions: FlashVisualWindowActions? = null,
) {
    private val playbackUiStateSync =
        AudioPlaybackUiStateSync(
            uiState = uiState,
            sessionStateStore = sessionStateStore,
            playbackRuntimeGateway = playbackRuntimeGateway,
            playbackSourceCoordinator = playbackSourceCoordinator,
            playbackSessionReducer = playbackSessionReducer,
            sampleRateHz = sampleRateHz,
            followDataWindowActions = followDataWindowActions,
            flashVisualWindowActions = flashVisualWindowActions,
        )
    private val commandActions =
        AudioPlaybackCommandActions(
            uiState = uiState,
            scope = scope,
            playbackCoordinator = playbackCoordinator,
            playbackRuntimeGateway = playbackRuntimeGateway,
            playbackSourceCoordinator = playbackSourceCoordinator,
            playbackUiStateSync = playbackUiStateSync,
            onPlaybackCompleted = onPlaybackCompleted,
        )
    private val scrubActions =
        AudioPlaybackScrubActions(
            uiState = uiState,
            playbackCoordinator = playbackCoordinator,
            playbackRuntimeGateway = playbackRuntimeGateway,
            playbackSourceCoordinator = playbackSourceCoordinator,
            playbackUiStateSync = playbackUiStateSync,
            followDataWindowActions = followDataWindowActions,
            flashVisualWindowActions = flashVisualWindowActions,
            startPlaybackFromTarget = commandActions::startPlaybackFromTarget,
        )

    fun onTogglePlayback() {
        commandActions.onTogglePlayback()
    }

    fun onScrubStarted() {
        scrubActions.onScrubStarted()
    }

    fun onScrubChanged(targetSamples: Int) {
        scrubActions.onScrubChanged(targetSamples)
    }

    fun onScrubFinished() {
        scrubActions.onScrubFinished()
    }

    fun onScrubCanceled() {
        scrubActions.onScrubCanceled()
    }

    fun stopPlayback() {
        commandActions.stopPlayback()
    }

    fun release() {
        commandActions.release()
    }

    fun playCurrentFromStart(): Boolean = commandActions.playCurrentFromStart()

    fun onPlaybackSpeedSelected(playbackSpeed: Float) {
        val current = uiState.value
        when (val source = current.currentPlaybackSource) {
            is AudioPlaybackSource.Generated ->
                sessionStateStore.updateSession(source.mode) {
                    it.copy(playbackSpeed = playbackSpeed)
                }

            is AudioPlaybackSource.Saved ->
                uiState.update { state ->
                    val selectedSavedAudio =
                        state.selectedSavedAudio
                            ?.takeIf { it.item.itemId == source.itemId }
                            ?: return@update state
                    state.copy(
                        selectedSavedAudio = selectedSavedAudio.copy(playbackSpeed = playbackSpeed),
                    )
                }
        }
        playbackCoordinator.setPlaybackSpeed(
            sourceKey = playbackSourceCoordinator.sourceKey(current.currentPlaybackSource),
            playbackSpeed = playbackSpeed,
        )
    }
}
