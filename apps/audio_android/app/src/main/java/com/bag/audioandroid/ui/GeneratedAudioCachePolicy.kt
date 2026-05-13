package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.state.AudioAppUiState

internal fun GeneratedAudioCacheGateway.enforceGeneratedAudioCachePolicy(uiState: AudioAppUiState) {
    pruneCachedFiles(retainedPaths = uiState.generatedAudioCacheRetainedPaths())
}

private fun AudioAppUiState.generatedAudioCacheRetainedPaths(): Set<String> =
    when (val playbackSource = currentPlaybackSource) {
        is AudioPlaybackSource.Generated ->
            sessions
                .getValue(playbackSource.mode)
                .generatedPcmFilePath
                .orEmpty()
                .takeIf { it.isNotBlank() }
                ?.let(::setOf)
                ?: emptySet()

        is AudioPlaybackSource.Saved ->
            selectedSavedAudio
                ?.takeIf { it.item.itemId == playbackSource.itemId }
                ?.pcmFilePath
                .orEmpty()
                .takeIf { it.isNotBlank() }
                ?.let(::setOf)
                ?: emptySet()
    }
