#include "bag/pro/text_codec.h"

#include "bag/pro/codec.h"
#include "bag/ultra/codec.h"

namespace bag::pro {

ErrorCode EncodeProTextToPayload(const std::string& text, std::vector<uint8_t>* out_payload) {
    return bag::pro::EncodeTextToPayload(text, out_payload);
}

ErrorCode DecodePayloadToProText(const std::vector<uint8_t>& payload, std::string* out_text) {
    return bag::pro::DecodePayloadToText(payload, out_text);
}

ErrorCode EncodeUltraTextToPayload(const std::string& text, std::vector<uint8_t>* out_payload) {
    return bag::ultra::EncodeTextToPayload(text, out_payload);
}

ErrorCode DecodePayloadToUltraText(const std::vector<uint8_t>& payload, std::string* out_text) {
    return bag::ultra::DecodePayloadToText(payload, out_text);
}

}  // namespace bag::pro
