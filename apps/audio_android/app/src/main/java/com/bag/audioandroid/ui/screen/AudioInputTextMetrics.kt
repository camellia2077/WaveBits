package com.bag.audioandroid.ui.screen

import java.nio.charset.StandardCharsets.UTF_8

internal data class AudioInputTextMetrics(
    val characterCount: Int,
    val byteCount: Int,
)

internal fun measureAudioInputText(inputText: String): AudioInputTextMetrics {
    val utf8ByteCount = inputText.toByteArray(UTF_8).size
    // Keep the visible counter aligned with user-perceived characters instead of
    // UTF-16 code units, while the byte budget continues to track transport size.
    return AudioInputTextMetrics(
        characterCount = inputText.codePointCount(0, inputText.length),
        byteCount = utf8ByteCount,
    )
}
