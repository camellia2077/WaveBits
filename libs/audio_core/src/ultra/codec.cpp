#include "bag/ultra/codec.h"

namespace bag::ultra {

ErrorCode EncodeTextToPayload(const std::string& text, std::vector<uint8_t>* out_payload) {
    if (out_payload == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    out_payload->assign(text.begin(), text.end());
    return ErrorCode::kOk;
}

ErrorCode DecodePayloadToText(const std::vector<uint8_t>& payload, std::string* out_text) {
    if (out_text == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    out_text->assign(payload.begin(), payload.end());
    return ErrorCode::kOk;
}

}  // namespace bag::ultra
