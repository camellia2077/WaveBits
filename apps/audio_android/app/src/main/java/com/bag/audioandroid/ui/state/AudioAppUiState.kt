package com.bag.audioandroid.ui.state

import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.theme.MaterialPalettes

data class AudioAppUiState(
    val selectedTab: AppTab = AppTab.Audio,
    val showAboutPage: Boolean = false,
    val showLicensesPage: Boolean = false,
    val presentationVersion: String = "unknown",
    val coreVersion: String = "unknown",
    val selectedPalette: PaletteOption = MaterialPalettes.first(),
    val transportMode: TransportModeOption = TransportModeOption.Flash,
    val inputText: String = "Hello Android",
    val generatedPcm: ShortArray = shortArrayOf(),
    val resultText: String = "",
    val statusText: String = "请输入文本后点击“文本转音频”",
    val isPlaying: Boolean = false,
    val playbackProgress: Float = 0f
)
