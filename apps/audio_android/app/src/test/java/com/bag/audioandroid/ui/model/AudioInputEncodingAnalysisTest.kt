package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioInputEncodingAnalysisTest {
    @Test
    fun `mini accepts morse compatible input and normalizes it`() {
        val analysis = analyzeAudioInputEncoding(TransportModeOption.Mini, "praise   1")

        assertFalse(analysis.isBlockingInvalid)
        assertEquals("PRAISE 1", analysis.normalizedText)
        assertEquals(".--. .-. .- .. ... . / .----", analysis.morseNotation)
    }

    @Test
    fun `mini blocks unsupported characters`() {
        val analysis = analyzeAudioInputEncoding(TransportModeOption.Mini, "你好")

        assertTrue(analysis.isBlockingInvalid)
        assertEquals(listOf("你", "好"), analysis.unsupportedCharacters)
    }

    @Test
    fun `pro accepts ascii input`() {
        val analysis = analyzeAudioInputEncoding(TransportModeOption.Pro, "ASCII 123")

        assertFalse(analysis.isBlockingInvalid)
        assertEquals(emptyList<String>(), analysis.unsupportedCharacters)
    }

    @Test
    fun `pro blocks non ascii code points`() {
        val analysis = analyzeAudioInputEncoding(TransportModeOption.Pro, "A你🚀")

        assertTrue(analysis.isBlockingInvalid)
        assertEquals(listOf("你", "🚀"), analysis.unsupportedCharacters)
    }
}
