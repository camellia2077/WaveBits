package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

@Composable
internal fun PlaybackDataFollowSection(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    transportMode: TransportModeOption?,
    modifier: Modifier = Modifier,
    initialAnnotationMode: PlaybackFollowViewMode = PlaybackFollowViewMode.Hex,
    onSeekToSample: (Int) -> Unit = {},
) {
    var selectedAnnotationMode by rememberSaveable { mutableStateOf(initialAnnotationMode.name) }
    var isTokenizerOpen by rememberSaveable { mutableStateOf(false) }
    val isMorseMode = transportMode == TransportModeOption.Mini
    val normalizedAnnotationModeName =
        if (isMorseMode) {
            PlaybackFollowViewMode.Morse.name
        } else {
            selectedAnnotationMode.takeUnless { it == PlaybackFollowViewMode.Morse.name }
                ?: PlaybackFollowViewMode.Hex.name
        }
    val presentationState =
        rememberPlaybackFollowPresentationState(
            followData = followData,
            displayedSamples = displayedSamples,
            selectedAnnotationModeName = normalizedAnnotationModeName,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-follow-section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!followData.followAvailable) {
            Text(
                text = stringResource(R.string.audio_follow_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        if (!followData.textFollowAvailable || followData.textTokens.isEmpty()) {
            Text(
                text = stringResource(R.string.audio_follow_text_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackFollowAnnotationModeSwitcher(
                selectedMode = presentationState.followViewMode,
                onModeSelected = { selectedAnnotationMode = it.name },
                transportMode = transportMode,
                modifier = Modifier.weight(1f),
            )
            val tokenizerDescription = stringResource(R.string.audio_action_open_tokenizer)
            IconButton(
                onClick = { isTokenizerOpen = true },
                modifier =
                    Modifier
                        .semantics { contentDescription = tokenizerDescription }
                        .testTag("follow-tokenizer-button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = null,
                )
            }
        }
        PlaybackFollowTokenStrip(
            followData = followData,
            presentationState = presentationState,
        )
        if (isTokenizerOpen) {
            PlaybackFollowTokenizerSheet(
                followData = followData,
                presentationState = presentationState,
                onSeekToSample = onSeekToSample,
                onDismiss = { isTokenizerOpen = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackFollowTokenizerSheet(
    followData: PayloadFollowViewData,
    presentationState: PlaybackFollowPresentationState,
    onSeekToSample: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val visualTokens = appThemeVisualTokens()
    val tokenStartSamples =
        remember(followData.textTokenTimeline) {
            followData.textTokenTimeline
                .groupBy { it.tokenIndex }
                .mapValues { (_, entries) -> entries.minOfOrNull { it.startSample } }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = visualTokens.modalContainerColor,
        contentColor = visualTokens.modalContentColor,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("follow-tokenizer-sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.audio_follow_tokenizer_title),
                style = MaterialTheme.typography.titleMedium,
            )
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(followData.textTokens) { index, token ->
                    val tokenStartSample = tokenStartSamples[index]?.takeIf { it >= 0 }
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag("follow-tokenizer-item-$index")
                                .then(
                                    if (tokenStartSample != null) {
                                        Modifier.clickable { onSeekToSample(tokenStartSample) }
                                    } else {
                                        Modifier
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlaybackFollowTokenCard(
                            token = token,
                            rawDisplayUnits = presentationState.rawDisplayUnitsByToken[index].orEmpty(),
                            annotationMode = presentationState.followViewMode,
                            isActive = index == presentationState.activeTextIndex,
                            activeByteIndexWithinToken =
                                if (index == presentationState.activeTextIndex) {
                                    presentationState.activeByteIndexWithinToken
                                } else {
                                    -1
                                },
                            activeBitIndexWithinByte =
                                if (index == presentationState.activeTextIndex) {
                                    presentationState.activeBitIndexWithinByte
                                } else {
                                    -1
                                },
                            isActiveBitTone =
                                index == presentationState.activeTextIndex &&
                                    presentationState.isActiveBitTone,
                            isPast = index < presentationState.activeTextIndex,
                            onClick =
                                tokenStartSample?.let { startSample ->
                                    { onSeekToSample(startSample) }
                                },
                            testTag = "follow-tokenizer-card-$index",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
