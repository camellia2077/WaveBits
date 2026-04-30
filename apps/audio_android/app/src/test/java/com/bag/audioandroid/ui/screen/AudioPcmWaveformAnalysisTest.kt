package com.bag.audioandroid.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPcmWaveformAnalysisTest {
    @Test
    fun `pcm waveform analysis step targets thirty fps`() {
        assertEquals(1470, pcmWaveformAnalysisSampleStep(sampleRateHz = 44_100, totalSamples = 44_100))
        assertEquals(1, pcmWaveformAnalysisSampleStep(sampleRateHz = 10, totalSamples = 44_100))
        assertEquals(120, pcmWaveformAnalysisSampleStep(sampleRateHz = 44_100, totalSamples = 120))
    }

    @Test
    fun `pcm waveform displayed samples quantize to analysis windows`() {
        assertEquals(0f, quantizePcmWaveformDisplayedSamples(displayedSamples = 10f, sampleStep = 100, totalSamples = 1_000))
        assertEquals(100f, quantizePcmWaveformDisplayedSamples(displayedSamples = 51f, sampleStep = 100, totalSamples = 1_000))
        assertEquals(1_000f, quantizePcmWaveformDisplayedSamples(displayedSamples = 1_020f, sampleStep = 100, totalSamples = 1_000))
    }
}
