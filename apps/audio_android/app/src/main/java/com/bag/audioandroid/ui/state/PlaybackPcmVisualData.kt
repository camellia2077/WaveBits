package com.bag.audioandroid.ui.state

data class PlaybackPcmVisualData(
    val samples: ShortArray = shortArrayOf(),
    val kind: PlaybackPcmVisualKind = PlaybackPcmVisualKind.Empty,
    val totalSamples: Int = 0,
) {
    val isPreview: Boolean
        get() = kind == PlaybackPcmVisualKind.WaveformPreview

    val hasSamples: Boolean
        get() = samples.isNotEmpty()

    fun displayedSamplesFor(realDisplayedSamples: Int): Int =
        if (isPreview && totalSamples > 0 && samples.isNotEmpty()) {
            ((realDisplayedSamples.toLong() * samples.size.toLong()) / totalSamples.toLong()).toInt()
        } else {
            realDisplayedSamples
        }
}

enum class PlaybackPcmVisualKind {
    Empty,
    FullPcm,
    WaveformPreview,
}
