package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.FlashVoicingStyleOption

data class SavedAudioItem(
    val itemId: String,
    val displayName: String,
    val uriString: String,
    val modeWireName: String,
    val durationMs: Long,
    // Tracks when the file entered the saved library so list ordering and library history stay accurate.
    val savedAtEpochSeconds: Long,
    // Preserves the original render timestamp from generated-audio metadata when the file carries it.
    val generatedAtEpochSeconds: Long? = null,
    val flashVoicingStyle: FlashVoicingStyleOption? = null,
    // Saved/detail surfaces use this to explain the actual render rate that produced the file.
    val sampleRateHz: Int? = null,
    // Distinguishes hand-entered text from built-in sample text so saved audio provenance stays visible.
    val inputSourceKind: GeneratedAudioInputSourceKind? = null,
    // Actual saved audio file size from MediaStore, distinct from the encoded payload size.
    val fileSizeBytes: Long? = null,
    // Helps explain payload scale without showing the original text content again in library surfaces.
    val payloadByteCount: Int? = null,
)
