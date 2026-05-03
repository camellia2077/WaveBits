package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                annotationByteGroups.chunked(BinaryByteGroupsPerRow).forEachIndexed { rowIndex, rowGroups ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowGroups.forEachIndexed { groupIndexInRow, group ->
                            val byteIndex = rowIndex * BinaryByteGroupsPerRow + groupIndexInRow
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
                annotationByteGroups.chunked(HexByteGroupsPerRow).forEachIndexed { rowIndex, rowGroups ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowGroups.forEachIndexed { groupIndexInRow, group ->
                            val byteIndex = rowIndex * HexByteGroupsPerRow + groupIndexInRow
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

private val MorseLetterDividerHeight = 34.dp
private const val BinaryByteGroupsPerRow = 3
private const val HexByteGroupsPerRow = 4
