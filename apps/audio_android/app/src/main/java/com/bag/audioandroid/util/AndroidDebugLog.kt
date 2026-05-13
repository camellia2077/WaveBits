package com.bag.audioandroid.util

import android.util.Log

internal inline fun <T> measureElapsedMs(block: () -> T): Pair<T, Long> {
    val startedAtNs = System.nanoTime()
    val result = block()
    val elapsedMs = (System.nanoTime() - startedAtNs) / 1_000_000L
    return result to elapsedMs
}

internal fun safeDebugLog(
    tag: String,
    message: String,
) {
    try {
        Log.d(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
    }
}
