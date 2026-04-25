package com.bag.audioandroid.ui

import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.theme.BrandDualToneThemes
import com.bag.audioandroid.ui.theme.DefaultBrandTheme
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.MaterialPalettes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
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
        observeSelectedBrandTheme()
        observeSelectedFlashVoicingStyle()
        observeSelectedPlaybackSequenceMode()
        observeConfigLanguageExpanded()
        observeConfigThemeAppearanceExpanded()
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
                    val brandTheme = BrandDualToneThemes.firstOrNull { it.id == brandThemeId } ?: DefaultBrandTheme
                    uiState.update { state ->
                        state.withSelectedBrandTheme(brandTheme, sampleInputSessionUpdater)
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
}
