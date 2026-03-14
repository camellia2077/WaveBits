module;

export module bag.ultra.codec;

import std;

export import bag.common.error_code;

export namespace bag::ultra {

inline constexpr std::size_t kSymbolsPerPayloadByte = 2;

ErrorCode EncodeTextToPayload(const std::string& text, std::vector<std::uint8_t>* out_payload);
ErrorCode DecodePayloadToText(const std::vector<std::uint8_t>& payload, std::string* out_text);
ErrorCode EncodePayloadToSymbols(const std::vector<std::uint8_t>& payload,
                                 std::vector<std::uint8_t>* out_symbols);
ErrorCode DecodeSymbolsToPayload(const std::vector<std::uint8_t>& symbols,
                                 std::vector<std::uint8_t>* out_payload);

}  // namespace bag::ultra
