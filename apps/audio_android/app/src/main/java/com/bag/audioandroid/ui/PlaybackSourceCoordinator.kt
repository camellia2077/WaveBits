package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.PlaybackUiState

class PlaybackSourceCoordinator(
    private val generatedSampleRateHz: Int,
) {
    fun resolveTarget(state: AudioAppUiState): PlaybackTarget? =
        when (val source = state.currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> {
                val session = state.sessions.getValue(source.mode)
                val totalSamples =
                    session.generatedAudioMetadata?.pcmSampleCount?.takeIf { it > 0 }
                        ?: session.generatedPcm.size
                if (totalSamples <= 0 || (session.generatedPcm.isEmpty() && session.generatedPcmFilePath == null)) {
                    null
                } else {
                    PlaybackTarget(
                        source = source,
                        pcm = session.generatedPcm,
                        pcmFilePath = session.generatedPcmFilePath,
                        totalSamples = totalSamples,
                        sampleRateHz = generatedSampleRateHz,
                        playback = session.playback,
                        playbackSpeed = session.playbackSpeed,
                    )
                }
            }

            is AudioPlaybackSource.Saved -> {
                val selectedSavedAudio =
                    state.selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?: return null
                PlaybackTarget(
                    source = source,
                    pcm = selectedSavedAudio.pcm,
                    pcmFilePath = selectedSavedAudio.pcmFilePath,
                    totalSamples = selectedSavedAudio.metadata?.pcmSampleCount ?: selectedSavedAudio.pcm.size,
                    sampleRateHz = selectedSavedAudio.sampleRateHz,
                    playback = selectedSavedAudio.playback,
                    playbackSpeed = selectedSavedAudio.playbackSpeed,
                )
            }
        }

    fun sourceKey(source: AudioPlaybackSource): String =
        when (source) {
            is AudioPlaybackSource.Generated -> "generated:${source.mode.wireName}"
            is AudioPlaybackSource.Saved -> "saved:${source.itemId}"
        }

    fun sourceForKey(
        state: AudioAppUiState,
        sourceKey: String,
    ): AudioPlaybackSource? {
        val currentSource = state.currentPlaybackSource
        if (sourceKey(currentSource) == sourceKey) {
            return currentSource
        }

        val generatedMode =
            TransportModeOption.entries.firstOrNull {
                sourceKey(AudioPlaybackSource.Generated(it)) == sourceKey
            }
        if (generatedMode != null) {
            return AudioPlaybackSource.Generated(generatedMode)
        }

        val selectedSavedAudio =
            state.selectedSavedAudio
                ?.takeIf { sourceKey(AudioPlaybackSource.Saved(it.item.itemId)) == sourceKey }
                ?: return null
        return AudioPlaybackSource.Saved(selectedSavedAudio.item.itemId)
    }

    data class PlaybackTarget(
        val source: AudioPlaybackSource,
        val pcm: ShortArray,
        val pcmFilePath: String? = null,
        val totalSamples: Int,
        val sampleRateHz: Int,
        val playback: PlaybackUiState,
        val playbackSpeed: Float,
    )
}
