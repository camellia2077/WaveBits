package com.bag.audioandroid.domain

data class SavedAudioFolder(
    val folderId: String,
    val name: String,
    val createdAtEpochMillis: Long,
)

data class SavedAudioLibraryMetadata(
    val folders: List<SavedAudioFolder> = emptyList(),
    val itemFolderAssignments: Map<String, String> = emptyMap(),
)

sealed interface SavedAudioFolderMutationResult {
    data class Success(
        val folder: SavedAudioFolder,
    ) : SavedAudioFolderMutationResult

    data object DuplicateName : SavedAudioFolderMutationResult

    data object Failed : SavedAudioFolderMutationResult
}
