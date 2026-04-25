package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData

internal data class ProSymbolExplanation(
    val symbolIndex: Int,
    val nibbleValue: Int,
    val nibbleHex: String,
    val highFreqHz: Int,
    val lowFreqHz: Int,
    val label: String,
    val slotIndexWithinByte: Int,
    val isCurrent: Boolean,
)

internal data class ProTokenByteMapping(
    val tokenIndex: Int,
    val tokenText: String,
    val byteIndexWithinToken: Int,
    val byteCountWithinUnit: Int,
)

internal data class ProUpcomingSymbolExplanation(
    val slotIndexWithinByte: Int,
    val nibbleHex: String,
    val lowFreqHz: Int,
    val highFreqHz: Int,
)

internal data class ProByteExplanation(
    val byteIndex: Int,
    val asciiDisplay: String,
    val tokenText: String,
    val tokenIndex: Int,
    val byteIndexWithinToken: Int,
    val byteCountWithinUnit: Int,
    val byteHex: String,
    val byteBinary: String,
    val highNibbleHex: String,
    val lowNibbleHex: String,
    val isHighNibbleCurrent: Boolean,
)

internal data class ProEncodingVisualizationState(
    val symbols: List<ProSymbolExplanation>,
    val currentSymbol: ProSymbolExplanation,
    val nextSymbol: ProUpcomingSymbolExplanation?,
    val tokenByteMapping: ProTokenByteMapping,
    val byteExplanation: ProByteExplanation,
)

internal fun deriveProEncodingVisualizationState(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    frameSamples: Int,
): ProEncodingVisualizationState? {
    if (frameSamples <= 0 || followData.byteTimeline.isEmpty() || followData.textRawDisplayUnits.isEmpty()) {
        return null
    }

    val activeByteEntry =
        resolveActiveByteTimelineEntry(
            byteTimeline = followData.byteTimeline,
            displayedSamples = displayedSamples,
        ) ?: return null
    val activeDisplayUnit =
        followData.textRawDisplayUnits.firstOrNull { unit ->
            unit.byteOffset == activeByteEntry.byteIndex
        } ?: return null

    val byteValue = parseHexByte(activeDisplayUnit.hexText) ?: return null
    val highNibble = (byteValue shr 4) and 0x0F
    val lowNibble = byteValue and 0x0F
    val relativeSample = (displayedSamples - activeByteEntry.startSample).coerceAtLeast(0)
    val symbolOffsetWithinByte =
        (relativeSample / frameSamples)
            .coerceIn(0, 1)
    val currentSymbolIndex = activeByteEntry.byteIndex * 2 + symbolOffsetWithinByte
    val allSymbols =
        followData.textRawDisplayUnits
            .sortedBy(TextFollowRawDisplayUnitViewData::byteOffset)
            .flatMap { unit ->
                val unitByteValue = parseHexByte(unit.hexText) ?: return@flatMap emptyList()
                listOf(
                    buildProSymbolExplanation(
                        symbolIndex = unit.byteOffset * 2,
                        nibbleValue = (unitByteValue shr 4) and 0x0F,
                        label = "High",
                        slotIndexWithinByte = 0,
                        isCurrent = false,
                    ),
                    buildProSymbolExplanation(
                        symbolIndex = unit.byteOffset * 2 + 1,
                        nibbleValue = unitByteValue and 0x0F,
                        label = "Low",
                        slotIndexWithinByte = 1,
                        isCurrent = false,
                    ),
                )
            }

    val currentSymbol =
        allSymbols.getOrNull(currentSymbolIndex)?.copy(isCurrent = true)
            ?: buildProSymbolExplanation(
                symbolIndex = currentSymbolIndex,
                nibbleValue = if (symbolOffsetWithinByte == 0) highNibble else lowNibble,
                label = if (symbolOffsetWithinByte == 0) "High" else "Low",
                slotIndexWithinByte = symbolOffsetWithinByte,
                isCurrent = true,
            )
    val symbols =
        listOfNotNull(
            allSymbols.getOrNull(currentSymbolIndex - 1)?.copy(isCurrent = false),
            currentSymbol,
            allSymbols.getOrNull(currentSymbolIndex + 1)?.copy(isCurrent = false),
        )
    val nextSymbol =
        allSymbols.getOrNull(currentSymbolIndex + 1)?.let { upcoming ->
            ProUpcomingSymbolExplanation(
                slotIndexWithinByte = upcoming.slotIndexWithinByte,
                nibbleHex = upcoming.nibbleHex,
                lowFreqHz = upcoming.lowFreqHz,
                highFreqHz = upcoming.highFreqHz,
            )
        }
    val tokenByteMapping =
        ProTokenByteMapping(
            tokenIndex = activeDisplayUnit.tokenIndex,
            tokenText = followData.textTokens.getOrNull(activeDisplayUnit.tokenIndex).orEmpty(),
            byteIndexWithinToken = activeDisplayUnit.byteIndexWithinToken,
            byteCountWithinUnit = activeDisplayUnit.byteCount,
        )
    val byteExplanation =
        ProByteExplanation(
            byteIndex = activeByteEntry.byteIndex,
            asciiDisplay = asciiDisplay(byteValue),
            tokenText = followData.textTokens.getOrNull(activeDisplayUnit.tokenIndex).orEmpty(),
            tokenIndex = activeDisplayUnit.tokenIndex,
            byteIndexWithinToken = activeDisplayUnit.byteIndexWithinToken,
            byteCountWithinUnit = activeDisplayUnit.byteCount,
            byteHex = activeDisplayUnit.hexText.ifBlank { byteValue.toString(16).uppercase().padStart(2, '0') },
            byteBinary = activeDisplayUnit.binaryText.ifBlank { byteValue.toString(2).padStart(8, '0') },
            highNibbleHex = highNibble.toString(16).uppercase(),
            lowNibbleHex = lowNibble.toString(16).uppercase(),
            isHighNibbleCurrent = symbolOffsetWithinByte == 0,
        )
    return ProEncodingVisualizationState(
        symbols = symbols,
        currentSymbol = currentSymbol,
        nextSymbol = nextSymbol,
        tokenByteMapping = tokenByteMapping,
        byteExplanation = byteExplanation,
    )
}

private fun resolveActiveByteTimelineEntry(
    byteTimeline: List<PayloadFollowByteTimelineEntry>,
    displayedSamples: Int,
): PayloadFollowByteTimelineEntry? =
    byteTimeline.firstOrNull { entry ->
        displayedSamples >= entry.startSample &&
            displayedSamples < entry.startSample + entry.sampleCount
    } ?: byteTimeline.lastOrNull { entry ->
        displayedSamples >= entry.startSample
    }

private fun buildProSymbolExplanation(
    symbolIndex: Int,
    nibbleValue: Int,
    label: String,
    slotIndexWithinByte: Int,
    isCurrent: Boolean,
): ProSymbolExplanation =
    ProSymbolExplanation(
        symbolIndex = symbolIndex,
        nibbleValue = nibbleValue,
        nibbleHex = nibbleValue.toString(16).uppercase(),
        highFreqHz = ProHighFreqHz[nibbleValue % ProHighFreqHz.size],
        lowFreqHz = ProLowFreqHz[nibbleValue / ProHighFreqHz.size],
        label = label,
        slotIndexWithinByte = slotIndexWithinByte,
        isCurrent = isCurrent,
    )

private fun parseHexByte(hexText: String): Int? =
    hexText
        .replace(" ", "")
        .takeIf { it.length >= 2 }
        ?.take(2)
        ?.toIntOrNull(radix = 16)

private fun asciiDisplay(byteValue: Int): String =
    when (byteValue) {
        in 32..126 -> byteValue.toChar().toString()
        0x20 -> "Space"
        else -> "?"
    }

internal val ProLowFreqHz = listOf(697, 770, 852, 941)
internal val ProHighFreqHz = listOf(1209, 1336, 1477, 1633)
