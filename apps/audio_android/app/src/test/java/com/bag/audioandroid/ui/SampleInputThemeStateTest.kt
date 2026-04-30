package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInput
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.DefaultCustomBrandThemeSettings
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.theme.BrandDualToneThemes
import com.bag.audioandroid.ui.theme.customBrandTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "dynasty-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "dynasty-en")
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
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "sacred-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "sacred-en")
    }

    @Test
    fun `switching from sacred machine preset to custom theme keeps sacred machine samples`() {
        val marsRelic = BrandDualToneThemes.first { it.id == "mars_relic" }
        val customTheme = customBrandTheme(DefaultCustomBrandThemeSettings)
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

        val updated = state.withSelectedBrandTheme(customTheme, updater)

        assertEquals(customTheme.id, updated.selectedBrandTheme.id)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "sacred-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "sacred-en")
    }

    @Test
    fun `switching from ancient dynasty to custom theme falls back to sacred machine samples`() {
        val ancientAlloy = BrandDualToneThemes.first { it.id == "ancient_alloy" }
        val customTheme = customBrandTheme(DefaultCustomBrandThemeSettings)
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

        val updated = state.withSelectedBrandTheme(customTheme, updater)

        assertEquals(customTheme.id, updated.selectedBrandTheme.id)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "sacred-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "sacred-en")
    }

    @Test
    fun `switching to named custom preset still uses sacred machine samples`() {
        val ancientAlloy = BrandDualToneThemes.first { it.id == "ancient_alloy" }
        val customTheme =
            customBrandTheme(
                DefaultCustomBrandThemeSettings.copy(
                    presetId = "named-custom",
                    displayName = "Named Custom",
                ),
            )
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

        val updated = state.withSelectedBrandTheme(customTheme, updater)

        assertEquals(customTheme.id, updated.selectedBrandTheme.id)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "sacred-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "sacred-en")
    }

    private fun assertFlavorSample(
        session: ModeAudioSessionState,
        expectedPrefix: String,
    ) {
        assertTrue(session.inputText == "$expectedPrefix-a" || session.inputText == "$expectedPrefix-b")
        assertEquals(session.inputText.removePrefix("$expectedPrefix-"), session.sampleInputId)
    }
}

private class ThemeStateFakeSampleInputTextProvider : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = entries(mode, language, flavor).first()

    override fun sampleIds(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<String> = entries(mode, AppLanguageOption.English, flavor).map(SampleInput::id)

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
            TransportModeOption.Pro, TransportModeOption.Mini ->
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
