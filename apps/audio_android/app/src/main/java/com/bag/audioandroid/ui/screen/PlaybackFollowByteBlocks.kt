package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun MorseLetterBlock(
    letter: String,
    morse: String,
    isActive: Boolean,
    isPast: Boolean,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
) {
    val letterColor =
        when {
            isActive -> onFocusColor
            isPast -> focusColor
            else -> inactiveColor
        }
    val letterBackground = if (isActive) focusColor else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text =
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = letterColor,
                            background = letterBackground,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        ),
                    ) {
                        append(letter)
                    }
                },
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
        PlaybackByteBlock(
            group = morse,
            mode = PlaybackFollowViewMode.Morse,
            isActive = isActive,
            isPast = isPast,
            activeBitIndex = activeBitIndex,
            isActiveBitTone = isActiveBitTone,
            focusColor = focusColor,
            onFocusColor = onFocusColor,
            inactiveColor = inactiveColor,
        )
    }
}

@Composable
internal fun PlaybackHexByteBlock(
    group: String,
    isActive: Boolean,
    isPast: Boolean,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
) {
    val nibbles = remember(group) { hexNibbleGroups(group) }
    val currentNibbleIndex =
        if (isActive && activeBitIndex >= 4 && nibbles.size > 1) {
            1
        } else {
            0
        }.coerceAtMost(nibbles.lastIndex.coerceAtLeast(0))
    val currentNibble = nibbles.getOrNull(currentNibbleIndex) ?: HexNibbleGroup(hex = group, binary = "0000")
    val activeBitIndexWithinNibble =
        if (isActive && activeBitIndex >= 0) {
            activeBitIndex % 4
        } else {
            -1
        }
    PlaybackHexNibbleBlock(
        hexText = if (isActive) currentNibble.hex else group.uppercase(),
        binaryText = currentNibble.binary,
        isActive = isActive,
        isPast = isPast,
        activeBitIndex = activeBitIndexWithinNibble,
        isActiveBitTone = isActiveBitTone,
        focusColor = focusColor,
        onFocusColor = onFocusColor,
        inactiveColor = inactiveColor,
    )
}

@Composable
private fun PlaybackHexNibbleBlock(
    hexText: String,
    binaryText: String,
    isActive: Boolean,
    isPast: Boolean,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
) {
    val hexColor =
        when {
            isActive -> onFocusColor
            isPast -> focusColor
            else -> inactiveColor
        }
    val hexBackground = if (isActive) focusColor else Color.Transparent
    val hexWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
    val annotatedBinaryText =
        buildAnnotatedString {
            binaryText.forEachIndexed { bitIndex, bitChar ->
                val isCurrentBit = isActive && isActiveBitTone && bitIndex == activeBitIndex
                val isHistoryBit =
                    isPast ||
                        (
                            isActive &&
                                activeBitIndex >= 0 &&
                                (
                                    if (isActiveBitTone) {
                                        bitIndex < activeBitIndex
                                    } else {
                                        bitIndex <= activeBitIndex
                                    }
                                )
                        )
                val (textColor, backgroundColor, weight) =
                    when {
                        isCurrentBit -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                        isHistoryBit -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                        else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                    }
                withStyle(SpanStyle(color = textColor, background = backgroundColor, fontWeight = weight)) {
                    append(bitChar)
                }
            }
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text =
                buildAnnotatedString {
                    withStyle(SpanStyle(color = hexColor, background = hexBackground, fontWeight = hexWeight)) {
                        append(hexText)
                    }
                },
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = annotatedBinaryText,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun PlaybackByteBlock(
    group: String,
    mode: PlaybackFollowViewMode,
    isActive: Boolean,
    isPast: Boolean,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
) {
    val text =
        buildAnnotatedString {
            if (isActive) {
                when (mode) {
                    PlaybackFollowViewMode.Binary -> {
                        group.forEachIndexed { bitIndex, bitChar ->
                            val isCurrentBit = isActiveBitTone && bitIndex == activeBitIndex
                            val isHistoryBit =
                                if (isActiveBitTone) {
                                    bitIndex < activeBitIndex
                                } else {
                                    bitIndex <= activeBitIndex
                                }

                            val (textColor, bgColor, weight) =
                                when {
                                    isCurrentBit -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                                    isHistoryBit -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                                    else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                                }

                            withStyle(SpanStyle(color = textColor, background = bgColor, fontWeight = weight)) {
                                append(bitChar)
                            }
                        }
                    }
                    PlaybackFollowViewMode.Hex -> {
                        val currentNibbleIndex = if (activeBitIndex >= 0) activeBitIndex / 4 else -1
                        group.forEachIndexed { nibbleIndex, hexChar ->
                            val isCurrentNibble = isActiveBitTone && nibbleIndex == currentNibbleIndex
                            val isHistoryNibble =
                                if (isActiveBitTone) {
                                    nibbleIndex < currentNibbleIndex
                                } else {
                                    nibbleIndex <= currentNibbleIndex
                                }

                            val (textColor, bgColor, weight) =
                                when {
                                    isCurrentNibble -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                                    isHistoryNibble -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                                    else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                                }

                            withStyle(SpanStyle(color = textColor, background = bgColor, fontWeight = weight)) {
                                append(hexChar)
                            }
                        }
                    }
                    PlaybackFollowViewMode.Morse -> {
                        group.forEachIndexed { elementIndex, elementChar ->
                            val isCurrentElement = isActiveBitTone && elementIndex == activeBitIndex
                            val isHistoryElement =
                                if (isActiveBitTone) {
                                    elementIndex < activeBitIndex
                                } else {
                                    elementIndex <= activeBitIndex
                                }

                            val (textColor, bgColor, weight) =
                                when {
                                    isCurrentElement -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                                    isHistoryElement -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                                    else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                                }

                            withStyle(SpanStyle(color = textColor, background = bgColor, fontWeight = weight)) {
                                append(elementChar)
                            }
                        }
                    }
                }
            } else {
                withStyle(
                    SpanStyle(
                        color = if (isPast) focusColor else inactiveColor,
                        fontWeight = FontWeight.Medium,
                    ),
                ) {
                    append(group)
                }
            }
        }

    Text(
        text = text,
        style =
            MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
            ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
    )
}
