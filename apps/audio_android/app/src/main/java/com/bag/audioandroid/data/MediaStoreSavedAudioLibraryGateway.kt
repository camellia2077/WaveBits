package com.bag.audioandroid.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.AudioIoWavCodes
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioLibraryGateway
import com.bag.audioandroid.domain.SavedAudioRenameResult

class MediaStoreSavedAudioLibraryGateway(
    context: Context,
    audioIoGateway: AudioIoGateway,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) : SavedAudioLibraryGateway {
    private val contentResolver = context.contentResolver
    private val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val queries = MediaStoreSavedAudioQueries(contentResolver, collection)
    private val namingPolicy = SavedAudioFileNamingPolicy()
    private val metadataReader = SavedAudioMetadataReader(audioIoGateway)
    private val audioIoGateway = audioIoGateway
    private val metadataCache = mutableMapOf<String, CachedSavedAudioMetadata?>()

    override fun listSavedAudio(): List<SavedAudioItem> =
        queries.listRows().map { row ->
            metadataReader.toSavedAudioItem(
                row = row,
                metadata = cachedMetadataForRow(row),
                unknownModeWireName = UNKNOWN_MODE,
            )
        }

    override fun loadSavedAudio(itemId: String): SavedAudioContent? {
        val savedAudioItem = findSavedAudioItem(itemId) ?: return null
        val uri = queries.uriForItemId(itemId) ?: return null
        val row = queries.findRow(itemId)
        val metadata =
            if (row != null) {
                cachedMetadataForRow(row)
            } else {
                metadataReader.readAudioMetadataHeader(contentResolver, uri)
            }
        if (shouldUseFileBackedLoad(savedAudioItem, metadata)) {
            return loadSavedAudioFileBacked(savedAudioItem, uri, metadata)
        }
        val fileBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val decoded = audioIoGateway.decodeMonoPcm16WavBytes(fileBytes)
        if (!decoded.isWavSuccess ||
            decoded.wavStatusCode != AudioIoWavCodes.STATUS_OK ||
            decoded.channels != 1 ||
            decoded.sampleRateHz <= 0
        ) {
            return null
        }

        return SavedAudioContent(
            item = savedAudioItem,
            pcm = decoded.pcm,
            waveformPcm = decoded.pcm,
            sampleRateHz = decoded.sampleRateHz,
            metadata = decoded.metadata,
        )
    }

    override fun deleteSavedAudio(itemId: String): Boolean =
        queries.delete(itemId).also { deleted ->
            if (deleted) {
                metadataCache.remove(itemId)
            }
        }

    override fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult {
        val savedAudioItem = findSavedAudioItem(itemId) ?: return SavedAudioRenameResult.Failed
        val finalDisplayName = namingPolicy.normalizeRenameBaseName(newBaseName) ?: return SavedAudioRenameResult.Failed
        if (finalDisplayName == savedAudioItem.displayName) {
            return SavedAudioRenameResult.Success(savedAudioItem)
        }
        if (queries.displayNameExists(finalDisplayName)) {
            return SavedAudioRenameResult.DuplicateName
        }
        if (!queries.rename(itemId, finalDisplayName)) {
            return SavedAudioRenameResult.Failed
        }
        metadataCache.remove(itemId)
        return findSavedAudioItem(itemId)
            ?.let { SavedAudioRenameResult.Success(it) }
            ?: SavedAudioRenameResult.Failed
    }

    override fun importAudio(uriString: String): SavedAudioImportResult {
        val sourceUri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return SavedAudioImportResult.Failed
        val sourceBytes =
            contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return SavedAudioImportResult.Failed
        val decoded = audioIoGateway.decodeMonoPcm16WavBytes(sourceBytes)
        if (!decoded.isWavSuccess ||
            decoded.wavStatusCode != AudioIoWavCodes.STATUS_OK ||
            decoded.channels != 1 ||
            decoded.sampleRateHz <= 0
        ) {
            return SavedAudioImportResult.UnsupportedFormat
        }

        val finalDisplayName =
            namingPolicy.nextAvailableDisplayName(
                preferredDisplayName =
                    namingPolicy.resolveImportDisplayName(
                        queries.resolveSourceDisplayName(sourceUri),
                    ),
                exists = queries::displayNameExists,
            )
        val insertedUri = queries.insertPendingAudio(finalDisplayName) ?: return SavedAudioImportResult.Failed

        val imported =
            runCatching {
                if (!queries.writeBytes(insertedUri, sourceBytes)) {
                    error("Failed to open destination output stream")
                }
                queries.markImportCompleted(insertedUri)
                val itemId = ContentUris.parseId(insertedUri).toString()
                findSavedAudioItem(itemId)
                    ?: metadataReader.importedFallbackItem(
                        itemId = itemId,
                        uri = insertedUri,
                        displayName = finalDisplayName,
                        metadata = decoded.metadata,
                        pcmSize = decoded.pcm.size,
                        sampleRateHz = decoded.sampleRateHz,
                        fileSizeBytes = sourceBytes.size.toLong(),
                        unknownModeWireName = UNKNOWN_MODE,
                    )
            }.getOrElse {
                contentResolver.delete(insertedUri, null, null)
                return SavedAudioImportResult.Failed
            }

        return SavedAudioImportResult.Success(imported)
    }

    override fun exportSavedAudioToDocument(
        itemId: String,
        destinationUriString: String,
    ): Boolean {
        val sourceUri = queries.uriForItemId(itemId) ?: return false
        val destinationUri = runCatching { Uri.parse(destinationUriString) }.getOrNull() ?: return false
        return runCatching {
            contentResolver.openInputStream(sourceUri)?.use { input ->
                contentResolver.openOutputStream(destinationUri)?.use { output ->
                    input.copyTo(output)
                    output.flush()
                    true
                }
            } ?: false
        }.getOrDefault(false)
    }

    private fun findSavedAudioItem(itemId: String): SavedAudioItem? =
        queries.findRow(itemId)?.let { row ->
            metadataReader.toSavedAudioItem(
                row = row,
                metadata = cachedMetadataForRow(row),
                unknownModeWireName = UNKNOWN_MODE,
            )
        }

    private fun cachedMetadataForRow(row: MediaStoreSavedAudioRow): com.bag.audioandroid.domain.GeneratedAudioMetadata? {
        val itemId = row.id.toString()
        val cached = metadataCache[itemId]
        if (cached != null &&
            cached.displayName == row.displayName &&
            cached.durationMs == row.durationMs &&
            cached.dateAddedEpochSeconds == row.dateAddedEpochSeconds
        ) {
            return cached.metadata
        }
        // Listing saved audio is a hot UI path. Cache both readable and missing metadata
        // so repeated Saved tab visits do not reopen every WAV just to read the header.
        val metadata = metadataReader.readAudioMetadataHeader(contentResolver, row.uri)
        metadataCache[itemId] =
            CachedSavedAudioMetadata(
                displayName = row.displayName,
                durationMs = row.durationMs,
                dateAddedEpochSeconds = row.dateAddedEpochSeconds,
                metadata = metadata,
            )
        return metadata
    }

    private fun loadSavedAudioFileBacked(
        savedAudioItem: SavedAudioItem,
        uri: Uri,
        metadata: com.bag.audioandroid.domain.GeneratedAudioMetadata?,
    ): SavedAudioContent? {
        val cacheWriter = generatedAudioCacheGateway.createPcmCacheWriter(savedAudioItem.modeWireName)
        return try {
            val extraction =
                contentResolver.openInputStream(uri)?.use { input ->
                    extractMonoPcm16WavToCache(input, cacheWriter::appendPcm)
                } ?: return null
            cacheWriter.finish()
            val waveformPcm =
                buildWaveformPreviewFromPcmFile(
                    pcmFilePath = cacheWriter.filePath,
                    totalSamples = extraction.sampleCount,
                    targetPoints = LONG_AUDIO_WAVEFORM_PREVIEW_POINTS,
                )
            SavedAudioContent(
                item = savedAudioItem,
                pcm = shortArrayOf(),
                waveformPcm = waveformPcm,
                pcmFilePath = cacheWriter.filePath,
                sampleRateHz = extraction.sampleRateHz,
                metadata = metadata,
            )
        } catch (_: Exception) {
            cacheWriter.abort()
            null
        }
    }

    private fun shouldUseFileBackedLoad(
        item: SavedAudioItem,
        metadata: com.bag.audioandroid.domain.GeneratedAudioMetadata?,
    ): Boolean {
        val metadataSampleCount = metadata?.pcmSampleCount ?: 0
        val durationMs = metadata?.durationMs ?: item.durationMs
        return metadataSampleCount >= LONG_AUDIO_FILE_THRESHOLD_SAMPLES ||
            durationMs >= LONG_AUDIO_FILE_THRESHOLD_MS
    }

    private companion object {
        const val UNKNOWN_MODE = "unknown"
        const val LONG_AUDIO_FILE_THRESHOLD_MS = 120_000L
        const val LONG_AUDIO_FILE_THRESHOLD_SAMPLES = 44100 * 120
        const val LONG_AUDIO_WAVEFORM_PREVIEW_POINTS = 4096
    }
}

private data class CachedSavedAudioMetadata(
    val displayName: String,
    val durationMs: Long,
    val dateAddedEpochSeconds: Long,
    val metadata: com.bag.audioandroid.domain.GeneratedAudioMetadata?,
)
