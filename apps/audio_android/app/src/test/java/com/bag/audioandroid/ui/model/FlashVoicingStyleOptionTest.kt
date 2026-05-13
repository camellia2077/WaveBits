package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FlashVoicingStyleOptionTest {
    @Test
    fun `fromId maps current emotion ids`() {
        assertEquals(FlashVoicingStyleOption.Standard, FlashVoicingStyleOption.fromId("standard"))
        assertEquals(FlashVoicingStyleOption.Hostile, FlashVoicingStyleOption.fromId("hostile"))
        assertEquals(FlashVoicingStyleOption.Litany, FlashVoicingStyleOption.fromId("litany"))
        assertEquals(FlashVoicingStyleOption.Collapse, FlashVoicingStyleOption.fromId("collapse"))
    }

    @Test
    fun `fromId falls back for unknown ids`() {
        assertEquals(FlashVoicingStyleOption.Standard, FlashVoicingStyleOption.fromId(null))
        assertEquals(FlashVoicingStyleOption.Standard, FlashVoicingStyleOption.fromId("unknown"))
    }

    @Test
    fun `emotion presets carry separate signal and voicing axes`() {
        assertEquals(FlashSignalProfileWire.STANDARD, FlashVoicingStyleOption.Standard.signalProfileValue)
        assertEquals(FlashVoicingFlavorWire.STANDARD, FlashVoicingStyleOption.Standard.voicingFlavorValue)
        assertEquals(FlashSignalProfileWire.HOSTILE, FlashVoicingStyleOption.Hostile.signalProfileValue)
        assertEquals(FlashVoicingFlavorWire.HOSTILE, FlashVoicingStyleOption.Hostile.voicingFlavorValue)
        assertEquals(FlashSignalProfileWire.LITANY, FlashVoicingStyleOption.Litany.signalProfileValue)
        assertEquals(FlashVoicingFlavorWire.LITANY, FlashVoicingStyleOption.Litany.voicingFlavorValue)
        assertEquals(FlashSignalProfileWire.COLLAPSE, FlashVoicingStyleOption.Collapse.signalProfileValue)
        assertEquals(FlashVoicingFlavorWire.COLLAPSE, FlashVoicingStyleOption.Collapse.voicingFlavorValue)
    }

    @Test
    fun `helpers expose behavior instead of wire-value branching`() {
        assertEquals(3, FlashVoicingStyleOption.Standard.flashVisualActiveWindowBucketCount)
        assertEquals(8, FlashVoicingStyleOption.Litany.flashVisualActiveWindowBucketCount)
        assertEquals(false, FlashVoicingStyleOption.Standard.usesLongCadencePayload)
        assertEquals(true, FlashVoicingStyleOption.Litany.usesLongCadencePayload)
    }
}
