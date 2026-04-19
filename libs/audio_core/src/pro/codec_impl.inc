namespace bag::pro {

using std::size_t;
using std::uint8_t;

namespace {

bool IsAsciiText(const std::string& text) {
  for (unsigned char ch : text) {
    if (ch > 0x7F) {
      return false;
    }
  }
  return true;
}

bool IsAsciiPayload(const std::vector<uint8_t>& payload) {
  for (uint8_t value : payload) {
    if (value > 0x7F) {
      return false;
    }
  }
  return true;
}

}  // namespace

ErrorCode EncodeTextToPayload(const std::string& text,
                              std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (!IsAsciiText(text)) {
    return ErrorCode::kInvalidArgument;
  }

  out_payload->assign(text.begin(), text.end());
  return ErrorCode::kOk;
}

ErrorCode DecodePayloadToText(const std::vector<uint8_t>& payload,
                              std::string* out_text) {
  if (out_text == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (!IsAsciiPayload(payload)) {
    return ErrorCode::kInvalidArgument;
  }

  out_text->assign(payload.begin(), payload.end());
  return ErrorCode::kOk;
}

ErrorCode EncodePayloadToSymbols(const std::vector<uint8_t>& payload,
                                 std::vector<uint8_t>* out_symbols) {
  if (out_symbols == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (!IsAsciiPayload(payload)) {
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

ErrorCode DecodeSymbolsToPayload(const std::vector<uint8_t>& symbols,
                                 std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (symbols.size() % kSymbolsPerPayloadByte != 0) {
    return ErrorCode::kInvalidArgument;
  }

  out_payload->clear();
  out_payload->reserve(symbols.size() / kSymbolsPerPayloadByte);
  for (size_t index = 0; index < symbols.size();
       index += kSymbolsPerPayloadByte) {
    const uint8_t high_nibble = symbols[index];
    const uint8_t low_nibble = symbols[index + 1];
    if (high_nibble > 0x0F || low_nibble > 0x0F) {
      return ErrorCode::kInvalidArgument;
    }

    const uint8_t value = static_cast<uint8_t>((high_nibble << 4) | low_nibble);
    if (value > 0x7F) {
      return ErrorCode::kInvalidArgument;
    }
    out_payload->push_back(value);
  }
  return ErrorCode::kOk;
}

}  // namespace bag::pro
