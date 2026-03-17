package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class PaletteFamily(
    @param:StringRes val titleResId: Int
) {
    RedsPinks(R.string.palette_family_reds_pinks),
    Oranges(R.string.palette_family_oranges),
    Yellows(R.string.palette_family_yellows),
    Greens(R.string.palette_family_greens),
    CyansBlues(R.string.palette_family_cyans_blues),
    PurplesMagentas(R.string.palette_family_purples_magentas),
    Neutrals(R.string.palette_family_neutrals)
}
