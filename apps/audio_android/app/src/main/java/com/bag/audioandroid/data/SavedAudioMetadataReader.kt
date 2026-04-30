package com.bag.audioandroid.data

import android.content.ContentResolver
import android.net.Uri
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.AudioIoMetadataCodes
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.SavedAudioItem
import java.time.Instant

internal class SavedAudioMetadataReader(
    private val audioIoGateway: AudioIoGateway,
) {
    fun readAudioMetadataHeader(
        contentResolver: ContentResolver,
        uri: Uri,
    ): GeneratedAudioMetadata? =
        runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                val headerBytes = input.readNBytes(MetadataHeaderReadLimitBytes)
                audioIoGateway
                    .decodeMonoPcm16WavBytes(headerBytes)
                    .takeIf {
                        it.metadataStatusCode == AudioIoMetadataCodes.STATUS_OK && it.hasReadableMetadata
                    }?.metadata
            }
        }.getOrNull()

    fun toSavedAudioItem(
        row: MediaStoreSavedAudioRow,
        metadata: GeneratedAudioMetadata?,
        unknownModeWireName: String,
    ): SavedAudioItem =
        SavedAudioItem(
            itemId = row.id.toString(),
            displayName = row.displayName,
            uriString = row.uri.toString(),
            modeWireName = metadata?.mode?.wireName ?: unknownModeWireName,
            durationMs = metadata?.durationMs?.takeIf { it > 0L } ?: row.durationMs.coerceAtLeast(0L),
            savedAtEpochSeconds = row.dateAddedEpochSeconds.coerceAtLeast(0L),
            generatedAtEpochSeconds = metadata?.createdAtIsoUtc?.let(::parseCreatedAtEpochSeconds),
            flashVoicingStyle = metadata?.flashVoicingStyle,
            sampleRateHz = metadata?.sampleRateHz?.takeIf { it > 0 },
            inputSourceKind = metadata?.inputSourceKind,
            fileSizeBytes = row.sizeBytes.takeIf { it >= 0L },
            payloadByteCount = metadata?.payloadByteCount?.takeIf { it >= 0 },
        )

    fun importedFallbackItem(
        itemId: String,
        uri: Uri,
        displayName: String,
        metadata: GeneratedAudioMetadata?,
        pcmSize: Int,
        sampleRateHz: Int,
        fileSizeBytes: Long?,
        unknownModeWireName: String,
    ): SavedAudioItem =
        SavedAudioItem(
            itemId = itemId,
            displayName = displayName,
            uriString = uri.toString(),
            modeWireName = metadata?.mode?.wireName ?: unknownModeWireName,
            durationMs =
                metadata?.durationMs?.takeIf { it > 0L }
                    ?: pcmSize.toLong() * 1000L / sampleRateHz.toLong(),
            savedAtEpochSeconds =
                Instant.now().epochSecond,
            generatedAtEpochSeconds = metadata?.createdAtIsoUtc?.let(::parseCreatedAtEpochSeconds),
            flashVoicingStyle = metadata?.flashVoicingStyle,
            sampleRateHz = metadata?.sampleRateHz?.takeIf { it > 0 } ?: sampleRateHz,
            inputSourceKind = metadata?.inputSourceKind,
            fileSizeBytes = fileSizeBytes?.takeIf { it >= 0L },
            payloadByteCount = metadata?.payloadByteCount?.takeIf { it >= 0 },
        )

    private fun parseCreatedAtEpochSeconds(createdAtIsoUtc: String): Long? =
        runCatching { Instant.parse(createdAtIsoUtc).epochSecond }.getOrNull()

    private companion object {
        const val MetadataHeaderReadLimitBytes = 4096
    }
}
