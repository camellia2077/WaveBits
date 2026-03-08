#include <algorithm>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "bag/common/config.h"
#include "bag/common/error_code.h"
#include "bag/common/types.h"
#include "bag/fsk/fsk_codec.h"
#include "bag/pipeline/pipeline.h"
#include "bag/pro/frame_codec.h"
#include "bag/pro/text_codec.h"
#include "bag/transport/transport.h"
#include "test_framework.h"
#include "test_fs.h"
#include "test_vectors.h"
#include "wav_io.h"

namespace {

bag::CoreConfig MakeCoreConfig(bag::TransportMode mode = bag::TransportMode::kFlash) {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.enable_diagnostics = false;
    config.mode = mode;
    config.reserved = 0;
    return config;
}

bag::fsk::FskConfig MakeFskConfig() {
    bag::fsk::FskConfig config{};
    config.sample_rate_hz = 44100;
    config.bit_duration_sec = 0.05;
    return config;
}

std::unique_ptr<bag::IPipeline> MakePipeline(
    bag::TransportMode mode = bag::TransportMode::kFlash) {
    return bag::CreatePipeline(MakeCoreConfig(mode));
}

std::unique_ptr<bag::ITransportDecoder> MakeTransportDecoder(
    bag::TransportMode mode = bag::TransportMode::kFlash) {
    return bag::CreateTransportDecoder(MakeCoreConfig(mode));
}

std::vector<int16_t> EncodeForModeCompat(bag::TransportMode mode, const std::string& text) {
    const auto fsk_config = MakeFskConfig();
    if (mode == bag::TransportMode::kFlash) {
        return bag::fsk::EncodeTextToPcm16(text, fsk_config);
    }

    std::vector<uint8_t> payload;
    bag::ErrorCode payload_code = bag::ErrorCode::kInvalidArgument;
    if (mode == bag::TransportMode::kPro) {
        payload_code = bag::pro::EncodeProTextToPayload(text, &payload);
    } else if (mode == bag::TransportMode::kUltra) {
        payload_code = bag::pro::EncodeUltraTextToPayload(text, &payload);
    }
    test::AssertEq(payload_code, bag::ErrorCode::kOk, "Transport payload encode should succeed.");

    std::vector<uint8_t> frame;
    const auto frame_code = bag::pro::EncodeFrame(mode, payload, &frame);
    test::AssertEq(frame_code, bag::ErrorCode::kOk, "Transport frame encode should succeed.");
    return bag::fsk::EncodeTextToPcm16(std::string(frame.begin(), frame.end()), fsk_config);
}

std::vector<int16_t> EncodeForModeFacade(bag::TransportMode mode, const std::string& text) {
    std::vector<int16_t> pcm;
    test::AssertEq(
        bag::EncodeTextToPcm16(MakeCoreConfig(mode), text, &pcm),
        bag::ErrorCode::kOk,
        "Transport facade encode should succeed.");
    return pcm;
}

void PushAndPollExpectingText(bag::TransportMode mode, const std::string& text) {
    auto pipeline = MakePipeline(mode);
    const auto pcm = EncodeForModeCompat(mode, text);

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 123;

    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline push should succeed for encoded PCM.");

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Pipeline poll should succeed after encoded PCM push.");
    test::AssertEq(result.text, text, "Pipeline should recover the original text.");
    test::AssertTrue(result.complete, "Pipeline result should be marked complete.");
    test::AssertEq(result.confidence, 1.0f, "Pipeline confidence should match the simplified value.");
    test::AssertEq(result.mode, mode, "Pipeline should report the decoded transport mode.");
}

void PushAndPollViaTransportDecoderExpectingText(bag::TransportMode mode, const std::string& text) {
    auto decoder = MakeTransportDecoder(mode);
    test::AssertTrue(decoder != nullptr, "Transport decoder should be created for valid mode.");
    const auto pcm = EncodeForModeFacade(mode, text);

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 456;

    test::AssertEq(
        decoder->PushPcm(block),
        bag::ErrorCode::kOk,
        "Transport decoder push should succeed for encoded PCM.");

    bag::TextResult result{};
    test::AssertEq(
        decoder->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Transport decoder poll should succeed after encoded PCM push.");
    test::AssertEq(result.text, text, "Transport decoder should recover original text.");
    test::AssertTrue(result.complete, "Transport decoder result should be complete.");
    test::AssertEq(result.confidence, 1.0f, "Transport decoder confidence should remain simplified.");
    test::AssertEq(result.mode, mode, "Transport decoder should preserve configured mode.");
}

void TestEncodeLengthMatchesExpected() {
    const auto config = MakeFskConfig();
    const std::string text = "A";
    const auto pcm = bag::fsk::EncodeTextToPcm16(text, config);
    const size_t chunk_size =
        static_cast<size_t>(config.sample_rate_hz * config.bit_duration_sec);
    test::AssertEq(
        pcm.size(),
        static_cast<size_t>(8) * chunk_size,
        "Encoded sample count should be 8 bits * chunk size for one byte.");
}

void TestEncodeAmplitudeInRange() {
    const auto config = MakeFskConfig();
    const auto pcm = bag::fsk::EncodeTextToPcm16("Hello", config);
    const auto [min_it, max_it] = std::minmax_element(pcm.begin(), pcm.end());
    test::AssertTrue(min_it != pcm.end(), "PCM should not be empty for non-empty input.");
    test::AssertTrue(*min_it >= static_cast<int16_t>(-32767), "PCM min out of range.");
    test::AssertTrue(*max_it <= static_cast<int16_t>(32767), "PCM max out of range.");
}

void TestDecodeEmptyInputReturnsEmptyText() {
    const auto config = MakeFskConfig();
    const std::vector<int16_t> pcm;
    const std::string decoded = bag::fsk::DecodePcm16ToText(pcm, config);
    test::AssertEq(decoded, std::string(), "Decoding empty input should return empty string.");
}

void TestWavIoMonoRoundTrip() {
    const auto config = MakeFskConfig();
    const std::string text = "Unit";
    const auto pcm = bag::fsk::EncodeTextToPcm16(text, config);

    const auto dir = test::MakeTempDir("unit");
    const auto path = dir / "mono_roundtrip.wav";
    audio_io::WriteMonoPcm16Wav(path, config.sample_rate_hz, pcm);

    const auto read_back = audio_io::ReadMonoPcm16Wav(path);
    test::AssertEq(
        read_back.sample_rate_hz,
        config.sample_rate_hz,
        "Sample rate should be preserved in wav read/write.");
    test::AssertEq(
        read_back.mono_pcm.size(),
        pcm.size(),
        "Sample count should be preserved in mono wav read/write.");
    test::AssertEq(read_back.mono_pcm, pcm, "PCM content should be identical after roundtrip.");
}

void TestPipelinePushPollLifecycle() {
    auto pipeline = MakePipeline();
    const auto config = MakeFskConfig();
    const auto pcm = bag::fsk::EncodeTextToPcm16("PIPE", config);

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 123;

    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline push should succeed.");

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Pipeline poll should succeed after push.");
    test::AssertEq(result.text, std::string("PIPE"), "Pipeline should decode the original text.");
    test::AssertTrue(result.complete, "Pipeline result should be marked complete.");
    test::AssertEq(result.confidence, 1.0f, "Pipeline confidence should match simplified value.");
    test::AssertEq(result.mode, bag::TransportMode::kFlash, "Flash pipeline should report flash mode.");

    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kNotReady,
        "Pipeline should report not ready after pending result is consumed.");
    test::AssertEq(result.text, std::string(), "Pipeline should clear text on not ready.");
    test::AssertTrue(!result.complete, "Pipeline complete flag should reset on not ready.");
}

void TestPipelineResetClearsPendingState() {
    auto pipeline = MakePipeline();
    const auto config = MakeFskConfig();
    const auto pcm = bag::fsk::EncodeTextToPcm16("RESET", config);

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();

    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline push before reset should succeed.");
    pipeline->Reset();

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kNotReady,
        "Pipeline reset should clear pending decode state.");
    test::AssertEq(result.text, std::string(), "Reset should clear buffered text state.");
    test::AssertTrue(!result.complete, "Reset should clear completion state.");
}

void TestSnapshotFirstSamplesStable() {
    const auto config = MakeFskConfig();
    const auto pcm = bag::fsk::EncodeTextToPcm16("A", config);
    const std::vector<int16_t> expected = {
        0, 1493, 2981, 4459, 5924, 7368, 8789, 10182,
        11541, 12863, 14143, 15377, 16561, 17692, 18765, 19777};

    test::AssertTrue(pcm.size() >= expected.size(), "PCM must contain enough samples for snapshot.");
    for (size_t index = 0; index < expected.size(); ++index) {
        if (pcm[index] != expected[index]) {
            test::Fail("Snapshot mismatch at sample index " + std::to_string(index));
        }
    }
}

void TestTransportFacadeEncodeMatchesCompatibility() {
    const auto flash_pcm = EncodeForModeFacade(bag::TransportMode::kFlash, u8"你好，WaveBits");
    test::AssertEq(
        flash_pcm,
        EncodeForModeCompat(bag::TransportMode::kFlash, u8"你好，WaveBits"),
        "Flash transport facade should preserve existing BFSK output.");

    const auto pro_pcm = EncodeForModeFacade(bag::TransportMode::kPro, "Hello-123");
    test::AssertEq(
        pro_pcm,
        EncodeForModeCompat(bag::TransportMode::kPro, "Hello-123"),
        "Pro transport facade should preserve current framed-over-FSK output.");

    const auto ultra_pcm = EncodeForModeFacade(bag::TransportMode::kUltra, u8"WaveBits 超级模式 🚀");
    test::AssertEq(
        ultra_pcm,
        EncodeForModeCompat(bag::TransportMode::kUltra, u8"WaveBits 超级模式 🚀"),
        "Ultra transport facade should preserve current framed-over-FSK output.");
}

void TestTransportFacadeValidation() {
    auto config = MakeCoreConfig();
    config.sample_rate_hz = 0;
    test::AssertEq(
        bag::ValidateEncodeRequest(config, "A"),
        bag::TransportValidationIssue::kInvalidSampleRate,
        "Transport validation should reject zero sample rate.");

    config = MakeCoreConfig();
    config.frame_samples = 0;
    test::AssertEq(
        bag::ValidateEncodeRequest(config, "A"),
        bag::TransportValidationIssue::kInvalidFrameSamples,
        "Transport validation should reject zero frame size.");

    config = MakeCoreConfig();
    config.mode = static_cast<bag::TransportMode>(99);
    test::AssertEq(
        bag::ValidateDecoderConfig(config),
        bag::TransportValidationIssue::kInvalidMode,
        "Transport decoder validation should reject unknown modes.");

    config = MakeCoreConfig(bag::TransportMode::kPro);
    test::AssertEq(
        bag::ValidateEncodeRequest(config, u8"中文"),
        bag::TransportValidationIssue::kProAsciiOnly,
        "Transport validation should keep the pro ASCII-only rule.");

    config = MakeCoreConfig(bag::TransportMode::kUltra);
    test::AssertEq(
        bag::ValidateEncodeRequest(config, test::BuildTooLongUltraCorpus()),
        bag::TransportValidationIssue::kPayloadTooLarge,
        "Transport validation should expose the single-frame payload limit.");
}

void TestProTextCodecRoundTrip() {
    std::vector<uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeProTextToPayload("A", &payload),
        bag::ErrorCode::kOk,
        "Single-character pro payload encode should succeed.");
    test::AssertEq(
        std::string(payload.begin(), payload.end()),
        std::string("065"),
        "Pro payload should store fixed 3-digit ASCII values.");

    payload.clear();
    test::AssertEq(
        bag::pro::EncodeProTextToPayload("Hello-123", &payload),
        bag::ErrorCode::kOk,
        "ASCII pro payload encode should succeed.");
    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToProText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Pro payload decode should succeed.");
    test::AssertEq(decoded, std::string("Hello-123"), "Pro payload decode should recover the original text.");
}

void TestProTextCodecRejectsInvalidInput() {
    std::vector<uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeProTextToPayload(u8"中文", &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro payload encode should reject non-ASCII input.");

    const std::vector<uint8_t> bad_length = {'0', '6'};
    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToProText(bad_length, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Pro payload decode should reject non-triplet payload length.");

    const std::vector<uint8_t> bad_digits = {'0', '6', 'X'};
    test::AssertEq(
        bag::pro::DecodePayloadToProText(bad_digits, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Pro payload decode should reject non-decimal groups.");
}

void TestProPayloadBoundary() {
    std::vector<uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeProTextToPayload(test::BuildMaxProCorpus(), &payload),
        bag::ErrorCode::kOk,
        "Pro payload encode should accept the max single-frame ASCII corpus.");
    test::AssertEq(
        payload.size(),
        static_cast<size_t>(510),
        "Pro max corpus should expand to 510 payload bytes.");

    std::vector<uint8_t> frame;
    test::AssertEq(
        bag::pro::EncodeFrame(bag::TransportMode::kPro, payload, &frame),
        bag::ErrorCode::kOk,
        "Frame encode should accept the max single-frame pro payload.");

    test::AssertEq(
        bag::pro::EncodeProTextToPayload(test::BuildTooLongProCorpus(), &payload),
        bag::ErrorCode::kOk,
        "Pro text codec should still encode payloads above frame size.");
    test::AssertEq(
        payload.size(),
        static_cast<size_t>(513),
        "Too-long pro corpus should expand to 513 payload bytes.");
    test::AssertEq(
        bag::pro::EncodeFrame(bag::TransportMode::kPro, payload, &frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame encode should reject pro payloads above the single-frame limit.");
}

void TestUltraTextCodecRoundTrip() {
    std::vector<uint8_t> payload;
    const std::string input = u8"WaveBits 超级模式 🚀";
    test::AssertEq(
        bag::pro::EncodeUltraTextToPayload(input, &payload),
        bag::ErrorCode::kOk,
        "Ultra payload encode should succeed.");

    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToUltraText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Ultra payload decode should succeed.");
    test::AssertEq(decoded, input, "Ultra payload decode should preserve UTF-8 bytes.");
}

void TestUltraPayloadBoundary() {
    std::vector<uint8_t> payload;
    const std::string max_input = test::BuildMaxUltraCorpus();
    test::AssertEq(
        bag::pro::EncodeUltraTextToPayload(max_input, &payload),
        bag::ErrorCode::kOk,
        "Ultra payload encode should accept the max single-frame UTF-8 corpus.");
    test::AssertEq(
        payload.size(),
        static_cast<size_t>(512),
        "Ultra max corpus should occupy exactly 512 UTF-8 bytes.");

    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToUltraText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Ultra max payload decode should succeed.");
    test::AssertEq(decoded, max_input, "Ultra max payload decode should preserve UTF-8 bytes.");

    std::vector<uint8_t> frame;
    test::AssertEq(
        bag::pro::EncodeFrame(bag::TransportMode::kUltra, payload, &frame),
        bag::ErrorCode::kOk,
        "Frame encode should accept the max single-frame ultra payload.");

    const std::string too_long_input = test::BuildTooLongUltraCorpus();
    test::AssertEq(
        bag::pro::EncodeUltraTextToPayload(too_long_input, &payload),
        bag::ErrorCode::kOk,
        "Ultra text codec should still encode payloads above frame size.");
    test::AssertEq(
        payload.size(),
        static_cast<size_t>(513),
        "Too-long ultra corpus should occupy 513 UTF-8 bytes.");
    test::AssertEq(
        bag::pro::EncodeFrame(bag::TransportMode::kUltra, payload, &frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame encode should reject ultra payloads above the single-frame limit.");
}

void TestFrameCodecRoundTrip() {
    std::vector<uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeProTextToPayload("Frame", &payload),
        bag::ErrorCode::kOk,
        "Frame test payload encode should succeed.");

    std::vector<uint8_t> frame;
    test::AssertEq(
        bag::pro::EncodeFrame(bag::TransportMode::kPro, payload, &frame),
        bag::ErrorCode::kOk,
        "Frame encode should succeed.");

    bag::pro::DecodedFrame decoded_frame{};
    test::AssertEq(
        bag::pro::DecodeFrame(frame, &decoded_frame),
        bag::ErrorCode::kOk,
        "Frame decode should succeed.");
    test::AssertEq(decoded_frame.mode, bag::TransportMode::kPro, "Decoded frame mode should match input.");
    test::AssertEq(decoded_frame.payload, payload, "Decoded frame payload should match input.");
}

void TestFrameCodecRejectsMalformedFrames() {
    std::vector<uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeUltraTextToPayload(u8"你好", &payload),
        bag::ErrorCode::kOk,
        "Malformed frame setup payload encode should succeed.");

    std::vector<uint8_t> frame;
    test::AssertEq(
        bag::pro::EncodeFrame(bag::TransportMode::kUltra, payload, &frame),
        bag::ErrorCode::kOk,
        "Malformed frame setup encode should succeed.");

    bag::pro::DecodedFrame decoded_frame{};

    auto bad_preamble = frame;
    bad_preamble[0] = 0x00;
    test::AssertEq(
        bag::pro::DecodeFrame(bad_preamble, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject bad preamble.");

    auto bad_version = frame;
    bad_version[2] = 0x02;
    test::AssertEq(
        bag::pro::DecodeFrame(bad_version, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject bad version.");

    auto bad_mode = frame;
    bad_mode[3] = 0x00;
    test::AssertEq(
        bag::pro::DecodeFrame(bad_mode, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject bad mode.");

    auto bad_length = frame;
    bad_length[5] = static_cast<uint8_t>(bad_length[5] + 1);
    test::AssertEq(
        bag::pro::DecodeFrame(bad_length, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject mismatched payload length.");

    auto bad_crc = frame;
    bad_crc.back() ^= 0x01;
    test::AssertEq(
        bag::pro::DecodeFrame(bad_crc, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject CRC mismatch.");

    std::vector<uint8_t> oversized_payload(bag::pro::kMaxFramePayloadBytes + 1, static_cast<uint8_t>('A'));
    test::AssertEq(
        bag::pro::EncodeFrame(bag::TransportMode::kPro, oversized_payload, &frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame encode should reject payloads above the single-frame limit.");
}

void TestPipelineProRoundTrip() {
    PushAndPollExpectingText(bag::TransportMode::kPro, "Hello-123");
}

void TestPipelineFlashUtf8RoundTrip() {
    PushAndPollExpectingText(bag::TransportMode::kFlash, u8"你好，WaveBits");
}

void TestPipelineUltraRoundTrip() {
    PushAndPollExpectingText(bag::TransportMode::kUltra, u8"WaveBits 超级模式 🚀");
}

void TestTransportDecoderRoundTripAcrossModes() {
    PushAndPollViaTransportDecoderExpectingText(bag::TransportMode::kFlash, u8"你好，WaveBits");
    PushAndPollViaTransportDecoderExpectingText(bag::TransportMode::kPro, "Hello-123");
    PushAndPollViaTransportDecoderExpectingText(bag::TransportMode::kUltra, u8"WaveBits 超级模式 🚀");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Unit.EncodeLengthMatchesExpected", TestEncodeLengthMatchesExpected);
    runner.Add("Unit.EncodeAmplitudeInRange", TestEncodeAmplitudeInRange);
    runner.Add("Unit.DecodeEmptyInputReturnsEmptyText", TestDecodeEmptyInputReturnsEmptyText);
    runner.Add("Unit.WavIoMonoRoundTrip", TestWavIoMonoRoundTrip);
    runner.Add("Unit.PipelinePushPollLifecycle", TestPipelinePushPollLifecycle);
    runner.Add("Unit.PipelineResetClearsPendingState", TestPipelineResetClearsPendingState);
    runner.Add("Unit.SnapshotFirstSamplesStable", TestSnapshotFirstSamplesStable);
    runner.Add("Unit.TransportFacadeEncodeMatchesCompatibility", TestTransportFacadeEncodeMatchesCompatibility);
    runner.Add("Unit.TransportFacadeValidation", TestTransportFacadeValidation);
    runner.Add("Unit.ProTextCodecRoundTrip", TestProTextCodecRoundTrip);
    runner.Add("Unit.ProTextCodecRejectsInvalidInput", TestProTextCodecRejectsInvalidInput);
    runner.Add("Unit.ProPayloadBoundary", TestProPayloadBoundary);
    runner.Add("Unit.UltraTextCodecRoundTrip", TestUltraTextCodecRoundTrip);
    runner.Add("Unit.UltraPayloadBoundary", TestUltraPayloadBoundary);
    runner.Add("Unit.FrameCodecRoundTrip", TestFrameCodecRoundTrip);
    runner.Add("Unit.FrameCodecRejectsMalformedFrames", TestFrameCodecRejectsMalformedFrames);
    runner.Add("Unit.PipelineFlashUtf8RoundTrip", TestPipelineFlashUtf8RoundTrip);
    runner.Add("Unit.PipelineProRoundTrip", TestPipelineProRoundTrip);
    runner.Add("Unit.PipelineUltraRoundTrip", TestPipelineUltraRoundTrip);
    runner.Add("Unit.TransportDecoderRoundTripAcrossModes", TestTransportDecoderRoundTripAcrossModes);
    return runner.Run();
}
