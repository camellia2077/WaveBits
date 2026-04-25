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
import org.junit.Test

class AudioSessionEditingActionsTest {
    @Test
    fun `randomize sample input replaces current sample without immediate repeats`() {
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    selectedLanguage = AppLanguageOption.English,
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
        val actions = createActions(state, CyclingSampleInputTextProvider())

        actions.onRandomizeSampleInput(SampleInputLengthOption.Short)
        assertEquals("flash-b", state.value.currentSession.inputText)
        assertEquals("b", state.value.currentSession.sampleInputId)

        actions.onRandomizeSampleInput(SampleInputLengthOption.Short)
        assertEquals("flash-a", state.value.currentSession.inputText)
        assertEquals("a", state.value.currentSession.sampleInputId)
    }

    @Test
    fun `randomize sample input respects requested sample length`() {
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    selectedLanguage = AppLanguageOption.English,
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
        val actions = createActions(state, LengthAwareSampleInputTextProvider())

        actions.onRandomizeSampleInput(SampleInputLengthOption.Long)

        assertEquals("flash-long-a", state.value.currentSession.inputText)
        assertEquals("long-a", state.value.currentSession.sampleInputId)
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
                        ),
                ),
            )
        val actions = createActions(state, CyclingSampleInputTextProvider())

        actions.onInputTextChange("typed by user")
        assertEquals("typed by user", state.value.currentSession.inputText)
        assertNull(state.value.currentSession.sampleInputId)

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

    private fun createActions(
        state: MutableStateFlow<AudioAppUiState>,
        provider: SampleInputTextProvider,
    ) = AudioSessionEditingActions(
        uiState = state,
        sessionStateStore = AudioSessionStateStore(state),
        sampleInputTextProvider = provider,
        stopPlayback = {},
        refreshSavedAudioItems = {},
    )

    private fun sessionsWithCurrentFlash(flashSession: ModeAudioSessionState): Map<TransportModeOption, ModeAudioSessionState> =
        mapOf(
            TransportModeOption.Flash to flashSession,
            TransportModeOption.Pro to ModeAudioSessionState(inputText = "PRO-A", sampleInputId = "a"),
            TransportModeOption.Ultra to ModeAudioSessionState(inputText = "ultra-a", sampleInputId = "a"),
        )
}

private class CyclingSampleInputTextProvider : SampleInputTextProvider {
    private val flashSamples =
        listOf(
            SampleInput("a", "flash-a"),
            SampleInput("b", "flash-b"),
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

    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = samples(mode).first()

    override fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        excludingSampleId: String?,
    ): SampleInput = samples(mode).firstOrNull { it.id != excludingSampleId } ?: samples(mode).first()

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

    override fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        excludingSampleId: String?,
    ): SampleInput {
        val samples =
            when (length) {
                SampleInputLengthOption.Short -> shortSamples
                SampleInputLengthOption.Long -> longSamples
            }
        return samples.firstOrNull { it.id != excludingSampleId } ?: samples.first()
    }

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? = (shortSamples + longSamples).firstOrNull { it.id == sampleId }
}
