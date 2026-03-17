package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageOptionTest {
    @Test
    fun `fromLanguageTags maps traditional chinese tags separately`() {
        assertEquals(
            AppLanguageOption.TraditionalChinese,
            AppLanguageOption.fromLanguageTags("zh-TW")
        )
        assertEquals(
            AppLanguageOption.TraditionalChinese,
            AppLanguageOption.fromLanguageTags("zh-Hant-HK")
        )
        assertEquals(
            AppLanguageOption.Chinese,
            AppLanguageOption.fromLanguageTags("zh-CN")
        )
        assertEquals(
            AppLanguageOption.Chinese,
            AppLanguageOption.fromLanguageTags("zh-Hans")
        )
    }
}
