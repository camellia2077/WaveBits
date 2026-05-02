package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

internal class AudioPlaybackUiStateSync(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val playbackSourceCoordinator: PlaybackSourceCoordinator,
    private val playbackSessionReducer: PlaybackSessionReducer,
    private val sampleRateHz: Int,
) {
    private val lastProgressUiUpdateNanosBySource = ConcurrentHashMap<String, Long>()

    fun updatePlaybackState(
        source: AudioPlaybackSource,
        transform: (PlaybackUiState) -> PlaybackUiState,
    ) {
        when (source) {
            is AudioPlaybackSource.Generated ->
                sessionStateStore.updateSession(source.mode) {
                    it.copy(playback = transform(it.playback))
                }

            is AudioPlaybackSource.Saved ->
                uiState.update { state ->
                    val selectedSavedAudio =
                        state.selectedSavedAudio
                            ?.takeIf { it.item.itemId == source.itemId }
                            ?: return@update state
                    state.copy(selectedSavedAudio = selectedSavedAudio.copy(playback = transform(selectedSavedAudio.playback)))
                }
        }
    }

    fun setCurrentStatusText(statusText: UiText) {
        sessionStateStore.updateCurrentSession {
            it.copy(statusText = statusText)
        }
    }

    fun playbackStatusPlaying(source: AudioPlaybackSource): UiText =
        when (source) {
            is AudioPlaybackSource.Generated -> playbackSessionReducer.playingStatus(source.mode)
            is AudioPlaybackSource.Saved ->
                UiText.Resource(
                    R.string.status_playing_saved_audio,
                    listOf(
                        uiState.value.selectedSavedAudio
                            ?.item
                            ?.displayName
                            .orEmpty(),
                    ),
                )
        }

    fun applyPlaybackProgress(
        source: AudioPlaybackSource,
        playedSamples: Int,
        totalSamples: Int,
    ) {
        if (!shouldPublishProgressToUi(source, playedSamples, totalSamples)) {
            return
        }
        updatePlaybackState(source) { currentPlayback ->
            val playbackBase =
                if (currentPlayback.totalSamples == 0 && totalSamples > 0) {
                    val resolvedSampleRateHz =
                        when (source) {
                            is AudioPlaybackSource.Generated -> sampleRateHz
                            is AudioPlaybackSource.Saved -> uiState.value.selectedSavedAudio?.sampleRateHz ?: sampleRateHz
                        }
                    playbackRuntimeGateway.load(totalSamples, resolvedSampleRateHz)
                } else {
                    currentPlayback
                }
            playbackRuntimeGateway.progress(playbackBase, playedSamples)
        }
    }

    fun applyPlaybackCompleted(source: AudioPlaybackSource) {
        resetProgressThrottle(source)
        updatePlaybackState(source) { playbackRuntimeGateway.completed(it) }
        setCurrentStatusText(UiText.Resource(R.string.status_playback_completed))
    }

    fun applyPlaybackFailed(sourceKey: String) {
        val source = playbackSourceCoordinator.sourceForKey(uiState.value, sourceKey) ?: return
        resetProgressThrottle(source)
        updatePlaybackState(source) { playbackRuntimeGateway.failed(it) }
        setCurrentStatusText(UiText.Resource(R.string.status_playback_failed))
    }

    fun applyPlaybackStopped(source: AudioPlaybackSource) {
        resetProgressThrottle(source)
        updatePlaybackState(source) { playbackRuntimeGateway.stopped(it) }
        setCurrentStatusText(UiText.Resource(R.string.status_playback_stopped))
    }

    private fun shouldPublishProgressToUi(
        source: AudioPlaybackSource,
        playedSamples: Int,
        totalSamples: Int,
    ): Boolean {
        val sourceKey = progressThrottleKey(source)
        val nowNanos = System.nanoTime()
        val lastUpdateNanos = lastProgressUiUpdateNanosBySource[sourceKey]
        val isBoundaryProgress = playedSamples <= 0 || (totalSamples > 0 && playedSamples >= totalSamples)
        val shouldPublish =
            isBoundaryProgress ||
                lastUpdateNanos == null ||
                nowNanos - lastUpdateNanos >= PlaybackProgressUiUpdateIntervalNanos
        if (shouldPublish) {
            // Playback polling can stay precise; only global Compose state is throttled
            // to the same rough 60 Hz cadence as the playback loop.
            lastProgressUiUpdateNanosBySource[sourceKey] = nowNanos
        }
        return shouldPublish
    }

    private fun resetProgressThrottle(source: AudioPlaybackSource) {
        lastProgressUiUpdateNanosBySource.remove(progressThrottleKey(source))
    }

    private fun progressThrottleKey(source: AudioPlaybackSource): String =
        when (source) {
            is AudioPlaybackSource.Generated -> "generated:${source.mode.wireName}"
            is AudioPlaybackSource.Saved -> "saved:${source.itemId}"
        }

    private companion object {
        const val PlaybackProgressUiUpdateIntervalNanos = 16_000_000L
    }
}
