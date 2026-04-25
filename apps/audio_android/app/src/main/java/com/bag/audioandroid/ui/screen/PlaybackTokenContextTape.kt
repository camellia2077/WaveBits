package com.bag.audioandroid.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
private val TokenTapeHorizontalPadding = 16.dp
private val TokenTapeVerticalPadding = 6.dp
private val TokenTapeFadeWidth = 28.dp

internal data class ContinuousViewportTokenSegment(
    val tokenIndex: Int,
    val start: Int,
    val endExclusive: Int,
)

internal data class ContinuousViewportLine(
    val text: String,
    val tokenSegments: List<ContinuousViewportTokenSegment>,
)

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
    
    val lineRanges = followData.lineTokenRanges.ifEmpty {
        listOf(TextFollowLineTokenRangeViewData(0, 0, followData.textTokens.size))
    }

    val activeTokenLineRange = remember(lineRanges, followData.lyricLineFollowAvailable, activeTokenIndex, followData.textTokens.size) {
        resolveActiveTokenLineRange(
            lineTokenRanges = lineRanges,
            lyricLineFollowAvailable = followData.lyricLineFollowAvailable,
            activeTokenIndex = activeTokenIndex,
            tokenCount = followData.textTokens.size,
        )
    }
    val activeLineIndex = remember(lineRanges, activeTokenLineRange) {
        lineRanges.indexOfFirst { lineRange ->
            activeTokenLineRange?.first == lineRange.tokenBeginIndex &&
                activeTokenLineRange.last == lineRange.tokenBeginIndex + lineRange.tokenCount - 1
        }.takeIf { it >= 0 } ?: -1
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Smoothly scroll the list so the active line moves to the center spot
    androidx.compose.runtime.LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            listState.animateScrollToItem(activeLineIndex)
        }
    }

    // A single line's height is roughly 36.dp (24.sp line height + 6.dp vertical padding * 2).
    // Using an inter-line spacing of 4.dp gives us a compact display.
    val singleLineHeightDp = 36f
    val spacingDp = 4f
    val totalHeightDp = singleLineHeightDp * visibleLineCount + spacingDp * (visibleLineCount - 1)
    val verticalPaddingDp = (totalHeightDp - singleLineHeightDp) / 2f

    // A fixed-height LazyColumn with userScrollEnabled = false prevents nested scrolling 
    // conflicts with the outer parent's verticalScroll. 
    // The large vertical padding ensures the scrolled-to active item naturally sits in the center.
    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(spacingDp.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = verticalPaddingDp.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeightDp.dp)
            .testTag("playback-token-context-tape-list"),
    ) {
        items(lineRanges.size) { lineIndex ->
            val lineRange = lineRanges[lineIndex]
            val isActiveLine = lineIndex == activeLineIndex
            val lineTokenRange = lineRange.tokenBeginIndex until (lineRange.tokenBeginIndex + lineRange.tokenCount)
            val lineModel = remember(followData.textTokens, lineRange) {
                resolveContinuousViewportLineForRange(followData, lineTokenRange)
            }

            // Fade out previous/future lines to create a Karaoke focus effect
            // We use a stepped alpha based on distance from the active line
            val distance = kotlin.math.abs(lineIndex - activeLineIndex)
            val targetAlpha = when (distance) {
                0 -> 1.0f
                1 -> 0.5f
                else -> 0.2f
            }
            val alpha by animateFloatAsState(
                targetValue = targetAlpha,
                label = "context_tape_line_alpha"
            )

            PlaybackTokenContextTapeLine(
                lineModel = lineModel,
                activeTokenIndex = activeTokenIndex,
                activeTimelineEntry = if (isActiveLine) activeTimelineEntry else null,
                displayedSamples = displayedSamples,
                modifier = Modifier.alpha(alpha)
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
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
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

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .clipToBounds(),
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val horizontalPaddingPx = with(density) { TokenTapeHorizontalPadding.toPx() }
        val fadeWidthPx = with(density) { TokenTapeFadeWidth.toPx() }
        val visibleViewportWidthPx =
            (viewportWidthPx - 2f * horizontalPaddingPx - 2f * fadeWidthPx).coerceAtLeast(1f)
        val activeSegment = lineModel.tokenSegments.firstOrNull { it.tokenIndex == activeTokenIndex }
        val activeBounds = activeSegment?.let { tokenPixelBounds[it.tokenIndex] }
        val targetTranslationPx =
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
        val shouldSweepWithinActiveToken =
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
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            onTextLayout = { layoutResult ->
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
                    .padding(horizontal = TokenTapeHorizontalPadding, vertical = TokenTapeVerticalPadding)
                    .testTag("playback-token-context-active"),
        )

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
                    )
                    .testTag("playback-token-context-fade-left"),
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
                    )
                    .testTag("playback-token-context-fade-right"),
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

internal fun resolveContinuousViewportLine(
    followData: PayloadFollowViewData,
    activeTokenIndex: Int,
): ContinuousViewportLine? {
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
): ContinuousViewportLine {
    val textBuilder = StringBuilder()
    val tokenSegments = ArrayList<ContinuousViewportTokenSegment>()
    tokenRange.forEachIndexed { indexInLine, tokenIndex ->
        if (indexInLine > 0) {
            textBuilder.append(' ')
        }
        val start = textBuilder.length
        textBuilder.append(followData.textTokens[tokenIndex])
        val endExclusive = textBuilder.length
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
