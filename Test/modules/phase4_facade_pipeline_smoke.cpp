#include "test_std_support.h"
#include "test_framework.h"
#include "test_utf8.h"

import bag.common.config;
import bag.common.error_code;
import bag.pipeline;
import bag.transport.facade;

namespace {

bag::CoreConfig MakeConfig(bag::TransportMode mode) {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.enable_diagnostics = false;
    config.mode = mode;
    return config;
}

void PushAndPollExpectingText(std::unique_ptr<bag::ITransportDecoder> decoder,
                              const std::vector<std::int16_t>& pcm,
                              bag::TransportMode mode,
                              std::string_view text) {
    test::AssertTrue(decoder != nullptr, "Transport facade should return a decoder instance.");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 1234;
    test::AssertEq(
        decoder->PushPcm(block),
        bag::ErrorCode::kOk,
        "Transport facade decoder should accept PCM input.");

    bag::TextResult result{};
    test::AssertEq(
        decoder->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Transport facade decoder should decode a result.");
    test::AssertEq(result.text, std::string(text), "Transport facade decoder should preserve decoded text.");
    test::AssertEq(result.mode, mode, "Transport facade decoder should preserve the transport mode.");
}

void TestTransportFacadeValidation() {
    auto config = MakeConfig(bag::TransportMode::kPro);
    const auto non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag::ValidateEncodeRequest(config, non_ascii),
        bag::TransportValidationIssue::kProAsciiOnly,
        "Transport facade module should keep the pro ASCII-only validation rule.");

    config.sample_rate_hz = 0;
    test::AssertEq(
        bag::ValidateDecoderConfig(config),
        bag::TransportValidationIssue::kInvalidSampleRate,
        "Transport facade module should validate decoder sample rate.");
}

void TestTransportFacadeRoundTrip(bag::TransportMode mode, std::string_view text) {
    const auto config = MakeConfig(mode);

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::EncodeTextToPcm16(config, std::string(text), &pcm),
        bag::ErrorCode::kOk,
        "Transport facade module should encode text for the requested mode.");
    test::AssertTrue(!pcm.empty(), "Transport facade module should emit PCM.");

    PushAndPollExpectingText(bag::CreateTransportDecoder(config), pcm, mode, text);
}

void TestTransportFacadeFlashRoundTrip() {
    TestTransportFacadeRoundTrip(
        bag::TransportMode::kFlash,
        test::Utf8Literal(u8"Phase4-Flash"));
}

void TestTransportFacadeProRoundTrip() {
    TestTransportFacadeRoundTrip(bag::TransportMode::kPro, "Phase4-Pro");
}

void TestTransportFacadeUltraRoundTrip() {
    TestTransportFacadeRoundTrip(
        bag::TransportMode::kUltra,
        test::Utf8Literal(u8"Phase4-Ultra-超级"));
}

void TestPipelineRoundTrip() {
    const auto config = MakeConfig(bag::TransportMode::kUltra);
    const std::string input = test::Utf8Literal(u8"Phase4 Pipeline");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Pipeline smoke setup should encode PCM through the facade module.");

    auto pipeline = bag::CreatePipeline(config);
    test::AssertTrue(pipeline != nullptr, "Pipeline module should create a facade-backed pipeline.");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 5678;
    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline module should forward PCM to the underlying transport decoder.");

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Pipeline module should surface decoded text results.");
    test::AssertEq(result.text, input, "Pipeline module should preserve decoded text.");
    test::AssertEq(result.mode, bag::TransportMode::kUltra, "Pipeline module should preserve the transport mode.");

    pipeline->Reset();
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kNotReady,
        "Pipeline reset should clear pending decoder state.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("ModulesPhase4.TransportFacadeValidation", TestTransportFacadeValidation);
    runner.Add("ModulesPhase4.TransportFacadeFlashRoundTrip", TestTransportFacadeFlashRoundTrip);
    runner.Add("ModulesPhase4.TransportFacadeProRoundTrip", TestTransportFacadeProRoundTrip);
    runner.Add("ModulesPhase4.TransportFacadeUltraRoundTrip", TestTransportFacadeUltraRoundTrip);
    runner.Add("ModulesPhase4.PipelineRoundTrip", TestPipelineRoundTrip);
    return runner.Run();
}
