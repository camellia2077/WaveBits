package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class FlashVoicingStyleOption(
    val id: String,
    // A preset carries both axes: signal timing stays decoder-facing, while
    // voicing flavor is the user-facing emotion layer.
    val signalProfileValue: Int,
    val voicingFlavorValue: Int,
    @param:StringRes val labelResId: Int,
    @param:StringRes val descriptionResId: Int,
) {
    Steady(
        id = "steady",
        signalProfileValue = FlashSignalProfileWire.STEADY,
        voicingFlavorValue = FlashVoicingFlavorWire.STEADY,
        labelResId = R.string.config_flash_style_steady_label,
        descriptionResId = R.string.config_flash_style_steady_description,
    ),
    Hostile(
        id = "hostile",
        signalProfileValue = FlashSignalProfileWire.HOSTILE,
        voicingFlavorValue = FlashVoicingFlavorWire.HOSTILE,
        labelResId = R.string.config_flash_style_hostile_label,
        descriptionResId = R.string.config_flash_style_hostile_description,
    ),
    Litany(
        id = "litany",
        signalProfileValue = FlashSignalProfileWire.LITANY,
        voicingFlavorValue = FlashVoicingFlavorWire.LITANY,
        labelResId = R.string.config_flash_style_litany_label,
        descriptionResId = R.string.config_flash_style_litany_description,
    ),
    Collapse(
        id = "collapse",
        signalProfileValue = FlashSignalProfileWire.COLLAPSE,
        voicingFlavorValue = FlashVoicingFlavorWire.COLLAPSE,
        labelResId = R.string.config_flash_style_collapse_label,
        descriptionResId = R.string.config_flash_style_collapse_description,
    ),
    Zeal(
        id = "zeal",
        signalProfileValue = FlashSignalProfileWire.ZEAL,
        voicingFlavorValue = FlashVoicingFlavorWire.ZEAL,
        labelResId = R.string.config_flash_style_zeal_label,
        descriptionResId = R.string.config_flash_style_zeal_description,
    ),
    Void(
        id = "void",
        signalProfileValue = FlashSignalProfileWire.VOID,
        voicingFlavorValue = FlashVoicingFlavorWire.VOID,
        labelResId = R.string.config_flash_style_void_label,
        descriptionResId = R.string.config_flash_style_void_description,
    ),
    ;

    val usesLongCadencePayload: Boolean
        get() = this == Litany

    val flashVisualActiveWindowBucketCount: Int
        get() =
            when (this) {
                Litany -> 8
                else -> 3
            }

    companion object {
        fun fromId(id: String?): FlashVoicingStyleOption = entries.firstOrNull { it.id == id } ?: Steady
    }
}
