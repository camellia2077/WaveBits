#include "bag/pro/frame_codec.h"

#include "bag/transport/compat/frame_codec.h"

namespace bag::pro {

uint16_t ComputeCrc16CcittFalse(const std::vector<uint8_t>& data) {
    return bag::transport::compat::ComputeCrc16CcittFalse(data);
}

ErrorCode EncodeFrame(bag::TransportMode mode,
                      const std::vector<uint8_t>& payload,
                      std::vector<uint8_t>* out_frame) {
    return bag::transport::compat::EncodeFrame(mode, payload, out_frame);
}

ErrorCode DecodeFrame(const std::vector<uint8_t>& frame_bytes, DecodedFrame* out_frame) {
    return bag::transport::compat::DecodeFrame(frame_bytes, out_frame);
}

}  // namespace bag::pro
