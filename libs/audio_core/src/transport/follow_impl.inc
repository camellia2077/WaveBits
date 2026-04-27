namespace bag {

namespace {

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

std::vector<Utf8CodePointSlice> DecodeUtf8CodePointSlices(std::string_view text);

std::vector<Utf8CodePointSlice> DecodeUtf8CodePointSlices(std::string_view text) {
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
      FlushGroupedTextToken(&grouped_text, &grouped_bytes, &grouped_display_units,
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
      tokens.back().joiner_after = pending_contains_newline ? "" : pending_separator;
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
    tokens.back().joiner_after = pending_contains_newline ? "" : pending_separator;
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

std::string ComposeLyricLineText(const std::vector<TextTokenSpec>& token_specs,
                                 std::size_t line_begin_token_index,
                                 std::size_t line_end_token_index) {
  std::string line_text;
  for (std::size_t token_index = line_begin_token_index;
       token_index < line_end_token_index; ++token_index) {
    if (token_index > line_begin_token_index) {
      line_text.append(token_specs[token_index - 1].joiner_after);
    }
    line_text.append(token_specs[token_index].display_text);
  }
  return line_text;
}

void AppendLyricLineRange(const std::vector<TextTokenSpec>& token_specs,
                          const std::vector<TextFollowTokenTimelineEntry>& text_token_timeline,
                          const std::vector<TextFollowRawSegmentEntry>& token_raw_segments,
                          std::size_t line_begin_token_index,
                          std::size_t line_end_token_index,
                          TextFollowData* out_text_follow_data) {
  if (out_text_follow_data == nullptr ||
      line_begin_token_index >= line_end_token_index ||
      line_end_token_index > text_token_timeline.size() ||
      line_end_token_index > token_raw_segments.size()) {
    return;
  }

  const std::size_t line_index = out_text_follow_data->lyric_lines.size();
  const auto& first_token_timeline = text_token_timeline[line_begin_token_index];
  const auto& last_token_timeline = text_token_timeline[line_end_token_index - 1];
  const auto& first_raw_segment = token_raw_segments[line_begin_token_index];
  const auto& last_raw_segment = token_raw_segments[line_end_token_index - 1];
  const std::size_t line_start_sample = first_token_timeline.start_sample;
  const std::size_t line_end_sample =
      last_token_timeline.start_sample + last_token_timeline.sample_count;
  const std::size_t line_byte_offset = first_raw_segment.byte_offset;
  const std::size_t line_byte_end =
      last_raw_segment.byte_offset + last_raw_segment.byte_count;

  out_text_follow_data->lyric_lines.push_back(
      ComposeLyricLineText(token_specs, line_begin_token_index, line_end_token_index));
  out_text_follow_data->lyric_line_timeline.push_back(
      TextFollowLyricLineTimelineEntry{
          .start_sample = line_start_sample,
          .sample_count = line_end_sample - line_start_sample,
          .line_index = line_index,
      });
  out_text_follow_data->line_token_ranges.push_back(TextFollowLineTokenRangeEntry{
      .line_index = line_index,
      .token_begin_index = line_begin_token_index,
      .token_count = line_end_token_index - line_begin_token_index,
  });
  out_text_follow_data->line_raw_segments.push_back(TextFollowLineRawSegmentEntry{
      .start_sample = line_start_sample,
      .sample_count = line_end_sample - line_start_sample,
      .line_index = line_index,
      .byte_offset = line_byte_offset,
      .byte_count = line_byte_end - line_byte_offset,
  });
}

void BuildLyricLineData(const std::vector<TextTokenSpec>& token_specs,
                        TextFollowData* out_text_follow_data) {
  if (out_text_follow_data == nullptr || token_specs.empty()) {
    return;
  }

  static constexpr std::size_t kPreferredLineUnitLimit = 18;
  static constexpr std::size_t kHardLineUnitLimit = 28;

  out_text_follow_data->lyric_lines.reserve(token_specs.size());
  out_text_follow_data->lyric_line_timeline.reserve(token_specs.size());
  out_text_follow_data->line_token_ranges.reserve(token_specs.size());
  out_text_follow_data->line_raw_segments.reserve(token_specs.size());

  std::size_t line_begin_token_index = 0;
  std::size_t current_line_units = 0;
  for (std::size_t token_index = 0; token_index < token_specs.size();
       ++token_index) {
    const auto& token_spec = token_specs[token_index];
    current_line_units += token_spec.display_unit_count;

    // Keep line grouping formal and deterministic: newline first, then strong
    // punctuation, then softer pauses, and only then fall back to the hard cap.
    const bool should_break_here =
        token_spec.force_line_break_after ||
        IsStrongLineBreakCodePoint(token_spec.tail_code_point) ||
        (current_line_units >= kPreferredLineUnitLimit &&
         IsWeakLineBreakCodePoint(token_spec.tail_code_point)) ||
        current_line_units >= kHardLineUnitLimit ||
        token_index + 1 == token_specs.size();
    if (!should_break_here) {
      continue;
    }

    AppendLyricLineRange(
        token_specs, out_text_follow_data->text_token_timeline,
        out_text_follow_data->token_raw_segments, line_begin_token_index,
        token_index + 1, out_text_follow_data);
    line_begin_token_index = token_index + 1;
    current_line_units = 0;
  }
}

PayloadFollowData BuildFlashPayloadFollowData(
    const CoreConfig& config,
    const std::vector<std::uint8_t>& raw_payload_bytes) {
  PayloadFollowData follow_data{};
  const flash::BfskConfig signal_config =
      flash::MakeBfskConfigForSignalProfile(config, config.flash_signal_profile);
  const flash::FlashPayloadLayout payload_layout =
      flash::BuildPayloadLayout(raw_payload_bytes, signal_config);
  const flash::FlashVoicingConfig voicing_config =
      flash::MakeFormalVoicingConfigForFlavor(config, config.flash_voicing_flavor);
  const std::size_t total_pcm_sample_count =
      payload_layout.payload_sample_count + voicing_config.preamble_sample_count +
      voicing_config.epilogue_sample_count;
  const flash::FlashVoicingDescriptor descriptor =
      flash::DescribeVoicingOutput(total_pcm_sample_count, voicing_config);

  follow_data.raw_payload_bytes = raw_payload_bytes;
  follow_data.byte_timeline.reserve(raw_payload_bytes.size());
  follow_data.binary_group_timeline.reserve(raw_payload_bytes.size() *
                                            static_cast<std::size_t>(8));
  follow_data.payload_begin_sample = descriptor.leading_nonpayload_samples;
  follow_data.payload_sample_count = descriptor.payload_sample_count;
  follow_data.total_pcm_sample_count = total_pcm_sample_count;
  follow_data.available = true;

  const std::size_t samples_per_bit = signal_config.samples_per_bit;
  for (std::size_t byte_index = 0; byte_index < raw_payload_bytes.size();
       ++byte_index) {
    follow_data.byte_timeline.push_back(PayloadFollowByteEntry{
        .start_sample = descriptor.leading_nonpayload_samples +
                        byte_index * static_cast<std::size_t>(8) *
                            samples_per_bit,
        .sample_count = static_cast<std::size_t>(8) * samples_per_bit,
        .byte_index = byte_index,
    });
  }

  const std::size_t bit_count = raw_payload_bytes.size() * static_cast<std::size_t>(8);
  for (std::size_t bit_index = 0; bit_index < bit_count; ++bit_index) {
    follow_data.binary_group_timeline.push_back(PayloadFollowBinaryGroupEntry{
        .start_sample =
            descriptor.leading_nonpayload_samples + bit_index * samples_per_bit,
        .sample_count = samples_per_bit,
        .group_index = bit_index,
        .bit_offset = bit_index,
        .bit_count = 1,
    });
  }

  return follow_data;
}

PayloadFollowData BuildNibblePayloadFollowData(
    const CoreConfig& config,
    const std::vector<std::uint8_t>& raw_payload_bytes) {
  PayloadFollowData follow_data{};
  const std::size_t symbol_sample_count =
      config.frame_samples > 0 ? static_cast<std::size_t>(config.frame_samples)
                               : static_cast<std::size_t>(0);
  const std::size_t symbol_count =
      raw_payload_bytes.size() * static_cast<std::size_t>(2);
  const std::size_t total_pcm_sample_count = symbol_count * symbol_sample_count;

  follow_data.raw_payload_bytes = raw_payload_bytes;
  follow_data.byte_timeline.reserve(raw_payload_bytes.size());
  follow_data.binary_group_timeline.reserve(symbol_count);
  follow_data.payload_begin_sample = 0;
  follow_data.payload_sample_count = total_pcm_sample_count;
  follow_data.total_pcm_sample_count = total_pcm_sample_count;
  follow_data.available = true;

  for (std::size_t byte_index = 0; byte_index < raw_payload_bytes.size();
       ++byte_index) {
    follow_data.byte_timeline.push_back(PayloadFollowByteEntry{
        .start_sample = byte_index * static_cast<std::size_t>(2) *
                        symbol_sample_count,
        .sample_count = static_cast<std::size_t>(2) * symbol_sample_count,
        .byte_index = byte_index,
    });
  }

  for (std::size_t group_index = 0; group_index < symbol_count; ++group_index) {
    follow_data.binary_group_timeline.push_back(PayloadFollowBinaryGroupEntry{
        .start_sample = group_index * symbol_sample_count,
        .sample_count = symbol_sample_count,
        .group_index = group_index,
        .bit_offset = group_index * static_cast<std::size_t>(4),
        .bit_count = 4,
    });
  }

  return follow_data;
}

}  // namespace

PayloadFollowData BuildPayloadFollowData(
    const CoreConfig& config,
    const std::vector<std::uint8_t>& raw_payload_bytes) {
  if (config.sample_rate_hz <= 0 || config.frame_samples <= 0 ||
      !IsValidTransportMode(config.mode) ||
      !IsValidFlashSignalProfile(config.flash_signal_profile) ||
      !IsValidFlashVoicingFlavor(config.flash_voicing_flavor)) {
    return {};
  }

  switch (config.mode) {
    case TransportMode::kFlash:
      return BuildFlashPayloadFollowData(config, raw_payload_bytes);
    case TransportMode::kPro:
    case TransportMode::kUltra:
      return BuildNibblePayloadFollowData(config, raw_payload_bytes);
    default:
      return {};
  }
}

TextFollowData BuildTextFollowData(const PayloadFollowData& payload_follow_data,
                                   std::string_view text) {
  TextFollowData text_follow_data{};
  if (!payload_follow_data.available) {
    return text_follow_data;
  }

  const std::vector<TextTokenSpec> token_specs = TokenizeTextForFollow(text);
  if (token_specs.empty()) {
    text_follow_data.available = text.empty() &&
                                 payload_follow_data.raw_payload_bytes.empty();
    return text_follow_data;
  }

  text_follow_data.text_tokens.reserve(token_specs.size());
  text_follow_data.text_token_timeline.reserve(token_specs.size());
  text_follow_data.token_raw_segments.reserve(token_specs.size());
  text_follow_data.token_raw_display_units.reserve(
      payload_follow_data.byte_timeline.size());
  std::size_t byte_cursor = 0;
  const auto& byte_timeline = payload_follow_data.byte_timeline;
  for (std::size_t token_index = 0; token_index < token_specs.size();
       ++token_index) {
    const TextTokenSpec& token = token_specs[token_index];
    if (token.covered_byte_count == 0 ||
        byte_cursor >= byte_timeline.size() ||
        byte_cursor + token.covered_byte_count > byte_timeline.size()) {
      return {};
    }
    const auto& first_byte = byte_timeline[byte_cursor];
    const auto& last_byte = byte_timeline[byte_cursor + token.covered_byte_count -
                                          static_cast<std::size_t>(1)];
    const std::size_t token_begin_sample = first_byte.start_sample;
    const std::size_t token_end_sample =
        last_byte.start_sample + last_byte.sample_count;
    text_follow_data.text_tokens.push_back(token.display_text);
    text_follow_data.text_token_timeline.push_back(TextFollowTokenTimelineEntry{
        .start_sample = token_begin_sample,
        .sample_count = token_end_sample - token_begin_sample,
        .token_index = token_index,
    });
    text_follow_data.token_raw_segments.push_back(TextFollowRawSegmentEntry{
        .start_sample = token_begin_sample,
        .sample_count = token_end_sample - token_begin_sample,
        .token_index = token_index,
        .byte_offset = byte_cursor,
        .byte_count = token.covered_byte_count,
    });
    // Publish byte-sized display units as a formal follow layer so UI code can
    // highlight human-readable Hex/Binary groups without rebuilding token-byte
    // timing from multiple lower-level timelines on its own.
    for (std::size_t token_byte_index = 0;
         token_byte_index < token.covered_byte_count; ++token_byte_index) {
      const auto& byte_entry = byte_timeline[byte_cursor + token_byte_index];
      text_follow_data.token_raw_display_units.push_back(
          TextFollowRawDisplayUnitEntry{
              .start_sample = byte_entry.start_sample,
              .sample_count = byte_entry.sample_count,
              .token_index = token_index,
              .byte_index_within_token = token_byte_index,
              .byte_offset = byte_entry.byte_index,
              .byte_count = 1,
          });
    }
    byte_cursor += token.covered_byte_count;
  }

  if (byte_cursor != byte_timeline.size()) {
    return {};
  }

  BuildLyricLineData(token_specs, &text_follow_data);
  text_follow_data.available = true;
  return text_follow_data;
}

}  // namespace bag
