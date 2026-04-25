package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData

@Composable
internal fun PlaybackFollowLineStrip(
    followData: PayloadFollowViewData,
    lineTokenRange: TextFollowLineTokenRangeViewData,
    presentationState: PlaybackFollowPresentationState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Slice the tokens that belong to this line
    val tokenStartIndex = lineTokenRange.tokenBeginIndex
    val tokenCount = lineTokenRange.tokenCount

    // Determine the relative index within this line for the active token
    val activeRelativeIndex =
        if (presentationState.activeTextIndex in tokenStartIndex until tokenStartIndex + tokenCount) {
            presentationState.activeTextIndex - tokenStartIndex
        } else {
            -1
        }

    // Scroll horizontally if the active token in this line changes
    LaunchedEffect(activeRelativeIndex) {
        if (activeRelativeIndex >= 0) {
            listState.animateScrollToItem(activeRelativeIndex)
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("follow-line-strip"),
    ) {
        items(tokenCount) { relativeIndex ->
            val absoluteTokenIndex = tokenStartIndex + relativeIndex
            val token = followData.textTokens.getOrNull(absoluteTokenIndex).orEmpty()

            PlaybackFollowTokenCard(
                token = token,
                rawDisplayUnits = presentationState.rawDisplayUnitsByToken[absoluteTokenIndex].orEmpty(),
                annotationMode = presentationState.followViewMode,
                isActive = absoluteTokenIndex == presentationState.activeTextIndex,
                activeByteIndexWithinToken =
                    if (absoluteTokenIndex == presentationState.activeTextIndex) {
                        presentationState.activeByteIndexWithinToken
                    } else {
                        -1
                    },
            )
        }
    }
}
