std::string FormatRawBytesHex(const std::vector<std::uint8_t>& bytes) {
  if (bytes.empty()) {
    return {};
  }

  static constexpr char kHexDigits[] = "0123456789ABCDEF";
  std::string out;
  out.reserve(bytes.size() * 3 - 1);
  for (std::size_t index = 0; index < bytes.size(); ++index) {
    const std::uint8_t value = bytes[index];
    if (index > 0) {
      out.push_back(' ');
    }
    out.push_back(kHexDigits[(value >> 4) & 0x0F]);
    out.push_back(kHexDigits[value & 0x0F]);
  }
  return out;
}

std::size_t FormattedRawBytesHexSize(const std::vector<std::uint8_t>& bytes) {
  return bytes.empty() ? static_cast<std::size_t>(0)
                       : bytes.size() * static_cast<std::size_t>(3) -
                             static_cast<std::size_t>(1);
}

std::string FormatRawBitsBinary(const std::vector<std::uint8_t>& bytes) {
  if (bytes.empty()) {
    return {};
  }

  std::string out;
  out.reserve(bytes.size() * 9 - 1);
  for (std::size_t index = 0; index < bytes.size(); ++index) {
    const std::uint8_t value = bytes[index];
    if (index > 0) {
      out.push_back(' ');
    }
    for (int shift = 7; shift >= 0; --shift) {
      out.push_back(((value >> shift) & 0x01U) != 0U ? '1' : '0');
    }
  }
  return out;
}

std::size_t FormattedRawBitsBinarySize(const std::vector<std::uint8_t>& bytes) {
  return bytes.empty() ? static_cast<std::size_t>(0)
                       : bytes.size() * static_cast<std::size_t>(9) -
                             static_cast<std::size_t>(1);
}

std::string JoinTextTokens(const std::vector<std::string>& tokens) {
  if (tokens.empty()) {
    return {};
  }

  std::size_t total_size = 0;
  for (const auto& token : tokens) {
    total_size += token.size();
  }
  total_size += tokens.size() - 1;

  std::string out;
  out.reserve(total_size);
  for (std::size_t index = 0; index < tokens.size(); ++index) {
    if (index > 0) {
      out.push_back('\n');
    }
    out.append(tokens[index]);
  }
  return out;
}

std::size_t JoinedTextTokensSize(const std::vector<std::string>& tokens) {
  if (tokens.empty()) {
    return 0;
  }

  std::size_t total_size = 0;
  for (const auto& token : tokens) {
    total_size += token.size();
  }
  return total_size + tokens.size() - static_cast<std::size_t>(1);
}

bag_decode_content_status CopyStringToApiBuffer(std::string_view input,
                                                char* buffer,
                                                std::size_t buffer_size) {
  if (buffer == nullptr) {
    return BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  }
  if (buffer_size == 0) {
    return input.empty() ? BAG_DECODE_CONTENT_STATUS_OK
                         : BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
  }

  const std::size_t copy_size =
      std::min(input.size(), buffer_size - static_cast<std::size_t>(1));
  if (copy_size > 0) {
    std::copy_n(input.data(), copy_size, buffer);
  }
  buffer[copy_size] = '\0';
  return copy_size == input.size() ? BAG_DECODE_CONTENT_STATUS_OK
                                   : BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
}

bag_decode_content_status CopyRawBytesHexToApi(
    const std::vector<std::uint8_t>& bytes, char* buffer,
    std::size_t buffer_size) {
  if (bytes.empty()) {
    return CopyStringToApiBuffer({}, buffer, buffer_size);
  }
  if (buffer == nullptr) {
    return BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  }
  if (buffer_size == 0) {
    return BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
  }
  return CopyStringToApiBuffer(FormatRawBytesHex(bytes), buffer, buffer_size);
}

bag_decode_content_status CopyRawBitsBinaryToApi(
    const std::vector<std::uint8_t>& bytes, char* buffer,
    std::size_t buffer_size) {
  if (bytes.empty()) {
    return CopyStringToApiBuffer({}, buffer, buffer_size);
  }
  if (buffer == nullptr) {
    return BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  }
  if (buffer_size == 0) {
    return BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
  }
  return CopyStringToApiBuffer(FormatRawBitsBinary(bytes), buffer, buffer_size);
}

bag_decode_content_status CopyJoinedTextTokensToApi(
    const std::vector<std::string>& tokens, char* buffer,
    std::size_t buffer_size) {
  if (tokens.empty()) {
    return CopyStringToApiBuffer({}, buffer, buffer_size);
  }
  if (buffer == nullptr) {
    return BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  }
  if (buffer_size == 0) {
    return BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
  }
  return CopyStringToApiBuffer(JoinTextTokens(tokens), buffer, buffer_size);
}
