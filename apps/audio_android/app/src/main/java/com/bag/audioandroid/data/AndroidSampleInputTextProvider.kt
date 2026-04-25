package com.bag.audioandroid.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import java.util.Locale
import kotlin.random.Random

class AndroidSampleInputTextProvider(
    private val appContext: Context,
    private val random: Random = Random.Default,
) : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput =
        resolveSample(
            resourcesFor(language),
            sampleEntries(flavor, mode, SampleInputLengthOption.Short).first(),
        )

    override fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        excludingSampleId: String?,
    ): SampleInput {
        val entries = sampleEntries(flavor, mode, length)
        val candidates =
            if (excludingSampleId != null && entries.size > 1) {
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
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? =
        allSampleEntries(flavor, mode)
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
            AppLanguageOption.TraditionalChinese -> localizedContext(Locale.forLanguageTag("zh-TW")).resources
            AppLanguageOption.English -> localizedContext(Locale.ENGLISH).resources
            AppLanguageOption.Japanese -> localizedContext(Locale.JAPANESE).resources
            AppLanguageOption.German -> localizedContext(Locale.GERMAN).resources
            AppLanguageOption.Russian -> localizedContext(Locale.forLanguageTag("ru")).resources
            AppLanguageOption.Spanish -> localizedContext(Locale.forLanguageTag("es")).resources
            AppLanguageOption.Portuguese -> localizedContext(Locale.forLanguageTag("pt-BR")).resources
            AppLanguageOption.Ukrainian -> localizedContext(Locale.forLanguageTag("uk")).resources
            AppLanguageOption.Korean -> localizedContext(Locale.KOREAN).resources
            AppLanguageOption.French -> localizedContext(Locale.FRENCH).resources
        }

    private fun resolveSample(
        resources: Resources,
        entry: SampleEntry,
    ): SampleInput =
        SampleInput(
            id = entry.id,
            text = resources.getString(entry.resId),
        )

    private fun sampleEntries(
        flavor: SampleFlavor,
        mode: TransportModeOption,
        length: SampleInputLengthOption,
    ): List<SampleEntry> =
        when (flavor) {
            SampleFlavor.SacredMachine -> sacredMachineSampleEntries(mode, length)
            SampleFlavor.AncientDynasty -> ancientDynastySampleEntries(mode, length)
            SampleFlavor.ImmortalRot -> immortalRotSampleEntries(mode, length)
            SampleFlavor.ScarletCarnage -> scarletCarnageSampleEntries(mode, length)
            SampleFlavor.ExquisiteFall -> exquisiteFallSampleEntries(mode, length)
            SampleFlavor.LabyrinthOfMutability -> labyrinthOfMutabilitySampleEntries(mode, length)
        }

    private fun allSampleEntries(
        flavor: SampleFlavor,
        mode: TransportModeOption,
    ): List<SampleEntry> =
        when (flavor) {
            SampleFlavor.SacredMachine -> sacredMachineSampleEntries(mode)
            SampleFlavor.AncientDynasty -> ancientDynastySampleEntries(mode)
            SampleFlavor.ImmortalRot -> immortalRotSampleEntries(mode)
            SampleFlavor.ScarletCarnage -> scarletCarnageSampleEntries(mode)
            SampleFlavor.ExquisiteFall -> exquisiteFallSampleEntries(mode)
            SampleFlavor.LabyrinthOfMutability -> labyrinthOfMutabilitySampleEntries(mode)
        }

    private fun ancientDynastySampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<SampleEntry> =
        ancientDynastySampleCatalog
            .asSequence()
            .filter { definition -> length == null || length in definition.allowedLengths }
            .map { definition ->
                SampleEntry(
                    id = definition.id,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun sacredMachineSampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<SampleEntry> =
        sacredMachineSampleCatalog
            .asSequence()
            .filter { definition -> length == null || length in definition.allowedLengths }
            .map { definition ->
                SampleEntry(
                    id = definition.id,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun immortalRotSampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<SampleEntry> =
        immortalRotSampleCatalog
            .asSequence()
            .filter { definition -> length == null || length in definition.allowedLengths }
            .map { definition ->
                SampleEntry(
                    id = definition.id,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun scarletCarnageSampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<SampleEntry> =
        scarletCarnageSampleCatalog
            .asSequence()
            .filter { definition -> length == null || length in definition.allowedLengths }
            .map { definition ->
                SampleEntry(
                    id = definition.id,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun exquisiteFallSampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<SampleEntry> =
        exquisiteFallSampleCatalog
            .asSequence()
            .filter { definition -> length == null || length in definition.allowedLengths }
            .map { definition ->
                SampleEntry(
                    id = definition.id,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun labyrinthOfMutabilitySampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<SampleEntry> =
        labyrinthOfMutabilitySampleCatalog
            .asSequence()
            .filter { definition -> length == null || length in definition.allowedLengths }
            .map { definition ->
                SampleEntry(
                    id = definition.id,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private data class SampleEntry(
        val id: String,
        @param:StringRes val resId: Int,
    )

    private enum class AncientDynastyThemeCategory {
        SomaticStripping,
        AeonicSleepAwakening,
        AbsoluteMaterialism,
        MindDecayAristocracy,
        CurseExtremeAlienation,
    }

    private enum class SacredMachineThemeCategory {
        RiteOfMaintenance,
        EngineAwakening,
        SignalLitany,
        ForgeChronicle,
        FleshTranscendence,
        OriginPilgrimage,
        AbyssalQuarantine,
        PurgeCalculus,
    }

    private enum class ImmortalRotThemeCategory {
        FesteringBloom,
        BenevolentContagion,
        LethargicEmbrace,
        EntropicChime,
    }

    private enum class ScarletCarnageThemeCategory {
        CrimsonFrenzy,
        OssuaryTribute,
        IronCredo,
        BrassInferno,
    }

    private enum class ExquisiteFallThemeCategory {
        TrapOfAccumulation,
        VoidOfConsumption,
        DissolutionOfEgo,
        SolipsisticApex,
        TyrannyOfPerfection,
        SeductionOfStasis,
    }

    private enum class LabyrinthOfMutabilityThemeCategory {
        FractalConspiracy,
        ParadoxArcanum,
        KaleidoscopeFlesh,
        AbyssalArchives,
    }

    private data class SacredMachineSampleDefinition(
        val id: String,
        val themeCategory: SacredMachineThemeCategory,
        val allowedLengths: Set<SampleInputLengthOption>,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro -> asciiResId
            }
    }

    private data class AncientDynastySampleDefinition(
        val id: String,
        val themeCategory: AncientDynastyThemeCategory,
        val allowedLengths: Set<SampleInputLengthOption>,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro -> asciiResId
            }
    }

    private data class ImmortalRotSampleDefinition(
        val id: String,
        val themeCategory: ImmortalRotThemeCategory,
        val allowedLengths: Set<SampleInputLengthOption>,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro -> asciiResId
            }
    }

    private data class ScarletCarnageSampleDefinition(
        val id: String,
        val themeCategory: ScarletCarnageThemeCategory,
        val allowedLengths: Set<SampleInputLengthOption>,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro -> asciiResId
            }
    }

    private data class ExquisiteFallSampleDefinition(
        val id: String,
        val themeCategory: ExquisiteFallThemeCategory,
        val allowedLengths: Set<SampleInputLengthOption>,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro -> asciiResId
            }
    }

    private data class LabyrinthOfMutabilitySampleDefinition(
        val id: String,
        val themeCategory: LabyrinthOfMutabilityThemeCategory,
        val allowedLengths: Set<SampleInputLengthOption>,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro -> asciiResId
            }
    }

    private companion object {
        val sacredMachineSampleCatalog =
            listOf(
                SacredMachineSampleDefinition(
                    id = "caliper_oil_rite",
                    themeCategory = SacredMachineThemeCategory.RiteOfMaintenance,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_sacred_machine_themed_caliper_oil_rite,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_caliper_oil_rite,
                ),
                SacredMachineSampleDefinition(
                    id = "bolt_sequence_litany",
                    themeCategory = SacredMachineThemeCategory.RiteOfMaintenance,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_sacred_machine_themed_bolt_sequence_litany,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_bolt_sequence_litany,
                ),
                SacredMachineSampleDefinition(
                    id = "plasma_matrix_wakes",
                    themeCategory = SacredMachineThemeCategory.EngineAwakening,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_sacred_machine_themed_plasma_matrix_wakes,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_plasma_matrix_wakes,
                ),
                SacredMachineSampleDefinition(
                    id = "ancient_engine_grants_motion",
                    themeCategory = SacredMachineThemeCategory.EngineAwakening,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_sacred_machine_themed_ancient_engine_grants_motion,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_ancient_engine_grants_motion,
                ),
                SacredMachineSampleDefinition(
                    id = "ping_canticle_confirmed",
                    themeCategory = SacredMachineThemeCategory.SignalLitany,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_sacred_machine_themed_ping_canticle_confirmed,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_ping_canticle_confirmed,
                ),
                SacredMachineSampleDefinition(
                    id = "deep_array_handshake",
                    themeCategory = SacredMachineThemeCategory.SignalLitany,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_sacred_machine_themed_deep_array_handshake,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_deep_array_handshake,
                ),
                SacredMachineSampleDefinition(
                    id = "serials_from_red_steel",
                    themeCategory = SacredMachineThemeCategory.ForgeChronicle,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_sacred_machine_themed_serials_from_red_steel,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_serials_from_red_steel,
                ),
                SacredMachineSampleDefinition(
                    id = "endless_quota_chronicle",
                    themeCategory = SacredMachineThemeCategory.ForgeChronicle,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_sacred_machine_themed_endless_quota_chronicle,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_endless_quota_chronicle,
                ),
                SacredMachineSampleDefinition(
                    id = "nerve_cut_ascension",
                    themeCategory = SacredMachineThemeCategory.FleshTranscendence,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_sacred_machine_themed_nerve_cut_ascension,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_nerve_cut_ascension,
                ),
                SacredMachineSampleDefinition(
                    id = "chromium_spine_rite",
                    themeCategory = SacredMachineThemeCategory.FleshTranscendence,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_sacred_machine_themed_chromium_spine_rite,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_chromium_spine_rite,
                ),
                SacredMachineSampleDefinition(
                    id = "source_matrix_recovered",
                    themeCategory = SacredMachineThemeCategory.OriginPilgrimage,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_sacred_machine_themed_source_matrix_recovered,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_source_matrix_recovered,
                ),
                SacredMachineSampleDefinition(
                    id = "ancient_database_pilgrimage",
                    themeCategory = SacredMachineThemeCategory.OriginPilgrimage,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_sacred_machine_themed_ancient_database_pilgrimage,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_ancient_database_pilgrimage,
                ),
                SacredMachineSampleDefinition(
                    id = "unpowered_terminal_thinks",
                    themeCategory = SacredMachineThemeCategory.AbyssalQuarantine,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_sacred_machine_themed_unpowered_terminal_thinks,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_unpowered_terminal_thinks,
                ),
                SacredMachineSampleDefinition(
                    id = "paradox_code_quarantine",
                    themeCategory = SacredMachineThemeCategory.AbyssalQuarantine,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_sacred_machine_themed_paradox_code_quarantine,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_paradox_code_quarantine,
                ),
                SacredMachineSampleDefinition(
                    id = "kill_confirmed_equation",
                    themeCategory = SacredMachineThemeCategory.PurgeCalculus,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_sacred_machine_themed_kill_confirmed_equation,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_kill_confirmed_equation,
                ),
                SacredMachineSampleDefinition(
                    id = "ballistic_debug_grid",
                    themeCategory = SacredMachineThemeCategory.PurgeCalculus,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_sacred_machine_themed_ballistic_debug_grid,
                    asciiResId = R.string.audio_sample_sacred_machine_ascii_ballistic_debug_grid,
                ),
            )

        val ancientDynastySampleCatalog =
            listOf(
                AncientDynastySampleDefinition(
                    id = "alloy_hand_no_warmth",
                    themeCategory = AncientDynastyThemeCategory.SomaticStripping,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_alloy_hand_no_warmth,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_alloy_hand_no_warmth,
                ),
                AncientDynastySampleDefinition(
                    id = "soul_lost_in_emerald_light",
                    themeCategory = AncientDynastyThemeCategory.SomaticStripping,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_soul_lost_in_emerald_light,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_soul_lost_in_emerald_light,
                ),
                AncientDynastySampleDefinition(
                    id = "aeonic_ash_stirs",
                    themeCategory = AncientDynastyThemeCategory.AeonicSleepAwakening,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_aeonic_ash_stirs,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_aeonic_ash_stirs,
                ),
                AncientDynastySampleDefinition(
                    id = "obsidian_obelisks_rise",
                    themeCategory = AncientDynastyThemeCategory.AeonicSleepAwakening,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_obsidian_obelisks_rise,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_obsidian_obelisks_rise,
                ),
                AncientDynastySampleDefinition(
                    id = "molecular_law_unthreads_flesh",
                    themeCategory = AncientDynastyThemeCategory.AbsoluteMaterialism,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_molecular_law_unthreads_flesh,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_molecular_law_unthreads_flesh,
                ),
                AncientDynastySampleDefinition(
                    id = "false_gods_reduced_to_batteries",
                    themeCategory = AncientDynastyThemeCategory.AbsoluteMaterialism,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_false_gods_reduced_to_batteries,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_false_gods_reduced_to_batteries,
                ),
                AncientDynastySampleDefinition(
                    id = "rusted_throne_bad_memory",
                    themeCategory = AncientDynastyThemeCategory.MindDecayAristocracy,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_rusted_throne_bad_memory,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_rusted_throne_bad_memory,
                ),
                AncientDynastySampleDefinition(
                    id = "court_debates_dead_realm",
                    themeCategory = AncientDynastyThemeCategory.MindDecayAristocracy,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_court_debates_dead_realm,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_court_debates_dead_realm,
                ),
                AncientDynastySampleDefinition(
                    id = "red_eyes_wear_skin",
                    themeCategory = AncientDynastyThemeCategory.CurseExtremeAlienation,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_red_eyes_wear_skin,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_red_eyes_wear_skin,
                ),
                AncientDynastySampleDefinition(
                    id = "extinction_logic_harvests_life",
                    themeCategory = AncientDynastyThemeCategory.CurseExtremeAlienation,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_extinction_logic_harvests_life,
                    asciiResId = R.string.audio_sample_ancient_dynasty_ascii_extinction_logic_harvests_life,
                ),
            )


        val immortalRotSampleCatalog =
            listOf(
                ImmortalRotSampleDefinition(
                    id = "mushrooms_from_empty_eyes",
                    themeCategory = ImmortalRotThemeCategory.FesteringBloom,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_immortal_rot_themed_mushrooms_from_empty_eyes,
                    asciiResId = R.string.audio_sample_immortal_rot_ascii_mushrooms_from_empty_eyes,
                ),
                ImmortalRotSampleDefinition(
                    id = "harvest_beneath_the_flowers",
                    themeCategory = ImmortalRotThemeCategory.FesteringBloom,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_immortal_rot_themed_harvest_beneath_the_flowers,
                    asciiResId = R.string.audio_sample_immortal_rot_ascii_harvest_beneath_the_flowers,
                ),
                ImmortalRotSampleDefinition(
                    id = "fever_shared_as_bread",
                    themeCategory = ImmortalRotThemeCategory.BenevolentContagion,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_immortal_rot_themed_fever_shared_as_bread,
                    asciiResId = R.string.audio_sample_immortal_rot_ascii_fever_shared_as_bread,
                ),
                ImmortalRotSampleDefinition(
                    id = "kind_contagion_gathers_family",
                    themeCategory = ImmortalRotThemeCategory.BenevolentContagion,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_immortal_rot_themed_kind_contagion_gathers_family,
                    asciiResId = R.string.audio_sample_immortal_rot_ascii_kind_contagion_gathers_family,
                ),
                ImmortalRotSampleDefinition(
                    id = "warm_mire_stillness",
                    themeCategory = ImmortalRotThemeCategory.LethargicEmbrace,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_immortal_rot_themed_warm_mire_stillness,
                    asciiResId = R.string.audio_sample_immortal_rot_ascii_warm_mire_stillness,
                ),
                ImmortalRotSampleDefinition(
                    id = "shield_sinks_from_hand",
                    themeCategory = ImmortalRotThemeCategory.LethargicEmbrace,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_immortal_rot_themed_shield_sinks_from_hand,
                    asciiResId = R.string.audio_sample_immortal_rot_ascii_shield_sinks_from_hand,
                ),
                ImmortalRotSampleDefinition(
                    id = "rust_bell_in_fog",
                    themeCategory = ImmortalRotThemeCategory.EntropicChime,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_immortal_rot_themed_rust_bell_in_fog,
                    asciiResId = R.string.audio_sample_immortal_rot_ascii_rust_bell_in_fog,
                ),
                ImmortalRotSampleDefinition(
                    id = "patient_rain_takes_the_wall",
                    themeCategory = ImmortalRotThemeCategory.EntropicChime,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_immortal_rot_themed_patient_rain_takes_the_wall,
                    asciiResId = R.string.audio_sample_immortal_rot_ascii_patient_rain_takes_the_wall,
                ),
            )

        val scarletCarnageSampleCatalog =
            listOf(
                ScarletCarnageSampleDefinition(
                    id = "red_mist_bites_back",
                    themeCategory = ScarletCarnageThemeCategory.CrimsonFrenzy,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_red_mist_bites_back,
                    asciiResId = R.string.audio_sample_scarlet_carnage_ascii_red_mist_bites_back,
                ),
                ScarletCarnageSampleDefinition(
                    id = "reason_breaks_blood_runs",
                    themeCategory = ScarletCarnageThemeCategory.CrimsonFrenzy,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_reason_breaks_blood_runs,
                    asciiResId = R.string.audio_sample_scarlet_carnage_ascii_reason_breaks_blood_runs,
                ),
                ScarletCarnageSampleDefinition(
                    id = "skull_step_receives",
                    themeCategory = ScarletCarnageThemeCategory.OssuaryTribute,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_skull_step_receives,
                    asciiResId = R.string.audio_sample_scarlet_carnage_ascii_skull_step_receives,
                ),
                ScarletCarnageSampleDefinition(
                    id = "red_river_pays_the_stair",
                    themeCategory = ScarletCarnageThemeCategory.OssuaryTribute,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_red_river_pays_the_stair,
                    asciiResId = R.string.audio_sample_scarlet_carnage_ascii_red_river_pays_the_stair,
                ),
                ScarletCarnageSampleDefinition(
                    id = "black_iron_answers_magic",
                    themeCategory = ScarletCarnageThemeCategory.IronCredo,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_black_iron_answers_magic,
                    asciiResId = R.string.audio_sample_scarlet_carnage_ascii_black_iron_answers_magic,
                ),
                ScarletCarnageSampleDefinition(
                    id = "face_to_face_truth",
                    themeCategory = ScarletCarnageThemeCategory.IronCredo,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_face_to_face_truth,
                    asciiResId = R.string.audio_sample_scarlet_carnage_ascii_face_to_face_truth,
                ),
                ScarletCarnageSampleDefinition(
                    id = "brass_gears_drink_blood",
                    themeCategory = ScarletCarnageThemeCategory.BrassInferno,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_brass_gears_drink_blood,
                    asciiResId = R.string.audio_sample_scarlet_carnage_ascii_brass_gears_drink_blood,
                ),
                ScarletCarnageSampleDefinition(
                    id = "war_engine_eats_the_sky",
                    themeCategory = ScarletCarnageThemeCategory.BrassInferno,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_war_engine_eats_the_sky,
                    asciiResId = R.string.audio_sample_scarlet_carnage_ascii_war_engine_eats_the_sky,
                ),
            )


        val exquisiteFallSampleCatalog =
            listOf(
                ExquisiteFallSampleDefinition(
                    id = "gold_dust_inhaled",
                    themeCategory = ExquisiteFallThemeCategory.TrapOfAccumulation,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_gold_dust_inhaled,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_gold_dust_inhaled,
                ),
                ExquisiteFallSampleDefinition(
                    id = "vault_without_enough",
                    themeCategory = ExquisiteFallThemeCategory.TrapOfAccumulation,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_vault_without_enough,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_vault_without_enough,
                ),
                ExquisiteFallSampleDefinition(
                    id = "sweet_meat_never_ends",
                    themeCategory = ExquisiteFallThemeCategory.VoidOfConsumption,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_sweet_meat_never_ends,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_sweet_meat_never_ends,
                ),
                ExquisiteFallSampleDefinition(
                    id = "banquet_eats_the_tongue",
                    themeCategory = ExquisiteFallThemeCategory.VoidOfConsumption,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_banquet_eats_the_tongue,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_banquet_eats_the_tongue,
                ),
                ExquisiteFallSampleDefinition(
                    id = "velvet_barbs_kiss",
                    themeCategory = ExquisiteFallThemeCategory.DissolutionOfEgo,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_velvet_barbs_kiss,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_velvet_barbs_kiss,
                ),
                ExquisiteFallSampleDefinition(
                    id = "self_dissolves_in_touch",
                    themeCategory = ExquisiteFallThemeCategory.DissolutionOfEgo,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_self_dissolves_in_touch,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_self_dissolves_in_touch,
                ),
                ExquisiteFallSampleDefinition(
                    id = "crown_too_heavy",
                    themeCategory = ExquisiteFallThemeCategory.SolipsisticApex,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_crown_too_heavy,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_crown_too_heavy,
                ),
                ExquisiteFallSampleDefinition(
                    id = "dream_throne_commands_all",
                    themeCategory = ExquisiteFallThemeCategory.SolipsisticApex,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_dream_throne_commands_all,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_dream_throne_commands_all,
                ),
                ExquisiteFallSampleDefinition(
                    id = "mirror_keeps_perfection",
                    themeCategory = ExquisiteFallThemeCategory.TyrannyOfPerfection,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_mirror_keeps_perfection,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_mirror_keeps_perfection,
                ),
                ExquisiteFallSampleDefinition(
                    id = "statue_of_self_praise",
                    themeCategory = ExquisiteFallThemeCategory.TyrannyOfPerfection,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_statue_of_self_praise,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_statue_of_self_praise,
                ),
                ExquisiteFallSampleDefinition(
                    id = "soft_moss_says_enough",
                    themeCategory = ExquisiteFallThemeCategory.SeductionOfStasis,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_soft_moss_says_enough,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_soft_moss_says_enough,
                ),
                ExquisiteFallSampleDefinition(
                    id = "lullaby_closes_the_eyes",
                    themeCategory = ExquisiteFallThemeCategory.SeductionOfStasis,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_exquisite_fall_themed_lullaby_closes_the_eyes,
                    asciiResId = R.string.audio_sample_exquisite_fall_ascii_lullaby_closes_the_eyes,
                ),
            )


        val labyrinthOfMutabilitySampleCatalog =
            listOf(
                LabyrinthOfMutabilitySampleDefinition(
                    id = "thread_pulls_the_hero",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.FractalConspiracy,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_thread_pulls_the_hero,
                    asciiResId = R.string.audio_sample_labyrinth_of_mutability_ascii_thread_pulls_the_hero,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "final_trap_needs_rebellion",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.FractalConspiracy,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_final_trap_needs_rebellion,
                    asciiResId = R.string.audio_sample_labyrinth_of_mutability_ascii_final_trap_needs_rebellion,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "cold_fire_folds_space",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.ParadoxArcanum,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_cold_fire_folds_space,
                    asciiResId = R.string.audio_sample_labyrinth_of_mutability_ascii_cold_fire_folds_space,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "laws_unwrite_themselves",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.ParadoxArcanum,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_laws_unwrite_themselves,
                    asciiResId = R.string.audio_sample_labyrinth_of_mutability_ascii_laws_unwrite_themselves,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "new_eye_blesses_spine",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.KaleidoscopeFlesh,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_new_eye_blesses_spine,
                    asciiResId = R.string.audio_sample_labyrinth_of_mutability_ascii_new_eye_blesses_spine,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "flesh_refuses_one_shape",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.KaleidoscopeFlesh,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_flesh_refuses_one_shape,
                    asciiResId = R.string.audio_sample_labyrinth_of_mutability_ascii_flesh_refuses_one_shape,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "library_bleeds_prophecy",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.AbyssalArchives,
                    allowedLengths = setOf(SampleInputLengthOption.Short),
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_library_bleeds_prophecy,
                    asciiResId = R.string.audio_sample_labyrinth_of_mutability_ascii_library_bleeds_prophecy,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "truth_crushes_the_seer",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.AbyssalArchives,
                    allowedLengths = setOf(SampleInputLengthOption.Long),
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_truth_crushes_the_seer,
                    asciiResId = R.string.audio_sample_labyrinth_of_mutability_ascii_truth_crushes_the_seer,
                ),
            )


    }
}
