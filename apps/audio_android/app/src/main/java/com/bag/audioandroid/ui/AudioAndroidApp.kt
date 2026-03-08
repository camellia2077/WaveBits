package com.bag.audioandroid.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bag.audioandroid.data.NativeAudioCodecGateway
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.screen.AboutScreen
import com.bag.audioandroid.ui.screen.AudioTabScreen
import com.bag.audioandroid.ui.screen.ConfigTabScreen
import com.bag.audioandroid.ui.screen.OpenSourceLicensesScreen

@Composable
fun AudioAndroidApp() {
    val audioCodecGateway = remember { NativeAudioCodecGateway() }
    val factory = remember(audioCodecGateway) { AudioAndroidViewModelFactory(audioCodecGateway) }
    val viewModel: AudioAndroidViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showLicensesPage) {
        MaterialTheme(colorScheme = uiState.selectedPalette.scheme) {
            OpenSourceLicensesScreen(onBack = viewModel::onCloseLicensesPage)
        }
        return
    }

    if (uiState.showAboutPage) {
        MaterialTheme(colorScheme = uiState.selectedPalette.scheme) {
            AboutScreen(
                onBack = viewModel::onCloseAboutPage,
                onOpenLicensesPage = viewModel::onOpenLicensesPage,
                presentationVersion = uiState.presentationVersion,
                coreVersion = uiState.coreVersion
            )
        }
        return
    }

    MaterialTheme(colorScheme = uiState.selectedPalette.scheme) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == uiState.selectedTab,
                            onClick = { viewModel.onTabSelected(tab) },
                            icon = { Text(if (tab == AppTab.Config) "C" else "A") },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (uiState.selectedTab) {
                AppTab.Config -> ConfigTabScreen(
                    selectedPalette = uiState.selectedPalette,
                    onPaletteSelected = viewModel::onPaletteSelected,
                    onOpenAboutPage = viewModel::onOpenAboutPage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )

                AppTab.Audio -> AudioTabScreen(
                    transportMode = uiState.transportMode,
                    onTransportModeSelected = viewModel::onTransportModeSelected,
                    inputText = uiState.inputText,
                    onInputTextChange = viewModel::onInputTextChange,
                    generatedPcm = uiState.generatedPcm,
                    resultText = uiState.resultText,
                    statusText = uiState.statusText,
                    isPlaying = uiState.isPlaying,
                    playbackProgress = uiState.playbackProgress,
                    onEncode = viewModel::onEncode,
                    onPlay = viewModel::onPlay,
                    onDecode = viewModel::onDecode,
                    onClear = viewModel::onClear,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
    }
}
