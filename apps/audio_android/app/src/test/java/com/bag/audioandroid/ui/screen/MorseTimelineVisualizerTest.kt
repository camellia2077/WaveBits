package com.bag.audioandroid.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class MorseTimelineVisualizerTest {
    @Test
    fun `timeline window keeps fixed sample count near audio end`() {
        val window = resolveMorseTimelineWindow(currentSample = 10_000, windowSamples = 9_600)

        assertEquals(6_160, window.startSample)
        assertEquals(9_600, window.sampleCount)
        assertEquals(15_760, window.endSample)
    }

    @Test
    fun `sample width fraction is stable for fixed window`() {
        val middleWindow = resolveMorseTimelineWindow(currentSample = 6_000, windowSamples = 9_600)
        val endWindow = resolveMorseTimelineWindow(currentSample = 10_000, windowSamples = 9_600)

        assertEquals(
            morseTimelineSampleWidthFraction(sampleCount = 300, window = middleWindow),
            morseTimelineSampleWidthFraction(sampleCount = 300, window = endWindow),
            0.0001f,
        )
    }
}
