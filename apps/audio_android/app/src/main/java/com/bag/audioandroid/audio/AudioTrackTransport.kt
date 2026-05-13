package com.bag.audioandroid.audio

import android.media.AudioTrack
import kotlin.math.roundToInt

internal fun safelyStopTrack(track: AudioTrack) {
    try {
        if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
            track.stop()
        }
    } catch (_: IllegalStateException) {
        // Ignore stop races while the track is being torn down.
    }
}

internal fun safelyPauseTrack(track: AudioTrack) {
    try {
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            track.pause()
        }
    } catch (_: IllegalStateException) {
        // Ignore pause races while the track is being torn down.
    }
}

internal fun safelyPlayTrack(track: AudioTrack) {
    try {
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            track.play()
        }
    } catch (_: IllegalStateException) {
        // Ignore play races while the track is being torn down.
    }
}

internal fun setPlaybackHeadPositionSafely(
    track: AudioTrack,
    sampleIndex: Int,
): Boolean =
    try {
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            track.pause()
        }
        track.setPlaybackHeadPosition(sampleIndex)
        true
    } catch (_: IllegalStateException) {
        false
    }

internal fun setPlaybackSpeedSafely(
    track: AudioTrack,
    playbackSpeed: Float,
): Boolean {
    val resolvedPlaybackSpeed = playbackSpeed.coerceAtLeast(0.1f)
    return try {
        track.playbackParams =
            track.playbackParams
                .allowDefaults()
                .setPitch(1.0f)
                .setSpeed(resolvedPlaybackSpeed)
        true
    } catch (_: Exception) {
        setPlaybackRateFallback(track, resolvedPlaybackSpeed)
    }
}

@Suppress("DEPRECATION")
private fun setPlaybackRateFallback(
    track: AudioTrack,
    playbackSpeed: Float,
): Boolean =
    try {
        track.playbackRate = (track.sampleRate * playbackSpeed).roundToInt().coerceAtLeast(1)
        true
    } catch (_: Exception) {
        false
    }
