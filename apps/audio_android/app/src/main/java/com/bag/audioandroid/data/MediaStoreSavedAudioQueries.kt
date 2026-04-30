package com.bag.audioandroid.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns

internal class MediaStoreSavedAudioQueries(
    private val contentResolver: ContentResolver,
    private val collection: Uri,
) {
    fun listRows(): List<MediaStoreSavedAudioRow> {
        val projection =
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.SIZE,
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
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    add(
                        MediaStoreSavedAudioRow(
                            id = id,
                            displayName = cursor.getString(nameIndex).orEmpty(),
                            durationMs = cursor.getLong(durationIndex),
                            dateAddedEpochSeconds = cursor.getLong(dateAddedIndex),
                            sizeBytes = cursor.getLong(sizeIndex),
                            uri = ContentUris.withAppendedId(collection, id),
                        ),
                    )
                }
            }
        }
    }

    fun findRow(itemId: String): MediaStoreSavedAudioRow? =
        uriForItemId(itemId)?.let { targetUri ->
            listRows().firstOrNull { it.uri == targetUri }
        }

    fun uriForItemId(itemId: String): Uri? = itemId.toLongOrNull()?.let { ContentUris.withAppendedId(collection, it) }

    fun delete(itemId: String): Boolean {
        val uri = uriForItemId(itemId) ?: return false
        return runCatching { contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false)
    }

    fun rename(
        itemId: String,
        finalDisplayName: String,
    ): Boolean {
        val uri = uriForItemId(itemId) ?: return false
        return runCatching {
            contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.DISPLAY_NAME, finalDisplayName) },
                null,
                null,
            ) > 0
        }.getOrDefault(false)
    }

    fun displayNameExists(displayName: String): Boolean {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(displayName, RELATIVE_PATH_PREFIX)
        contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    fun resolveSourceDisplayName(sourceUri: Uri): String? =
        contentResolver
            .query(
                sourceUri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }

    fun insertPendingAudio(displayName: String): Uri? =
        contentResolver.insert(
            collection,
            ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, MimeTypeWav)
                put(MediaStore.Audio.Media.RELATIVE_PATH, RelativePathDirectory)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            },
        )

    fun writeBytes(
        destinationUri: Uri,
        bytes: ByteArray,
    ): Boolean =
        contentResolver.openOutputStream(destinationUri)?.use { output ->
            output.write(bytes)
            output.flush()
            true
        } ?: false

    fun markImportCompleted(uri: Uri) {
        contentResolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
            null,
            null,
        )
    }

    private companion object {
        const val RELATIVE_PATH_PREFIX = "Music/FlipBits%"
        const val RelativePathDirectory = "Music/FlipBits/"
        const val MimeTypeWav = "audio/wav"
    }
}

internal data class MediaStoreSavedAudioRow(
    val id: Long,
    val displayName: String,
    val durationMs: Long,
    val dateAddedEpochSeconds: Long,
    val sizeBytes: Long,
    val uri: Uri,
)
