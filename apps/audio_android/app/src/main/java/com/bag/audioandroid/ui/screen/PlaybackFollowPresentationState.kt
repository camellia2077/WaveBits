package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData

@Composable
internal fun rememberPlaybackFollowPresentationState(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    selectedAnnotationModeName: String,
): PlaybackFollowPresentationState {
    val followViewMode =
        remember(selectedAnnotationModeName) {
            PlaybackFollowViewMode.entries.firstOrNull { it.name == selectedAnnotationModeName }
                ?: PlaybackFollowViewMode.Binary
        }
    val activeLineIndex =
        remember(followData.lyricLineTimeline, displayedSamples) {
            activeLineTimelineIndex(followData.lyricLineTimeline, displayedSamples)
        }
    val activeTextIndex =
        remember(followData.textTokenTimeline, displayedSamples) {
            activeTextTimelineIndex(followData.textTokenTimeline, displayedSamples)
        }
    val rawDisplayUnitsByToken =
        remember(
            followData.textRawDisplayUnits,
        ) {
            followData.textRawDisplayUnits.groupBy(TextFollowRawDisplayUnitViewData::tokenIndex)
        }
    val activeByteIndexWithinToken =
        remember(
            activeTextIndex,
            displayedSamples,
            rawDisplayUnitsByToken,
        ) {
            activeByteIndexWithinToken(
                activeTextIndex = activeTextIndex,
                displayedSamples = displayedSamples,
                rawDisplayUnitsByToken = rawDisplayUnitsByToken,
            )
        }
    val activeBitPositionWithinByte =
        remember(
            activeTextIndex,
            activeByteIndexWithinToken,
            displayedSamples,
            followData,
            rawDisplayUnitsByToken,
        ) {
            activeBitPositionWithinByte(
                activeTextIndex = activeTextIndex,
                activeByteIndexWithinToken = activeByteIndexWithinToken,
                displayedSamples = displayedSamples,
                followData = followData,
                rawDisplayUnitsByToken = rawDisplayUnitsByToken,
            )
        }
    return remember(
        followViewMode,
        activeLineIndex,
        activeTextIndex,
        activeByteIndexWithinToken,
        activeBitPositionWithinByte,
        rawDisplayUnitsByToken,
    ) {
        PlaybackFollowPresentationState(
            followViewMode = followViewMode,
            activeLineIndex = activeLineIndex,
            activeTextIndex = activeTextIndex,
            activeByteIndexWithinToken = activeByteIndexWithinToken,
            activeBitIndexWithinByte = activeBitPositionWithinByte.bitIndexWithinByte,
            isActiveBitTone = activeBitPositionWithinByte.isToneActive,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    }
}

@Immutable
internal data class PlaybackFollowPresentationState(
    val followViewMode: PlaybackFollowViewMode,
    val activeLineIndex: Int,
    val activeTextIndex: Int,
    val activeByteIndexWithinToken: Int,
    val activeBitIndexWithinByte: Int,
    val isActiveBitTone: Boolean,
    val rawDisplayUnitsByToken: Map<Int, List<TextFollowRawDisplayUnitViewData>>,
)
