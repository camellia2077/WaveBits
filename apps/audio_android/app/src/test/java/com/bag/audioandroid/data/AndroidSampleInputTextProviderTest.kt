package com.bag.audioandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.TransportModeOption
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidSampleInputTextProviderTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `localized thematic samples follow app language while pro stays ascii`() {
        val provider = AndroidSampleInputTextProvider(context)

        assertEquals(
            "红袍守夜人正在点亮旧星图",
            provider.defaultSample(TransportModeOption.Flash, AppLanguageOption.Chinese).text
        )
        assertEquals(
            "紅袍守夜人正在點亮舊星圖",
            provider.defaultSample(TransportModeOption.Flash, AppLanguageOption.TraditionalChinese).text
        )
        assertEquals(
            "The red-robed keepers are lighting the old star chart",
            provider.defaultSample(TransportModeOption.Ultra, AppLanguageOption.English).text
        )
        assertEquals(
            "赤衣の守人たちが古い星図に灯をともしている",
            provider.defaultSample(TransportModeOption.Flash, AppLanguageOption.Japanese).text
        )

        AppLanguageOption.entries.forEach { language ->
            val proText = provider.defaultSample(TransportModeOption.Pro, language).text
            assertEquals("ASH BELLS ANSWER AT DAWN", proText)
            assertTrue(proText.all { it.code in 0..0x7F })
        }
    }

    @Test
    fun `random sample avoids returning the excluded sample id`() {
        val provider = AndroidSampleInputTextProvider(
            appContext = context,
            random = FixedIndexRandom(0)
        )

        val sample = provider.randomSample(
            mode = TransportModeOption.Flash,
            language = AppLanguageOption.English,
            excludingSampleId = "old_star_chart"
        )

        assertEquals("sealed_engine", sample.id)
        assertEquals("In the depths of the bell tower, the sealed engine slowly awakens", sample.text)
    }
}

private class FixedIndexRandom(
    private val fixedIndex: Int
) : Random() {
    override fun nextBits(bitCount: Int): Int = 0

    override fun nextInt(until: Int): Int {
        if (until <= 0) {
            return 0
        }
        return fixedIndex.coerceIn(0, until - 1)
    }
}
