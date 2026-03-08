#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include "bag/common/error_code.h"

namespace bag::flash {

ErrorCode EncodeTextToBytes(const std::string& text, std::vector<uint8_t>* out_bytes);
ErrorCode DecodeBytesToText(const std::vector<uint8_t>& bytes, std::string* out_text);

}  // namespace bag::flash
