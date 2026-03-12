#include "test_std_support.h"
#include "test_framework.h"
#include "test_utf8.h"
#include "test_vectors.h"

import bag.common.config;
import bag.common.error_code;
import bag.flash.codec;
import bag.flash.phy_clean;
import bag.pipeline;
import bag.pro.codec;
import bag.pro.phy_clean;
import bag.transport.facade;
import bag.ultra.codec;
import bag.ultra.phy_clean;

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

std::unique_ptr<bag::IPipeline> MakePipeline(
    bag::TransportMode mode = bag::TransportMode::kFlash) {
    return bag::CreatePipeline(MakeCoreConfig(mode));
}

std::unique_ptr<bag::ITransportDecoder> MakeTransportDecoder(
    bag::TransportMode mode = bag::TransportMode::kFlash) {
    return bag::CreateTransportDecoder(MakeCoreConfig(mode));
}

std::vector<std::int16_t> EncodeForModeReference(bag::TransportMode mode, const std::string& text) {
    const auto config = MakeCoreConfig(mode);
    std::vector<std::int16_t> pcm;
    if (mode == bag::TransportMode::kFlash) {
        test::AssertEq(
            bag::flash::EncodeTextToPcm16(config, text, &pcm),
            bag::ErrorCode::kOk,
            "Flash clean module encode should succeed.");
        return pcm;
    }

    if (mode == bag::TransportMode::kPro) {
        test::AssertEq(
            bag::pro::EncodeTextToPcm16(config, text, &pcm),
            bag::ErrorCode::kOk,
            "Pro clean module encode should succeed.");
        return pcm;
    }

    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(config, text, &pcm),
        bag::ErrorCode::kOk,
        "Ultra clean module encode should succeed.");
    return pcm;
}

std::vector<std::int16_t> EncodeForModeFacade(bag::TransportMode mode, const std::string& text) {
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::EncodeTextToPcm16(MakeCoreConfig(mode), text, &pcm),
        bag::ErrorCode::kOk,
        "Transport facade module encode should succeed.");
    return pcm;
}

void PushAndPollExpectingText(std::unique_ptr<bag::IPipeline> pipeline,
                              const std::vector<std::int16_t>& pcm,
                              bag::TransportMode mode,
                              std::string_view text) {
    test::AssertTrue(pipeline != nullptr, "Pipeline module should return an instance.");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 123;

    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline module push should succeed for encoded PCM.");

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Pipeline module poll should succeed after encoded PCM push.");
    test::AssertEq(result.text, std::string(text), "Pipeline module should recover the original text.");
    test::AssertTrue(result.complete, "Pipeline module result should be marked complete.");
    test::AssertEq(result.confidence, 1.0f, "Pipeline module confidence should remain simplified.");
    test::AssertEq(result.mode, mode, "Pipeline module should preserve the decoded transport mode.");
}

void PushAndPollViaTransportDecoderExpectingText(std::unique_ptr<bag::ITransportDecoder> decoder,
                                                 const std::vector<std::int16_t>& pcm,
                                                 bag::TransportMode mode,
                                                 std::string_view text) {
    test::AssertTrue(decoder != nullptr, "Transport facade module should return a decoder.");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 456;

    test::AssertEq(
        decoder->PushPcm(block),
        bag::ErrorCode::kOk,
        "Transport decoder module push should succeed for encoded PCM.");

    bag::TextResult result{};
    test::AssertEq(
        decoder->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Transport decoder module poll should succeed after encoded PCM push.");
    test::AssertEq(result.text, std::string(text), "Transport decoder module should recover original text.");
    test::AssertTrue(result.complete, "Transport decoder module result should be complete.");
    test::AssertEq(result.confidence, 1.0f, "Transport decoder module confidence should remain simplified.");
    test::AssertEq(result.mode, mode, "Transport decoder module should preserve configured mode.");
}

void TestFlashCodecRoundTrip() {
    const std::string text = test::Utf8Literal(u8"你好，WaveBits");
    std::vector<std::uint8_t> bytes;
    test::AssertEq(
        bag::flash::EncodeTextToBytes(text, &bytes),
        bag::ErrorCode::kOk,
        "Flash codec module should accept raw UTF-8 bytes.");
    test::AssertEq(
        bytes,
        std::vector<std::uint8_t>(text.begin(), text.end()),
        "Flash codec module should preserve the original raw bytes.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodeBytesToText(bytes, &decoded),
        bag::ErrorCode::kOk,
        "Flash codec module decode should succeed.");
    test::AssertEq(decoded, text, "Flash codec module should roundtrip raw UTF-8 text.");
}

void TestFlashPhyCleanRoundTrip() {
    const auto config = MakeCoreConfig(bag::TransportMode::kFlash);
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, test::Utf8Literal(u8"你好，WaveBits"), &pcm),
        bag::ErrorCode::kOk,
        "Flash clean module encode should succeed.");
    test::AssertTrue(!pcm.empty(), "Flash clean module should emit PCM for non-empty input.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Flash clean module decode should succeed.");
    test::AssertEq(
        decoded,
        test::Utf8Literal(u8"你好，WaveBits"),
        "Flash clean module should roundtrip UTF-8 text.");
}

void TestTransportFacadeEncodeMatchesCleanModes() {
    const auto flash_pcm = EncodeForModeFacade(
        bag::TransportMode::kFlash,
        test::Utf8Literal(u8"你好，WaveBits"));
    test::AssertEq(
        flash_pcm,
        EncodeForModeReference(
            bag::TransportMode::kFlash,
            test::Utf8Literal(u8"你好，WaveBits")),
        "Flash transport facade module should delegate to the flash clean path.");

    const auto pro_pcm = EncodeForModeFacade(bag::TransportMode::kPro, "Hello-123");
    test::AssertEq(
        pro_pcm,
        EncodeForModeReference(bag::TransportMode::kPro, "Hello-123"),
        "Pro transport facade module should delegate to the pro clean path.");

    const auto ultra_pcm = EncodeForModeFacade(
        bag::TransportMode::kUltra,
        test::Utf8Literal(u8"WaveBits 超级模式 🚀"));
    test::AssertEq(
        ultra_pcm,
        EncodeForModeReference(
            bag::TransportMode::kUltra,
            test::Utf8Literal(u8"WaveBits 超级模式 🚀")),
        "Ultra transport facade module should delegate to the ultra clean path.");
}

void TestTransportFacadeValidation() {
    auto config = MakeCoreConfig();
    config.sample_rate_hz = 0;
    test::AssertEq(
        bag::ValidateEncodeRequest(config, "A"),
        bag::TransportValidationIssue::kInvalidSampleRate,
        "Transport facade module should reject zero sample rate.");

    config = MakeCoreConfig();
    config.frame_samples = 0;
    test::AssertEq(
        bag::ValidateEncodeRequest(config, "A"),
        bag::TransportValidationIssue::kInvalidFrameSamples,
        "Transport facade module should reject zero frame size.");

    config = MakeCoreConfig();
    config.mode = static_cast<bag::TransportMode>(99);
    test::AssertEq(
        bag::ValidateDecoderConfig(config),
        bag::TransportValidationIssue::kInvalidMode,
        "Transport facade module should reject unknown modes.");

    config = MakeCoreConfig(bag::TransportMode::kFlash);
    test::AssertEq(
        bag::ValidateEncodeRequest(config, std::string(513, 'F')),
        bag::TransportValidationIssue::kOk,
        "Flash facade validation should not inherit the old framed payload limit.");
    const auto flash_utf8 = test::Utf8Literal(u8"你好，WaveBits");
    test::AssertEq(
        bag::ValidateEncodeRequest(config, flash_utf8),
        bag::TransportValidationIssue::kOk,
        "Flash facade validation should continue to allow raw UTF-8 text.");

    config = MakeCoreConfig(bag::TransportMode::kPro);
    const auto pro_non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag::ValidateEncodeRequest(config, pro_non_ascii),
        bag::TransportValidationIssue::kProAsciiOnly,
        "Transport facade module should keep the pro ASCII-only rule.");
    test::AssertEq(
        bag::ValidateEncodeRequest(config, test::BuildTooLongProCorpus()),
        bag::TransportValidationIssue::kOk,
        "Pro facade validation should not inherit the old compat single-frame limit.");

    config = MakeCoreConfig(bag::TransportMode::kUltra);
    test::AssertEq(
        bag::ValidateEncodeRequest(config, test::BuildTooLongUltraCorpus()),
        bag::TransportValidationIssue::kOk,
        "Ultra facade validation should not inherit the old compat single-frame limit.");
}

void TestProPhyCleanRoundTrip() {
    const auto config = MakeCoreConfig(bag::TransportMode::kPro);
    const std::string text = "Hello-123";
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::pro::EncodeTextToPcm16(config, text, &pcm),
        bag::ErrorCode::kOk,
        "Pro clean module encode should succeed.");
    test::AssertEq(
        pcm.size(),
        text.size() * bag::pro::kSymbolsPerPayloadByte * static_cast<std::size_t>(config.frame_samples),
        "Pro clean module PCM length should be byte count * 2 symbols * frame size.");

    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Pro clean module decode should succeed.");
    test::AssertEq(decoded, text, "Pro clean module should roundtrip ASCII text.");
}

void TestProPayloadUsesRawAsciiBytes() {
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeTextToPayload(test::BuildMaxProCorpus(), &payload),
        bag::ErrorCode::kOk,
        "Pro codec module should accept the representative long ASCII corpus.");
    test::AssertEq(
        payload.size(),
        test::BuildMaxProCorpus().size(),
        "Pro codec module payload length should remain equal to the ASCII byte count.");

    test::AssertEq(
        bag::pro::EncodeTextToPayload(test::BuildTooLongProCorpus(), &payload),
        bag::ErrorCode::kOk,
        "Pro codec module should keep accepting ASCII text beyond the old compat limit.");
    test::AssertEq(
        payload.size(),
        test::BuildTooLongProCorpus().size(),
        "Pro codec module payload should stay as raw ASCII bytes for longer corpus inputs.");
}

void TestUltraTextCodecRoundTrip() {
    std::vector<std::uint8_t> payload;
    const std::string input = test::Utf8Literal(u8"WaveBits 超级模式 🚀");
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(input, &payload),
        bag::ErrorCode::kOk,
        "Ultra codec module payload encode should succeed.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePayloadToText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Ultra codec module payload decode should succeed.");
    test::AssertEq(decoded, input, "Ultra codec module payload decode should preserve UTF-8 bytes.");

    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "Ultra codec module payload-to-symbol encode should succeed.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload(symbols, &decoded_payload),
        bag::ErrorCode::kOk,
        "Ultra codec module symbol decode should succeed.");
    test::AssertEq(decoded_payload, payload, "Ultra codec module symbol decode should recover UTF-8 bytes.");
}

void TestUltraPhyCleanRoundTrip() {
    const auto config = MakeCoreConfig(bag::TransportMode::kUltra);
    const std::string input = test::Utf8Literal(u8"WaveBits 超级模式 🚀");
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Ultra clean module encode should succeed.");
    test::AssertEq(
        pcm.size(),
        std::vector<std::uint8_t>(input.begin(), input.end()).size() *
            bag::ultra::kSymbolsPerPayloadByte * static_cast<std::size_t>(config.frame_samples),
        "Ultra clean module PCM length should be byte count * 2 symbols * frame size.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Ultra clean module decode should succeed.");
    test::AssertEq(decoded, input, "Ultra clean module should roundtrip UTF-8 text.");
}

void TestUltraPayloadUsesUtf8Bytes() {
    std::vector<std::uint8_t> payload;
    const std::string max_input = test::BuildMaxUltraCorpus();
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(max_input, &payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should accept representative large UTF-8 input.");
    test::AssertEq(
        payload.size(),
        static_cast<std::size_t>(512),
        "Ultra representative corpus should occupy exactly 512 UTF-8 bytes.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePayloadToText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Ultra representative payload decode should succeed.");
    test::AssertEq(decoded, max_input, "Ultra representative payload decode should preserve UTF-8 bytes.");

    const std::string too_long_input = test::BuildTooLongUltraCorpus();
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(too_long_input, &payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should keep accepting UTF-8 input beyond the old compat limit.");
    test::AssertEq(
        payload.size(),
        static_cast<std::size_t>(513),
        "Extended ultra corpus should occupy 513 UTF-8 bytes.");
}

void TestPipelinePushPollLifecycle() {
    auto pipeline = MakePipeline();
    const auto pcm = EncodeForModeFacade(bag::TransportMode::kFlash, "PIPE");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 123;

    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline module push should succeed.");

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Pipeline module poll should succeed after push.");
    test::AssertEq(result.text, std::string("PIPE"), "Pipeline module should decode the original text.");
    test::AssertTrue(result.complete, "Pipeline module result should be marked complete.");
    test::AssertEq(result.confidence, 1.0f, "Pipeline module confidence should match simplified value.");
    test::AssertEq(result.mode, bag::TransportMode::kFlash, "Flash pipeline module should report flash mode.");

    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kNotReady,
        "Pipeline module should report not ready after pending result is consumed.");
    test::AssertEq(result.text, std::string(), "Pipeline module should clear text on not ready.");
    test::AssertTrue(!result.complete, "Pipeline module complete flag should reset on not ready.");
}

void TestPipelineResetClearsPendingState() {
    auto pipeline = MakePipeline();
    const auto pcm = EncodeForModeFacade(bag::TransportMode::kFlash, "RESET");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();

    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline module push before reset should succeed.");
    pipeline->Reset();

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kNotReady,
        "Pipeline module reset should clear pending decode state.");
    test::AssertEq(result.text, std::string(), "Pipeline module reset should clear buffered text state.");
    test::AssertTrue(!result.complete, "Pipeline module reset should clear completion state.");
}

void TestPipelineFlashUtf8RoundTrip() {
    const auto flash_utf8 = test::Utf8Literal(u8"你好，WaveBits");
    PushAndPollExpectingText(
        MakePipeline(bag::TransportMode::kFlash),
        EncodeForModeReference(bag::TransportMode::kFlash, flash_utf8),
        bag::TransportMode::kFlash,
        flash_utf8);
}

void TestPipelineProRoundTrip() {
    PushAndPollExpectingText(
        MakePipeline(bag::TransportMode::kPro),
        EncodeForModeReference(bag::TransportMode::kPro, "Hello-123"),
        bag::TransportMode::kPro,
        "Hello-123");
}

void TestPipelineUltraRoundTrip() {
    const auto ultra_utf8 = test::Utf8Literal(u8"WaveBits 超级模式 🚀");
    PushAndPollExpectingText(
        MakePipeline(bag::TransportMode::kUltra),
        EncodeForModeReference(bag::TransportMode::kUltra, ultra_utf8),
        bag::TransportMode::kUltra,
        ultra_utf8);
}

void TestTransportDecoderRoundTripAcrossModes() {
    const auto flash_utf8 = test::Utf8Literal(u8"你好，WaveBits");
    PushAndPollViaTransportDecoderExpectingText(
        MakeTransportDecoder(bag::TransportMode::kFlash),
        EncodeForModeFacade(bag::TransportMode::kFlash, flash_utf8),
        bag::TransportMode::kFlash,
        flash_utf8);
    PushAndPollViaTransportDecoderExpectingText(
        MakeTransportDecoder(bag::TransportMode::kPro),
        EncodeForModeFacade(bag::TransportMode::kPro, "Hello-123"),
        bag::TransportMode::kPro,
        "Hello-123");
    const auto ultra_utf8 = test::Utf8Literal(u8"WaveBits 超级模式 🚀");
    PushAndPollViaTransportDecoderExpectingText(
        MakeTransportDecoder(bag::TransportMode::kUltra),
        EncodeForModeFacade(bag::TransportMode::kUltra, ultra_utf8),
        bag::TransportMode::kUltra,
        ultra_utf8);
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("ModulesPhase10.FlashCodecRoundTrip", TestFlashCodecRoundTrip);
    runner.Add("ModulesPhase10.FlashPhyCleanRoundTrip", TestFlashPhyCleanRoundTrip);
    runner.Add("ModulesPhase10.TransportFacadeEncodeMatchesCleanModes", TestTransportFacadeEncodeMatchesCleanModes);
    runner.Add("ModulesPhase10.TransportFacadeValidation", TestTransportFacadeValidation);
    runner.Add("ModulesPhase10.ProPhyCleanRoundTrip", TestProPhyCleanRoundTrip);
    runner.Add("ModulesPhase10.ProPayloadUsesRawAsciiBytes", TestProPayloadUsesRawAsciiBytes);
    runner.Add("ModulesPhase10.UltraTextCodecRoundTrip", TestUltraTextCodecRoundTrip);
    runner.Add("ModulesPhase10.UltraPhyCleanRoundTrip", TestUltraPhyCleanRoundTrip);
    runner.Add("ModulesPhase10.UltraPayloadUsesUtf8Bytes", TestUltraPayloadUsesUtf8Bytes);
    runner.Add("ModulesPhase10.PipelinePushPollLifecycle", TestPipelinePushPollLifecycle);
    runner.Add("ModulesPhase10.PipelineResetClearsPendingState", TestPipelineResetClearsPendingState);
    runner.Add("ModulesPhase10.PipelineFlashUtf8RoundTrip", TestPipelineFlashUtf8RoundTrip);
    runner.Add("ModulesPhase10.PipelineProRoundTrip", TestPipelineProRoundTrip);
    runner.Add("ModulesPhase10.PipelineUltraRoundTrip", TestPipelineUltraRoundTrip);
    runner.Add("ModulesPhase10.TransportDecoderRoundTripAcrossModes", TestTransportDecoderRoundTripAcrossModes);
    return runner.Run();
}
