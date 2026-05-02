package com.bag.audioandroid.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry

private const val TokenTapeAnimationDurationMs = 180
private const val TokenTapeAnchorRatio = 0.72f
private const val DisplayLinePreferredUnits = 24
private const val DisplayLineHardUnits = 32
private val TokenTapeVerticalPadding = 6.dp
private val TokenTapeFadeWidth = 28.dp
private const val TokenTapeSingleLineHeightDp = 36f
private const val TokenTapeWrappedLineHeightDp = 56f

internal data class ContinuousViewportTokenSegment(
    val tokenIndex: Int,
    val start: Int,
    val endExclusive: Int,
)

internal data class ContinuousViewportLine(
    val text: String,
    val tokenSegments: List<ContinuousViewportTokenSegment>,
    val shouldWrap: Boolean,
)

internal data class DisplayTokenLineRange(
    val lineIndex: Int,
    val tokenBeginIndex: Int,
    val tokenCount: Int,
    val sourceLineIndex: Int,
    val coversFullSourceLine: Boolean,
) {
    val tokenRange: IntRange
        get() = tokenBeginIndex until (tokenBeginIndex + tokenCount)
}

internal data class TokenPixelBounds(
    val startPx: Float,
    val endPx: Float,
) {
    val widthPx: Float
        get() = (endPx - startPx).coerceAtLeast(0f)
}

/**
 * Renders a vertically scrolling, multi-line "lyrics style" view of the decoded text tokens.
 *
 * - The view behaves like a standard music player's lyrics scroller.
 * - The active line is kept vertically centered using large vertical content padding.
 * - Previous and future lines are faded out, while the currently playing line is fully opaque.
 * - Within the active line, the horizontal "tape" translation continues to track the
 *   currently playing token, smoothly panning left as long sentences are sung.
 */
@Composable
internal fun PlaybackTokenContextTape(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    visibleLineCount: Int = 3,
    modifier: Modifier = Modifier,
    onSeekToSample: (Int) -> Unit = {},
) {
    if (!followData.textFollowAvailable || followData.textTokens.isEmpty()) {
        return
    }

    val activeTimelineIndex =
        remember(followData.textTokenTimeline, displayedSamples) {
            followActiveTextTimelineIndex(followData, displayedSamples)
        }
    val activeTimelineEntry = followData.textTokenTimeline.getOrNull(activeTimelineIndex)
    val activeTokenIndex = activeTimelineEntry?.tokenIndex ?: -1
    val tokenStartSamples =
        remember(followData.textTokenTimeline) {
            followData.textTokenTimeline
                .groupBy { it.tokenIndex }
                .mapValues { (_, entries) -> entries.minOfOrNull { it.startSample } }
        }

    val sourceLineRanges =
        followData.lineTokenRanges.ifEmpty {
            listOf(TextFollowLineTokenRangeViewData(0, 0, followData.textTokens.size))
        }
    val displayLineRanges =
        remember(followData.textTokens, sourceLineRanges) {
            buildDisplayTokenLineRanges(
                followData = followData,
                sourceLineRanges = sourceLineRanges,
            )
        }
    val prefersWrappedLines =
        remember(followData.textTokens, followData.lyricLines, displayLineRanges) {
            displayLineRanges.any { lineRange ->
                resolveContinuousViewportLineForRange(
                    followData = followData,
                    tokenRange = lineRange.tokenRange,
                    lyricLineText = lineRange.exactLyricLineText(followData),
                ).shouldWrap
            }
        }

    val activeLineIndex =
        remember(displayLineRanges, activeTokenIndex) {
            resolveActiveDisplayLineIndex(
                displayLineRanges = displayLineRanges,
                activeTokenIndex = activeTokenIndex,
            )
        }

    val listState =
        androidx.compose.foundation.lazy
            .rememberLazyListState()

    // Smoothly scroll the list so the active line moves to the center spot
    androidx.compose.runtime.LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            listState.animateScrollToItem(activeLineIndex)
        }
    }

    // A single line's height is roughly 36.dp (24.sp line height + 6.dp vertical padding * 2).
    // Using an inter-line spacing of 4.dp gives us a compact display.
    val singleLineHeightDp = if (prefersWrappedLines) TokenTapeWrappedLineHeightDp else TokenTapeSingleLineHeightDp
    val spacingDp = 4f
    val totalHeightDp = singleLineHeightDp * visibleLineCount + spacingDp * (visibleLineCount - 1)
    val verticalPaddingDp =
        if (visibleLineCount <= 2) {
            singleLineHeightDp + spacingDp
        } else {
            (totalHeightDp - singleLineHeightDp) / 2f
        }

    // A fixed-height LazyColumn with userScrollEnabled = false prevents nested scrolling
    // conflicts with the outer parent's verticalScroll.
    // The vertical padding positions the active item: centered for wide lyric
    // stacks, or on the lower row in the compact two-line Lyrics view so the
    // just-played line can remain fully visible above it instead of being clipped.
    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(spacingDp.dp),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(vertical = verticalPaddingDp.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .height(totalHeightDp.dp)
                .testTag("playback-token-context-tape-list"),
    ) {
        items(displayLineRanges.size) { lineIndex ->
            val lineRange = displayLineRanges[lineIndex]
            val isActiveLine = lineIndex == activeLineIndex
            val lineModel =
                remember(followData.textTokens, followData.lyricLines, lineRange) {
                    resolveContinuousViewportLineForRange(
                        followData = followData,
                        tokenRange = lineRange.tokenRange,
                        lyricLineText = lineRange.exactLyricLineText(followData),
                    )
                }

            // Fade out previous/future lines to create a Karaoke focus effect
            // We use a stepped alpha based on distance from the active line
            val distance = kotlin.math.abs(lineIndex - activeLineIndex)
            val targetAlpha =
                when (distance) {
                    0 -> 1.0f
                    1 -> 0.5f
                    else -> 0.2f
                }
            val alpha by animateFloatAsState(
                targetValue = targetAlpha,
                label = "context_tape_line_alpha",
            )

            PlaybackTokenContextTapeLine(
                lineModel = lineModel,
                activeTokenIndex = activeTokenIndex,
                activeTimelineEntry = if (isActiveLine) activeTimelineEntry else null,
                displayedSamples = displayedSamples,
                tokenStartSamples = tokenStartSamples,
                onSeekToSample = onSeekToSample,
                modifier = Modifier.alpha(alpha),
            )
        }
    }
}

@Composable
internal fun PlaybackTokenContextTapeLine(
    lineModel: ContinuousViewportLine,
    activeTokenIndex: Int,
    activeTimelineEntry: TextFollowTimelineEntry?,
    displayedSamples: Int,
    tokenStartSamples: Map<Int, Int?> = emptyMap(),
    onSeekToSample: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
    val shouldWrap = lineModel.shouldWrap
    val activeColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = TokenTapeAnimationDurationMs, easing = FastOutSlowInEasing),
        label = "playbackTokenContinuousActiveColor",
    )
    val historyColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        animationSpec = tween(durationMillis = TokenTapeAnimationDurationMs, easing = FastOutSlowInEasing),
        label = "playbackTokenContinuousHistoryColor",
    )
    val futureColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
        animationSpec = tween(durationMillis = TokenTapeAnimationDurationMs, easing = FastOutSlowInEasing),
        label = "playbackTokenContinuousFutureColor",
    )
    val tokenPixelBounds = remember(lineModel) { mutableStateMapOf<Int, TokenPixelBounds>() }
    var lineTextWidthPx by remember(lineModel) { mutableFloatStateOf(0f) }
    var textLayoutResult by remember(lineModel) { mutableStateOf<TextLayoutResult?>(null) }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .clipToBounds(),
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val horizontalPaddingPx = with(density) { PlaybackLyricsHorizontalPadding.toPx() }
        val fadeWidthPx = with(density) { TokenTapeFadeWidth.toPx() }
        val visibleViewportWidthPx =
            (viewportWidthPx - 2f * horizontalPaddingPx - 2f * fadeWidthPx).coerceAtLeast(1f)
        val activeSegment = lineModel.tokenSegments.firstOrNull { it.tokenIndex == activeTokenIndex }
        val activeBounds = activeSegment?.let { tokenPixelBounds[it.tokenIndex] }
        val targetTranslationPx =
            if (shouldWrap) {
                0f
            } else {
                targetContinuousViewportTranslationPx(
                    displayedSamples = displayedSamples,
                    activeTimelineEntry = activeTimelineEntry,
                    activeSegment = activeSegment,
                    activeBounds = activeBounds,
                    viewportWidthPx = viewportWidthPx,
                    visibleViewportWidthPx = visibleViewportWidthPx,
                    contentWidthPx = lineTextWidthPx + horizontalPaddingPx * 2f,
                    horizontalPaddingPx = horizontalPaddingPx,
                    fadeWidthPx = fadeWidthPx,
                )
            }
        val shouldSweepWithinActiveToken =
            !shouldWrap &&
                shouldSweepContinuousSegment(
                    activeTimelineEntry = activeTimelineEntry,
                    activeBounds = activeBounds,
                    visibleViewportWidthPx = visibleViewportWidthPx,
                )
        val animatedTranslationPx by animateFloatAsState(
            targetValue = targetTranslationPx,
            animationSpec =
                tween(
                    durationMillis = TokenTapeAnimationDurationMs,
                    easing = FastOutSlowInEasing,
                ),
            label = "playbackTokenContinuousTranslation",
        )
        val resolvedTranslationPx =
            if (shouldSweepWithinActiveToken) {
                targetTranslationPx
            } else {
                animatedTranslationPx
            }

        Text(
            text =
                buildContinuousViewportAnnotatedString(
                    lineModel = lineModel,
                    activeTokenIndex = activeTokenIndex,
                    activeColor = activeColor,
                    historyColor = historyColor,
                    futureColor = futureColor,
                ),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                ),
            maxLines = if (shouldWrap) 2 else 1,
            softWrap = shouldWrap,
            overflow = TextOverflow.Clip,
            onTextLayout = { layoutResult ->
                textLayoutResult = layoutResult
                lineTextWidthPx = layoutResult.size.width.toFloat()
                updateTokenPixelBounds(
                    layoutResult = layoutResult,
                    lineModel = lineModel,
                    tokenPixelBounds = tokenPixelBounds,
                )
            },
            modifier =
                Modifier
                    .graphicsLayer { translationX = resolvedTranslationPx }
                    .padding(horizontal = PlaybackLyricsHorizontalPadding, vertical = TokenTapeVerticalPadding)
                    .testTag("playback-token-context-active"),
        )

        if (!shouldWrap) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(TokenTapeFadeWidth)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(surfaceColor, surfaceColor.copy(alpha = 0f)),
                            ),
                        ).testTag("playback-token-context-fade-left"),
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(TokenTapeFadeWidth)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(surfaceColor.copy(alpha = 0f), surfaceColor),
                            ),
                        ).testTag("playback-token-context-fade-right"),
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(lineModel, resolvedTranslationPx, tokenStartSamples, onSeekToSample) {
                        detectTapGestures { tapOffset ->
                            val tappedTokenIndex =
                                resolveTappedContinuousTokenIndex(
                                    tapOffset = tapOffset,
                                    layoutResult = textLayoutResult,
                                    lineModel = lineModel,
                                    translationPx = resolvedTranslationPx,
                                    horizontalPaddingPx = horizontalPaddingPx,
                                    verticalPaddingPx = with(density) { TokenTapeVerticalPadding.toPx() },
                                ) ?: return@detectTapGestures
                            val startSample =
                                tokenStartSamples[tappedTokenIndex]?.takeIf { it >= 0 }
                                    ?: return@detectTapGestures
                            onSeekToSample(startSample)
                        }
                    },
        )
    }
}

internal fun buildContinuousViewportAnnotatedString(
    lineModel: ContinuousViewportLine,
    activeTokenIndex: Int,
    activeColor: androidx.compose.ui.graphics.Color,
    historyColor: androidx.compose.ui.graphics.Color,
    futureColor: androidx.compose.ui.graphics.Color,
) = buildAnnotatedString {
    var cursor = 0
    lineModel.tokenSegments.forEach { segment ->
        if (cursor < segment.start) {
            append(lineModel.text.substring(cursor, segment.start))
        }
        val tokenColor =
            when {
                segment.tokenIndex == activeTokenIndex -> activeColor
                activeTokenIndex >= 0 && segment.tokenIndex < activeTokenIndex -> historyColor
                else -> futureColor
            }
        withStyle(
            SpanStyle(
                color = tokenColor,
                fontWeight = if (segment.tokenIndex == activeTokenIndex) FontWeight.SemiBold else FontWeight.Medium,
            ),
        ) {
            append(lineModel.text.substring(segment.start, segment.endExclusive))
        }
        cursor = segment.endExclusive
    }
    if (cursor < lineModel.text.length) {
        append(lineModel.text.substring(cursor))
    }
}

internal fun resolveContinuousTokenIndexAtTextOffset(
    lineModel: ContinuousViewportLine,
    characterOffset: Int,
): Int? =
    lineModel.tokenSegments
        .firstOrNull { segment ->
            characterOffset >= segment.start && characterOffset < segment.endExclusive
        }?.tokenIndex

private fun resolveTappedContinuousTokenIndex(
    tapOffset: Offset,
    layoutResult: TextLayoutResult?,
    lineModel: ContinuousViewportLine,
    translationPx: Float,
    horizontalPaddingPx: Float,
    verticalPaddingPx: Float,
): Int? {
    if (layoutResult == null) {
        return null
    }
    val localX = tapOffset.x - translationPx - horizontalPaddingPx
    val localY = tapOffset.y - verticalPaddingPx
    if (localX < 0f || localX > layoutResult.size.width || localY < 0f || localY > layoutResult.size.height) {
        return null
    }
    return resolveContinuousTokenIndexAtTextOffset(
        lineModel = lineModel,
        characterOffset = layoutResult.getOffsetForPosition(Offset(localX, localY)),
    )
}

internal fun updateTokenPixelBounds(
    layoutResult: TextLayoutResult,
    lineModel: ContinuousViewportLine,
    tokenPixelBounds: MutableMap<Int, TokenPixelBounds>,
) {
    lineModel.tokenSegments.forEach { segment ->
        if (segment.endExclusive <= segment.start || segment.start >= layoutResult.layoutInput.text.text.length) {
            tokenPixelBounds[segment.tokenIndex] = TokenPixelBounds(0f, 0f)
        } else {
            val safeStart = segment.start.coerceAtMost(layoutResult.layoutInput.text.text.lastIndex)
            val safeEndInclusive = (segment.endExclusive - 1).coerceAtMost(layoutResult.layoutInput.text.text.lastIndex)
            val startBox = layoutResult.getBoundingBox(safeStart)
            val endBox = layoutResult.getBoundingBox(safeEndInclusive)
            tokenPixelBounds[segment.tokenIndex] = TokenPixelBounds(startPx = startBox.left, endPx = endBox.right)
        }
    }
}

internal fun followActiveTextTimelineIndex(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
): Int =
    followData.textTokenTimeline.indexOfLast { entry ->
        displayedSamples >= entry.startSample &&
            displayedSamples < entry.startSample + entry.sampleCount
    }

internal fun resolveActiveTokenLineRange(
    lineTokenRanges: List<TextFollowLineTokenRangeViewData>,
    lyricLineFollowAvailable: Boolean,
    activeTokenIndex: Int,
    tokenCount: Int,
): IntRange? {
    if (tokenCount <= 0 || activeTokenIndex !in 0 until tokenCount) {
        return null
    }
    if (!lyricLineFollowAvailable || lineTokenRanges.isEmpty()) {
        return 0 until tokenCount
    }
    val activeLineRange =
        lineTokenRanges.firstOrNull { lineRange ->
            activeTokenIndex >= lineRange.tokenBeginIndex &&
                activeTokenIndex < lineRange.tokenBeginIndex + lineRange.tokenCount
        } ?: return null
    val start = activeLineRange.tokenBeginIndex.coerceIn(0, tokenCount - 1)
    val endExclusive = (activeLineRange.tokenBeginIndex + activeLineRange.tokenCount).coerceIn(start + 1, tokenCount)
    return start until endExclusive
}

internal fun buildDisplayTokenLineRanges(
    followData: PayloadFollowViewData,
    sourceLineRanges: List<TextFollowLineTokenRangeViewData> =
        followData.lineTokenRanges.ifEmpty {
            listOf(TextFollowLineTokenRangeViewData(0, 0, followData.textTokens.size))
        },
): List<DisplayTokenLineRange> {
    if (followData.textTokens.isEmpty()) {
        return emptyList()
    }

    val displayLineRanges = ArrayList<DisplayTokenLineRange>()
    sourceLineRanges.forEach { sourceLineRange ->
        val sourceStart = sourceLineRange.tokenBeginIndex
        val sourceEndExclusive = (sourceLineRange.tokenBeginIndex + sourceLineRange.tokenCount)
        if (sourceStart !in followData.textTokens.indices || sourceEndExclusive > followData.textTokens.size) {
            return@forEach
        }
        appendDisplayLineRangesForSourceLine(
            followData = followData,
            sourceLineRange = sourceLineRange,
            out = displayLineRanges,
        )
    }
    return displayLineRanges
}

private fun appendDisplayLineRangesForSourceLine(
    followData: PayloadFollowViewData,
    sourceLineRange: TextFollowLineTokenRangeViewData,
    out: MutableList<DisplayTokenLineRange>,
) {
    val sourceStart = sourceLineRange.tokenBeginIndex
    val sourceEndExclusive = sourceLineRange.tokenBeginIndex + sourceLineRange.tokenCount
    var lineStart = sourceStart
    var lineUnits = 0

    fun appendLine(endExclusive: Int) {
        if (endExclusive <= lineStart) {
            return
        }
        out +=
            DisplayTokenLineRange(
                lineIndex = out.size,
                tokenBeginIndex = lineStart,
                tokenCount = endExclusive - lineStart,
                sourceLineIndex = sourceLineRange.lineIndex,
                coversFullSourceLine = lineStart == sourceStart && endExclusive == sourceEndExclusive,
            )
        lineStart = endExclusive
        lineUnits = 0
    }

    for (tokenIndex in sourceStart until sourceEndExclusive) {
        val tokenText = followData.textTokens[tokenIndex]
        val separatorUnits = displaySeparatorUnitsBeforeToken(followData.textTokens, tokenIndex, lineStart)
        val tokenUnits = tokenText.displayLineUnitCount()
        val projectedUnits = lineUnits + separatorUnits + tokenUnits

        // Punctuation tokens should finish the current display line; otherwise a split can
        // leave commas or sentence endings stranded at the start of the next UI line.
        if (
            lineUnits > 0 &&
            projectedUnits > DisplayLinePreferredUnits &&
            !shouldAttachDisplayTokenToPreviousLine(tokenText)
        ) {
            appendLine(tokenIndex)
        }

        lineUnits += displaySeparatorUnitsBeforeToken(followData.textTokens, tokenIndex, lineStart)
        lineUnits += tokenUnits

        val tailCodePoint = tokenText.codePointBeforeOrNull(tokenText.length)
        val nextTokenExists = tokenIndex + 1 < sourceEndExclusive
        val shouldBreakAfterToken =
            nextTokenExists &&
                (
                    tailCodePoint?.let(::isStrongDisplayLineBreakCodePoint) == true ||
                        (
                            lineUnits >= DisplayLinePreferredUnits &&
                                tailCodePoint?.let(::isWeakDisplayLineBreakCodePoint) == true
                        ) ||
                        lineUnits >= DisplayLineHardUnits
                )
        if (shouldBreakAfterToken) {
            appendLine(tokenIndex + 1)
        }
    }
    appendLine(sourceEndExclusive)
}

private fun displaySeparatorUnitsBeforeToken(
    textTokens: List<String>,
    tokenIndex: Int,
    lineStart: Int,
): Int =
    if (tokenIndex > lineStart && shouldInsertTokenSeparator(textTokens[tokenIndex - 1], textTokens[tokenIndex])) {
        1
    } else {
        0
    }

internal fun resolveActiveDisplayLineIndex(
    displayLineRanges: List<DisplayTokenLineRange>,
    activeTokenIndex: Int,
): Int =
    displayLineRanges
        .indexOfFirst { lineRange ->
            activeTokenIndex >= lineRange.tokenBeginIndex &&
                activeTokenIndex < lineRange.tokenBeginIndex + lineRange.tokenCount
        }.takeIf { it >= 0 } ?: -1

private fun DisplayTokenLineRange.exactLyricLineText(followData: PayloadFollowViewData): String? =
    if (coversFullSourceLine) {
        followData.lyricLines.getOrNull(sourceLineIndex)
    } else {
        null
    }

internal fun resolveContinuousViewportLine(
    followData: PayloadFollowViewData,
    activeTokenIndex: Int,
): ContinuousViewportLine? {
    if (
        followData.lyricLineFollowAvailable &&
        followData.lineTokenRanges.isNotEmpty() &&
        activeTokenIndex in 0 until followData.textTokens.size
    ) {
        val activeLineRange =
            followData.lineTokenRanges.firstOrNull { lineRange ->
                activeTokenIndex >= lineRange.tokenBeginIndex &&
                    activeTokenIndex < lineRange.tokenBeginIndex + lineRange.tokenCount
            }
        if (activeLineRange != null) {
            val start = activeLineRange.tokenBeginIndex.coerceIn(0, followData.textTokens.lastIndex)
            val endExclusive =
                (activeLineRange.tokenBeginIndex + activeLineRange.tokenCount).coerceIn(
                    start + 1,
                    followData.textTokens.size,
                )
            return resolveContinuousViewportLineForRange(
                followData = followData,
                tokenRange = start until endExclusive,
                lyricLineText = followData.lyricLines.getOrNull(activeLineRange.lineIndex),
            )
        }
    }

    val tokenRange =
        resolveActiveTokenLineRange(
            lineTokenRanges = followData.lineTokenRanges,
            lyricLineFollowAvailable = followData.lyricLineFollowAvailable,
            activeTokenIndex = activeTokenIndex,
            tokenCount = followData.textTokens.size,
        ) ?: return null
    return resolveContinuousViewportLineForRange(followData, tokenRange)
}

internal fun resolveContinuousViewportLineForRange(
    followData: PayloadFollowViewData,
    tokenRange: IntRange,
    lyricLineText: String? = null,
): ContinuousViewportLine {
    val exactLineModel = lyricLineText?.let { resolveExactLyricViewportLine(followData, tokenRange, it) }
    if (exactLineModel != null) {
        return exactLineModel
    }

    val textBuilder = StringBuilder()
    val tokenSegments = ArrayList<ContinuousViewportTokenSegment>()
    var shouldWrap = false
    tokenRange.forEachIndexed { indexInLine, tokenIndex ->
        val tokenText = followData.textTokens[tokenIndex]
        if (indexInLine > 0 && shouldInsertTokenSeparator(followData.textTokens[tokenIndex - 1], tokenText)) {
            textBuilder.append(' ')
        }
        val start = textBuilder.length
        textBuilder.append(tokenText)
        val endExclusive = textBuilder.length
        shouldWrap = shouldWrap || tokenText.containsCjkCodePoint()
        tokenSegments +=
            ContinuousViewportTokenSegment(
                tokenIndex = tokenIndex,
                start = start,
                endExclusive = endExclusive,
            )
    }
    return ContinuousViewportLine(
        text = textBuilder.toString(),
        tokenSegments = tokenSegments,
        shouldWrap = shouldWrap || textBuilder.toString().displayLineUnitCount() > DisplayLineHardUnits,
    )
}

internal fun resolveExactLyricViewportLine(
    followData: PayloadFollowViewData,
    tokenRange: IntRange,
    lyricLineText: String,
): ContinuousViewportLine? {
    val tokenSegments = ArrayList<ContinuousViewportTokenSegment>()
    var searchStart = 0
    var shouldWrap = false
    for (tokenIndex in tokenRange) {
        val tokenText = followData.textTokens[tokenIndex]
        val start = lyricLineText.indexOf(tokenText, startIndex = searchStart)
        if (start < 0) {
            return null
        }
        val endExclusive = start + tokenText.length
        shouldWrap = shouldWrap || tokenText.containsCjkCodePoint()
        tokenSegments +=
            ContinuousViewportTokenSegment(
                tokenIndex = tokenIndex,
                start = start,
                endExclusive = endExclusive,
            )
        searchStart = endExclusive
    }
    return ContinuousViewportLine(
        text = lyricLineText,
        tokenSegments = tokenSegments,
        shouldWrap = shouldWrap || lyricLineText.displayLineUnitCount() > DisplayLineHardUnits,
    )
}

internal fun shouldSweepContinuousSegment(
    activeTimelineEntry: TextFollowTimelineEntry?,
    activeBounds: TokenPixelBounds?,
    visibleViewportWidthPx: Float,
): Boolean {
    if (activeTimelineEntry == null || activeTimelineEntry.sampleCount <= 0 || activeBounds == null) {
        return false
    }
    return activeBounds.widthPx > visibleViewportWidthPx
}

internal fun shouldInsertTokenSeparator(
    previousToken: String,
    currentToken: String,
): Boolean {
    val previousCodePoint = previousToken.codePointBeforeOrNull(previousToken.length) ?: return false
    val currentCodePoint = currentToken.codePointAtOrNull(0) ?: return false
    if (isCjkCodePoint(previousCodePoint) || isCjkCodePoint(currentCodePoint)) {
        return false
    }
    if (isGluePunctuationCodePoint(previousCodePoint) || isGluePunctuationCodePoint(currentCodePoint)) {
        return false
    }
    if (isPunctuationCodePoint(currentCodePoint)) {
        return false
    }
    if (isOpeningPunctuationCodePoint(previousCodePoint)) {
        return false
    }
    return true
}

internal fun shouldAttachDisplayTokenToPreviousLine(tokenText: String): Boolean {
    val firstCodePoint = tokenText.codePointAtOrNull(0) ?: return false
    return isPunctuationCodePoint(firstCodePoint) || isGluePunctuationCodePoint(firstCodePoint)
}

internal fun String.codePointAtOrNull(index: Int): Int? =
    if (index !in indices) {
        null
    } else {
        codePointAt(index)
    }

internal fun String.codePointBeforeOrNull(index: Int): Int? =
    if (isEmpty() || index <= 0 || index > length) {
        null
    } else {
        codePointBefore(index)
    }

internal fun isCjkCodePoint(codePoint: Int): Boolean {
    val script = Character.UnicodeScript.of(codePoint)
    return script == Character.UnicodeScript.HAN ||
        script == Character.UnicodeScript.HIRAGANA ||
        script == Character.UnicodeScript.KATAKANA ||
        script == Character.UnicodeScript.HANGUL
}

internal fun isPunctuationCodePoint(codePoint: Int): Boolean =
    when (Character.getType(codePoint)) {
        Character.CONNECTOR_PUNCTUATION.toInt(),
        Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
        Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt(),
        -> true

        else -> false
    }

internal fun isOpeningPunctuationCodePoint(codePoint: Int): Boolean =
    when (Character.getType(codePoint)) {
        Character.START_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
        -> true

        else -> false
    }

internal fun isGluePunctuationCodePoint(codePoint: Int): Boolean =
    when (codePoint) {
        0x0027, // '
        0x002D, // hyphen-minus
        0x02BC, // modifier apostrophe
        0x2010, // hyphen
        0x2011, // non-breaking hyphen
        0x2019, // right single quotation mark / apostrophe
        0xFF07, // fullwidth apostrophe
        -> true

        else -> false
    }

internal fun String.containsCjkCodePoint(): Boolean {
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        if (isCjkCodePoint(codePoint)) {
            return true
        }
        index += Character.charCount(codePoint)
    }
    return false
}

internal fun String.displayLineUnitCount(): Int {
    var index = 0
    var units = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        units += if (isCjkCodePoint(codePoint)) 2 else 1
        index += Character.charCount(codePoint)
    }
    return units
}

internal fun isStrongDisplayLineBreakCodePoint(codePoint: Int): Boolean =
    when (codePoint) {
        '.'.code,
        '!'.code,
        '?'.code,
        ';'.code,
        ':'.code,
        0x3002,
        0xFF01,
        0xFF1F,
        0xFF1B,
        0xFF1A,
        -> true

        else -> false
    }

internal fun isWeakDisplayLineBreakCodePoint(codePoint: Int): Boolean =
    when (codePoint) {
        ','.code,
        0x3001,
        0xFF0C,
        -> true

        else -> false
    }

internal fun targetContinuousViewportTranslationPx(
    displayedSamples: Int,
    activeTimelineEntry: TextFollowTimelineEntry?,
    activeSegment: ContinuousViewportTokenSegment?,
    activeBounds: TokenPixelBounds?,
    viewportWidthPx: Float,
    visibleViewportWidthPx: Float,
    contentWidthPx: Float,
    horizontalPaddingPx: Float,
    fadeWidthPx: Float,
): Float {
    if (activeSegment == null || activeBounds == null || contentWidthPx <= viewportWidthPx) {
        return 0f
    }
    val visibleViewportStartPx = horizontalPaddingPx + fadeWidthPx
    val minTranslation = viewportWidthPx - contentWidthPx

    if (activeTimelineEntry == null || activeTimelineEntry.sampleCount <= 0 || activeBounds.widthPx <= visibleViewportWidthPx) {
        val tokenCenter = activeBounds.startPx + activeBounds.widthPx / 2f
        val anchorX = visibleViewportStartPx + visibleViewportWidthPx * TokenTapeAnchorRatio
        return (anchorX - tokenCenter).coerceIn(minTranslation, 0f)
    }

    val progress =
        ((displayedSamples - activeTimelineEntry.startSample).toFloat() / activeTimelineEntry.sampleCount.toFloat())
            .coerceIn(0f, 1f)
    val tokenSweepRangePx = (activeBounds.widthPx - visibleViewportWidthPx).coerceAtLeast(0f)
    val desiredVisibleStartInToken = tokenSweepRangePx * progress
    val targetVisibleStartInText = activeBounds.startPx + desiredVisibleStartInToken
    return (visibleViewportStartPx - targetVisibleStartInText).coerceIn(minTranslation, 0f)
}
