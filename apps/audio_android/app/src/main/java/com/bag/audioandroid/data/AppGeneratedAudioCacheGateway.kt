package com.bag.audioandroid.data

import android.content.Context
import android.util.Log
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioPcmCacheWriter
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AppGeneratedAudioCacheGateway(
    context: Context,
) : GeneratedAudioCacheGateway {
    private val cacheDirectory = File(context.cacheDir, CACHE_DIRECTORY_NAME).apply { mkdirs() }

    override fun createPcmCacheWriter(modeWireName: String): GeneratedAudioPcmCacheWriter {
        val file =
            File.createTempFile(
                "${modeWireName}_",
                CACHE_FILE_SUFFIX,
                cacheDirectory,
            )
        safeLogD(CACHE_AUTOMATION_TAG, "create mode=$modeWireName path=${file.absolutePath}")
        return FileGeneratedAudioPcmCacheWriter(file)
    }

    override fun deleteCachedFile(path: String?) {
        if (path.isNullOrBlank()) {
            return
        }
        runCatching { File(path).delete() }
            .onSuccess { deleted -> safeLogD(CACHE_AUTOMATION_TAG, "delete path=$path deleted=$deleted") }
            .onFailure { error -> safeLogD(CACHE_AUTOMATION_TAG, "deleteFailed path=$path error=${error.message}") }
    }

    override fun pruneCachedFiles(retainedPaths: Set<String>) {
        val retainedAbsolutePaths =
            retainedPaths
                .filter { it.isNotBlank() }
                .map { File(it).absoluteFile.path }
                .toSet()
        val files = cacheDirectory.listFiles() ?: return
        files.forEach { file ->
            if (file.absoluteFile.path !in retainedAbsolutePaths) {
                runCatching { file.delete() }
                    .onSuccess { deleted ->
                        safeLogD(
                            CACHE_AUTOMATION_TAG,
                            "prune delete path=${file.absolutePath} deleted=$deleted retained=${retainedAbsolutePaths.size}",
                        )
                    }.onFailure { error ->
                        safeLogD(
                            CACHE_AUTOMATION_TAG,
                            "pruneDeleteFailed path=${file.absolutePath} error=${error.message}",
                        )
                    }
            }
        }
    }

    private companion object {
        const val CACHE_DIRECTORY_NAME = "generated-audio"
        const val CACHE_FILE_SUFFIX = ".pcm16"
    }
}

private class FileGeneratedAudioPcmCacheWriter(
    private val file: File,
) : GeneratedAudioPcmCacheWriter {
    override val filePath: String = file.absolutePath

    private var appendedSamples = 0L
    private var output =
        BufferedOutputStream(
            FileOutputStream(file),
            BUFFER_SIZE_BYTES,
        )
    private var closed = false

    override fun appendPcm(pcm: ShortArray) {
        if (closed || pcm.isEmpty()) {
            return
        }
        appendedSamples += pcm.size.toLong()
        val bytes = ByteArray(pcm.size * SHORT_BYTES)
        var byteIndex = 0
        pcm.forEach { sample ->
            val value = sample.toInt()
            bytes[byteIndex] = (value and BYTE_MASK).toByte()
            bytes[byteIndex + 1] = ((value ushr BITS_PER_BYTE) and BYTE_MASK).toByte()
            byteIndex += SHORT_BYTES
        }
        output.write(bytes)
    }

    override fun finish() {
        closeOutput()
        safeLogD(
            CACHE_AUTOMATION_TAG,
            "finish path=${file.absolutePath} samples=$appendedSamples bytes=${file.length()}",
        )
    }

    override fun abort() {
        closeOutput()
        runCatching { file.delete() }
            .onSuccess { deleted ->
                safeLogD(
                    CACHE_AUTOMATION_TAG,
                    "abort path=${file.absolutePath} samples=$appendedSamples deleted=$deleted",
                )
            }.onFailure { error ->
                safeLogD(CACHE_AUTOMATION_TAG, "abortFailed path=${file.absolutePath} error=${error.message}")
            }
    }

    private fun closeOutput() {
        if (closed) {
            return
        }
        closed = true
        try {
            output.flush()
        } catch (_: IOException) {
        } finally {
            runCatching { output.close() }
        }
    }

    private companion object {
        const val BITS_PER_BYTE = 8
        const val BYTE_MASK = 0xFF
        const val BUFFER_SIZE_BYTES = 32 * 1024
        const val SHORT_BYTES = 2
    }
}

private const val CACHE_AUTOMATION_TAG = "GeneratedAudioCache"

private fun safeLogD(
    tag: String,
    message: String,
) {
    if (!BuildConfig.DEBUG) {
        return
    }
    try {
        Log.d(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
    }
}
