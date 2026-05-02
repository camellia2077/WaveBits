package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PlaybackFollowTokenStrip(
    followData: PayloadFollowViewData,
    presentationState: PlaybackFollowPresentationState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var autoFollowPaused by remember { mutableStateOf(false) }
    var resumeAutoFollowJob by remember { mutableStateOf<Job?>(null) }

    fun pauseAutoFollowBriefly() {
        autoFollowPaused = true
        resumeAutoFollowJob?.cancel()
        resumeAutoFollowJob =
            scope.launch {
                delay(TokenStripAutoFollowResumeDelayMs)
                autoFollowPaused = false
            }
    }
    DisposableEffect(Unit) {
        onDispose {
            resumeAutoFollowJob?.cancel()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth
        val density = androidx.compose.ui.platform.LocalDensity.current
        val centeredItemWidth =
            with(density) {
                (maxWidthPx * PlaybackFollowTokenCenterWidthFraction)
                    .toDp()
                    .coerceIn(PlaybackFollowTokenMinimumWidth, PlaybackFollowTokenMaximumWidth)
            }
        val centerEdgePadding =
            with(density) {
                ((maxWidthPx - centeredItemWidth.toPx()) / 2f)
                    .coerceAtLeast(PlaybackFollowTokenMinimumEdgePadding.toPx())
                    .toDp()
            }

        LaunchedEffect(presentationState.activeTextIndex, maxWidthPx, autoFollowPaused) {
            if (!autoFollowPaused && presentationState.activeTextIndex >= 0 && maxWidthPx > 0) {
                listState.animateScrollToItem(presentationState.activeTextIndex)
            }
        }
        val stripModifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 188.dp, max = 240.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        pauseAutoFollowBriefly()
                        scope.launch { listState.stopScroll() }
                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        } while (event.changes.any { it.pressed })
                        pauseAutoFollowBriefly()
                    }
                }.testTag("follow-token-strip")

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = centerEdgePadding),
            modifier = stripModifier,
        ) {
            itemsIndexed(followData.textTokens) { index, token ->
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
                    modifier = Modifier.animateItem().width(centeredItemWidth),
                )
            }
        }
    }
}

private const val PlaybackFollowTokenCenterWidthFraction = 0.82f
private val PlaybackFollowTokenMinimumWidth = 92.dp
private val PlaybackFollowTokenMaximumWidth = 360.dp
private val PlaybackFollowTokenMinimumEdgePadding = 24.dp
private const val TokenStripAutoFollowResumeDelayMs = 650L
