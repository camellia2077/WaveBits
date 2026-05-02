package com.bag.audioandroid.ui.model

internal data class AudioInputEncodingAnalysis(
    val isBlockingInvalid: Boolean,
    val unsupportedCharacters: List<String> = emptyList(),
    val normalizedText: String = "",
    val morseNotation: String = "",
)

internal fun analyzeAudioInputEncoding(
    transportMode: TransportModeOption,
    inputText: String,
): AudioInputEncodingAnalysis =
    when (transportMode) {
        TransportModeOption.Mini -> analyzeMiniInputEncoding(inputText)
        TransportModeOption.Pro -> analyzeProInputEncoding(inputText)
        TransportModeOption.Flash,
        TransportModeOption.Ultra,
        -> AudioInputEncodingAnalysis(isBlockingInvalid = false)
    }

private fun analyzeMiniInputEncoding(inputText: String): AudioInputEncodingAnalysis {
    val morseAnalysis = analyzeMorseText(inputText)
    return AudioInputEncodingAnalysis(
        isBlockingInvalid = !morseAnalysis.isValid,
        unsupportedCharacters = morseAnalysis.unsupportedCharacters.map(Char::toString),
        normalizedText = morseAnalysis.normalizedText,
        morseNotation = morseAnalysis.morseNotation,
    )
}

private fun analyzeProInputEncoding(inputText: String): AudioInputEncodingAnalysis {
    val unsupportedCharacters =
        inputText
            .codePoints()
            .toArray()
            .filter { codePoint -> codePoint > AsciiMaxCodePoint }
            .distinct()
            .map { codePoint -> String(Character.toChars(codePoint)) }
    return AudioInputEncodingAnalysis(
        isBlockingInvalid = unsupportedCharacters.isNotEmpty(),
        unsupportedCharacters = unsupportedCharacters,
    )
}

private const val AsciiMaxCodePoint = 0x7F
