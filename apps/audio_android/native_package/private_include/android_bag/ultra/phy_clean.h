#pragma once

#include <array>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "android_bag/common/config.h"
#include "android_bag/transport/decoder.h"

namespace bag::ultra {

struct Mfsk16Config {
    std::array<double, 16> freqs_hz = {
        1000.0, 1140.0, 1280.0, 1420.0,
        1560.0, 1700.0, 1840.0, 1980.0,
        2120.0, 2260.0, 2400.0, 2540.0,
        2680.0, 2820.0, 2960.0, 3100.0};
    int sample_rate_hz = 44100;
    int symbol_samples = 2205;
    double amplitude = 0.8;
};

Mfsk16Config MakeMfsk16Config(const CoreConfig& config);

ErrorCode EncodeSymbolsToPcm16(const std::vector<std::uint8_t>& symbols,
                               const Mfsk16Config& config,
                               std::vector<std::int16_t>* out_pcm);
ErrorCode DecodePcm16ToSymbols(const std::vector<std::int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<std::uint8_t>* out_symbols);

ErrorCode EncodePayloadToPcm16(const std::vector<std::uint8_t>& payload,
                               const Mfsk16Config& config,
                               std::vector<std::int16_t>* out_pcm);
ErrorCode DecodePcm16ToPayload(const std::vector<std::int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<std::uint8_t>* out_payload);

ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text);
std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config);

}  // namespace bag::ultra
