package com.bag.audioandroid.ui

import android.content.Intent
import android.util.Log
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.screen.FlashSignalVisualizationMode
import com.bag.audioandroid.ui.screen.PlaybackDisplayMode
import com.bag.audioandroid.ui.screen.toPlaybackDisplayMode

data class FlashDebugScenario(
    val text: String = DefaultText,
    val hasTextOverride: Boolean = false,
    val sampleLength: SampleInputLengthOption? = null,
    val sampleId: String? = null,
    val languageOverride: AppLanguageOption? = null,
    val scenario: FlashDebugScenarioKind = FlashDebugScenarioKind.Ui,
    val style: FlashVoicingStyleOption = FlashVoicingStyleOption.Standard,
    val displayMode: PlaybackDisplayMode = PlaybackDisplayMode.Lyrics,
    val visualMode: FlashSignalVisualizationMode = FlashSignalVisualizationMode.Lanes,
    val visualPerfOverlayEnabled: Boolean? = null,
    val playbackSpeed: Float = PlaybackSpeedOption.default.speed,
    val encode: Boolean = true,
    val play: Boolean = true,
    val playDurationMs: Long = DefaultPlayDurationMs,
    val requestId: Long = System.nanoTime(),
) {
    companion object {
        const val Action = "com.bag.audioandroid.DEBUG_FLASH_SCENARIO"
        const val ExtraText = "wb.input"
        const val ExtraSampleLength = "wb.sample.length"
        const val ExtraSampleId = "wb.sample.id"
        const val ExtraLanguage = "wb.lang"
        const val ExtraScenario = "wb.scenario"
        const val ExtraStyle = "wb.flash.style"
        const val ExtraDisplay = "wb.display"
        const val ExtraVisual = "wb.visual"
        const val ExtraVisualPerfOverlay = "wb.visual.perf_overlay"
        const val ExtraPlaybackSpeed = "wb.playback.speed"
        const val ExtraEncode = "wb.encode"
        const val ExtraPlay = "wb.play"
        const val ExtraPlayDurationMs = "wb.play.ms"
        const val DefaultText = "flash sync test"
        const val DefaultPlayDurationMs = 6_000L
        private const val Tag = "FlashAutomation"

        fun fromIntent(intent: Intent?): FlashDebugScenario? {
            if (!BuildConfig.DEBUG || intent?.action != Action) {
                return null
            }
            val textOverride = intent.getStringExtra(ExtraText)?.takeIf { it.isNotBlank() }
            val scenario =
                FlashDebugScenario(
                    text = textOverride ?: DefaultText,
                    hasTextOverride = textOverride != null,
                    sampleLength = SampleInputLengthOption.fromId(intent.getStringExtra(ExtraSampleLength)),
                    sampleId = intent.getStringExtra(ExtraSampleId)?.takeIf { it.isNotBlank() },
                    languageOverride = intent.getStringExtra(ExtraLanguage).toDebugAppLanguageOption(),
                    scenario = FlashDebugScenarioKind.fromId(intent.getStringExtra(ExtraScenario)),
                    style = FlashVoicingStyleOption.fromId(intent.getStringExtra(ExtraStyle)),
                    displayMode = intent.getStringExtra(ExtraDisplay).toPlaybackDisplayMode(),
                    visualMode = intent.getStringExtra(ExtraVisual).toFlashVisualizationMode(),
                    visualPerfOverlayEnabled =
                        intent
                            .takeIf { it.hasExtra(ExtraVisualPerfOverlay) }
                            ?.getBooleanExtra(ExtraVisualPerfOverlay, false),
                    playbackSpeed =
                        intent
                            .getFloatExtra(ExtraPlaybackSpeed, PlaybackSpeedOption.default.speed)
                            .coerceIn(PlaybackSpeedOption.speeds.first(), PlaybackSpeedOption.speeds.last()),
                    encode = intent.getBooleanExtra(ExtraEncode, true),
                    play = intent.getBooleanExtra(ExtraPlay, true),
                    playDurationMs = intent.getLongExtra(ExtraPlayDurationMs, DefaultPlayDurationMs).coerceAtLeast(0L),
                )
            Log.d(
                Tag,
                "received scenario=${scenario.scenario.id} style=${scenario.style.id} display=${scenario.displayMode.name.lowercase()} " +
                    "visual=${scenario.visualMode.name} " +
                    "visualPerfOverlay=${scenario.visualPerfOverlayEnabled?.toString() ?: "default"} " +
                    "playbackSpeed=${scenario.playbackSpeed} " +
                    "lang=${scenario.languageOverride?.languageTag ?: "current"} " +
                    "encode=${scenario.encode} play=${scenario.play} playMs=${scenario.playDurationMs} " +
                    "requestId=${scenario.requestId} input=${scenario.inputDebugSummary()}",
            )
            return scenario
        }
    }
}

private fun FlashDebugScenario.inputDebugSummary(): String {
    if (hasTextOverride) {
        return "text chars=${text.length}"
    }
    val sampleIdPart = sampleId?.let { " sampleId=$it" }.orEmpty()
    val sampleLengthPart = sampleLength?.let { " sampleLength=${it.id}" }.orEmpty()
    return if (sampleId != null || sampleLength != null) {
        "sample$sampleIdPart$sampleLengthPart fallbackChars=${text.length}"
    } else {
        "text chars=${text.length}"
    }
}

data class MiniDebugScenario(
    val text: String = DefaultText,
    val hasTextOverride: Boolean = false,
    val sampleLength: SampleInputLengthOption? = null,
    val sampleId: String? = null,
    val languageOverride: AppLanguageOption? = null,
    val scenario: FlashDebugScenarioKind = FlashDebugScenarioKind.Ui,
    val speed: MorseSpeedOption = MorseSpeedOption.default,
    val expandLyrics: Boolean = false,
    val displayMode: PlaybackDisplayMode = PlaybackDisplayMode.Lyrics,
    val encode: Boolean = true,
    val play: Boolean = true,
    val playDurationMs: Long = DefaultPlayDurationMs,
    val requestId: Long = System.nanoTime(),
) {
    companion object {
        const val Action = "com.bag.audioandroid.DEBUG_MINI_SCENARIO"
        const val ExtraText = FlashDebugScenario.ExtraText
        const val ExtraSampleLength = FlashDebugScenario.ExtraSampleLength
        const val ExtraSampleId = FlashDebugScenario.ExtraSampleId
        const val ExtraScenario = FlashDebugScenario.ExtraScenario
        const val ExtraSpeed = "wb.mini.speed"
        const val ExtraExpandLyrics = "wb.lyrics.expand"
        const val ExtraDisplay = "wb.display"
        const val ExtraEncode = FlashDebugScenario.ExtraEncode
        const val ExtraPlay = FlashDebugScenario.ExtraPlay
        const val ExtraPlayDurationMs = FlashDebugScenario.ExtraPlayDurationMs
        const val DefaultText = "mini sync test"
        const val DefaultPlayDurationMs = 6_000L
        private const val Tag = "MiniAutomation"

        fun fromIntent(intent: Intent?): MiniDebugScenario? {
            if (!BuildConfig.DEBUG || intent?.action != Action) {
                return null
            }
            val textOverride = intent.getStringExtra(ExtraText)?.takeIf { it.isNotBlank() }
            val scenario =
                MiniDebugScenario(
                    text = textOverride ?: DefaultText,
                    hasTextOverride = textOverride != null,
                    sampleLength = SampleInputLengthOption.fromId(intent.getStringExtra(ExtraSampleLength)),
                    sampleId = intent.getStringExtra(ExtraSampleId)?.takeIf { it.isNotBlank() },
                    languageOverride = intent.getStringExtra(FlashDebugScenario.ExtraLanguage).toDebugAppLanguageOption(),
                    scenario = FlashDebugScenarioKind.fromId(intent.getStringExtra(ExtraScenario)),
                    speed = intent.getStringExtra(ExtraSpeed).toMorseSpeedOption(),
                    expandLyrics = intent.getBooleanExtra(ExtraExpandLyrics, false),
                    displayMode = intent.getStringExtra(ExtraDisplay).toPlaybackDisplayMode(),
                    encode = intent.getBooleanExtra(ExtraEncode, true),
                    play = intent.getBooleanExtra(ExtraPlay, true),
                    playDurationMs = intent.getLongExtra(ExtraPlayDurationMs, DefaultPlayDurationMs).coerceAtLeast(0L),
                )
            Log.d(
                Tag,
                "received scenario=${scenario.scenario.id} speed=${scenario.speed.id} expandLyrics=${scenario.expandLyrics} " +
                    "display=${scenario.displayMode.name.lowercase()} " +
                    "lang=${scenario.languageOverride?.languageTag ?: "current"} " +
                    "encode=${scenario.encode} play=${scenario.play} playMs=${scenario.playDurationMs} " +
                    "requestId=${scenario.requestId} input=${scenario.inputDebugSummary()}",
            )
            return scenario
        }
    }
}

private fun MiniDebugScenario.inputDebugSummary(): String {
    if (hasTextOverride) {
        return "text chars=${text.length}"
    }
    val sampleIdPart = sampleId?.let { " sampleId=$it" }.orEmpty()
    val sampleLengthPart = sampleLength?.let { " sampleLength=${it.id}" }.orEmpty()
    return if (sampleId != null || sampleLength != null) {
        "sample$sampleIdPart$sampleLengthPart fallbackChars=${text.length}"
    } else {
        "text chars=${text.length}"
    }
}

data class EncodeProgressDebugScenario(
    val mode: TransportModeOption = TransportModeOption.Mini,
    val text: String = DefaultText,
    val hasTextOverride: Boolean = false,
    val sampleLength: SampleInputLengthOption? = null,
    val sampleId: String? = null,
    val languageOverride: AppLanguageOption? = null,
    val repeatCount: Int = 1,
    val speed: MorseSpeedOption = MorseSpeedOption.default,
    val encode: Boolean = true,
    val captureDurationMs: Long = DefaultCaptureDurationMs,
    val pollIntervalMs: Long = DefaultPollIntervalMs,
    val requestId: Long = System.nanoTime(),
) {
    companion object {
        const val Action = "com.bag.audioandroid.DEBUG_ENCODE_PROGRESS_SCENARIO"
        const val ExtraMode = "wb.mode"
        const val ExtraText = FlashDebugScenario.ExtraText
        const val ExtraSampleLength = FlashDebugScenario.ExtraSampleLength
        const val ExtraSampleId = FlashDebugScenario.ExtraSampleId
        const val ExtraRepeat = "wb.repeat"
        const val ExtraSpeed = MiniDebugScenario.ExtraSpeed
        const val ExtraEncode = FlashDebugScenario.ExtraEncode
        const val DefaultText = "encode progress test"
        const val ExtraCaptureDurationMs = "wb.capture.ms"
        const val ExtraPollIntervalMs = "wb.poll.ms"
        const val DefaultCaptureDurationMs = 120_000L
        const val DefaultPollIntervalMs = 33L
        private const val Tag = "EncodeProgressAutomation"

        fun fromIntent(intent: Intent?): EncodeProgressDebugScenario? {
            if (!BuildConfig.DEBUG || intent?.action != Action) {
                return null
            }
            val textOverride = intent.getStringExtra(ExtraText)?.takeIf { it.isNotBlank() }
            val scenario =
                EncodeProgressDebugScenario(
                    mode = intent.getStringExtra(ExtraMode).toProgressScenarioMode(),
                    text = textOverride ?: DefaultText,
                    hasTextOverride = textOverride != null,
                    sampleLength = SampleInputLengthOption.fromId(intent.getStringExtra(ExtraSampleLength)),
                    sampleId = intent.getStringExtra(ExtraSampleId)?.takeIf { it.isNotBlank() },
                    languageOverride = intent.getStringExtra(FlashDebugScenario.ExtraLanguage).toDebugAppLanguageOption(),
                    repeatCount = intent.getIntExtra(ExtraRepeat, 1).coerceIn(1, 50),
                    speed = intent.getStringExtra(ExtraSpeed).toMorseSpeedOption(),
                    encode = intent.getBooleanExtra(ExtraEncode, true),
                    captureDurationMs =
                        intent
                            .getLongExtra(ExtraCaptureDurationMs, DefaultCaptureDurationMs)
                            .coerceIn(1_000L, 600_000L),
                    pollIntervalMs =
                        intent
                            .getLongExtra(ExtraPollIntervalMs, DefaultPollIntervalMs)
                            .coerceIn(16L, 1_000L),
                )
            Log.d(
                Tag,
                "received mode=${scenario.mode.wireName} speed=${scenario.speed.id} " +
                    "lang=${scenario.languageOverride?.languageTag ?: "current"} " +
                    "encode=${scenario.encode} repeat=${scenario.repeatCount} " +
                    "captureMs=${scenario.captureDurationMs} pollMs=${scenario.pollIntervalMs} " +
                    "requestId=${scenario.requestId} input=${scenario.inputDebugSummary()}",
            )
            return scenario
        }
    }
}

private fun EncodeProgressDebugScenario.inputDebugSummary(): String {
    if (hasTextOverride) {
        return "text chars=${text.length}"
    }
    val sampleIdPart = sampleId?.let { " sampleId=$it" }.orEmpty()
    val sampleLengthPart = sampleLength?.let { " sampleLength=${it.id}" }.orEmpty()
    return if (sampleId != null || sampleLength != null) {
        "sample$sampleIdPart$sampleLengthPart fallbackChars=${text.length}"
    } else {
        "text chars=${text.length}"
    }
}

data class SavedAudioDebugScenario(
    val itemId: String? = null,
    val displayName: String? = null,
    val seedDurationMs: Long = 0L,
    val seedMode: TransportModeOption = TransportModeOption.Pro,
    val requestId: Long = System.nanoTime(),
) {
    companion object {
        const val Action = "com.bag.audioandroid.DEBUG_SAVED_AUDIO_SCENARIO"
        const val ExtraItemId = "wb.saved.item_id"
        const val ExtraDisplayName = "wb.saved.display_name"
        const val ExtraSeedDurationMs = "wb.saved.seed_duration_ms"
        const val ExtraSeedMode = "wb.saved.seed_mode"
        private const val Tag = "SavedAudioAutomation"

        fun fromIntent(intent: Intent?): SavedAudioDebugScenario? {
            if (!BuildConfig.DEBUG || intent?.action != Action) {
                return null
            }
            val scenario =
                SavedAudioDebugScenario(
                    itemId = intent.getStringExtra(ExtraItemId)?.takeIf { it.isNotBlank() },
                    displayName = intent.getStringExtra(ExtraDisplayName)?.takeIf { it.isNotBlank() },
                    seedDurationMs = intent.getLongExtra(ExtraSeedDurationMs, 0L).coerceAtLeast(0L),
                    seedMode = intent.getStringExtra(ExtraSeedMode).toProgressScenarioMode(),
                )
            Log.d(
                Tag,
                "received requestId=${scenario.requestId} itemId=${scenario.itemId.orEmpty()} " +
                    "displayName=${scenario.displayName.orEmpty()} seedDurationMs=${scenario.seedDurationMs} " +
                    "seedMode=${scenario.seedMode.wireName}",
            )
            return scenario
        }
    }
}

enum class FlashDebugScenarioKind(
    val id: String,
) {
    Ui("ui"),
    Headless("headless"),
    ;

    companion object {
        fun fromId(id: String?): FlashDebugScenarioKind = entries.firstOrNull { it.id == id?.lowercase() } ?: Ui
    }
}

private fun String?.toFlashVisualizationMode(): FlashSignalVisualizationMode =
    when (this?.lowercase()) {
        "lanes" -> FlashSignalVisualizationMode.Lanes
        "pitch" -> FlashSignalVisualizationMode.Pitch
        "pulse" -> FlashSignalVisualizationMode.Pulse
        else -> FlashSignalVisualizationMode.Lanes
    }

private fun String?.toDebugAppLanguageOption(): AppLanguageOption? {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) {
        return null
    }
    return AppLanguageOption
        .fromLanguageTags(raw)
        .takeUnless { it == AppLanguageOption.FollowSystem }
}

internal val MorseSpeedOption.id: String
    get() =
        when (this) {
            MorseSpeedOption.Slow -> "slow"
            MorseSpeedOption.Standard -> "standard"
            MorseSpeedOption.Fast -> "fast"
        }

private fun String?.toMorseSpeedOption(): MorseSpeedOption =
    when (this?.lowercase()) {
        "slow" -> MorseSpeedOption.Slow
        "fast" -> MorseSpeedOption.Fast
        else -> MorseSpeedOption.Standard
    }

private fun String?.toProgressScenarioMode(): TransportModeOption =
    when (this?.lowercase()) {
        "pro" -> TransportModeOption.Pro
        "ultra" -> TransportModeOption.Ultra
        "mini" -> TransportModeOption.Mini
        else -> TransportModeOption.Mini
    }
