package com.bag.audioandroid.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.asString
import com.bag.audioandroid.ui.screen.AboutScreen
import com.bag.audioandroid.ui.screen.AudioTabScreen
import com.bag.audioandroid.ui.screen.ConfigTabScreen
import com.bag.audioandroid.ui.screen.LibraryTabScreen
import com.bag.audioandroid.ui.screen.MiniPlayerBar
import com.bag.audioandroid.ui.screen.OpenSourceLicensesScreen
import com.bag.audioandroid.ui.screen.PlayerDetailSheetContent
import com.bag.audioandroid.ui.screen.SavedAudioPickerSheet
import com.bag.audioandroid.ui.screen.formatDurationMillis
import com.bag.audioandroid.ui.screen.samplesToMillis
import com.bag.audioandroid.ui.state.AudioAppUiState

private val AudioAndroidNavigationTabs = listOf(AppTab.Config, AppTab.Audio, AppTab.Library)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioAndroidAppShell(
    uiState: AudioAppUiState,
    savedAudioFilter: SavedAudioModeFilter,
    onSavedAudioFilterChange: (SavedAudioModeFilter) -> Unit,
    onImportAudio: () -> Unit,
    viewModel: AudioAndroidViewModel,
    modifier: Modifier = Modifier
) {
    val colorScheme = if (shouldUseDarkTheme(uiState.selectedThemeMode)) {
        uiState.selectedPalette.darkScheme
    } else {
        uiState.selectedPalette.lightScheme
    }

    MaterialTheme(colorScheme = colorScheme) {
        when {
            uiState.showLicensesPage -> {
                OpenSourceLicensesScreen(onBack = viewModel::onCloseLicensesPage)
            }

            uiState.showAboutPage -> {
                AboutScreen(
                    onBack = viewModel::onCloseAboutPage,
                    onOpenLicensesPage = viewModel::onOpenLicensesPage,
                    presentationVersion = uiState.presentationVersion,
                    coreVersion = uiState.coreVersion
                )
            }

            else -> {
                AudioAndroidMainScaffold(
                    uiState = uiState,
                    savedAudioFilter = savedAudioFilter,
                    onSavedAudioFilterChange = onSavedAudioFilterChange,
                    viewModel = viewModel,
                    onImportAudio = onImportAudio,
                    modifier = modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioAndroidMainScaffold(
    uiState: AudioAppUiState,
    savedAudioFilter: SavedAudioModeFilter,
    onSavedAudioFilterChange: (SavedAudioModeFilter) -> Unit,
    onImportAudio: () -> Unit,
    viewModel: AudioAndroidViewModel,
    modifier: Modifier = Modifier
) {
    val currentSession = uiState.currentSession
    val currentPlayback = uiState.currentPlayback
    val miniPlayerModel = uiState.miniPlayerModel
    val snackbarHostState = remember { SnackbarHostState() }
    val playerDetailSnackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = uiState.snackbarMessage
    val snackbarText = snackbarMessage?.text?.asString().orEmpty()
    val displayedTime = formatDurationMillis(
        samplesToMillis(currentPlayback.displayedSamples, currentPlayback.sampleRateHz)
    )
    val totalTime = formatDurationMillis(
        samplesToMillis(currentPlayback.totalSamples, currentPlayback.sampleRateHz)
    )
    val navigationBarColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
        unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    )

    LaunchedEffect(snackbarMessage?.id) {
        val message = snackbarMessage ?: return@LaunchedEffect
        val hostState = if (uiState.showPlayerDetailSheet) {
            playerDetailSnackbarHostState
        } else {
            snackbarHostState
        }
        hostState.showSnackbar(snackbarText)
        viewModel.onSnackbarMessageShown(message.id)
    }

    if (uiState.showSavedAudioSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onCloseSavedAudioSheet
        ) {
            SavedAudioPickerSheet(
                savedAudioItems = uiState.savedAudioItems,
                selectedFilter = savedAudioFilter,
                onFilterSelected = onSavedAudioFilterChange,
                onSavedAudioSelected = viewModel::onShellSavedAudioSelected
            )
        }
    }

    if (uiState.showPlayerDetailSheet && miniPlayerModel != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onClosePlayerDetailSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = playerDetailSnackbarHostState)
                }
            ) { sheetInnerPadding ->
                PlayerDetailSheetContent(
                    miniPlayerModel = miniPlayerModel,
                    displayedSamples = currentPlayback.displayedSamples,
                    totalSamples = currentPlayback.totalSamples,
                    isScrubbing = currentPlayback.isScrubbing,
                    waveformPcm = uiState.currentPlaybackPcm,
                    sampleRateHz = currentPlayback.sampleRateHz,
                    displayedTime = displayedTime,
                    totalTime = totalTime,
                    isPlaying = currentPlayback.isPlaying,
                    playbackSequenceMode = uiState.playbackSequenceMode,
                    canSkipPrevious = uiState.canSkipPrevious,
                    canSkipNext = uiState.canSkipNext,
                    canExportGeneratedAudio = uiState.currentPlaybackSource is AudioPlaybackSource.Generated &&
                        uiState.currentPlaybackSampleCount > 0,
                    isCodecBusy = uiState.currentSession.isCodecBusy,
                    decodedText = uiState.currentPlaybackDecodedText,
                    savedAudioItem = uiState.currentSavedAudioItem,
                    onTogglePlayback = viewModel::onTogglePlayback,
                    onDecodeAudio = viewModel::onDecode,
                    onClearDecodedText = viewModel::onClearResult,
                    onSkipToPreviousTrack = viewModel::onSkipToPreviousTrack,
                    onSkipToNextTrack = viewModel::onSkipToNextTrack,
                    onPlaybackSequenceModeSelected = viewModel::onPlaybackSequenceModeSelected,
                    onExportGeneratedAudio = viewModel::onExportAudio,
                    onShareSavedAudio = uiState.currentSavedAudioItem?.let { viewModel::onShareCurrentSavedAudio },
                    onOpenSavedAudioSheet = viewModel::onOpenSavedAudioSheet,
                    onScrubStarted = viewModel::onScrubStarted,
                    onScrubChanged = viewModel::onScrubChanged,
                    onScrubFinished = viewModel::onScrubFinished,
                    modifier = Modifier.padding(sheetInnerPadding)
                )
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            Column {
                miniPlayerModel?.let { model ->
                    MiniPlayerBar(
                        model = model,
                        isPlaying = currentPlayback.isPlaying,
                        onTogglePlayback = viewModel::onTogglePlayback,
                        onOpenSavedAudioSheet = viewModel::onOpenSavedAudioSheet,
                        onOpenDetails = viewModel::onOpenPlayerDetailSheet,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    AudioAndroidNavigationTabs.forEach { tab ->
                        val selected = tab == uiState.selectedTab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.onTabSelected(tab) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelResId)
                                )
                            },
                            label = { Text(stringResource(tab.labelResId)) },
                            colors = navigationBarColors
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (uiState.selectedTab) {
            AppTab.Config -> ConfigTabScreen(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageSelected = viewModel::onLanguageSelected,
                selectedThemeMode = uiState.selectedThemeMode,
                onThemeModeSelected = viewModel::onThemeModeSelected,
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
                isCodecBusy = uiState.currentSession.isCodecBusy,
                encodeProgress = uiState.currentSession.encodeProgress,
                encodePhase = uiState.currentSession.encodePhase,
                isEncodeCancelling = uiState.currentSession.isEncodeCancelling,
                onTransportModeSelected = viewModel::onTransportModeSelected,
                selectedFlashVoicingStyle = uiState.selectedFlashVoicingStyle,
                onFlashVoicingStyleSelected = viewModel::onFlashVoicingStyleSelected,
                inputText = currentSession.inputText,
                onInputTextChange = viewModel::onInputTextChange,
                onRandomizeSampleInput = viewModel::onRandomizeSampleInput,
                resultText = currentSession.resultText,
                onEncode = viewModel::onEncode,
                onCancelEncode = viewModel::onCancelEncode,
                onDecode = viewModel::onDecode,
                onClear = viewModel::onClear,
                onClearResult = viewModel::onClearResult,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )

            AppTab.Library -> LibraryTabScreen(
                savedAudioItems = uiState.savedAudioItems,
                librarySelection = uiState.librarySelection,
                statusText = uiState.libraryStatusText,
                onImportAudio = onImportAudio,
                onSelectSavedAudio = viewModel::onSavedAudioSelected,
                onEnterLibrarySelection = viewModel::onEnterLibrarySelection,
                onToggleLibrarySelection = viewModel::onToggleLibrarySelection,
                onSelectAllLibraryItems = viewModel::onSelectAllLibraryItems,
                onDeleteSelectedSavedAudio = viewModel::onDeleteSelectedSavedAudio,
                onClearLibrarySelection = viewModel::onClearLibrarySelection,
                onDeleteSavedAudio = viewModel::onDeleteSavedAudio,
                onRenameSavedAudio = viewModel::onRenameSavedAudio,
                onShareSavedAudio = viewModel::onShareSavedAudio,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun shouldUseDarkTheme(selectedThemeMode: ThemeModeOption): Boolean {
    val systemDarkTheme = isSystemInDarkTheme()
    return when (selectedThemeMode) {
        ThemeModeOption.FollowSystem -> systemDarkTheme
        ThemeModeOption.Light -> false
        ThemeModeOption.Dark -> true
    }
}
