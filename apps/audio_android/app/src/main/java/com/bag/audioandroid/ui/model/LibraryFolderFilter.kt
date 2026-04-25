package com.bag.audioandroid.ui.model

import com.bag.audioandroid.domain.SavedAudioFolder

sealed interface LibraryFolderFilter {
    val key: String

    data object All : LibraryFolderFilter {
        override val key: String = KEY_ALL
    }

    data object Uncategorized : LibraryFolderFilter {
        override val key: String = KEY_UNCATEGORIZED
    }

    data class Folder(
        val folderId: String,
    ) : LibraryFolderFilter {
        override val key: String = "$KEY_FOLDER_PREFIX$folderId"
    }

    fun matches(
        itemId: String,
        assignments: Map<String, String>,
    ): Boolean =
        when (this) {
            All -> true
            Uncategorized -> assignments[itemId] == null
            is Folder -> assignments[itemId] == folderId
        }

    companion object {
        private const val KEY_ALL = "__all__"
        private const val KEY_UNCATEGORIZED = "__uncategorized__"
        private const val KEY_FOLDER_PREFIX = "folder:"

        fun fromKey(
            key: String,
            folders: List<SavedAudioFolder>,
        ): LibraryFolderFilter =
            when {
                key == KEY_UNCATEGORIZED -> Uncategorized
                key.startsWith(KEY_FOLDER_PREFIX) -> {
                    val folderId = key.removePrefix(KEY_FOLDER_PREFIX)
                    if (folders.any { it.folderId == folderId }) {
                        Folder(folderId)
                    } else {
                        All
                    }
                }

                else -> All
            }
    }
}
