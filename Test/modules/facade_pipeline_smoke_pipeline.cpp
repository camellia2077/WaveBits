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

void TestPipelineChunkedRoundTrip() {
    const auto config = MakeConfig(bag::TransportMode::kUltra);
    const std::string input = test::Utf8Literal(u8"Pipeline-Chunked-超级");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Chunked pipeline encode should succeed.");

    auto pipeline = bag::CreatePipeline(config);
    test::AssertTrue(pipeline != nullptr, "Chunked pipeline test should create a pipeline.");
    PushPcmInChunks(pipeline.get(), pcm, std::max<std::size_t>(1, pcm.size() / static_cast<std::size_t>(5)),
                    "Pipeline");

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Chunked pipeline poll should succeed.");
    test::AssertEq(result.text, input, "Chunked pipeline should preserve decoded text.");
    test::AssertEq(result.mode, bag::TransportMode::kUltra, "Chunked pipeline should preserve transport mode.");
}

}  // namespace

namespace modules_facade_pipeline_smoke {

void RegisterPipelineSmokeTests(test::Runner& runner) {
    runner.Add("ModulesFacadePipeline.PipelineRoundTrip", TestPipelineRoundTrip);
    runner.Add("ModulesFacadePipeline.PipelineChunkedRoundTrip", TestPipelineChunkedRoundTrip);
}

}  // namespace modules_facade_pipeline_smoke
