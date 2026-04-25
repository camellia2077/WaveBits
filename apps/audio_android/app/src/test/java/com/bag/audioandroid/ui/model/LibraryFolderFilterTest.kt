package com.bag.audioandroid.ui.model

import com.bag.audioandroid.domain.SavedAudioFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFolderFilterTest {
    @Test
    fun `uncategorized only matches items without folder assignment`() {
        val assignments = mapOf("a" to "folder-1")

        assertTrue(LibraryFolderFilter.Uncategorized.matches("b", assignments))
        assertFalse(LibraryFolderFilter.Uncategorized.matches("a", assignments))
    }

    @Test
    fun `folder filter key falls back to all when folder is missing`() {
        val folders = listOf(SavedAudioFolder(folderId = "folder-1", name = "Archive", createdAtEpochMillis = 1L))

        assertEquals(
            LibraryFolderFilter.Folder("folder-1"),
            LibraryFolderFilter.fromKey("folder:folder-1", folders),
        )
        assertEquals(
            LibraryFolderFilter.All,
            LibraryFolderFilter.fromKey("folder:missing", folders),
        )
    }
}
