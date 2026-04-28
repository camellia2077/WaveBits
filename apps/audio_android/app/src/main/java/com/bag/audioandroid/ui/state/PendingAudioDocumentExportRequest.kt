package com.bag.audioandroid.ui.state

import com.bag.audioandroid.ui.model.TransportModeOption

data class PendingAudioDocumentExportRequest(
    val id: Long,
    val suggestedFileName: String,
    val source: AudioDocumentExportSource,
)

sealed interface AudioDocumentExportSource {
    data class Generated(
        val mode: TransportModeOption,
    ) : AudioDocumentExportSource

    data class Saved(
        val itemId: String,
    ) : AudioDocumentExportSource
}
