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
                ?: PlaybackFollowViewMode.Hex
        }
    val activeLineIndex = remember(followData.lyricLineTimeline, displayedSamples) {
        activeLineTimelineIndex(followData.lyricLineTimeline, displayedSamples)
    }
    val activeTextIndex = remember(followData.textTokenTimeline, displayedSamples) {
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
    return remember(
        followViewMode,
        activeLineIndex,
        activeTextIndex,
        activeByteIndexWithinToken,
        rawDisplayUnitsByToken,
    ) {
        PlaybackFollowPresentationState(
            followViewMode = followViewMode,
            activeLineIndex = activeLineIndex,
            activeTextIndex = activeTextIndex,
            activeByteIndexWithinToken = activeByteIndexWithinToken,
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
    val rawDisplayUnitsByToken: Map<Int, List<TextFollowRawDisplayUnitViewData>>,
)
