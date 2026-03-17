package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
fun AudioTabScreen(
    transportMode: TransportModeOption,
    isCodecBusy: Boolean,
    encodeProgress: Float?,
    encodePhase: AudioEncodePhase?,
    isEncodeCancelling: Boolean,
    onTransportModeSelected: (TransportModeOption) -> Unit,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onRandomizeSampleInput: () -> Unit,
    resultText: String,
    onEncode: () -> Unit,
    onCancelEncode: () -> Unit,
    onDecode: () -> Unit,
    onClear: () -> Unit,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isEncodingBusy = isCodecBusy && encodeProgress != null
    val isDecodingBusy = isCodecBusy && !isEncodingBusy
    var flashVoicingExpanded by rememberSaveable(transportMode) {
        mutableStateOf(transportMode == TransportModeOption.Flash)
    }
    var resultExpanded by rememberSaveable(transportMode) { mutableStateOf(true) }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.audio_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        AudioModeSwitcher(
            transportMode = transportMode,
            onTransportModeSelected = onTransportModeSelected,
            enabled = !isCodecBusy,
            modifier = Modifier.fillMaxWidth()
        )
        AudioInputActionsCard(
            transportMode = transportMode,
            isCodecBusy = isCodecBusy,
            encodeProgress = encodeProgress,
            encodePhase = encodePhase,
            isEncodeCancelling = isEncodeCancelling,
            selectedFlashVoicingStyle = selectedFlashVoicingStyle,
            onFlashVoicingStyleSelected = onFlashVoicingStyleSelected,
            inputText = inputText,
            onInputTextChange = onInputTextChange,
            onRandomizeSampleInput = onRandomizeSampleInput,
            onEncode = onEncode,
            onCancelEncode = onCancelEncode,
            flashVoicingExpanded = flashVoicingExpanded,
            onToggleFlashVoicingExpanded = { flashVoicingExpanded = !flashVoicingExpanded }
        )
        AudioResultCard(
            resultText = resultText,
            isCodecBusy = isCodecBusy,
            isDecodeBusy = isDecodingBusy,
            expanded = resultExpanded,
            onToggleExpanded = { resultExpanded = !resultExpanded },
            onDecode = onDecode,
            onClearInput = onClear,
            onClearResult = onClearResult
        )
    }
}
