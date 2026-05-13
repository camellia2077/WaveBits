package com.bag.audioandroid.ui

import androidx.appcompat.app.AppCompatDelegate
import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.hasSameConfigAs
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.theme.customBrandTheme
import com.bag.audioandroid.ui.theme.customBrandThemeOptionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

internal class AudioAndroidPreferencesActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sampleInputSessionUpdater: SampleInputSessionUpdater,
    private val appSettingsRepository: AppSettingsRepository,
    private val scope: CoroutineScope,
) {
    fun onLanguageSelected(language: AppLanguageOption) {
        val previousLanguage = uiState.value.selectedLanguage
        if (previousLanguage == language) {
            return
        }
        uiState.update { state ->
            state.copy(
                selectedLanguage = language,
                sessions =
                    sampleInputSessionUpdater.refreshForLanguageChange(
                        state.sessions,
                        language,
                        state.currentSampleFlavor,
                    ),
            )
        }
        AppCompatDelegate.setApplicationLocales(language.toLocaleList())
    }

    fun onPaletteSelected(palette: PaletteOption) {
        uiState.update { it.copy(selectedPalette = palette) }
        scope.launch {
            appSettingsRepository.setSelectedPaletteId(palette.id)
        }
    }

    fun onThemeModeSelected(themeMode: ThemeModeOption) {
        uiState.update { it.copy(selectedThemeMode = themeMode) }
        scope.launch {
            appSettingsRepository.setSelectedThemeModeId(themeMode.id)
        }
    }

    fun onThemeStyleSelected(themeStyle: ThemeStyleOption) {
        uiState.update { state -> state.withSelectedThemeStyle(themeStyle, sampleInputSessionUpdater) }
        scope.launch {
            appSettingsRepository.setSelectedThemeStyleId(themeStyle.id)
        }
    }

    fun onBrandThemeSelected(brandTheme: BrandThemeOption) {
        uiState.update { state -> state.withSelectedBrandTheme(brandTheme, sampleInputSessionUpdater) }
        scope.launch {
            appSettingsRepository.setSelectedBrandThemeId(brandTheme.id)
        }
    }

    fun onCustomBrandThemeSaved(
        settings: CustomBrandThemeSettings,
        replacePresetId: String?,
    ) {
        uiState.update { state ->
            val normalizedSettings =
                settings.copy(
                    presetId = replacePresetId ?: UUID.randomUUID().toString(),
                    displayName = settings.displayName.trim(),
                )
            val updatedPresets =
                if (replacePresetId == null) {
                    state.customBrandThemePresets + normalizedSettings
                } else {
                    state.customBrandThemePresets.map { preset ->
                        if (preset.presetId == replacePresetId) {
                            normalizedSettings
                        } else {
                            preset
                        }
                    }
                }
            val updatedState = state.copy(customBrandThemePresets = updatedPresets)
            updatedState.withSelectedBrandTheme(customBrandTheme(normalizedSettings), sampleInputSessionUpdater)
        }
        scope.launch {
            appSettingsRepository.setCustomBrandThemePresets(uiState.value.customBrandThemePresets)
            appSettingsRepository.setSelectedBrandThemeId(uiState.value.activeBrandTheme.id)
        }
    }

    fun onCustomBrandThemeDeleted(presetId: String) {
        uiState.update { state ->
            val remainingPresets = state.customBrandThemePresets.filterNot { it.presetId == presetId }
            if (remainingPresets.isEmpty()) {
                return@update state
            }

            val updatedState = state.copy(customBrandThemePresets = remainingPresets)
            val deletedActiveCustomTheme = state.selectedBrandTheme.id == customBrandThemeOptionId(presetId)

            if (deletedActiveCustomTheme) {
                updatedState.withSelectedBrandTheme(customBrandTheme(remainingPresets.first()), sampleInputSessionUpdater)
            } else {
                updatedState
            }
        }
        scope.launch {
            appSettingsRepository.setCustomBrandThemePresets(uiState.value.customBrandThemePresets)
            appSettingsRepository.setSelectedBrandThemeId(uiState.value.activeBrandTheme.id)
        }
    }

    fun onCustomBrandThemesImported(settings: List<CustomBrandThemeSettings>) {
        if (settings.isEmpty()) {
            return
        }
        uiState.update { state ->
            val imported =
                settings
                    .map { setting ->
                        setting.copy(
                            presetId = UUID.randomUUID().toString(),
                            displayName = setting.displayName.trim(),
                        )
                    }.filterNot { candidate ->
                        state.customBrandThemePresets.any { preset -> preset.hasSameConfigAs(candidate) }
                    }
            if (imported.isEmpty()) {
                state
            } else {
                state.copy(customBrandThemePresets = state.customBrandThemePresets + imported)
            }
        }
        scope.launch {
            appSettingsRepository.setCustomBrandThemePresets(uiState.value.customBrandThemePresets)
        }
    }

    fun onFlashVoicingStyleSelected(style: FlashVoicingStyleOption) {
        val isEnhancedStyle = style != FlashVoicingStyleOption.Standard
        uiState.update {
            it.copy(
                selectedFlashVoicingStyle = style,
                isFlashVoicingEnabled = isEnhancedStyle,
            )
        }
        scope.launch {
            appSettingsRepository.setSelectedFlashVoicingStyleId(style.id)
            appSettingsRepository.setFlashVoicingEnabled(isEnhancedStyle)
        }
    }

    fun onPlaybackSequenceModeSelected(mode: PlaybackSequenceMode) {
        uiState.update { it.copy(playbackSequenceMode = mode) }
        scope.launch {
            appSettingsRepository.setSelectedPlaybackSequenceModeId(mode.id)
        }
    }

    fun onConfigLanguageExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigLanguageExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigLanguageExpanded(expanded)
        }
    }

    fun onConfigThemeAppearanceExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigThemeAppearanceExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigThemeAppearanceExpanded(expanded)
        }
    }

    fun onDemoModeEnabledChanged(enabled: Boolean) {
        uiState.update { it.copy(isDemoModeEnabled = enabled) }
        scope.launch {
            appSettingsRepository.setDemoModeEnabled(enabled)
        }
    }

    fun onSampleDecorationEnabledChanged(enabled: Boolean) {
        uiState.update { state ->
            state.withSampleDecoration(isDecorationEnabled = enabled)
        }
        scope.launch {
            appSettingsRepository.setSampleDecorationEnabled(enabled)
        }
    }

    fun onFlashVisualPerfOverlayEnabledChanged(enabled: Boolean) {
        uiState.update { it.copy(isFlashVisualPerfOverlayEnabled = enabled) }
        scope.launch {
            appSettingsRepository.setFlashVisualPerfOverlayEnabled(enabled)
        }
    }
}

internal fun AudioAppUiState.withSampleDecoration(isDecorationEnabled: Boolean): AudioAppUiState {
    val updatedSession =
        applySampleEmojiDecoration(
            session = currentSession,
            mode = transportMode,
            flavor = currentSampleFlavor,
            isDecorationEnabled = isDecorationEnabled,
        )
    return copy(
        isSampleDecorationEnabled = isDecorationEnabled,
        sessions = sessions + (transportMode to updatedSession),
    )
}
