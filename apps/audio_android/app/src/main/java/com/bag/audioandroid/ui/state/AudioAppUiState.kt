package com.bag.audioandroid.ui.state

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioFolder
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.DefaultCustomBrandThemeSettings
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MiniPlayerLeadingIcon
import com.bag.audioandroid.ui.model.MiniPlayerSource
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.screen.formatDurationMillis
import com.bag.audioandroid.ui.theme.DefaultBrandTheme
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.customBrandTheme
import com.bag.audioandroid.ui.theme.isCustomBrandThemeOptionId

data class AudioAppUiState(
    val selectedTab: AppTab = AppTab.Audio,
    val selectedLanguage: AppLanguageOption = AppLanguageOption.FollowSystem,
    val showAboutPage: Boolean = false,
    val showLicensesPage: Boolean = false,
    val presentationVersion: String = "",
    val coreVersion: String = "",
    val selectedPalette: PaletteOption = DefaultMaterialPalette,
    val selectedBrandTheme: BrandThemeOption = DefaultBrandTheme,
    val customBrandThemePresets: List<CustomBrandThemeSettings> = listOf(DefaultCustomBrandThemeSettings),
    val selectedThemeStyle: ThemeStyleOption = ThemeStyleOption.BrandDualTone,
    val selectedThemeMode: ThemeModeOption = ThemeModeOption.FollowSystem,
    val isConfigLanguageExpanded: Boolean = true,
    val isConfigThemeAppearanceExpanded: Boolean = true,
    val isFlashVoicingEnabled: Boolean = true,
    val selectedFlashVoicingStyle: FlashVoicingStyleOption = FlashVoicingStyleOption.Steady,
    val selectedMorseSpeed: MorseSpeedOption = MorseSpeedOption.default,
    val transportMode: TransportModeOption = TransportModeOption.Flash,
    val sessions: Map<TransportModeOption, ModeAudioSessionState> = defaultModeSessions(),
    val currentPlaybackSource: AudioPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Flash),
    val playbackSequenceMode: PlaybackSequenceMode = PlaybackSequenceMode.Normal,
    val selectedSavedAudio: SavedAudioPlaybackSelection? = null,
    val savedAudioItems: List<SavedAudioItem> = emptyList(),
    val savedAudioFolders: List<SavedAudioFolder> = emptyList(),
    val savedAudioFolderAssignments: Map<String, String> = emptyMap(),
    val librarySelection: LibrarySelectionUiState = LibrarySelectionUiState(),
    val showSavedAudioSheet: Boolean = false,
    val showPlayerDetailSheet: Boolean = false,
    val libraryStatusText: UiText = UiText.Empty,
    val snackbarMessage: SnackbarMessage? = null,
    val pendingDocumentExportRequest: PendingAudioDocumentExportRequest? = null,
) {
    val customBrandThemes: List<BrandThemeOption>
        get() = customBrandThemePresets.map(::customBrandTheme)

    val activeBrandTheme: BrandThemeOption
        get() =
            if (isCustomBrandThemeOptionId(selectedBrandTheme.id)) {
                customBrandThemes.firstOrNull { it.id == selectedBrandTheme.id } ?: customBrandThemes.first()
            } else {
                selectedBrandTheme
            }

    val currentSession: ModeAudioSessionState
        get() = sessions.getValue(transportMode)

    val currentPlayback: PlaybackUiState
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).playback
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.playback
                        ?: PlaybackUiState()
            }

    val currentPlaybackSpeed: Float
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).playbackSpeed
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.playbackSpeed
                        ?: com.bag.audioandroid.ui.model.PlaybackSpeedOption.default.speed
            }

    val currentPlaybackSampleCount: Int
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> {
                    val session = sessions.getValue(source.mode)
                    session.generatedAudioMetadata?.pcmSampleCount?.takeIf { it > 0 }
                        ?: session.generatedPcm.size
                }
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.let { saved ->
                            saved.metadata?.pcmSampleCount?.takeIf { it > 0 } ?: saved.pcm.size
                        }
                        ?: 0
            }

    val currentPlaybackVisualData: PlaybackPcmVisualData
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> {
                    val session = sessions.getValue(source.mode)
                    val totalSamples =
                        session.generatedAudioMetadata?.pcmSampleCount?.takeIf { it > 0 }
                            ?: session.generatedPcm.size
                    if (session.generatedPcm.isNotEmpty()) {
                        PlaybackPcmVisualData(
                            samples = session.generatedPcm,
                            kind = PlaybackPcmVisualKind.FullPcm,
                            totalSamples = totalSamples,
                        )
                    } else {
                        PlaybackPcmVisualData(
                            samples = session.generatedWaveformPcm,
                            kind =
                                if (session.generatedWaveformPcm.isNotEmpty()) {
                                    PlaybackPcmVisualKind.WaveformPreview
                                } else {
                                    PlaybackPcmVisualKind.Empty
                                },
                            totalSamples = totalSamples,
                        )
                    }
                }
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.let { saved ->
                            val totalSamples = saved.metadata?.pcmSampleCount?.takeIf { it > 0 } ?: saved.pcm.size
                            if (saved.pcm.isNotEmpty()) {
                                PlaybackPcmVisualData(
                                    samples = saved.pcm,
                                    kind = PlaybackPcmVisualKind.FullPcm,
                                    totalSamples = totalSamples,
                                )
                            } else {
                                PlaybackPcmVisualData(
                                    samples = saved.waveformPcm,
                                    kind =
                                        if (saved.waveformPcm.isNotEmpty()) {
                                            PlaybackPcmVisualKind.WaveformPreview
                                        } else {
                                            PlaybackPcmVisualKind.Empty
                                        },
                                    totalSamples = totalSamples,
                                )
                            }
                        }
                        ?: PlaybackPcmVisualData()
            }

    val currentPlaybackPcm: ShortArray
        get() = currentPlaybackVisualData.samples

    val currentPlaybackTransportMode: TransportModeOption?
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> source.mode
                is AudioPlaybackSource.Saved -> currentSavedAudioItem?.modeWireName?.let(TransportModeOption::fromWireName)
            }

    val currentPlaybackFrameSamples: Int
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated ->
                    sessions.getValue(source.mode).generatedAudioMetadata?.frameSamples ?: 2205
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.metadata
                        ?.frameSamples
                        ?: 2205
            }

    val currentSavedAudioItem: SavedAudioItem?
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> null
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.item
            }

    val currentPlaybackDecodedPayload: DecodedPayloadViewData?
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).decodedPayload
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.decodedPayload
            }

    val audioTabDecodedPayload: DecodedPayloadViewData
        get() = currentPlaybackDecodedPayload ?: DecodedPayloadViewData.Empty

    val audioTabCodecBusy: Boolean
        get() =
            currentSession.isCodecBusy ||
                when (val source = currentPlaybackSource) {
                    is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).isCodecBusy
                    is AudioPlaybackSource.Saved -> currentSession.isCodecBusy
                }

    val audioTabDecodeSourceLabel: UiText
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated ->
                    UiText.Resource(
                        R.string.audio_decode_source_generated,
                        listOf(source.mode.wireName),
                    )
                is AudioPlaybackSource.Saved ->
                    UiText.Resource(
                        R.string.audio_decode_source_saved,
                        listOf(currentSavedAudioItem?.displayName.orEmpty()),
                    )
            }

    val audioTabDecodeBusyReason: UiText
        get() =
            if (!audioTabCodecBusy) {
                UiText.Empty
            } else {
                when (currentPlaybackSource) {
                    is AudioPlaybackSource.Generated ->
                        UiText.Resource(R.string.audio_decode_busy_reason_generated)
                    is AudioPlaybackSource.Saved ->
                        UiText.Resource(R.string.audio_decode_busy_reason_saved)
                }
            }

    val currentPlaybackFollowData: PayloadFollowViewData
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).followData
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.followData
                        ?: PayloadFollowViewData.Empty
            }

    val currentPlaybackFlashSignalInfo: FlashSignalInfo
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).generatedFlashSignalInfo
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.flashSignalInfo
                        ?: FlashSignalInfo.Empty
            }

    val currentPlaybackWavAudioInfo: WavAudioInfo
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).generatedWavAudioInfo
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.wavAudioInfo
                        ?: WavAudioInfo.Empty
            }

    val miniPlayerModel: MiniPlayerUiModel?
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> {
                    val session = sessions.getValue(source.mode)
                    val pcmSampleCount =
                        session.generatedAudioMetadata?.pcmSampleCount?.takeIf { it > 0 }
                            ?: session.generatedPcm.size
                    if (pcmSampleCount <= 0 || (session.generatedPcm.isEmpty() && session.generatedPcmFilePath == null)) {
                        null
                    } else {
                        val durationMs =
                            samplesToDurationMillis(
                                sampleCount = currentPlayback.totalSamples.takeIf { it > 0 } ?: pcmSampleCount,
                                sampleRateHz = currentPlayback.sampleRateHz,
                            )
                        MiniPlayerUiModel(
                            title =
                                UiText.Resource(
                                    R.string.audio_mini_player_generated_title,
                                    listOf(UiText.Resource(source.mode.labelResId)),
                                ),
                            subtitle = generatedMiniPlayerSubtitle(source.mode, session.generatedFlashVoicingStyle, durationMs),
                            leadingIcon = MiniPlayerLeadingIcon.Generated,
                            durationMs = durationMs,
                            transportMode = source.mode,
                            isFlashMode = source.mode == TransportModeOption.Flash,
                            flashVoicingStyle = session.generatedFlashVoicingStyle,
                            source = MiniPlayerSource.Generated,
                        )
                    }
                }

                is AudioPlaybackSource.Saved ->
                    currentSavedAudioItem?.let { item ->
                        MiniPlayerUiModel(
                            title = UiText.Plain(item.displayName),
                            subtitle =
                                savedMiniPlayerSubtitle(
                                    modeWireName = item.modeWireName,
                                    flashVoicingStyle = item.flashVoicingStyle,
                                    durationMs = item.durationMs,
                                ),
                            leadingIcon = MiniPlayerLeadingIcon.Saved,
                            durationMs = item.durationMs,
                            transportMode =
                                TransportModeOption.fromWireName(item.modeWireName) ?: TransportModeOption.Flash,
                            isFlashMode = item.modeWireName == TransportModeOption.Flash.wireName,
                            flashVoicingStyle = item.flashVoicingStyle,
                            source = MiniPlayerSource.Saved,
                        )
                    }
            }

    val canSkipPrevious: Boolean
        get() {
            val currentItemId = currentSavedAudioItem?.itemId ?: return false
            val currentIndex = savedAudioItems.indexOfFirst { it.itemId == currentItemId }
            return currentIndex > 0
        }

    val canSkipNext: Boolean
        get() {
            val currentItemId = currentSavedAudioItem?.itemId ?: return false
            val currentIndex = savedAudioItems.indexOfFirst { it.itemId == currentItemId }
            return currentIndex >= 0 && currentIndex < savedAudioItems.lastIndex
        }
}

private fun defaultModeSessions(): Map<TransportModeOption, ModeAudioSessionState> =
    TransportModeOption.entries.associateWith { ModeAudioSessionState() }

private fun samplesToDurationMillis(
    sampleCount: Int,
    sampleRateHz: Int,
): Long {
    if (sampleCount <= 0 || sampleRateHz <= 0) {
        return 0L
    }
    return (sampleCount.toLong() * 1000L) / sampleRateHz.toLong()
}

private fun generatedMiniPlayerSubtitle(
    mode: TransportModeOption,
    flashVoicingStyle: FlashVoicingStyleOption?,
    durationMs: Long,
): UiText {
    val durationText = formatDurationMillis(durationMs)
    return if (mode == TransportModeOption.Flash && flashVoicingStyle != null) {
        UiText.Resource(
            R.string.audio_mini_player_generated_flash_subtitle,
            listOf(UiText.Resource(flashVoicingStyle.labelResId), durationText),
        )
    } else {
        UiText.Resource(
            R.string.audio_mini_player_duration_only,
            listOf(durationText),
        )
    }
}

private fun savedMiniPlayerSubtitle(
    modeWireName: String,
    flashVoicingStyle: FlashVoicingStyleOption?,
    durationMs: Long,
): UiText {
    val isFlashMode = modeWireName == TransportModeOption.Flash.wireName
    val modeLabel =
        if (isFlashMode && flashVoicingStyle != null) {
            UiText.Resource(flashVoicingStyle.labelResId)
        } else {
            UiText.Resource(SavedAudioModeFilter.labelResIdForModeWireName(modeWireName))
        }
    return UiText.Resource(
        if (isFlashMode && flashVoicingStyle != null) {
            R.string.audio_mini_player_generated_flash_subtitle
        } else {
            R.string.audio_mini_player_saved_subtitle
        },
        listOf(modeLabel, formatDurationMillis(durationMs)),
    )
}
