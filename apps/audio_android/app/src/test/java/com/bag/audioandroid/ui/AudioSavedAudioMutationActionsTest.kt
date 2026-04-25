package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.SavedAudioFolder
import com.bag.audioandroid.domain.SavedAudioFolderMutationResult
import com.bag.audioandroid.domain.SavedAudioLibraryMetadata
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSavedAudioMutationActionsTest {
    @Test
    fun `import success refreshes library and shows imported status`() {
        val imported =
            SavedAudioItem(
                itemId = "2",
                displayName = "imported.wav",
                uriString = "content://imported/2",
                modeWireName = "unknown",
                durationMs = 1000L,
                savedAtEpochSeconds = 1L,
            )
        val repository =
            FakeSavedAudioRepository(
                importResult = SavedAudioImportResult.Success(imported),
                listedItems = listOf(imported),
            )
        val state = MutableStateFlow(AudioAppUiState())
        val actions =
            AudioSavedAudioMutationActions(
                uiState = state,
                savedAudioRepository = repository,
                stopPlayback = {},
                setCurrentStatusText = {},
            )

        actions.onImportAudio("content://picked/audio")

        assertEquals(listOf(imported), state.value.savedAudioItems)
        assertResId(state.value.libraryStatusText, R.string.library_status_imported)
    }

    @Test
    fun `import unsupported format shows explicit status`() {
        val state = MutableStateFlow(AudioAppUiState())
        val actions =
            AudioSavedAudioMutationActions(
                uiState = state,
                savedAudioRepository =
                    FakeSavedAudioRepository(
                        importResult = SavedAudioImportResult.UnsupportedFormat,
                    ),
                stopPlayback = {},
                setCurrentStatusText = {},
            )

        actions.onImportAudio("content://picked/audio")

        assertResId(state.value.libraryStatusText, R.string.library_status_import_unsupported)
    }

    @Test
    fun `create folder refreshes metadata and shows created status`() {
        val repository =
            FakeSavedAudioRepository(
                folderCreateResult =
                    SavedAudioFolderMutationResult.Success(
                        SavedAudioFolder(
                            folderId = "folder-1",
                            name = "Demo",
                            createdAtEpochMillis = 1L,
                        ),
                    ),
                libraryMetadata =
                    SavedAudioLibraryMetadata(
                        folders =
                            listOf(
                                SavedAudioFolder(
                                    folderId = "folder-1",
                                    name = "Demo",
                                    createdAtEpochMillis = 1L,
                                ),
                            ),
                    ),
            )
        val state = MutableStateFlow(AudioAppUiState())
        val actions =
            AudioSavedAudioMutationActions(
                uiState = state,
                savedAudioRepository = repository,
                stopPlayback = {},
                setCurrentStatusText = {},
            )

        actions.onCreateSavedAudioFolder("Demo")

        assertEquals(1, state.value.savedAudioFolders.size)
        assertResId(state.value.libraryStatusText, R.string.library_status_folder_created)
    }

    @Test
    fun `move to folder updates assignment map and shows moved status`() {
        val item =
            SavedAudioItem(
                itemId = "2",
                displayName = "imported.wav",
                uriString = "content://imported/2",
                modeWireName = "unknown",
                durationMs = 1000L,
                savedAtEpochSeconds = 1L,
            )
        val folder =
            SavedAudioFolder(
                folderId = "folder-1",
                name = "Archive",
                createdAtEpochMillis = 1L,
            )
        val repository =
            FakeSavedAudioRepository(
                listedItems = listOf(item),
                assignToFolderResult = true,
                libraryMetadata =
                    SavedAudioLibraryMetadata(
                        folders = listOf(folder),
                        itemFolderAssignments = mapOf(item.itemId to folder.folderId),
                    ),
            )
        val state = MutableStateFlow(AudioAppUiState(savedAudioItems = listOf(item), savedAudioFolders = listOf(folder)))
        val actions =
            AudioSavedAudioMutationActions(
                uiState = state,
                savedAudioRepository = repository,
                stopPlayback = {},
                setCurrentStatusText = {},
            )

        actions.onMoveSavedAudioToFolder(listOf(item.itemId), folder.folderId)

        assertEquals(folder.folderId, state.value.savedAudioFolderAssignments[item.itemId])
        assertResId(state.value.libraryStatusText, R.string.library_status_moved_to_folder)
    }

    private fun assertResId(
        text: UiText,
        expectedResId: Int,
    ) {
        assertTrue(text is UiText.Resource)
        assertEquals(expectedResId, (text as UiText.Resource).resId)
    }
}

private class FakeSavedAudioRepository(
    private val importResult: SavedAudioImportResult = SavedAudioImportResult.Failed,
    private val listedItems: List<SavedAudioItem> = emptyList(),
    private val libraryMetadata: SavedAudioLibraryMetadata = SavedAudioLibraryMetadata(),
    private val folderCreateResult: SavedAudioFolderMutationResult = SavedAudioFolderMutationResult.Failed,
    private val assignToFolderResult: Boolean = false,
) : SavedAudioRepository {
    override fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult = AudioExportResult.Failed

    override fun listSavedAudio(): List<SavedAudioItem> = listedItems

    override fun loadSavedAudio(itemId: String): SavedAudioContent? = null

    override fun deleteSavedAudio(itemId: String): Boolean = false

    override fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult = SavedAudioRenameResult.Failed

    override fun importAudio(uriString: String): SavedAudioImportResult = importResult

    override fun shareSavedAudio(item: SavedAudioItem): Boolean = false

    override fun readLibraryMetadata(): SavedAudioLibraryMetadata = libraryMetadata

    override fun createSavedAudioFolder(name: String): SavedAudioFolderMutationResult = folderCreateResult

    override fun assignSavedAudioToFolder(
        itemIds: Collection<String>,
        folderId: String?,
    ): Boolean = assignToFolderResult
}
