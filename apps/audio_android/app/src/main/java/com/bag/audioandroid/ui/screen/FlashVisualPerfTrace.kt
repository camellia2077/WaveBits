package com.bag.audioandroid.ui.screen

import android.util.Log
import com.bag.audioandroid.ui.state.FlashVisualWindowState

internal object FlashVisualPerfTrace {
    private const val Tag = "FlashVisualPerf"
    private const val ReportIntervalNanos = 1_000_000_000L
    private const val LargeVisualPxStepThresholdPx = 24f

    private var windowStartNanos = 0L
    private var composeCount = 0
    private var drawCount = 0
    private var windowRequestCount = 0
    private var windowBuildCount = 0
    private var windowActiveJobSkipCount = 0
    private var drawDurationTotalNanos = 0L
    private var drawDurationMaxNanos = 0L
    private var motionSampleRateHz = 0
    private var motionFrameCount = 0
    private var motionStepCount = 0
    private var rawPositionUpdateCount = 0
    private var rawStepTotalSamples = 0f
    private var rawStepMaxSamples = 0f
    private var smoothStepTotalSamples = 0f
    private var smoothStepMaxSamples = 0f
    private var visualPxStepTotal = 0f
    private var visualPxStepMax = 0f
    private var largePxStepCount = 0
    private var smoothResetCount = 0
    private var anchorJumpMaxSamples = 0f
    private var windowShiftMaxSamples = 0f
    private var viewportStartStepMaxSamples = 0f
    private var intervalStartDisplayedSample: Float? = null
    private var previousRawMotionSample: Float? = null
    private var previousSmoothMotionSample: Float? = null
    private var previousMotionWindowStartSample: Int? = null
    private var previousViewportStartSample: Float? = null
    private var lastMode = "unknown"
    private var lastIsPlaying = false
    private var lastDisplayedSample = 0f
    private var lastRawSample = 0f
    private var lastSmoothSample = 0f
    private var lastVisualErrorMs = 0f
    private var lastDrawableSegments = 0
    private var lastExactSegments = 0
    private var lastPrimitiveEstimate = 0
    private var lastVisibleSegments = 0
    private var lastVisiblePrimitiveEstimate = 0
    private var lastBuckets = 0
    private var lastFixedTimeline = false
    private var lastFallbackTimeline = false
    private var lastBitReadout = false
    private var lastWindowStart = 0
    private var lastWindowEnd = 0
    private var lastWindowSamples = 0
    private var lastDistanceToWindowStart = 0f
    private var lastDistanceToWindowEnd = 0f
    private var lastComfortablyInside = false
    private var lastTotalSamples = 0
    private var lastReadoutSample = 0f
    private var lastReadoutCurrentBitOffset: Int? = null
    private var lastReadoutRevealedBitOffset = -1
    private var lastReadoutGroupStart = -1
    private var lastReadoutPreviousBits = ""
    private var lastReadoutCurrentBits = ""
    private var lastVisualBitOffset: Int? = null
    private var lastRawBitOffset: Int? = null
    private var latestSnapshot = FlashVisualPerfSnapshot()
    private var enabled = false

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            latestSnapshot = FlashVisualPerfSnapshot()
        }
    }

    fun recordCompose(
        mode: FlashSignalVisualizationMode,
        isPlaying: Boolean,
        displayedSample: Float,
        drawableSegments: Int,
        exactSegments: Int,
        primitiveEstimate: Int,
        buckets: Int,
        hasFixedTimeline: Boolean,
        usesFallbackTimeline: Boolean,
        hasBitReadout: Boolean,
        windowSamples: Int,
        totalSamples: Int,
        windowStartSample: Int,
        windowEndSample: Int,
    ) {
        if (!enabled) {
            return
        }
        composeCount += 1
        captureFrame(
            mode = mode,
            isPlaying = isPlaying,
            displayedSample = displayedSample,
            drawableSegments = drawableSegments,
            exactSegments = exactSegments,
            primitiveEstimate = primitiveEstimate,
            buckets = buckets,
            hasFixedTimeline = hasFixedTimeline,
            usesFallbackTimeline = usesFallbackTimeline,
            hasBitReadout = hasBitReadout,
            windowSamples = windowSamples,
            totalSamples = totalSamples,
            windowStartSample = windowStartSample,
            windowEndSample = windowEndSample,
        )
        maybeReport()
    }

    fun recordDraw(
        mode: FlashSignalVisualizationMode,
        isPlaying: Boolean,
        displayedSample: Float,
        drawableSegments: Int,
        exactSegments: Int,
        primitiveEstimate: Int,
        visibleSegments: Int,
        visiblePrimitiveEstimate: Int,
        drawDurationNanos: Long,
        buckets: Int,
        hasFixedTimeline: Boolean,
        usesFallbackTimeline: Boolean,
        hasBitReadout: Boolean,
        windowSamples: Int,
        totalSamples: Int,
        windowStartSample: Int,
        windowEndSample: Int,
    ) {
        if (!enabled) {
            return
        }
        drawCount += 1
        drawDurationTotalNanos += drawDurationNanos
        drawDurationMaxNanos = maxOf(drawDurationMaxNanos, drawDurationNanos)
        captureFrame(
            mode = mode,
            isPlaying = isPlaying,
            displayedSample = displayedSample,
            drawableSegments = drawableSegments,
            exactSegments = exactSegments,
            primitiveEstimate = primitiveEstimate,
            visibleSegments = visibleSegments,
            visiblePrimitiveEstimate = visiblePrimitiveEstimate,
            buckets = buckets,
            hasFixedTimeline = hasFixedTimeline,
            usesFallbackTimeline = usesFallbackTimeline,
            hasBitReadout = hasBitReadout,
            windowSamples = windowSamples,
            totalSamples = totalSamples,
            windowStartSample = windowStartSample,
            windowEndSample = windowEndSample,
        )
        maybeReport()
    }

    fun recordWindowActiveJobSkip() {
        if (!enabled) {
            return
        }
        windowActiveJobSkipCount += 1
        maybeReport()
    }

    fun recordWindowRequest(
        sample: Int,
        currentWindow: FlashVisualWindowState,
        comfortablyInside: Boolean,
    ) {
        if (!enabled) {
            return
        }
        windowRequestCount += 1
        captureWindow(
            displayedSample = sample.toFloat(),
            windowStartSample = currentWindow.startSample,
            windowEndSample = currentWindow.endSampleExclusive,
            comfortablyInside = comfortablyInside,
        )
        maybeReport()
    }

    fun recordWindowBuilt(
        displayedSample: Int,
        exactSegments: Int,
        drawableSegments: Int,
        windowStart: Int,
        windowEnd: Int,
        windowSamples: Int,
        totalSamples: Int,
    ) {
        if (!enabled) {
            return
        }
        windowBuildCount += 1
        lastExactSegments = exactSegments
        lastDrawableSegments = drawableSegments
        lastWindowStart = windowStart
        lastWindowEnd = windowEnd
        lastWindowSamples = windowSamples
        lastTotalSamples = totalSamples
        captureWindow(
            displayedSample = displayedSample.toFloat(),
            windowStartSample = windowStart,
            windowEndSample = windowEnd,
            comfortablyInside = true,
        )
        maybeReport()
    }

    fun recordMotion(
        rawSample: Float,
        smoothSample: Float,
        sampleRateHz: Int,
        viewportWidthPx: Float,
        viewportSamples: Int,
        windowStartSample: Int,
        viewportStartSample: Float,
    ) {
        if (!enabled || sampleRateHz <= 0 || viewportSamples <= 0) {
            return
        }
        motionSampleRateHz = sampleRateHz
        motionFrameCount += 1
        lastRawSample = rawSample
        lastSmoothSample = smoothSample
        lastVisualErrorMs = samplesToMs(rawSample - smoothSample, sampleRateHz).toFloat()

        previousRawMotionSample?.let { previousRaw ->
            val rawStep = kotlin.math.abs(rawSample - previousRaw)
            if (rawStep > 0.5f) {
                rawPositionUpdateCount += 1
                rawStepTotalSamples += rawStep
                rawStepMaxSamples = maxOf(rawStepMaxSamples, rawStep)
                anchorJumpMaxSamples = maxOf(anchorJumpMaxSamples, rawStep)
            }
        }
        previousSmoothMotionSample?.let { previousSmooth ->
            val smoothStep = kotlin.math.abs(smoothSample - previousSmooth)
            val pxStep = smoothStep / viewportSamples.toFloat() * viewportWidthPx.coerceAtLeast(1f)
            motionStepCount += 1
            smoothStepTotalSamples += smoothStep
            smoothStepMaxSamples = maxOf(smoothStepMaxSamples, smoothStep)
            visualPxStepTotal += pxStep
            visualPxStepMax = maxOf(visualPxStepMax, pxStep)
            if (pxStep > LargeVisualPxStepThresholdPx) {
                largePxStepCount += 1
            }
        }
        previousMotionWindowStartSample?.let { previousWindowStart ->
            windowShiftMaxSamples =
                maxOf(windowShiftMaxSamples, kotlin.math.abs(windowStartSample - previousWindowStart).toFloat())
        }
        previousViewportStartSample?.let { previousViewportStart ->
            viewportStartStepMaxSamples =
                maxOf(viewportStartStepMaxSamples, kotlin.math.abs(viewportStartSample - previousViewportStart))
        }
        previousRawMotionSample = rawSample
        previousSmoothMotionSample = smoothSample
        previousMotionWindowStartSample = windowStartSample
        previousViewportStartSample = viewportStartSample
    }

    fun recordSmoothReset(
        anchorSample: Float,
        previousSmoothSample: Float,
        sampleRateHz: Int,
    ) {
        if (!enabled || sampleRateHz <= 0) {
            return
        }
        smoothResetCount += 1
        anchorJumpMaxSamples = maxOf(anchorJumpMaxSamples, kotlin.math.abs(anchorSample - previousSmoothSample))
    }

    fun recordBitReadout(
        readoutSample: Float,
        currentBitOffset: Int?,
        revealedBitOffset: Int,
        groupStart: Int,
        previousBits: String,
        currentBits: String,
        visualBitOffset: Int?,
        rawBitOffset: Int?,
    ) {
        if (!enabled) {
            return
        }
        lastReadoutSample = readoutSample
        lastReadoutCurrentBitOffset = currentBitOffset
        lastReadoutRevealedBitOffset = revealedBitOffset
        lastReadoutGroupStart = groupStart
        lastReadoutPreviousBits = previousBits
        lastReadoutCurrentBits = currentBits
        lastVisualBitOffset = visualBitOffset
        lastRawBitOffset = rawBitOffset
    }

    private fun captureFrame(
        mode: FlashSignalVisualizationMode,
        isPlaying: Boolean,
        displayedSample: Float,
        drawableSegments: Int,
        exactSegments: Int,
        primitiveEstimate: Int,
        visibleSegments: Int = lastVisibleSegments,
        visiblePrimitiveEstimate: Int = lastVisiblePrimitiveEstimate,
        buckets: Int,
        hasFixedTimeline: Boolean,
        usesFallbackTimeline: Boolean,
        hasBitReadout: Boolean,
        windowSamples: Int,
        totalSamples: Int,
        windowStartSample: Int,
        windowEndSample: Int,
    ) {
        lastMode = mode.name
        lastIsPlaying = isPlaying
        if (intervalStartDisplayedSample == null) {
            intervalStartDisplayedSample = displayedSample
        }
        lastDisplayedSample = displayedSample
        lastDrawableSegments = drawableSegments
        lastExactSegments = exactSegments
        lastPrimitiveEstimate = primitiveEstimate
        lastVisibleSegments = visibleSegments
        lastVisiblePrimitiveEstimate = visiblePrimitiveEstimate
        lastBuckets = buckets
        lastFixedTimeline = hasFixedTimeline
        lastFallbackTimeline = usesFallbackTimeline
        lastBitReadout = hasBitReadout
        lastWindowSamples = windowSamples
        lastTotalSamples = totalSamples
        captureWindow(
            displayedSample = displayedSample,
            windowStartSample = windowStartSample,
            windowEndSample = windowEndSample,
            comfortablyInside = displayedSample >= windowStartSample && displayedSample < windowEndSample,
        )
    }

    private fun captureWindow(
        displayedSample: Float,
        windowStartSample: Int,
        windowEndSample: Int,
        comfortablyInside: Boolean,
    ) {
        lastWindowStart = windowStartSample
        lastWindowEnd = windowEndSample
        lastWindowSamples = (windowEndSample - windowStartSample).coerceAtLeast(0)
        lastDistanceToWindowStart = displayedSample - windowStartSample
        lastDistanceToWindowEnd = windowEndSample - displayedSample
        lastComfortablyInside = comfortablyInside
    }

    private fun maybeReport() {
        val now = System.nanoTime()
        if (windowStartNanos == 0L) {
            windowStartNanos = now
            return
        }
        val elapsed = now - windowStartNanos
        if (elapsed < ReportIntervalNanos) {
            return
        }
        val seconds = elapsed / 1_000_000_000.0
        val displayedSampleDelta =
            intervalStartDisplayedSample
                ?.let { lastDisplayedSample - it }
                ?: 0f
        val drawAvgMs = if (drawCount == 0) 0.0 else drawDurationTotalNanos / drawCount / 1_000_000.0
        val drawMaxMs = drawDurationMaxNanos / 1_000_000.0
        val drawRate = drawCount / seconds
        val rawStepAvgMs =
            if (rawPositionUpdateCount == 0) {
                0.0
            } else {
                samplesToMs(rawStepTotalSamples / rawPositionUpdateCount, motionSampleRateHz)
            }
        val rawStepMaxMs = samplesToMs(rawStepMaxSamples, motionSampleRateHz)
        val smoothStepAvgMs =
            if (motionStepCount == 0) {
                0.0
            } else {
                samplesToMs(smoothStepTotalSamples / motionStepCount, motionSampleRateHz)
            }
        val smoothStepMaxMs = samplesToMs(smoothStepMaxSamples, motionSampleRateHz)
        val visualPxStepAvg =
            if (motionStepCount == 0) {
                0.0
            } else {
                visualPxStepTotal / motionStepCount
            }
        val anchorJumpMaxMs = samplesToMs(anchorJumpMaxSamples, motionSampleRateHz)
        val windowShiftMaxMs = samplesToMs(windowShiftMaxSamples, motionSampleRateHz)
        val viewportStartStepMaxMs = samplesToMs(viewportStartStepMaxSamples, motionSampleRateHz)
        latestSnapshot =
            FlashVisualPerfSnapshot(
                drawFps = drawRate.toFloat(),
                drawAvgMs = drawAvgMs.toFloat(),
                drawMaxMs = drawMaxMs.toFloat(),
                visibleSegments = lastVisibleSegments,
                visiblePrimitives = lastVisiblePrimitiveEstimate,
                rawUpdatesPerSecond = (rawPositionUpdateCount / seconds).toFloat(),
                rawStepAvgMs = rawStepAvgMs.toFloat(),
                rawStepMaxMs = rawStepMaxMs.toFloat(),
                smoothStepAvgMs = smoothStepAvgMs.toFloat(),
                smoothStepMaxMs = smoothStepMaxMs.toFloat(),
                visualErrorMs = lastVisualErrorMs,
                visualPxStepAvg = visualPxStepAvg.toFloat(),
                visualPxStepMax = visualPxStepMax,
                anchorJumpMaxMs = anchorJumpMaxMs.toFloat(),
                smoothResetCount = smoothResetCount,
                windowShiftMaxMs = windowShiftMaxMs.toFloat(),
                viewportStartStepMaxMs = viewportStartStepMaxMs.toFloat(),
                largePxStepCount = largePxStepCount,
            )
        logDebug(
            "mode=$lastMode playing=$lastIsPlaying compose/s=${rate(composeCount, seconds)} draw/s=${rate(drawRate)} " +
                "windowReq/s=${rate(windowRequestCount, seconds)} windowBuild/s=${rate(windowBuildCount, seconds)} " +
                "windowBusySkip/s=${rate(windowActiveJobSkipCount, seconds)} " +
                "displayed=${lastDisplayedSample.toInt()} displayedDelta/s=${rate(displayedSampleDelta, seconds)} " +
                "drawable=$lastDrawableSegments exact=$lastExactSegments primitives=$lastPrimitiveEstimate " +
                "visible=$lastVisibleSegments visiblePrimitives=$lastVisiblePrimitiveEstimate " +
                "drawAvgMs=${ms(drawAvgMs)} drawMaxMs=${ms(drawMaxMs)} " +
                "raw=${lastRawSample.toInt()} smooth=${lastSmoothSample.toInt()} " +
                "rawUpdate/s=${rate(rawPositionUpdateCount, seconds)} rawStepMs=${ms(rawStepAvgMs)} rawStepMaxMs=${ms(rawStepMaxMs)} " +
                "smoothStepMs=${ms(smoothStepAvgMs)} smoothStepMaxMs=${ms(smoothStepMaxMs)} " +
                "visualErrorMs=${ms(
                    lastVisualErrorMs.toDouble(),
                )} pxStep=${ms(visualPxStepAvg.toDouble())} pxStepMax=${ms(visualPxStepMax.toDouble())} " +
                "anchorJumpMaxMs=${ms(anchorJumpMaxMs)} smoothReset=$smoothResetCount " +
                "windowShiftMaxMs=${ms(
                    windowShiftMaxMs,
                )} viewportStartStepMaxMs=${ms(viewportStartStepMaxMs)} largePxStep=$largePxStepCount " +
                "buckets=$lastBuckets fixed=$lastFixedTimeline fallback=$lastFallbackTimeline " +
                "bitReadout=$lastBitReadout window=[$lastWindowStart,$lastWindowEnd) " +
                "readoutSample=${lastReadoutSample.toInt()} readoutBit=${lastReadoutCurrentBitOffset ?: -1} " +
                "revealedBit=$lastReadoutRevealedBitOffset readoutGroup=$lastReadoutGroupStart " +
                "readoutPrev=$lastReadoutPreviousBits readoutCurrent=$lastReadoutCurrentBits " +
                "visualBit=${lastVisualBitOffset ?: -1} rawBit=${lastRawBitOffset ?: -1} " +
                "distStart=${lastDistanceToWindowStart.toInt()} distEnd=${lastDistanceToWindowEnd.toInt()} " +
                "inside=$lastComfortablyInside windowSamples=$lastWindowSamples totalSamples=$lastTotalSamples",
        )
        windowStartNanos = now
        intervalStartDisplayedSample = lastDisplayedSample
        composeCount = 0
        drawCount = 0
        windowRequestCount = 0
        windowBuildCount = 0
        windowActiveJobSkipCount = 0
        drawDurationTotalNanos = 0L
        drawDurationMaxNanos = 0L
        motionFrameCount = 0
        motionStepCount = 0
        rawPositionUpdateCount = 0
        rawStepTotalSamples = 0f
        rawStepMaxSamples = 0f
        smoothStepTotalSamples = 0f
        smoothStepMaxSamples = 0f
        visualPxStepTotal = 0f
        visualPxStepMax = 0f
        largePxStepCount = 0
        smoothResetCount = 0
        anchorJumpMaxSamples = 0f
        windowShiftMaxSamples = 0f
        viewportStartStepMaxSamples = 0f
    }

    private fun rate(
        count: Int,
        seconds: Double,
    ): String = "%.1f".format(count / seconds)

    private fun rate(value: Double): String = "%.1f".format(value)

    private fun rate(
        count: Float,
        seconds: Double,
    ): String = "%.1f".format(count / seconds)

    private fun ms(value: Double): String = "%.2f".format(value)

    private fun samplesToMs(
        samples: Float,
        sampleRateHz: Int,
    ): Double =
        if (sampleRateHz <= 0) {
            0.0
        } else {
            samples.toDouble() / sampleRateHz.toDouble() * 1000.0
        }

    private fun logDebug(message: String) {
        try {
            Log.d(Tag, message)
        } catch (_: Throwable) {
            // JVM unit tests use the Android stub jar, where Log.d is not implemented.
        }
    }

    fun snapshot(): FlashVisualPerfSnapshot =
        if (enabled) {
            latestSnapshot
        } else {
            FlashVisualPerfSnapshot()
        }
}

internal data class FlashVisualPerfSnapshot(
    val drawFps: Float = 0f,
    val drawAvgMs: Float = 0f,
    val drawMaxMs: Float = 0f,
    val visibleSegments: Int = 0,
    val visiblePrimitives: Int = 0,
    val rawUpdatesPerSecond: Float = 0f,
    val rawStepAvgMs: Float = 0f,
    val rawStepMaxMs: Float = 0f,
    val smoothStepAvgMs: Float = 0f,
    val smoothStepMaxMs: Float = 0f,
    val visualErrorMs: Float = 0f,
    val visualPxStepAvg: Float = 0f,
    val visualPxStepMax: Float = 0f,
    val anchorJumpMaxMs: Float = 0f,
    val smoothResetCount: Int = 0,
    val windowShiftMaxMs: Float = 0f,
    val viewportStartStepMaxMs: Float = 0f,
    val largePxStepCount: Int = 0,
)
