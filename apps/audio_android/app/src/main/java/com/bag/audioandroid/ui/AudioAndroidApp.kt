package com.bag.audioandroid.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.state.AudioAppUiState

@Composable
fun AudioAndroidApp() {
    val appContext = LocalContext.current.applicationContext
    val factory = rememberAudioAndroidViewModelFactory(appContext)
    val viewModel: AudioAndroidViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onImportAudio(it.toString()) }
    }
    var savedAudioFilter by rememberSaveable {
        mutableStateOf(defaultSavedAudioFilter(uiState))
    }

    LaunchedEffect(uiState.showSavedAudioSheet, uiState.currentPlaybackSource, uiState.transportMode) {
        if (uiState.showSavedAudioSheet) {
            savedAudioFilter = defaultSavedAudioFilter(uiState)
        }
    }

    LaunchedEffect(uiState.showPlayerDetailSheet, uiState.miniPlayerModel) {
        if (uiState.showPlayerDetailSheet && uiState.miniPlayerModel == null) {
            viewModel.onClosePlayerDetailSheet()
        }
    }

    AudioAndroidAppShell(
        uiState = uiState,
        savedAudioFilter = savedAudioFilter,
        onSavedAudioFilterChange = { savedAudioFilter = it },
        onImportAudio = { importAudioLauncher.launch(arrayOf("audio/*")) },
        viewModel = viewModel
    )
}

private fun defaultSavedAudioFilter(uiState: AudioAppUiState): SavedAudioModeFilter =
    when (val source = uiState.currentPlaybackSource) {
        is AudioPlaybackSource.Generated -> SavedAudioModeFilter.fromTransportMode(source.mode)
        is AudioPlaybackSource.Saved -> SavedAudioModeFilter.entries.firstOrNull {
            it.mode?.wireName == uiState.currentSavedAudioItem?.modeWireName
        } ?: SavedAudioModeFilter.All
    }
