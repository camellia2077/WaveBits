package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.ui.playbackLyricsAccentTextColor
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import java.nio.charset.StandardCharsets

@Composable
internal fun PlaybackFollowTokenCard(
    token: String,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    annotationMode: PlaybackFollowViewMode,
    isActive: Boolean,
    activeByteIndexWithinToken: Int,
    activeBitIndexWithinByte: Int = -1,
    isActiveBitTone: Boolean = false,
    isPast: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    modifier: Modifier = Modifier,
) {
    val visualTokens = appThemeVisualTokens()
    val activeAnnotationContainer = Color.Transparent
    val lyricsAccentTextColor = playbackLyricsAccentTextColor()
    val focusColor = MaterialTheme.colorScheme.primary
    val onFocusColor = MaterialTheme.colorScheme.onPrimary
    val activeAnnotationTint = lyricsAccentTextColor

    val containerColor =
        when {
            isActive -> visualTokens.followTokenContainerColor
            else -> Color.Transparent
        }

    val tokenColor =
        when {
            isActive -> lyricsAccentTextColor
            isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.onSurface
        }
    val cardModifier =
        if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        }

    val inactiveRawColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isPast) 0.5f else 1.0f)
    val inactiveCharacterColor = tokenColor
    val annotationByteGroups = annotationByteGroupsForMode(annotationMode, rawDisplayUnits)
    val characterDisplayUnits = remember(token) { characterDisplayUnits(token) }
    val tokenDisplayText =
        remember(
            token,
            characterDisplayUnits,
            activeByteIndexWithinToken,
            isActive,
            inactiveCharacterColor,
            lyricsAccentTextColor,
        ) {
            buildAnnotatedString {
                if (characterDisplayUnits.isEmpty()) {
                    append(token)
                    return@buildAnnotatedString
                }
                characterDisplayUnits.forEach { unit ->
                    val isActiveCharacter =
                        isActive &&
                            activeByteIndexWithinToken >= unit.byteStartIndexWithinToken &&
                            activeByteIndexWithinToken <
                            unit.byteStartIndexWithinToken + unit.byteCount
                    withStyle(
                        SpanStyle(
                            color = if (isActiveCharacter) onFocusColor else inactiveCharacterColor,
                            background = if (isActiveCharacter) focusColor else Color.Transparent,
                            fontWeight = if (isActiveCharacter) FontWeight.ExtraBold else FontWeight.Bold,
                        ),
                    ) {
                        if (isActiveCharacter) {
                            append(unit.text)
                        } else {
                            append(unit.text)
                        }
                    }
                }
            }
        }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        modifier =
            cardModifier
                .widthIn(min = PlaybackFollowTokenCardMinimumWidth, max = PlaybackFollowTokenCardMaximumWidth)
                .testTag(
                    testTag ?: if (isActive) {
                        "follow-token-active"
                    } else {
                        "follow-token"
                    },
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = tokenDisplayText,
                color = tokenColor,
                style =
                    if (isActive) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Surface(
                color =
                    if (isActive) {
                        activeAnnotationContainer
                    } else {
                        Color.Transparent
                    },
                contentColor =
                    if (isActive) {
                        activeAnnotationTint
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
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
        }
    }
}

private val PlaybackFollowTokenCardMinimumWidth = 92.dp
private val PlaybackFollowTokenCardMaximumWidth = 360.dp
private val MorseLetterDividerHeight = 34.dp
private const val BinaryByteGroupsPerRow = 3
private const val HexByteGroupsPerRow = 4

internal data class MorseLetterDisplayGroup(
    val text: String,
    val morse: String,
    val byteStartIndexWithinToken: Int,
    val byteCount: Int,
)

internal fun morseLetterDisplayGroups(
    token: String,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
): List<MorseLetterDisplayGroup> =
    morseLetterDisplayGroups(
        token = token,
        characterDisplayUnits = characterDisplayUnits(token),
        rawDisplayUnits = rawDisplayUnits,
        annotationByteGroups = annotationByteGroupsForMode(PlaybackFollowViewMode.Morse, rawDisplayUnits),
    )

private fun morseLetterDisplayGroups(
    token: String,
    characterDisplayUnits: List<CharacterDisplayUnit>,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    annotationByteGroups: List<String>,
): List<MorseLetterDisplayGroup> {
    if (token.isEmpty() || characterDisplayUnits.isEmpty()) {
        return annotationByteGroups.mapIndexed { index, group ->
            MorseLetterDisplayGroup(
                text = group,
                morse = group,
                byteStartIndexWithinToken = index,
                byteCount = 1,
            )
        }
    }
    return characterDisplayUnits
        .mapNotNull { character ->
            val firstByte =
                rawDisplayUnits.firstOrNull { unit ->
                    unit.byteIndexWithinToken >= character.byteStartIndexWithinToken &&
                        unit.byteIndexWithinToken < character.byteStartIndexWithinToken + character.byteCount
                } ?: return@mapNotNull null
            val morse = annotationByteGroups.getOrNull(rawDisplayUnits.indexOf(firstByte)).orEmpty()
            if (morse.isBlank()) {
                null
            } else {
                MorseLetterDisplayGroup(
                    text = character.text,
                    morse = morse,
                    byteStartIndexWithinToken = character.byteStartIndexWithinToken,
                    byteCount = character.byteCount,
                )
            }
        }.ifEmpty {
            annotationByteGroups.mapIndexed { index, group ->
                MorseLetterDisplayGroup(
                    text = group,
                    morse = group,
                    byteStartIndexWithinToken = index,
                    byteCount = 1,
                )
            }
        }
}

@Composable
private fun MorseLetterBlock(
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

private data class CharacterDisplayUnit(
    val text: String,
    val byteStartIndexWithinToken: Int,
    val byteCount: Int,
)

private fun characterDisplayUnits(token: String): List<CharacterDisplayUnit> {
    if (token.isEmpty()) {
        return emptyList()
    }
    val units = ArrayList<CharacterDisplayUnit>()
    var index = 0
    var byteStart = 0
    while (index < token.length) {
        val codePoint = token.codePointAt(index)
        val characterText = String(Character.toChars(codePoint))
        val byteCount = characterText.toByteArray(StandardCharsets.UTF_8).size.coerceAtLeast(1)
        units +=
            CharacterDisplayUnit(
                text = characterText,
                byteStartIndexWithinToken = byteStart,
                byteCount = byteCount,
            )
        byteStart += byteCount
        index += Character.charCount(codePoint)
    }
    return units
}

@Composable
private fun PlaybackHexByteBlock(
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

private data class HexNibbleGroup(
    val hex: String,
    val binary: String,
)

private fun hexNibbleGroups(group: String): List<HexNibbleGroup> =
    group
        .mapNotNull { hexChar ->
            hexChar.digitToIntOrNull(radix = 16)?.let { value ->
                HexNibbleGroup(
                    hex = hexChar.uppercaseChar().toString(),
                    binary = value.toString(radix = 2).padStart(4, '0'),
                )
            }
        }.ifEmpty {
            listOf(HexNibbleGroup(hex = group, binary = "0000"))
        }

@Composable
private fun PlaybackByteBlock(
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
