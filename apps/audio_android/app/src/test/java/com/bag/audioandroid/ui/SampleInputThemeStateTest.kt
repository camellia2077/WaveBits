package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInput
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.theme.BrandDualToneThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SampleInputThemeStateTest {
    private val provider = ThemeStateFakeSampleInputTextProvider()
    private val updater = SampleInputSessionUpdater(provider)

    @Test
    fun `brand theme change within same flavor leaves sampled sessions untouched`() {
        val marsRelic = BrandDualToneThemes.first { it.id == "mars_relic" }
        val scarletGuard = BrandDualToneThemes.first { it.id == "scarlet_guard" }
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.BrandDualTone,
                selectedBrandTheme = marsRelic,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "sacred-en-a",
                                sampleInputId = "a",
                            ),
                        TransportModeOption.Pro to
                            ModeAudioSessionState(
                                inputText = "CUSTOM INPUT",
                                sampleInputId = null,
                            ),
                        TransportModeOption.Ultra to
                            ModeAudioSessionState(
                                inputText = "sacred-en-b",
                                sampleInputId = "b",
                            ),
                    ),
            )

        val updated = state.withSelectedBrandTheme(scarletGuard, updater)

        assertEquals("scarlet_guard", updated.selectedBrandTheme.id)
        assertEquals("sacred-en-a", updated.sessions.getValue(TransportModeOption.Flash).inputText)
        assertEquals("a", updated.sessions.getValue(TransportModeOption.Flash).sampleInputId)
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertEquals("sacred-en-b", updated.sessions.getValue(TransportModeOption.Ultra).inputText)
        assertEquals("b", updated.sessions.getValue(TransportModeOption.Ultra).sampleInputId)
    }

    @Test
    fun `brand theme change across flavors refreshes sampled sessions only`() {
        val marsRelic = BrandDualToneThemes.first { it.id == "mars_relic" }
        val ancientAlloy = BrandDualToneThemes.first { it.id == "ancient_alloy" }
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.BrandDualTone,
                selectedBrandTheme = marsRelic,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "sacred-en-a",
                                sampleInputId = "a",
                            ),
                        TransportModeOption.Pro to
                            ModeAudioSessionState(
                                inputText = "CUSTOM INPUT",
                                sampleInputId = null,
                            ),
                        TransportModeOption.Ultra to
                            ModeAudioSessionState(
                                inputText = "sacred-en-b",
                                sampleInputId = "b",
                            ),
                    ),
            )

        val updated = state.withSelectedBrandTheme(ancientAlloy, updater)

        assertEquals("ancient_alloy", updated.selectedBrandTheme.id)
        assertEquals("dynasty-en-a", updated.sessions.getValue(TransportModeOption.Flash).inputText)
        assertEquals("a", updated.sessions.getValue(TransportModeOption.Flash).sampleInputId)
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertEquals("dynasty-en-b", updated.sessions.getValue(TransportModeOption.Ultra).inputText)
        assertEquals("b", updated.sessions.getValue(TransportModeOption.Ultra).sampleInputId)
    }

    @Test
    fun `switching away from dual tone falls back to sacred machine flavor`() {
        val ancientAlloy = BrandDualToneThemes.first { it.id == "ancient_alloy" }
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.BrandDualTone,
                selectedBrandTheme = ancientAlloy,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "dynasty-en-a",
                                sampleInputId = "a",
                            ),
                        TransportModeOption.Pro to
                            ModeAudioSessionState(
                                inputText = "CUSTOM INPUT",
                                sampleInputId = null,
                            ),
                        TransportModeOption.Ultra to
                            ModeAudioSessionState(
                                inputText = "dynasty-en-b",
                                sampleInputId = "b",
                            ),
                    ),
            )

        val updated = state.withSelectedThemeStyle(ThemeStyleOption.Material, updater)

        assertEquals(ThemeStyleOption.Material, updated.selectedThemeStyle)
        assertEquals("sacred-en-a", updated.sessions.getValue(TransportModeOption.Flash).inputText)
        assertEquals("a", updated.sessions.getValue(TransportModeOption.Flash).sampleInputId)
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertEquals("sacred-en-b", updated.sessions.getValue(TransportModeOption.Ultra).inputText)
        assertEquals("b", updated.sessions.getValue(TransportModeOption.Ultra).sampleInputId)
    }
}

private class ThemeStateFakeSampleInputTextProvider : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = entries(mode, language, flavor).first()

    override fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        excludingSampleId: String?,
    ): SampleInput = entries(mode, language, flavor).first { it.id != excludingSampleId }

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? = entries(mode, language, flavor).firstOrNull { it.id == sampleId }

    private fun entries(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): List<SampleInput> =
        when (mode) {
            TransportModeOption.Pro ->
                listOf(
                    SampleInput("a", "PRO-CUSTOM-A"),
                    SampleInput("b", "PRO-CUSTOM-B"),
                )

            TransportModeOption.Flash, TransportModeOption.Ultra ->
                when (flavor) {
                    SampleFlavor.SacredMachine ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "sacred-ja-a"),
                                    SampleInput("b", "sacred-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "sacred-en-a"),
                                    SampleInput("b", "sacred-en-b"),
                                )
                        }
                    SampleFlavor.AncientDynasty ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "dynasty-ja-a"),
                                    SampleInput("b", "dynasty-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "dynasty-en-a"),
                                    SampleInput("b", "dynasty-en-b"),
                                )
                        }
                    SampleFlavor.ImmortalRot ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "rot-ja-a"),
                                    SampleInput("b", "rot-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "rot-en-a"),
                                    SampleInput("b", "rot-en-b"),
                                )
                        }
                    SampleFlavor.ScarletCarnage ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "scarlet-ja-a"),
                                    SampleInput("b", "scarlet-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "scarlet-en-a"),
                                    SampleInput("b", "scarlet-en-b"),
                                )
                        }
                    SampleFlavor.ExquisiteFall ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "exquisite-ja-a"),
                                    SampleInput("b", "exquisite-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "exquisite-en-a"),
                                    SampleInput("b", "exquisite-en-b"),
                                )
                        }
                    SampleFlavor.LabyrinthOfMutability ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "labyrinth-ja-a"),
                                    SampleInput("b", "labyrinth-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "labyrinth-en-a"),
                                    SampleInput("b", "labyrinth-en-b"),
                                )
                        }
                }
        }
}
