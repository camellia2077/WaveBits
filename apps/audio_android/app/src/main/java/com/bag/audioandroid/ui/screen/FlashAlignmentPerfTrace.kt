package com.bag.audioandroid.ui.screen

import android.util.Log
import com.bag.audioandroid.BuildConfig

internal object FlashAlignmentPerfTrace {
    private const val Tag = "FlashAlignmentPerf"
    private const val ReportIntervalNanos = 500_000_000L

    private var lastReportNanos = 0L
    private var visualPlaying = false
    private var visualSample = 0
    private var rawSample = 0
    private var readoutSample = 0
    private var readoutBit = -1
    private var readoutBitValue = "_"
    private var revealedBit = -1
    private var visualBit = -1
    private var rawBit = -1
    private var visualCurrentBitValue = "_"
    private var fallback = false
    private var bitReadout = false
    private var mode = "unknown"
    private var lyricsPlaying = false
    private var lyricsSample = 0
    private var token = -1
    private var tokenText = "_"
    private var tokenStart = -1
    private var tokenEnd = -1
    private var tokenProgress = "-1.00"
    private var byte = -1
    private var lyricBit = -1
    private var lyricBitOffset = -1
    private var tone = false
    private var displayUnitByteOffset = -1
    private var displayUnitByteCount = -1
    private var displayUnitHex = "_"
    private var displayUnitBinary = "_"
    private var displayCardHex = "_"
    private var displayCardBinary = "_"
    private var displayBitInByte = -1
    private var displayGlobalBit = -1
    private var cardCurrentBitValue = "_"
    private var displayGroupIndex = -1
    private var displayGroupBits = "_"
    private var lastTokenUnitDumpKey = ""

    fun recordVisual(
        mode: FlashSignalVisualizationMode,
        isPlaying: Boolean,
        smoothSample: Float,
        rawSample: Float,
        readoutSample: Float,
        readoutBit: Int?,
        readoutBitValue: Char?,
        revealedBit: Int,
        visualBit: Int?,
        rawBit: Int?,
        visualBitValue: Char?,
        usesFallbackTimeline: Boolean,
        hasBitReadout: Boolean,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }
        this.mode = mode.name
        visualPlaying = isPlaying
        visualSample = smoothSample.toInt()
        this.rawSample = rawSample.toInt()
        this.readoutSample = readoutSample.toInt()
        this.readoutBit = readoutBit ?: -1
        this.readoutBitValue = readoutBitValue?.toString() ?: "_"
        this.revealedBit = revealedBit
        this.visualBit = visualBit ?: -1
        this.rawBit = rawBit ?: -1
        visualCurrentBitValue = visualBitValue?.toString() ?: "_"
        fallback = usesFallbackTimeline
        bitReadout = hasBitReadout
        maybeReport()
    }

    fun recordLyrics(
        isPlaying: Boolean,
        sample: Int,
        state: FlashAlignmentLyricsState,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }
        lyricsPlaying = isPlaying
        lyricsSample = sample
        token = state.token
        tokenText = state.tokenText.logSafe()
        tokenStart = state.tokenStart
        tokenEnd = state.tokenEnd
        tokenProgress = "%.2f".format(state.tokenProgress)
        byte = state.byte
        lyricBit = state.bit
        lyricBitOffset = state.bitOffset
        tone = state.tone
    }

    fun recordTokenCard(
        followData: com.bag.audioandroid.domain.PayloadFollowViewData,
        presentationState: PlaybackFollowPresentationState,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }
        val activeTokenIndex = presentationState.activeTextIndex
        val activeByteIndexWithinToken = presentationState.activeByteIndexWithinToken
        val activeBitIndexWithinByte = presentationState.activeBitIndexWithinByte
        val activeTokenUnits = presentationState.rawDisplayUnitsByToken[activeTokenIndex].orEmpty()
        val activeUnit =
            activeTokenUnits
                .firstOrNull { it.byteIndexWithinToken == activeByteIndexWithinToken }

        displayUnitByteOffset = activeUnit?.byteOffset ?: -1
        displayUnitByteCount = activeUnit?.byteCount ?: -1
        displayUnitHex =
            activeUnit
                ?.hexText
                ?.logSafe()
                .orEmpty()
                .ifBlank { "_" }
        displayUnitBinary =
            activeUnit
                ?.binaryText
                ?.logSafe()
                .orEmpty()
                .ifBlank { "_" }
        displayBitInByte = activeBitIndexWithinByte
        displayGlobalBit =
            activeUnit
                ?.takeIf { activeBitIndexWithinByte >= 0 }
                ?.let { it.byteOffset * 8 + activeBitIndexWithinByte }
                ?: -1
        cardCurrentBitValue =
            activeUnit
                ?.binaryText
                ?.filter { it == '0' || it == '1' }
                ?.getOrNull(activeBitIndexWithinByte)
                ?.toString()
                ?: "_"
        val activeNibble =
            activeUnit
                ?.hexText
                ?.let { hexNibbleGroups(it) }
                ?.let { nibbles ->
                    val nibbleIndex =
                        if (activeBitIndexWithinByte >= 4 && nibbles.size > 1) {
                            1
                        } else {
                            0
                        }.coerceAtMost(nibbles.lastIndex.coerceAtLeast(0))
                    nibbles.getOrNull(nibbleIndex)
                }
        displayCardHex =
            activeNibble
                ?.hex
                ?.logSafe()
                .orEmpty()
                .ifBlank { displayUnitHex }
        displayCardBinary =
            activeNibble
                ?.binary
                ?.logSafe()
                .orEmpty()
                .ifBlank { displayUnitBinary }
        val activeGroup =
            followData.binaryGroupTimeline.firstOrNull { entry ->
                displayGlobalBit >= 0 &&
                    displayGlobalBit >= entry.bitOffset &&
                    displayGlobalBit < entry.bitOffset + entry.bitCount
            }
        visualCurrentBitValue =
            activeGroup
                ?.let { group ->
                    followData.binaryTokens
                        .getOrNull(group.groupIndex)
                        ?.filter { it == '0' || it == '1' }
                        ?.getOrNull(displayGlobalBit - group.bitOffset)
                }?.toString()
                ?: "_"
        displayGroupIndex = activeGroup?.groupIndex ?: -1
        displayGroupBits =
            activeGroup
                ?.groupIndex
                ?.let { followData.binaryTokens.getOrNull(it) }
                ?.logSafe()
                .orEmpty()
                .ifBlank { "_" }
        val tokenUnitDumpKey =
            buildString {
                append(activeTokenIndex)
                append(':')
                append(
                    activeTokenUnits.joinToString(separator = "|") {
                        "${it.byteIndexWithinToken}:${it.byteOffset}:${it.hexText}@${it.startSample}+${it.sampleCount}"
                    },
                )
            }
        if (activeTokenIndex >= 0 && tokenUnitDumpKey != lastTokenUnitDumpKey) {
            lastTokenUnitDumpKey = tokenUnitDumpKey
            logDebug(
                "tokenUnits token=$activeTokenIndex tokenText=${followData.textTokens.getOrNull(activeTokenIndex).orEmpty().logSafe()} " +
                    "units=${activeTokenUnits.joinToString(separator = ",") {
                        "idx=${it.byteIndexWithinToken}/off=${it.byteOffset}/hex=${it.hexText.logSafe()}/start=${it.startSample}/count=${it.sampleCount}"
                    }}",
            )
        }
    }

    private fun maybeReport() {
        val now = System.nanoTime()
        if (lastReportNanos == 0L) {
            lastReportNanos = now
            return
        }
        if (now - lastReportNanos < ReportIntervalNanos) {
            return
        }
        lastReportNanos = now
        logDebug(
            "mode=$mode visualPlaying=$visualPlaying lyricsPlaying=$lyricsPlaying " +
                "visualSample=$visualSample rawSample=$rawSample readoutSample=$readoutSample " +
                "readoutGlobalBit=$readoutBit revealedGlobalBit=$revealedBit " +
                "readoutCurrentBitValue=$readoutBitValue " +
                "visualGlobalBit=$visualBit rawGlobalBit=$rawBit visualCurrentBitValue=$visualCurrentBitValue " +
                "fallback=$fallback bitReadout=$bitReadout " +
                "lyricsSample=$lyricsSample token=$token tokenText=$tokenText " +
                "tokenStart=$tokenStart tokenEnd=$tokenEnd tokenProgress=$tokenProgress " +
                "tokenByteIndex=$byte tokenBitIndexWithinByte=$lyricBit " +
                "tokenBitOffsetWithinToken=$lyricBitOffset tokenToneActive=$tone " +
                "cardByteOffsetGlobal=$displayUnitByteOffset cardByteCount=$displayUnitByteCount " +
                "cardByteHex=$displayUnitHex cardByteBinary=$displayUnitBinary " +
                "cardNibbleHex=$displayCardHex cardNibbleBinary=$displayCardBinary " +
                "cardBitIndexWithinByte=$displayBitInByte cardGlobalBitOffset=$displayGlobalBit cardCurrentBitValue=$cardCurrentBitValue " +
                "cardGlobalGroupIndex=$displayGroupIndex cardGlobalGroupBits=$displayGroupBits " +
                "visualMinusLyricsSample=${visualSample - lyricsSample} " +
                "globalBitMinusTokenBit=${readoutBit - lyricBitOffset} " +
                "globalBitMinusCardBit=${readoutBit - displayGlobalBit}",
        )
    }

    private fun String.logSafe(): String =
        replace('\n', ' ')
            .replace('\r', ' ')
            .take(64)
            .ifBlank { "_" }

    private fun logDebug(message: String) {
        try {
            Log.d(Tag, message)
        } catch (_: Throwable) {
            // JVM unit tests use the Android stub jar, where Log.d is not implemented.
        }
    }
}

internal data class FlashAlignmentLyricsState(
    val token: Int,
    val tokenText: String,
    val tokenStart: Int,
    val tokenEnd: Int,
    val tokenProgress: Float,
    val byte: Int,
    val bit: Int,
    val bitOffset: Int,
    val tone: Boolean,
)

internal fun flashAlignmentLyricsState(
    followData: com.bag.audioandroid.domain.PayloadFollowViewData,
    displayedSamples: Int,
): FlashAlignmentLyricsState {
    val activeTimelineIndex = activeTextTimelineIndex(followData.textTokenTimeline, displayedSamples)
    val activeTimelineEntry = followData.textTokenTimeline.getOrNull(activeTimelineIndex)
    val activeTokenIndex = activeTimelineEntry?.tokenIndex ?: -1
    val rawDisplayUnitsByToken = followData.textRawDisplayUnits.groupBy { it.tokenIndex }
    val byte =
        activeByteIndexWithinToken(
            activeTextIndex = activeTokenIndex,
            displayedSamples = displayedSamples,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    val bitPosition =
        activeBitPositionWithinByte(
            activeTextIndex = activeTokenIndex,
            activeByteIndexWithinToken = byte,
            displayedSamples = displayedSamples,
            followData = followData,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    val bitOffset =
        byte
            .takeIf { it >= 0 && bitPosition.bitIndexWithinByte >= 0 }
            ?.let { it * 8 + bitPosition.bitIndexWithinByte }
            ?: -1
    val tokenProgress =
        activeTimelineEntry
            ?.takeIf { it.sampleCount > 0 }
            ?.let { ((displayedSamples - it.startSample).toFloat() / it.sampleCount.toFloat()).coerceIn(0f, 1f) }
            ?: -1f
    return FlashAlignmentLyricsState(
        token = activeTokenIndex,
        tokenText = followData.textTokens.getOrNull(activeTokenIndex).orEmpty(),
        tokenStart = activeTimelineEntry?.startSample ?: -1,
        tokenEnd = activeTimelineEntry?.let { it.startSample + it.sampleCount } ?: -1,
        tokenProgress = tokenProgress,
        byte = byte,
        bit = bitPosition.bitIndexWithinByte,
        bitOffset = bitOffset,
        tone = bitPosition.isToneActive,
    )
}
