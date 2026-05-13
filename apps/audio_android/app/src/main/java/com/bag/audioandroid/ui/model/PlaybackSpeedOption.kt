package com.bag.audioandroid.ui.model

import java.util.Locale

internal enum class PlaybackSpeedOption(
    val speed: Float,
) {
    Tenth(0.1f),
    Quarter(0.25f),
    Half(0.5f),
    ThreeQuarter(0.75f),
    Normal(1.0f),
    OneAndHalf(1.5f),
    Double(2.0f),
    Quadruple(4.0f),
    ;

    companion object {
        val default: PlaybackSpeedOption = Normal

        val speeds: List<Float> = entries.map { it.speed }
        val slowerSpeeds: List<PlaybackSpeedOption> = entries.filter { it.speed <= 1.0f }
        val fasterSpeeds: List<PlaybackSpeedOption> = entries.filter { it.speed >= 1.0f }

        fun fromSpeed(speed: Float): PlaybackSpeedOption = entries.minBy { option -> kotlin.math.abs(option.speed - speed) }

        fun nextSpeed(currentSpeed: Float): Float {
            val currentIndex = entries.indexOf(fromSpeed(currentSpeed))
            val nextIndex = (currentIndex + 1) % entries.size
            return entries[nextIndex].speed
        }

        fun sliderPosition(speed: Float): Float = entries.indexOf(fromSpeed(speed)).toFloat()

        fun speedAtSliderPosition(position: Float): Float {
            val index = position.toInt().coerceIn(0, entries.lastIndex)
            return entries[index].speed
        }

        fun format(speed: Float): String =
            String
                .format(Locale.US, "%.2fx", speed)
                .replace(".00x", ".0x")
                .replace(Regex("(\\.\\d)0x$"), "$1x")
    }
}
