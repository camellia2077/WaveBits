package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioPcmCacheWriter
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.PlaybackUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.text.Charsets.UTF_8

private val DefaultFollowData =
    PayloadFollowViewData(
        textTokens = listOf("A"),
        textTokenTimeline = listOf(TextFollowTimelineEntry(0, 8, 0)),
        textFollowAvailable = true,
        lyricLines = listOf("A"),
        lyricLineTimeline = listOf(TextFollowLyricLineTimelineEntry(0, 8, 0)),
        lineTokenRanges = listOf(TextFollowLineTokenRangeViewData(0, 0, 1)),
        lyricLineFollowAvailable = true,
        hexTokens = listOf("41"),
        binaryTokens = listOf("01000001"),
        byteTimeline = listOf(PayloadFollowByteTimelineEntry(0, 8, 0)),
        binaryGroupTimeline = listOf(PayloadFollowBinaryGroupTimelineEntry(0, 8, 0, 0, 8)),
        payloadBeginSample = 0,
        payloadSampleCount = 8,
        totalPcmSampleCount = 8,
        followAvailable = true,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSessionCodecActionsTest {
    @Test
    fun `same phase progress keeps status text stable while progress stays real time`() =
        runTest {
            val completion = CompletableDeferred<EncodeAudioResult>()
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeBlock = { onProgress ->
                                onProgress(EncodeProgressUpdate(AudioEncodePhase.PreparingInput, 0.10f))
                                onProgress(EncodeProgressUpdate(AudioEncodePhase.PreparingInput, 0.35f))
                                completion.await()
                            },
                        ),
                    testScope = this,
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
    fun `phase change updates status text to the new phase`() =
        runTest {
            val completion = CompletableDeferred<EncodeAudioResult>()
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeBlock = { onProgress ->
                                onProgress(EncodeProgressUpdate(AudioEncodePhase.PreparingInput, 0.10f))
                                onProgress(EncodeProgressUpdate(AudioEncodePhase.RenderingPcm, 0.40f))
                                completion.await()
                            },
                        ),
                    testScope = this,
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
    fun `cancelled failed and success results stay distinct`() =
        runTest {
            val cancelledFixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeResult = EncodeAudioResult.Cancelled,
                        ),
                    testScope = this,
                )
            cancelledFixture.actions.onEncode()
            advanceUntilIdle()
            val cancelledSession = cancelledFixture.uiState.value.currentSession
            assertFalse(cancelledSession.isCodecBusy)
            assertEquals(null, cancelledSession.encodePhase)
            assertResId(cancelledSession.statusText, R.string.status_mode_audio_cancelled)

            val failedFixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeResult = EncodeAudioResult.Failed(BagApiCodes.ERROR_NOT_IMPLEMENTED),
                        ),
                    testScope = this,
                )
            failedFixture.actions.onEncode()
            advanceUntilIdle()
            val failedSession = failedFixture.uiState.value.currentSession
            assertFalse(failedSession.isCodecBusy)
            assertEquals(null, failedSession.encodePhase)
            assertResId(failedSession.statusText, R.string.error_not_implemented)

            val successFixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeResult = EncodeAudioResult.Success(shortArrayOf(5, 6, 7), followData = DefaultFollowData),
                        ),
                    testScope = this,
                )
            successFixture.actions.onEncode()
            advanceUntilIdle()
            val successSession = successFixture.uiState.value.currentSession
            assertFalse(successSession.isCodecBusy)
            assertEquals(null, successSession.encodePhase)
            assertEquals(
                listOf(5.toShort(), 6.toShort(), 7.toShort()),
                successSession.generatedPcm.toList(),
            )
            assertEquals(3, successSession.playback.totalSamples)
            assertTrue(successSession.followData.followAvailable)
            assertTrue(successSession.followData.textFollowAvailable)
            assertEquals(listOf("A"), successSession.followData.textTokens)
        }

    @Test
    fun `payload too large input auto segments and keeps aggregated playback`() =
        runTest {
            val longText = "祭".repeat(200)
            var nextSample = 1
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            validateEncodeRequestBlock = { text, _, _, _, _, _ ->
                                if (text == longText) {
                                    BagApiCodes.VALIDATION_PAYLOAD_TOO_LARGE
                                } else {
                                    BagApiCodes.VALIDATION_OK
                                }
                            },
                            encodeBlock = { _ ->
                                EncodeAudioResult.Success(shortArrayOf(nextSample++.toShort()))
                            },
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    sessions =
                        fixture.uiState.value.sessions.mapValues { (_, session) ->
                            session.copy(inputText = longText)
                        },
                )

            fixture.actions.onEncode()
            advanceUntilIdle()

            val session = fixture.uiState.value.currentSession
            val status = session.statusText as UiText.Resource
            assertEquals(R.string.status_mode_audio_generated_segmented, status.resId)
            assertTrue(session.generatedAudioMetadata?.isSegmented == true)
            assertTrue((session.generatedAudioMetadata?.segmentCount ?: 0) > 1)
            assertEquals(0, session.generatedPcm.size)
            assertTrue(session.generatedWaveformPcm.isNotEmpty())
            assertTrue(session.generatedPcmFilePath?.let { File(it).exists() } == true)
            assertEquals(
                session.generatedAudioMetadata?.segmentCount,
                session.generatedAudioMetadata?.segmentSampleCounts?.size,
            )
            assertEquals(session.generatedAudioMetadata?.pcmSampleCount, session.playback.totalSamples)
            assertTrue(fixture.uiState.value.miniPlayerModel != null)
        }

    @Test
    fun `long valid payload segments before native result exceeds jvm audio limit`() =
        runTest {
            val longText = "A".repeat(1_025)
            val encodedTexts = mutableListOf<String>()
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeTextBlock = { text, _ ->
                                encodedTexts += text
                                EncodeAudioResult.Success(shortArrayOf(text.length.toShort()))
                            },
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    sessions =
                        fixture.uiState.value.sessions.mapValues { (_, session) ->
                            session.copy(inputText = longText)
                        },
                )

            fixture.actions.onEncode()
            advanceUntilIdle()

            val session = fixture.uiState.value.currentSession
            assertTrue(encodedTexts.size > 1)
            assertFalse(encodedTexts.contains(longText))
            assertTrue(encodedTexts.all { it.toByteArray(UTF_8).size <= 512 })
            assertEquals(longText, encodedTexts.joinToString(separator = ""))
            assertEquals(0, session.generatedPcm.size)
            assertTrue(session.generatedPcmFilePath?.let { File(it).exists() } == true)
            assertEquals(encodedTexts.size, session.generatedAudioMetadata?.segmentCount)
            assertEquals(session.generatedAudioMetadata?.pcmSampleCount, session.playback.totalSamples)
            assertTrue(fixture.uiState.value.miniPlayerModel != null)
        }

    @Test
    fun `segmented long duration still hydrates follow data`() =
        runTest {
            val longText = "A".repeat(513)
            val longSegmentPcmSampleCount = 2_700_000
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeTextBlock = { _, _ ->
                                EncodeAudioResult.Success(ShortArray(longSegmentPcmSampleCount))
                            },
                            buildFollowDataBlock = {
                                DefaultFollowData.copy(totalPcmSampleCount = longSegmentPcmSampleCount)
                            },
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    sessions =
                        fixture.uiState.value.sessions.mapValues { (_, session) ->
                            session.copy(inputText = longText)
                        },
                )

            fixture.actions.onEncode()
            advanceUntilIdle()

            val session = fixture.uiState.value.currentSession
            assertTrue((session.generatedAudioMetadata?.pcmSampleCount ?: 0) > 44_100 * 120)
            assertTrue(session.followData.followAvailable)
            assertTrue(session.followData.textFollowAvailable)
        }

    @Test
    fun `utf8 segmentation keeps every segment within 512 bytes`() {
        val longText = "flash 祭 ".repeat(90)

        val plan = splitInputIntoPayloadSegments(longText, maxPayloadBytes = 512)

        assertTrue(plan.segmentCount > 1)
        assertEquals(longText, plan.segments.joinToString(separator = ""))
        assertTrue(plan.segments.all { it.toByteArray(UTF_8).size <= 512 })
    }

    private fun createFixture(
        gateway: AudioCodecGateway,
        testScope: TestScope,
    ): Fixture {
        val dispatcher = StandardTestDispatcher(testScope.testScheduler)
        val uiState = MutableStateFlow(AudioAppUiState())
        val actions =
            AudioSessionCodecActions(
                uiState = uiState,
                scope = CoroutineScope(dispatcher),
                audioCodecGateway = gateway,
                sessionStateStore = AudioSessionStateStore(uiState),
                uiTextMapper = BagUiTextMapper(),
                playbackRuntimeGateway = FakePlaybackRuntimeGateway(),
                sampleRateHz = 44_100,
                frameSamples = 2_205,
                stopPlayback = {},
                workerDispatcher = dispatcher,
                generatedAudioCacheGateway = CodecFakeGeneratedAudioCacheGateway(),
            )
        return Fixture(uiState, actions)
    }

    private fun assertResId(
        text: UiText,
        expectedResId: Int,
    ) {
        val resource = text as UiText.Resource
        assertEquals(expectedResId, resource.resId)
    }

    private data class Fixture(
        val uiState: MutableStateFlow<AudioAppUiState>,
        val actions: AudioSessionCodecActions,
    )
}

private class FakeAudioCodecGateway(
    private val encodeResult: EncodeAudioResult =
        EncodeAudioResult.Success(
            shortArrayOf(1, 2),
            followData = DefaultFollowData,
        ),
    private val encodeBlock: (suspend ((EncodeProgressUpdate) -> Unit) -> EncodeAudioResult)? = null,
    private val encodeTextBlock: (suspend (String, (EncodeProgressUpdate) -> Unit) -> EncodeAudioResult)? = null,
    private val validateEncodeRequestBlock:
        ((
            text: String,
            sampleRateHz: Int,
            frameSamples: Int,
            mode: Int,
            flashSignalProfile: Int,
            flashVoicingFlavor: Int,
        ) -> Int)? = null,
    private val buildFollowDataBlock: ((String) -> PayloadFollowViewData)? = null,
) : AudioCodecGateway {
    override fun validateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int =
        validateEncodeRequestBlock?.invoke(
            text,
            sampleRateHz,
            frameSamples,
            mode,
            flashSignalProfile,
            flashVoicingFlavor,
        ) ?: BagApiCodes.VALIDATION_OK

    override suspend fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (EncodeProgressUpdate) -> Unit,
    ): EncodeAudioResult = encodeTextBlock?.invoke(text, onProgress) ?: encodeBlock?.invoke(onProgress) ?: encodeResult

    override suspend fun buildEncodeFollowData(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ) = com.bag.audioandroid.domain.EncodedAudioPayloadResult(
        followData = buildFollowDataBlock?.invoke(text) ?: DefaultFollowData,
    )

    override fun validateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int = BagApiCodes.VALIDATION_OK

    override fun decodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): DecodedAudioPayloadResult = DecodedAudioPayloadResult()

    override fun getCoreVersion(): String = "test"
}

private class FakePlaybackRuntimeGateway : PlaybackRuntimeGateway {
    override fun cleared(): PlaybackUiState = PlaybackUiState()

    override fun load(
        totalSamples: Int,
        sampleRateHz: Int,
    ): PlaybackUiState = PlaybackUiState(totalSamples = totalSamples, sampleRateHz = sampleRateHz)

    override fun playStarted(state: PlaybackUiState): PlaybackUiState = state

    override fun paused(state: PlaybackUiState): PlaybackUiState = state

    override fun resumed(state: PlaybackUiState): PlaybackUiState = state

    override fun progress(
        state: PlaybackUiState,
        playedSamples: Int,
    ): PlaybackUiState = state

    override fun scrubStarted(state: PlaybackUiState): PlaybackUiState = state

    override fun scrubChanged(
        state: PlaybackUiState,
        targetSamples: Int,
    ): PlaybackUiState = state

    override fun scrubCommitted(state: PlaybackUiState): PlaybackUiState = state

    override fun scrubCanceled(state: PlaybackUiState): PlaybackUiState = state

    override fun stopped(state: PlaybackUiState): PlaybackUiState = state

    override fun completed(state: PlaybackUiState): PlaybackUiState = state

    override fun failed(state: PlaybackUiState): PlaybackUiState = state

    override fun clampSamples(
        totalSamples: Int,
        sampleIndex: Int,
    ): Int = sampleIndex

    override fun fractionToSamples(
        totalSamples: Int,
        fraction: Float,
    ): Int = 0

    override fun progressFraction(state: PlaybackUiState): Float = 0f

    override fun elapsedMs(state: PlaybackUiState): Long = 0L

    override fun totalMs(state: PlaybackUiState): Long = 0L
}

private class CodecFakeGeneratedAudioCacheGateway : GeneratedAudioCacheGateway {
    override fun createPcmCacheWriter(modeWireName: String): GeneratedAudioPcmCacheWriter =
        FakeGeneratedAudioPcmCacheWriter(
            File.createTempFile("${modeWireName}_", ".pcm16").apply {
                deleteOnExit()
            },
        )

    override fun deleteCachedFile(path: String?) {
        if (!path.isNullOrBlank()) {
            File(path).delete()
        }
    }
}

private class FakeGeneratedAudioPcmCacheWriter(
    private val file: File,
) : GeneratedAudioPcmCacheWriter {
    override val filePath: String = file.absolutePath

    private var output: BufferedOutputStream? =
        BufferedOutputStream(
            FileOutputStream(file),
        )

    override fun appendPcm(pcm: ShortArray) {
        val stream = output ?: return
        val bytes = ByteArray(pcm.size * 2)
        var byteIndex = 0
        pcm.forEach { sample ->
            val value = sample.toInt()
            bytes[byteIndex] = (value and 0xFF).toByte()
            bytes[byteIndex + 1] = ((value ushr 8) and 0xFF).toByte()
            byteIndex += 2
        }
        stream.write(bytes)
    }

    override fun finish() {
        output?.flush()
        output?.close()
        output = null
    }

    override fun abort() {
        finish()
        file.delete()
    }
}
