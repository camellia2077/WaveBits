package com.bag.audioandroid.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class AudioPlaybackCoordinator(
    private val audioPlayer: AudioPlayer = AudioPlayer(),
) {
    private val playbackRequestIds = AtomicLong(0L)

    @Volatile
    private var activePlaybackRequestId: Long? = null

    @Volatile
    private var activePlaybackKey: String? = null

    @Volatile
    private var playbackPaused = false

    fun hasActivePlaybackForOtherSource(sourceKey: String): Boolean = activePlaybackKey != null && activePlaybackKey != sourceKey

    fun isPlaybackActiveForSource(sourceKey: String): Boolean = activePlaybackRequestId != null && activePlaybackKey == sourceKey

    fun hasActivePlaybackForSource(sourceKey: String): Boolean = isPlaybackActiveForSource(sourceKey)

    fun isPlaybackPausedForSource(sourceKey: String): Boolean = isPlaybackActiveForSource(sourceKey) && playbackPaused

    fun startPlayback(
        scope: CoroutineScope,
        sourceKey: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        totalSamples: Int,
        sampleRateHz: Int,
        playbackSpeed: Float,
        startSampleIndex: Int,
        onStarted: () -> Unit,
        onProgressChanged: (Int, Int) -> Unit,
        onFinished: (String, PlaybackResult) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        val playbackHandle = audioPlayer.prepareForNewPlayback()
        val requestId = beginPlayback(sourceKey)
        onStarted()
        scope.launch(Dispatchers.IO) {
            try {
                val result =
                    if (pcmFilePath.isNullOrBlank()) {
                        audioPlayer.playPcm(
                            playback = playbackHandle,
                            pcm = pcm.copyOf(),
                            sampleRateHz = sampleRateHz,
                            playbackSpeed = playbackSpeed,
                            startSampleIndex = startSampleIndex,
                        ) { playedSamples, reportedTotalSamples ->
                            if (isPlaybackActive(requestId, sourceKey)) {
                                onProgressChanged(playedSamples, reportedTotalSamples)
                            }
                        }
                    } else {
                        audioPlayer.playPcmFile(
                            playback = playbackHandle,
                            pcmFilePath = pcmFilePath,
                            sampleRateHz = sampleRateHz,
                            totalSamples = totalSamples,
                            playbackSpeed = playbackSpeed,
                            startSampleIndex = startSampleIndex,
                        ) { playedSamples, reportedTotalSamples ->
                            if (isPlaybackActive(requestId, sourceKey)) {
                                onProgressChanged(playedSamples, reportedTotalSamples)
                            }
                        }
                    }
                if (!isPlaybackActive(requestId, sourceKey)) {
                    return@launch
                }
                clearActivePlayback(requestId)
                onFinished(sourceKey, result)
            } catch (_: Exception) {
                if (!isPlaybackActive(requestId, sourceKey)) {
                    return@launch
                }
                clearActivePlayback(requestId)
                onFailed(sourceKey)
            }
        }
    }

    fun pausePlayback(sourceKey: String): Boolean {
        if (!isPlaybackActiveForSource(sourceKey) || playbackPaused) {
            return false
        }
        playbackPaused = true
        audioPlayer.pause()
        return true
    }

    fun resumePlayback(sourceKey: String): Boolean {
        if (!isPlaybackActiveForSource(sourceKey) || !playbackPaused) {
            return false
        }
        playbackPaused = false
        audioPlayer.resume()
        return true
    }

    fun setPlaybackSpeed(
        sourceKey: String,
        playbackSpeed: Float,
    ): Boolean {
        if (activePlaybackKey != sourceKey) {
            return false
        }
        return audioPlayer.setPlaybackSpeed(playbackSpeed)
    }

    fun beginScrub(sourceKey: String): Boolean =
        if (isPlaybackActiveForSource(sourceKey)) {
            pausePlayback(sourceKey)
        } else {
            false
        }

    fun updateScrub(
        sourceKey: String,
        targetSamples: Int,
    ): Boolean = isPlaybackActiveForSource(sourceKey) && targetSamples >= 0

    fun commitScrub(
        sourceKey: String,
        targetSamples: Int,
        resumeAfterCommit: Boolean,
    ): Boolean {
        if (!isPlaybackActiveForSource(sourceKey)) {
            return false
        }
        val appliedPosition = audioPlayer.seekTo(targetSamples) ?: return false
        playbackPaused = !resumeAfterCommit
        if (resumeAfterCommit) {
            audioPlayer.resume()
        } else {
            audioPlayer.pause()
        }
        return appliedPosition >= 0
    }

    fun cancelScrub(
        sourceKey: String,
        resumeAfterCancel: Boolean,
    ): Boolean {
        if (!isPlaybackActiveForSource(sourceKey)) {
            return false
        }
        playbackPaused = !resumeAfterCancel
        if (resumeAfterCancel) {
            audioPlayer.resume()
        } else {
            audioPlayer.pause()
        }
        return true
    }

    fun stopPlayback(onStopped: (String) -> Unit = {}) {
        val playbackKey = activePlaybackKey ?: return
        clearActivePlayback()
        audioPlayer.stop()
        onStopped(playbackKey)
    }

    fun release() {
        clearActivePlayback()
        audioPlayer.stop()
    }

    private fun beginPlayback(sourceKey: String): Long {
        val requestId = playbackRequestIds.incrementAndGet()
        activePlaybackRequestId = requestId
        activePlaybackKey = sourceKey
        playbackPaused = false
        return requestId
    }

    private fun clearActivePlayback(requestId: Long? = null) {
        if (requestId == null || activePlaybackRequestId == requestId) {
            activePlaybackRequestId = null
            activePlaybackKey = null
            playbackPaused = false
        }
    }

    private fun isPlaybackActive(
        requestId: Long,
        sourceKey: String,
    ): Boolean = activePlaybackRequestId == requestId && activePlaybackKey == sourceKey
}
