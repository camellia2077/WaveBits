package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedOptionTest {
    @Test
    fun `playback speed labels show selected custom speed`() {
        assertEquals("0.1x", PlaybackSpeedOption.format(0.1f))
        assertEquals("0.25x", PlaybackSpeedOption.format(0.25f))
        assertEquals("0.5x", PlaybackSpeedOption.format(0.5f))
        assertEquals("0.75x", PlaybackSpeedOption.format(0.75f))
        assertEquals("1.0x", PlaybackSpeedOption.format(1.0f))
        assertEquals("1.3x", PlaybackSpeedOption.format(1.3f))
        assertEquals("3.7x", PlaybackSpeedOption.format(3.7f))
    }

    @Test
    fun `playback speed cycle loops through presets`() {
        assertEquals(0.25f, PlaybackSpeedOption.nextSpeed(0.1f), 0.0001f)
        assertEquals(0.5f, PlaybackSpeedOption.nextSpeed(0.25f), 0.0001f)
        assertEquals(4.0f, PlaybackSpeedOption.nextSpeed(2.0f), 0.0001f)
        assertEquals(0.1f, PlaybackSpeedOption.nextSpeed(4.0f), 0.0001f)
    }

    @Test
    fun `slider mapping uses the fixed eight playback speeds`() {
        assertEquals(listOf(0.1f, 0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 4.0f), PlaybackSpeedOption.speeds)
        assertEquals(0f, PlaybackSpeedOption.sliderPosition(0.1f), 0.0001f)
        assertEquals(3f, PlaybackSpeedOption.sliderPosition(0.75f), 0.0001f)
        assertEquals(7f, PlaybackSpeedOption.sliderPosition(4.0f), 0.0001f)
        assertEquals(0.1f, PlaybackSpeedOption.speedAtSliderPosition(0f), 0.0001f)
        assertEquals(1.0f, PlaybackSpeedOption.speedAtSliderPosition(4f), 0.0001f)
        assertEquals(4.0f, PlaybackSpeedOption.speedAtSliderPosition(7f), 0.0001f)
    }
}
