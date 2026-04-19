#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "bag/interface/common/config.h"

namespace bag {

struct PcmBlock {
  const std::int16_t* samples = nullptr;
  std::size_t sample_count = 0;
  std::int64_t timestamp_ms = 0;
};

struct IrPacket {
  std::vector<std::uint8_t> bits;
  std::int64_t timestamp_ms = 0;
  float confidence = 0.0f;
};

struct TextResult {
  std::string text;
  bool complete = false;
  float confidence = 0.0f;
  TransportMode mode = TransportMode::kFlash;
};

}  // namespace bag
