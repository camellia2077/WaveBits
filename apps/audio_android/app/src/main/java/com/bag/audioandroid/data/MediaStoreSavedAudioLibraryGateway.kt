package com.bag.audioandroid.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.MediaStore
import com.bag.audioandroid.domain.AudioIoCodes
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioLibraryGateway
import com.bag.audioandroid.domain.SavedAudioRenameResult
import java.time.Instant

class MediaStoreSavedAudioLibraryGateway(
    context: Context,
    private val audioIoGateway: AudioIoGateway
) : SavedAudioLibraryGateway {
    private val contentResolver = context.contentResolver
    private val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    override fun listSavedAudio(): List<SavedAudioItem> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(RELATIVE_PATH_PREFIX)
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        return buildList {
            contentResolver.query(collection, projection, selection, args, sortOrder)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val displayName = cursor.getString(nameIndex).orEmpty()
                    val itemUri = ContentUris.withAppendedId(collection, id)
                    val metadata = readAudioMetadataHeader(itemUri)
                    add(
                        SavedAudioItem(
                            itemId = id.toString(),
                            displayName = displayName,
                            uriString = itemUri.toString(),
                            modeWireName = metadata?.mode?.wireName ?: UNKNOWN_MODE,
                            durationMs = metadata?.durationMs?.takeIf { it > 0L }
                                ?: cursor.getLong(durationIndex).coerceAtLeast(0L),
                            savedAtEpochSeconds = metadata?.createdAtIsoUtc?.let(::parseCreatedAtEpochSeconds)
                                ?: cursor.getLong(dateAddedIndex).coerceAtLeast(0L),
                            flashVoicingStyle = metadata?.flashVoicingStyle
                        )
                    )
                }
            }
        }
    }

    override fun loadSavedAudio(itemId: String): SavedAudioContent? {
        val savedAudioItem = findSavedAudioItem(itemId) ?: return null
        val uri = ContentUris.withAppendedId(collection, itemId.toLongOrNull() ?: return null)
        val fileBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val decoded = audioIoGateway.decodeMonoPcm16WavBytes(fileBytes)
        if (!decoded.isSuccess ||
            decoded.statusCode != AudioIoCodes.STATUS_OK ||
            decoded.channels != 1 ||
            decoded.sampleRateHz <= 0) {
            return null
        }

        return SavedAudioContent(
            item = savedAudioItem,
            pcm = decoded.pcm,
            sampleRateHz = decoded.sampleRateHz
        )
    }

    override fun deleteSavedAudio(itemId: String): Boolean {
        val uri = ContentUris.withAppendedId(collection, itemId.toLongOrNull() ?: return false)
        return runCatching { contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false)
    }

    override fun renameSavedAudio(itemId: String, newBaseName: String): SavedAudioRenameResult {
        val savedAudioItem = findSavedAudioItem(itemId) ?: return SavedAudioRenameResult.Failed
        val normalizedBaseName = newBaseName.trim()
        if (normalizedBaseName.isEmpty()) {
            return SavedAudioRenameResult.Failed
        }
        val finalDisplayName = ensureWavExtension(normalizedBaseName)
        if (finalDisplayName == savedAudioItem.displayName) {
            return SavedAudioRenameResult.Success(savedAudioItem)
        }
        if (displayNameExists(finalDisplayName)) {
            return SavedAudioRenameResult.DuplicateName
        }

        val uri = ContentUris.withAppendedId(collection, itemId.toLongOrNull() ?: return SavedAudioRenameResult.Failed)
        val updated = runCatching {
            contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.DISPLAY_NAME, finalDisplayName) },
                null,
                null
            ) > 0
        }.getOrDefault(false)
        if (!updated) {
            return SavedAudioRenameResult.Failed
        }
        return findSavedAudioItem(itemId)
            ?.let { SavedAudioRenameResult.Success(it) }
            ?: SavedAudioRenameResult.Failed
    }

    override fun importAudio(uriString: String): SavedAudioImportResult {
        val sourceUri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return SavedAudioImportResult.Failed
        val sourceBytes = contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: return SavedAudioImportResult.Failed
        val decoded = audioIoGateway.decodeMonoPcm16WavBytes(sourceBytes)
        if (!decoded.isSuccess ||
            decoded.statusCode != AudioIoCodes.STATUS_OK ||
            decoded.channels != 1 ||
            decoded.sampleRateHz <= 0) {
            return SavedAudioImportResult.UnsupportedFormat
        }

        val preferredDisplayName = resolveImportDisplayName(sourceUri)
        val finalDisplayName = nextAvailableDisplayName(preferredDisplayName)
        val insertedUri = contentResolver.insert(
            collection,
            ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, finalDisplayName)
                put(MediaStore.Audio.Media.MIME_TYPE, MimeTypeWav)
                put(MediaStore.Audio.Media.RELATIVE_PATH, RelativePathDirectory)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        ) ?: return SavedAudioImportResult.Failed

        val imported = runCatching {
            val wroteBytes = contentResolver.openOutputStream(insertedUri)?.use { output ->
                output.write(sourceBytes)
                output.flush()
                true
            } ?: false
            if (!wroteBytes) {
                error("Failed to open destination output stream")
            }
            contentResolver.update(
                insertedUri,
                ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                null,
                null
            )
            val itemId = ContentUris.parseId(insertedUri).toString()
            findSavedAudioItem(itemId) ?: SavedAudioItem(
                itemId = itemId,
                displayName = finalDisplayName,
                uriString = insertedUri.toString(),
                modeWireName = decoded.metadata?.mode?.wireName ?: UNKNOWN_MODE,
                durationMs = decoded.metadata?.durationMs
                    ?.takeIf { it > 0L }
                    ?: decoded.pcm.size.toLong() * 1000L / decoded.sampleRateHz.toLong(),
                savedAtEpochSeconds = decoded.metadata?.createdAtIsoUtc?.let(::parseCreatedAtEpochSeconds)
                    ?: Instant.now().epochSecond,
                flashVoicingStyle = decoded.metadata?.flashVoicingStyle
            )
        }.getOrElse {
            contentResolver.delete(insertedUri, null, null)
            return SavedAudioImportResult.Failed
        }

        return SavedAudioImportResult.Success(imported)
    }

    private fun findSavedAudioItem(itemId: String): SavedAudioItem? =
        listSavedAudio().firstOrNull { it.itemId == itemId }

    private fun displayNameExists(displayName: String): Boolean {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(displayName, RELATIVE_PATH_PREFIX)
        contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    private fun ensureWavExtension(baseName: String): String =
        if (baseName.endsWith(".wav", ignoreCase = true)) {
            baseName
        } else {
            "$baseName.wav"
        }

    private fun resolveImportDisplayName(sourceUri: Uri): String {
        val queriedDisplayName = contentResolver.query(
            sourceUri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
        val rawName = queriedDisplayName
            ?.substringAfterLast('/')
            ?.trim()
            .orEmpty()
            .ifBlank { "imported_audio" }
        return ensureWavExtension(rawName)
    }

    private fun nextAvailableDisplayName(preferredDisplayName: String): String {
        if (!displayNameExists(preferredDisplayName)) {
            return preferredDisplayName
        }
        val baseName = preferredDisplayName.removeSuffix(".wav")
        var counter = 1
        while (true) {
            val candidate = "$baseName ($counter).wav"
            if (!displayNameExists(candidate)) {
                return candidate
            }
            counter += 1
        }
    }

    private fun readAudioMetadataHeader(uri: Uri): GeneratedAudioMetadata? =
        runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                val headerBytes = input.readNBytes(MetadataHeaderReadLimitBytes)
                audioIoGateway.decodeMonoPcm16WavBytes(headerBytes).metadata
            }
        }.getOrNull()

    private fun parseCreatedAtEpochSeconds(createdAtIsoUtc: String): Long? =
        runCatching { Instant.parse(createdAtIsoUtc).epochSecond }.getOrNull()

    private companion object {
        const val RELATIVE_PATH_PREFIX = "Music/WaveBits%"
        const val RelativePathDirectory = "Music/WaveBits/"
        const val MimeTypeWav = "audio/wav"
        const val UNKNOWN_MODE = "unknown"
        const val MetadataHeaderReadLimitBytes = 4096
    }
}
