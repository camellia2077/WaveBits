package com.bag.audioandroid.ui.screen

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatSavedAudioTime(savedAtEpochSeconds: Long): String {
    if (savedAtEpochSeconds <= 0L) {
        return ""
    }
    return Instant
        .ofEpochSecond(savedAtEpochSeconds)
        .atZone(ZoneId.systemDefault())
        .format(SAVED_AUDIO_TIME_FORMATTER)
}

internal fun samplesToMillis(
    samples: Int,
    sampleRateHz: Int,
): Long {
    if (samples <= 0 || sampleRateHz <= 0) {
        return 0L
    }
    return (samples.toLong() * 1000L) / sampleRateHz.toLong()
}

internal fun formatDurationMillis(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

internal fun formatStorageSizeMb(byteCount: Long): String {
    if (byteCount <= 0L) {
        return "0.00 MB"
    }
    val megabytes = byteCount.toDouble() / BytesPerMegabyte
    val displayMegabytes = megabytes.coerceAtLeast(0.01)
    return "%.2f MB".format(Locale.US, displayMegabytes)
}

private val SAVED_AUDIO_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private const val BytesPerMegabyte = 1024.0 * 1024.0
