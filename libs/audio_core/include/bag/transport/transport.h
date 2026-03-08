#pragma once

#include <memory>
#include <string>
#include <string_view>
#include <vector>

#include "bag/common/config.h"
#include "bag/common/error_code.h"
#include "bag/common/types.h"

namespace bag {

enum class TransportValidationIssue {
    kOk = 0,
    kInvalidSampleRate = 1,
    kInvalidFrameSamples = 2,
    kInvalidMode = 3,
    kProAsciiOnly = 4,
    kPayloadTooLarge = 5,
};

class ITransportDecoder {
public:
    virtual ~ITransportDecoder() = default;

    virtual ErrorCode PushPcm(const PcmBlock& block) = 0;
    virtual ErrorCode PollTextResult(TextResult* out_result) = 0;
    virtual void Reset() = 0;
};

TransportValidationIssue ValidateEncodeRequest(const CoreConfig& config, std::string_view text);
TransportValidationIssue ValidateDecoderConfig(const CoreConfig& config);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<int16_t>* out_pcm);
std::unique_ptr<ITransportDecoder> CreateTransportDecoder(const CoreConfig& config);

}  // namespace bag
