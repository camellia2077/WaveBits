#include "bag/pro/codec.h"

#include <array>
#include <cctype>

namespace bag::pro {
namespace {

std::array<uint8_t, 3> EncodeAsciiByte(unsigned char value) {
    return {
        static_cast<uint8_t>('0' + (value / 100) % 10),
        static_cast<uint8_t>('0' + (value / 10) % 10),
        static_cast<uint8_t>('0' + value % 10),
    };
}

bool IsAsciiText(const std::string& text) {
    for (unsigned char ch : text) {
        if (ch > 0x7F) {
            return false;
        }
    }
    return true;
}

}  // namespace

ErrorCode EncodeTextToPayload(const std::string& text, std::vector<uint8_t>* out_payload) {
    if (out_payload == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    if (!IsAsciiText(text)) {
        return ErrorCode::kInvalidArgument;
    }

    out_payload->clear();
    out_payload->reserve(text.size() * 3);
    for (unsigned char ch : text) {
        const auto digits = EncodeAsciiByte(ch);
        out_payload->insert(out_payload->end(), digits.begin(), digits.end());
    }
    return ErrorCode::kOk;
}

ErrorCode DecodePayloadToText(const std::vector<uint8_t>& payload, std::string* out_text) {
    if (out_text == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    if (payload.size() % 3 != 0) {
        return ErrorCode::kInvalidArgument;
    }

    out_text->clear();
    out_text->reserve(payload.size() / 3);
    for (size_t index = 0; index < payload.size(); index += 3) {
        const uint8_t hundreds = payload[index];
        const uint8_t tens = payload[index + 1];
        const uint8_t ones = payload[index + 2];
        if (!std::isdigit(hundreds) || !std::isdigit(tens) || !std::isdigit(ones)) {
            return ErrorCode::kInvalidArgument;
        }

        const int value =
            (hundreds - static_cast<uint8_t>('0')) * 100 +
            (tens - static_cast<uint8_t>('0')) * 10 +
            (ones - static_cast<uint8_t>('0'));
        if (value < 0 || value > 0x7F) {
            return ErrorCode::kInvalidArgument;
        }
        out_text->push_back(static_cast<char>(value));
    }
    return ErrorCode::kOk;
}

}  // namespace bag::pro
