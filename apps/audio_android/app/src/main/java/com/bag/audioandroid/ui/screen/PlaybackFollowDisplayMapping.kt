package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import java.nio.charset.StandardCharsets

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

internal fun morseLetterDisplayGroups(
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

internal data class CharacterDisplayUnit(
    val text: String,
    val byteStartIndexWithinToken: Int,
    val byteCount: Int,
)

internal fun characterDisplayUnits(token: String): List<CharacterDisplayUnit> {
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

internal data class HexNibbleGroup(
    val hex: String,
    val binary: String,
)

internal fun hexNibbleGroups(group: String): List<HexNibbleGroup> =
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
