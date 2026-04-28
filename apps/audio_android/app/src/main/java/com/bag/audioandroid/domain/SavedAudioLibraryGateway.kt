package com.bag.audioandroid.domain

sealed interface SavedAudioRenameResult {
    data class Success(
        val updatedItem: SavedAudioItem,
    ) : SavedAudioRenameResult

    data object DuplicateName : SavedAudioRenameResult

    data object Failed : SavedAudioRenameResult
}

sealed interface SavedAudioImportResult {
    data class Success(
        val importedItem: SavedAudioItem,
    ) : SavedAudioImportResult

    data object UnsupportedFormat : SavedAudioImportResult

    data object Failed : SavedAudioImportResult
}

interface SavedAudioLibraryGateway {
    fun listSavedAudio(): List<SavedAudioItem>

    fun loadSavedAudio(itemId: String): SavedAudioContent?

    fun deleteSavedAudio(itemId: String): Boolean

    fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult

    fun importAudio(uriString: String): SavedAudioImportResult

    fun exportSavedAudioToDocument(
        itemId: String,
        destinationUriString: String,
    ): Boolean
}
