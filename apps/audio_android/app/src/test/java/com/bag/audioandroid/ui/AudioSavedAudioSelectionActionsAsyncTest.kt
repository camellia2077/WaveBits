package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.GeneratedAudioPcmCacheWriter
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.PlaybackUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSavedAudioSelectionActionsAsyncTest {
    @Test
    fun `long saved audio selection updates placeholder first then hydrates in background`() =
        runTest {
            val savedItem =
                SavedAudioItem(
                    itemId = "saved-long",
                    displayName = "saved-long.wav",
                    uriString = "content://saved/long",
                    modeWireName = TransportModeOption.Pro.wireName,
                    durationMs = 181_000L,
                    savedAtEpochSeconds = 1L,
                    sampleRateHz = 44_100,
                )
            val hydratedContent =
                SavedAudioContent(
                    item = savedItem,
                    pcm = shortArrayOf(),
                    waveformPcm = shortArrayOf(4, 5, 6),
                    pcmFilePath = "cached-long.pcm16",
                    sampleRateHz = 44_100,
                    metadata =
                        GeneratedAudioMetadata(
                            mode = TransportModeOption.Pro,
                            createdAtIsoUtc = "2026-05-11T00:00:00Z",
                            durationMs = savedItem.durationMs,
                            sampleRateHz = 44_100,
                            frameSamples = 2_205,
                            pcmSampleCount = 7_982_100,
                            payloadByteCount = 0,
                            inputSourceKind = GeneratedAudioInputSourceKind.Manual,
                            appVersion = "test",
                            coreVersion = "test",
                        ),
                )
            val loadGate = CompletableDeferred<Unit>()
            val repository =
                AsyncTestSavedAudioRepository(
                    listItems = listOf(savedItem),
                    loadBlock = {
                        loadGate.await()
                        hydratedContent
                    },
                )
            val uiState = MutableStateFlow(AudioAppUiState(savedAudioItems = listOf(savedItem)))
            val actions =
                AudioSavedAudioSelectionActions(
                    uiState = uiState,
                    scope = this,
                    playbackRuntimeGateway = AsyncTestPlaybackRuntimeGateway(),
                    savedAudioRepository = repository,
                    stopPlayback = {},
                    setCurrentStatusText = {},
                    generatedAudioCacheGateway = AsyncTestGeneratedAudioCacheGateway(),
                    savedAudioDecodeCacheGateway = AsyncTestSavedAudioDecodeCacheGateway(),
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            assertTrue(actions.prepareSavedAudioSelection(savedItem.itemId))

            val placeholder = uiState.value.selectedSavedAudio ?: error("placeholder missing")
            assertEquals(savedItem.itemId, placeholder.item.itemId)
            assertTrue(placeholder.isLoadingContent)
            assertEquals(0, placeholder.waveformPcm.size)
            assertEquals(null, placeholder.pcmFilePath)
            assertEquals(
                com.bag.audioandroid.ui.model.AudioPlaybackSource
                    .Saved(savedItem.itemId),
                uiState.value.currentPlaybackSource,
            )

            loadGate.complete(Unit)
            advanceUntilIdle()

            val hydrated = uiState.value.selectedSavedAudio ?: error("hydrated selection missing")
            assertFalse(hydrated.isLoadingContent)
            assertEquals(hydratedContent.waveformPcm.toList(), hydrated.waveformPcm.toList())
            assertEquals(hydratedContent.pcmFilePath, hydrated.pcmFilePath)
            assertEquals(hydratedContent.metadata?.pcmSampleCount, hydrated.metadata?.pcmSampleCount)
        }
}

private class AsyncTestSavedAudioRepository(
    private val listItems: List<SavedAudioItem>,
    private val loadBlock: suspend (String) -> SavedAudioContent?,
) : SavedAudioRepository {
    override fun suggestGeneratedAudioDisplayName(
        mode: TransportModeOption,
        inputText: String,
    ): String = "generated.wav"

    override fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult = AudioExportResult.Failed

    override fun exportGeneratedAudioToDocument(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
        destinationUriString: String,
    ): Boolean = false

    override fun listSavedAudio(): List<SavedAudioItem> = listItems

    override fun loadSavedAudio(itemId: String): SavedAudioContent? = kotlinx.coroutines.runBlocking { loadBlock(itemId) }

    override fun deleteSavedAudio(itemId: String): Boolean = false

    override fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult = SavedAudioRenameResult.Failed

    override fun importAudio(uriString: String): SavedAudioImportResult = SavedAudioImportResult.Failed

    override fun exportSavedAudioToDocument(
        itemId: String,
        destinationUriString: String,
    ): Boolean = false

    override fun shareSavedAudio(item: SavedAudioItem): Boolean = false
}

private class AsyncTestPlaybackRuntimeGateway : PlaybackRuntimeGateway {
    override fun cleared(): PlaybackUiState = PlaybackUiState()

    override fun load(
        totalSamples: Int,
        sampleRateHz: Int,
    ): PlaybackUiState = PlaybackUiState(totalSamples = totalSamples, sampleRateHz = sampleRateHz)

    override fun playStarted(state: PlaybackUiState): PlaybackUiState = state

    override fun paused(state: PlaybackUiState): PlaybackUiState = state

    override fun resumed(state: PlaybackUiState): PlaybackUiState = state

    override fun progress(
        state: PlaybackUiState,
        playedSamples: Int,
    ): PlaybackUiState = state

    override fun scrubStarted(state: PlaybackUiState): PlaybackUiState = state

    override fun scrubChanged(
        state: PlaybackUiState,
        targetSamples: Int,
    ): PlaybackUiState = state

    override fun scrubCommitted(state: PlaybackUiState): PlaybackUiState = state

    override fun scrubCanceled(state: PlaybackUiState): PlaybackUiState = state

    override fun stopped(state: PlaybackUiState): PlaybackUiState = state

    override fun completed(state: PlaybackUiState): PlaybackUiState = state

    override fun failed(state: PlaybackUiState): PlaybackUiState = state

    override fun clampSamples(
        totalSamples: Int,
        sampleIndex: Int,
    ): Int = sampleIndex

    override fun fractionToSamples(
        totalSamples: Int,
        fraction: Float,
    ): Int = 0

    override fun progressFraction(state: PlaybackUiState): Float = 0f

    override fun elapsedMs(state: PlaybackUiState): Long = 0L

    override fun totalMs(state: PlaybackUiState): Long = 0L
}

private class AsyncTestGeneratedAudioCacheGateway : GeneratedAudioCacheGateway {
    override fun createPcmCacheWriter(modeWireName: String): GeneratedAudioPcmCacheWriter {
        error("Not needed in async selection test")
    }

    override fun deleteCachedFile(path: String?) = Unit

    override fun pruneCachedFiles(retainedPaths: Set<String>) = Unit
}

private class AsyncTestSavedAudioDecodeCacheGateway : SavedAudioDecodeCacheGateway {
    override fun read(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
    ) = null

    override fun write(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
        decodedPayload: com.bag.audioandroid.domain.DecodedPayloadViewData,
        followData: com.bag.audioandroid.domain.PayloadFollowViewData,
        flashSignalInfo: FlashSignalInfo,
    ) = Unit

    override fun delete(itemId: String) = Unit

    override fun prune(validItemIds: Set<String>) = Unit
}
