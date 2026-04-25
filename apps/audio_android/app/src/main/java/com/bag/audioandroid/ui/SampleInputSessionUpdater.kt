package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.ModeAudioSessionState

class SampleInputSessionUpdater(
    private val sampleInputTextProvider: SampleInputTextProvider,
) {
    fun initialize(
        sessions: Map<TransportModeOption, ModeAudioSessionState>,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): Map<TransportModeOption, ModeAudioSessionState> =
        sessions.mapValues { (mode, session) ->
            sampleInputTextProvider.defaultSample(mode, language, flavor).let { sample ->
                session.copy(
                    inputText = sample.text,
                    sampleInputId = sample.id,
                )
            }
        }

    fun refreshForLanguageChange(
        sessions: Map<TransportModeOption, ModeAudioSessionState>,
        newLanguage: AppLanguageOption,
        flavor: SampleFlavor,
    ): Map<TransportModeOption, ModeAudioSessionState> =
        sessions.mapValues { (mode, session) ->
            session.sampleInputId
                ?.let { sampleInputTextProvider.sampleById(mode, newLanguage, flavor, it) }
                ?.let { sample -> session.copy(inputText = sample.text, sampleInputId = sample.id) }
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
                // Theme lineups do not share sample ids, so fall back to the new flavor's default
                // sample instead of leaving the previous dual-tone theme text on screen.
                val sample =
                    sampleInputTextProvider.sampleById(mode, language, newFlavor, sampleId)
                        ?: sampleInputTextProvider.defaultSample(mode, language, newFlavor)
                session.copy(inputText = sample.text, sampleInputId = sample.id)
            }
        }
}
