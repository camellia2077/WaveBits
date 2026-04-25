package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.TransportModeOption

interface SavedAudioRepository {
    fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult

    fun listSavedAudio(): List<SavedAudioItem>

    fun loadSavedAudio(itemId: String): SavedAudioContent?

    fun deleteSavedAudio(itemId: String): Boolean

    fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult

    fun importAudio(uriString: String): SavedAudioImportResult

    fun shareSavedAudio(item: SavedAudioItem): Boolean

    fun readLibraryMetadata(): SavedAudioLibraryMetadata = SavedAudioLibraryMetadata()

    fun createSavedAudioFolder(name: String): SavedAudioFolderMutationResult = SavedAudioFolderMutationResult.Failed

    fun renameSavedAudioFolder(
        folderId: String,
        name: String,
    ): SavedAudioFolderMutationResult = SavedAudioFolderMutationResult.Failed

    fun deleteSavedAudioFolder(folderId: String): Boolean = false

    fun assignSavedAudioToFolder(
        itemIds: Collection<String>,
        folderId: String?,
    ): Boolean = false
}
