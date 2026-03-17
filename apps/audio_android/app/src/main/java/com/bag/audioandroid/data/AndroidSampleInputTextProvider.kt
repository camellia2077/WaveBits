package com.bag.audioandroid.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.TransportModeOption
import kotlin.random.Random
import java.util.Locale

class AndroidSampleInputTextProvider(
    private val appContext: Context,
    private val random: Random = Random.Default
) : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption
    ): SampleInput = resolveSample(resourcesFor(language), sampleEntries(mode).first())

    override fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        excludingSampleId: String?
    ): SampleInput {
        val entries = sampleEntries(mode)
        val candidates = if (excludingSampleId != null && entries.size > 1) {
            entries.filterNot { it.id == excludingSampleId }
        } else {
            entries
        }
        val selectedEntry = candidates[random.nextInt(candidates.size)]
        return resolveSample(resourcesFor(language), selectedEntry)
    }

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        sampleId: String
    ): SampleInput? =
        sampleEntries(mode)
            .firstOrNull { it.id == sampleId }
            ?.let { resolveSample(resourcesFor(language), it) }

    private fun localizedContext(locale: Locale): Context {
        val configuration = Configuration(appContext.resources.configuration)
        configuration.setLocale(locale)
        return appContext.createConfigurationContext(configuration)
    }

    private fun resourcesFor(language: AppLanguageOption): Resources =
        when (language) {
            AppLanguageOption.FollowSystem -> {
                val systemLocale = Resources.getSystem().configuration.locales[0] ?: Locale.getDefault()
                localizedContext(systemLocale).resources
            }
            AppLanguageOption.Chinese -> localizedContext(Locale.SIMPLIFIED_CHINESE).resources
            AppLanguageOption.TraditionalChinese ->
                localizedContext(Locale.forLanguageTag("zh-TW")).resources
            AppLanguageOption.English -> localizedContext(Locale.ENGLISH).resources
            AppLanguageOption.Japanese -> localizedContext(Locale.JAPANESE).resources
        }

    private fun resolveSample(
        resources: Resources,
        entry: SampleEntry
    ): SampleInput = SampleInput(
        id = entry.id,
        text = resources.getString(entry.resId)
    )

    private fun sampleEntries(mode: TransportModeOption): List<SampleEntry> =
        when (mode) {
            TransportModeOption.Flash, TransportModeOption.Ultra -> thematicSamples
            TransportModeOption.Pro -> proSamples
        }

    private data class SampleEntry(
        val id: String,
        @param:StringRes val resId: Int
    )

    private companion object {
        val thematicSamples = listOf(
            SampleEntry("old_star_chart", R.string.audio_transport_flash_example),
            SampleEntry("sealed_engine", R.string.audio_sample_thematic_2),
            SampleEntry("seventh_torch", R.string.audio_sample_thematic_3),
            SampleEntry("pilgrim_ship", R.string.audio_sample_thematic_4),
            SampleEntry("iron_bells", R.string.audio_sample_thematic_5)
        )

        val proSamples = listOf(
            SampleEntry("ash_bells", R.string.audio_transport_pro_example),
            SampleEntry("outer_gate", R.string.audio_sample_pro_2),
            SampleEntry("red_keepers", R.string.audio_sample_pro_3),
            SampleEntry("seventh_torch", R.string.audio_sample_pro_4),
            SampleEntry("iron_spires", R.string.audio_sample_pro_5)
        )
    }
}
