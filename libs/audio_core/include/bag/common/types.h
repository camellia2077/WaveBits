#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "bag/common/config.h"

namespace bag {

struct PcmBlock {
    const int16_t* samples = nullptr;
    size_t sample_count = 0;
    int64_t timestamp_ms = 0;
};

struct IrPacket {
    std::vector<uint8_t> bits;
    int64_t timestamp_ms = 0;
    float confidence = 0.0f;
};

struct TextResult {
    std::string text;
    bool complete = false;
    float confidence = 0.0f;
    TransportMode mode = TransportMode::kFlash;
};

}  // namespace bag
