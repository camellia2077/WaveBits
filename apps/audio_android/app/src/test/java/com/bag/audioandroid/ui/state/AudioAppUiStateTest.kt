package com.bag.audioandroid.ui.state

import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioAppUiStateTest {
    @Test
    fun `generated playback frame samples fall back to default when metadata is missing`() {
        val state =
            AudioAppUiState(
                currentPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Pro),
            )

        assertEquals(2205, state.currentPlaybackFrameSamples)
    }

    @Test
    fun `saved playback frame samples prefer metadata value`() {
        val savedItem =
            SavedAudioItem(
                itemId = "saved-1",
                displayName = "Saved Pro",
                uriString = "content://saved/pro",
                modeWireName = TransportModeOption.Pro.wireName,
                durationMs = 1_000L,
                savedAtEpochSeconds = 100L,
            )
        val metadata =
            GeneratedAudioMetadata(
                mode = TransportModeOption.Pro,
                createdAtIsoUtc = "2026-01-01T00:00:00Z",
                durationMs = 1_000L,
                frameSamples = 4096,
                pcmSampleCount = 8_192,
                appVersion = "1.0.0",
                coreVersion = "1.0.0",
            )
        val state =
            AudioAppUiState(
                currentPlaybackSource = AudioPlaybackSource.Saved(savedItem.itemId),
                selectedSavedAudio =
                    SavedAudioPlaybackSelection(
                        item = savedItem,
                        pcm = shortArrayOf(1, 2, 3),
                        sampleRateHz = 44_100,
                        metadata = metadata,
                        playback = PlaybackUiState(),
                    ),
            )

        assertEquals(4096, state.currentPlaybackFrameSamples)
    }
}
