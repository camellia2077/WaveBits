#include "test_std_support.h"
#include "test_framework.h"

import bag.common.config;
import bag.common.error_code;
import bag.pipeline;
import bag.transport.facade;

#include "facade_pipeline_smoke_support.h"

namespace modules_facade_pipeline_smoke {

bag::CoreConfig MakeConfig(bag::TransportMode mode) {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.enable_diagnostics = false;
    config.mode = mode;
    return config;
}

void PushPcmInChunks(bag::ITransportDecoder* decoder,
                     const std::vector<std::int16_t>& pcm,
                     std::size_t chunk_size,
                     const std::string& message_prefix) {
    test::AssertTrue(decoder != nullptr, message_prefix + " decoder should exist before chunked push.");
    const std::size_t normalized_chunk_size = std::max<std::size_t>(1, chunk_size);
    for (std::size_t offset = 0; offset < pcm.size(); offset += normalized_chunk_size) {
        bag::PcmBlock block{};
        block.samples = pcm.data() + offset;
        block.sample_count = std::min(normalized_chunk_size, pcm.size() - offset);
        block.timestamp_ms = static_cast<decltype(block.timestamp_ms)>(offset);
        test::AssertEq(
            decoder->PushPcm(block),
            bag::ErrorCode::kOk,
            message_prefix + " chunked decoder push should succeed.");
    }
}

void PushPcmInChunks(bag::IPipeline* pipeline,
                     const std::vector<std::int16_t>& pcm,
                     std::size_t chunk_size,
                     const std::string& message_prefix) {
    test::AssertTrue(pipeline != nullptr, message_prefix + " pipeline should exist before chunked push.");
    const std::size_t normalized_chunk_size = std::max<std::size_t>(1, chunk_size);
    for (std::size_t offset = 0; offset < pcm.size(); offset += normalized_chunk_size) {
        bag::PcmBlock block{};
        block.samples = pcm.data() + offset;
        block.sample_count = std::min(normalized_chunk_size, pcm.size() - offset);
        block.timestamp_ms = static_cast<decltype(block.timestamp_ms)>(offset);
        test::AssertEq(
            pipeline->PushPcm(block),
            bag::ErrorCode::kOk,
            message_prefix + " chunked pipeline push should succeed.");
    }
}

}  // namespace modules_facade_pipeline_smoke
