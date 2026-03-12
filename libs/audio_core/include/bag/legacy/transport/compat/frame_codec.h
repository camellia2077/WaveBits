#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

#include "bag/legacy/common/config.h"
#include "bag/legacy/common/error_code.h"

namespace bag::transport::compat {

inline constexpr std::uint8_t kFrameVersion = 0x01;
inline constexpr std::size_t kMaxFramePayloadBytes = 512;

struct DecodedFrame {
    bag::TransportMode mode = bag::TransportMode::kFlash;
    std::vector<std::uint8_t> payload;
};

std::uint16_t ComputeCrc16CcittFalse(const std::vector<std::uint8_t>& data);
ErrorCode EncodeFrame(bag::TransportMode mode,
                      const std::vector<std::uint8_t>& payload,
                      std::vector<std::uint8_t>* out_frame);
ErrorCode DecodeFrame(const std::vector<std::uint8_t>& frame_bytes, DecodedFrame* out_frame);

}  // namespace bag::transport::compat
