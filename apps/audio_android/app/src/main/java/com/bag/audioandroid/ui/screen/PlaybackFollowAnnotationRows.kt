package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData

@Composable
internal fun PlaybackFollowAnnotationRows(
    token: String,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    annotationMode: PlaybackFollowViewMode,
    annotationByteGroups: List<String>,
    characterDisplayUnits: List<CharacterDisplayUnit>,
    isActive: Boolean,
    activeByteIndexWithinToken: Int,
    activeBitIndexWithinByte: Int,
    isActiveBitTone: Boolean,
    inactiveRawColor: Color,
    focusColor: Color,
    onFocusColor: Color,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val byteGroupsPerRow = annotationByteGroupsPerRow(annotationMode, maxWidth.value)
        val maxVisibleRows = annotationMaxVisibleRows(annotationMode)
        var previousWindowStartIndex by
            rememberSaveable(
                annotationMode,
                annotationByteGroups,
                byteGroupsPerRow,
                maxVisibleRows,
            ) {
                mutableIntStateOf(0)
            }
        val visibleWindow =
            remember(
                annotationByteGroups,
                byteGroupsPerRow,
                maxVisibleRows,
                activeByteIndexWithinToken,
                isActive,
                previousWindowStartIndex,
            ) {
                resolveAnnotationWindow(
                    annotationByteGroups = annotationByteGroups,
                    byteGroupsPerRow = byteGroupsPerRow,
                    maxVisibleRows = maxVisibleRows,
                    activeByteIndexWithinToken = activeByteIndexWithinToken,
                    centerActiveGroup = isActive,
                    previousStartIndex = previousWindowStartIndex,
                )
            }
        SideEffect {
            if (visibleWindow.startIndex != previousWindowStartIndex) {
                previousWindowStartIndex = visibleWindow.startIndex
            }
        }
        val boundaryByteIndexes =
            remember(token, characterDisplayUnits) {
                annotationCharacterBoundaryByteIndexes(token, characterDisplayUnits)
            }
        when (annotationMode) {
            PlaybackFollowViewMode.Binary -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    visibleWindow.groups.chunked(byteGroupsPerRow).forEachIndexed { rowIndex, rowGroups ->
                        PlaybackByteGroupRow {
                            val isFirstRow = rowIndex == 0
                            val isLastRow = rowIndex == visibleWindow.groups.chunked(byteGroupsPerRow).lastIndex
                            if (isFirstRow && visibleWindow.hasLeadingOverflow) {
                                AnnotationOverflowIndicator(color = inactiveRawColor)
                            }
                            rowGroups.forEachIndexed { groupIndexInRow, group ->
                                val byteIndex = visibleWindow.startIndex + rowIndex * byteGroupsPerRow + groupIndexInRow
                                PlaybackByteBlock(
                                    group = group,
                                    mode = annotationMode,
                                    isActive = isActive && byteIndex == activeByteIndexWithinToken,
                                    isPast = isActive && byteIndex < activeByteIndexWithinToken,
                                    activeBitIndex =
                                        if (isActive &&
                                            byteIndex == activeByteIndexWithinToken
                                        ) {
                                            activeBitIndexWithinByte
                                        } else {
                                            -1
                                        },
                                    isActiveBitTone = isActiveBitTone,
                                    focusColor = focusColor,
                                    onFocusColor = onFocusColor,
                                    inactiveColor = inactiveRawColor,
                                )
                                PlaybackCharacterBoundaryDivider(
                                    visible = byteIndex + 1 in boundaryByteIndexes,
                                    color = inactiveRawColor,
                                )
                            }
                            if (isLastRow && visibleWindow.hasTrailingOverflow) {
                                AnnotationOverflowIndicator(color = inactiveRawColor)
                            }
                        }
                    }
                }
            }

            PlaybackFollowViewMode.Hex -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    visibleWindow.groups.chunked(byteGroupsPerRow).forEachIndexed { rowIndex, rowGroups ->
                        PlaybackByteGroupRow {
                            val isFirstRow = rowIndex == 0
                            val isLastRow = rowIndex == visibleWindow.groups.chunked(byteGroupsPerRow).lastIndex
                            if (isFirstRow && visibleWindow.hasLeadingOverflow) {
                                AnnotationOverflowIndicator(color = inactiveRawColor)
                            }
                            rowGroups.forEachIndexed { groupIndexInRow, group ->
                                val byteIndex = visibleWindow.startIndex + rowIndex * byteGroupsPerRow + groupIndexInRow
                                val isActiveByte = isActive && byteIndex == activeByteIndexWithinToken
                                PlaybackHexByteBlock(
                                    group = group,
                                    isActive = isActiveByte,
                                    isPast = isActive && byteIndex < activeByteIndexWithinToken,
                                    activeBitIndex = if (isActiveByte) activeBitIndexWithinByte else -1,
                                    isActiveBitTone = isActiveBitTone,
                                    focusColor = focusColor,
                                    onFocusColor = onFocusColor,
                                    inactiveColor = inactiveRawColor,
                                )
                                PlaybackCharacterBoundaryDivider(
                                    visible = byteIndex + 1 in boundaryByteIndexes,
                                    color = inactiveRawColor,
                                )
                            }
                            if (isLastRow && visibleWindow.hasTrailingOverflow) {
                                AnnotationOverflowIndicator(color = inactiveRawColor)
                            }
                        }
                    }
                }
            }

            PlaybackFollowViewMode.Morse -> {
                val morseLetterGroups =
                    remember(token, rawDisplayUnits, annotationByteGroups) {
                        morseLetterDisplayGroups(
                            token = token,
                            characterDisplayUnits = characterDisplayUnits,
                            rawDisplayUnits = rawDisplayUnits,
                            annotationByteGroups = annotationByteGroups,
                        )
                    }
                val dividerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.Top,
                    ) {
                        morseLetterGroups.forEachIndexed { index, group ->
                            if (index > 0) {
                                Box(
                                    modifier =
                                        Modifier
                                            .height(MorseLetterDividerHeight)
                                            .width(1.dp)
                                            .background(dividerColor),
                                )
                            }
                            val isActiveByte =
                                isActive &&
                                    activeByteIndexWithinToken >= group.byteStartIndexWithinToken &&
                                    activeByteIndexWithinToken <
                                    group.byteStartIndexWithinToken + group.byteCount
                            MorseLetterBlock(
                                letter = group.text,
                                morse = group.morse,
                                isActive = isActiveByte,
                                isPast =
                                    isActive &&
                                        group.byteStartIndexWithinToken + group.byteCount - 1 <
                                        activeByteIndexWithinToken,
                                activeBitIndex = if (isActiveByte) activeBitIndexWithinByte else -1,
                                isActiveBitTone = isActiveBitTone,
                                focusColor = focusColor,
                                onFocusColor = onFocusColor,
                                inactiveColor = inactiveRawColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackByteGroupRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun PlaybackCharacterBoundaryDivider(
    visible: Boolean,
    color: Color,
) {
    if (visible) {
        Box(
            modifier =
                Modifier
                    .height(CharacterBoundaryDividerHeight)
                    .width(1.dp)
                    .background(color.copy(alpha = CharacterBoundaryDividerAlpha)),
        )
    }
}

@Composable
private fun AnnotationOverflowIndicator(color: Color) {
    Text(
        text = "...",
        color = color.copy(alpha = 0.72f),
        style =
            MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
            ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
    )
}

private val MorseLetterDividerHeight = 34.dp
private val CharacterBoundaryDividerHeight = 28.dp
private const val CharacterBoundaryDividerAlpha = 0.44f

internal fun annotationByteGroupsPerRow(
    mode: PlaybackFollowViewMode,
    availableWidthDp: Float,
): Int =
    when (mode) {
        PlaybackFollowViewMode.Hex ->
            when {
                availableWidthDp >= HexWideRowMinWidthDp -> 8
                availableWidthDp >= HexMediumRowMinWidthDp -> 6
                else -> 4
            }

        PlaybackFollowViewMode.Binary ->
            if (availableWidthDp >= BinaryWideRowMinWidthDp) {
                4
            } else {
                3
            }

        PlaybackFollowViewMode.Morse -> 1
    }

private const val HexMediumRowMinWidthDp = 288f
private const val HexWideRowMinWidthDp = 320f
private const val BinaryWideRowMinWidthDp = 320f

internal fun annotationMaxVisibleRows(mode: PlaybackFollowViewMode): Int =
    when (mode) {
        PlaybackFollowViewMode.Hex -> 3
        PlaybackFollowViewMode.Binary -> 4
        PlaybackFollowViewMode.Morse -> Int.MAX_VALUE
    }

internal data class AnnotationWindow(
    val startIndex: Int,
    val groups: List<String>,
    val hasLeadingOverflow: Boolean,
    val hasTrailingOverflow: Boolean,
)

internal fun resolveAnnotationWindow(
    annotationByteGroups: List<String>,
    byteGroupsPerRow: Int,
    maxVisibleRows: Int,
    activeByteIndexWithinToken: Int,
    centerActiveGroup: Boolean,
    previousStartIndex: Int = 0,
): AnnotationWindow {
    val capacity = (byteGroupsPerRow * maxVisibleRows).coerceAtLeast(1)
    if (annotationByteGroups.size <= capacity) {
        return AnnotationWindow(
            startIndex = 0,
            groups = annotationByteGroups,
            hasLeadingOverflow = false,
            hasTrailingOverflow = false,
        )
    }

    val lastPossibleStart = (annotationByteGroups.size - capacity).coerceAtLeast(0)
    val resolvedStart =
        if (centerActiveGroup && activeByteIndexWithinToken >= 0) {
            resolveWindowStartIndex(
                activeIndex = activeByteIndexWithinToken,
                previousStartIndex = previousStartIndex.coerceIn(0, lastPossibleStart),
                capacity = capacity,
                lastPossibleStart = lastPossibleStart,
            )
        } else {
            0
        }
    val endExclusive = (resolvedStart + capacity).coerceAtMost(annotationByteGroups.size)
    return AnnotationWindow(
        startIndex = resolvedStart,
        groups = annotationByteGroups.subList(resolvedStart, endExclusive),
        hasLeadingOverflow = resolvedStart > 0,
        hasTrailingOverflow = endExclusive < annotationByteGroups.size,
    )
}

internal fun resolveWindowStartIndex(
    activeIndex: Int,
    previousStartIndex: Int,
    capacity: Int,
    lastPossibleStart: Int,
): Int {
    val clampedPreviousStart = previousStartIndex.coerceIn(0, lastPossibleStart)
    if (activeIndex < 0) {
        return clampedPreviousStart
    }
    val comfortMargin = (capacity / 4).coerceAtLeast(2)
    val previousEndExclusive = clampedPreviousStart + capacity
    val safeStart = clampedPreviousStart + comfortMargin
    val safeEndExclusive = previousEndExclusive - comfortMargin
    if (activeIndex in safeStart until safeEndExclusive) {
        return clampedPreviousStart
    }
    val targetOffset = (capacity * 2) / 5
    return (activeIndex - targetOffset).coerceIn(0, lastPossibleStart)
}
