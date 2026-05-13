package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState

@Composable
internal fun PlayerDetailSheetContent(
    miniPlayerModel: MiniPlayerUiModel,
    displayedSamples: Int,
    waveformDisplayedSamples: Int = displayedSamples,
    totalSamples: Int,
    isScrubbing: Boolean,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean = false,
    sampleRateHz: Int,
    frameSamples: Int = 2205,
    wavAudioInfo: WavAudioInfo = WavAudioInfo.Empty,
    flashSignalInfo: FlashSignalInfo = FlashSignalInfo.Empty,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    playbackSpeed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    canExportGeneratedAudio: Boolean,
    followData: PayloadFollowViewData,
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    savedAudioItem: SavedAudioItem?,
    showSavedAudioDecodeLoadingNotice: Boolean = false,
    isFlashVisualPerfOverlayEnabled: Boolean = false,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onExportGeneratedAudio: () -> Unit,
    onExportGeneratedAudioToDocument: () -> Unit,
    onShareSavedAudio: (() -> Unit)?,
    onOpenSavedAudioSheet: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onLyricsRequested: () -> Unit = {},
    onPlaybackDisplayModeSelected: (PlaybackDisplayMode) -> Unit = {},
    debugExpandLyricsRequestId: Long? = null,
    onDebugExpandLyricsHandled: (Long) -> Unit = {},
    debugPlaybackDisplayModeRequest: DebugPlaybackDisplayModeRequest? = null,
    onDebugPlaybackDisplayModeHandled: (Long) -> Unit = {},
    onSeekToSample: (Int) -> Unit = { targetSamples ->
        onScrubStarted()
        onScrubChanged(targetSamples)
        onScrubFinished()
    },
    initialDisplayMode: PlaybackDisplayMode = PlaybackDisplayMode.Lyrics,
    initialFlashVisualizationMode: FlashSignalVisualizationMode? = null,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var rootSlice by remember { mutableStateOf<PlaybackVerticalSlice?>(null) }
    var scrollSlice by remember { mutableStateOf<PlaybackVerticalSlice?>(null) }
    var displaySlice by remember { mutableStateOf<PlaybackVerticalSlice?>(null) }
    var bottomDockSlice by remember { mutableStateOf<PlaybackVerticalSlice?>(null) }
    val displaySectionState =
        rememberPlaybackDisplaySectionState(
            isFlashMode = miniPlayerModel.isFlashMode,
            onLyricsRequested = onLyricsRequested,
            initialDisplayMode = initialDisplayMode,
            initialFlashVisualizationMode = initialFlashVisualizationMode ?: FlashSignalVisualizationMode.Lanes,
            onDisplayModeSelected = onPlaybackDisplayModeSelected,
        )
    val layoutPolicyState =
        rememberPlayerDetailLayoutPolicyState(
            transportMode = miniPlayerModel.transportMode,
            playbackDisplayMode = displaySectionState.playbackDisplayMode,
            displaySlice = displaySlice,
            bottomDockSlice = bottomDockSlice,
            density = density,
        )
    LaunchedEffect(debugExpandLyricsRequestId) {
        val requestId = debugExpandLyricsRequestId ?: return@LaunchedEffect
        displaySectionState.onLyricsExpandedChanged(true)
        android.util.Log.d(playerDetailAutomationTag(miniPlayerModel.transportMode), "lyricsExpanded requestId=$requestId expanded=true")
        onDebugExpandLyricsHandled(requestId)
    }
    LaunchedEffect(debugPlaybackDisplayModeRequest) {
        val request = debugPlaybackDisplayModeRequest ?: return@LaunchedEffect
        displaySectionState.onDisplayModeSelected(request.mode)
        android.util.Log.d(
            playerDetailAutomationTag(miniPlayerModel.transportMode),
            "displayModeApplied requestId=${request.requestId} mode=${request.mode.name.lowercase()}",
        )
        onDebugPlaybackDisplayModeHandled(request.requestId)
    }
    LaunchedEffect(
        miniPlayerModel.transportMode,
        displaySectionState.playbackDisplayMode,
        rootSlice,
        scrollSlice,
        displaySlice,
        bottomDockSlice,
    ) {
        val root = rootSlice ?: return@LaunchedEffect
        val scroll = scrollSlice ?: return@LaunchedEffect
        val display = displaySlice ?: return@LaunchedEffect
        val dock = bottomDockSlice ?: return@LaunchedEffect
        val gapToScrollBottomPx = (scroll.bottomPx - display.bottomPx).coerceAtLeast(0)
        val gapToDockTopPx = (dock.topPx - display.bottomPx).coerceAtLeast(0)
        android.util.Log.d(
            "PlaybackVerticalLayout",
            "transport=${miniPlayerModel.transportMode.wireName} displayMode=${displaySectionState.playbackDisplayMode.name.lowercase()} " +
                "rootHeightDp=${root.heightDp(density)} scrollHeightDp=${scroll.heightDp(density)} " +
                "displayHeightDp=${display.heightDp(density)} bottomDockHeightDp=${dock.heightDp(density)} " +
                "displayBottomToScrollBottomDp=${pxToDpString(gapToScrollBottomPx, density)} " +
                "displayBottomToDockTopDp=${pxToDpString(gapToDockTopPx, density)} " +
                "scrollTopDp=${scroll.topDp(density)} displayTopDp=${display.topDp(density)} dockTopDp=${dock.topDp(density)}",
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    rootSlice = coordinates.toPlaybackVerticalSlice()
                }.testTag("player-detail-sheet-content"),
    ) {
        PlayerDetailScrollContent(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
            displayedSamples = displayedSamples,
            waveformDisplayedSamples = waveformDisplayedSamples,
            waveformPcm = waveformPcm,
            isWaveformPreview = isWaveformPreview,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            transportMode = miniPlayerModel.transportMode,
            isFlashMode = miniPlayerModel.isFlashMode,
            flashVoicingStyle = miniPlayerModel.flashVoicingStyle,
            followData = followData,
            flashVisualWindow = flashVisualWindow,
            isPlaying = isPlaying,
            isScrubbing = isScrubbing,
            isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
            playbackSpeed = playbackSpeed,
            displaySectionState = displaySectionState,
            savedAudioItem = savedAudioItem,
            showSavedAudioDecodeLoadingNotice = showSavedAudioDecodeLoadingNotice,
            extraLyricsRecoveryHeight = layoutPolicyState.extraLyricsRecoveryHeight,
            applyLyricsPreviewBonusLine = layoutPolicyState.applyLyricsPreviewBonusLine,
            onLayoutMeasured = { slice -> displaySlice = slice },
            onSeekToSample = onSeekToSample,
            onContainerMeasured = { slice -> scrollSlice = slice },
        )
        PlayerDetailBottomDock(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        bottomDockSlice = coordinates.toPlaybackVerticalSlice()
                    },
            displayedSamples = displayedSamples,
            totalSamples = totalSamples,
            isScrubbing = isScrubbing,
            displayedTime = displayedTime,
            totalTime = totalTime,
            isPlaying = isPlaying,
            playbackSequenceMode = playbackSequenceMode,
            playbackSpeed = playbackSpeed,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            canExportGeneratedAudio = canExportGeneratedAudio,
            transportMode = miniPlayerModel.transportMode,
            durationMs = miniPlayerModel.durationMs,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            wavAudioInfo = wavAudioInfo,
            flashSignalInfo = flashSignalInfo,
            flashVoicingStyle = miniPlayerModel.flashVoicingStyle,
            savedAudioItem = savedAudioItem,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
            onPlaybackSpeedSelected = onPlaybackSpeedSelected,
            onExportGeneratedAudio = onExportGeneratedAudio,
            onExportGeneratedAudioToDocument = onExportGeneratedAudioToDocument,
            onOpenSavedAudioSheet = onOpenSavedAudioSheet,
            onScrubStarted = onScrubStarted,
            onScrubChanged = onScrubChanged,
            onScrubFinished = onScrubFinished,
        )
    }
}

@Composable
private fun PlayerDetailScrollContent(
    displayedSamples: Int,
    waveformDisplayedSamples: Int,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean,
    sampleRateHz: Int,
    frameSamples: Int,
    transportMode: TransportModeOption,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    flashVisualWindow: FlashVisualWindowState,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    isFlashVisualPerfOverlayEnabled: Boolean,
    playbackSpeed: Float,
    displaySectionState: PlaybackDisplaySectionState,
    savedAudioItem: SavedAudioItem?,
    showSavedAudioDecodeLoadingNotice: Boolean,
    extraLyricsRecoveryHeight: Dp,
    applyLyricsPreviewBonusLine: Boolean,
    onContainerMeasured: (PlaybackVerticalSlice) -> Unit,
    onLayoutMeasured: (PlaybackVerticalSlice) -> Unit,
    onSeekToSample: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .onGloballyPositioned { coordinates ->
                    onContainerMeasured(coordinates.toPlaybackVerticalSlice())
                }.padding(horizontal = PlayerDetailHorizontalPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (savedAudioItem != null && showSavedAudioDecodeLoadingNotice) {
            SavedAudioDecodeLoadingNotice()
        }
        AudioPlaybackDisplayBlock(
            displayedSamples = displayedSamples,
            visualDisplayedSamples = waveformDisplayedSamples,
            waveformPcm = waveformPcm,
            isWaveformPreview = isWaveformPreview,
            sampleRateHz = sampleRateHz,
            transportMode = transportMode,
            frameSamples = frameSamples,
            isFlashMode = isFlashMode,
            flashVoicingStyle = flashVoicingStyle,
            followData = followData,
            flashVisualWindow = flashVisualWindow,
            isPlaying = isPlaying,
            isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
            playbackSpeed = playbackSpeed,
            displaySectionState = displaySectionState,
            extraLyricsRecoveryHeight = extraLyricsRecoveryHeight,
            applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
            modifier =
                Modifier.onGloballyPositioned { coordinates ->
                    onLayoutMeasured(coordinates.toPlaybackVerticalSlice())
                },
            onSeekToSample = onSeekToSample,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SavedAudioDecodeLoadingNotice() {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(R.string.audio_saved_audio_decode_loading_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.audio_saved_audio_decode_loading_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayerDetailBottomDock(
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    playbackSpeed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    canExportGeneratedAudio: Boolean,
    transportMode: TransportModeOption,
    durationMs: Long,
    sampleRateHz: Int,
    frameSamples: Int,
    wavAudioInfo: WavAudioInfo,
    flashSignalInfo: FlashSignalInfo,
    flashVoicingStyle: FlashVoicingStyleOption?,
    savedAudioItem: SavedAudioItem?,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onExportGeneratedAudio: () -> Unit,
    onExportGeneratedAudioToDocument: () -> Unit,
    onOpenSavedAudioSheet: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(
                    start = PlayerDetailHorizontalPadding,
                    top = 16.dp,
                    end = PlayerDetailHorizontalPadding,
                    bottom = 8.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AudioPlaybackTimelineBlock(
            displayedSamples = displayedSamples,
            totalSamples = totalSamples,
            isScrubbing = isScrubbing,
            displayedTime = displayedTime,
            totalTime = totalTime,
            isPlaying = isPlaying,
            onScrubStarted = onScrubStarted,
            onScrubChanged = onScrubChanged,
            onScrubFinished = onScrubFinished,
        )
        AudioPlaybackTransportControls(
            isPlaying = isPlaying,
            playbackSequenceMode = playbackSequenceMode,
            playbackSpeed = playbackSpeed,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            canExportGeneratedAudio = canExportGeneratedAudio,
            transportMode = transportMode,
            durationMs = durationMs,
            totalSamples = totalSamples,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            wavAudioInfo = wavAudioInfo,
            flashSignalInfo = flashSignalInfo,
            flashVoicingStyle = flashVoicingStyle,
            savedAudioItem = savedAudioItem,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
            onPlaybackSpeedSelected = onPlaybackSpeedSelected,
            onExportGeneratedAudio = onExportGeneratedAudio,
            onExportGeneratedAudioToDocument = onExportGeneratedAudioToDocument,
            onOpenSavedAudioSheet = onOpenSavedAudioSheet,
        )
    }
}
