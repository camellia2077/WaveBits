#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include "bag/common/error_code.h"

namespace bag::pro {

ErrorCode EncodeTextToPayload(const std::string& text, std::vector<uint8_t>* out_payload);
ErrorCode DecodePayloadToText(const std::vector<uint8_t>& payload, std::string* out_text);

}  // namespace bag::pro
