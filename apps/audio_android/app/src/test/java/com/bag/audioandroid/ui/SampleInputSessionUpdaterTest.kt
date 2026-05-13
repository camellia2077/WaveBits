package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInput
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleInputSessionUpdaterTest {
    private val provider = UpdaterFakeSampleInputTextProvider()
    private val updater = SampleInputSessionUpdater(provider)

    @Test
    fun `initialize seeds a shuffled short-sample round for every mode`() {
        val updated =
            updater.initialize(
                sessions = TransportModeOption.entries.associateWith { ModeAudioSessionState() },
                language = AppLanguageOption.English,
                flavor = SampleFlavor.SacredMachine,
            )

        TransportModeOption.entries.forEach { mode ->
            val session = updated.getValue(mode)
            assertTrue(session.sampleInputId in setOf("a", "b"))
            assertTrue(session.inputText.isNotBlank())
            assertEquals(SampleFlavor.SacredMachine, session.sampleShuffleState?.flavor)
            assertEquals(SampleInputLengthOption.Short, session.sampleShuffleState?.length)
            assertEquals(1, session.sampleShuffleState?.nextSampleIndex)
            assertEquals(session.sampleInputId, session.sampleShuffleState?.lastPresentedSampleId)
        }
    }

    @Test
    fun `initialize decorates the first flash sample with emoji when decoration is enabled`() {
        val updated =
            updater.initialize(
                sessions = TransportModeOption.entries.associateWith { ModeAudioSessionState() },
                language = AppLanguageOption.English,
                flavor = SampleFlavor.SacredMachine,
                isDecorationEnabled = true,
            )

        val flashSession = updated.getValue(TransportModeOption.Flash)
        assertTrue(flashSession.inputText.contains("sacred-en-"))
        assertTrue(flashSession.inputText != "sacred-en-${flashSession.sampleInputId}")
        assertTrue(flashSession.inputText.endsWith("sacred-en-${flashSession.sampleInputId}"))
        assertEquals(1, flashSession.sampleEmojiShuffleState?.nextEmojiIndex)
        assertEquals(flashSession.appliedSampleEmojiPrefix, flashSession.sampleEmojiShuffleState?.lastPresentedEmoji)
        assertEquals(
            "PRO-SACRED-${updated.getValue(TransportModeOption.Pro).sampleInputId?.uppercase()}",
            updated.getValue(TransportModeOption.Pro).inputText,
        )
        assertEquals(
            "PRO-SACRED-${updated.getValue(TransportModeOption.Mini).sampleInputId?.uppercase()}",
            updated.getValue(TransportModeOption.Mini).inputText,
        )
    }

    @Test
    fun `initialize leaves the first flash sample plain when emoji decoration is disabled`() {
        val updated =
            updater.initialize(
                sessions = TransportModeOption.entries.associateWith { ModeAudioSessionState() },
                language = AppLanguageOption.English,
                flavor = SampleFlavor.SacredMachine,
                isDecorationEnabled = false,
            )

        val flashSession = updated.getValue(TransportModeOption.Flash)
        assertEquals("sacred-en-${flashSession.sampleInputId}", flashSession.inputText)
        assertNull(flashSession.sampleEmojiShuffleState)
        assertNull(flashSession.appliedSampleEmojiPrefix)
    }

    @Test
    fun `language change remaps existing sample id but keeps custom text untouched`() {
        val sessions =
            mapOf(
                TransportModeOption.Flash to
                    ModeAudioSessionState(
                        inputText = "sacred-en-b",
                        sampleInputId = "b",
                    ),
                TransportModeOption.Pro to
                    ModeAudioSessionState(
                        inputText = "CUSTOM INPUT",
                        sampleInputId = null,
                    ),
                TransportModeOption.Ultra to
                    ModeAudioSessionState(
                        inputText = "sacred-en-a",
                        sampleInputId = "a",
                    ),
            )

        val updated =
            updater.refreshForLanguageChange(
                sessions = sessions,
                newLanguage = AppLanguageOption.Japanese,
                flavor = SampleFlavor.SacredMachine,
            )

        assertEquals("sacred-ja-b", updated.getValue(TransportModeOption.Flash).inputText)
        assertEquals("b", updated.getValue(TransportModeOption.Flash).sampleInputId)
        assertEquals("CUSTOM INPUT", updated.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.getValue(TransportModeOption.Pro).sampleInputId)
        assertEquals("sacred-ja-a", updated.getValue(TransportModeOption.Ultra).inputText)
        assertEquals("a", updated.getValue(TransportModeOption.Ultra).sampleInputId)
    }

    @Test
    fun `flavor change starts a fresh shuffled round but keeps custom text untouched`() {
        val sessions =
            mapOf(
                TransportModeOption.Flash to
                    ModeAudioSessionState(
                        inputText = "sacred-en-b",
                        sampleInputId = "b",
                    ),
                TransportModeOption.Pro to
                    ModeAudioSessionState(
                        inputText = "CUSTOM INPUT",
                        sampleInputId = null,
                    ),
                TransportModeOption.Ultra to
                    ModeAudioSessionState(
                        inputText = "sacred-en-a",
                        sampleInputId = "a",
                    ),
            )

        val updated =
            updater.refreshForFlavorChange(
                sessions = sessions,
                language = AppLanguageOption.English,
                newFlavor = SampleFlavor.AncientDynasty,
            )

        assertTrue(updated.getValue(TransportModeOption.Flash).sampleInputId in setOf("a", "b"))
        assertTrue(updated.getValue(TransportModeOption.Flash).inputText.startsWith("dynasty-en-"))
        assertEquals(SampleFlavor.AncientDynasty, updated.getValue(TransportModeOption.Flash).sampleShuffleState?.flavor)
        assertEquals(1, updated.getValue(TransportModeOption.Flash).sampleShuffleState?.nextSampleIndex)
        assertEquals("CUSTOM INPUT", updated.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.getValue(TransportModeOption.Pro).sampleInputId)
        assertTrue(updated.getValue(TransportModeOption.Ultra).sampleInputId in setOf("a", "b"))
        assertTrue(updated.getValue(TransportModeOption.Ultra).inputText.startsWith("dynasty-en-"))
        assertEquals(SampleFlavor.AncientDynasty, updated.getValue(TransportModeOption.Ultra).sampleShuffleState?.flavor)
        assertEquals(1, updated.getValue(TransportModeOption.Ultra).sampleShuffleState?.nextSampleIndex)
    }

    @Test
    fun `flavor change falls back to a shuffled initial sample when old sample id is missing`() {
        val sessions =
            mapOf(
                TransportModeOption.Flash to
                    ModeAudioSessionState(
                        inputText = "old-flash-theme-text",
                        sampleInputId = "missing-flash-id",
                    ),
                TransportModeOption.Pro to
                    ModeAudioSessionState(
                        inputText = "old-pro-theme-text",
                        sampleInputId = "missing-pro-id",
                    ),
                TransportModeOption.Ultra to
                    ModeAudioSessionState(
                        inputText = "CUSTOM INPUT",
                        sampleInputId = null,
                    ),
            )

        val updated =
            updater.refreshForFlavorChange(
                sessions = sessions,
                language = AppLanguageOption.English,
                newFlavor = SampleFlavor.LabyrinthOfMutability,
            )

        assertTrue(updated.getValue(TransportModeOption.Flash).sampleInputId in setOf("a", "b"))
        assertTrue(updated.getValue(TransportModeOption.Flash).inputText.startsWith("labyrinth-en-"))
        assertEquals(SampleFlavor.LabyrinthOfMutability, updated.getValue(TransportModeOption.Flash).sampleShuffleState?.flavor)
        assertTrue(updated.getValue(TransportModeOption.Pro).sampleInputId in setOf("a", "b"))
        assertTrue(updated.getValue(TransportModeOption.Pro).inputText.startsWith("PRO-LABYRINTH-"))
        assertEquals(SampleFlavor.LabyrinthOfMutability, updated.getValue(TransportModeOption.Pro).sampleShuffleState?.flavor)
        assertEquals("CUSTOM INPUT", updated.getValue(TransportModeOption.Ultra).inputText)
        assertNull(updated.getValue(TransportModeOption.Ultra).sampleInputId)
    }
}

private class UpdaterFakeSampleInputTextProvider : SampleInputTextProvider {
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
                when (flavor) {
                    SampleFlavor.SacredMachine ->
                        listOf(
                            SampleInput("a", "PRO-SACRED-A"),
                            SampleInput("b", "PRO-SACRED-B"),
                        )
                    SampleFlavor.AncientDynasty ->
                        listOf(
                            SampleInput("a", "PRO-DYNASTY-A"),
                            SampleInput("b", "PRO-DYNASTY-B"),
                        )
                    SampleFlavor.ImmortalRot ->
                        listOf(
                            SampleInput("a", "PRO-ROT-A"),
                            SampleInput("b", "PRO-ROT-B"),
                        )
                    SampleFlavor.ScarletCarnage ->
                        listOf(
                            SampleInput("a", "PRO-SCARLET-A"),
                            SampleInput("b", "PRO-SCARLET-B"),
                        )
                    SampleFlavor.ExquisiteFall ->
                        listOf(
                            SampleInput("a", "PRO-EXQUISITE-A"),
                            SampleInput("b", "PRO-EXQUISITE-B"),
                        )
                    SampleFlavor.LabyrinthOfMutability ->
                        listOf(
                            SampleInput("a", "PRO-LABYRINTH-A"),
                            SampleInput("b", "PRO-LABYRINTH-B"),
                        )
                }

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
