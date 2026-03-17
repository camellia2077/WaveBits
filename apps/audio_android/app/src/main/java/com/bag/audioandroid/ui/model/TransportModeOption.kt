package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class TransportModeOption(
    val nativeValue: Int,
    val wireName: String,
    @param:StringRes val labelResId: Int,
    @param:StringRes val charsetHintResId: Int,
    @param:StringRes val exampleTextResId: Int
) {
    Flash(
        nativeValue = 0,
        wireName = "flash",
        labelResId = R.string.transport_mode_flash_label,
        charsetHintResId = R.string.audio_transport_flash_hint,
        exampleTextResId = R.string.audio_transport_flash_example
    ),
    Pro(
        nativeValue = 1,
        wireName = "pro",
        labelResId = R.string.transport_mode_pro_label,
        charsetHintResId = R.string.audio_transport_pro_hint,
        exampleTextResId = R.string.audio_transport_pro_example
    ),
    Ultra(
        nativeValue = 2,
        wireName = "ultra",
        labelResId = R.string.transport_mode_ultra_label,
        charsetHintResId = R.string.audio_transport_ultra_hint,
        exampleTextResId = R.string.audio_transport_ultra_example
    );

    companion object {
        fun fromWireName(wireName: String): TransportModeOption? =
            entries.firstOrNull { it.wireName == wireName }
    }
}
