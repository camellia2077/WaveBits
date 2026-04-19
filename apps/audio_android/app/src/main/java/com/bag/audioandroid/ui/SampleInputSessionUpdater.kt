package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.ModeAudioSessionState

class SampleInputSessionUpdater(
    private val sampleInputTextProvider: SampleInputTextProvider,
) {
    fun initialize(
        sessions: Map<TransportModeOption, ModeAudioSessionState>,
        language: AppLanguageOption,
    ): Map<TransportModeOption, ModeAudioSessionState> =
        sessions.mapValues { (mode, session) ->
            sampleInputTextProvider.defaultSample(mode, language).let { sample ->
                session.copy(
                    inputText = sample.text,
                    sampleInputId = sample.id,
                )
            }
        }

    fun refreshForLanguageChange(
        sessions: Map<TransportModeOption, ModeAudioSessionState>,
        newLanguage: AppLanguageOption,
    ): Map<TransportModeOption, ModeAudioSessionState> =
        sessions.mapValues { (mode, session) ->
            session.sampleInputId
                ?.let { sampleInputTextProvider.sampleById(mode, newLanguage, it) }
                ?.let { sample -> session.copy(inputText = sample.text, sampleInputId = sample.id) }
                ?: session
        }
}
