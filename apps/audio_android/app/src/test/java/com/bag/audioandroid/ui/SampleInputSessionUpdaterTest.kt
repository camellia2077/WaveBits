package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInput
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SampleInputSessionUpdaterTest {
    private val provider = FakeSampleInputTextProvider()
    private val updater = SampleInputSessionUpdater(provider)

    @Test
    fun `initialize uses stable default sample for every mode`() {
        val updated = updater.initialize(
            sessions = TransportModeOption.entries.associateWith { ModeAudioSessionState() },
            language = AppLanguageOption.English
        )

        TransportModeOption.entries.forEach { mode ->
            val expected = provider.defaultSample(mode, AppLanguageOption.English)
            assertEquals(expected.text, updated.getValue(mode).inputText)
            assertEquals(expected.id, updated.getValue(mode).sampleInputId)
        }
    }

    @Test
    fun `language change remaps existing sample id but keeps custom text untouched`() {
        val sessions = mapOf(
            TransportModeOption.Flash to ModeAudioSessionState(
                inputText = "flash-en-b",
                sampleInputId = "b"
            ),
            TransportModeOption.Pro to ModeAudioSessionState(
                inputText = "CUSTOM INPUT",
                sampleInputId = null
            ),
            TransportModeOption.Ultra to ModeAudioSessionState(
                inputText = "ultra-en-a",
                sampleInputId = "a"
            )
        )

        val updated = updater.refreshForLanguageChange(
            sessions = sessions,
            previousLanguage = AppLanguageOption.English,
            newLanguage = AppLanguageOption.Japanese
        )

        assertEquals("flash-ja-b", updated.getValue(TransportModeOption.Flash).inputText)
        assertEquals("b", updated.getValue(TransportModeOption.Flash).sampleInputId)
        assertEquals("CUSTOM INPUT", updated.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.getValue(TransportModeOption.Pro).sampleInputId)
        assertEquals("ultra-ja-a", updated.getValue(TransportModeOption.Ultra).inputText)
        assertEquals("a", updated.getValue(TransportModeOption.Ultra).sampleInputId)
    }
}

private class FakeSampleInputTextProvider : SampleInputTextProvider {
    private val flashUltraSamples = mapOf(
        AppLanguageOption.English to listOf(
            SampleInput("a", "flash-en-a"),
            SampleInput("b", "flash-en-b")
        ),
        AppLanguageOption.Japanese to listOf(
            SampleInput("a", "flash-ja-a"),
            SampleInput("b", "flash-ja-b")
        )
    )
    private val proSamples = listOf(
        SampleInput("a", "PRO-A"),
        SampleInput("b", "PRO-B")
    )

    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption
    ): SampleInput = entries(mode, language).first()

    override fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        excludingSampleId: String?
    ): SampleInput = entries(mode, language).first { it.id != excludingSampleId }

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        sampleId: String
    ): SampleInput? = entries(mode, language).firstOrNull { it.id == sampleId }

    private fun entries(
        mode: TransportModeOption,
        language: AppLanguageOption
    ): List<SampleInput> =
        when (mode) {
            TransportModeOption.Pro -> proSamples
            TransportModeOption.Flash, TransportModeOption.Ultra -> {
                val prefix = if (mode == TransportModeOption.Ultra) "ultra" else "flash"
                flashUltraSamples
                    .getValue(language)
                    .map { sample -> sample.copy(text = sample.text.replace("flash", prefix)) }
            }
        }
}
