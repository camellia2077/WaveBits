#if !defined(WAVEBITS_MODULE_IMPL_WRAPPER)
#if __cplusplus >= 202002L
module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>
#endif

module bag.ultra.codec;
#endif
#endif

namespace bag::ultra {

using std::size_t;
using std::uint8_t;

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

ErrorCode EncodePayloadToSymbols(const std::vector<uint8_t>& payload, std::vector<uint8_t>* out_symbols) {
    if (out_symbols == nullptr) {
        return ErrorCode::kInvalidArgument;
    }

    out_symbols->clear();
    out_symbols->reserve(payload.size() * kSymbolsPerPayloadByte);
    for (uint8_t value : payload) {
        out_symbols->push_back(static_cast<uint8_t>((value >> 4) & 0x0F));
        out_symbols->push_back(static_cast<uint8_t>(value & 0x0F));
    }
    return ErrorCode::kOk;
}

ErrorCode DecodeSymbolsToPayload(const std::vector<uint8_t>& symbols, std::vector<uint8_t>* out_payload) {
    if (out_payload == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    if (symbols.size() % kSymbolsPerPayloadByte != 0) {
        return ErrorCode::kInvalidArgument;
    }

    out_payload->clear();
    out_payload->reserve(symbols.size() / kSymbolsPerPayloadByte);
    for (size_t index = 0; index < symbols.size(); index += kSymbolsPerPayloadByte) {
        const uint8_t high_nibble = symbols[index];
        const uint8_t low_nibble = symbols[index + 1];
        if (high_nibble > 0x0F || low_nibble > 0x0F) {
            return ErrorCode::kInvalidArgument;
        }
        out_payload->push_back(static_cast<uint8_t>((high_nibble << 4) | low_nibble));
    }
    return ErrorCode::kOk;
}

}  // namespace bag::ultra
