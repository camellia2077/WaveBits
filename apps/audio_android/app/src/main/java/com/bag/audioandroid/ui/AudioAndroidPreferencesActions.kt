package com.bag.audioandroid.ui

import androidx.appcompat.app.AppCompatDelegate
import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    fun onFlashVoicingStyleSelected(style: FlashVoicingStyleOption) {
        uiState.update { it.copy(selectedFlashVoicingStyle = style) }
        scope.launch {
            appSettingsRepository.setSelectedFlashVoicingStyleId(style.id)
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
}
