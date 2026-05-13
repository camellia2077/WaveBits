package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

// Controls only the Flash Visual viewport density. Playback progress and audio
// timeline semantics stay based on absolute samples.
private const val FlashSignalViewportSeconds = 0.80f

@Composable
internal fun AudioFlashSignalVisualizer(
    input: FlashSignalVisualizationInput,
    isPlaying: Boolean,
    mode: FlashSignalVisualizationMode,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    sharedPlaybackSampleState: FlashVisualPlaybackSampleState? = null,
    showPerfOverlay: Boolean = false,
    playbackSpeed: Float = 1f,
    isScrubbing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pcm = input.pcm
    val sampleRateHz = input.sampleRateHz
    if (pcm.isEmpty() || sampleRateHz <= 0) {
        return
    }

    val visualTokens = appThemeVisualTokens()
    SideEffect {
        FlashVisualPerfTrace.setEnabled(showPerfOverlay)
    }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (showPerfOverlay) 230.dp else 170.dp),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val targetBucketCount =
            remember(widthPx) {
                val bucketSpacingPx = with(density) { 6.dp.toPx() }
                ceil((widthPx / bucketSpacingPx).toDouble())
                    .toInt()
                    .coerceIn(FlashSignalMinBucketCount, FlashSignalMaxBucketCount)
            }
        val windowSampleCount =
            remember(sampleRateHz) {
                (sampleRateHz.coerceAtLeast(1) * FlashSignalViewportSeconds)
                    .roundToInt()
                    .coerceAtLeast(1)
            }
        val renderState =
            rememberFlashSignalVisualizerRenderState(
                input = input,
                isPlaying = isPlaying,
                mode = mode,
                flashVoicingStyle = flashVoicingStyle,
                flashVisualWindow = flashVisualWindow,
                sharedPlaybackSampleState = sharedPlaybackSampleState,
                playbackSpeed = playbackSpeed,
                isScrubbing = isScrubbing,
                targetBucketCount = targetBucketCount,
                windowSampleCount = windowSampleCount,
            )
        val activeToneColor = MaterialTheme.colorScheme.primary
        val inactiveToneColor = visualTokens.visualizationInactiveToneColor
        val glowColor = MaterialTheme.colorScheme.onPrimaryContainer
        val baseBackground = visualTokens.visualizationBaseBackgroundColor
        val centerLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        FlashVisualPerfTrace.recordCompose(
            mode = mode,
            isPlaying = isPlaying,
            displayedSample = renderState.playbackSampleState.displayedSample,
            drawableSegments = renderState.visualSegments.size,
            exactSegments = renderState.fixedTimelineFrame?.segments?.size ?: 0,
            primitiveEstimate = renderState.primitiveEstimate,
            buckets = renderState.buckets.size,
            hasFixedTimeline = renderState.fixedTimelineFrame != null,
            usesFallbackTimeline = renderState.usesFallbackTimeline,
            hasBitReadout = renderState.bitReadoutFrame != null,
            windowSamples = renderState.traceWindowSamples,
            totalSamples = renderState.totalSamples,
            windowStartSample = renderState.traceWindowStartSample,
            windowEndSample = renderState.traceWindowEndSample,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FlashSignalCanvas(
                mode = mode,
                isPlaying = isPlaying,
                renderState = renderState,
                sampleRateHz = sampleRateHz,
                windowSampleCount = windowSampleCount,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                glowColor = glowColor,
                baseBackground = baseBackground,
                centerLineColor = centerLineColor,
                showPerfOverlay = showPerfOverlay,
                modifier = Modifier.fillMaxWidth(),
            )
            if (showPerfOverlay) {
                FlashVisualFpsOverlay(
                    snapshot = FlashVisualPerfTrace.snapshot(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                )
            }
            if (renderState.bitReadoutFrame != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FlashBitReadoutRow(
                        cells = renderState.bitReadoutFrame.previousCells,
                        activeToneColor = activeToneColor,
                        baseBackground = baseBackground,
                        isPreviousRow = true,
                    )
                    FlashBitReadoutRow(
                        cells = renderState.bitReadoutFrame.currentCells,
                        activeToneColor = activeToneColor,
                        baseBackground = baseBackground,
                        isPreviousRow = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashSignalCanvas(
    mode: FlashSignalVisualizationMode,
    isPlaying: Boolean,
    renderState: FlashSignalVisualizerRenderState,
    sampleRateHz: Int,
    windowSampleCount: Int,
    activeToneColor: Color,
    inactiveToneColor: Color,
    glowColor: Color,
    baseBackground: Color,
    centerLineColor: Color,
    showPerfOverlay: Boolean,
    modifier: Modifier = Modifier,
) {
    val followDisplayedSamplePosition = renderState.playbackSampleState.displayedSample
    val runtimeState =
        rememberFlashSignalCanvasRuntimeState(
            followDisplayedSamplePosition = followDisplayedSamplePosition,
            rawSample = renderState.playbackSampleState.rawSample,
            followData = renderState.followData,
            bitReadoutSource = renderState.bitReadoutSource,
            bitReadoutFrame = renderState.bitReadoutFrame,
        )
    val visualTransition = rememberInfiniteTransition(label = "flashSignalCanvas")
    val glowPulseAnimated by visualTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "flashSignalGlowPulse",
    )
    val sweepAnimated by visualTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "flashSignalSweep",
    )
    val glowPulse = if (isPlaying) glowPulseAnimated else 0.82f
    val sweepPhase = if (isPlaying) sweepAnimated else 0.24f
    val ambientBrush =
        Brush.horizontalGradient(
            colors =
                listOf(
                    inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                    activeToneColor.copy(alpha = 0.12f + 0.02f * sweepPhase),
                    inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                ),
        )
    val fixedViewportState =
        renderState.fixedTimelineFrame?.let {
            val windowStart = followDisplayedSamplePosition - windowSampleCount * FlashSignalPlayheadAnchorRatio
            FlashSignalViewport(
                startSample = windowStart,
                endSample = windowStart + windowSampleCount,
                playheadSample = followDisplayedSamplePosition,
            )
        }

    Box(modifier = modifier.height(112.dp)) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(112.dp),
        ) {
            if (renderState.buckets.isEmpty() && renderState.fixedTimelineFrame == null) {
                return@Canvas
            }

            val corner = CornerRadius(24f, 24f)
            val leftPadding = 12.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 12.dp.toPx()
            val bottomPadding = 12.dp.toPx()
            val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val bucketWidth = if (renderState.buckets.isNotEmpty()) innerWidth / renderState.buckets.size.toFloat() else 1f
            val analysisBucketSampleWidth =
                if (renderState.buckets.isNotEmpty()) {
                    windowSampleCount.toFloat() / renderState.buckets.size.toFloat()
                } else {
                    1f
                }
            val bucketOffset =
                if (renderState.buckets.isNotEmpty()) {
                    (
                        (renderState.bucketFrame.displayedSamplePosition - renderState.bucketFrame.analysisDisplayedSamplePosition) /
                            analysisBucketSampleWidth
                    ).coerceIn(-FlashSignalMaxVisualBucketOffset, FlashSignalMaxVisualBucketOffset)
                } else {
                    0f
                }
            val scanHeadBucketIndex =
                if (renderState.buckets.isNotEmpty()) {
                    (
                        renderState.buckets.size * FlashSignalPlayheadAnchorRatio
                    ).coerceIn(0f, renderState.buckets.lastIndex.toFloat())
                } else {
                    0f
                }
            val activeThresholdBucketIndex =
                if (renderState.buckets.isNotEmpty()) {
                    (scanHeadBucketIndex + bucketOffset).coerceIn(0f, renderState.buckets.lastIndex.toFloat())
                } else {
                    0f
                }
            val fixedViewport = fixedViewportState
            val telemetryState = runtimeState.telemetryState
            if (renderState.bitReadoutFrame != null && renderState.followData != null) {
                FlashVisualPerfTrace.recordBitReadout(
                    readoutSample = renderState.bitReadoutSample,
                    currentBitOffset = telemetryState.currentReadoutBit,
                    revealedBitOffset = telemetryState.revealedBitOffset,
                    groupStart = renderState.bitReadoutFrame.currentGroupStartIndex,
                    previousBits = renderState.bitReadoutFrame.previousBitsText(),
                    currentBits = renderState.bitReadoutFrame.currentBitsText(),
                    visualBitOffset = telemetryState.currentVisualBit,
                    rawBitOffset = telemetryState.currentRawBit,
                )
            }
            renderState.followData?.let { data ->
                FlashAlignmentPerfTrace.recordLyrics(
                    isPlaying = isPlaying,
                    sample = followDisplayedSamplePosition.toInt(),
                    state =
                        flashAlignmentLyricsState(
                            followData = data,
                            displayedSamples = followDisplayedSamplePosition.toInt(),
                        ),
                )
            }
            FlashAlignmentPerfTrace.recordVisual(
                mode = mode,
                isPlaying = isPlaying,
                smoothSample = followDisplayedSamplePosition,
                rawSample = renderState.playbackSampleState.rawSample,
                readoutSample = renderState.bitReadoutSample,
                readoutBit = telemetryState.currentReadoutBit,
                readoutBitValue = telemetryState.currentReadoutBitValue,
                revealedBit = telemetryState.revealedBitOffset,
                visualBit = telemetryState.currentVisualBit,
                rawBit = telemetryState.currentRawBit,
                visualBitValue = runtimeState.laneActiveBitState?.bitValue,
                usesFallbackTimeline = renderState.usesFallbackTimeline,
                hasBitReadout = renderState.bitReadoutFrame != null,
            )
            FlashVisualPerfTrace.recordMotion(
                rawSample = renderState.playbackSampleState.rawSample,
                smoothSample = followDisplayedSamplePosition,
                sampleRateHz = sampleRateHz,
                viewportWidthPx = innerWidth,
                viewportSamples = windowSampleCount,
                windowStartSample = renderState.traceWindowStartSample,
                viewportStartSample = fixedViewport?.startSample ?: 0f,
            )
            val visibleSegmentCount =
                fixedViewport
                    ?.let { viewport -> renderState.visualSegments.count { segment -> segment.overlaps(viewport) } }
                    ?: 0
            val visiblePrimitiveEstimate =
                flashVisualPrimitiveEstimate(
                    mode = mode,
                    drawableSegments = visibleSegmentCount,
                    buckets = renderState.buckets.size,
                    hasFixedTimeline = renderState.fixedTimelineFrame != null,
                )
            val drawStartNanos = System.nanoTime()

            drawRoundRect(
                color = baseBackground,
                size = size,
                cornerRadius = corner,
            )
            drawRoundRect(
                brush = ambientBrush,
                size = size,
                cornerRadius = corner,
            )

            when (mode) {
                FlashSignalVisualizationMode.Lanes ->
                    if (renderState.fixedTimelineFrame != null && fixedViewport != null) {
                        drawBitCellSegments(
                            segments = renderState.visualSegments,
                            viewport = fixedViewport,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                            enableViewportEdgeFade = renderState.enableViewportEdgeFade,
                        )
                    } else {
                        drawBitCells(
                            buckets = renderState.buckets,
                            activeThresholdBucketIndex = activeThresholdBucketIndex,
                            activeWindowBucketCount = renderState.activeWindowBucketCount,
                            bucketOffset = bucketOffset,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            bucketWidth = bucketWidth,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                        )
                    }

                FlashSignalVisualizationMode.Pitch ->
                    if (renderState.fixedTimelineFrame != null && fixedViewport != null) {
                        drawPitchSegments(
                            segments = renderState.visualSegments,
                            viewport = fixedViewport,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                            enableViewportEdgeFade = renderState.enableViewportEdgeFade,
                        )
                    } else {
                        drawPitch(
                            buckets = renderState.buckets,
                            activeThresholdBucketIndex = activeThresholdBucketIndex,
                            activeWindowBucketCount = renderState.activeWindowBucketCount,
                            bucketOffset = bucketOffset,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            bucketWidth = bucketWidth,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                        )
                    }

                FlashSignalVisualizationMode.Pulse -> Unit
            }
            val drawDurationNanos = System.nanoTime() - drawStartNanos
            FlashVisualPerfTrace.recordDraw(
                mode = mode,
                isPlaying = isPlaying,
                displayedSample = followDisplayedSamplePosition,
                drawableSegments = renderState.visualSegments.size,
                exactSegments = renderState.fixedTimelineFrame?.segments?.size ?: 0,
                primitiveEstimate = renderState.primitiveEstimate,
                visibleSegments = visibleSegmentCount,
                visiblePrimitiveEstimate = visiblePrimitiveEstimate,
                drawDurationNanos = drawDurationNanos,
                buckets = renderState.buckets.size,
                hasFixedTimeline = renderState.fixedTimelineFrame != null,
                usesFallbackTimeline = renderState.usesFallbackTimeline,
                hasBitReadout = renderState.bitReadoutFrame != null,
                windowSamples = renderState.traceWindowSamples,
                totalSamples = renderState.totalSamples,
                windowStartSample = renderState.traceWindowStartSample,
                windowEndSample = renderState.traceWindowEndSample,
            )
        }
        if (mode == FlashSignalVisualizationMode.Pulse && runtimeState.pulseTapeState != null) {
            FlashPulseTapeOverlay(
                tapeState = runtimeState.pulseTapeState,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (showPerfOverlay &&
            mode == FlashSignalVisualizationMode.Lanes &&
            runtimeState.laneActiveBitState != null &&
            fixedViewportState != null
        ) {
            FlashLaneBitBoundaryOverlay(
                viewport = fixedViewportState,
                activeBit = runtimeState.laneActiveBitState,
                layout = flashVisualPlayheadLayout(mode),
                color = glowColor,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("flash-visual-lanes-alignment-overlay"),
            )
        }
        FlashVisualPlayheadOverlay(
            layout = flashVisualPlayheadLayout(mode),
            color = glowColor,
            modifier = Modifier.fillMaxSize(),
        )
        if (showPerfOverlay && mode == FlashSignalVisualizationMode.Lanes) {
            FlashLaneAlignmentSummaryOverlay(
                laneActiveBit = runtimeState.laneActiveBitState,
                readoutBitOffset = runtimeState.telemetryState.currentReadoutBit ?: -1,
                readoutBitValue = runtimeState.telemetryState.currentReadoutBitValue,
                tokenState = runtimeState.tokenAlignmentState,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp)
                        .testTag("flash-visual-lanes-alignment-summary"),
            )
        }
    }
}

@Composable
private fun FlashLaneBitBoundaryOverlay(
    viewport: FlashSignalViewport,
    activeBit: FlashLaneActiveBitState,
    layout: FlashVisualPlayheadLayout,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val leftPadding = layout.leadingPadding.toPx()
        val rightPadding = layout.trailingPadding.toPx()
        val topPadding = layout.topPadding.toPx()
        val bottomPadding = layout.bottomPadding.toPx()
        val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
        val startX =
            flashOverlaySampleToViewportX(
                sample = activeBit.startSample,
                viewport = viewport,
                leftPadding = leftPadding,
                innerWidth = innerWidth,
            ).coerceIn(leftPadding, size.width - rightPadding)
        val endX =
            flashOverlaySampleToViewportX(
                sample = activeBit.endSample,
                viewport = viewport,
                leftPadding = leftPadding,
                innerWidth = innerWidth,
            ).coerceIn(leftPadding, size.width - rightPadding)
        val playheadX =
            flashVisualPlayheadX(
                totalWidthPx = size.width,
                leftPaddingPx = leftPadding,
                rightPaddingPx = rightPadding,
            )
        val touchThreshold = 1.dp.toPx()
        val boundaryColor =
            if (abs(playheadX - startX) <= touchThreshold) {
                color.copy(alpha = 0.94f)
            } else {
                color.copy(alpha = 0.42f)
            }
        val overlayHeight = size.height - topPadding - bottomPadding

        drawLine(
            color = boundaryColor,
            start = Offset(startX, topPadding),
            end = Offset(startX, size.height - bottomPadding),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = color.copy(alpha = 0.22f),
            start = Offset(endX, topPadding),
            end = Offset(endX, size.height - bottomPadding),
            strokeWidth = 1.dp.toPx(),
        )
        drawRect(
            color = color.copy(alpha = 0.10f),
            topLeft = Offset(startX, topPadding),
            size = Size((endX - startX).coerceAtLeast(1f), overlayHeight),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun FlashLaneAlignmentSummaryOverlay(
    laneActiveBit: FlashLaneActiveBitState?,
    readoutBitOffset: Int,
    readoutBitValue: Char?,
    tokenState: FlashTokenAlignmentState?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.88f),
        contentColor = Color.Black,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text =
                buildString {
                    append("lane ")
                    append(bitLabel(laneActiveBit?.bitOffset ?: -1, laneActiveBit?.bitValue))
                    append("  row ")
                    append(bitLabel(readoutBitOffset, readoutBitValue))
                    append("  token ")
                    append(bitLabel(tokenState?.globalBitOffset ?: -1, tokenState?.currentBitValue))
                    tokenState?.takeIf { it.byteHex.isNotBlank() || it.byteBinary.isNotBlank() }?.let { state ->
                        append('\n')
                        append("hex ")
                        append(state.byteHex.ifBlank { "_" })
                        append("  bin ")
                        append(state.byteBinary.ifBlank { "_" })
                    }
                },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun bitLabel(
    bitOffset: Int,
    bitValue: Char?,
): String =
    if (bitOffset < 0) {
        "_"
    } else {
        "$bitOffset:${bitValue ?: '_'}"
    }

private fun flashOverlaySampleToViewportX(
    sample: Float,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    innerWidth: Float,
): Float = leftPadding + ((sample - viewport.startSample) / viewport.sampleCount) * innerWidth

@Composable
private fun FlashPulseTapeOverlay(
    tapeState: FlashPulseTapeState,
    activeToneColor: Color,
    inactiveToneColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .padding(
                    horizontal = FlashPulseOverlayHorizontalPadding,
                    vertical = FlashPulseOverlayVerticalPadding,
                ).clipToBounds()
                .testTag("flash-visual-pulse-tape"),
    ) {
        val density = LocalDensity.current
        LaunchedEffect(maxWidth, maxHeight) {
            val overlayWidthDp = maxWidth.value
            val overlayHeightDp = maxHeight.value
            val upperTrackCenterDp = overlayHeightDp * 0.22f
            val lowerTrackCenterDp = overlayHeightDp * 0.78f
            Log.d(
                "PlaybackPulseLayout",
                "overlayWidthDp=${"%.1f".format(overlayWidthDp)} overlayHeightDp=${"%.1f".format(overlayHeightDp)} " +
                    "waveWidthDp=${"%.1f".format(overlayWidthDp)} upperTrackCenterDp=${"%.1f".format(upperTrackCenterDp)} " +
                    "lowerTrackCenterDp=${"%.1f".format(lowerTrackCenterDp)} visibleSegments=$FlashPulseVisibleCellCount",
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeDp = with(density) { 3.dp.toPx() }
            val activeStrokeDp = with(density) { 5.dp.toPx() }
            val overlayHorizontalPaddingPx = with(density) { FlashPulseOverlayHorizontalPadding.toPx() }
            val upperCenterY = size.height * 0.22f
            val lowerCenterY = size.height * 0.78f
            val guideColor = inactiveToneColor.copy(alpha = 0.18f)
            val segmentWidth = size.width / FlashPulseVisibleCellCount.coerceAtLeast(1)
            val fullCardWidth = size.width + overlayHorizontalPaddingPx * 2f
            val anchorX =
                flashVisualPlayheadX(
                    totalWidthPx = fullCardWidth,
                    leftPaddingPx = 12.dp.toPx(),
                    rightPaddingPx = 12.dp.toPx(),
                ) - overlayHorizontalPaddingPx
            val activeIndex = FlashPulseVisibleCellCount / 2
            val inactiveStroke = strokeDp * 1.15f
            val activeStroke = activeStrokeDp * 1.2f

            drawLine(
                color = guideColor,
                start = Offset(0f, upperCenterY),
                end = Offset(size.width, upperCenterY),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = guideColor,
                start = Offset(0f, lowerCenterY),
                end = Offset(size.width, lowerCenterY),
                strokeWidth = 1.dp.toPx(),
            )

            tapeState.cells.forEachIndexed { index, cell ->
                val xStart =
                    anchorX +
                        (index - activeIndex - tapeState.currentBitProgress) * segmentWidth
                val xEnd = xStart + segmentWidth
                val y =
                    when (cell.bit) {
                        '1' -> upperCenterY
                        '0' -> lowerCenterY
                        else -> null
                    } ?: return@forEachIndexed
                val color =
                    when {
                        cell.isActive -> activeToneColor.copy(alpha = 0.92f)
                        cell.isRevealed -> inactiveToneColor.copy(alpha = 0.54f)
                        else -> inactiveToneColor.copy(alpha = 0.24f)
                    }
                if (cell.isActive) {
                    drawLine(
                        color = activeToneColor.copy(alpha = 0.10f),
                        start = Offset(xStart, y),
                        end = Offset(xEnd, y),
                        strokeWidth = 8.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    )
                }
                drawLine(
                    color = color,
                    start = Offset(xStart, y),
                    end = Offset(xEnd, y),
                    strokeWidth = if (cell.isActive) activeStroke else inactiveStroke,
                    cap = if (cell.isActive) androidx.compose.ui.graphics.StrokeCap.Butt else androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun FlashVisualPlayheadOverlay(
    layout: FlashVisualPlayheadLayout,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val leftPadding = layout.leadingPadding.toPx()
        val rightPadding = layout.trailingPadding.toPx()
        val topPadding = layout.topPadding.toPx()
        val bottomPadding = layout.bottomPadding.toPx()
        val playheadX =
            flashVisualPlayheadX(
                totalWidthPx = size.width,
                leftPaddingPx = leftPadding,
                rightPaddingPx = rightPadding,
            )

        drawLine(
            color = color.copy(alpha = 0.80f),
            start = Offset(playheadX, topPadding),
            end = Offset(playheadX, size.height - bottomPadding),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

@Composable
private fun FlashVisualFpsOverlay(
    snapshot: FlashVisualPerfSnapshot,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        contentColor = Color.Black,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text =
                "FPS ${snapshot.drawFps.toInt()}  " +
                    "avg ${"%.1f".format(snapshot.drawAvgMs)}ms  " +
                    "max ${"%.1f".format(snapshot.drawMaxMs)}ms\n" +
                    "raw ${"%.1f".format(snapshot.rawUpdatesPerSecond)}/s  " +
                    "step ${"%.1f".format(snapshot.rawStepAvgMs)}ms  " +
                    "max ${"%.1f".format(snapshot.rawStepMaxMs)}ms\n" +
                    "smooth ${"%.1f".format(snapshot.smoothStepAvgMs)}ms  " +
                    "err ${"%.1f".format(snapshot.visualErrorMs)}ms  " +
                    "px ${"%.2f".format(snapshot.visualPxStepAvg)}/${"%.2f".format(snapshot.visualPxStepMax)}\n" +
                    "jump ${"%.1f".format(snapshot.anchorJumpMaxMs)}ms  " +
                    "reset ${snapshot.smoothResetCount}  " +
                    "vp ${"%.1f".format(snapshot.viewportStartStepMaxMs)}ms",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun FlashBitReadoutRow(
    cells: List<FlashBitReadoutCell>,
    activeToneColor: androidx.compose.ui.graphics.Color,
    baseBackground: androidx.compose.ui.graphics.Color,
    isPreviousRow: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        cells.forEach { cell ->
            val cellBackground =
                when {
                    cell.isCurrent -> activeToneColor.copy(alpha = 0.18f)
                    cell.bit != null && isPreviousRow -> baseBackground.copy(alpha = 0.40f)
                    cell.bit != null -> baseBackground.copy(alpha = 0.52f)
                    else -> baseBackground.copy(alpha = 0.24f)
                }
            val cellColor =
                when {
                    cell.isCurrent -> activeToneColor.copy(alpha = 0.94f)
                    cell.bit != null && isPreviousRow -> activeToneColor.copy(alpha = 0.48f)
                    cell.bit != null -> activeToneColor.copy(alpha = 0.72f)
                    else -> activeToneColor.copy(alpha = 0f)
                }
            Text(
                text = cell.bit?.toString().orEmpty(),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(
                            color = cellBackground,
                            shape = RoundedCornerShape(4.dp),
                        ).padding(vertical = 1.dp),
                color = cellColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                fontWeight = if (cell.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val FlashSignalMinBucketCount = 56
private const val FlashSignalMaxBucketCount = 124
private const val FlashSignalMaxVisualBucketOffset = 3f
private val FlashPulseOverlayHorizontalPadding = 18.dp
private val FlashPulseOverlayVerticalPadding = 18.dp

internal data class FlashVisualPlayheadLayout(
    val leadingPadding: Dp,
    val trailingPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
)

internal fun flashVisualPlayheadLayout(mode: FlashSignalVisualizationMode): FlashVisualPlayheadLayout =
    FlashVisualPlayheadLayout(
        leadingPadding = 12.dp,
        trailingPadding = 12.dp,
        topPadding = 12.dp,
        bottomPadding = 12.dp,
    )

internal fun flashVisualPlayheadX(
    totalWidthPx: Float,
    leftPaddingPx: Float,
    rightPaddingPx: Float,
): Float {
    val innerWidth = (totalWidthPx - leftPaddingPx - rightPaddingPx).coerceAtLeast(1f)
    return leftPaddingPx + innerWidth * FlashSignalPlayheadAnchorRatio
}

internal data class FlashLaneActiveBitState(
    val bitOffset: Int,
    val bitValue: Char?,
    val startSample: Float,
    val endSample: Float,
)

internal fun flashLaneActiveBitState(
    entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    bitByOffset: Map<Int, Char>,
    sample: Float,
): FlashLaneActiveBitState? {
    val activeEntry =
        entries.firstOrNull { entry ->
            sample >= entry.startSample && sample < entry.startSample + entry.sampleCount
        } ?: return null
    if (activeEntry.bitCount <= 0 || activeEntry.sampleCount <= 0) {
        return null
    }
    val bitProgress = activeEntry.bitProgressAtSample(sample)
    val bitIndexWithinEntry = bitProgress.toInt().coerceIn(0, activeEntry.bitCount - 1)
    val bitOffset = activeEntry.bitOffset + bitIndexWithinEntry
    val bitSampleWidth = activeEntry.sampleCount.toFloat() / activeEntry.bitCount.toFloat()
    val bitStartSample = activeEntry.startSample + bitSampleWidth * bitIndexWithinEntry.toFloat()
    val bitEndSample = bitStartSample + bitSampleWidth
    return FlashLaneActiveBitState(
        bitOffset = bitOffset,
        bitValue = bitByOffset[bitOffset],
        startSample = bitStartSample,
        endSample = bitEndSample,
    )
}

internal data class FlashTokenAlignmentState(
    val activeTokenIndex: Int,
    val activeByteIndexWithinToken: Int,
    val activeBitIndexWithinByte: Int,
    val globalBitOffset: Int,
    val currentBitValue: Char?,
    val byteHex: String,
    val byteBinary: String,
)

internal fun flashTokenAlignmentState(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
): FlashTokenAlignmentState {
    val rawDisplayUnitsByToken = followData.textRawDisplayUnits.groupBy { it.tokenIndex }
    val activeTokenIndex = activeTextTimelineIndex(followData.textTokenTimeline, displayedSamples)
    val activeByteIndexWithinToken =
        activeByteIndexWithinToken(
            activeTextIndex = activeTokenIndex,
            displayedSamples = displayedSamples,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    val activeBitPosition =
        activeBitPositionWithinByte(
            activeTextIndex = activeTokenIndex,
            activeByteIndexWithinToken = activeByteIndexWithinToken,
            displayedSamples = displayedSamples,
            followData = followData,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    val activeUnit =
        rawDisplayUnitsByToken[activeTokenIndex]
            .orEmpty()
            .firstOrNull { it.byteIndexWithinToken == activeByteIndexWithinToken }
    val globalBitOffset =
        activeUnit
            ?.takeIf { activeBitPosition.bitIndexWithinByte >= 0 }
            ?.let { it.byteOffset * 8 + activeBitPosition.bitIndexWithinByte }
            ?: -1
    val byteBinary = activeUnit?.binaryText?.filter { it == '0' || it == '1' }.orEmpty()
    return FlashTokenAlignmentState(
        activeTokenIndex = activeTokenIndex,
        activeByteIndexWithinToken = activeByteIndexWithinToken,
        activeBitIndexWithinByte = activeBitPosition.bitIndexWithinByte,
        globalBitOffset = globalBitOffset,
        currentBitValue = byteBinary.getOrNull(activeBitPosition.bitIndexWithinByte),
        byteHex = activeUnit?.hexText.orEmpty(),
        byteBinary = byteBinary,
    )
}
