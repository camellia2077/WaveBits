package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.state.SampleInputShuffleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

internal class AudioSessionEditingActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val sampleInputTextProvider: SampleInputTextProvider,
    private val stopPlayback: () -> Unit,
    private val refreshSavedAudioItems: () -> Unit,
    private val random: Random = Random.Default,
) {
    fun onInputTextChange(value: String) {
        sessionStateStore.updateCurrentSession {
            it.copy(
                inputText = value,
                sampleInputId = null,
                sampleShuffleState = null,
                appliedSampleEmojiPrefix = null,
            )
        }
    }

    fun onRandomizeSampleInput(length: SampleInputLengthOption) {
        val currentState = uiState.value
        if (currentState.currentSession.isCodecBusy) {
            return
        }
        val sampleIds =
            sampleInputTextProvider.sampleIds(
                mode = currentState.transportMode,
                flavor = currentState.currentSampleFlavor,
                length = length,
            )
        if (sampleIds.isEmpty()) {
            return
        }
        val nextSampleSelection =
            nextSampleSelection(
                session = currentState.currentSession,
                flavor = currentState.currentSampleFlavor,
                length = length,
                sampleIds = sampleIds,
            )
        val sample =
            sampleInputTextProvider.sampleById(
                mode = currentState.transportMode,
                language = currentState.selectedLanguage,
                flavor = currentState.currentSampleFlavor,
                sampleId = nextSampleSelection.sampleId,
            ) ?: return
        sessionStateStore.updateCurrentSession {
            val emojiPrefix =
                nextSampleEmojiPrefix(
                    mode = currentState.transportMode,
                    flavor = currentState.currentSampleFlavor,
                    isDecorationEnabled = currentState.isSampleDecorationEnabled,
                    currentState = it.sampleEmojiShuffleState,
                    random = random,
                )
            it.copy(
                inputText = withSampleEmojiPrefix(sample.text, emojiPrefix?.emoji),
                sampleInputId = sample.id,
                sampleShuffleState = nextSampleSelection.shuffleState,
                sampleEmojiShuffleState = emojiPrefix?.state ?: it.sampleEmojiShuffleState,
                appliedSampleEmojiPrefix = emojiPrefix?.emoji,
            )
        }
    }

    fun onTransportModeSelected(mode: TransportModeOption) {
        val currentState = uiState.value
        if (currentState.transportMode == mode &&
            currentState.currentPlaybackSource == AudioPlaybackSource.Generated(mode)
        ) {
            return
        }
        stopPlayback()
        uiState.update {
            it.copy(
                transportMode = mode,
                currentPlaybackSource = AudioPlaybackSource.Generated(mode),
                showSavedAudioSheet = false,
                showPlayerDetailSheet = false,
            )
        }
    }

    fun onOpenSavedAudioSheet() {
        refreshSavedAudioItems()
        uiState.update { it.copy(showSavedAudioSheet = true, showPlayerDetailSheet = false) }
    }

    fun onCloseSavedAudioSheet() {
        uiState.update { it.copy(showSavedAudioSheet = false) }
    }

    private fun nextSampleSelection(
        session: ModeAudioSessionState,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        sampleIds: List<String>,
    ): NextSampleSelection {
        // Sample rotation is meant to feel fresh without starving parts of the catalog.
        // We therefore keep a shuffled round in session state and advance through it
        // one item at a time, rather than doing a brand new random pick on every tap.
        val activeShuffleState =
            session.sampleShuffleState
                ?.takeIf { it.matches(flavor, length) && it.shuffledSampleIds.toSet() == sampleIds.toSet() }
        val currentShuffleState =
            activeShuffleState ?: initialShuffleState(session, flavor, length, sampleIds)
        val reshuffledWhenConsumed =
            if (currentShuffleState.nextSampleIndex >= currentShuffleState.shuffledSampleIds.size) {
                reshuffledState(flavor, length, sampleIds, currentShuffleState.lastPresentedSampleId)
            } else {
                currentShuffleState
            }
        val selectedId = reshuffledWhenConsumed.shuffledSampleIds[reshuffledWhenConsumed.nextSampleIndex]
        return NextSampleSelection(
            sampleId = selectedId,
            shuffleState =
                reshuffledWhenConsumed.copy(
                    nextSampleIndex = reshuffledWhenConsumed.nextSampleIndex + 1,
                    lastPresentedSampleId = selectedId,
                ),
        )
    }

    private fun reshuffledState(
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        sampleIds: List<String>,
        lastPresentedSampleId: String?,
    ): SampleInputShuffleState =
        SampleInputShuffleState(
            flavor = flavor,
            length = length,
            shuffledSampleIds = shuffledSampleIds(sampleIds, lastPresentedSampleId),
            nextSampleIndex = 0,
            lastPresentedSampleId = lastPresentedSampleId,
        )

    private fun initialShuffleState(
        session: ModeAudioSessionState,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        sampleIds: List<String>,
    ): SampleInputShuffleState {
        val currentSampleId = session.sampleInputId
        val remainingSampleIds =
            currentSampleId
                ?.takeIf(sampleIds::contains)
                ?.let { sampleIds.filterNot { id -> id == currentSampleId } }
                .orEmpty()
                .ifEmpty { sampleIds }
        return SampleInputShuffleState(
            flavor = flavor,
            length = length,
            shuffledSampleIds = shuffledSampleIds(remainingSampleIds, null),
            nextSampleIndex = 0,
            lastPresentedSampleId = currentSampleId,
        )
    }

    // Once a round has been exhausted, every sample goes back into the pool.
    // We only avoid a hard handoff where the last card of the previous round is
    // immediately repeated as the first card of the next round.
    private fun shuffledSampleIds(
        sampleIds: List<String>,
        leadingAvoidId: String?,
    ): List<String> {
        if (sampleIds.size <= 1) {
            return sampleIds
        }
        val shuffled = sampleIds.shuffled(random).toMutableList()
        if (leadingAvoidId == null || shuffled.first() != leadingAvoidId) {
            return shuffled
        }
        val swapIndex = shuffled.indexOfFirst { it != leadingAvoidId }
        if (swapIndex > 0) {
            val first = shuffled.first()
            shuffled[0] = shuffled[swapIndex]
            shuffled[swapIndex] = first
        }
        return shuffled
    }

    private data class NextSampleSelection(
        val sampleId: String,
        val shuffleState: SampleInputShuffleState,
    )
}
