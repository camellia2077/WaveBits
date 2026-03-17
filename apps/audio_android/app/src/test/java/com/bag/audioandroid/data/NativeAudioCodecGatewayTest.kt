package com.bag.audioandroid.data

import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.EncodeAudioResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAudioCodecGatewayTest {
    @Test
    fun `poll snapshot parses state phase progress and terminal code`() {
        val snapshot = floatArrayOf(
            EncodeJobState.Running.nativeValue.toFloat(),
            AudioEncodePhase.Postprocessing.nativeValue.toFloat(),
            0.625f,
            BagApiCodes.ERROR_NOT_READY.toFloat()
        ).toEncodeJobSnapshot()

        assertEquals(EncodeJobState.Running, snapshot.state)
        assertEquals(AudioEncodePhase.Postprocessing, snapshot.phase)
        assertEquals(0.625f, snapshot.progress0To1)
        assertEquals(BagApiCodes.ERROR_NOT_READY, snapshot.terminalCode)
    }

    @Test
    fun `zero native handle maps to internal failure`() {
        val result = 0L.toStartFailureResultOrNull()

        assertTrue(result is EncodeAudioResult.Failed)
        assertEquals(BagApiCodes.ERROR_INTERNAL, (result as EncodeAudioResult.Failed).errorCode)
    }

    @Test
    fun `empty successful pcm result maps to internal failure`() {
        val result = shortArrayOf().toEncodeSuccessOrFailureResult()

        assertTrue(result is EncodeAudioResult.Failed)
        assertEquals(BagApiCodes.ERROR_INTERNAL, (result as EncodeAudioResult.Failed).errorCode)
    }

    @Test
    fun `non-empty successful pcm result maps to success`() {
        val pcm = shortArrayOf(1, 2, 3)
        val result = pcm.toEncodeSuccessOrFailureResult()

        assertTrue(result is EncodeAudioResult.Success)
        assertEquals(pcm.toList(), (result as EncodeAudioResult.Success).pcm.toList())
    }
}
