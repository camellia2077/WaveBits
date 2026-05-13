package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFlashSignalVisualizerTest {
    @Test
    fun `all flash visual modes share the same playhead layout`() {
        val lanesLayout = flashVisualPlayheadLayout(FlashSignalVisualizationMode.Lanes)

        assertEquals(lanesLayout, flashVisualPlayheadLayout(FlashSignalVisualizationMode.Pitch))
        assertEquals(lanesLayout, flashVisualPlayheadLayout(FlashSignalVisualizationMode.Pulse))
    }

    @Test
    fun `pulse anchor resolves to the shared playhead position`() {
        val fullCardWidthPx = 300f
        val sharedPlayheadX =
            flashVisualPlayheadX(
                totalWidthPx = fullCardWidthPx,
                leftPaddingPx = 12f,
                rightPaddingPx = 12f,
            )
        val pulseCanvasWidthPx = fullCardWidthPx - 36f
        val pulseAnchorX = sharedPlayheadX - 18f

        assertEquals(122.4f, sharedPlayheadX, 0.001f)
        assertEquals(104.4f, pulseAnchorX, 0.001f)
        assertEquals(264f, pulseCanvasWidthPx, 0.001f)
    }

    @Test
    fun `lane active bit state resolves current bit span inside timeline`() {
        val state =
            flashLaneActiveBitState(
                entries =
                    listOf(
                        PayloadFollowBinaryGroupTimelineEntry(
                            startSample = 0,
                            sampleCount = 80,
                            groupIndex = 0,
                            bitOffset = 0,
                            bitCount = 8,
                        ),
                    ),
                bitByOffset = mapOf(2 to '1'),
                sample = 25f,
            )

        assertEquals(2, state?.bitOffset)
        assertEquals('1', state?.bitValue)
        assertEquals(20f, state!!.startSample, 0.001f)
        assertEquals(30f, state.endSample, 0.001f)
    }

    @Test
    fun `token alignment state keeps full byte and current bit`() {
        val state =
            flashTokenAlignmentState(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("l"),
                        textTokenTimeline =
                            listOf(
                                TextFollowTimelineEntry(
                                    startSample = 0,
                                    sampleCount = 80,
                                    tokenIndex = 0,
                                ),
                            ),
                        textRawDisplayUnits =
                            listOf(
                                TextFollowRawDisplayUnitViewData(
                                    tokenIndex = 0,
                                    startSample = 0,
                                    sampleCount = 80,
                                    byteIndexWithinToken = 0,
                                    byteOffset = 0,
                                    byteCount = 1,
                                    hexText = "6C",
                                    binaryText = "01101100",
                                ),
                            ),
                        binaryTokens = listOf("0", "1", "1", "0", "1", "1", "0", "0"),
                        binaryGroupTimeline =
                            List(8) { index ->
                                PayloadFollowBinaryGroupTimelineEntry(
                                    startSample = index * 10,
                                    sampleCount = 10,
                                    groupIndex = index,
                                    bitOffset = index,
                                    bitCount = 1,
                                )
                            },
                        totalPcmSampleCount = 80,
                        followAvailable = true,
                        textFollowAvailable = true,
                    ),
                displayedSamples = 25,
            )

        assertEquals(0, state.activeTokenIndex)
        assertEquals(0, state.activeByteIndexWithinToken)
        assertEquals(2, state.activeBitIndexWithinByte)
        assertEquals(2, state.globalBitOffset)
        assertEquals('1', state.currentBitValue)
        assertEquals("6C", state.byteHex)
        assertEquals("01101100", state.byteBinary)
    }
}
