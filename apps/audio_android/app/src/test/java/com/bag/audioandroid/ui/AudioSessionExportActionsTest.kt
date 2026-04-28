package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSessionExportActionsTest {
    @Test
    fun `export success updates status and emits snackbar`() {
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to sessionWithGeneratedAudio(),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository =
                    FakeExportRepository(
                        exportResult =
                            AudioExportResult.Success(
                                displayName = "test.wav",
                                uriString = "content://saved/test",
                            ),
                ),
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onExportAudio()

        assertResId(state.value.currentSession.statusText, R.string.status_audio_saved)
        assertResId(
            state.value.snackbarMessage?.text ?: UiText.Empty,
            R.string.snackbar_audio_saved_to_library,
        )
        assertNotNull(state.value.snackbarMessage?.id)
    }

    @Test
    fun `export failure emits failure snackbar`() {
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to sessionWithGeneratedAudio(),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository = FakeExportRepository(exportResult = AudioExportResult.Failed),
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onExportAudio()

        assertResId(state.value.currentSession.statusText, R.string.status_audio_save_failed)
        assertResId(
            state.value.snackbarMessage?.text ?: UiText.Empty,
            R.string.snackbar_audio_save_failed,
        )
    }

    private fun sessionWithGeneratedAudio() =
        ModeAudioSessionState(
            inputText = "text",
            generatedPcm = shortArrayOf(1, 2, 3),
            generatedAudioMetadata =
                GeneratedAudioMetadata(
                    mode = TransportModeOption.Flash,
                    flashVoicingStyle = FlashVoicingStyleOption.CodedBurst,
                    createdAtIsoUtc = "2026-03-17T00:00:00Z",
                    durationMs = 1L,
                    sampleRateHz = 44_100,
                    frameSamples = 2205,
                    pcmSampleCount = 3,
                    payloadByteCount = 4,
                    inputSourceKind = GeneratedAudioInputSourceKind.Manual,
                    appVersion = "1.0.0",
                    coreVersion = "1.0.0",
                ),
            generatedFlashVoicingStyle = FlashVoicingStyleOption.CodedBurst,
        )

    private fun assertResId(
        text: UiText,
        expectedResId: Int,
    ) {
        assertTrue(text is UiText.Resource)
        assertEquals(expectedResId, (text as UiText.Resource).resId)
    }
}

private class FakeExportRepository(
    private val exportResult: AudioExportResult,
) : SavedAudioRepository {
    override fun suggestGeneratedAudioDisplayName(
        mode: TransportModeOption,
        inputText: String,
    ): String = "test.wav"

    override fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult = exportResult

    override fun exportGeneratedAudioToDocument(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
        destinationUriString: String,
    ): Boolean = false

    override fun listSavedAudio(): List<SavedAudioItem> = emptyList()

    override fun loadSavedAudio(itemId: String): SavedAudioContent? = null

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
