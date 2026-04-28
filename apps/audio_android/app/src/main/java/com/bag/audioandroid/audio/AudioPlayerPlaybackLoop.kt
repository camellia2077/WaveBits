package com.bag.audioandroid.audio

import android.media.AudioTrack
import java.io.BufferedInputStream

internal fun runStaticPlaybackLoop(
    playback: AudioPlayer.PlaybackHandle,
    track: AudioTrack,
    pcm: ShortArray,
    playbackStartOffsetSamples: Int = 0,
    reportedTotalSamples: Int = pcm.size,
    onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
): PlaybackResult {
    val bufferedSamples = pcm.size
    track.write(pcm, 0, pcm.size)
    val initialPosition = playbackStartOffsetSamples.coerceIn(0, reportedTotalSamples)
    onProgressChanged(initialPosition, reportedTotalSamples)
    track.play()
    while (!playback.stopRequested && track.playbackHeadPosition < bufferedSamples) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
        }
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            Thread.sleep(PlaybackProgressUpdateIntervalMs)
            continue
        }
        val reportedProgress =
            (playbackStartOffsetSamples + track.playbackHeadPosition)
                .coerceIn(0, reportedTotalSamples)
        onProgressChanged(reportedProgress, reportedTotalSamples)
        Thread.sleep(PlaybackProgressUpdateIntervalMs)
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    onProgressChanged(reportedTotalSamples, reportedTotalSamples)
    return PlaybackResult.Completed
}

internal fun runStreamingPlaybackLoop(
    playback: AudioPlayer.PlaybackHandle,
    track: AudioTrack,
    input: BufferedInputStream,
    playbackStartOffsetSamples: Int = 0,
    reportedTotalSamples: Int,
    onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
): PlaybackResult {
    val sampleBuffer = ShortArray(StreamingChunkSamples)
    var streamedSamples = 0
    onProgressChanged(playbackStartOffsetSamples, reportedTotalSamples)
    safelyPlayTrack(track)
    while (!playback.stopRequested) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
        }
        val samplesRead = readShortSamples(input, sampleBuffer)
        if (samplesRead <= 0) {
            break
        }
        var writeOffset = 0
        while (writeOffset < samplesRead && !playback.stopRequested) {
            if (playback.pauseRequested) {
                safelyPauseTrack(track)
                while (playback.pauseRequested && !playback.stopRequested) {
                    Thread.sleep(PlaybackProgressUpdateIntervalMs)
                }
                if (playback.stopRequested) {
                    return PlaybackResult.Stopped
                }
                safelyPlayTrack(track)
            }
            val written = track.write(sampleBuffer, writeOffset, samplesRead - writeOffset, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) {
                return PlaybackResult.Stopped
            }
            writeOffset += written
            streamedSamples += written
            val reportedProgress =
                (playbackStartOffsetSamples + streamedSamples)
                    .coerceIn(0, reportedTotalSamples)
            onProgressChanged(reportedProgress, reportedTotalSamples)
        }
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    while (!playback.stopRequested && track.playbackHeadPosition < streamedSamples) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
        }
        val reportedProgress =
            (playbackStartOffsetSamples + track.playbackHeadPosition)
                .coerceIn(0, reportedTotalSamples)
        onProgressChanged(reportedProgress, reportedTotalSamples)
        Thread.sleep(PlaybackProgressUpdateIntervalMs)
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    onProgressChanged(reportedTotalSamples, reportedTotalSamples)
    return PlaybackResult.Completed
}

private fun readShortSamples(
    input: BufferedInputStream,
    buffer: ShortArray,
): Int {
    val byteBuffer = ByteArray(buffer.size * 2)
    val bytesRead = input.read(byteBuffer)
    if (bytesRead <= 0) {
        return 0
    }
    val sampleCount = bytesRead / 2
    var byteIndex = 0
    repeat(sampleCount) { sampleIndex ->
        val low = byteBuffer[byteIndex].toInt() and 0xFF
        val high = byteBuffer[byteIndex + 1].toInt() shl 8
        buffer[sampleIndex] = (high or low).toShort()
        byteIndex += 2
    }
    return sampleCount
}

private const val PlaybackProgressUpdateIntervalMs = 50L
private const val StreamingChunkSamples = 4096
