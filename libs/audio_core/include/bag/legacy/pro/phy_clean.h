#pragma once

#include <array>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "bag/legacy/common/config.h"
#include "bag/legacy/transport/decoder.h"

namespace bag::pro {

struct DualToneConfig {
    std::array<double, 4> low_freqs_hz = {697.0, 770.0, 852.0, 941.0};
    std::array<double, 4> high_freqs_hz = {1209.0, 1336.0, 1477.0, 1633.0};
    int sample_rate_hz = 44100;
    int symbol_samples = 2205;
    double amplitude = 0.8;
};

DualToneConfig MakeDualToneConfig(const CoreConfig& config);

ErrorCode EncodeSymbolsToPcm16(const std::vector<std::uint8_t>& symbols,
                               const DualToneConfig& config,
                               std::vector<std::int16_t>* out_pcm);
ErrorCode DecodePcm16ToSymbols(const std::vector<std::int16_t>& pcm,
                               const DualToneConfig& config,
                               std::vector<std::uint8_t>* out_symbols);

ErrorCode EncodePayloadToPcm16(const std::vector<std::uint8_t>& payload,
                               const DualToneConfig& config,
                               std::vector<std::int16_t>* out_pcm);
ErrorCode DecodePcm16ToPayload(const std::vector<std::int16_t>& pcm,
                               const DualToneConfig& config,
                               std::vector<std::uint8_t>* out_payload);

ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text);
std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config);

}  // namespace bag::pro
