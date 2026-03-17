package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class ThemeModeOption(
    val id: String,
    @param:StringRes val labelResId: Int
) {
    FollowSystem(
        id = "system",
        labelResId = R.string.config_theme_mode_follow_system
    ),
    Light(
        id = "light",
        labelResId = R.string.config_theme_mode_light
    ),
    Dark(
        id = "dark",
        labelResId = R.string.config_theme_mode_dark
    );

    companion object {
        fun fromId(id: String?): ThemeModeOption =
            entries.firstOrNull { it.id == id } ?: FollowSystem
    }
}
