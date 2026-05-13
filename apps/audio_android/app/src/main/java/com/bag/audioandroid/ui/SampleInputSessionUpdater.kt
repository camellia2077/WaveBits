package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.state.SampleInputShuffleState
import kotlin.random.Random

class SampleInputSessionUpdater(
    private val sampleInputTextProvider: SampleInputTextProvider,
    private val random: Random = Random.Default,
) {
    fun initialize(
        sessions: Map<TransportModeOption, ModeAudioSessionState>,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        isDecorationEnabled: Boolean = true,
    ): Map<TransportModeOption, ModeAudioSessionState> =
        sessions.mapValues { (mode, session) ->
            randomizedInitialSession(
                session = session,
                mode = mode,
                language = language,
                flavor = flavor,
                isDecorationEnabled = isDecorationEnabled,
            )
        }

    fun refreshForLanguageChange(
        sessions: Map<TransportModeOption, ModeAudioSessionState>,
        newLanguage: AppLanguageOption,
        flavor: SampleFlavor,
    ): Map<TransportModeOption, ModeAudioSessionState> =
        sessions.mapValues { (mode, session) ->
            session.sampleInputId
                ?.let { sampleInputTextProvider.sampleById(mode, newLanguage, flavor, it) }
                ?.let { sample ->
                    session.copy(
                        inputText = sample.text,
                        sampleInputId = sample.id,
                    )
                }
                ?: session
        }

    fun refreshForFlavorChange(
        sessions: Map<TransportModeOption, ModeAudioSessionState>,
        language: AppLanguageOption,
        newFlavor: SampleFlavor,
    ): Map<TransportModeOption, ModeAudioSessionState> =
        sessions.mapValues { (mode, session) ->
            val sampleId = session.sampleInputId
            if (sampleId == null) {
                session
            } else {
                // Flavor changes switch the sample catalog itself. We intentionally
                // start a fresh shuffled round for the new catalog instead of trying
                // to preserve a fixed first sample or carry over the old deck.
                randomizedInitialSession(
                    session = session,
                    mode = mode,
                    language = language,
                    flavor = newFlavor,
                    isDecorationEnabled = false,
                )
            }
        }

    private fun randomizedInitialSession(
        session: ModeAudioSessionState,
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        isDecorationEnabled: Boolean,
    ): ModeAudioSessionState {
        val length = SampleInputLengthOption.Short
        // The first sample does not need to be fixed. We can shuffle first, show
        // the first card immediately, and persist the same deck state so the
        // randomize action simply continues dealing the next sample.
        val shuffledSampleIds = sampleInputTextProvider.sampleIds(mode, flavor, length).shuffled(random)
        val firstSampleId = shuffledSampleIds.firstOrNull()
        val sample =
            firstSampleId
                ?.let { sampleInputTextProvider.sampleById(mode, language, flavor, it) }
                ?: sampleInputTextProvider.defaultSample(mode, language, flavor)
        val shuffleState =
            if (shuffledSampleIds.isEmpty()) {
                null
            } else {
                SampleInputShuffleState(
                    flavor = flavor,
                    length = length,
                    shuffledSampleIds = shuffledSampleIds,
                    nextSampleIndex = 1.coerceAtMost(shuffledSampleIds.size),
                    lastPresentedSampleId = sample.id,
                )
            }
        val emojiPrefix =
            nextSampleEmojiPrefix(
                mode = mode,
                flavor = flavor,
                isDecorationEnabled = isDecorationEnabled,
                currentState = session.sampleEmojiShuffleState,
                random = random,
            )
        return session.copy(
            inputText = withSampleEmojiPrefix(sample.text, emojiPrefix?.emoji),
            sampleInputId = sample.id,
            sampleShuffleState = shuffleState,
            sampleEmojiShuffleState = emojiPrefix?.state ?: session.sampleEmojiShuffleState,
            appliedSampleEmojiPrefix = emojiPrefix?.emoji,
        )
    }
}
