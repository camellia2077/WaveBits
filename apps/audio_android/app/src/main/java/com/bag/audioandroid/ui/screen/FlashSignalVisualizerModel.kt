package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.state.FlashVisualWindowState

internal data class FlashSignalVisualizerModel(
    val totalSamples: Int,
    val clampedDisplayedSamples: Int,
    val followTimelineSource: FlashSignalBucketSource.FollowTimeline?,
    val followTimelineTotalSamples: Int,
    val clampedFollowDisplayedSamples: Int,
    val displayedSamplePosition: Float,
    val followDisplayedSamplePosition: Float,
    val windowedTimelineFrame: FlashSignalFixedTimelineFrame?,
)

@Composable
internal fun rememberFlashSignalVisualizerModel(
    input: FlashSignalVisualizationInput,
    flashVisualWindow: FlashVisualWindowState,
): FlashSignalVisualizerModel {
    val totalSamples = remember(input.pcm) { input.pcm.size.coerceAtLeast(1) }
    val clampedDisplayedSamples =
        remember(input.pcmDisplayedSamples, totalSamples) {
            input.pcmDisplayedSamples.coerceIn(0, totalSamples)
        }
    val followTimelineSource = input.bucketSource as? FlashSignalBucketSource.FollowTimeline
    val followTimelineTotalSamples =
        remember(followTimelineSource, clampedDisplayedSamples) {
            followTimelineSource
                ?.followData
                ?.totalPcmSampleCount
                ?.coerceAtLeast(followTimelineSource.displayedSamples)
                ?.coerceAtLeast(1)
                ?: 1
        }
    val clampedFollowDisplayedSamples =
        remember(followTimelineSource, followTimelineTotalSamples, clampedDisplayedSamples) {
            followTimelineSource
                ?.displayedSamples
                ?.coerceIn(0, followTimelineTotalSamples)
                ?: clampedDisplayedSamples
        }
    val windowedTimelineFrame =
        remember(flashVisualWindow) {
            flashVisualWindow
                .takeIf { it.available }
                ?.toFixedTimelineFrame()
        }
    return remember(
        totalSamples,
        clampedDisplayedSamples,
        followTimelineSource,
        followTimelineTotalSamples,
        clampedFollowDisplayedSamples,
        windowedTimelineFrame,
    ) {
        FlashSignalVisualizerModel(
            totalSamples = totalSamples,
            clampedDisplayedSamples = clampedDisplayedSamples,
            followTimelineSource = followTimelineSource,
            followTimelineTotalSamples = followTimelineTotalSamples,
            clampedFollowDisplayedSamples = clampedFollowDisplayedSamples,
            displayedSamplePosition = clampedDisplayedSamples.toFloat(),
            followDisplayedSamplePosition = clampedFollowDisplayedSamples.toFloat(),
            windowedTimelineFrame = windowedTimelineFrame,
        )
    }
}

internal data class FlashSignalFixedTimelineFrame(
    val segments: List<FlashSignalToneSegment>,
    val drawableSegments: List<FlashSignalToneSegment>,
    val totalSamples: Int,
)

internal fun FlashVisualWindowState.toFixedTimelineFrame(): FlashSignalFixedTimelineFrame =
    FlashSignalFixedTimelineFrame(
        segments = segments,
        drawableSegments = drawableSegments,
        totalSamples = totalPcmSampleCount.coerceAtLeast(1),
    )

internal fun PayloadFollowViewData.toFixedTimelineFrameOrNull(): FlashSignalFixedTimelineFrame? {
    val segments = buildFlashSignalToneSegments(this)
    if (segments.isEmpty()) {
        return null
    }
    return FlashSignalFixedTimelineFrame(
        segments = segments,
        drawableSegments = segments,
        totalSamples = totalPcmSampleCount.coerceAtLeast(1),
    )
}
