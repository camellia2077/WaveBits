struct Utf8CodePointSlice {
  std::string_view bytes;
  std::uint32_t code_point = 0;
};

struct TextTokenSpec {
  std::string display_text;
  std::size_t covered_byte_count = 0;
  std::size_t display_unit_count = 0;
  std::uint32_t tail_code_point = 0;
  std::string joiner_after;
  bool force_line_break_after = false;
};

bool IsCjkCodePoint(std::uint32_t code_point) {
  return (code_point >= 0x3400 && code_point <= 0x4DBF) ||
         (code_point >= 0x4E00 && code_point <= 0x9FFF) ||
         (code_point >= 0xF900 && code_point <= 0xFAFF) ||
         (code_point >= 0x3040 && code_point <= 0x30FF) ||
         (code_point >= 0x31F0 && code_point <= 0x31FF) ||
         (code_point >= 0xAC00 && code_point <= 0xD7AF) ||
         (code_point >= 0x1100 && code_point <= 0x11FF);
}

bool IsWhitespaceCodePoint(std::uint32_t code_point) {
  switch (code_point) {
    case 0x09:
    case 0x0A:
    case 0x0D:
    case 0x20:
    case 0x3000:
      return true;
    default:
      return false;
  }
}

bool IsAsciiPunctuation(std::uint32_t code_point) {
  return (code_point >= 0x21 && code_point <= 0x2F) ||
         (code_point >= 0x3A && code_point <= 0x40) ||
         (code_point >= 0x5B && code_point <= 0x60) ||
         (code_point >= 0x7B && code_point <= 0x7E);
}

bool IsCjkPunctuation(std::uint32_t code_point) {
  return (code_point >= 0x3001 && code_point <= 0x303F) ||
         (code_point >= 0xFF01 && code_point <= 0xFF0F) ||
         (code_point >= 0xFF1A && code_point <= 0xFF20) ||
         (code_point >= 0xFF3B && code_point <= 0xFF40) ||
         (code_point >= 0xFF5B && code_point <= 0xFF65);
}

bool IsUnicodePunctuationBlock(std::uint32_t code_point) {
  return (code_point >= 0x00A1 && code_point <= 0x00BF) ||
         (code_point >= 0x2000 && code_point <= 0x206F) ||
         (code_point >= 0x2E00 && code_point <= 0x2E7F);
}

bool IsPunctuationCodePoint(std::uint32_t code_point) {
  return IsAsciiPunctuation(code_point) || IsCjkPunctuation(code_point) ||
         IsUnicodePunctuationBlock(code_point);
}

bool IsStrongLineBreakCodePoint(std::uint32_t code_point) {
  switch (code_point) {
    case '.':
    case '!':
    case '?':
    case ';':
    case ':':
    case 0x3002:
    case 0xFF01:
    case 0xFF1F:
    case 0xFF1B:
    case 0xFF1A:
      return true;
    default:
      return false;
  }
}

bool IsWeakLineBreakCodePoint(std::uint32_t code_point) {
  switch (code_point) {
    case ',':
    case 0x3001:
    case 0xFF0C:
      return true;
    default:
      return false;
  }
}

std::vector<Utf8CodePointSlice> DecodeUtf8CodePointSlices(
    std::string_view text) {
  std::vector<Utf8CodePointSlice> slices;
  std::size_t index = 0;
  while (index < text.size()) {
    const unsigned char lead = static_cast<unsigned char>(text[index]);
    std::size_t length = 1;
    std::uint32_t code_point = lead;
    if ((lead & 0xE0U) == 0xC0U && index + 1 < text.size()) {
      length = 2;
      code_point = (static_cast<std::uint32_t>(lead & 0x1FU) << 6) |
                   static_cast<std::uint32_t>(
                       static_cast<unsigned char>(text[index + 1]) & 0x3FU);
    } else if ((lead & 0xF0U) == 0xE0U && index + 2 < text.size()) {
      length = 3;
      code_point = (static_cast<std::uint32_t>(lead & 0x0FU) << 12) |
                   (static_cast<std::uint32_t>(
                        static_cast<unsigned char>(text[index + 1]) & 0x3FU)
                    << 6) |
                   static_cast<std::uint32_t>(
                       static_cast<unsigned char>(text[index + 2]) & 0x3FU);
    } else if ((lead & 0xF8U) == 0xF0U && index + 3 < text.size()) {
      length = 4;
      code_point = (static_cast<std::uint32_t>(lead & 0x07U) << 18) |
                   (static_cast<std::uint32_t>(
                        static_cast<unsigned char>(text[index + 1]) & 0x3FU)
                    << 12) |
                   (static_cast<std::uint32_t>(
                        static_cast<unsigned char>(text[index + 2]) & 0x3FU)
                    << 6) |
                   static_cast<std::uint32_t>(
                       static_cast<unsigned char>(text[index + 3]) & 0x3FU);
    }
    slices.push_back(Utf8CodePointSlice{
        .bytes = text.substr(index, length),
        .code_point = code_point,
    });
    index += length;
  }
  return slices;
}

void AppendStandaloneSliceToken(const Utf8CodePointSlice& slice,
                                std::vector<TextTokenSpec>* out_tokens) {
  if (out_tokens == nullptr) {
    return;
  }
  out_tokens->push_back(TextTokenSpec{
      .display_text = std::string(slice.bytes),
      .covered_byte_count = slice.bytes.size(),
      .display_unit_count = 1,
      .tail_code_point = slice.code_point,
  });
}

void FlushGroupedTextToken(std::string* grouped_text,
                           std::size_t* grouped_bytes,
                           std::size_t* grouped_display_units,
                           std::uint32_t* grouped_tail_code_point,
                           std::vector<TextTokenSpec>* out_tokens) {
  if (grouped_text == nullptr || grouped_bytes == nullptr ||
      grouped_display_units == nullptr || grouped_tail_code_point == nullptr ||
      out_tokens == nullptr || grouped_text->empty()) {
    return;
  }
  out_tokens->push_back(TextTokenSpec{
      .display_text = *grouped_text,
      .covered_byte_count = *grouped_bytes,
      .display_unit_count = *grouped_display_units,
      .tail_code_point = *grouped_tail_code_point,
  });
  grouped_text->clear();
  *grouped_bytes = 0;
  *grouped_display_units = 0;
  *grouped_tail_code_point = 0;
}

std::vector<TextTokenSpec> TokenizeSegmentForFollow(
    const Utf8CodePointSlice* slices, std::size_t slice_count,
    bool split_non_punctuation_per_code_point) {
  std::vector<TextTokenSpec> tokens;
  std::string grouped_text;
  std::size_t grouped_bytes = 0;
  std::size_t grouped_display_units = 0;
  std::uint32_t grouped_tail_code_point = 0;
  for (std::size_t slice_index = 0; slice_index < slice_count; ++slice_index) {
    const auto& slice = slices[slice_index];
    if (IsPunctuationCodePoint(slice.code_point)) {
      FlushGroupedTextToken(&grouped_text, &grouped_bytes,
                            &grouped_display_units,
                            &grouped_tail_code_point, &tokens);
      AppendStandaloneSliceToken(slice, &tokens);
      continue;
    }
    if (split_non_punctuation_per_code_point) {
      AppendStandaloneSliceToken(slice, &tokens);
    } else {
      grouped_text.append(slice.bytes);
      grouped_bytes += slice.bytes.size();
      ++grouped_display_units;
      grouped_tail_code_point = slice.code_point;
    }
  }
  FlushGroupedTextToken(&grouped_text, &grouped_bytes, &grouped_display_units,
                        &grouped_tail_code_point, &tokens);
  return tokens;
}

std::vector<TextTokenSpec> TokenizeTextForFollow(std::string_view text) {
  if (text.empty()) {
    return {};
  }

  const auto slices = DecodeUtf8CodePointSlices(text);
  std::vector<TextTokenSpec> tokens;
  std::size_t segment_begin = 0;
  std::size_t pending_whitespace_bytes = 0;
  std::string pending_separator;
  bool pending_contains_newline = false;
  const auto flush_segment = [&](std::size_t segment_end) {
    if (segment_begin >= segment_end) {
      return;
    }
    bool contains_cjk = false;
    for (std::size_t index = segment_begin; index < segment_end; ++index) {
      contains_cjk = contains_cjk || IsCjkCodePoint(slices[index].code_point);
    }
    std::vector<TextTokenSpec> segment_tokens;
    segment_tokens = TokenizeSegmentForFollow(
        slices.data() + segment_begin, segment_end - segment_begin,
        contains_cjk);
    if (pending_whitespace_bytes > 0 && !tokens.empty()) {
      tokens.back().covered_byte_count += pending_whitespace_bytes;
      tokens.back().joiner_after =
          pending_contains_newline ? "" : pending_separator;
      tokens.back().force_line_break_after = pending_contains_newline;
      pending_whitespace_bytes = 0;
      pending_separator.clear();
      pending_contains_newline = false;
    }
    tokens.insert(tokens.end(), segment_tokens.begin(), segment_tokens.end());
  };

  for (std::size_t index = 0; index < slices.size(); ++index) {
    if (!IsWhitespaceCodePoint(slices[index].code_point)) {
      continue;
    }
    flush_segment(index);
    segment_begin = index + 1;
    pending_whitespace_bytes += slices[index].bytes.size();
    pending_separator.append(slices[index].bytes);
    pending_contains_newline =
        pending_contains_newline || slices[index].code_point == 0x0A ||
        slices[index].code_point == 0x0D;
  }
  flush_segment(slices.size());

  if (pending_whitespace_bytes > 0 && !tokens.empty()) {
    tokens.back().covered_byte_count += pending_whitespace_bytes;
    tokens.back().joiner_after =
        pending_contains_newline ? "" : pending_separator;
    tokens.back().force_line_break_after = pending_contains_newline;
  }

  if (tokens.empty()) {
    tokens.push_back(TextTokenSpec{
        .display_text = std::string(text),
        .covered_byte_count = text.size(),
        .display_unit_count = slices.size(),
        .tail_code_point = slices.back().code_point,
    });
  }
  return tokens;
}
