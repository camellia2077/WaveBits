package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
fun AudioTabScreen(
    selectedThemeStyle: ThemeStyleOption,
    transportMode: TransportModeOption,
    isCodecBusy: Boolean,
    encodeProgress: Float?,
    encodePhase: AudioEncodePhase?,
    isEncodeCancelling: Boolean,
    onTransportModeSelected: (TransportModeOption) -> Unit,
    isFlashVoicingEnabled: Boolean,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
    selectedMorseSpeed: MorseSpeedOption,
    onMorseSpeedSelected: (MorseSpeedOption) -> Unit,
    inputText: String,
    inputPlaceholderText: String,
    onInputTextChange: (String) -> Unit,
    onRandomizeSampleInput: (SampleInputLengthOption) -> Unit,
    decodedPayload: DecodedPayloadViewData,
    onEncode: () -> Unit,
    onCancelEncode: () -> Unit,
    onDecode: () -> Unit,
    onClear: () -> Unit,
    onClearResult: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val isEncodingBusy = isCodecBusy && encodeProgress != null
    val isDecodingBusy = isCodecBusy && !isEncodingBusy
    var showInputEditor by rememberSaveable { mutableStateOf(false) }
    var inputCardExpanded by rememberSaveable(transportMode) { mutableStateOf(true) }
    var resultExpanded by rememberSaveable(transportMode) { mutableStateOf(true) }
    var resultContentExpanded by rememberSaveable(transportMode) { mutableStateOf(true) }
    var sampleInputLength by rememberSaveable { mutableStateOf(SampleInputLengthOption.Short) }

    if (showInputEditor) {
        AudioInputEditorDialog(
            selectedThemeStyle = selectedThemeStyle,
            transportMode = transportMode,
            inputText = inputText,
            placeholderText = inputPlaceholderText,
            sampleInputLength = sampleInputLength,
            randomizeEnabled = !isCodecBusy,
            onInputTextChange = onInputTextChange,
            onRandomizeSampleInput = onRandomizeSampleInput,
            onDismiss = { showInputEditor = false },
        )
    }

    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                ),
    ) {
        // Treat transport mode as the Audio page's primary navigation, not as just another
        // card inside the scroll content. Keeping it in a dedicated top bar makes flash/pro/ultra
        // always reachable while the rest of the page scrolls underneath.
        AudioModeSwitcherBar(
            transportMode = transportMode,
            onTransportModeSelected = onTransportModeSelected,
            enabled = !isCodecBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = AudioModeSwitcherBarReservedHeight)
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AudioInputActionsCard(
                selectedThemeStyle = selectedThemeStyle,
                transportMode = transportMode,
                isCodecBusy = isCodecBusy,
                encodeProgress = encodeProgress,
                encodePhase = encodePhase,
                isEncodeCancelling = isEncodeCancelling,
                isFlashVoicingEnabled = isFlashVoicingEnabled,
                selectedFlashVoicingStyle = selectedFlashVoicingStyle,
                onFlashVoicingStyleSelected = onFlashVoicingStyleSelected,
                selectedMorseSpeed = selectedMorseSpeed,
                onMorseSpeedSelected = onMorseSpeedSelected,
                inputCardExpanded = inputCardExpanded,
                onToggleInputCardExpanded = { inputCardExpanded = !inputCardExpanded },
                inputText = inputText,
                inputPlaceholderText = inputPlaceholderText,
                onInputTextChange = onInputTextChange,
                onOpenInputEditor = { showInputEditor = true },
                sampleInputLength = sampleInputLength,
                onSampleInputLengthSelected = { length ->
                    if (sampleInputLength != length) {
                        sampleInputLength = length
                        onRandomizeSampleInput(length)
                    }
                },
                onRandomizeSampleInput = { onRandomizeSampleInput(sampleInputLength) },
                onClearInput = onClear,
                onEncode = onEncode,
                onCancelEncode = onCancelEncode,
            )
            AudioResultCard(
                decodedPayload = decodedPayload,
                transportMode = transportMode,
                isCodecBusy = isCodecBusy,
                isDecodeBusy = isDecodingBusy,
                expanded = resultExpanded,
                onToggleExpanded = { resultExpanded = !resultExpanded },
                resultContentExpanded = resultContentExpanded,
                onToggleResultContentExpanded = { resultContentExpanded = !resultContentExpanded },
                onDecode = onDecode,
                onClearResult = onClearResult,
            )

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(contentPadding.calculateBottomPadding()),
            )
        }
        AudioModeSwitcherBar(
            transportMode = transportMode,
            onTransportModeSelected = onTransportModeSelected,
            enabled = !isCodecBusy,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val AudioModeSwitcherBarReservedHeight = 60.dp
