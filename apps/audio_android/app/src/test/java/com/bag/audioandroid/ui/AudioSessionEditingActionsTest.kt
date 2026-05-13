package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInput
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AudioSessionEditingActionsTest {
    @Test
    fun `randomize sample input walks the whole shuffled deck before repeating`() {
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    selectedLanguage = AppLanguageOption.English,
                    isSampleDecorationEnabled = false,
                    transportMode = TransportModeOption.Flash,
                    sessions =
                        sessionsWithCurrentFlash(
                            ModeAudioSessionState(
                                inputText = "flash-a",
                                sampleInputId = "a",
                            ),
                        ),
                ),
            )
        val actions =
            createActions(
                state = state,
                provider = CyclingSampleInputTextProvider(),
                random = FixedShuffleRandom(),
            )

        actions.onRandomizeSampleInput(SampleInputLengthOption.Short)
        val firstSeen = state.value.currentSession.sampleInputId
        assertTrue(firstSeen in setOf("b", "c"))

        actions.onRandomizeSampleInput(SampleInputLengthOption.Short)
        val secondSeen = state.value.currentSession.sampleInputId
        assertTrue(secondSeen in setOf("b", "c"))
        assertTrue(firstSeen != secondSeen)
    }

    @Test
    fun `randomize sample input respects requested sample length`() {
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    selectedLanguage = AppLanguageOption.English,
                    isSampleDecorationEnabled = false,
                    transportMode = TransportModeOption.Flash,
                    sessions =
                        sessionsWithCurrentFlash(
                            ModeAudioSessionState(
                                inputText = "flash-short-a",
                                sampleInputId = "short-a",
                            ),
                        ),
                ),
            )
        val actions = createActions(state, LengthAwareSampleInputTextProvider(), FixedShuffleRandom())

        actions.onRandomizeSampleInput(SampleInputLengthOption.Long)

        assertEquals("long-b", state.value.currentSession.sampleInputId)
        assertEquals("flash-long-b", state.value.currentSession.inputText)
    }

    @Test
    fun `manual input clears sample id and mode switch preserves per mode text`() {
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    selectedLanguage = AppLanguageOption.English,
                    transportMode = TransportModeOption.Flash,
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to
                                ModeAudioSessionState(
                                    inputText = "flash-a",
                                    sampleInputId = "a",
                                ),
                            TransportModeOption.Pro to
                                ModeAudioSessionState(
                                    inputText = "PRO-A",
                                    sampleInputId = "a",
                                ),
                            TransportModeOption.Ultra to
                                ModeAudioSessionState(
                                    inputText = "ultra-custom",
                                    sampleInputId = null,
                                ),
                            TransportModeOption.Mini to
                                ModeAudioSessionState(
                                    inputText = "MINI-A",
                                    sampleInputId = "a",
                                ),
                        ),
                ),
            )
        val actions = createActions(state, CyclingSampleInputTextProvider(), FixedShuffleRandom())

        actions.onInputTextChange("typed by user")
        assertEquals("typed by user", state.value.currentSession.inputText)
        assertNull(state.value.currentSession.sampleInputId)
        assertNull(state.value.currentSession.sampleShuffleState)
        assertNull(state.value.currentSession.appliedSampleEmojiPrefix)

        actions.onTransportModeSelected(TransportModeOption.Ultra)
        assertEquals(TransportModeOption.Ultra, state.value.transportMode)
        assertEquals(
            "typed by user",
            state.value.sessions
                .getValue(TransportModeOption.Flash)
                .inputText,
        )
        assertEquals("ultra-custom", state.value.currentSession.inputText)
    }

    @Test
    fun `sample decoration toggle removes and reapplies only app generated emoji prefix`() {
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                isSampleDecorationEnabled = true,
                transportMode = TransportModeOption.Flash,
                sessions =
                    sessionsWithCurrentFlash(
                        ModeAudioSessionState(
                            inputText = "\uD83D\uDEE0\uFE0F flash-a",
                            sampleInputId = "a",
                            appliedSampleEmojiPrefix = "\uD83D\uDEE0\uFE0F",
                        ),
                    ),
            )

        val disabled =
            state.withSampleDecoration(
                isDecorationEnabled = false,
            )
        assertEquals("flash-a", disabled.currentSession.inputText)
        assertNull(disabled.currentSession.appliedSampleEmojiPrefix)

        val enabled =
            disabled.withSampleDecoration(
                isDecorationEnabled = true,
            )
        assertTrue(enabled.currentSession.inputText.endsWith("flash-a"))
        assertTrue(enabled.currentSession.inputText != "flash-a")
        assertEquals(enabled.currentSession.appliedSampleEmojiPrefix, enabled.currentSession.sampleEmojiShuffleState?.lastPresentedEmoji)
    }

    @Test
    fun `sample decoration toggle does not remove user authored emoji text`() {
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                isSampleDecorationEnabled = true,
                transportMode = TransportModeOption.Flash,
                sessions =
                    sessionsWithCurrentFlash(
                        ModeAudioSessionState(
                            inputText = "\uD83D\uDE80 user text",
                            sampleInputId = null,
                            appliedSampleEmojiPrefix = null,
                        ),
                    ),
            )

        val disabled =
            state.withSampleDecoration(
                isDecorationEnabled = false,
            )

        assertEquals("\uD83D\uDE80 user text", disabled.currentSession.inputText)
        assertNull(disabled.currentSession.sampleInputId)
        assertNull(disabled.currentSession.appliedSampleEmojiPrefix)
    }

    private fun createActions(
        state: MutableStateFlow<AudioAppUiState>,
        provider: SampleInputTextProvider,
        random: Random = Random.Default,
    ) = AudioSessionEditingActions(
        uiState = state,
        sessionStateStore = AudioSessionStateStore(state),
        sampleInputTextProvider = provider,
        stopPlayback = {},
        refreshSavedAudioItems = {},
        random = random,
    )

    private fun sessionsWithCurrentFlash(flashSession: ModeAudioSessionState): Map<TransportModeOption, ModeAudioSessionState> =
        mapOf(
            TransportModeOption.Flash to flashSession,
            TransportModeOption.Pro to ModeAudioSessionState(inputText = "PRO-A", sampleInputId = "a"),
            TransportModeOption.Ultra to ModeAudioSessionState(inputText = "ultra-a", sampleInputId = "a"),
            TransportModeOption.Mini to ModeAudioSessionState(inputText = "MINI-A", sampleInputId = "a"),
        )
}

private class CyclingSampleInputTextProvider : SampleInputTextProvider {
    private val flashSamples =
        listOf(
            SampleInput("a", "flash-a"),
            SampleInput("b", "flash-b"),
            SampleInput("c", "flash-c"),
        )
    private val proSamples =
        listOf(
            SampleInput("a", "PRO-A"),
            SampleInput("b", "PRO-B"),
        )
    private val ultraSamples =
        listOf(
            SampleInput("a", "ultra-a"),
            SampleInput("b", "ultra-b"),
        )
    private val miniSamples =
        listOf(
            SampleInput("a", "MINI-A"),
            SampleInput("b", "MAX-B"),
        )

    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = samples(mode).first()

    override fun sampleIds(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<String> = samples(mode).map(SampleInput::id)

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? = samples(mode).firstOrNull { it.id == sampleId }

    private fun samples(mode: TransportModeOption): List<SampleInput> =
        when (mode) {
            TransportModeOption.Flash -> flashSamples
            TransportModeOption.Pro -> proSamples
            TransportModeOption.Ultra -> ultraSamples
            TransportModeOption.Mini -> miniSamples
        }
}

private class LengthAwareSampleInputTextProvider : SampleInputTextProvider {
    private val shortSamples =
        listOf(
            SampleInput("short-a", "flash-short-a"),
            SampleInput("short-b", "flash-short-b"),
        )
    private val longSamples =
        listOf(
            SampleInput("long-a", "flash-long-a"),
            SampleInput("long-b", "flash-long-b"),
        )

    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = shortSamples.first()

    override fun sampleIds(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<String> {
        val samples =
            when (length) {
                SampleInputLengthOption.Short -> shortSamples
                SampleInputLengthOption.Long -> longSamples
            }
        return samples.map(SampleInput::id)
    }

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? = (shortSamples + longSamples).firstOrNull { it.id == sampleId }
}

private class FixedShuffleRandom : Random() {
    override fun nextBits(bitCount: Int): Int = 0
}
