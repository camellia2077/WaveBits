package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProEncodingVisualizationAnalysisTest {
    @Test
    fun `derives current byte and nibble explanation from follow data`() {
        val state =
            deriveProEncodingVisualizationState(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("AB"),
                        textTokenTimeline = listOf(TextFollowTimelineEntry(0, 8820, 0)),
                        textRawDisplayUnits =
                            listOf(
                                TextFollowRawDisplayUnitViewData(
                                    tokenIndex = 0,
                                    startSample = 0,
                                    sampleCount = 4410,
                                    byteIndexWithinToken = 0,
                                    byteOffset = 0,
                                    byteCount = 2,
                                    hexText = "41",
                                    binaryText = "01000001",
                                ),
                                TextFollowRawDisplayUnitViewData(
                                    tokenIndex = 0,
                                    startSample = 4410,
                                    sampleCount = 4410,
                                    byteIndexWithinToken = 1,
                                    byteOffset = 1,
                                    byteCount = 2,
                                    hexText = "42",
                                    binaryText = "01000010",
                                ),
                            ),
                        byteTimeline =
                            listOf(
                                PayloadFollowByteTimelineEntry(0, 4410, 0),
                                PayloadFollowByteTimelineEntry(4410, 4410, 1),
                            ),
                        textFollowAvailable = true,
                        followAvailable = true,
                    ),
                displayedSamples = 3000,
                frameSamples = 2205,
            )

        assertNotNull(state)
        assertEquals("A", state?.byteExplanation?.asciiDisplay)
        assertEquals("41", state?.byteExplanation?.byteHex)
        assertEquals("4", state?.byteExplanation?.highNibbleHex)
        assertEquals("1", state?.byteExplanation?.lowNibbleHex)
        assertEquals("1", state?.currentSymbol?.nibbleHex)
        assertEquals(697, state?.currentSymbol?.lowFreqHz)
        assertEquals(1336, state?.currentSymbol?.highFreqHz)
        assertEquals(1, state?.currentSymbol?.slotIndexWithinByte)
        assertEquals(0, state?.nextSymbol?.slotIndexWithinByte)
        assertEquals("4", state?.nextSymbol?.nibbleHex)
        assertEquals(0, state?.tokenByteMapping?.tokenIndex)
        assertEquals("AB", state?.tokenByteMapping?.tokenText)
        assertEquals(0, state?.tokenByteMapping?.byteIndexWithinToken)
        assertEquals(2, state?.tokenByteMapping?.byteCountWithinUnit)
        assertEquals(0, state?.byteExplanation?.tokenIndex)
        assertEquals(0, state?.byteExplanation?.byteIndexWithinToken)
        assertEquals(2, state?.byteExplanation?.byteCountWithinUnit)
    }
}
