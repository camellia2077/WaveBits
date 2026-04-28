package com.bag.audioandroid.data

import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.max

internal fun writeMonoPcm16WavFromFile(
    output: OutputStream,
    pcmFilePath: String,
    sampleRateHz: Int,
    totalSamples: Int,
) {
    val dataBytes = totalSamples * ShortBytes
    val byteRate = sampleRateHz * ShortBytes
    val riffChunkSize = WaveHeaderBytes - 8 + dataBytes
    output.writeAscii("RIFF")
    output.writeInt32LE(riffChunkSize)
    output.writeAscii("WAVE")
    output.writeAscii("fmt ")
    output.writeInt32LE(16)
    output.writeInt16LE(1)
    output.writeInt16LE(1)
    output.writeInt32LE(sampleRateHz)
    output.writeInt32LE(byteRate)
    output.writeInt16LE(ShortBytes)
    output.writeInt16LE(16)
    output.writeAscii("data")
    output.writeInt32LE(dataBytes)
    BufferedInputStream(FileInputStream(pcmFilePath)).use { input ->
        input.copyTo(output, CopyBufferBytes)
    }
}

internal fun buildWaveformPreviewFromPcmFile(
    pcmFilePath: String,
    totalSamples: Int,
    targetPoints: Int,
): ShortArray {
    if (totalSamples <= 0 || targetPoints <= 0) {
        return shortArrayOf()
    }
    val bucketSize = max(1, totalSamples / targetPoints)
    val preview = ArrayList<Short>(targetPoints)
    var bucketMax = 0
    var bucketSamples = 0
    forEachPcmSample(pcmFilePath) { sample ->
        bucketMax = max(bucketMax, abs(sample.toInt()))
        bucketSamples += 1
        if (bucketSamples >= bucketSize) {
            preview += bucketMax.coerceAtMost(Short.MAX_VALUE.toInt()).toShort()
            bucketMax = 0
            bucketSamples = 0
        }
    }
    if (bucketSamples > 0) {
        preview += bucketMax.coerceAtMost(Short.MAX_VALUE.toInt()).toShort()
    }
    return preview.toShortArray()
}

internal fun readPcmSegmentsFromFile(
    pcmFilePath: String,
    segmentSampleCounts: List<Int>,
): List<ShortArray> {
    if (segmentSampleCounts.isEmpty()) {
        return emptyList()
    }
    BufferedInputStream(FileInputStream(pcmFilePath)).use { input ->
        return segmentSampleCounts.map { sampleCount ->
            readShortArray(input, sampleCount)
        }
    }
}

internal data class WavPcmExtractionResult(
    val sampleRateHz: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val sampleCount: Int,
)

internal fun extractMonoPcm16WavToCache(
    input: InputStream,
    appendPcm: (ShortArray) -> Unit,
): WavPcmExtractionResult? {
    val buffered = BufferedInputStream(input, CopyBufferBytes)
    if (buffered.readFourCc() != "RIFF") {
        return null
    }
    buffered.skipExact(4)
    if (buffered.readFourCc() != "WAVE") {
        return null
    }

    var sampleRateHz = 0
    var channels = 0
    var bitsPerSample = 0
    var sampleCount = 0

    while (true) {
        val chunkId = buffered.readFourCcOrNull() ?: break
        val chunkSize = buffered.readInt32LE()
        when (chunkId) {
            "fmt " -> {
                val audioFormat = buffered.readInt16LE()
                channels = buffered.readInt16LE()
                sampleRateHz = buffered.readInt32LE()
                buffered.skipExact(6)
                bitsPerSample = buffered.readInt16LE()
                val remaining = chunkSize - 16
                if (remaining > 0) {
                    buffered.skipExact(remaining)
                }
                if (audioFormat != 1) {
                    return null
                }
            }

            "data" -> {
                if (channels != 1 || bitsPerSample != 16 || sampleRateHz <= 0) {
                    return null
                }
                sampleCount = streamPcmDataChunk(buffered, chunkSize, appendPcm)
                if (chunkSize % 2 != 0) {
                    buffered.skipExact(1)
                }
                return WavPcmExtractionResult(
                    sampleRateHz = sampleRateHz,
                    channels = channels,
                    bitsPerSample = bitsPerSample,
                    sampleCount = sampleCount,
                )
            }

            else -> buffered.skipExact(chunkSize + (chunkSize % 2))
        }
    }
    return null
}

private inline fun forEachPcmSample(
    pcmFilePath: String,
    consume: (Short) -> Unit,
) {
    BufferedInputStream(FileInputStream(pcmFilePath)).use { input ->
        val byteBuffer = ByteArray(CopyBufferBytes)
        while (true) {
            val bytesRead = input.read(byteBuffer)
            if (bytesRead <= 0) {
                break
            }
            var byteIndex = 0
            val limit = bytesRead - (bytesRead % ShortBytes)
            while (byteIndex < limit) {
                val low = byteBuffer[byteIndex].toInt() and 0xFF
                val high = byteBuffer[byteIndex + 1].toInt() shl 8
                consume((high or low).toShort())
                byteIndex += ShortBytes
            }
        }
    }
}

private fun readShortArray(
    input: BufferedInputStream,
    sampleCount: Int,
): ShortArray {
    val buffer = ShortArray(sampleCount)
    val byteBuffer = ByteArray(sampleCount * ShortBytes)
    var bytesFilled = 0
    while (bytesFilled < byteBuffer.size) {
        val bytesRead = input.read(byteBuffer, bytesFilled, byteBuffer.size - bytesFilled)
        if (bytesRead <= 0) {
            throw IOException("Unexpected end of PCM file.")
        }
        bytesFilled += bytesRead
    }
    var byteIndex = 0
    repeat(sampleCount) { sampleIndex ->
        val low = byteBuffer[byteIndex].toInt() and 0xFF
        val high = byteBuffer[byteIndex + 1].toInt() shl 8
        buffer[sampleIndex] = (high or low).toShort()
        byteIndex += ShortBytes
    }
    return buffer
}

private fun OutputStream.writeAscii(value: String) {
    write(value.toByteArray(Charsets.US_ASCII))
}

private fun BufferedInputStream.readFourCc(): String {
    val bytes = ByteArray(4)
    if (read(bytes) != bytes.size) {
        throw IOException("Unexpected end of stream.")
    }
    return bytes.toString(Charsets.US_ASCII)
}

private fun BufferedInputStream.readFourCcOrNull(): String? {
    val bytes = ByteArray(4)
    val readCount = read(bytes)
    if (readCount < 0) {
        return null
    }
    if (readCount != bytes.size) {
        throw IOException("Unexpected end of stream.")
    }
    return bytes.toString(Charsets.US_ASCII)
}

private fun BufferedInputStream.readInt16LE(): Int {
    val low = read()
    val high = read()
    if (low < 0 || high < 0) {
        throw IOException("Unexpected end of stream.")
    }
    return low or (high shl 8)
}

private fun BufferedInputStream.readInt32LE(): Int {
    val b1 = read()
    val b2 = read()
    val b3 = read()
    val b4 = read()
    if (b1 < 0 || b2 < 0 || b3 < 0 || b4 < 0) {
        throw IOException("Unexpected end of stream.")
    }
    return b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)
}

private fun BufferedInputStream.skipExact(bytes: Int) {
    var remaining = bytes.toLong()
    while (remaining > 0) {
        val skipped = skip(remaining)
        if (skipped > 0) {
            remaining -= skipped
            continue
        }
        if (read() == -1) {
            throw IOException("Unexpected end of stream.")
        }
        remaining -= 1
    }
}

private fun streamPcmDataChunk(
    input: BufferedInputStream,
    chunkSize: Int,
    appendPcm: (ShortArray) -> Unit,
): Int {
    var remaining = chunkSize
    var totalSamples = 0
    val byteBuffer = ByteArray(CopyBufferBytes)
    while (remaining > 0) {
        val toRead = minOf(byteBuffer.size, remaining)
        val bytesRead = input.read(byteBuffer, 0, toRead)
        if (bytesRead <= 0) {
            throw IOException("Unexpected end of stream.")
        }
        val sampleCount = bytesRead / ShortBytes
        if (sampleCount > 0) {
            val pcm = ShortArray(sampleCount)
            var byteIndex = 0
            repeat(sampleCount) { sampleIndex ->
                val low = byteBuffer[byteIndex].toInt() and 0xFF
                val high = byteBuffer[byteIndex + 1].toInt() shl 8
                pcm[sampleIndex] = (high or low).toShort()
                byteIndex += ShortBytes
            }
            appendPcm(pcm)
            totalSamples += sampleCount
        }
        remaining -= bytesRead
    }
    return totalSamples
}

private fun OutputStream.writeInt16LE(value: Int) {
    write(byteArrayOf((value and 0xFF).toByte(), ((value ushr 8) and 0xFF).toByte()))
}

private fun OutputStream.writeInt32LE(value: Int) {
    write(
        byteArrayOf(
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 24) and 0xFF).toByte(),
        ),
    )
}

private const val CopyBufferBytes = 32 * 1024
private const val ShortBytes = 2
private const val WaveHeaderBytes = 44
