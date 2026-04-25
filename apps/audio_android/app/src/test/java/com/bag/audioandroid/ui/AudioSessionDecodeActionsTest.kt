package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.data.SampleInput
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
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

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSessionDecodeActionsTest {
    @Test
    fun `decode stores text and raw payload for generated audio`() =
        runTest {
            val expected =
                DecodedPayloadViewData(
                    text = "decoded",
                    rawBytesHex = "64 65 63 6F 64 65 64",
                    rawBitsBinary = "01100100 01100101 01100011 01101111 01100100 01100101 01100100",
                    textDecodeStatusCode = BagDecodeContentCodes.STATUS_OK,
                    rawPayloadAvailable = true,
                )
            val expectedFollow =
                PayloadFollowViewData(
                    lyricLines = listOf("decoded"),
                    lyricLineTimeline = listOf(TextFollowLyricLineTimelineEntry(12, 8, 0)),
                    lineTokenRanges = listOf(TextFollowLineTokenRangeViewData(0, 0, 1)),
                    lyricLineFollowAvailable = true,
                    hexTokens = listOf("64"),
                    binaryTokens = listOf("01100100"),
                    byteTimeline = listOf(PayloadFollowByteTimelineEntry(12, 8, 0)),
                    binaryGroupTimeline = listOf(PayloadFollowBinaryGroupTimelineEntry(12, 8, 0, 0, 8)),
                    payloadBeginSample = 12,
                    payloadSampleCount = 8,
                    totalPcmSampleCount = 20,
                    followAvailable = true,
                )
            val fixture =
                createFixture(
                    gateway =
                        FakeDecodeAudioCodecGateway(
                            decodedResult =
                                DecodedAudioPayloadResult(
                                    decodedPayload = expected,
                                    followData = expectedFollow,
                                ),
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    sessions =
                        fixture.uiState.value.sessions +
                            (
                                TransportModeOption.Flash to
                                    fixture.uiState.value.sessions.getValue(TransportModeOption.Flash).copy(
                                        generatedPcm = shortArrayOf(1, 2, 3),
                                    )
                            ),
                    currentPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Flash),
                    transportMode = TransportModeOption.Flash,
                )

            fixture.actions.onDecode()
            advanceUntilIdle()

            val session = fixture.uiState.value.sessions.getValue(TransportModeOption.Flash)
            assertEquals(expected, session.decodedPayload)
            assertEquals(expectedFollow, session.followData)
            assertFalse(session.isCodecBusy)
            assertResId(session.statusText, R.string.status_mode_decode_completed)
        }

    @Test
    fun `decode keeps raw payload when text interpretation fails`() =
        runTest {
            val expected =
                DecodedPayloadViewData(
                    text = "",
                    rawBytesHex = "FF 80 41",
                    rawBitsBinary = "11111111 10000000 01000001",
                    textDecodeStatusCode = BagDecodeContentCodes.STATUS_INVALID_TEXT_PAYLOAD,
                    rawPayloadAvailable = true,
                )
            val expectedFollow =
                PayloadFollowViewData(
                    lyricLines = listOf("FF"),
                    lyricLineTimeline = listOf(TextFollowLyricLineTimelineEntry(0, 4, 0)),
                    lineTokenRanges = listOf(TextFollowLineTokenRangeViewData(0, 0, 0)),
                    lyricLineFollowAvailable = false,
                    hexTokens = listOf("FF", "80", "41"),
                    binaryTokens = listOf("1111", "1111"),
                    byteTimeline = listOf(PayloadFollowByteTimelineEntry(0, 4, 0)),
                    binaryGroupTimeline = listOf(PayloadFollowBinaryGroupTimelineEntry(0, 2, 0, 0, 4)),
                    payloadBeginSample = 0,
                    payloadSampleCount = 4,
                    totalPcmSampleCount = 4,
                    followAvailable = true,
                )
            val fixture =
                createFixture(
                    gateway =
                        FakeDecodeAudioCodecGateway(
                            decodedResult =
                                DecodedAudioPayloadResult(
                                    decodedPayload = expected,
                                    followData = expectedFollow,
                                ),
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    sessions =
                        fixture.uiState.value.sessions +
                            (
                                TransportModeOption.Pro to
                                    fixture.uiState.value.sessions.getValue(TransportModeOption.Pro).copy(
                                        generatedPcm = shortArrayOf(4, 5, 6),
                                    )
                            ),
                    currentPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Pro),
                    transportMode = TransportModeOption.Pro,
                )

            fixture.actions.onDecode()
            advanceUntilIdle()

            val session = fixture.uiState.value.sessions.getValue(TransportModeOption.Pro)
            assertTrue(session.decodedPayload.rawPayloadAvailable)
            assertEquals(BagDecodeContentCodes.STATUS_INVALID_TEXT_PAYLOAD, session.decodedPayload.textDecodeStatusCode)
            assertEquals("FF 80 41", session.decodedPayload.rawBytesHex)
            assertEquals(expectedFollow, session.followData)
            assertResId(session.statusText, R.string.status_mode_decode_completed)
        }

    @Test
    fun `clear result resets decoded payload and keeps follow data`() =
        runTest {
            val followData =
                PayloadFollowViewData(
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
            val fixture =
                createFixture(
                    gateway = FakeDecodeAudioCodecGateway(),
                    testScope = this,
                )
            fixture.uiState.value =
                fixture.uiState.value.copy(
                    sessions =
                        fixture.uiState.value.sessions +
                            (
                                TransportModeOption.Flash to
                                    fixture.uiState.value.sessions.getValue(TransportModeOption.Flash).copy(
                                        decodedPayload =
                                            DecodedPayloadViewData(
                                                text = "decoded",
                                                rawBytesHex = "41",
                                                rawBitsBinary = "01000001",
                                                textDecodeStatusCode = BagDecodeContentCodes.STATUS_OK,
                                                rawPayloadAvailable = true,
                                            ),
                                        followData = followData,
                                    )
                            ),
                    currentPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Flash),
                    transportMode = TransportModeOption.Flash,
                )

            fixture.sessionActions.onClearResult()

            val session = fixture.uiState.value.sessions.getValue(TransportModeOption.Flash)
            assertEquals(DecodedPayloadViewData.Empty, session.decodedPayload)
            assertEquals(followData, session.followData)
            assertResId(session.statusText, R.string.status_result_cleared)
        }

    private fun createFixture(
        gateway: AudioCodecGateway,
        testScope: TestScope,
    ): Fixture {
        val dispatcher = StandardTestDispatcher(testScope.testScheduler)
        val uiState = MutableStateFlow(AudioAppUiState())
        val sessionStateStore = AudioSessionStateStore(uiState)
        return Fixture(
            uiState = uiState,
            actions =
                AudioSessionDecodeActions(
                    uiState = uiState,
                    scope = CoroutineScope(dispatcher),
                    audioCodecGateway = gateway,
                    sessionStateStore = sessionStateStore,
                    uiTextMapper = BagUiTextMapper(),
                    sampleRateHz = 44_100,
                    frameSamples = 2_205,
                    workerDispatcher = dispatcher,
                ),
            sessionActions =
                AudioAndroidSessionActions(
                    uiState = uiState,
                    scope = CoroutineScope(dispatcher),
                    audioCodecGateway = gateway,
                    sampleInputTextProvider = LocalFakeSampleInputTextProvider(),
                    sessionStateStore = sessionStateStore,
                    uiTextMapper = BagUiTextMapper(),
                    playbackRuntimeGateway = LocalFakePlaybackRuntimeGateway(),
                    savedAudioRepository = LocalFakeSavedAudioRepository(),
                    sampleRateHz = 44_100,
                    frameSamples = 2_205,
                    stopPlayback = {},
                    refreshSavedAudioItems = {},
                ),
        )
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
        val actions: AudioSessionDecodeActions,
        val sessionActions: AudioAndroidSessionActions,
    )
}

private class FakeDecodeAudioCodecGateway(
    private val decodedResult: DecodedAudioPayloadResult =
        DecodedAudioPayloadResult(
            decodedPayload =
                DecodedPayloadViewData(
                    text = "decoded",
                    rawBytesHex = "64 65 63 6F 64 65 64",
                    rawBitsBinary = "01100100 01100101 01100011 01101111 01100100 01100101 01100100",
                    textDecodeStatusCode = BagDecodeContentCodes.STATUS_OK,
                    rawPayloadAvailable = true,
                ),
        ),
) : AudioCodecGateway {
    override fun validateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int = BagApiCodes.VALIDATION_OK

    override suspend fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (com.bag.audioandroid.domain.EncodeProgressUpdate) -> Unit,
    ) = throw UnsupportedOperationException()

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
    ): DecodedAudioPayloadResult = decodedResult

    override fun getCoreVersion(): String = "test"
}

private class LocalFakeSampleInputTextProvider : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = SampleInput(id = "default", text = "sample")

    override fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        excludingSampleId: String?,
    ): SampleInput = SampleInput(id = "random", text = "sample")

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? = SampleInput(id = sampleId, text = "sample")
}

private class LocalFakeSavedAudioRepository : SavedAudioRepository {
    override fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult = AudioExportResult.Failed

    override fun listSavedAudio(): List<SavedAudioItem> = emptyList()

    override fun loadSavedAudio(itemId: String): SavedAudioContent? = null

    override fun deleteSavedAudio(itemId: String): Boolean = false

    override fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult = SavedAudioRenameResult.Failed

    override fun importAudio(uriString: String): SavedAudioImportResult = SavedAudioImportResult.Failed

    override fun shareSavedAudio(item: SavedAudioItem): Boolean = false
}

private class LocalFakePlaybackRuntimeGateway : PlaybackRuntimeGateway {
    override fun cleared() = com.bag.audioandroid.ui.state.PlaybackUiState()

    override fun load(
        totalSamples: Int,
        sampleRateHz: Int,
    ) = com.bag.audioandroid.ui.state.PlaybackUiState(totalSamples = totalSamples, sampleRateHz = sampleRateHz)

    override fun playStarted(state: com.bag.audioandroid.ui.state.PlaybackUiState) = state

    override fun paused(state: com.bag.audioandroid.ui.state.PlaybackUiState) = state

    override fun resumed(state: com.bag.audioandroid.ui.state.PlaybackUiState) = state

    override fun progress(
        state: com.bag.audioandroid.ui.state.PlaybackUiState,
        playedSamples: Int,
    ) = state

    override fun scrubStarted(state: com.bag.audioandroid.ui.state.PlaybackUiState) = state

    override fun scrubChanged(
        state: com.bag.audioandroid.ui.state.PlaybackUiState,
        targetSamples: Int,
    ) = state

    override fun scrubCommitted(state: com.bag.audioandroid.ui.state.PlaybackUiState) = state

    override fun scrubCanceled(state: com.bag.audioandroid.ui.state.PlaybackUiState) = state

    override fun stopped(state: com.bag.audioandroid.ui.state.PlaybackUiState) = state

    override fun completed(state: com.bag.audioandroid.ui.state.PlaybackUiState) = state

    override fun failed(state: com.bag.audioandroid.ui.state.PlaybackUiState) = state

    override fun clampSamples(
        totalSamples: Int,
        sampleIndex: Int,
    ): Int = sampleIndex

    override fun fractionToSamples(
        totalSamples: Int,
        fraction: Float,
    ): Int = 0

    override fun progressFraction(state: com.bag.audioandroid.ui.state.PlaybackUiState): Float = 0f

    override fun elapsedMs(state: com.bag.audioandroid.ui.state.PlaybackUiState): Long = 0L

    override fun totalMs(state: com.bag.audioandroid.ui.state.PlaybackUiState): Long = 0L
}
