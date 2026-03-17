package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.PlaybackUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSessionCodecActionsTest {
    @Test
    fun `same phase progress keeps status text stable while progress stays real time`() = runTest {
        val completion = CompletableDeferred<EncodeAudioResult>()
        val fixture = createFixture(
            gateway = FakeAudioCodecGateway(
                encodeBlock = { onProgress ->
                    onProgress(EncodeProgressUpdate(AudioEncodePhase.PreparingInput, 0.10f))
                    onProgress(EncodeProgressUpdate(AudioEncodePhase.PreparingInput, 0.35f))
                    completion.await()
                }
            ),
            testScope = this
        )

        fixture.actions.onEncode()
        advanceUntilIdle()

        val session = fixture.uiState.value.currentSession
        assertTrue(session.isCodecBusy)
        assertEquals(0.35f, session.encodeProgress)
        assertEquals(AudioEncodePhase.PreparingInput, session.encodePhase)
        assertResId(session.statusText, R.string.status_mode_audio_generating_preparing_input)

        completion.complete(EncodeAudioResult.Success(shortArrayOf(1, 2, 3)))
        advanceUntilIdle()
    }

    @Test
    fun `phase change updates status text to the new phase`() = runTest {
        val completion = CompletableDeferred<EncodeAudioResult>()
        val fixture = createFixture(
            gateway = FakeAudioCodecGateway(
                encodeBlock = { onProgress ->
                    onProgress(EncodeProgressUpdate(AudioEncodePhase.PreparingInput, 0.10f))
                    onProgress(EncodeProgressUpdate(AudioEncodePhase.RenderingPcm, 0.40f))
                    completion.await()
                }
            ),
            testScope = this
        )

        fixture.actions.onEncode()
        advanceUntilIdle()

        val session = fixture.uiState.value.currentSession
        assertEquals(AudioEncodePhase.RenderingPcm, session.encodePhase)
        assertEquals(0.40f, session.encodeProgress)
        assertResId(session.statusText, R.string.status_mode_audio_generating_rendering_pcm)

        completion.complete(EncodeAudioResult.Success(shortArrayOf(1, 2, 3)))
        advanceUntilIdle()
    }

    @Test
    fun `cancelled failed and success results stay distinct`() = runTest {
        val cancelledFixture = createFixture(
            gateway = FakeAudioCodecGateway(
                encodeResult = EncodeAudioResult.Cancelled
            ),
            testScope = this
        )
        cancelledFixture.actions.onEncode()
        advanceUntilIdle()
        val cancelledSession = cancelledFixture.uiState.value.currentSession
        assertFalse(cancelledSession.isCodecBusy)
        assertEquals(null, cancelledSession.encodePhase)
        assertResId(cancelledSession.statusText, R.string.status_mode_audio_cancelled)

        val failedFixture = createFixture(
            gateway = FakeAudioCodecGateway(
                encodeResult = EncodeAudioResult.Failed(BagApiCodes.ERROR_NOT_IMPLEMENTED)
            ),
            testScope = this
        )
        failedFixture.actions.onEncode()
        advanceUntilIdle()
        val failedSession = failedFixture.uiState.value.currentSession
        assertFalse(failedSession.isCodecBusy)
        assertEquals(null, failedSession.encodePhase)
        assertResId(failedSession.statusText, R.string.error_not_implemented)

        val successFixture = createFixture(
            gateway = FakeAudioCodecGateway(
                encodeResult = EncodeAudioResult.Success(shortArrayOf(5, 6, 7))
            ),
            testScope = this
        )
        successFixture.actions.onEncode()
        advanceUntilIdle()
        val successSession = successFixture.uiState.value.currentSession
        assertFalse(successSession.isCodecBusy)
        assertEquals(null, successSession.encodePhase)
        assertEquals(
            listOf(5.toShort(), 6.toShort(), 7.toShort()),
            successSession.generatedPcm.toList()
        )
        assertEquals(3, successSession.playback.totalSamples)
    }

    private fun createFixture(
        gateway: AudioCodecGateway,
        testScope: TestScope
    ): Fixture {
        val dispatcher = StandardTestDispatcher(testScope.testScheduler)
        val uiState = MutableStateFlow(AudioAppUiState())
        val actions = AudioSessionCodecActions(
            uiState = uiState,
            scope = CoroutineScope(dispatcher),
            audioCodecGateway = gateway,
            sessionStateStore = AudioSessionStateStore(uiState),
            uiTextMapper = BagUiTextMapper(),
            playbackRuntimeGateway = FakePlaybackRuntimeGateway(),
            sampleRateHz = 44_100,
            frameSamples = 2_205,
            stopPlayback = {},
            workerDispatcher = dispatcher
        )
        return Fixture(uiState, actions)
    }

    private fun assertResId(text: UiText, expectedResId: Int) {
        val resource = text as UiText.Resource
        assertEquals(expectedResId, resource.resId)
    }

    private data class Fixture(
        val uiState: MutableStateFlow<AudioAppUiState>,
        val actions: AudioSessionCodecActions
    )
}

private class FakeAudioCodecGateway(
    private val encodeResult: EncodeAudioResult = EncodeAudioResult.Success(shortArrayOf(1, 2)),
    private val encodeBlock: (suspend ((EncodeProgressUpdate) -> Unit) -> EncodeAudioResult)? = null
) : AudioCodecGateway {
    override fun validateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int
    ): Int = BagApiCodes.VALIDATION_OK

    override suspend fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (EncodeProgressUpdate) -> Unit
    ): EncodeAudioResult = encodeBlock?.invoke(onProgress) ?: encodeResult

    override fun validateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int
    ): Int = BagApiCodes.VALIDATION_OK

    override fun decodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int
    ): String = ""

    override fun getCoreVersion(): String = "test"
}

private class FakePlaybackRuntimeGateway : PlaybackRuntimeGateway {
    override fun cleared(): PlaybackUiState = PlaybackUiState()

    override fun load(totalSamples: Int, sampleRateHz: Int): PlaybackUiState =
        PlaybackUiState(totalSamples = totalSamples, sampleRateHz = sampleRateHz)

    override fun playStarted(state: PlaybackUiState): PlaybackUiState = state
    override fun paused(state: PlaybackUiState): PlaybackUiState = state
    override fun resumed(state: PlaybackUiState): PlaybackUiState = state
    override fun progress(state: PlaybackUiState, playedSamples: Int): PlaybackUiState = state
    override fun scrubStarted(state: PlaybackUiState): PlaybackUiState = state
    override fun scrubChanged(state: PlaybackUiState, targetSamples: Int): PlaybackUiState = state
    override fun scrubCommitted(state: PlaybackUiState): PlaybackUiState = state
    override fun scrubCanceled(state: PlaybackUiState): PlaybackUiState = state
    override fun stopped(state: PlaybackUiState): PlaybackUiState = state
    override fun completed(state: PlaybackUiState): PlaybackUiState = state
    override fun failed(state: PlaybackUiState): PlaybackUiState = state
    override fun clampSamples(totalSamples: Int, sampleIndex: Int): Int = sampleIndex
    override fun fractionToSamples(totalSamples: Int, fraction: Float): Int = 0
    override fun progressFraction(state: PlaybackUiState): Float = 0f
    override fun elapsedMs(state: PlaybackUiState): Long = 0L
    override fun totalMs(state: PlaybackUiState): Long = 0L
}
