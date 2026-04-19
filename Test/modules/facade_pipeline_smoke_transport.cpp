#include "test_std_support.h"
#include "test_framework.h"
#include "test_utf8.h"

import bag.common.config;
import bag.common.error_code;
import bag.pipeline;
import bag.transport.facade;

#include "facade_pipeline_smoke_support.h"

namespace {

using namespace modules_facade_pipeline_smoke;

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

void TestTransportFacadeChunkedRoundTrip() {
    struct Case {
        bag::TransportMode mode;
        std::string text;
    };
    const std::array<Case, 3> cases = {{
        {bag::TransportMode::kFlash, test::Utf8Literal(u8"Facade-Flash-你好")},
        {bag::TransportMode::kPro, "Facade-Pro-123"},
        {bag::TransportMode::kUltra, test::Utf8Literal(u8"Facade-Ultra-超级")},
    }};

    for (const auto& item : cases) {
        const auto config = MakeConfig(item.mode);
        std::vector<std::int16_t> pcm;
        test::AssertEq(
            bag::EncodeTextToPcm16(config, item.text, &pcm),
            bag::ErrorCode::kOk,
            "Chunked facade encode should succeed.");

        auto decoder = bag::CreateTransportDecoder(config);
        PushPcmInChunks(decoder.get(), pcm, std::max<std::size_t>(1, pcm.size() / static_cast<std::size_t>(6)),
                        "Transport facade");

        bag::TextResult result{};
        test::AssertEq(
            decoder->PollTextResult(&result),
            bag::ErrorCode::kOk,
            "Chunked transport facade poll should succeed.");
        test::AssertEq(result.text, item.text, "Chunked transport facade should preserve decoded text.");
        test::AssertEq(result.mode, item.mode, "Chunked transport facade should preserve transport mode.");
    }
}

}  // namespace

namespace modules_facade_pipeline_smoke {

void RegisterTransportFacadeSmokeTests(test::Runner& runner) {
    runner.Add("ModulesFacadePipeline.TransportFacadeValidation", TestTransportFacadeValidation);
    runner.Add("ModulesFacadePipeline.TransportFacadeFlashRoundTrip", TestTransportFacadeFlashRoundTrip);
    runner.Add("ModulesFacadePipeline.TransportFacadeProRoundTrip", TestTransportFacadeProRoundTrip);
    runner.Add("ModulesFacadePipeline.TransportFacadeUltraRoundTrip", TestTransportFacadeUltraRoundTrip);
    runner.Add("ModulesFacadePipeline.TransportFacadeChunkedRoundTrip", TestTransportFacadeChunkedRoundTrip);
}

}  // namespace modules_facade_pipeline_smoke
