package com.bag.audioandroid.ui

import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.DefaultCustomBrandThemeSettings
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.theme.BrandDualToneThemes
import com.bag.audioandroid.ui.theme.DefaultBrandTheme
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.MaterialPalettes
import com.bag.audioandroid.ui.theme.customBrandTheme
import com.bag.audioandroid.ui.theme.isCustomBrandThemeOptionId
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHex
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHexOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AudioAndroidPreferencesBindings(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sampleInputSessionUpdater: SampleInputSessionUpdater,
    private val appSettingsRepository: AppSettingsRepository,
    private val scope: CoroutineScope,
) {
    fun startObserving() {
        observeSelectedPalette()
        observeSelectedThemeMode()
        observeSelectedThemeStyle()
        observeCustomBrandThemeSettings()
        observeSelectedBrandTheme()
        observeSelectedFlashVoicingStyle()
        observeFlashVoicingEnabled()
        observeSelectedPlaybackSequenceMode()
        observeConfigLanguageExpanded()
        observeConfigThemeAppearanceExpanded()
        observeDemoModeEnabled()
        observeSampleDecorationEnabled()
        observeFlashVisualPerfOverlayEnabled()
    }

    private fun observeSelectedPalette() {
        scope.launch {
            appSettingsRepository.selectedPaletteId
                .distinctUntilChanged()
                .collect { paletteId ->
                    val palette = MaterialPalettes.firstOrNull { it.id == paletteId } ?: DefaultMaterialPalette
                    uiState.update { state ->
                        if (state.selectedPalette.id == palette.id) {
                            state
                        } else {
                            state.copy(selectedPalette = palette)
                        }
                    }
                }
        }
    }

    private fun observeSelectedThemeMode() {
        scope.launch {
            appSettingsRepository.selectedThemeModeId
                .distinctUntilChanged()
                .collect { themeModeId ->
                    val themeMode = ThemeModeOption.fromId(themeModeId)
                    uiState.update { state ->
                        if (state.selectedThemeMode == themeMode) {
                            state
                        } else {
                            state.copy(selectedThemeMode = themeMode)
                        }
                    }
                }
        }
    }

    private fun observeSelectedThemeStyle() {
        scope.launch {
            appSettingsRepository.selectedThemeStyleId
                .distinctUntilChanged()
                .collect { themeStyleId ->
                    val themeStyle = ThemeStyleOption.fromId(themeStyleId)
                    uiState.update { state ->
                        state.withSelectedThemeStyle(themeStyle, sampleInputSessionUpdater)
                    }
                }
        }
    }

    private fun observeSelectedBrandTheme() {
        scope.launch {
            appSettingsRepository.selectedBrandThemeId
                .distinctUntilChanged()
                .collect { brandThemeId ->
                    uiState.update { state ->
                        val brandTheme =
                            if (brandThemeId != null && isCustomBrandThemeOptionId(brandThemeId)) {
                                state.customBrandThemes.firstOrNull { it.id == brandThemeId } ?: state.customBrandThemes.first()
                            } else {
                                BrandDualToneThemes.firstOrNull { it.id == brandThemeId } ?: DefaultBrandTheme
                            }
                        state.withSelectedBrandTheme(brandTheme, sampleInputSessionUpdater)
                    }
                }
        }
    }

    private fun observeCustomBrandThemeSettings() {
        scope.launch {
            appSettingsRepository.customBrandThemePresets
                .map { presets ->
                    val normalizedPresets =
                        presets.map { settings ->
                            CustomBrandThemeSettings(
                                presetId = settings.presetId,
                                displayName = settings.displayName.trim().ifBlank { DefaultCustomBrandThemeSettings.displayName },
                                backgroundHex =
                                    normalizeBrandThemeHex(settings.backgroundHex)
                                        ?: DefaultCustomBrandThemeSettings.backgroundHex,
                                accentHex =
                                    normalizeBrandThemeHex(settings.accentHex)
                                        ?: DefaultCustomBrandThemeSettings.accentHex,
                                outlineHexOrNull =
                                    settings.outlineHexOrNull?.let { outlineHex ->
                                        normalizeBrandThemeHexOrNull(outlineHex) ?: DefaultCustomBrandThemeSettings.outlineHexOrNull
                                    },
                            )
                        }
                    if (normalizedPresets.isEmpty()) {
                        listOf(DefaultCustomBrandThemeSettings)
                    } else {
                        normalizedPresets
                    }
                }.distinctUntilChanged()
                .collect { presets ->
                    uiState.update { state ->
                        val presetThemes = presets.map(::customBrandTheme)
                        val selectedBrandTheme =
                            if (isCustomBrandThemeOptionId(state.selectedBrandTheme.id)) {
                                presetThemes.firstOrNull { it.id == state.selectedBrandTheme.id } ?: presetThemes.first()
                            } else {
                                state.selectedBrandTheme
                            }
                        state.copy(
                            customBrandThemePresets = presets,
                            selectedBrandTheme = selectedBrandTheme,
                        )
                    }
                }
        }
    }

    private fun observeSelectedFlashVoicingStyle() {
        scope.launch {
            appSettingsRepository.selectedFlashVoicingStyleId
                .distinctUntilChanged()
                .collect { styleId ->
                    val style = FlashVoicingStyleOption.fromId(styleId)
                    uiState.update { state ->
                        if (state.selectedFlashVoicingStyle == style) {
                            state
                        } else {
                            state.copy(selectedFlashVoicingStyle = style)
                        }
                    }
                }
        }
    }

    private fun observeFlashVoicingEnabled() {
        scope.launch {
            appSettingsRepository.isFlashVoicingEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isFlashVoicingEnabled == enabled) {
                            state
                        } else {
                            state.copy(isFlashVoicingEnabled = enabled)
                        }
                    }
                }
        }
    }

    private fun observeSelectedPlaybackSequenceMode() {
        scope.launch {
            appSettingsRepository.selectedPlaybackSequenceModeId
                .distinctUntilChanged()
                .collect { modeId ->
                    val mode = PlaybackSequenceMode.fromId(modeId)
                    uiState.update { state ->
                        if (state.playbackSequenceMode == mode) {
                            state
                        } else {
                            state.copy(playbackSequenceMode = mode)
                        }
                    }
                }
        }
    }

    private fun observeConfigLanguageExpanded() {
        scope.launch {
            appSettingsRepository.isConfigLanguageExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigLanguageExpanded == expanded) state else state.copy(isConfigLanguageExpanded = expanded)
                    }
                }
        }
    }

    private fun observeConfigThemeAppearanceExpanded() {
        scope.launch {
            appSettingsRepository.isConfigThemeAppearanceExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigThemeAppearanceExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigThemeAppearanceExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeDemoModeEnabled() {
        scope.launch {
            appSettingsRepository.isDemoModeEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isDemoModeEnabled == enabled) {
                            state
                        } else {
                            state.copy(isDemoModeEnabled = enabled)
                        }
                    }
                }
        }
    }

    private fun observeSampleDecorationEnabled() {
        scope.launch {
            appSettingsRepository.isSampleDecorationEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isSampleDecorationEnabled == enabled) {
                            state
                        } else {
                            state.copy(isSampleDecorationEnabled = enabled)
                        }
                    }
                }
        }
    }

    private fun observeFlashVisualPerfOverlayEnabled() {
        scope.launch {
            appSettingsRepository.isFlashVisualPerfOverlayEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isFlashVisualPerfOverlayEnabled == enabled) {
                            state
                        } else {
                            state.copy(isFlashVisualPerfOverlayEnabled = enabled)
                        }
                    }
                }
        }
    }
}
