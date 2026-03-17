package com.bag.audioandroid.data

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.bag.audioandroid.domain.AudioExportGateway
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.ui.model.TransportModeOption
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MediaStoreAudioExportGateway(
    context: Context,
    private val audioIoGateway: AudioIoGateway
) : AudioExportGateway {
    private val contentResolver = context.contentResolver

    override fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata
    ): AudioExportResult {
        val wavBytes = audioIoGateway.encodeMonoPcm16ToWavBytes(sampleRateHz, pcm, metadata)
        if (wavBytes.isEmpty()) {
            return AudioExportResult.Failed
        }

        val baseName = buildBaseName(mode, inputText)
        val displayName = resolveUniqueDisplayName(baseName) ?: return AudioExportResult.Failed
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, MIME_TYPE_WAV)
            put(MediaStore.Audio.Media.RELATIVE_PATH, RELATIVE_DIRECTORY)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(collection, contentValues) ?: return AudioExportResult.Failed
        return try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(wavBytes)
            } ?: return AudioExportResult.Failed.also { contentResolver.delete(uri, null, null) }

            contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                null,
                null
            )
            AudioExportResult.Success(displayName = displayName, uriString = uri.toString())
        } catch (_: IOException) {
            contentResolver.delete(uri, null, null)
            AudioExportResult.Failed
        } catch (_: SecurityException) {
            contentResolver.delete(uri, null, null)
            AudioExportResult.Failed
        }
    }

    private fun buildBaseName(mode: TransportModeOption, inputText: String): String {
        val timestamp = LocalDateTime.now().format(FILE_NAME_TIME_FORMATTER)
        val previewStem = buildPreviewStem(inputText)
        val stem = if (previewStem.isEmpty()) {
            "wavebits_${mode.wireName}_${timestamp}"
        } else {
            "${previewStem}_${mode.wireName}_${timestamp}"
        }
        return "$stem.wav"
    }

    private fun buildPreviewStem(inputText: String): String {
        val normalized = inputText.replace(WHITESPACE_REGEX, " ").trim()
        if (normalized.isEmpty()) {
            return ""
        }

        val preview = StringBuilder()
        var visibleCount = 0
        var index = 0
        while (index < normalized.length && visibleCount < MAX_PREVIEW_VISIBLE_CHARACTERS) {
            val codePoint = normalized.codePointAt(index)
            val isWhitespace = Character.isWhitespace(codePoint)
            val sanitizedCodePoint = sanitizeFileNameCodePoint(codePoint)
            if (sanitizedCodePoint == null) {
                index += Character.charCount(codePoint)
                continue
            }
            if (!isWhitespace) {
                visibleCount += 1
            }
            preview.appendCodePoint(sanitizedCodePoint)
            index += Character.charCount(codePoint)
        }

        return preview.toString().trim().trim('.')
    }

    private fun sanitizeFileNameCodePoint(codePoint: Int): Int? = when {
        Character.isISOControl(codePoint) -> null
        codePoint <= Char.MAX_VALUE.code && ILLEGAL_FILE_NAME_CHARACTERS.contains(codePoint.toChar()) -> '_'.code
        else -> codePoint
    }

    private fun resolveUniqueDisplayName(baseName: String): String? {
        if (!displayNameExists(baseName)) {
            return baseName
        }
        val lastDot = baseName.lastIndexOf('.')
        val stem = if (lastDot >= 0) baseName.substring(0, lastDot) else baseName
        val extension = if (lastDot >= 0) baseName.substring(lastDot) else ""
        for (suffix in 2..MAX_DUPLICATE_SUFFIX) {
            val candidate = "${stem}_$suffix$extension"
            if (!displayNameExists(candidate)) {
                return candidate
            }
        }
        return null
    }

    private fun displayNameExists(displayName: String): Boolean {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(displayName, RELATIVE_PATH_PREFIX)
        contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    private companion object {
        const val MAX_DUPLICATE_SUFFIX = 999
        const val MAX_PREVIEW_VISIBLE_CHARACTERS = 12
        const val MIME_TYPE_WAV = "audio/wav"
        const val RELATIVE_DIRECTORY = "Music/WaveBits"
        const val RELATIVE_PATH_PREFIX = "Music/WaveBits%"
        const val ILLEGAL_FILE_NAME_CHARACTERS = "\\/:*?\"<>|"
        val FILE_NAME_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
