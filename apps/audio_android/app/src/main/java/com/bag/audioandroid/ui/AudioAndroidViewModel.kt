package com.bag.audioandroid.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.audio.AudioPlaybackCoordinator
import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AudioAndroidViewModel(
    audioCodecGateway: AudioCodecGateway,
    audioIoGateway: AudioIoGateway,
    private val sampleInputTextProvider: SampleInputTextProvider,
    appSettingsRepository: AppSettingsRepository,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    savedAudioRepository: SavedAudioRepository,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
) : ViewModel() {
    private val uiStateFlow = MutableStateFlow(AudioAppUiState())
    val uiState: StateFlow<AudioAppUiState> = uiStateFlow

    private val uiTextMapper = BagUiTextMapper()
    private val sampleInputSessionUpdater = SampleInputSessionUpdater(sampleInputTextProvider)
    private val sessionStateStore = AudioSessionStateStore(uiStateFlow)
    private val playbackCoordinator = AudioPlaybackCoordinator()
    private val playbackSourceCoordinator = PlaybackSourceCoordinator(SAMPLE_RATE_HZ)
    private val playbackSessionReducer = PlaybackSessionReducer(playbackRuntimeGateway, SAMPLE_RATE_HZ)
    private val playbackSequenceNavigator = PlaybackSequenceNavigator()
    private val playbackActions =
        AudioAndroidPlaybackActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            sessionStateStore = sessionStateStore,
            playbackCoordinator = playbackCoordinator,
            playbackRuntimeGateway = playbackRuntimeGateway,
            playbackSourceCoordinator = playbackSourceCoordinator,
            playbackSessionReducer = playbackSessionReducer,
            sampleRateHz = SAMPLE_RATE_HZ,
            onPlaybackCompleted = ::handlePlaybackCompleted,
        )
    private val libraryActions =
        AudioAndroidLibraryActions(
            uiState = uiStateFlow,
            sessionStateStore = sessionStateStore,
            audioCodecGateway = audioCodecGateway,
            playbackRuntimeGateway = playbackRuntimeGateway,
            savedAudioRepository = savedAudioRepository,
            stopPlayback = playbackActions::stopPlayback,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
        )
    private val documentExportActions =
        AudioDocumentExportActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            sessionStateStore = sessionStateStore,
            savedAudioRepository = savedAudioRepository,
            sampleRateHz = SAMPLE_RATE_HZ,
            workerDispatcher = Dispatchers.IO,
        )
    private val sessionActions =
        AudioAndroidSessionActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            audioCodecGateway = audioCodecGateway,
            audioIoGateway = audioIoGateway,
            sampleInputTextProvider = sampleInputTextProvider,
            sessionStateStore = sessionStateStore,
            uiTextMapper = uiTextMapper,
            playbackRuntimeGateway = playbackRuntimeGateway,
            savedAudioRepository = savedAudioRepository,
            sampleRateHz = SAMPLE_RATE_HZ,
            frameSamples = FRAME_SAMPLES,
            stopPlayback = playbackActions::stopPlayback,
            refreshSavedAudioItems = libraryActions::refreshSavedAudioItems,
            workerDispatcher = Dispatchers.IO,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
        )
    private val navigationActions =
        AudioAndroidNavigationActions(
            uiState = uiStateFlow,
        )
    private val preferencesActions =
        AudioAndroidPreferencesActions(
            uiState = uiStateFlow,
            sampleInputSessionUpdater = sampleInputSessionUpdater,
            appSettingsRepository = appSettingsRepository,
            scope = viewModelScope,
        )
    private val preferencesBindings =
        AudioAndroidPreferencesBindings(
            uiState = uiStateFlow,
            sampleInputSessionUpdater = sampleInputSessionUpdater,
            appSettingsRepository = appSettingsRepository,
            scope = viewModelScope,
        )

    init {
        val coreVersion = audioCodecGateway.getCoreVersion()
        val selectedLanguage =
            AppLanguageOption.fromLanguageTags(
                AppCompatDelegate.getApplicationLocales().toLanguageTags(),
            )
        uiStateFlow.update {
            it.copy(
                selectedLanguage = selectedLanguage,
                presentationVersion = BuildConfig.VERSION_NAME.ifBlank { "" },
                coreVersion = coreVersion.ifBlank { "" },
                sessions =
                    sampleInputSessionUpdater.initialize(
                        sessions = it.sessions,
                        language = selectedLanguage,
                        flavor = it.currentSampleFlavor,
                    ),
                currentPlaybackSource = AudioPlaybackSource.Generated(it.transportMode),
            )
        }
        preferencesBindings.startObserving()
    }

    fun onTabSelected(tab: AppTab) {
        navigationActions.onTabSelected(tab, libraryActions::refreshSavedAudioItems)
    }

    fun onLanguageSelected(language: AppLanguageOption) {
        preferencesActions.onLanguageSelected(language)
    }

    fun onOpenAboutPage() {
        navigationActions.onOpenAboutPage()
    }

    fun onCloseAboutPage() {
        navigationActions.onCloseAboutPage()
    }

    fun onOpenLicensesPage() {
        navigationActions.onOpenLicensesPage()
    }

    fun onCloseLicensesPage() {
        navigationActions.onCloseLicensesPage()
    }

    fun onPaletteSelected(palette: PaletteOption) {
        preferencesActions.onPaletteSelected(palette)
    }

    fun onThemeModeSelected(themeMode: ThemeModeOption) {
        preferencesActions.onThemeModeSelected(themeMode)
    }

    fun onThemeStyleSelected(themeStyle: ThemeStyleOption) {
        preferencesActions.onThemeStyleSelected(themeStyle)
    }

    fun onBrandThemeSelected(brandTheme: BrandThemeOption) {
        preferencesActions.onBrandThemeSelected(brandTheme)
    }

    fun onCustomBrandThemeSaved(
        settings: CustomBrandThemeSettings,
        replacePresetId: String?,
    ) {
        preferencesActions.onCustomBrandThemeSaved(settings, replacePresetId)
    }

    fun onCustomBrandThemeDeleted(presetId: String) {
        preferencesActions.onCustomBrandThemeDeleted(presetId)
    }

    fun onConfigLanguageExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigLanguageExpandedChanged(expanded)
    }

    fun onConfigThemeAppearanceExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigThemeAppearanceExpandedChanged(expanded)
    }

    fun onFlashVoicingStyleSelected(style: com.bag.audioandroid.ui.model.FlashVoicingStyleOption) {
        preferencesActions.onFlashVoicingStyleSelected(style)
    }

    fun onMorseSpeedSelected(speed: MorseSpeedOption) {
        uiStateFlow.update { it.copy(selectedMorseSpeed = speed) }
    }

    fun onOpenPlayerDetailSheet() {
        playbackActions.onPlaybackSpeedSelected(PlaybackSpeedOption.default.speed)
        navigationActions.onOpenPlayerDetailSheet()
    }

    fun onClosePlayerDetailSheet() {
        navigationActions.onClosePlayerDetailSheet()
    }

    fun onSnackbarMessageShown(messageId: Long) {
        navigationActions.onSnackbarMessageShown(messageId)
    }

    fun onInputTextChange(value: String) {
        sessionActions.onInputTextChange(value)
    }

    fun onRandomizeSampleInput(length: SampleInputLengthOption) {
        sessionActions.onRandomizeSampleInput(length)
    }

    fun currentPlaceholderText(mode: TransportModeOption): String {
        val state = uiStateFlow.value
        val currentSession = state.sessions.getValue(mode)
        if (currentSession.sampleInputId == null) {
            return ""
        }
        return sampleInputTextProvider
            .defaultSample(
                mode = mode,
                language = state.selectedLanguage,
                flavor = state.currentSampleFlavor,
            ).text
    }

    fun onTransportModeSelected(mode: TransportModeOption) {
        sessionActions.onTransportModeSelected(mode)
    }

    fun onEncode() {
        sessionActions.onEncode()
    }

    fun onCancelEncode() {
        sessionActions.onCancelEncode()
    }

    fun onTogglePlayback() {
        playbackActions.onTogglePlayback()
    }

    fun onPlaybackSequenceModeSelected(mode: PlaybackSequenceMode) {
        preferencesActions.onPlaybackSequenceModeSelected(mode)
    }

    fun onPlaybackSpeedSelected(playbackSpeed: Float) {
        playbackActions.onPlaybackSpeedSelected(playbackSpeed)
    }

    fun onSkipToPreviousTrack() {
        skipToAdjacentSavedTrack { state, currentSource ->
            playbackSequenceNavigator.previousSavedSource(
                savedAudioItems = state.savedAudioItems,
                currentSource = currentSource,
            )
        }
    }

    fun onSkipToNextTrack() {
        skipToAdjacentSavedTrack { state, currentSource ->
            playbackSequenceNavigator.nextSavedSource(
                savedAudioItems = state.savedAudioItems,
                currentSource = currentSource,
            )
        }
    }

    fun onScrubStarted() {
        playbackActions.onScrubStarted()
    }

    fun onScrubChanged(targetSamples: Int) {
        playbackActions.onScrubChanged(targetSamples)
    }

    fun onScrubFinished() {
        playbackActions.onScrubFinished()
    }

    fun onScrubCanceled() {
        playbackActions.onScrubCanceled()
    }

    fun onDecode() {
        sessionActions.onDecode()
    }

    fun ensureCurrentPlaybackDecodedForLyrics() {
        sessionActions.ensureCurrentPlaybackDecodedForLyrics()
    }

    fun onClear() {
        sessionActions.onClear()
    }

    fun onClearResult() {
        sessionActions.onClearResult()
    }

    fun onExportAudio() {
        sessionActions.onExportAudio()
    }

    fun onRequestExportGeneratedAudioToDocument() {
        documentExportActions.onRequestGeneratedAudioExportToDocument()
    }

    fun onDocumentExportPicked(uriString: String?) {
        documentExportActions.onDocumentExportPicked(uriString)
    }

    override fun onCleared() {
        playbackActions.release()
        uiStateFlow.value.sessions.values.forEach { session ->
            generatedAudioCacheGateway.deleteCachedFile(session.generatedPcmFilePath)
        }
        generatedAudioCacheGateway.deleteCachedFile(uiStateFlow.value.selectedSavedAudio?.pcmFilePath)
        super.onCleared()
    }

    fun onOpenSavedAudioSheet() {
        sessionActions.onOpenSavedAudioSheet()
    }

    fun onCloseSavedAudioSheet() {
        sessionActions.onCloseSavedAudioSheet()
    }

    fun onSavedAudioSelected(itemId: String) {
        libraryActions.onSavedAudioSelected(itemId)
    }

    fun onImportAudio(uriString: String) {
        libraryActions.onImportAudio(uriString)
    }

    fun onShellSavedAudioSelected(itemId: String) {
        libraryActions.onShellSavedAudioSelected(itemId)
    }

    fun onEnterLibrarySelection(itemId: String) {
        libraryActions.onEnterLibrarySelection(itemId)
    }

    fun onToggleLibrarySelection(itemId: String) {
        libraryActions.onToggleLibrarySelection(itemId)
    }

    fun onSelectAllLibraryItems(itemIds: Collection<String>) {
        libraryActions.onSelectAllLibraryItems(itemIds)
    }

    fun onClearLibrarySelection() {
        libraryActions.onClearLibrarySelection()
    }

    fun onDeleteSelectedSavedAudio() {
        libraryActions.onDeleteSelectedSavedAudio()
    }

    fun onDeleteSavedAudio(itemId: String) {
        libraryActions.onDeleteSavedAudio(itemId)
    }

    fun onRenameSavedAudio(
        itemId: String,
        newBaseName: String,
    ) {
        libraryActions.onRenameSavedAudio(itemId, newBaseName)
    }

    fun onShareCurrentSavedAudio() {
        libraryActions.onShareCurrentSavedAudio()
    }

    fun onShareSavedAudio(item: SavedAudioItem) {
        libraryActions.onShareSavedAudio(item)
    }

    fun onRequestExportSavedAudioToDocument(item: SavedAudioItem) {
        documentExportActions.onRequestSavedAudioExportToDocument(item)
    }

    fun onCreateSavedAudioFolder(name: String) {
        libraryActions.onCreateSavedAudioFolder(name)
    }

    fun onRenameSavedAudioFolder(
        folderId: String,
        name: String,
    ) {
        libraryActions.onRenameSavedAudioFolder(folderId, name)
    }

    fun onDeleteSavedAudioFolder(folderId: String) {
        libraryActions.onDeleteSavedAudioFolder(folderId)
    }

    fun onMoveSavedAudioToFolder(
        itemIds: Collection<String>,
        folderId: String?,
    ) {
        libraryActions.onMoveSavedAudioToFolder(itemIds, folderId)
    }

    private fun handlePlaybackCompleted(source: AudioPlaybackSource): Boolean {
        val nextSource = playbackSequenceNavigator.nextSourceForCompletion(uiStateFlow.value, source) ?: return false
        return when (nextSource) {
            is AudioPlaybackSource.Generated -> playbackActions.playCurrentFromStart()
            is AudioPlaybackSource.Saved -> {
                if (nextSource.itemId != uiStateFlow.value.currentSavedAudioItem?.itemId &&
                    !libraryActions.prepareSavedAudioSelection(nextSource.itemId)
                ) {
                    return false
                }
                playbackActions.playCurrentFromStart()
            }
        }
    }

    private fun skipToAdjacentSavedTrack(resolveTarget: (AudioAppUiState, AudioPlaybackSource) -> AudioPlaybackSource?) {
        val currentState = uiStateFlow.value
        val currentSource = currentState.currentPlaybackSource
        val targetSource = resolveTarget(currentState, currentSource) as? AudioPlaybackSource.Saved ?: return
        if (!libraryActions.prepareSavedAudioSelection(targetSource.itemId)) {
            return
        }
        playbackActions.playCurrentFromStart()
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 44100
        const val FRAME_SAMPLES = 2205
    }
}
