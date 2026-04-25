package com.bag.audioandroid.data

import androidx.test.core.app.ApplicationProvider
import com.bag.audioandroid.domain.SavedAudioFolderMutationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedAudioLibraryMetadataStoreTest {
    private lateinit var store: SavedAudioLibraryMetadataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("saved_audio_library_metadata", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = SavedAudioLibraryMetadataStore(context)
    }

    @Test
    fun `folder CRUD and assignment keep metadata in sync`() {
        val createResult = store.createFolder("Ritual samples")
        assertTrue(createResult is SavedAudioFolderMutationResult.Success)
        val folder = (createResult as SavedAudioFolderMutationResult.Success).folder

        assertTrue(store.assignItemsToFolder(listOf("a", "b"), folder.folderId))

        val renamedResult = store.renameFolder(folder.folderId, "Demo set")
        assertTrue(renamedResult is SavedAudioFolderMutationResult.Success)

        val metadataAfterRename = store.readMetadata()
        assertEquals(listOf("Demo set"), metadataAfterRename.folders.map { it.name })
        assertEquals(folder.folderId, metadataAfterRename.itemFolderAssignments["a"])
        assertEquals(folder.folderId, metadataAfterRename.itemFolderAssignments["b"])

        assertTrue(store.deleteFolder(folder.folderId))

        val metadataAfterDelete = store.readMetadata()
        assertTrue(metadataAfterDelete.folders.isEmpty())
        assertTrue(metadataAfterDelete.itemFolderAssignments.isEmpty())
    }

    @Test
    fun `prune removes stale assignments and duplicate folder names are rejected`() {
        val createResult = store.createFolder("Archive")
        val folder = (createResult as SavedAudioFolderMutationResult.Success).folder
        assertTrue(store.assignItemsToFolder(listOf("keep", "drop"), folder.folderId))

        val duplicateResult = store.createFolder("archive")
        assertTrue(duplicateResult is SavedAudioFolderMutationResult.DuplicateName)

        store.pruneItemAssignments(setOf("keep"))

        val metadata = store.readMetadata()
        assertEquals(mapOf("keep" to folder.folderId), metadata.itemFolderAssignments)
        assertFalse(metadata.itemFolderAssignments.containsKey("drop"))
    }
}
