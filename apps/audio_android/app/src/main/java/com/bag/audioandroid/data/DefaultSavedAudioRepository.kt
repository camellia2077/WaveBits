package com.bag.audioandroid.data

import com.bag.audioandroid.domain.AudioExportGateway
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.AudioShareGateway
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioFolderMutationResult
import com.bag.audioandroid.domain.SavedAudioLibraryMetadata
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioLibraryGateway
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.TransportModeOption

class DefaultSavedAudioRepository(
    private val audioExportGateway: AudioExportGateway,
    private val savedAudioLibraryGateway: SavedAudioLibraryGateway,
    private val audioShareGateway: AudioShareGateway,
    private val libraryMetadataStore: SavedAudioLibraryMetadataStore,
) : SavedAudioRepository {
    override fun suggestGeneratedAudioDisplayName(
        mode: TransportModeOption,
        inputText: String,
    ): String = audioExportGateway.suggestGeneratedAudioDisplayName(mode, inputText)

    override fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult =
        audioExportGateway.exportGeneratedAudio(mode, inputText, pcm, pcmFilePath, sampleRateHz, metadata)

    override fun exportGeneratedAudioToDocument(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
        destinationUriString: String,
    ): Boolean =
        audioExportGateway.exportGeneratedAudioToDocument(
            mode = mode,
            inputText = inputText,
            pcm = pcm,
            pcmFilePath = pcmFilePath,
            sampleRateHz = sampleRateHz,
            metadata = metadata,
            destinationUriString = destinationUriString,
        )

    override fun listSavedAudio(): List<SavedAudioItem> =
        savedAudioLibraryGateway.listSavedAudio().also { items ->
            libraryMetadataStore.pruneItemAssignments(items.map { it.itemId }.toSet())
        }

    override fun loadSavedAudio(itemId: String): SavedAudioContent? = savedAudioLibraryGateway.loadSavedAudio(itemId)

    override fun deleteSavedAudio(itemId: String): Boolean = savedAudioLibraryGateway.deleteSavedAudio(itemId)

    override fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult = savedAudioLibraryGateway.renameSavedAudio(itemId, newBaseName)

    override fun importAudio(uriString: String): SavedAudioImportResult = savedAudioLibraryGateway.importAudio(uriString)

    override fun exportSavedAudioToDocument(
        itemId: String,
        destinationUriString: String,
    ): Boolean = savedAudioLibraryGateway.exportSavedAudioToDocument(itemId, destinationUriString)

    override fun shareSavedAudio(item: SavedAudioItem): Boolean = audioShareGateway.shareSavedAudio(item)

    override fun readLibraryMetadata(): SavedAudioLibraryMetadata = libraryMetadataStore.readMetadata()

    override fun createSavedAudioFolder(name: String): SavedAudioFolderMutationResult =
        libraryMetadataStore.createFolder(name)

    override fun renameSavedAudioFolder(
        folderId: String,
        name: String,
    ): SavedAudioFolderMutationResult = libraryMetadataStore.renameFolder(folderId, name)

    override fun deleteSavedAudioFolder(folderId: String): Boolean = libraryMetadataStore.deleteFolder(folderId)

    override fun assignSavedAudioToFolder(
        itemIds: Collection<String>,
        folderId: String?,
    ): Boolean = libraryMetadataStore.assignItemsToFolder(itemIds, folderId)
}
