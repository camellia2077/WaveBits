package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.TransportModeOption

sealed interface AudioExportResult {
    data class Success(
        val displayName: String,
        val uriString: String,
    ) : AudioExportResult

    data object Failed : AudioExportResult
}

interface AudioExportGateway {
    fun suggestGeneratedAudioDisplayName(
        mode: TransportModeOption,
        inputText: String,
    ): String

    fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult

    fun exportGeneratedAudioToDocument(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
        destinationUriString: String,
    ): Boolean
}
