package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.effectiveSampleFlavor
import com.bag.audioandroid.ui.state.AudioAppUiState

internal val AudioAppUiState.currentSampleFlavor: SampleFlavor
    get() = effectiveSampleFlavor(selectedThemeStyle, selectedBrandTheme)

internal fun AudioAppUiState.withSelectedBrandTheme(
    brandTheme: BrandThemeOption,
    sampleInputSessionUpdater: SampleInputSessionUpdater,
): AudioAppUiState {
    if (selectedBrandTheme.id == brandTheme.id) {
        return this
    }
    val newFlavor = effectiveSampleFlavor(selectedThemeStyle, brandTheme)
    val updatedSessions =
        if (currentSampleFlavor != newFlavor) {
            sampleInputSessionUpdater.refreshForFlavorChange(
                sessions = sessions,
                language = selectedLanguage,
                newFlavor = newFlavor,
            )
        } else {
            sessions
        }
    return copy(
        selectedBrandTheme = brandTheme,
        sessions = updatedSessions,
    )
}

internal fun AudioAppUiState.withSelectedThemeStyle(
    themeStyle: ThemeStyleOption,
    sampleInputSessionUpdater: SampleInputSessionUpdater,
): AudioAppUiState {
    if (selectedThemeStyle == themeStyle) {
        return this
    }
    val newFlavor = effectiveSampleFlavor(themeStyle, selectedBrandTheme)
    val updatedSessions =
        if (currentSampleFlavor != newFlavor) {
            sampleInputSessionUpdater.refreshForFlavorChange(
                sessions = sessions,
                language = selectedLanguage,
                newFlavor = newFlavor,
            )
        } else {
            sessions
        }
    return copy(
        selectedThemeStyle = themeStyle,
        sessions = updatedSessions,
    )
}
