package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class PaletteOption(
    val id: String,
    val family: PaletteFamily,
    @param:StringRes val titleResId: Int,
    val previewColor: Color,
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme
)
