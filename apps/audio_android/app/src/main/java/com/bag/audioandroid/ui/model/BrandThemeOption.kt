package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class BrandThemeOption(
    val id: String,
    @param:StringRes val groupTitleResId: Int,
    @param:StringRes val titleResId: Int,
    @param:StringRes val descriptionResId: Int,
    @param:StringRes val accessibilityLabelResId: Int,
    val sampleFlavor: SampleFlavor,
    val isDarkTheme: Boolean,
    val backgroundColor: Color,
    val accentColor: Color,
    val outlineColor: Color,
    val colorScheme: ColorScheme,
) {
    val primaryColor: Color
        get() = backgroundColor

    val secondaryColor: Color
        get() = accentColor
}
