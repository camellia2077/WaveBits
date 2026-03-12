#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "bag/legacy/common/config.h"
#include "bag/legacy/transport/decoder.h"

namespace bag::flash {

struct BfskConfig {
    double low_freq_hz = 400.0;
    double high_freq_hz = 800.0;
    double bit_duration_sec = 0.05;
    int sample_rate_hz = 44100;
    double amplitude = 0.8;
};

BfskConfig MakeBfskConfig(const CoreConfig& config);

std::vector<std::int16_t> EncodeBytesToPcm16(const std::vector<std::uint8_t>& bytes,
                                             const BfskConfig& config = {});
std::vector<std::uint8_t> DecodePcm16ToBytes(const std::vector<std::int16_t>& pcm,
                                             const BfskConfig& config = {});

ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text);
std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config);

}  // namespace bag::flash
