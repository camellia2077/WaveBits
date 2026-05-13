package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.screen.FlashSignalPlayheadAnchorRatio
import com.bag.audioandroid.ui.screen.FlashSignalToneSegment
import com.bag.audioandroid.ui.screen.FlashVisualPerfTrace
import com.bag.audioandroid.ui.screen.FskDominantTone
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.FlashVisualWindowSource
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

internal class FlashVisualWindowActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val sessionStateStore: AudioSessionStateStore,
    private val workerDispatcher: CoroutineDispatcher,
) {
    private val jobs = mutableMapOf<TransportModeOption, Job>()
    private val pendingWindowRanges = mutableMapOf<TransportModeOption, FlashVisualWindowRange>()

    fun ensureCurrentWindow(
        mode: TransportModeOption,
        displayedSamples: Int,
    ) {
        if (mode != TransportModeOption.Flash) {
            return
        }
        val session = uiState.value.sessions[mode] ?: return
        val source = session.flashVisualWindowSource ?: return
        val sample = displayedSamples.coerceIn(0, source.totalPcmSampleCount)
        val comfortablyInside = session.flashVisualWindow.isComfortablyInside(sample)
        if (comfortablyInside) {
            return
        }
        val revision = session.generatedContentRevision
        val window = source.flashVisualWindowAround(sample)
        val requestedRange = window.toRange()
        val currentRange = session.flashVisualWindow.toRangeOrNull()
        if (requestedRange == currentRange || requestedRange == pendingWindowRanges[mode]) {
            return
        }
        FlashVisualPerfTrace.recordWindowRequest(
            sample = sample,
            currentWindow = session.flashVisualWindow,
            comfortablyInside = comfortablyInside,
        )
        if (jobs[mode]?.isActive == true) {
            FlashVisualPerfTrace.recordWindowActiveJobSkip()
            return
        }
        pendingWindowRanges[mode] = requestedRange
        jobs[mode] =
            scope.launch {
                val state = buildWindow(source, window)
                sessionStateStore.updateSession(mode) { current ->
                    if (current.generatedContentRevision != revision) {
                        current
                    } else {
                        current.copy(flashVisualWindow = state)
                    }
                }
                if (pendingWindowRanges[mode] == requestedRange) {
                    pendingWindowRanges.remove(mode)
                }
            }
    }

    private suspend fun buildWindow(
        source: FlashVisualWindowSource,
        window: FlashVisualWindowState,
    ): FlashVisualWindowState =
        withContext(workerDispatcher) {
            val exactSegments =
                source.segmentsForRange(
                    startSample = window.startSample,
                    endSampleExclusive = window.endSampleExclusive,
                )
            val drawableSegments =
                source.drawableSegmentsForRange(
                    startSample = window.startSample,
                    endSampleExclusive = window.endSampleExclusive,
                    maxSegments = source.maxDrawableSegmentsForWindow(window),
                )
            val state =
                window.copy(
                    segments = exactSegments,
                    drawableSegments = drawableSegments,
                )
            FlashVisualPerfTrace.recordWindowBuilt(
                displayedSample = state.displayedSamples,
                exactSegments = state.segments.size,
                drawableSegments = state.drawableSegments.size,
                windowStart = state.startSample,
                windowEnd = state.endSampleExclusive,
                windowSamples = state.endSampleExclusive - state.startSample,
                totalSamples = state.totalPcmSampleCount,
            )
            state
        }
}

internal fun FlashVisualWindowSource.flashVisualWindowAround(sample: Int): FlashVisualWindowState {
    val lookBehindSamples = (VisualViewportSampleCount * FlashSignalPlayheadAnchorRatio).toInt()
    val lookAheadSamples = (VisualViewportSampleCount * (1f - FlashSignalPlayheadAnchorRatio)).toInt()
    val anchorSample = sample.quantizeDown(VisualWindowAnchorStepSamples)
    val start = (anchorSample - lookBehindSamples - VisualWindowPaddingSamples).coerceAtLeast(0)
    val end = (anchorSample + lookAheadSamples + VisualWindowPaddingSamples).coerceAtMost(totalPcmSampleCount)
    val safeEnd = end.coerceAtLeast((start + 1).coerceAtMost(totalPcmSampleCount.coerceAtLeast(1)))
    val window =
        FlashVisualWindowState(
            segments = emptyList(),
            drawableSegments = emptyList(),
            startSample = start,
            endSampleExclusive = safeEnd,
            displayedSamples = anchorSample,
            totalPcmSampleCount = totalPcmSampleCount,
        )
    return window.copy(
        segments =
            segmentsForRange(
                startSample = window.startSample,
                endSampleExclusive = window.endSampleExclusive,
            ),
        drawableSegments =
            drawableSegmentsForRange(
                startSample = window.startSample,
                endSampleExclusive = window.endSampleExclusive,
                maxSegments = maxDrawableSegmentsForWindow(window),
            ),
        startSample = start,
        endSampleExclusive = safeEnd,
        displayedSamples = anchorSample,
        totalPcmSampleCount = totalPcmSampleCount,
    )
}

internal fun FlashVisualWindowSource.segmentsForRange(
    startSample: Int,
    endSampleExclusive: Int,
): List<FlashSignalToneSegment> {
    if (timelineSegments.isEmpty() || endSampleExclusive <= startSample) {
        return emptyList()
    }

    val firstIndex = firstSegmentEndingAfter(startSample)
    if (firstIndex >= timelineSegments.size) {
        return emptyList()
    }

    val selected = ArrayList<FlashSignalToneSegment>()
    var index = firstIndex
    while (index < timelineSegments.size) {
        val segment = timelineSegments[index]
        if (segment.startSample >= endSampleExclusive) {
            break
        }
        if (segment.endSample > startSample) {
            selected += segment
        }
        index += 1
    }
    return selected
}

internal fun FlashVisualWindowSource.drawableSegmentsForRange(
    startSample: Int,
    endSampleExclusive: Int,
    maxSegments: Int,
): List<FlashSignalToneSegment> {
    // Rendering must stay bounded even when the exact timeline is dense.
    return segmentsForRange(
        startSample = startSample,
        endSampleExclusive = endSampleExclusive,
    ).downsampleToBudget(maxSegments)
}

internal fun FlashVisualWindowSource.maxDrawableSegmentsForWindow(window: FlashVisualWindowState): Int {
    val viewportSamples = (window.endSampleExclusive - window.startSample).coerceAtLeast(1)
    val estimatedVisibleBits = (viewportSamples / FlashEstimatedSamplesPerBit).coerceAtLeast(1)
    // Lanes and Pulse expand each segment into multiple paint primitives,
    // so keep the drawable budget below the raw visible-bit estimate.
    return minOf(
        estimatedVisibleBits * FlashVisualSegmentsPerVisibleBit,
        FlashVisualMaxDrawableSegments,
    )
}

private fun FlashVisualWindowSource.firstSegmentEndingAfter(sample: Int): Int {
    var low = 0
    var high = timelineSegments.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (timelineSegments[mid].endSample <= sample) {
            low = mid + 1
        } else {
            high = mid
        }
    }
    return low
}

private fun List<FlashSignalToneSegment>.downsampleToBudget(maxSegments: Int): List<FlashSignalToneSegment> {
    if (size <= maxSegments || maxSegments <= 0) {
        return this
    }

    val stride = ceil(size.toDouble() / maxSegments.toDouble()).toInt().coerceAtLeast(1)
    val compacted = ArrayList<FlashSignalToneSegment>((size + stride - 1) / stride)
    var index = 0
    while (index < size) {
        val first = this[index]
        var endSample = first.endSample
        var highCount = 0
        var lowCount = 0
        var scan = index
        while (scan < size && scan < index + stride) {
            val segment = this[scan]
            endSample = segment.endSample
            when (segment.tone) {
                FskDominantTone.High -> highCount += 1
                FskDominantTone.Low -> lowCount += 1
                FskDominantTone.Unknown -> Unit
            }
            scan += 1
        }
        val tone =
            when {
                highCount > lowCount -> FskDominantTone.High
                lowCount > highCount -> FskDominantTone.Low
                else -> first.tone
            }
        compacted += first.copy(endSample = endSample, tone = tone)
        index += stride
    }
    return compacted
}

private data class FlashVisualWindowRange(
    val startSample: Int,
    val endSampleExclusive: Int,
)

private fun FlashVisualWindowState.toRange(): FlashVisualWindowRange =
    FlashVisualWindowRange(
        startSample = startSample,
        endSampleExclusive = endSampleExclusive,
    )

private fun FlashVisualWindowState.toRangeOrNull(): FlashVisualWindowRange? = takeIf { it.available }?.toRange()

private fun Int.quantizeDown(step: Int): Int =
    if (step <= 0) {
        this
    } else {
        (this / step) * step
    }

private const val VisualViewportSampleCount = 44_100
private const val VisualWindowAnchorStepSamples = 44_100
private const val VisualWindowPaddingSamples = 44_100 * 2
private const val FlashEstimatedSamplesPerBit = 1_024
private const val FlashVisualSegmentsPerVisibleBit = 2
private const val FlashVisualMaxDrawableSegments = 96
