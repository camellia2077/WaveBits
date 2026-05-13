package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.R

enum class FlashSignalVisualizationMode(
    val labelResId: Int,
) {
    Lanes(R.string.audio_flash_visualizer_mode_lanes),
    Pitch(R.string.audio_flash_visualizer_mode_pitch),
    Pulse(R.string.audio_flash_visualizer_mode_pulse),
}
