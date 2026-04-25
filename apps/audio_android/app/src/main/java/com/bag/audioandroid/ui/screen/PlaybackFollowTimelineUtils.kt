package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry

internal fun annotationByteGroupsForMode(
    mode: PlaybackFollowViewMode,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
): List<String> =
    rawDisplayUnits.map { unit ->
        when (mode) {
            PlaybackFollowViewMode.Hex -> unit.hexText
            PlaybackFollowViewMode.Binary -> unit.binaryText
        }
    }

internal fun activeTextTimelineIndex(
    entries: List<TextFollowTimelineEntry>,
    displayedSamples: Int,
): Int =
    entries.indexOfLast { entry ->
        displayedSamples >= entry.startSample &&
            displayedSamples < entry.startSample + entry.sampleCount
    }

internal fun activeLineTimelineIndex(
    entries: List<TextFollowLyricLineTimelineEntry>,
    displayedSamples: Int,
): Int =
    entries.indexOfLast { entry ->
        displayedSamples >= entry.startSample &&
            displayedSamples < entry.startSample + entry.sampleCount
    }

internal fun activeByteIndexWithinToken(
    activeTextIndex: Int,
    displayedSamples: Int,
    rawDisplayUnitsByToken: Map<Int, List<TextFollowRawDisplayUnitViewData>>,
): Int {
    if (activeTextIndex < 0) {
        return -1
    }
    val rawDisplayUnits = rawDisplayUnitsByToken[activeTextIndex].orEmpty()
    val activeByte =
        rawDisplayUnits.firstOrNull { entry ->
            displayedSamples >= entry.startSample &&
                displayedSamples < entry.startSample + entry.sampleCount
        } ?: return -1
    return activeByte.byteIndexWithinToken
}
