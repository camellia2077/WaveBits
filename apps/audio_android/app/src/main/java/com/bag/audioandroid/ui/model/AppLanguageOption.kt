package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import androidx.core.os.LocaleListCompat
import com.bag.audioandroid.R

enum class AppLanguageOption(
    val languageTag: String,
    @param:StringRes val labelResId: Int
) {
    FollowSystem(languageTag = "", labelResId = R.string.config_language_follow_system),
    Chinese(languageTag = "zh", labelResId = R.string.config_language_chinese),
    TraditionalChinese(languageTag = "zh-TW", labelResId = R.string.config_language_traditional_chinese),
    English(languageTag = "en", labelResId = R.string.config_language_english),
    Japanese(languageTag = "ja", labelResId = R.string.config_language_japanese);

    fun toLocaleList(): LocaleListCompat =
        if (languageTag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }

    companion object {
        fun fromLanguageTags(languageTags: String): AppLanguageOption {
            val firstTag = languageTags
                .split(',')
                .firstOrNull()
                ?.trim()
                ?.lowercase()
                .orEmpty()

            return when {
                firstTag.startsWith("zh-hant") ||
                    firstTag.startsWith("zh-tw") ||
                    firstTag.startsWith("zh-hk") ||
                    firstTag.startsWith("zh-mo") -> TraditionalChinese
                firstTag.startsWith("zh") -> Chinese
                firstTag.startsWith("en") -> English
                firstTag.startsWith("ja") -> Japanese
                else -> FollowSystem
            }
        }
    }
}
