#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "bag/legacy/common/config.h"
#include "bag/legacy/common/error_code.h"
#include "bag/legacy/transport/decoder.h"

namespace bag::ultra {

ErrorCode EncodeFrameBytesToPcm16(const CoreConfig& config,
                                  const std::vector<std::uint8_t>& frame_bytes,
                                  std::vector<std::int16_t>* out_pcm);
ErrorCode DecodePcm16ToFrameBytes(const CoreConfig& config,
                                  const std::vector<std::int16_t>& pcm,
                                  std::vector<std::uint8_t>* out_frame_bytes);
ErrorCode EncodeTextToPcm16Compat(const CoreConfig& config,
                                  const std::string& text,
                                  std::vector<std::int16_t>* out_pcm);
std::unique_ptr<ITransportDecoder> CreateCompatDecoder(const CoreConfig& config);

}  // namespace bag::ultra
