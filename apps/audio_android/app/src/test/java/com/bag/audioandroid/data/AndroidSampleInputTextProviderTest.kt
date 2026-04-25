package com.bag.audioandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class AndroidSampleInputTextProviderTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `localized thematic samples follow app language while pro stays ascii for both flavors`() {
        val provider = AndroidSampleInputTextProvider(context)

        assertEquals(
            "黄铜游标受圣油，首枚齿轮方可触碰",
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.Chinese,
                SampleFlavor.SacredMachine,
            ).text,
        )
        assertEquals(
            "黃銅游標受聖油，首枚齒輪方可觸碰",
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.TraditionalChinese,
                SampleFlavor.SacredMachine,
            ).text,
        )
        assertEquals(
            "The brass calipers receive purified oil before the first gear is touched",
            provider.defaultSample(
                TransportModeOption.Ultra,
                AppLanguageOption.English,
                SampleFlavor.SacredMachine,
            ).text,
        )
        assertEquals(
            "黄銅ノギスに聖油を受け、第一歯車は初めて触れられる",
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.Japanese,
                SampleFlavor.SacredMachine,
            ).text,
        )

        AppLanguageOption.entries.forEach { language ->
            val sacredProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.SacredMachine,
                ).text
            assertEquals("APPLY SACRED OIL TO BRASS CALIPERS", sacredProText)
            assertTrue(sacredProText.all { it.code in 0..0x7F })

            val dynastyProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.AncientDynasty,
                ).text
            assertEquals("IMMORTAL ALLOY HAND CLOSES. NO WARMTH ANSWERS.", dynastyProText)
            assertTrue(dynastyProText.all { it.code in 0..0x7F })

            val rotProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.ImmortalRot,
                ).text
            assertEquals("BRIGHT MUSHROOMS RISE FROM EMPTY EYES", rotProText)
            assertTrue(rotProText.all { it.code in 0..0x7F })

            val scarletProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.ScarletCarnage,
                ).text
            assertEquals("EYES BURST RED. BLOOD BOILS. TEETH KEEP BITING.", scarletProText)
            assertTrue(scarletProText.all { it.code in 0..0x7F })

            val exquisiteProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.ExquisiteFall,
                ).text
            assertEquals("GOLD DUST FILLS THE LUNGS. HANDS STILL REACH FOR MORE.", exquisiteProText)
            assertTrue(exquisiteProText.all { it.code in 0..0x7F })

            val labyrinthProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.LabyrinthOfMutability,
                ).text
            assertEquals("HERO CUTS ONE STRING. THE NEXT NOOSE TIGHTENS.", labyrinthProText)
            assertTrue(labyrinthProText.all { it.code in 0..0x7F })
        }
    }

    @Test
    fun `default sample keeps the same semantic slot while flavor changes the text`() {
        val provider = AndroidSampleInputTextProvider(context)

        val sacred =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.SacredMachine,
            )
        val dynasty =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.AncientDynasty,
            )

        // The sample prose is creative content and changes often; lock the semantic slot id,
        // then only verify that Android resources resolve to non-empty text.
        assertEquals("caliper_oil_rite", sacred.id)
        assertTrue(sacred.text.isNotBlank())
        val rot =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.ImmortalRot,
            )

        assertEquals("alloy_hand_no_warmth", dynasty.id)
        assertTrue(dynasty.text.isNotBlank())
        val scarlet =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.ScarletCarnage,
            )

        assertEquals("mushrooms_from_empty_eyes", rot.id)
        assertTrue(rot.text.isNotBlank())
        val exquisite =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.ExquisiteFall,
            )

        assertEquals("red_mist_bites_back", scarlet.id)
        assertTrue(scarlet.text.isNotBlank())
        val labyrinth =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.LabyrinthOfMutability,
            )

        assertEquals("gold_dust_inhaled", exquisite.id)
        assertTrue(exquisite.text.isNotBlank())
        assertEquals("thread_pulls_the_hero", labyrinth.id)
        assertTrue(labyrinth.text.isNotBlank())
    }

    @Test
    fun `random sample avoids returning the excluded sample id within the active flavor`() {
        val provider =
            AndroidSampleInputTextProvider(
                appContext = context,
                random = FixedIndexRandom(0),
            )

        val sample =
            provider.randomSample(
                mode = TransportModeOption.Flash,
                language = AppLanguageOption.English,
                flavor = SampleFlavor.AncientDynasty,
                length = SampleInputLengthOption.Short,
                excludingSampleId = "alloy_hand_no_warmth",
            )

        assertEquals("aeonic_ash_stirs", sample.id)
        assertTrue(sample.text.isNotBlank())
    }

    @Test
    fun `sample ids remain stable across themed and ascii lookups for long entries`() {
        val provider = AndroidSampleInputTextProvider(context)

        val themed =
            provider.sampleById(
                mode = TransportModeOption.Flash,
                language = AppLanguageOption.English,
                flavor = SampleFlavor.SacredMachine,
                sampleId = "ancient_engine_grants_motion",
            )
        val ascii =
            provider.sampleById(
                mode = TransportModeOption.Pro,
                language = AppLanguageOption.English,
                flavor = SampleFlavor.SacredMachine,
                sampleId = "ancient_engine_grants_motion",
            )

        assertEquals("ancient_engine_grants_motion", themed?.id)
        assertTrue(themed?.text.orEmpty().startsWith("A subsonic note moved"))
        assertEquals("ancient_engine_grants_motion", ascii?.id)
        assertTrue(ascii?.text.orEmpty().startsWith("ENGINE AWAKENING."))
    }
}

private class FixedIndexRandom(
    private val fixedIndex: Int,
) : Random() {
    override fun nextBits(bitCount: Int): Int = 0

    override fun nextInt(until: Int): Int {
        if (until <= 0) {
            return 0
        }
        return fixedIndex.coerceIn(0, until - 1)
    }
}
