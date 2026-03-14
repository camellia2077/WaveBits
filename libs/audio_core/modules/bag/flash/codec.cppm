module;

export module bag.flash.codec;

import std;

export import bag.common.error_code;

export namespace bag::flash {

ErrorCode EncodeTextToBytes(const std::string& text, std::vector<std::uint8_t>* out_bytes);
ErrorCode DecodeBytesToText(const std::vector<std::uint8_t>& bytes, std::string* out_text);

}  // namespace bag::flash
