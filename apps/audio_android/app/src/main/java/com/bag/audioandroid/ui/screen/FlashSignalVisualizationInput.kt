package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowViewData

internal data class FlashSignalVisualizationInput(
    val pcm: ShortArray,
    val sampleRateHz: Int,
    val pcmDisplayedSamples: Int,
    val bucketSource: FlashSignalBucketSource,
)

internal sealed interface FlashSignalBucketSource {
    data class Pcm(
        val displayedSamples: Int,
    ) : FlashSignalBucketSource

    data class FollowTimeline(
        val followData: PayloadFollowViewData,
        val displayedSamples: Int,
    ) : FlashSignalBucketSource
}

internal fun flashSignalVisualizationInput(
    pcm: ShortArray,
    sampleRateHz: Int,
    visualDisplayedSamples: Int,
    followDisplayedSamples: Int,
    followData: PayloadFollowViewData,
    useFollowTimelineBuckets: Boolean,
): FlashSignalVisualizationInput =
    FlashSignalVisualizationInput(
        pcm = pcm,
        sampleRateHz = sampleRateHz,
        pcmDisplayedSamples = visualDisplayedSamples,
        bucketSource =
            if (useFollowTimelineBuckets) {
                FlashSignalBucketSource.FollowTimeline(
                    followData = followData,
                    displayedSamples = followDisplayedSamples,
                )
            } else {
                FlashSignalBucketSource.Pcm(displayedSamples = visualDisplayedSamples)
            },
    )
