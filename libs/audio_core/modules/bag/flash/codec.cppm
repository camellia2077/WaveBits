module;

#if !defined(WAVEBITS_CORE_IMPORT_STD)
#include <cstdint>
#include <string>
#include <vector>
#endif

export module bag.flash.codec;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#endif

export import bag.common.error_code;

export namespace bag::flash {

ErrorCode EncodeTextToBytes(const std::string& text, std::vector<std::uint8_t>* out_bytes);
ErrorCode DecodeBytesToText(const std::vector<std::uint8_t>& bytes, std::string* out_text);

}  // namespace bag::flash
