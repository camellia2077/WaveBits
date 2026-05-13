package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import kotlin.math.abs

internal data class FlashSignalVisualizerRenderState(
    val buckets: List<FskEnergyBucket>,
    val bucketFrame: FlashSignalBucketFrame,
    val fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
    val visualSegments: List<FlashSignalToneSegment>,
    val playbackSampleState: FlashVisualPlaybackSampleState,
    val followData: PayloadFollowViewData?,
    val bitReadoutSource: FlashBitReadoutSource?,
    val bitReadoutFrame: FlashBitReadoutFrame?,
    val bitReadoutSample: Float,
    val activeWindowBucketCount: Int,
    val primitiveEstimate: Int,
    val usesFallbackTimeline: Boolean,
    val enableViewportEdgeFade: Boolean,
    val traceWindowSamples: Int,
    val traceWindowStartSample: Int,
    val traceWindowEndSample: Int,
    val totalSamples: Int,
)

@Composable
internal fun rememberFlashSignalVisualizerRenderState(
    input: FlashSignalVisualizationInput,
    isPlaying: Boolean,
    mode: FlashSignalVisualizationMode,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState,
    sharedPlaybackSampleState: FlashVisualPlaybackSampleState?,
    playbackSpeed: Float,
    isScrubbing: Boolean,
    targetBucketCount: Int,
    windowSampleCount: Int,
): FlashSignalVisualizerRenderState {
    val pcm = input.pcm
    val sampleRateHz = input.sampleRateHz
    val visualizerModel =
        rememberFlashSignalVisualizerModel(
            input = input,
            flashVisualWindow = flashVisualWindow,
        )
    val totalSamples = visualizerModel.totalSamples
    val followTimelineSource = visualizerModel.followTimelineSource
    val followTimelineTotalSamples = visualizerModel.followTimelineTotalSamples
    val displayedSamplePosition = visualizerModel.displayedSamplePosition
    val followDisplayedSamplePosition = visualizerModel.followDisplayedSamplePosition
    val windowedTimelineFrame = visualizerModel.windowedTimelineFrame
    val analysisCache = remember(pcm, sampleRateHz, input.bucketSource.stableCacheKey()) { FlashSignalAnalysisCache() }
    val analysisSampleStep =
        remember(sampleRateHz, totalSamples) {
            visualizationAnalysisSampleStep(sampleRateHz = sampleRateHz, totalSamples = totalSamples)
        }
    val analysisDisplayedSamplePosition =
        remember(displayedSamplePosition, analysisSampleStep, totalSamples) {
            quantizeVisualizationDisplayedSamples(
                displayedSamples = displayedSamplePosition,
                sampleStep = analysisSampleStep,
                totalSamples = totalSamples,
            )
        }
    val followAnalysisSampleStep =
        remember(sampleRateHz, followTimelineTotalSamples) {
            visualizationAnalysisSampleStep(sampleRateHz = sampleRateHz, totalSamples = followTimelineTotalSamples)
        }
    val activeWindowBucketCount =
        remember(flashVoicingStyle) {
            flashSignalActiveWindowBucketCount(flashVoicingStyle)
        }
    val hasWindowedTimelineFrame = windowedTimelineFrame != null && !isScrubbing
    val shouldUseTimelineFallback =
        remember(mode, hasWindowedTimelineFrame, input.bucketSource.stableTimelineKey(), isScrubbing) {
            when (mode) {
                FlashSignalVisualizationMode.Pulse -> !hasWindowedTimelineFrame
                FlashSignalVisualizationMode.Lanes,
                FlashSignalVisualizationMode.Pitch,
                -> true
            }
        }
    val fallbackTimelineFrame =
        remember(input.bucketSource.stableTimelineKey(), shouldUseTimelineFallback) {
            if (!shouldUseTimelineFallback) {
                return@remember null
            }
            (input.bucketSource as? FlashSignalBucketSource.FollowTimeline)
                ?.followData
                ?.toFixedTimelineFrameOrNull()
        }
    val fixedTimelineFrame = windowedTimelineFrame ?: fallbackTimelineFrame
    val visualSegments = fixedTimelineFrame?.segments.orEmpty()
    val usesFallbackTimeline = fallbackTimelineFrame != null && fixedTimelineFrame === fallbackTimelineFrame
    val visualTotalSamples = fixedTimelineFrame?.totalSamples ?: followTimelineTotalSamples
    val playbackSampleState =
        sharedPlaybackSampleState
            ?: rememberFlashVisualPlaybackSampleState(
                rawSample = if (isScrubbing) displayedSamplePosition else followDisplayedSamplePosition,
                isPlaying = isPlaying && !isScrubbing,
                playbackSpeed = playbackSpeed,
                sampleRateHz = sampleRateHz,
                totalSamples = visualTotalSamples,
            )
    val visualFollowDisplayedSamplePosition = playbackSampleState.displayedSample
    val visualFollowAnalysisDisplayedSamplePosition =
        remember(visualFollowDisplayedSamplePosition, followAnalysisSampleStep, followTimelineTotalSamples) {
            quantizeVisualizationDisplayedSamples(
                displayedSamples = visualFollowDisplayedSamplePosition,
                sampleStep = followAnalysisSampleStep,
                totalSamples = followTimelineTotalSamples,
            )
        }
    val followData = followTimelineSource?.followData
    val bitReadoutSource =
        remember(followData) {
            followData?.toFlashBitReadoutSource()
        }
    val bitReadoutFrame =
        bitReadoutSource?.let { source ->
            flashBitReadoutFrame(
                source = source,
                sample = visualFollowDisplayedSamplePosition,
            )
        }
    val bucketFrame =
        remember(
            pcm,
            sampleRateHz,
            input.bucketSource.stableCacheKey(),
            targetBucketCount,
            windowSampleCount,
            analysisDisplayedSamplePosition,
            visualFollowAnalysisDisplayedSamplePosition,
            fixedTimelineFrame,
        ) {
            if (fixedTimelineFrame != null) {
                return@remember FlashSignalBucketFrame.Empty
            }
            when (val bucketSource = input.bucketSource) {
                is FlashSignalBucketSource.FollowTimeline -> {
                    val followBuckets =
                        analysisCache
                            .followBuckets(
                                currentSample = visualFollowAnalysisDisplayedSamplePosition,
                                windowSampleCount = windowSampleCount,
                                targetBucketCount = targetBucketCount,
                            ) {
                                buildFskEnergyBucketsFromFollowData(
                                    followData = bucketSource.followData,
                                    currentSample = visualFollowAnalysisDisplayedSamplePosition,
                                    windowSampleCount = windowSampleCount,
                                    targetBucketCount = targetBucketCount,
                                )
                            }
                    if (followBuckets.isNotEmpty()) {
                        FlashSignalBucketFrame(
                            buckets = followBuckets,
                            displayedSamplePosition = visualFollowDisplayedSamplePosition,
                            analysisDisplayedSamplePosition = visualFollowAnalysisDisplayedSamplePosition,
                        )
                    } else {
                        FlashSignalBucketFrame(
                            buckets =
                                analysisCache.pcmBuckets(
                                    currentSample = analysisDisplayedSamplePosition,
                                    windowSampleCount = windowSampleCount,
                                    targetBucketCount = targetBucketCount,
                                ) {
                                    buildFskEnergyBuckets(
                                        pcm = pcm,
                                        sampleRateHz = sampleRateHz,
                                        currentSample = analysisDisplayedSamplePosition,
                                        windowSampleCount = windowSampleCount,
                                        targetBucketCount = targetBucketCount,
                                    )
                                },
                            displayedSamplePosition = displayedSamplePosition,
                            analysisDisplayedSamplePosition = analysisDisplayedSamplePosition,
                        )
                    }
                }

                is FlashSignalBucketSource.Pcm ->
                    FlashSignalBucketFrame(
                        buckets =
                            analysisCache.pcmBuckets(
                                currentSample = analysisDisplayedSamplePosition,
                                windowSampleCount = windowSampleCount,
                                targetBucketCount = targetBucketCount,
                            ) {
                                buildFskEnergyBuckets(
                                    pcm = pcm,
                                    sampleRateHz = sampleRateHz,
                                    currentSample = analysisDisplayedSamplePosition,
                                    windowSampleCount = windowSampleCount,
                                    targetBucketCount = targetBucketCount,
                                )
                            },
                        displayedSamplePosition = displayedSamplePosition,
                        analysisDisplayedSamplePosition = analysisDisplayedSamplePosition,
                    )
            }
        }
    val primitiveEstimate =
        flashVisualPrimitiveEstimate(
            mode = mode,
            drawableSegments = visualSegments.size,
            buckets = bucketFrame.buckets.size,
            hasFixedTimeline = fixedTimelineFrame != null,
        )
    return remember(
        bucketFrame,
        fixedTimelineFrame,
        visualSegments,
        playbackSampleState,
        followData,
        bitReadoutSource,
        bitReadoutFrame,
        visualFollowDisplayedSamplePosition,
        activeWindowBucketCount,
        primitiveEstimate,
        usesFallbackTimeline,
        flashVoicingStyle,
        flashVisualWindow,
        visualTotalSamples,
    ) {
        FlashSignalVisualizerRenderState(
            buckets = bucketFrame.buckets,
            bucketFrame = bucketFrame,
            fixedTimelineFrame = fixedTimelineFrame,
            visualSegments = visualSegments,
            playbackSampleState = playbackSampleState,
            followData = followData,
            bitReadoutSource = bitReadoutSource,
            bitReadoutFrame = bitReadoutFrame,
            bitReadoutSample = visualFollowDisplayedSamplePosition,
            activeWindowBucketCount = activeWindowBucketCount,
            primitiveEstimate = primitiveEstimate,
            usesFallbackTimeline = usesFallbackTimeline,
            enableViewportEdgeFade = flashVoicingStyle != FlashVoicingStyleOption.Litany,
            traceWindowSamples = flashVisualWindow.endSampleExclusive - flashVisualWindow.startSample,
            traceWindowStartSample = flashVisualWindow.startSample,
            traceWindowEndSample = flashVisualWindow.endSampleExclusive,
            totalSamples = visualTotalSamples,
        )
    }
}

internal data class FlashSignalCanvasRuntimeState(
    val pulseTapeState: FlashPulseTapeState?,
    val laneActiveBitState: FlashLaneActiveBitState?,
    val tokenAlignmentState: FlashTokenAlignmentState?,
    val telemetryState: FlashSignalCanvasTelemetryState,
)

internal data class FlashSignalCanvasTelemetryState(
    val currentReadoutBit: Int?,
    val currentReadoutBitValue: Char?,
    val revealedBitOffset: Int,
    val currentVisualBit: Int?,
    val currentRawBit: Int?,
)

@Composable
internal fun rememberFlashSignalCanvasRuntimeState(
    followDisplayedSamplePosition: Float,
    rawSample: Float,
    followData: PayloadFollowViewData?,
    bitReadoutSource: FlashBitReadoutSource?,
    bitReadoutFrame: FlashBitReadoutFrame?,
): FlashSignalCanvasRuntimeState {
    val pulseTapeState =
        remember(bitReadoutSource, followDisplayedSamplePosition) {
            bitReadoutSource?.let { source ->
                flashPulseTapeState(
                    source = source,
                    sample = followDisplayedSamplePosition,
                )
            }
        }
    val laneActiveBitState =
        remember(bitReadoutSource, followDisplayedSamplePosition) {
            bitReadoutSource?.let { source ->
                flashLaneActiveBitState(
                    entries = source.entries,
                    bitByOffset = source.bitByOffset,
                    sample = followDisplayedSamplePosition,
                )
            }
        }
    val tokenAlignmentState =
        remember(followData, followDisplayedSamplePosition) {
            followData?.let { data ->
                flashTokenAlignmentState(
                    followData = data,
                    displayedSamples = followDisplayedSamplePosition.toInt(),
                )
            }
        }
    val telemetryState =
        remember(bitReadoutFrame, bitReadoutSource, followDisplayedSamplePosition, rawSample) {
            val currentReadoutBit = bitReadoutFrame?.currentBitOffset
            FlashSignalCanvasTelemetryState(
                currentReadoutBit = currentReadoutBit,
                currentReadoutBitValue = currentReadoutBit?.let { bitReadoutSource?.bitByOffset?.get(it) },
                revealedBitOffset = bitReadoutFrame?.revealedBitOffset ?: -1,
                currentVisualBit = bitReadoutSource?.currentBitOffsetAtSample(followDisplayedSamplePosition),
                currentRawBit = bitReadoutSource?.currentBitOffsetAtSample(rawSample),
            )
        }
    return remember(
        pulseTapeState,
        laneActiveBitState,
        tokenAlignmentState,
        telemetryState,
    ) {
        FlashSignalCanvasRuntimeState(
            pulseTapeState = pulseTapeState,
            laneActiveBitState = laneActiveBitState,
            tokenAlignmentState = tokenAlignmentState,
            telemetryState = telemetryState,
        )
    }
}

private data class FlashSignalBucketSourceCacheKey(
    val source: String,
    val identity: Int,
    val timelineSize: Int,
    val totalSamples: Int,
)

private data class FlashSignalBucketSourceTimelineKey(
    val identity: Int,
    val timelineSize: Int,
    val totalSamples: Int,
)

private fun FlashSignalBucketSource.stableCacheKey(): FlashSignalBucketSourceCacheKey =
    when (this) {
        is FlashSignalBucketSource.Pcm ->
            FlashSignalBucketSourceCacheKey(
                source = "pcm",
                identity = 0,
                timelineSize = 0,
                totalSamples = 0,
            )

        is FlashSignalBucketSource.FollowTimeline ->
            FlashSignalBucketSourceCacheKey(
                source = "follow",
                identity = System.identityHashCode(followData),
                timelineSize = followData.binaryGroupTimeline.size,
                totalSamples = followData.totalPcmSampleCount,
            )
    }

private fun FlashSignalBucketSource.stableTimelineKey(): FlashSignalBucketSourceTimelineKey? =
    (this as? FlashSignalBucketSource.FollowTimeline)?.followData?.let { followData ->
        FlashSignalBucketSourceTimelineKey(
            identity = System.identityHashCode(followData),
            timelineSize = followData.binaryGroupTimeline.size,
            totalSamples = followData.totalPcmSampleCount,
        )
    }

private data class FlashSignalAnalysisCacheKey(
    val source: String,
    val currentSample: Int,
    val windowSampleCount: Int,
    val targetBucketCount: Int,
)

private class FlashSignalAnalysisCache {
    private val bucketsByKey = LinkedHashMap<FlashSignalAnalysisCacheKey, List<FskEnergyBucket>>()

    fun pcmBuckets(
        currentSample: Float,
        windowSampleCount: Int,
        targetBucketCount: Int,
        build: () -> List<FskEnergyBucket>,
    ): List<FskEnergyBucket> =
        bucketsFor(
            FlashSignalAnalysisCacheKey(
                source = "pcm",
                currentSample = currentSample.toInt(),
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount,
            ),
            build,
        )

    fun followBuckets(
        currentSample: Float,
        windowSampleCount: Int,
        targetBucketCount: Int,
        build: () -> List<FskEnergyBucket>,
    ): List<FskEnergyBucket> =
        bucketsFor(
            FlashSignalAnalysisCacheKey(
                source = "follow",
                currentSample = currentSample.toInt(),
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount,
            ),
            build,
        )

    private fun bucketsFor(
        key: FlashSignalAnalysisCacheKey,
        build: () -> List<FskEnergyBucket>,
    ): List<FskEnergyBucket> {
        bucketsByKey[key]?.let { return it }
        val buckets = build()
        bucketsByKey[key] = buckets
        if (bucketsByKey.size > FlashSignalAnalysisCacheMaxEntries) {
            val eldestKey = bucketsByKey.keys.first()
            bucketsByKey.remove(eldestKey)
        }
        return buckets
    }
}

internal data class FlashVisualPlaybackSampleState(
    val rawSample: Float,
    val displayedSample: Float,
)

@Composable
internal fun rememberFlashVisualPlaybackSampleState(
    rawSample: Float,
    isPlaying: Boolean,
    playbackSpeed: Float,
    sampleRateHz: Int,
    totalSamples: Int,
): FlashVisualPlaybackSampleState {
    var visualSample by remember { mutableFloatStateOf(rawSample) }
    val safeSpeed = playbackSpeed.coerceIn(0.1f, 4f)
    val latestAnchorSample by rememberUpdatedState(rawSample)
    val latestTotalSamples by rememberUpdatedState(totalSamples)
    if (!isPlaying || sampleRateHz <= 0 || totalSamples <= 0) {
        return FlashVisualPlaybackSampleState(
            rawSample = rawSample,
            displayedSample = visualSample.coerceIn(0f, totalSamples.coerceAtLeast(1).toFloat()),
        )
    }
    LaunchedEffect(safeSpeed, sampleRateHz, totalSamples) {
        FlashVisualPerfTrace.recordSmoothReset(
            anchorSample = latestAnchorSample,
            previousSmoothSample = visualSample,
            sampleRateHz = sampleRateHz,
        )
        val maxSample = latestTotalSamples.toFloat()
        visualSample = visualSample.coerceIn(0f, maxSample)
        var frameAnchorNanos = withFrameNanos { it }
        var frameAnchorSample = visualSample
        while (true) {
            val frameNanos = withFrameNanos { it }
            val elapsedSeconds = (frameNanos - frameAnchorNanos).toDouble() / 1_000_000_000.0
            val currentMaxSample = latestTotalSamples.toFloat()
            val anchor = latestAnchorSample.coerceIn(0f, currentMaxSample)
            val nextSample =
                frameAnchorSample +
                    (elapsedSeconds * sampleRateHz.toDouble() * safeSpeed.toDouble()).toFloat()
            val predictedSample = nextSample.coerceIn(0f, currentMaxSample)
            visualSample =
                if (abs(anchor - predictedSample) > sampleRateHz * 0.35f) {
                    frameAnchorNanos = frameNanos
                    frameAnchorSample = anchor
                    anchor
                } else {
                    predictedSample
                }
            if (visualSample >= currentMaxSample) {
                frameAnchorNanos = frameNanos
                frameAnchorSample = visualSample
            }
        }
    }
    return FlashVisualPlaybackSampleState(
        rawSample = rawSample,
        displayedSample = visualSample,
    )
}

internal data class FlashSignalBucketFrame(
    val buckets: List<FskEnergyBucket>,
    val displayedSamplePosition: Float,
    val analysisDisplayedSamplePosition: Float,
) {
    companion object {
        val Empty =
            FlashSignalBucketFrame(
                buckets = emptyList(),
                displayedSamplePosition = 0f,
                analysisDisplayedSamplePosition = 0f,
            )
    }
}

internal data class FlashBitReadoutFrame(
    val currentGroupStartIndex: Int,
    val currentBitOffset: Int?,
    val revealedBitOffset: Int,
    val previousCells: List<FlashBitReadoutCell>,
    val currentCells: List<FlashBitReadoutCell>,
)

internal data class FlashBitReadoutCell(
    val bit: Char?,
    val isCurrent: Boolean,
)

internal data class FlashPulseTapeState(
    val cells: List<FlashPulseCellState>,
    val currentBitProgress: Float,
)

internal data class FlashPulseCellState(
    val bit: Char?,
    val isActive: Boolean,
    val isRevealed: Boolean,
)

internal data class FlashBitReadoutSource(
    val entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    val bitByOffset: Map<Int, Char>,
)

internal fun PayloadFollowViewData.toFlashBitReadoutSource(): FlashBitReadoutSource? {
    if (!followAvailable || binaryGroupTimeline.isEmpty() || binaryTokens.isEmpty()) {
        return null
    }
    return FlashBitReadoutSource(
        entries = binaryGroupTimeline,
        bitByOffset = binaryBitsByOffset(),
    )
}

internal fun flashBitReadoutFrame(
    followData: PayloadFollowViewData,
    sample: Float,
): FlashBitReadoutFrame? {
    val source = followData.toFlashBitReadoutSource() ?: return null
    return flashBitReadoutFrame(source = source, sample = sample)
}

internal fun flashBitReadoutFrame(
    source: FlashBitReadoutSource,
    sample: Float,
): FlashBitReadoutFrame? {
    if (source.entries.isEmpty() || source.bitByOffset.isEmpty()) {
        return null
    }
    val playbackState = flashTimelinePlaybackState(entries = source.entries, sample = sample)
    val currentGroupStartIndex = (playbackState.revealedBitOffset.coerceAtLeast(0) / FlashBitReadoutGroupSize) * FlashBitReadoutGroupSize
    val previousGroupStartIndex = currentGroupStartIndex - FlashBitReadoutGroupSize
    return FlashBitReadoutFrame(
        currentGroupStartIndex = currentGroupStartIndex,
        currentBitOffset = playbackState.currentBitOffset,
        revealedBitOffset = playbackState.revealedBitOffset,
        previousCells =
            buildFlashBitReadoutCells(
                bitByOffset = source.bitByOffset,
                groupStartIndex = previousGroupStartIndex,
                revealThroughIndex = previousGroupStartIndex + FlashBitReadoutGroupSize - 1,
                currentBitOffset = null,
            ),
        currentCells =
            buildFlashBitReadoutCells(
                bitByOffset = source.bitByOffset,
                groupStartIndex = currentGroupStartIndex,
                revealThroughIndex = playbackState.revealedBitOffset,
                currentBitOffset = playbackState.currentBitOffset,
            ),
    )
}

internal fun flashPulseTapeState(
    source: FlashBitReadoutSource,
    sample: Float,
): FlashPulseTapeState? {
    if (source.entries.isEmpty() || source.bitByOffset.isEmpty()) {
        return null
    }
    val playbackState = flashTimelinePlaybackState(entries = source.entries, sample = sample)
    val anchorBitOffset = playbackState.currentBitOffset ?: playbackState.revealedBitOffset.takeIf { it >= 0 } ?: return null
    val halfWindow = FlashPulseVisibleCellCount / 2
    return FlashPulseTapeState(
        cells =
            List(FlashPulseVisibleCellCount) { index ->
                val bitOffset = anchorBitOffset + index - halfWindow
                FlashPulseCellState(
                    bit = source.bitByOffset[bitOffset],
                    isActive = playbackState.currentBitOffset == bitOffset,
                    isRevealed = bitOffset <= playbackState.revealedBitOffset,
                )
            },
        currentBitProgress = playbackState.currentBitProgress,
    )
}

private fun buildFlashBitReadoutCells(
    bitByOffset: Map<Int, Char>,
    groupStartIndex: Int,
    revealThroughIndex: Int,
    currentBitOffset: Int?,
): List<FlashBitReadoutCell> =
    List(FlashBitReadoutGroupSize) { slot ->
        val bitOffset = groupStartIndex + slot
        FlashBitReadoutCell(
            bit =
                if (bitOffset >= 0 && bitOffset <= revealThroughIndex) {
                    bitByOffset[bitOffset]
                } else {
                    null
                },
            isCurrent = currentBitOffset == bitOffset,
        )
    }

private data class FlashTimelinePlaybackState(
    val currentBitOffset: Int?,
    val revealedBitOffset: Int,
    val currentBitProgress: Float,
)

private fun flashTimelinePlaybackState(
    entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    sample: Float,
): FlashTimelinePlaybackState {
    var low = 0
    var high = entries.lastIndex
    var previousRevealedBitOffset = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        val entry = entries[mid]
        val entryEndSample = entry.startSample + entry.sampleCount
        when {
            sample < entry.startSample -> high = mid - 1
            sample >= entryEndSample -> {
                previousRevealedBitOffset = entry.lastBitOffset
                low = mid + 1
            }
            else -> {
                val bitProgress = entry.bitProgressAtSample(sample)
                val currentBitOffset =
                    entry.bitOffset + bitProgress.toInt().coerceIn(0, entry.bitCount.coerceAtLeast(1) - 1)
                return FlashTimelinePlaybackState(
                    currentBitOffset = currentBitOffset,
                    revealedBitOffset = currentBitOffset,
                    currentBitProgress = bitProgress - bitProgress.toInt(),
                )
            }
        }
    }
    return FlashTimelinePlaybackState(
        currentBitOffset = null,
        revealedBitOffset = previousRevealedBitOffset,
        currentBitProgress = 0f,
    )
}

internal fun FlashBitReadoutSource.currentBitOffsetAtSample(sample: Float): Int? =
    flashTimelinePlaybackState(entries = entries, sample = sample).currentBitOffset

internal fun FlashBitReadoutFrame.currentBitsText(): String = currentCells.joinToString(separator = "") { it.bit?.toString() ?: "_" }

internal fun FlashBitReadoutFrame.previousBitsText(): String = previousCells.joinToString(separator = "") { it.bit?.toString() ?: "_" }

private val PayloadFollowBinaryGroupTimelineEntry.lastBitOffset: Int
    get() = bitOffset + bitCount - 1

internal fun PayloadFollowBinaryGroupTimelineEntry.bitProgressAtSample(sample: Float): Float {
    if (bitCount <= 1 || sampleCount <= 0) {
        return 0f
    }
    val progress = ((sample - startSample.toFloat()) / sampleCount.toFloat()).coerceIn(0f, 0.9999f)
    return progress * bitCount.toFloat()
}

private fun PayloadFollowViewData.binaryBitsByOffset(): Map<Int, Char> {
    val bitsByOffset = LinkedHashMap<Int, Char>()
    binaryGroupTimeline.forEach { entry ->
        val bits = binaryTokens.getOrNull(entry.groupIndex).orEmpty().filter { it == '0' || it == '1' }
        repeat(entry.bitCount.coerceAtLeast(0)) { bitIndex ->
            val tokenBitIndex =
                if (bits.length == entry.bitCount) {
                    bitIndex
                } else {
                    (entry.bitOffset + bitIndex).floorMod(bits.length)
                }
            bits.getOrNull(tokenBitIndex)?.let { bit ->
                bitsByOffset[entry.bitOffset + bitIndex] = bit
            }
        }
    }
    return bitsByOffset
}

private fun Int.floorMod(divisor: Int): Int =
    if (divisor <= 0) {
        0
    } else {
        ((this % divisor) + divisor) % divisor
    }

internal fun flashVisualPrimitiveEstimate(
    mode: FlashSignalVisualizationMode,
    drawableSegments: Int,
    buckets: Int,
    hasFixedTimeline: Boolean,
): Int =
    if (hasFixedTimeline) {
        when (mode) {
            FlashSignalVisualizationMode.Lanes -> drawableSegments * 2
            FlashSignalVisualizationMode.Pitch -> drawableSegments
            FlashSignalVisualizationMode.Pulse -> FlashPulseVisibleCellCount
        }
    } else {
        when (mode) {
            FlashSignalVisualizationMode.Lanes -> buckets * 2
            FlashSignalVisualizationMode.Pitch -> buckets * 2
            FlashSignalVisualizationMode.Pulse -> FlashPulseVisibleCellCount
        }
    }

private const val FlashSignalAnalysisCacheMaxEntries = 12
private const val FlashBitReadoutGroupSize = 8
internal const val FlashPulseVisibleCellCount = 13
