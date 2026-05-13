package com.bag.audioandroid.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.screen.AboutScreen
import com.bag.audioandroid.ui.screen.DebugPlaybackDisplayModeRequest
import com.bag.audioandroid.ui.screen.OpenSourceLicensesScreen
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.theme.BrandDualToneThemes
import com.bag.audioandroid.ui.theme.LocalAppThemeAccentTokens
import com.bag.audioandroid.ui.theme.LocalAppThemeVisualTokens
import com.bag.audioandroid.ui.theme.LocalAudioEncodeGlyphColors
import com.bag.audioandroid.ui.theme.MaterialPalettes
import com.bag.audioandroid.ui.theme.audioEncodeGlyphColorsForBrandTheme
import com.bag.audioandroid.ui.theme.brandAccentTokens
import com.bag.audioandroid.ui.theme.brandThemeVisualTokens
import com.bag.audioandroid.ui.theme.defaultAudioEncodeGlyphColors
import com.bag.audioandroid.ui.theme.materialAccentTokens
import com.bag.audioandroid.ui.theme.materialThemeVisualTokens

@Composable
internal fun AudioAndroidAppShell(
    uiState: AudioAppUiState,
    savedAudioFilter: SavedAudioModeFilter,
    onSavedAudioFilterChange: (SavedAudioModeFilter) -> Unit,
    debugScenario: FlashDebugScenario? = null,
    debugExpandLyricsRequestId: Long? = null,
    onDebugExpandLyricsHandled: (Long) -> Unit = {},
    debugPlaybackDisplayModeRequest: DebugPlaybackDisplayModeRequest? = null,
    onDebugPlaybackDisplayModeHandled: (Long) -> Unit = {},
    onImportAudio: () -> Unit,
    viewModel: AudioAndroidViewModel,
    modifier: Modifier = Modifier,
) {
    val colorScheme =
        when (uiState.selectedThemeStyle) {
            // Dual-tone brand themes are curated, fixed looks. They can be either light or dark
            // on their own, so they do not follow the app's Material light/dark mode toggle.
            ThemeStyleOption.BrandDualTone -> uiState.activeBrandTheme.colorScheme
            ThemeStyleOption.Material ->
                if (shouldUseDarkTheme(uiState.selectedThemeMode)) {
                    uiState.selectedPalette.darkScheme
                } else {
                    uiState.selectedPalette.lightScheme
                }
        }
    val accentTokens =
        when (uiState.selectedThemeStyle) {
            ThemeStyleOption.BrandDualTone -> brandAccentTokens(uiState.activeBrandTheme)
            ThemeStyleOption.Material -> materialAccentTokens(colorScheme.primary)
        }
    val audioEncodeGlyphColors =
        when (uiState.selectedThemeStyle) {
            ThemeStyleOption.BrandDualTone ->
                audioEncodeGlyphColorsForBrandTheme(uiState.activeBrandTheme)
            ThemeStyleOption.Material -> defaultAudioEncodeGlyphColors()
        }
    val visualTokens =
        when (uiState.selectedThemeStyle) {
            ThemeStyleOption.BrandDualTone -> brandThemeVisualTokens(uiState.activeBrandTheme, accentTokens)
            ThemeStyleOption.Material -> materialThemeVisualTokens(colorScheme)
        }

    MaterialTheme(colorScheme = colorScheme) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalAppThemeAccentTokens provides accentTokens,
            LocalAppThemeVisualTokens provides visualTokens,
            LocalAudioEncodeGlyphColors provides audioEncodeGlyphColors,
        ) {
            when {
                uiState.showLicensesPage -> {
                    OpenSourceLicensesScreen(onBack = viewModel::onCloseLicensesPage)
                }

                uiState.showAboutPage -> {
                    AboutScreen(
                        onBack = viewModel::onCloseAboutPage,
                        onOpenLicensesPage = viewModel::onOpenLicensesPage,
                        presentationVersion = uiState.presentationVersion,
                        coreVersion = uiState.coreVersion,
                    )
                }

                else -> {
                    AudioAndroidMainScaffold(
                        uiState = uiState,
                        savedAudioFilter = savedAudioFilter,
                        onSavedAudioFilterChange = onSavedAudioFilterChange,
                        accentTokens = accentTokens,
                        materialPalettes = MaterialPalettes,
                        brandThemes = BrandDualToneThemes,
                        viewModel = viewModel,
                        debugScenario = debugScenario,
                        debugExpandLyricsRequestId = debugExpandLyricsRequestId,
                        onDebugExpandLyricsHandled = onDebugExpandLyricsHandled,
                        debugPlaybackDisplayModeRequest = debugPlaybackDisplayModeRequest,
                        onDebugPlaybackDisplayModeHandled = onDebugPlaybackDisplayModeHandled,
                        onImportAudio = onImportAudio,
                        modifier = modifier,
                    )
                }
            }
        }
    }
}
