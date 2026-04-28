package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.TransportModeOption

internal sealed interface PlaybackVisualizationRoute {
    data object PcmWaveform : PlaybackVisualizationRoute
    data class SymbolEnvelope(
        val transportMode: TransportModeOption,
    ) : PlaybackVisualizationRoute
    data class FlashSignal(
        val input: FlashSignalVisualizationInput,
    ) : PlaybackVisualizationRoute
    data object ProExplanation : PlaybackVisualizationRoute
    data object UltraStep : PlaybackVisualizationRoute
}

internal fun resolvePlaybackVisualizationRoute(
    transportMode: TransportModeOption?,
    isFlashMode: Boolean,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean,
    sampleRateHz: Int,
    visualDisplayedSamples: Int,
    displayedSamples: Int,
    followData: PayloadFollowViewData,
): PlaybackVisualizationRoute {
    val useLightweightVisualization =
        (isWaveformPreview || waveformPcm.size >= LONG_AUDIO_VISUALIZATION_SAMPLE_THRESHOLD) &&
            !followData.followAvailable

    return when (transportMode) {
        TransportModeOption.Flash ->
            if (useLightweightVisualization) {
                PlaybackVisualizationRoute.PcmWaveform
            } else {
                PlaybackVisualizationRoute.FlashSignal(
                    input =
                        flashSignalVisualizationInput(
                            pcm = waveformPcm,
                            sampleRateHz = sampleRateHz,
                            visualDisplayedSamples = visualDisplayedSamples,
                            followDisplayedSamples = displayedSamples,
                            followData = followData,
                            useFollowTimelineBuckets = isWaveformPreview,
                        ),
                )
            }

        TransportModeOption.Pro, TransportModeOption.Ultra ->
            if (useLightweightVisualization) {
                PlaybackVisualizationRoute.SymbolEnvelope(transportMode = transportMode)
            } else if (transportMode == TransportModeOption.Pro) {
                PlaybackVisualizationRoute.ProExplanation
            } else {
                PlaybackVisualizationRoute.UltraStep
            }

        null ->
            if (isFlashMode && !useLightweightVisualization) {
                PlaybackVisualizationRoute.FlashSignal(
                    input =
                        flashSignalVisualizationInput(
                            pcm = waveformPcm,
                            sampleRateHz = sampleRateHz,
                            visualDisplayedSamples = visualDisplayedSamples,
                            followDisplayedSamples = displayedSamples,
                            followData = followData,
                            useFollowTimelineBuckets = isWaveformPreview,
                        ),
                )
            } else {
                PlaybackVisualizationRoute.PcmWaveform
            }
    }
}

private const val LONG_AUDIO_VISUALIZATION_SAMPLE_THRESHOLD = 44100 * 120
