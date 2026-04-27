#include "test_std_support.h"
#include "audio_io_api.h"
#include "test_audio_io.h"
#include "test_framework.h"
#include "test_fs.h"
#include "wav_io.h"

namespace {

// These version strings are a stable metadata round-trip fixture, not a live
// product-version mirror. Keep them fixed unless the WBAG test contract itself
// changes, so the tests continue to validate encode/decode consistency rather
// than track repository release bumps.
constexpr auto kTestAppVersion = "0.2.2";
constexpr auto kTestCoreVersion = "0.4.1";
constexpr std::size_t kWavChunkPayloadOffset = 44;
constexpr std::size_t kMetadataVersionOffset = kWavChunkPayloadOffset + 0;
constexpr std::size_t kMetadataModeOffset = kWavChunkPayloadOffset + 1;
constexpr std::size_t kMetadataFlashStyleOffset = kWavChunkPayloadOffset + 3;
constexpr std::size_t kMetadataInputSourceKindOffset = kWavChunkPayloadOffset + 4;
constexpr std::size_t kMetadataCreatedAtOffset = kWavChunkPayloadOffset + 12;
constexpr std::size_t kMetadataAppVersionOffset = kWavChunkPayloadOffset + 44;

std::vector<std::uint32_t> MakeValidSegmentSampleCounts(
    std::uint32_t pcm_sample_count) {
    const std::uint32_t first = std::max<std::uint32_t>(1u, pcm_sample_count / 3u);
    const std::uint32_t remaining_after_first = pcm_sample_count - first;
    const std::uint32_t second = std::max<std::uint32_t>(1u, remaining_after_first / 2u);
    const std::uint32_t third = pcm_sample_count - first - second;
    return {first, second, third};
}

audio_io::FlipBitsAudioMetadata MakeValidMetadata(std::uint32_t pcm_sample_count) {
    audio_io::FlipBitsAudioMetadata metadata{};
    metadata.version = 6;
    metadata.mode = audio_io::FlipBitsAudioMetadataMode::kFlash;
    metadata.has_flash_voicing_style = true;
    metadata.flash_voicing_style = audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kDeepRitual;
    metadata.created_at_iso_utc = "2026-03-17T09:45:00Z";
    metadata.duration_ms = 4321u;
    metadata.sample_rate_hz = 44100u;
    metadata.frame_samples = 2205u;
    metadata.pcm_sample_count = pcm_sample_count;
    metadata.payload_byte_count = 37u;
    metadata.input_source_kind = audio_io::FlipBitsAudioMetadataInputSourceKind::kSample;
    metadata.segment_count = 3u;
    metadata.segment_sample_counts = MakeValidSegmentSampleCounts(pcm_sample_count);
    metadata.app_version = kTestAppVersion;
    metadata.core_version = kTestCoreVersion;
    return metadata;
}

const test::AudioIoRoundTripCase& MetadataRoundTripCase() {
    return test::AudioIoRoundTripCases().at(1);
}

std::vector<std::uint8_t> MakeMetadataWavBytes() {
    const auto& test_case = MetadataRoundTripCase();
    return audio_io::SerializeMonoPcm16WavWithMetadata(
        test_case.sample_rate_hz,
        test_case.mono_pcm,
        MakeValidMetadata(static_cast<std::uint32_t>(test_case.mono_pcm.size())));
}

audio_io_string_view MakeStringView(std::string_view value) {
    return audio_io_string_view{value.data(), value.size()};
}

struct ApiMetadataFixture {
    std::vector<std::uint32_t> segment_sample_counts;
    audio_io_metadata_view view{};
};

ApiMetadataFixture MakeApiMetadataView(std::uint32_t pcm_sample_count) {
    ApiMetadataFixture fixture{};
    fixture.segment_sample_counts = MakeValidSegmentSampleCounts(pcm_sample_count);
    fixture.view = audio_io_metadata_view{
        6u,
        AUDIO_IO_METADATA_MODE_FLASH,
        1u,
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_DEEP_RITUAL,
        MakeStringView("2026-03-17T09:45:00Z"),
        4321u,
        44100u,
        2205u,
        pcm_sample_count,
        37u,
        AUDIO_IO_METADATA_INPUT_SOURCE_KIND_SAMPLE,
        3u,
        fixture.segment_sample_counts.data(),
        fixture.segment_sample_counts.size(),
        MakeStringView(kTestAppVersion),
        MakeStringView(kTestCoreVersion),
    };
    return fixture;
}

void TestWavIoHeaderRoundTripContract() {
    const auto dir = test::MakeTempDir("unit");
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto path = dir / (std::string(test_case.name) + ".wav");
        audio_io::WriteMonoPcm16Wav(path, test_case.sample_rate_hz, test_case.mono_pcm);
        const auto read_back = audio_io::ReadMonoPcm16Wav(path);
        test::AssertAudioIoRoundTripResult(read_back, test_case, "Header audio_io boundary");
    }
}

void TestWavIoHeaderBytesRoundTripContract() {
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
        const auto parsed = audio_io::ParseMonoPcm16Wav(wav_bytes);
        test::AssertEq(
            parsed.status,
            audio_io::WavPcm16Status::kOk,
            "Header bytes route should parse canonical mono PCM16 WAV bytes.");
        test::AssertAudioIoRoundTripResult(parsed.wav, test_case, "Header audio_io bytes boundary");
    }
}

void TestWavIoHeaderReadMissingFileFails() {
    const auto missing_path = test::MakeTempDir("unit") / "missing.wav";
    test::AssertThrows(
        [&] {
            (void)audio_io::ReadMonoPcm16Wav(missing_path);
        },
        "Header audio_io boundary should throw when the input file does not exist.");
}

void TestWavIoHeaderRejectsInvalidBytes() {
    const std::vector<std::uint8_t> bad_header = {'N', 'O', 'T', 'W', 'A', 'V', 'E'};
    const auto parsed = audio_io::ParseMonoPcm16Wav(bad_header);
    test::AssertEq(
        parsed.status,
        audio_io::WavPcm16Status::kInvalidHeader,
        "Header bytes route should reject invalid RIFF/WAVE bytes.");
}

void TestWavIoHeaderRejectsUnsupportedStereoBytes() {
    const auto test_case = test::AudioIoRoundTripCases().front();
    auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
    wav_bytes[22] = 0x02;
    wav_bytes[23] = 0x00;
    wav_bytes[32] = 0x04;
    wav_bytes[33] = 0x00;
    const auto stereo_byte_rate = static_cast<std::uint32_t>(test_case.sample_rate_hz * 4);
    wav_bytes[28] = static_cast<std::uint8_t>(stereo_byte_rate & 0xFFu);
    wav_bytes[29] = static_cast<std::uint8_t>((stereo_byte_rate >> 8) & 0xFFu);
    wav_bytes[30] = static_cast<std::uint8_t>((stereo_byte_rate >> 16) & 0xFFu);
    wav_bytes[31] = static_cast<std::uint8_t>((stereo_byte_rate >> 24) & 0xFFu);
    const auto parsed = audio_io::ParseMonoPcm16Wav(wav_bytes);
    test::AssertEq(
        parsed.status,
        audio_io::WavPcm16Status::kUnsupportedFormat,
        "Header bytes route should reject stereo WAV input.");
}

void TestWavIoHeaderRejectsTruncatedDataBytes() {
    const auto test_case = test::AudioIoRoundTripCases().front();
    auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
    wav_bytes.pop_back();
    const auto parsed = audio_io::ParseMonoPcm16Wav(wav_bytes);
    test::AssertEq(
        parsed.status,
        audio_io::WavPcm16Status::kTruncatedData,
        "Header bytes route should reject truncated data chunks.");
}

void TestWavIoCApiRoundTripContract() {
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        audio_io_byte_buffer wav_bytes{};
        const auto encode_status = audio_io_encode_mono_pcm16_wav(
            test_case.sample_rate_hz,
            test_case.mono_pcm.data(),
            test_case.mono_pcm.size(),
            &wav_bytes);
        test::AssertEq(
            encode_status,
            AUDIO_IO_WAV_OK,
            "C ABI WAV encode should succeed for canonical mono PCM16 input.");
        test::AssertTrue(wav_bytes.data != nullptr, "C ABI WAV encode should allocate output bytes.");
        test::AssertTrue(wav_bytes.size > 0, "C ABI WAV encode should produce non-empty bytes.");

        audio_io_decoded_wav decoded{};
        const auto decode_status = audio_io_decode_mono_pcm16_wav(
            wav_bytes.data,
            wav_bytes.size,
            &decoded);
        test::AssertEq(
            decode_status,
            AUDIO_IO_WAV_OK,
            "C ABI WAV decode should succeed for canonical mono PCM16 WAV bytes.");
        test::AssertEq(decoded.sample_rate_hz, test_case.sample_rate_hz, "C ABI decode sample rate should round-trip.");
        test::AssertEq(decoded.channels, 1, "C ABI decode should preserve the mono channel count.");
        test::AssertEq(decoded.sample_count, test_case.mono_pcm.size(), "C ABI decode sample count should round-trip.");
        test::AssertEq(decoded.metadata_status,
                       AUDIO_IO_METADATA_NOT_FOUND,
                       "C ABI decode should surface missing metadata separately for canonical WAV.");
        const bool samples_match =
            decoded.sample_count == test_case.mono_pcm.size() &&
            (decoded.sample_count == 0 ||
             std::equal(decoded.samples, decoded.samples + decoded.sample_count, test_case.mono_pcm.begin()));
        test::AssertTrue(samples_match, "C ABI decode PCM samples should round-trip.");

        audio_io_free_decoded_wav(&decoded);
        audio_io_free_byte_buffer(&wav_bytes);
    }
}

void TestWavIoCApiRejectsInvalidBytes() {
    const std::vector<std::uint8_t> invalid_bytes = {'N', 'O', 'T', 'W', 'A', 'V', 'E'};
    audio_io_decoded_wav decoded{};
    const auto status = audio_io_decode_mono_pcm16_wav(
        invalid_bytes.data(),
        invalid_bytes.size(),
        &decoded);
    test::AssertEq(
        status,
        AUDIO_IO_WAV_INVALID_HEADER,
        "C ABI WAV decode should reject invalid headers.");
    test::AssertContains(
        audio_io_wav_status_message(status),
        "Invalid WAV header",
        "C ABI WAV status message should explain the invalid-header failure.");
    test::AssertEq(decoded.metadata_status,
                   AUDIO_IO_METADATA_INVALID_HEADER,
                   "C ABI decode should surface metadata invalid-header when RIFF/WAVE parsing fails early.");
    audio_io_free_decoded_wav(&decoded);
}

void TestWavIoCApiMetadataRoundTripContract() {
    const auto& test_case = MetadataRoundTripCase();
    auto metadata = MakeApiMetadataView(static_cast<std::uint32_t>(test_case.mono_pcm.size()));

    audio_io_byte_buffer wav_bytes{};
    const auto encode_status = audio_io_encode_mono_pcm16_wav_with_metadata(
        test_case.sample_rate_hz,
        test_case.mono_pcm.data(),
        test_case.mono_pcm.size(),
        &metadata.view,
        &wav_bytes);
    test::AssertEq(
        encode_status,
        AUDIO_IO_WAV_OK,
        "C ABI WAV encode with metadata should succeed.");

    audio_io_decoded_wav decoded{};
    const auto decode_status = audio_io_decode_mono_pcm16_wav(
        wav_bytes.data,
        wav_bytes.size,
        &decoded);
    test::AssertEq(decode_status, AUDIO_IO_WAV_OK, "C ABI decode should succeed for metadata WAV.");
    test::AssertEq(decoded.metadata_status,
                   AUDIO_IO_METADATA_OK,
                   "C ABI unified decode should report metadata success.");
    test::AssertEq(decoded.metadata.version, static_cast<std::uint8_t>(6u), "C ABI metadata version should round-trip.");
    test::AssertEq(decoded.metadata.mode, AUDIO_IO_METADATA_MODE_FLASH, "C ABI metadata mode should round-trip.");
    test::AssertEq(decoded.metadata.has_flash_voicing_style,
                   static_cast<std::uint8_t>(1u),
                   "C ABI metadata flash-style presence should round-trip.");
    test::AssertEq(decoded.metadata.flash_voicing_style,
                   AUDIO_IO_METADATA_FLASH_VOICING_STYLE_DEEP_RITUAL,
                   "C ABI metadata flash-style value should round-trip.");
    test::AssertEq(std::string(decoded.metadata.created_at_iso_utc.data, decoded.metadata.created_at_iso_utc.size),
                   std::string("2026-03-17T09:45:00Z"),
                   "C ABI metadata timestamp should round-trip.");
    test::AssertEq(decoded.metadata.duration_ms, static_cast<std::uint32_t>(4321u), "C ABI metadata duration should round-trip.");
    test::AssertEq(decoded.metadata.sample_rate_hz,
                   static_cast<std::uint32_t>(44100u),
                   "C ABI metadata sample rate should round-trip.");
    test::AssertEq(decoded.metadata.frame_samples, static_cast<std::uint32_t>(2205u), "C ABI frame_samples should round-trip.");
    test::AssertEq(
        decoded.metadata.pcm_sample_count,
        static_cast<std::uint32_t>(test_case.mono_pcm.size()),
        "C ABI metadata PCM sample count should round-trip.");
    test::AssertEq(decoded.metadata.payload_byte_count,
                   static_cast<std::uint32_t>(37u),
                   "C ABI metadata payload byte count should round-trip.");
    test::AssertEq(decoded.metadata.input_source_kind,
                   AUDIO_IO_METADATA_INPUT_SOURCE_KIND_SAMPLE,
                   "C ABI metadata input source kind should round-trip.");
    test::AssertEq(decoded.metadata.segment_count,
                   static_cast<std::uint32_t>(3u),
                   "C ABI metadata segment count should round-trip.");
    test::AssertEq(decoded.metadata.segment_sample_count_count,
                   static_cast<std::size_t>(3u),
                   "C ABI metadata segment boundary count should round-trip.");
    test::AssertTrue(
        std::equal(
            decoded.metadata.segment_sample_counts,
            decoded.metadata.segment_sample_counts +
                decoded.metadata.segment_sample_count_count,
            metadata.segment_sample_counts.begin()),
        "C ABI metadata segment boundaries should round-trip.");
    test::AssertEq(std::string(decoded.metadata.app_version.data, decoded.metadata.app_version.size),
                   std::string(kTestAppVersion),
                   "C ABI metadata app version should round-trip.");
    test::AssertEq(std::string(decoded.metadata.core_version.data, decoded.metadata.core_version.size),
                   std::string(kTestCoreVersion),
                   "C ABI metadata core version should round-trip.");

    audio_io_free_decoded_wav(&decoded);
    audio_io_free_byte_buffer(&wav_bytes);
}

void TestWavIoCApiMetadataMissingOnCanonicalWav() {
    const auto& test_case = MetadataRoundTripCase();
    const auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);

    audio_io_decoded_wav decoded{};
    const auto status = audio_io_decode_mono_pcm16_wav(
        wav_bytes.data(),
        wav_bytes.size(),
        &decoded);
    test::AssertEq(status, AUDIO_IO_WAV_OK, "C ABI decode should still succeed for canonical WAV.");
    test::AssertEq(decoded.metadata_status,
                   AUDIO_IO_METADATA_NOT_FOUND,
                   "C ABI unified decode should report metadata-not-found on canonical WAV.");
    test::AssertContains(
        audio_io_metadata_status_message(decoded.metadata_status),
        "not found",
        "C ABI metadata status message should explain the missing-metadata case.");
    audio_io_free_decoded_wav(&decoded);
}

void TestWavIoCApiTruncatedWavPreservesMetadataStatus() {
    auto wav_bytes = MakeMetadataWavBytes();
    wav_bytes.pop_back();

    audio_io_decoded_wav decoded{};
    const auto status = audio_io_decode_mono_pcm16_wav(
        wav_bytes.data(),
        wav_bytes.size(),
        &decoded);
    test::AssertEq(status,
                   AUDIO_IO_WAV_TRUNCATED_DATA,
                   "C ABI unified decode should return truncated WAV status for truncated payloads.");
    test::AssertEq(decoded.metadata_status,
                   AUDIO_IO_METADATA_OK,
                   "C ABI unified decode should preserve metadata success when WBAG is intact.");
    test::AssertEq(decoded.metadata.mode,
                   AUDIO_IO_METADATA_MODE_FLASH,
                   "C ABI truncated decode should still expose parsed metadata.");
    audio_io_free_decoded_wav(&decoded);
}

void TestWavIoCApiFreeFunctionsAreIdempotentOnZeroedBuffers() {
    audio_io_metadata metadata{};
    audio_io_free_metadata(&metadata);
    audio_io_free_metadata(&metadata);

    audio_io_decoded_wav decoded{};
    audio_io_free_decoded_wav(&decoded);
    audio_io_free_decoded_wav(&decoded);
}

void TestWavIoMetadataRoundTripContract() {
    const auto test_case = MetadataRoundTripCase();
    const auto metadata = MakeValidMetadata(static_cast<std::uint32_t>(test_case.mono_pcm.size()));
    const auto wav_bytes = audio_io::SerializeMonoPcm16WavWithMetadata(
        test_case.sample_rate_hz,
        test_case.mono_pcm,
        metadata);
    test::AssertTrue(!wav_bytes.empty(), "Metadata WAV serialization should succeed.");

    const auto parsed_wav = audio_io::ParseMonoPcm16Wav(wav_bytes);
    test::AssertEq(parsed_wav.status, audio_io::WavPcm16Status::kOk, "PCM parsing should still succeed.");
    test::AssertAudioIoRoundTripResult(parsed_wav.wav, test_case, "Metadata WAV PCM round trip");

    const auto parsed_metadata = audio_io::ParseFlipBitsAudioMetadata(wav_bytes);
    test::AssertEq(parsed_metadata.status, audio_io::FlipBitsAudioMetadataStatus::kOk, "Metadata parse should succeed.");
    test::AssertEq(parsed_metadata.metadata.version, 6u, "Metadata version should round-trip.");
    test::AssertEq(parsed_metadata.metadata.mode,
                   audio_io::FlipBitsAudioMetadataMode::kFlash,
                   "Metadata mode should round-trip.");
    test::AssertTrue(parsed_metadata.metadata.has_flash_voicing_style,
                     "Metadata should preserve the flash voicing style flag.");
    test::AssertEq(parsed_metadata.metadata.flash_voicing_style,
                   audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kDeepRitual,
                   "Metadata flash voicing style should round-trip.");
    test::AssertEq(parsed_metadata.metadata.created_at_iso_utc,
                   std::string("2026-03-17T09:45:00Z"),
                   "Metadata creation time should round-trip.");
    test::AssertEq(parsed_metadata.metadata.duration_ms,
                   static_cast<std::uint32_t>(4321u),
                   "Metadata duration should round-trip.");
    test::AssertEq(parsed_metadata.metadata.sample_rate_hz,
                   static_cast<std::uint32_t>(44100u),
                   "Metadata sample rate should round-trip.");
    test::AssertEq(parsed_metadata.metadata.frame_samples,
                   static_cast<std::uint32_t>(2205u),
                   "Metadata frame sample count should round-trip.");
    test::AssertEq(parsed_metadata.metadata.pcm_sample_count,
                   static_cast<std::uint32_t>(test_case.mono_pcm.size()),
                   "Metadata PCM sample count should round-trip.");
    test::AssertEq(parsed_metadata.metadata.payload_byte_count,
                   static_cast<std::uint32_t>(37u),
                   "Metadata payload byte count should round-trip.");
    test::AssertEq(parsed_metadata.metadata.input_source_kind,
                   audio_io::FlipBitsAudioMetadataInputSourceKind::kSample,
                   "Metadata input source kind should round-trip.");
    test::AssertEq(parsed_metadata.metadata.segment_count,
                   static_cast<std::uint32_t>(3u),
                   "Metadata segment count should round-trip.");
    test::AssertEq(parsed_metadata.metadata.segment_sample_counts,
                   MakeValidSegmentSampleCounts(
                       static_cast<std::uint32_t>(test_case.mono_pcm.size())),
                   "Metadata segment sample counts should round-trip.");
    test::AssertEq(parsed_metadata.metadata.app_version,
                   std::string(kTestAppVersion),
                   "Metadata app version should round-trip.");
    test::AssertEq(parsed_metadata.metadata.core_version,
                   std::string(kTestCoreVersion),
                   "Metadata core version should round-trip.");
}

void TestWavIoMetadataMissingOnCanonicalWav() {
    const auto test_case = MetadataRoundTripCase();
    const auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
    const auto parsed_metadata = audio_io::ParseFlipBitsAudioMetadata(wav_bytes);
    test::AssertEq(parsed_metadata.status,
                   audio_io::FlipBitsAudioMetadataStatus::kNotFound,
                   "Canonical WAV without WBAG chunk should report metadata not found.");
}

void TestWavIoMetadataRespectsChunkPadding() {
    const auto test_case = MetadataRoundTripCase();
    auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
    std::vector<std::uint8_t> padded_bytes;
    padded_bytes.reserve(wav_bytes.size() + 18);
    padded_bytes.insert(padded_bytes.end(), wav_bytes.begin(), wav_bytes.begin() + 36);
    padded_bytes.push_back('J');
    padded_bytes.push_back('U');
    padded_bytes.push_back('N');
    padded_bytes.push_back('K');
    padded_bytes.push_back(1);
    padded_bytes.push_back(0);
    padded_bytes.push_back(0);
    padded_bytes.push_back(0);
    padded_bytes.push_back(0xAB);
    padded_bytes.push_back(0x00);
    padded_bytes.insert(padded_bytes.end(), wav_bytes.begin() + 36, wav_bytes.end());
    const auto riff_size = static_cast<std::uint32_t>(padded_bytes.size() - 8);
    padded_bytes[4] = static_cast<std::uint8_t>(riff_size & 0xFFu);
    padded_bytes[5] = static_cast<std::uint8_t>((riff_size >> 8) & 0xFFu);
    padded_bytes[6] = static_cast<std::uint8_t>((riff_size >> 16) & 0xFFu);
    padded_bytes[7] = static_cast<std::uint8_t>((riff_size >> 24) & 0xFFu);

    const auto parsed = audio_io::ParseMonoPcm16Wav(padded_bytes);
    test::AssertEq(parsed.status,
                   audio_io::WavPcm16Status::kOk,
                   "WAV parser should handle odd-sized chunk padding before data.");
    test::AssertAudioIoRoundTripResult(parsed.wav, test_case, "Chunk padding WAV PCM round trip");
}

void TestWavIoMetadataRejectsOlderVersions() {
    const auto test_case = MetadataRoundTripCase();
    audio_io::FlipBitsAudioMetadata metadata_v2{};
    metadata_v2.version = 2;
    metadata_v2.mode = audio_io::FlipBitsAudioMetadataMode::kUltra;
    metadata_v2.created_at_iso_utc = "2026-03-17T09:45:00Z";
    metadata_v2.duration_ms = 4321u;
    const auto wav_bytes_v2 = audio_io::SerializeMonoPcm16WavWithMetadata(
        test_case.sample_rate_hz,
        test_case.mono_pcm,
        metadata_v2);
    test::AssertTrue(
        wav_bytes_v2.empty(),
        "Serialization should reject older metadata versions now that only v6 is writable.");
}

void TestWavIoMetadataParseRejectsUnsupportedVersion() {
    auto wav_bytes = MakeMetadataWavBytes();
    wav_bytes[kMetadataVersionOffset] = 2u;
    const auto parsed_metadata = audio_io::ParseFlipBitsAudioMetadata(wav_bytes);
    test::AssertEq(parsed_metadata.status,
                   audio_io::FlipBitsAudioMetadataStatus::kUnsupportedVersion,
                   "Metadata parsing should surface unsupported versions from existing WBAG chunks.");
}

void TestWavIoMetadataRejectsSegmentBoundariesThatDoNotMatchPcm() {
    const auto test_case = MetadataRoundTripCase();
    auto metadata = MakeValidMetadata(static_cast<std::uint32_t>(test_case.mono_pcm.size()));
    metadata.segment_sample_counts = {1u, 1u, 1u};
    const auto wav_bytes = audio_io::SerializeMonoPcm16WavWithMetadata(
        test_case.sample_rate_hz,
        test_case.mono_pcm,
        metadata);
    test::AssertTrue(
        wav_bytes.empty(),
        "Serialization should reject segment boundaries that do not add up to the PCM sample count.");
}

void TestWavIoMetadataRejectsTruncatedChunkData() {
    auto wav_bytes = MakeMetadataWavBytes();
    wav_bytes[40] = 0xFFu;
    wav_bytes[41] = 0xFFu;
    wav_bytes[42] = 0xFFu;
    wav_bytes[43] = 0xFFu;
    const auto parsed_metadata = audio_io::ParseFlipBitsAudioMetadata(wav_bytes);
    test::AssertEq(parsed_metadata.status,
                   audio_io::FlipBitsAudioMetadataStatus::kTruncatedData,
                   "Metadata parsing should reject truncated WBAG chunks.");
}

void TestWavIoMetadataRejectsShortChunkPayload() {
    auto wav_bytes = MakeMetadataWavBytes();
    wav_bytes[40] = 79u;
    wav_bytes[41] = 0u;
    wav_bytes[42] = 0u;
    wav_bytes[43] = 0u;
    const auto parsed_metadata = audio_io::ParseFlipBitsAudioMetadata(wav_bytes);
    test::AssertEq(parsed_metadata.status,
                   audio_io::FlipBitsAudioMetadataStatus::kInvalidMetadata,
                   "Metadata parsing should reject short WBAG payloads.");
}

void TestWavIoMetadataRejectsUnknownModeValues() {
    auto wav_bytes = MakeMetadataWavBytes();
    wav_bytes[kMetadataModeOffset] = 99u;
    const auto parsed_metadata = audio_io::ParseFlipBitsAudioMetadata(wav_bytes);
    test::AssertEq(parsed_metadata.status,
                   audio_io::FlipBitsAudioMetadataStatus::kInvalidMetadata,
                   "Metadata parsing should reject unknown transport modes.");
}

void TestWavIoMetadataRejectsUnknownFlashStyleValues() {
    auto wav_bytes = MakeMetadataWavBytes();
    wav_bytes[kMetadataFlashStyleOffset] = 99u;
    const auto parsed_metadata = audio_io::ParseFlipBitsAudioMetadata(wav_bytes);
    test::AssertEq(parsed_metadata.status,
                   audio_io::FlipBitsAudioMetadataStatus::kInvalidMetadata,
                   "Metadata parsing should reject unknown flash voicing styles.");
}

void TestWavIoMetadataRejectsCorruptedFields() {
    {
        auto wav_bytes = MakeMetadataWavBytes();
        wav_bytes[kMetadataCreatedAtOffset] = static_cast<std::uint8_t>('X');
        const auto parsed_metadata = audio_io::ParseFlipBitsAudioMetadata(wav_bytes);
        test::AssertEq(parsed_metadata.status,
                       audio_io::FlipBitsAudioMetadataStatus::kInvalidMetadata,
                       "Metadata parsing should reject corrupted ISO-UTC timestamps.");
    }

    {
        auto wav_bytes = MakeMetadataWavBytes();
        wav_bytes[kMetadataAppVersionOffset] = 0u;
        const auto parsed_metadata = audio_io::ParseFlipBitsAudioMetadata(wav_bytes);
        test::AssertEq(parsed_metadata.status,
                       audio_io::FlipBitsAudioMetadataStatus::kInvalidMetadata,
                       "Metadata parsing should reject empty app-version fields inside WBAG chunks.");
    }
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Unit.WavIoHeaderRoundTripContract", TestWavIoHeaderRoundTripContract);
    runner.Add("Unit.WavIoHeaderBytesRoundTripContract", TestWavIoHeaderBytesRoundTripContract);
    runner.Add("Unit.WavIoHeaderReadMissingFileFails", TestWavIoHeaderReadMissingFileFails);
    runner.Add("Unit.WavIoHeaderRejectsInvalidBytes", TestWavIoHeaderRejectsInvalidBytes);
    runner.Add("Unit.WavIoHeaderRejectsUnsupportedStereoBytes", TestWavIoHeaderRejectsUnsupportedStereoBytes);
    runner.Add("Unit.WavIoHeaderRejectsTruncatedDataBytes", TestWavIoHeaderRejectsTruncatedDataBytes);
    runner.Add("Unit.WavIoCApiRoundTripContract", TestWavIoCApiRoundTripContract);
    runner.Add("Unit.WavIoCApiRejectsInvalidBytes", TestWavIoCApiRejectsInvalidBytes);
    runner.Add("Unit.WavIoCApiMetadataRoundTripContract", TestWavIoCApiMetadataRoundTripContract);
    runner.Add("Unit.WavIoCApiMetadataMissingOnCanonicalWav", TestWavIoCApiMetadataMissingOnCanonicalWav);
    runner.Add("Unit.WavIoCApiTruncatedWavPreservesMetadataStatus", TestWavIoCApiTruncatedWavPreservesMetadataStatus);
    runner.Add("Unit.WavIoCApiFreeFunctionsAreIdempotentOnZeroedBuffers", TestWavIoCApiFreeFunctionsAreIdempotentOnZeroedBuffers);
    runner.Add("Unit.WavIoMetadataRoundTripContract", TestWavIoMetadataRoundTripContract);
    runner.Add("Unit.WavIoMetadataMissingOnCanonicalWav", TestWavIoMetadataMissingOnCanonicalWav);
    runner.Add("Unit.WavIoMetadataRespectsChunkPadding", TestWavIoMetadataRespectsChunkPadding);
    runner.Add("Unit.WavIoMetadataRejectsOlderVersions", TestWavIoMetadataRejectsOlderVersions);
    runner.Add("Unit.WavIoMetadataParseRejectsUnsupportedVersion", TestWavIoMetadataParseRejectsUnsupportedVersion);
    runner.Add("Unit.WavIoMetadataRejectsTruncatedChunkData", TestWavIoMetadataRejectsTruncatedChunkData);
    runner.Add("Unit.WavIoMetadataRejectsShortChunkPayload", TestWavIoMetadataRejectsShortChunkPayload);
    runner.Add("Unit.WavIoMetadataRejectsUnknownModeValues", TestWavIoMetadataRejectsUnknownModeValues);
    runner.Add("Unit.WavIoMetadataRejectsUnknownFlashStyleValues", TestWavIoMetadataRejectsUnknownFlashStyleValues);
    runner.Add("Unit.WavIoMetadataRejectsCorruptedFields", TestWavIoMetadataRejectsCorruptedFields);
    runner.Add("Unit.WavIoMetadataRejectsSegmentBoundariesThatDoNotMatchPcm", TestWavIoMetadataRejectsSegmentBoundariesThatDoNotMatchPcm);
    return runner.Run();
}
