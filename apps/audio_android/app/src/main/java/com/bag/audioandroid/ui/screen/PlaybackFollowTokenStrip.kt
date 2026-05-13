package com.bag.audioandroid.ui.screen

import android.util.Log
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.TransportModeOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PlaybackFollowTokenStrip(
    followData: PayloadFollowViewData,
    presentationState: PlaybackFollowPresentationState,
    transportMode: TransportModeOption?,
    onMeasuredHeightDpChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var autoFollowPaused by remember { mutableStateOf(false) }
    var resumeAutoFollowJob by remember { mutableStateOf<Job?>(null) }
    var measuredStripHeightPx by remember { mutableStateOf(0) }
    var measuredActiveCardHeightPx by remember { mutableStateOf(0) }

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
        val minStripHeight =
            if (transportMode == TransportModeOption.Mini) {
                // Mini token pages render morse chips rather than tall binary/hex blocks.
                // Use a smaller minimum strip height so the compact lyrics preview can
                // reclaim the token-strip's internal spare space without touching the dock.
                148.dp
            } else {
                188.dp
            }
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
                .heightIn(min = minStripHeight, max = 320.dp)
                .onSizeChanged { size ->
                    val heightPx = size.height
                    val widthPx = size.width
                    measuredStripHeightPx = heightPx
                    val heightDp = with(density) { heightPx.toDp().value }
                    val widthDp = with(density) { widthPx.toDp().value }
                    onMeasuredHeightDpChanged(heightDp)
                    Log.d(
                        "PlaybackLyricsLayout",
                        "mode=lyrics surface=token-strip widthPx=$widthPx widthDp=${"%.1f".format(widthDp)} " +
                            "heightPx=$heightPx heightDp=${"%.1f".format(heightDp)} tokenCount=${followData.textTokens.size}",
                    )
                }.pointerInput(Unit) {
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
                    onMeasuredHeightPxChanged =
                        if (index == presentationState.activeTextIndex) {
                            { heightPx ->
                                measuredActiveCardHeightPx = heightPx
                                if (measuredStripHeightPx > 0) {
                                    val stripHeightDp = with(density) { measuredStripHeightPx.toDp().value }
                                    val activeCardHeightDp = with(density) { heightPx.toDp().value }
                                    val spareHeightDp = with(density) { (measuredStripHeightPx - heightPx).coerceAtLeast(0).toDp().value }
                                    Log.d(
                                        "PlaybackTokenStripLayout",
                                        "mode=lyrics stripHeightDp=${"%.1f".format(stripHeightDp)} " +
                                            "activeCardHeightDp=${"%.1f".format(activeCardHeightDp)} " +
                                            "stripSpareHeightDp=${"%.1f".format(spareHeightDp)} " +
                                            "activeIndex=${presentationState.activeTextIndex}",
                                    )
                                }
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}

private const val PlaybackFollowTokenCenterWidthFraction = 1.0f
private val PlaybackFollowTokenMinimumWidth = 92.dp
private val PlaybackFollowTokenMaximumWidth = 420.dp
private val PlaybackFollowTokenMinimumEdgePadding = 0.dp
private const val TokenStripAutoFollowResumeDelayMs = 650L
