package com.bag.audioandroid.ui

import androidx.appcompat.app.AppCompatDelegate
import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.LibrarySelectionUiState
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.MaterialPalettes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AudioAndroidChromeActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sampleInputSessionUpdater: SampleInputSessionUpdater,
    private val appSettingsRepository: AppSettingsRepository,
    private val scope: CoroutineScope
) {
    fun onTabSelected(
        tab: AppTab,
        refreshSavedAudioItems: () -> Unit
    ) {
        if (tab == AppTab.Library) {
            refreshSavedAudioItems()
        }
        uiState.update { state ->
            state.copy(
                selectedTab = tab,
                librarySelection = if (tab == AppTab.Library) {
                    state.librarySelection
                } else {
                    LibrarySelectionUiState()
                }
            )
        }
    }

    fun onLanguageSelected(language: AppLanguageOption) {
        val previousLanguage = uiState.value.selectedLanguage
        if (previousLanguage == language) {
            return
        }
        uiState.update { state ->
            state.copy(
                selectedLanguage = language,
                sessions = sampleInputSessionUpdater.refreshForLanguageChange(
                    state.sessions,
                    previousLanguage,
                    language
                )
            )
        }
        AppCompatDelegate.setApplicationLocales(language.toLocaleList())
    }

    fun onOpenAboutPage() {
        uiState.update { it.copy(showAboutPage = true) }
    }

    fun onCloseAboutPage() {
        uiState.update { it.copy(showAboutPage = false) }
    }

    fun onOpenLicensesPage() {
        uiState.update { it.copy(showLicensesPage = true, showAboutPage = false) }
    }

    fun onCloseLicensesPage() {
        uiState.update { it.copy(showLicensesPage = false, showAboutPage = true) }
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

    fun onOpenPlayerDetailSheet() {
        uiState.update { it.copy(showPlayerDetailSheet = true, showSavedAudioSheet = false) }
    }

    fun onClosePlayerDetailSheet() {
        uiState.update { it.copy(showPlayerDetailSheet = false) }
    }

    fun onSnackbarMessageShown(messageId: Long) {
        uiState.update { state ->
            if (state.snackbarMessage?.id == messageId) {
                state.copy(snackbarMessage = null)
            } else {
                state
            }
        }
    }

    fun observeSelectedPalette() {
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

    fun observeSelectedThemeMode() {
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

    fun observeSelectedFlashVoicingStyle() {
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

    fun observeSelectedPlaybackSequenceMode() {
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
}
