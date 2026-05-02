package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTokenContextTapeTest {
    @Test
    fun `active token not found keeps translation at origin`() {
        val translation =
            targetContinuousViewportTranslationPx(
                displayedSamples = 100,
                activeTimelineEntry = null,
                activeSegment = null,
                activeBounds = null,
                viewportWidthPx = 320f,
                visibleViewportWidthPx = 232f,
                contentWidthPx = 600f,
                horizontalPaddingPx = 16f,
                fadeWidthPx = 28f,
            )

        assertEquals(0f, translation, 0.0001f)
    }

    @Test
    fun `active token resolves to its lyric line range`() {
        val range =
            resolveActiveTokenLineRange(
                lineTokenRanges =
                    listOf(
                        TextFollowLineTokenRangeViewData(lineIndex = 0, tokenBeginIndex = 0, tokenCount = 3),
                        TextFollowLineTokenRangeViewData(lineIndex = 1, tokenBeginIndex = 3, tokenCount = 2),
                    ),
                lyricLineFollowAvailable = true,
                activeTokenIndex = 4,
                tokenCount = 5,
            )

        assertEquals(3..4, range)
    }

    @Test
    fun `missing lyric line metadata falls back to full token flow`() {
        val line =
            resolveContinuousViewportLine(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("ASH", "BELL", "RITE"),
                        textFollowAvailable = true,
                    ),
                activeTokenIndex = 1,
            )

        assertNotNull(line)
        assertEquals("ASH BELL RITE", line?.text)
        assertEquals(listOf(0, 1, 2), line?.tokenSegments?.map { it.tokenIndex })
    }

    @Test
    fun `current lyric line builds continuous text with stable token ranges`() {
        val line =
            resolveContinuousViewportLine(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("The", "lamp", "keepers", "unseal", "it"),
                        lineTokenRanges =
                            listOf(
                                TextFollowLineTokenRangeViewData(lineIndex = 0, tokenBeginIndex = 0, tokenCount = 3),
                                TextFollowLineTokenRangeViewData(lineIndex = 1, tokenBeginIndex = 3, tokenCount = 2),
                            ),
                        lyricLineFollowAvailable = true,
                        textFollowAvailable = true,
                    ),
                activeTokenIndex = 1,
            )

        assertEquals("The lamp keepers", line?.text)
        assertEquals(0, line?.tokenSegments?.get(0)?.start)
        assertEquals(3, line?.tokenSegments?.get(0)?.endExclusive)
        assertEquals(4, line?.tokenSegments?.get(1)?.start)
        assertEquals(8, line?.tokenSegments?.get(1)?.endExclusive)
        assertEquals(9, line?.tokenSegments?.get(2)?.start)
    }

    @Test
    fun `lyric line text preserves punctuation spacing when tokens are detached`() {
        val line =
            resolveContinuousViewportLine(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("Hello", ",", "world", "!"),
                        lyricLines = listOf("Hello, world!"),
                        lineTokenRanges =
                            listOf(
                                TextFollowLineTokenRangeViewData(lineIndex = 0, tokenBeginIndex = 0, tokenCount = 4),
                            ),
                        lyricLineFollowAvailable = true,
                        textFollowAvailable = true,
                    ),
                activeTokenIndex = 2,
            )

        assertEquals("Hello, world!", line?.text)
        assertEquals(0, line?.tokenSegments?.get(0)?.start)
        assertEquals(5, line?.tokenSegments?.get(0)?.endExclusive)
        assertEquals(5, line?.tokenSegments?.get(1)?.start)
        assertEquals(6, line?.tokenSegments?.get(1)?.endExclusive)
        assertEquals(7, line?.tokenSegments?.get(2)?.start)
        assertEquals(12, line?.tokenSegments?.get(2)?.endExclusive)
        assertEquals(12, line?.tokenSegments?.get(3)?.start)
    }

    @Test
    fun `fallback separator logic keeps detached punctuation readable`() {
        val line =
            resolveContinuousViewportLineForRange(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("don", "'", "t", "panic", "!"),
                        textFollowAvailable = true,
                    ),
                tokenRange = 0..4,
            )

        assertEquals("don't panic!", line.text)
    }

    @Test
    fun `display splitter keeps punctuation attached to previous token`() {
        val followData =
            PayloadFollowViewData(
                textTokens = listOf("AAAAAAAAAAAAAAAAAAAAAAAA", ",", "next"),
                lineTokenRanges = listOf(TextFollowLineTokenRangeViewData(0, 0, 3)),
                textFollowAvailable = true,
            )
        val displayLines = buildDisplayTokenLineRanges(followData)

        assertEquals(2, displayLines.size)
        assertEquals(0, displayLines[0].tokenBeginIndex)
        assertEquals(2, displayLines[0].tokenCount)
        assertEquals(
            "AAAAAAAAAAAAAAAAAAAAAAAA,",
            resolveContinuousViewportLineForRange(followData, displayLines[0].tokenRange).text,
        )
        assertEquals(
            "next",
            resolveContinuousViewportLineForRange(followData, displayLines[1].tokenRange).text,
        )
    }

    @Test
    fun `long ascii token flow is split into display lines`() {
        val displayLines =
            buildDisplayTokenLineRanges(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("PRAISE", "THE", "OMNISSIAH", "PRAISE", "THE", "MACHINE"),
                        lineTokenRanges = listOf(TextFollowLineTokenRangeViewData(0, 0, 6)),
                        textFollowAvailable = true,
                    ),
            )

        assertEquals(2, displayLines.size)
        assertEquals(0, displayLines[0].tokenBeginIndex)
        assertEquals(3, displayLines[0].tokenCount)
        assertEquals(3, displayLines[1].tokenBeginIndex)
        assertEquals(3, displayLines[1].tokenCount)
    }

    @Test
    fun `mini morse style long text does not remain one display line`() {
        val displayLines =
            buildDisplayTokenLineRanges(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("PRAISE", "THE", "OMNISSIAH", "PRAISE", "THE", "MACHINE"),
                        textFollowAvailable = true,
                    ),
            )

        assertTrue(displayLines.size > 1)
    }

    @Test
    fun `cjk display line still wraps`() {
        val line =
            resolveContinuousViewportLineForRange(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("二", "进", "制", "祷", "文"),
                        textFollowAvailable = true,
                    ),
                tokenRange = 0..4,
            )

        assertTrue(line.shouldWrap)
    }

    @Test
    fun `oversized single token stays one display line and wraps`() {
        val followData =
            PayloadFollowViewData(
                textTokens = listOf("SUPERCALIFRAGILISTICEXPIALIDOCIOUS"),
                textFollowAvailable = true,
            )
        val displayLines = buildDisplayTokenLineRanges(followData)
        val line = resolveContinuousViewportLineForRange(followData, displayLines.single().tokenRange)

        assertEquals(1, displayLines.size)
        assertEquals(1, displayLines.single().tokenCount)
        assertTrue(line.shouldWrap)
    }

    @Test
    fun `active token resolves to split display line`() {
        val displayLines =
            buildDisplayTokenLineRanges(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("PRAISE", "THE", "OMNISSIAH", "PRAISE", "THE", "MACHINE"),
                        lineTokenRanges = listOf(TextFollowLineTokenRangeViewData(0, 0, 6)),
                        textFollowAvailable = true,
                    ),
            )

        assertEquals(1, resolveActiveDisplayLineIndex(displayLines, activeTokenIndex = 4))
    }

    @Test
    fun `tap resolver maps character offset to token index`() {
        val line =
            resolveContinuousViewportLineForRange(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("ASH", "BELL", "RITE"),
                        textFollowAvailable = true,
                    ),
                tokenRange = 0..2,
            )

        assertEquals(1, resolveContinuousTokenIndexAtTextOffset(line, characterOffset = 5))
        assertEquals(2, resolveContinuousTokenIndexAtTextOffset(line, characterOffset = 9))
    }

    @Test
    fun `tap resolver ignores separator space between tokens`() {
        val line =
            resolveContinuousViewportLineForRange(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("ASH", "BELL"),
                        textFollowAvailable = true,
                    ),
                tokenRange = 0..1,
            )

        assertEquals(null, resolveContinuousTokenIndexAtTextOffset(line, characterOffset = 3))
    }

    @Test
    fun `short active segment stays anchored in viewport`() {
        val translation =
            targetContinuousViewportTranslationPx(
                displayedSamples = 8,
                activeTimelineEntry = TextFollowTimelineEntry(startSample = 8, sampleCount = 8, tokenIndex = 1),
                activeSegment = ContinuousViewportTokenSegment(tokenIndex = 1, start = 4, endExclusive = 13),
                activeBounds = TokenPixelBounds(startPx = 260f, endPx = 330f),
                viewportWidthPx = 320f,
                visibleViewportWidthPx = 232f,
                contentWidthPx = 600f,
                horizontalPaddingPx = 16f,
                fadeWidthPx = 28f,
            )

        assertEquals(-83.96f, translation, 0.01f)
    }

    @Test
    fun `long active segment sweep starts by showing segment head`() {
        val translation =
            targetContinuousViewportTranslationPx(
                displayedSamples = 0,
                activeTimelineEntry = TextFollowTimelineEntry(startSample = 0, sampleCount = 10, tokenIndex = 2),
                activeSegment = ContinuousViewportTokenSegment(tokenIndex = 2, start = 14, endExclusive = 37),
                activeBounds = TokenPixelBounds(startPx = 280f, endPx = 640f),
                viewportWidthPx = 320f,
                visibleViewportWidthPx = 232f,
                contentWidthPx = 900f,
                horizontalPaddingPx = 16f,
                fadeWidthPx = 28f,
            )

        assertEquals(-236f, translation, 0.01f)
    }

    @Test
    fun `long active segment sweep advances with playback progress`() {
        val earlyTranslation =
            targetContinuousViewportTranslationPx(
                displayedSamples = 2,
                activeTimelineEntry = TextFollowTimelineEntry(startSample = 0, sampleCount = 10, tokenIndex = 2),
                activeSegment = ContinuousViewportTokenSegment(tokenIndex = 2, start = 14, endExclusive = 37),
                activeBounds = TokenPixelBounds(startPx = 280f, endPx = 640f),
                viewportWidthPx = 320f,
                visibleViewportWidthPx = 232f,
                contentWidthPx = 900f,
                horizontalPaddingPx = 16f,
                fadeWidthPx = 28f,
            )
        val lateTranslation =
            targetContinuousViewportTranslationPx(
                displayedSamples = 8,
                activeTimelineEntry = TextFollowTimelineEntry(startSample = 0, sampleCount = 10, tokenIndex = 2),
                activeSegment = ContinuousViewportTokenSegment(tokenIndex = 2, start = 14, endExclusive = 37),
                activeBounds = TokenPixelBounds(startPx = 280f, endPx = 640f),
                viewportWidthPx = 320f,
                visibleViewportWidthPx = 232f,
                contentWidthPx = 900f,
                horizontalPaddingPx = 16f,
                fadeWidthPx = 28f,
            )

        assertTrue(lateTranslation < earlyTranslation)
        assertEquals(-338.4f, lateTranslation, 0.01f)
    }

    @Test
    fun `long active segment sweep ends without overshooting viewport bounds`() {
        val translation =
            targetContinuousViewportTranslationPx(
                displayedSamples = 10,
                activeTimelineEntry = TextFollowTimelineEntry(startSample = 0, sampleCount = 10, tokenIndex = 2),
                activeSegment = ContinuousViewportTokenSegment(tokenIndex = 2, start = 14, endExclusive = 37),
                activeBounds = TokenPixelBounds(startPx = 280f, endPx = 640f),
                viewportWidthPx = 320f,
                visibleViewportWidthPx = 232f,
                contentWidthPx = 640f,
                horizontalPaddingPx = 16f,
                fadeWidthPx = 28f,
            )

        assertEquals(-320f, translation, 0.01f)
    }

    @Test
    fun `continuous sweep detection only enables for active segment wider than viewport`() {
        assertTrue(
            shouldSweepContinuousSegment(
                activeTimelineEntry = TextFollowTimelineEntry(startSample = 0, sampleCount = 10, tokenIndex = 1),
                activeBounds = TokenPixelBounds(startPx = 0f, endPx = 280f),
                visibleViewportWidthPx = 232f,
            ),
        )
        assertFalse(
            shouldSweepContinuousSegment(
                activeTimelineEntry = TextFollowTimelineEntry(startSample = 0, sampleCount = 10, tokenIndex = 1),
                activeBounds = TokenPixelBounds(startPx = 0f, endPx = 180f),
                visibleViewportWidthPx = 232f,
            ),
        )
        assertFalse(
            shouldSweepContinuousSegment(
                activeTimelineEntry = null,
                activeBounds = TokenPixelBounds(startPx = 0f, endPx = 280f),
                visibleViewportWidthPx = 232f,
            ),
        )
    }

    @Test
    fun `invalid active token line range returns null`() {
        assertEquals(
            0..2,
            resolveActiveTokenLineRange(
                lineTokenRanges = emptyList(),
                lyricLineFollowAvailable = true,
                activeTokenIndex = 1,
                tokenCount = 3,
            ),
        )
    }
}
