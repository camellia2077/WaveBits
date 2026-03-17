#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

#include "android_bag/flash/signal.h"

namespace bag::flash {

struct FlashVoicingConfig {
    int sample_rate_hz = 0;
    double attack_ratio = 0.0;
    double release_ratio = 0.0;
    double second_harmonic_gain = 0.0;
    double third_harmonic_gain = 0.0;
    double boundary_click_gain = 0.0;
    bool enable_preamble = false;
    bool enable_epilogue = false;
    std::size_t preamble_sample_count = 0;
    std::size_t epilogue_sample_count = 0;
};

struct FlashVoicingDescriptor {
    std::size_t leading_nonpayload_samples = 0;
    std::size_t trailing_nonpayload_samples = 0;
    std::size_t payload_sample_count = 0;
};

struct FlashVoicingResult {
    std::vector<std::int16_t> pcm;
    FlashVoicingDescriptor descriptor;
};

FlashVoicingDescriptor DescribeVoicingOutput(std::size_t total_sample_count,
                                             const FlashVoicingConfig& config = {});

FlashVoicingConfig MakeFormalVoicingConfigForFlavor(const CoreConfig& config,
                                                    FlashVoicingFlavor flavor);

std::vector<std::int16_t> TrimToPayloadPcm(const std::vector<std::int16_t>& voiced_pcm,
                                           const FlashVoicingDescriptor& descriptor);

FlashVoicingResult ApplyVoicingToPayloadWithFlavor(const std::vector<std::int16_t>& clean_payload_pcm,
                                                  const FlashPayloadLayout& payload_layout,
                                                  FlashVoicingFlavor flavor,
                                                  const FlashVoicingConfig& config = {},
                                                  const EncodeProgressSink* progress_sink = nullptr,
                                                  float progress_begin = 0.0f,
                                                  float progress_end = 1.0f);

FlashVoicingResult ApplyVoicingToPayload(const std::vector<std::int16_t>& clean_payload_pcm,
                                         const FlashPayloadLayout& payload_layout,
                                         const FlashVoicingConfig& config = {},
                                         const EncodeProgressSink* progress_sink = nullptr,
                                         float progress_begin = 0.0f,
                                         float progress_end = 1.0f);

}  // namespace bag::flash
