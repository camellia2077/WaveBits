package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val nibbles = hexNibbleGroups(group)
    val currentNibbleIndex =
        if (isActive && activeBitIndex >= 4 && nibbles.size > 1) {
            1
        } else {
            0
        }.coerceAtMost(nibbles.lastIndex.coerceAtLeast(0))
    val binaryText = nibbles.joinToString(separator = "") { it.binary }
    PlaybackHexByteVisualBlock(
        hexText = group.uppercase(),
        binaryText = binaryText,
        isActive = isActive,
        isPast = isPast,
        activeNibbleIndex = if (isActive) currentNibbleIndex else -1,
        activeBitIndex = if (isActive) activeBitIndex else -1,
        isActiveBitTone = isActiveBitTone,
        focusColor = focusColor,
        onFocusColor = onFocusColor,
        inactiveColor = inactiveColor,
    )
}

@Composable
private fun PlaybackHexByteVisualBlock(
    hexText: String,
    binaryText: String,
    isActive: Boolean,
    isPast: Boolean,
    activeNibbleIndex: Int,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
) {
    val annotatedHexText =
        buildAnnotatedString {
            hexText.forEachIndexed { nibbleIndex, hexChar ->
                val isCurrentNibble = isActive && isActiveBitTone && nibbleIndex == activeNibbleIndex
                val isHistoryNibble =
                    isPast ||
                        (
                            isActive &&
                                activeNibbleIndex >= 0 &&
                                (
                                    if (isActiveBitTone) {
                                        nibbleIndex < activeNibbleIndex
                                    } else {
                                        nibbleIndex <= activeNibbleIndex
                                    }
                                )
                        )
                val (textColor, backgroundColor, weight) =
                    when {
                        isCurrentNibble -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                        isHistoryNibble -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                        else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                    }
                withStyle(SpanStyle(color = textColor, background = backgroundColor, fontWeight = weight)) {
                    append(hexChar)
                }
            }
        }
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
            text = annotatedHexText,
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
