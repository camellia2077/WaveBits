#if !defined(WAVEBITS_MODULE_IMPL_WRAPPER)
#if __cplusplus >= 202002L
module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <cstddef>
#include <cstdint>
#include <vector>
#endif

module bag.transport.compat.frame_codec;
#endif
#endif

namespace bag::transport::compat {

using std::size_t;
using std::uint16_t;
using std::uint8_t;

namespace {

inline constexpr uint8_t kPreamble0 = 0x55;
inline constexpr uint8_t kPreamble1 = 0xAA;
inline constexpr size_t kHeaderBytes = 6;
inline constexpr size_t kCrcBytes = 2;
inline constexpr size_t kMinFrameBytes = kHeaderBytes + kCrcBytes;

uint8_t ModeToFrameValue(bag::TransportMode mode) {
    switch (mode) {
    case bag::TransportMode::kPro:
        return 0x01;
    case bag::TransportMode::kUltra:
        return 0x02;
    case bag::TransportMode::kFlash:
    default:
        return 0x00;
    }
}

bool FrameValueToMode(uint8_t value, bag::TransportMode* out_mode) {
    if (out_mode == nullptr) {
        return false;
    }
    switch (value) {
    case 0x01:
        *out_mode = bag::TransportMode::kPro;
        return true;
    case 0x02:
        *out_mode = bag::TransportMode::kUltra;
        return true;
    default:
        return false;
    }
}

std::vector<uint8_t> SliceBodyForCrc(uint8_t mode_value, const std::vector<uint8_t>& payload) {
    std::vector<uint8_t> body;
    body.reserve(1 + 1 + 2 + payload.size());
    body.push_back(kFrameVersion);
    body.push_back(mode_value);
    body.push_back(static_cast<uint8_t>((payload.size() >> 8) & 0xFF));
    body.push_back(static_cast<uint8_t>(payload.size() & 0xFF));
    body.insert(body.end(), payload.begin(), payload.end());
    return body;
}

}  // namespace

uint16_t ComputeCrc16CcittFalse(const std::vector<uint8_t>& data) {
    uint16_t crc = 0xFFFF;
    for (uint8_t byte : data) {
        crc ^= static_cast<uint16_t>(byte) << 8;
        for (int bit = 0; bit < 8; ++bit) {
            if ((crc & 0x8000U) != 0) {
                crc = static_cast<uint16_t>((crc << 1) ^ 0x1021U);
            } else {
                crc = static_cast<uint16_t>(crc << 1);
            }
        }
    }
    return crc;
}

ErrorCode EncodeFrame(bag::TransportMode mode,
                      const std::vector<uint8_t>& payload,
                      std::vector<uint8_t>* out_frame) {
    if (out_frame == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    if (!bag::IsFramedTransportMode(mode) || payload.size() > kMaxFramePayloadBytes) {
        return ErrorCode::kInvalidArgument;
    }

    const uint8_t mode_value = ModeToFrameValue(mode);
    const std::vector<uint8_t> body = SliceBodyForCrc(mode_value, payload);
    const uint16_t crc = ComputeCrc16CcittFalse(body);

    out_frame->clear();
    out_frame->reserve(kMinFrameBytes + payload.size());
    out_frame->push_back(kPreamble0);
    out_frame->push_back(kPreamble1);
    out_frame->insert(out_frame->end(), body.begin(), body.end());
    out_frame->push_back(static_cast<uint8_t>((crc >> 8) & 0xFF));
    out_frame->push_back(static_cast<uint8_t>(crc & 0xFF));
    return ErrorCode::kOk;
}

ErrorCode DecodeFrame(const std::vector<uint8_t>& frame_bytes, DecodedFrame* out_frame) {
    if (out_frame == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    if (frame_bytes.size() < kMinFrameBytes) {
        return ErrorCode::kInvalidArgument;
    }
    if (frame_bytes[0] != kPreamble0 || frame_bytes[1] != kPreamble1) {
        return ErrorCode::kInvalidArgument;
    }
    if (frame_bytes[2] != kFrameVersion) {
        return ErrorCode::kInvalidArgument;
    }

    bag::TransportMode mode = bag::TransportMode::kFlash;
    if (!FrameValueToMode(frame_bytes[3], &mode)) {
        return ErrorCode::kInvalidArgument;
    }

    const size_t payload_length =
        (static_cast<size_t>(frame_bytes[4]) << 8) | static_cast<size_t>(frame_bytes[5]);
    if (payload_length > kMaxFramePayloadBytes) {
        return ErrorCode::kInvalidArgument;
    }

    const size_t expected_size = kMinFrameBytes + payload_length;
    if (frame_bytes.size() != expected_size) {
        return ErrorCode::kInvalidArgument;
    }

    const std::vector<uint8_t> body(frame_bytes.begin() + 2, frame_bytes.begin() + 6 + payload_length);
    const uint16_t actual_crc = ComputeCrc16CcittFalse(body);
    const uint16_t expected_crc =
        (static_cast<uint16_t>(frame_bytes[6 + payload_length]) << 8) |
        static_cast<uint16_t>(frame_bytes[7 + payload_length]);
    if (actual_crc != expected_crc) {
        return ErrorCode::kInvalidArgument;
    }

    out_frame->mode = mode;
    out_frame->payload.assign(frame_bytes.begin() + 6, frame_bytes.begin() + 6 + payload_length);
    return ErrorCode::kOk;
}

}  // namespace bag::transport::compat
