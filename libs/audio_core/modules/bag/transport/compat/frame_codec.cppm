module;

#if !defined(WAVEBITS_CORE_IMPORT_STD)
#include <cstddef>
#include <cstdint>
#include <vector>
#endif

export module bag.transport.compat.frame_codec;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#endif

export import bag.common.config;
export import bag.common.error_code;

export namespace bag::transport::compat {

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
