package com.bag.audioandroid.ui.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bag.audioandroid.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class AppLocaleResourcesTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val selfNamedLanguageLabels =
        mapOf(
            R.string.config_language_chinese to "简体中文",
            R.string.config_language_traditional_chinese to "繁體中文",
            R.string.config_language_english to "English",
            R.string.config_language_japanese to "日本語",
            R.string.config_language_german to "Deutsch",
            R.string.config_language_russian to "Русский",
            R.string.config_language_spanish to "Español",
            R.string.config_language_portuguese to "Português",
        )

    @Test
    fun `extended locales resolve their own translated resources`() {
        val cases =
            listOf(
                AppLanguageOption.German to "App-Sprache wählen",
                AppLanguageOption.Russian to "Выберите язык приложения",
                AppLanguageOption.Spanish to "Elegir idioma de la app",
                AppLanguageOption.Portuguese to "Escolha o idioma do app",
            )

        cases.forEach { (language, expectedSubtitle) ->
            assertEquals(
                expectedSubtitle,
                localizedString(language, R.string.config_language_subtitle),
            )
        }
    }

    @Test
    fun `language names stay self named across localized resource bundles`() {
        val localesToCheck =
            listOf(
                AppLanguageOption.English,
                AppLanguageOption.Chinese,
                AppLanguageOption.TraditionalChinese,
                AppLanguageOption.Japanese,
                AppLanguageOption.German,
                AppLanguageOption.Russian,
                AppLanguageOption.Spanish,
                AppLanguageOption.Portuguese,
            )

        localesToCheck.forEach { language ->
            selfNamedLanguageLabels.forEach { (resId, expectedLabel) ->
                assertEquals(expectedLabel, localizedString(language, resId))
            }
        }
    }

    private fun localizedString(
        language: AppLanguageOption,
        resId: Int,
    ): String {
        val configuration = appContext.resources.configuration
        val localizedConfiguration = android.content.res.Configuration(configuration)
        localizedConfiguration.setLocale(Locale.forLanguageTag(language.languageTag))
        return appContext.createConfigurationContext(localizedConfiguration).getString(resId)
    }
}
