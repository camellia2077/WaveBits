package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodedAudioData
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.GeneratedAudioPcmCacheWriter
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.PlaybackUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
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
class AudioSessionCodecProgressTest {
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
                            flashSignalInfo =
                                FlashSignalInfo(
                                    lowCarrierHz = "300",
                                    highCarrierHz = "600",
                                    bitDurationSamples = "2205",
                                    payloadSilence = "none",
                                    decodePath = "fixed low/high window",
                                    available = true,
                                ),
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
            assertTrue(successSession.generatedFlashSignalInfo.available)
            assertEquals("300", successSession.generatedFlashSignalInfo.lowCarrierHz)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSessionCodecFailureStateTest {
    @Test
    fun `failed encode clears stale generated audio instead of exposing zero duration mini player`() =
        runTest {
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeResult = EncodeAudioResult.Failed(BagApiCodes.ERROR_ENCODED_AUDIO_TOO_LARGE),
                        ),
                    testScope = this,
                )
            val staleMetadata =
                GeneratedAudioMetadata(
                    mode = TransportModeOption.Flash,
                    flashVoicingStyle = FlashVoicingStyleOption.Standard,
                    createdAtIsoUtc = "2026-05-11T00:00:00Z",
                    durationMs = 1_000,
                    sampleRateHz = 44_100,
                    frameSamples = 2_205,
                    pcmSampleCount = 44_100,
                    payloadByteCount = 3,
                    inputSourceKind = GeneratedAudioInputSourceKind.Manual,
                    appVersion = "test",
                    coreVersion = "test",
                )
            fixture.uiState.value =
                fixture.uiState.value.copy(
                    sessions =
                        fixture.uiState.value.sessions.mapValues { (mode, session) ->
                            if (mode == TransportModeOption.Flash) {
                                session.copy(
                                    inputText = "new text",
                                    generatedPcm = shortArrayOf(1, 2, 3),
                                    generatedWaveformPcm = shortArrayOf(1, 2, 3),
                                    generatedAudioMetadata = staleMetadata,
                                    generatedFlashVoicingStyle = FlashVoicingStyleOption.Standard,
                                    generatedFlashSignalInfo =
                                        FlashSignalInfo(
                                            lowCarrierHz = "300",
                                            highCarrierHz = "600",
                                            bitDurationSamples = "2205",
                                            payloadSilence = "none",
                                            decodePath = "fixed low/high window",
                                            available = true,
                                        ),
                                    followData = DefaultFollowData,
                                    playback = PlaybackUiState(totalSamples = 44_100, sampleRateHz = 44_100),
                                )
                            } else {
                                session
                            }
                        },
                )
            assertTrue(fixture.uiState.value.miniPlayerModel != null)

            fixture.actions.onEncode()
            advanceUntilIdle()

            val session = fixture.uiState.value.currentSession
            assertFalse(session.isCodecBusy)
            assertResId(session.statusText, R.string.error_encoded_audio_too_large)
            assertEquals(0, session.generatedPcm.size)
            assertEquals(0, session.generatedWaveformPcm.size)
            assertEquals(null, session.generatedAudioMetadata)
            assertEquals(null, session.generatedFlashVoicingStyle)
            assertFalse(session.generatedFlashSignalInfo.available)
            assertFalse(session.followData.followAvailable)
            assertEquals(0, session.playback.totalSamples)
            assertEquals(null, fixture.uiState.value.miniPlayerModel)
        }

    @Test
    fun `saved playback source survives later generated encode failure`() =
        runTest {
            var encodeCount = 0
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeBlock = { _ ->
                                if (encodeCount++ == 0) {
                                    EncodeAudioResult.Success(shortArrayOf(1, 2, 3), followData = DefaultFollowData)
                                } else {
                                    EncodeAudioResult.Failed(BagApiCodes.ERROR_ENCODED_AUDIO_TOO_LARGE)
                                }
                            },
                        ),
                    testScope = this,
                )

            fixture.actions.onEncode()
            advanceUntilIdle()

            assertTrue(fixture.uiState.value.currentPlaybackSource is AudioPlaybackSource.Generated)
            assertTrue(fixture.uiState.value.miniPlayerModel != null)

            val savedItem =
                SavedAudioItem(
                    itemId = "saved-pro",
                    displayName = "saved-pro.wav",
                    uriString = "content://saved/pro",
                    modeWireName = TransportModeOption.Pro.wireName,
                    durationMs = 2_000L,
                    savedAtEpochSeconds = 1L,
                )
            val savedContent =
                SavedAudioContent(
                    item = savedItem,
                    pcm = shortArrayOf(9, 8, 7, 6),
                    sampleRateHz = 44_100,
                    metadata =
                        GeneratedAudioMetadata(
                            mode = TransportModeOption.Pro,
                            createdAtIsoUtc = "2026-05-11T00:00:00Z",
                            durationMs = 2_000L,
                            sampleRateHz = 44_100,
                            frameSamples = 2_205,
                            pcmSampleCount = 4,
                            payloadByteCount = 4,
                            inputSourceKind = GeneratedAudioInputSourceKind.Manual,
                            appVersion = "test",
                            coreVersion = "test",
                        ),
                )
            val selectionActions =
                AudioSavedAudioSelectionActions(
                    uiState = fixture.uiState,
                    scope = this,
                    playbackRuntimeGateway = FakePlaybackRuntimeGateway(),
                    savedAudioRepository = CodecFakeSavedAudioRepository(mapOf(savedItem.itemId to savedContent)),
                    stopPlayback = {},
                    setCurrentStatusText = {},
                    generatedAudioCacheGateway = CodecFakeGeneratedAudioCacheGateway(),
                    savedAudioDecodeCacheGateway = CodecFakeSavedAudioDecodeCacheGateway(),
                    workerDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            selectionActions.onSavedAudioSelected(savedItem.itemId)

            assertEquals(AudioPlaybackSource.Saved(savedItem.itemId), fixture.uiState.value.currentPlaybackSource)
            assertEquals(
                2_000L,
                fixture.uiState.value.miniPlayerModel
                    ?.durationMs,
            )

            fixture.actions.onEncode()
            advanceUntilIdle()

            val flashSession =
                fixture.uiState.value.sessions
                    .getValue(TransportModeOption.Flash)
            assertEquals(AudioPlaybackSource.Saved(savedItem.itemId), fixture.uiState.value.currentPlaybackSource)
            assertEquals(
                2_000L,
                fixture.uiState.value.miniPlayerModel
                    ?.durationMs,
            )
            assertEquals(0, flashSession.generatedPcm.size)
            assertEquals(null, flashSession.generatedAudioMetadata)
            assertFalse(flashSession.followData.followAvailable)
            assertEquals(0, flashSession.playback.totalSamples)
        }

    @Test
    fun `generated encode decode roundtrip stores decoded text for all modes`() =
        runTest {
            TransportModeOption.entries.forEach { mode ->
                val text = "roundtrip-${mode.wireName}"
                val encodedPcm = shortArrayOf(mode.nativeValue.toShort(), 42)
                val fixture =
                    createFixture(
                        gateway =
                            FakeAudioCodecGateway(
                                encodeResult =
                                    EncodeAudioResult.Success(
                                        pcm = encodedPcm,
                                        followData = DefaultFollowData,
                                    ),
                                decodeBlock = { pcm, nativeMode ->
                                    assertEquals(mode.nativeValue, nativeMode)
                                    assertEquals(encodedPcm.toList(), pcm.toList())
                                    DecodedAudioPayloadResult(
                                        decodedPayload =
                                            DecodedPayloadViewData(
                                                text = text,
                                                rawBytesHex = "72 6F 75 6E 64",
                                                rawBitsBinary = "01110010",
                                                textDecodeStatusCode = BagDecodeContentCodes.STATUS_OK,
                                                rawPayloadAvailable = true,
                                            ),
                                        followData = DefaultFollowData,
                                    )
                                },
                            ),
                        testScope = this,
                    )
                fixture.uiState.value =
                    fixture.uiState.value.copy(
                        transportMode = mode,
                        currentPlaybackSource = AudioPlaybackSource.Generated(mode),
                        sessions =
                            fixture.uiState.value.sessions.mapValues { (sessionMode, session) ->
                                if (sessionMode == mode) {
                                    session.copy(inputText = text)
                                } else {
                                    session
                                }
                            },
                    )

                fixture.actions.onEncode()
                advanceUntilIdle()
                fixture.actions.onDecode()
                advanceUntilIdle()

                val session =
                    fixture.uiState.value.sessions
                        .getValue(mode)
                assertEquals(text, session.decodedPayload.text)
                assertEquals(BagDecodeContentCodes.STATUS_OK, session.decodedPayload.textDecodeStatusCode)
                assertTrue(session.decodedPayload.rawPayloadAvailable)
                assertTrue(session.followData.followAvailable)
                assertFalse(session.isCodecBusy)
                assertResId(session.statusText, R.string.status_mode_decode_completed)
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSessionCodecSegmentationTest {
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
    fun `segmented progress phase does not jump back to preparing between segments`() =
        runTest {
            val longText = "A".repeat(1_025)
            val observedPhases = mutableListOf<AudioEncodePhase>()
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeTextBlock = { text, onProgress ->
                                onProgress(EncodeProgressUpdate(AudioEncodePhase.PreparingInput, 0f))
                                yield()
                                onProgress(EncodeProgressUpdate(AudioEncodePhase.Finalizing, 1f))
                                yield()
                                EncodeAudioResult.Success(ShortArray(text.length))
                            },
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    transportMode = TransportModeOption.Ultra,
                    currentPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Ultra),
                    sessions =
                        fixture.uiState.value.sessions.mapValues { (_, session) ->
                            session.copy(inputText = longText)
                        },
                )

            val collectionJob =
                launch {
                    fixture.uiState.collect { state ->
                        state.sessions.getValue(TransportModeOption.Ultra).encodePhase?.let { phase ->
                            if (observedPhases.lastOrNull() != phase) {
                                observedPhases += phase
                            }
                        }
                    }
                }

            fixture.actions.onEncode()
            advanceUntilIdle()
            collectionJob.cancel()

            assertEquals(
                listOf(
                    AudioEncodePhase.PreparingInput,
                    AudioEncodePhase.RenderingPcm,
                    AudioEncodePhase.Finalizing,
                ),
                observedPhases,
            )
        }

    @Test
    fun `litany long cadence uses smaller payload segments`() =
        runTest {
            val longText = "A".repeat(129)
            val encodedTexts = mutableListOf<String>()
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeTextBlock = { text, _ ->
                                encodedTexts += text
                                EncodeAudioResult.Success(ShortArray(text.length))
                            },
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    selectedFlashVoicingStyle = FlashVoicingStyleOption.Litany,
                    sessions =
                        fixture.uiState.value.sessions.mapValues { (_, session) ->
                            session.copy(inputText = longText)
                        },
                )

            fixture.actions.onEncode()
            advanceUntilIdle()

            assertTrue(encodedTexts.size > 2)
            assertTrue(encodedTexts.all { it.toByteArray(UTF_8).size <= 64 })
            assertEquals(longText, encodedTexts.joinToString(separator = ""))
        }

    @Test
    fun `file backed segmented generation does not allocate wav probe pcm`() =
        runTest {
            val longText = "A".repeat(1_025)
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeTextBlock = { text, _ ->
                                EncodeAudioResult.Success(ShortArray(text.length))
                            },
                        ),
                    audioIoGateway = CodecFakeAudioIoGateway(failOnEncode = true),
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
            val wavInfo = session.generatedWavAudioInfo
            assertEquals(0, session.generatedPcm.size)
            assertTrue(session.generatedPcmFilePath?.let { File(it).exists() } == true)
            assertTrue(wavInfo.isWavSuccess)
            assertEquals(session.generatedAudioMetadata?.pcmSampleCount?.toLong(), wavInfo.pcmSampleCount)
            assertEquals(wavInfo.pcmSampleCount * 2L, wavInfo.dataByteCount)
            assertEquals(44L + wavInfo.dataByteCount, wavInfo.fileByteCount)
        }

    @Test
    fun `very long flash payload still hydrates follow data for visual and lyrics`() =
        runTest {
            val longText = "汉".repeat(8_832)
            val segmentSamples = 110_250
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            encodeTextBlock = { text, _ ->
                                EncodeAudioResult.Success(ShortArray(text.length / 3 + segmentSamples))
                            },
                            buildFollowDataBlock = { text ->
                                val byteCount = text.toByteArray(UTF_8).size
                                val sampleCount = text.length / 3 + segmentSamples
                                DefaultFollowData.copy(
                                    textTokens = listOf(text),
                                    textTokenTimeline = listOf(TextFollowTimelineEntry(0, sampleCount, 0)),
                                    lyricLines = listOf(text),
                                    lyricLineTimeline = listOf(TextFollowLyricLineTimelineEntry(0, sampleCount, 0)),
                                    lineTokenRanges = listOf(TextFollowLineTokenRangeViewData(0, 0, 1)),
                                    hexTokens = List(byteCount) { "E6" },
                                    binaryTokens = List(byteCount) { "11100110" },
                                    byteTimeline = List(byteCount) { index -> PayloadFollowByteTimelineEntry(index * 8, 8, index) },
                                    binaryGroupTimeline =
                                        List(byteCount) { index ->
                                            PayloadFollowBinaryGroupTimelineEntry(index * 8, 8, index, index * 8, 8)
                                        },
                                    payloadSampleCount = byteCount * 8,
                                    totalPcmSampleCount = sampleCount,
                                )
                            },
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    transportMode = TransportModeOption.Flash,
                    sessions =
                        fixture.uiState.value.sessions.mapValues { (_, session) ->
                            session.copy(inputText = longText)
                        },
                )

            fixture.actions.onEncode()
            advanceUntilIdle()

            val session = fixture.uiState.value.currentSession
            assertEquals(26_496, session.generatedAudioMetadata?.payloadByteCount)
            assertTrue((session.generatedAudioMetadata?.segmentCount ?: 0) > 8)
            assertEquals(0, session.generatedPcm.size)
            assertTrue(session.generatedWaveformPcm.isNotEmpty())
            assertTrue(session.followWindowSource != null)
            assertTrue(session.followWindow.endSampleExclusive < session.generatedAudioMetadata!!.pcmSampleCount)
            assertTrue(session.flashVisualWindowSource != null)
            assertTrue(session.flashVisualWindow.available)
            assertTrue(session.flashVisualWindow.segments.isNotEmpty())
            assertTrue(session.followData.followAvailable)
            assertTrue(session.followData.textFollowAvailable)
            assertTrue(session.followData.lyricLineFollowAvailable)
            assertTrue(session.followData.binaryGroupTimeline.isNotEmpty())
            assertTrue(session.followData.textTokens.size < session.generatedAudioMetadata!!.segmentCount)
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
}

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSessionCodecFlashPresetTest {
    @Test
    fun `flash encode uses base preset when voicing effect is disabled`() =
        runTest {
            val seenPresets = mutableListOf<Pair<Int, Int>>()
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            validateEncodeRequestBlock = { _, _, _, _, flashSignalProfile, flashVoicingFlavor ->
                                seenPresets += flashSignalProfile to flashVoicingFlavor
                                BagApiCodes.VALIDATION_OK
                            },
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    transportMode = TransportModeOption.Flash,
                    isFlashVoicingEnabled = false,
                    selectedFlashVoicingStyle = FlashVoicingStyleOption.Hostile,
                    sessions =
                        fixture.uiState.value.sessions.mapValues { (_, session) ->
                            session.copy(inputText = "flash baseline")
                        },
                )

            fixture.actions.onEncode()
            advanceUntilIdle()

            assertTrue(seenPresets.isNotEmpty())
            assertEquals(listOf(0 to 0), seenPresets.distinct())
        }

    @Test
    fun `flash encode sends selected emotion preset axes`() =
        runTest {
            val seenPresets = mutableListOf<Pair<Int, Int>>()
            val fixture =
                createFixture(
                    gateway =
                        FakeAudioCodecGateway(
                            validateEncodeRequestBlock = { _, _, _, _, flashSignalProfile, flashVoicingFlavor ->
                                seenPresets += flashSignalProfile to flashVoicingFlavor
                                BagApiCodes.VALIDATION_OK
                            },
                        ),
                    testScope = this,
                )

            fixture.uiState.value =
                fixture.uiState.value.copy(
                    transportMode = TransportModeOption.Flash,
                    isFlashVoicingEnabled = true,
                    selectedFlashVoicingStyle = FlashVoicingStyleOption.Hostile,
                    sessions =
                        fixture.uiState.value.sessions.mapValues { (_, session) ->
                            session.copy(inputText = "hostile flash")
                        },
                )

            fixture.actions.onEncode()
            advanceUntilIdle()

            assertTrue(seenPresets.isNotEmpty())
            assertEquals(
                listOf(FlashVoicingStyleOption.Hostile.signalProfileValue to FlashVoicingStyleOption.Hostile.voicingFlavorValue),
                seenPresets.distinct(),
            )
        }
}

private fun createFixture(
    gateway: AudioCodecGateway,
    testScope: TestScope,
    audioIoGateway: AudioIoGateway = CodecFakeAudioIoGateway(),
): Fixture {
    val dispatcher = StandardTestDispatcher(testScope.testScheduler)
    val uiState = MutableStateFlow(AudioAppUiState())
    val actions =
        AudioSessionCodecActions(
            uiState = uiState,
            scope = CoroutineScope(dispatcher),
            audioCodecGateway = gateway,
            audioIoGateway = audioIoGateway,
            sessionStateStore = AudioSessionStateStore(uiState),
            uiTextMapper = BagUiTextMapper(),
            playbackRuntimeGateway = FakePlaybackRuntimeGateway(),
            sampleRateHz = 44_100,
            frameSamples = 2_205,
            stopPlayback = {},
            workerDispatcher = dispatcher,
            generatedAudioCacheGateway = CodecFakeGeneratedAudioCacheGateway(),
            savedAudioDecodeCacheGateway = CodecFakeSavedAudioDecodeCacheGateway(),
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

private class FakeAudioCodecGateway(
    private val encodeResult: EncodeAudioResult =
        EncodeAudioResult.Success(
            shortArrayOf(1, 2),
            followData = DefaultFollowData,
        ),
    private val encodeBlock: (suspend ((EncodeProgressUpdate) -> Unit) -> EncodeAudioResult)? = null,
    private val encodeTextBlock: (suspend (String, (EncodeProgressUpdate) -> Unit) -> EncodeAudioResult)? = null,
    private val decodeBlock: ((ShortArray, Int) -> DecodedAudioPayloadResult)? = null,
    private val flashSignalInfo: FlashSignalInfo = FlashSignalInfo.Empty,
    private val validateEncodeRequestBlock: (
        (
            text: String,
            sampleRateHz: Int,
            frameSamples: Int,
            mode: Int,
            flashSignalProfile: Int,
            flashVoicingFlavor: Int,
        ) -> Int
    )? = null,
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

    override fun describeFlashSignal(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ) = flashSignalInfo

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
    ): DecodedAudioPayloadResult = decodeBlock?.invoke(pcm, mode) ?: DecodedAudioPayloadResult()

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

private class CodecFakeAudioIoGateway(
    private val failOnEncode: Boolean = false,
) : AudioIoGateway {
    override fun encodeMonoPcm16ToWavBytes(
        sampleRateHz: Int,
        pcm: ShortArray,
        metadata: GeneratedAudioMetadata?,
    ): ByteArray {
        check(!failOnEncode) { "WAV encode should not be called for file-backed generated audio." }
        return ByteArray(44 + pcm.size * 2)
    }

    override fun decodeMonoPcm16WavBytes(wavBytes: ByteArray): DecodedAudioData =
        DecodedAudioData(
            wavStatusCode = com.bag.audioandroid.domain.AudioIoWavCodes.STATUS_OK,
            metadataStatusCode = com.bag.audioandroid.domain.AudioIoMetadataCodes.STATUS_NOT_FOUND,
            sampleRateHz = 44_100,
            channels = 1,
            pcm = shortArrayOf(),
        )

    override fun probeMonoPcm16WavBytes(wavBytes: ByteArray): WavAudioInfo =
        WavAudioInfo(
            wavStatusCode = com.bag.audioandroid.domain.AudioIoWavCodes.STATUS_OK,
            sampleRateHz = 44_100,
            channels = 1,
            bitsPerSample = 16,
            pcmSampleCount = ((wavBytes.size - 44) / 2).coerceAtLeast(0).toLong(),
            dataByteCount = (wavBytes.size - 44).coerceAtLeast(0).toLong(),
            fileByteCount = wavBytes.size.toLong(),
            durationMs = 0L,
        )
}

private class CodecFakeSavedAudioRepository(
    private val savedContentById: Map<String, SavedAudioContent> = emptyMap(),
) : SavedAudioRepository {
    override fun suggestGeneratedAudioDisplayName(
        mode: TransportModeOption,
        inputText: String,
    ): String = "generated.wav"

    override fun exportGeneratedAudio(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult = AudioExportResult.Failed

    override fun exportGeneratedAudioToDocument(
        mode: TransportModeOption,
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
        destinationUriString: String,
    ): Boolean = false

    override fun listSavedAudio(): List<SavedAudioItem> = savedContentById.values.map { it.item }

    override fun loadSavedAudio(itemId: String): SavedAudioContent? = savedContentById[itemId]

    override fun deleteSavedAudio(itemId: String): Boolean = false

    override fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult = SavedAudioRenameResult.Failed

    override fun importAudio(uriString: String): SavedAudioImportResult = SavedAudioImportResult.Failed

    override fun exportSavedAudioToDocument(
        itemId: String,
        destinationUriString: String,
    ): Boolean = false

    override fun shareSavedAudio(item: SavedAudioItem): Boolean = false
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

    override fun pruneCachedFiles(retainedPaths: Set<String>) {
        retainedPaths
            .filter { it.isNotBlank() }
            .map(::File)
            .forEach { it.deleteOnExit() }
    }
}

private class CodecFakeSavedAudioDecodeCacheGateway : SavedAudioDecodeCacheGateway {
    override fun read(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
    ) = null

    override fun write(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
        decodedPayload: DecodedPayloadViewData,
        followData: PayloadFollowViewData,
        flashSignalInfo: FlashSignalInfo,
    ) = Unit

    override fun delete(itemId: String) = Unit

    override fun prune(validItemIds: Set<String>) = Unit
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
