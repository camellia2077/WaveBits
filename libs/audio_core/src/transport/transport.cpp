#include "bag/transport/transport.h"

#include "bag/flash/phy_clean.h"
#include "bag/pro/codec.h"
#include "bag/pro/phy_compat.h"
#include "bag/transport/compat/frame_codec.h"
#include "bag/ultra/codec.h"
#include "bag/ultra/phy_compat.h"

namespace bag {
namespace {

bool IsAsciiText(std::string_view text) {
    for (unsigned char ch : text) {
        if (ch > 0x7F) {
            return false;
        }
    }
    return true;
}

TransportValidationIssue ValidateCommonConfig(const CoreConfig& config) {
    if (config.sample_rate_hz <= 0) {
        return TransportValidationIssue::kInvalidSampleRate;
    }
    if (config.frame_samples <= 0) {
        return TransportValidationIssue::kInvalidFrameSamples;
    }
    if (!IsValidTransportMode(config.mode)) {
        return TransportValidationIssue::kInvalidMode;
    }
    return TransportValidationIssue::kOk;
}

}  // namespace

TransportValidationIssue ValidateEncodeRequest(const CoreConfig& config, std::string_view text) {
    const TransportValidationIssue config_issue = ValidateCommonConfig(config);
    if (config_issue != TransportValidationIssue::kOk) {
        return config_issue;
    }

    if (config.mode == TransportMode::kFlash) {
        return TransportValidationIssue::kOk;
    }

    if (config.mode == TransportMode::kPro && !IsAsciiText(text)) {
        return TransportValidationIssue::kProAsciiOnly;
    }

    std::vector<uint8_t> payload;
    ErrorCode payload_code = ErrorCode::kInvalidArgument;
    if (config.mode == TransportMode::kPro) {
        payload_code = bag::pro::EncodeTextToPayload(std::string(text), &payload);
    } else if (config.mode == TransportMode::kUltra) {
        payload_code = bag::ultra::EncodeTextToPayload(std::string(text), &payload);
    }
    if (payload_code != ErrorCode::kOk) {
        return TransportValidationIssue::kInvalidMode;
    }
    if (payload.size() > bag::transport::compat::kMaxFramePayloadBytes) {
        return TransportValidationIssue::kPayloadTooLarge;
    }
    return TransportValidationIssue::kOk;
}

TransportValidationIssue ValidateDecoderConfig(const CoreConfig& config) {
    return ValidateCommonConfig(config);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<int16_t>* out_pcm) {
    if (out_pcm == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    if (ValidateEncodeRequest(config, text) != TransportValidationIssue::kOk) {
        return ErrorCode::kInvalidArgument;
    }

    switch (config.mode) {
    case TransportMode::kFlash:
        return bag::flash::EncodeTextToPcm16(config, text, out_pcm);
    case TransportMode::kPro:
        return bag::pro::EncodeTextToPcm16Compat(config, text, out_pcm);
    case TransportMode::kUltra:
        return bag::ultra::EncodeTextToPcm16Compat(config, text, out_pcm);
    default:
        return ErrorCode::kInvalidArgument;
    }
}

std::unique_ptr<ITransportDecoder> CreateTransportDecoder(const CoreConfig& config) {
    if (ValidateDecoderConfig(config) != TransportValidationIssue::kOk) {
        return nullptr;
    }

    switch (config.mode) {
    case TransportMode::kFlash:
        return bag::flash::CreateDecoder(config);
    case TransportMode::kPro:
        return bag::pro::CreateCompatDecoder(config);
    case TransportMode::kUltra:
        return bag::ultra::CreateCompatDecoder(config);
    default:
        return nullptr;
    }
}

}  // namespace bag
