package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.TransportModeOption

sealed interface AudioExportResult {
    data class Success(
        val displayName: String,
        val uriString: String
    ) : AudioExportResult

    data object Failed : AudioExportResult
}

interface AudioExportGateway {
    fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata
    ): AudioExportResult
}
