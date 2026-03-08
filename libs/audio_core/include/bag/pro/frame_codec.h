#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

#include "bag/common/config.h"
#include "bag/common/error_code.h"

namespace bag::pro {

inline constexpr uint8_t kFrameVersion = 0x01;
inline constexpr size_t kMaxFramePayloadBytes = 512;

struct DecodedFrame {
    bag::TransportMode mode = bag::TransportMode::kFlash;
    std::vector<uint8_t> payload;
};

uint16_t ComputeCrc16CcittFalse(const std::vector<uint8_t>& data);
ErrorCode EncodeFrame(bag::TransportMode mode,
                      const std::vector<uint8_t>& payload,
                      std::vector<uint8_t>* out_frame);
ErrorCode DecodeFrame(const std::vector<uint8_t>& frame_bytes, DecodedFrame* out_frame);

}  // namespace bag::pro
