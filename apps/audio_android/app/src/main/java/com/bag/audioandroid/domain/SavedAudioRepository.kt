package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.TransportModeOption

interface SavedAudioRepository {
    fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata
    ): AudioExportResult

    fun listSavedAudio(): List<SavedAudioItem>
    fun loadSavedAudio(itemId: String): SavedAudioContent?
    fun deleteSavedAudio(itemId: String): Boolean
    fun renameSavedAudio(itemId: String, newBaseName: String): SavedAudioRenameResult
    fun importAudio(uriString: String): SavedAudioImportResult
    fun shareSavedAudio(item: SavedAudioItem): Boolean
}
