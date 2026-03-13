#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <string_view>
#include <vector>

#include "android_bag/common/config.h"
#include "android_bag/transport/decoder.h"

namespace bag {

enum class TransportValidationIssue {
    kOk = 0,
    kInvalidSampleRate = 1,
    kInvalidFrameSamples = 2,
    kInvalidMode = 3,
    kProAsciiOnly = 4,
    kPayloadTooLarge = 5,
};

TransportValidationIssue ValidateEncodeRequest(const CoreConfig& config, std::string_view text);
TransportValidationIssue ValidateDecoderConfig(const CoreConfig& config);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
std::unique_ptr<ITransportDecoder> CreateTransportDecoder(const CoreConfig& config);

}  // namespace bag
