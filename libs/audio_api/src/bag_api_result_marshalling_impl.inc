bag_error_code CopyPcmToApiResult(const std::vector<std::int16_t>& pcm,
                                  bag_pcm16_result* out_result) {
  if (out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  out_result->samples = nullptr;
  out_result->sample_count = 0;
  if (pcm.empty()) {
    return BAG_OK;
  }

  auto* samples = new (std::nothrow) std::int16_t[pcm.size()];
  if (samples == nullptr) {
    return BAG_INTERNAL;
  }

  std::copy_n(pcm.data(), pcm.size(), samples);
  out_result->samples = samples;
  out_result->sample_count = pcm.size();
  return BAG_OK;
}

void ClearApiFollowData(bag_payload_follow_data* out_follow) {
  if (out_follow == nullptr) {
    return;
  }

  out_follow->byte_timeline_count = 0;
  out_follow->byte_timeline_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_follow->binary_group_timeline_count = 0;
  out_follow->binary_group_timeline_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_follow->payload_begin_sample = 0;
  out_follow->payload_sample_count = 0;
  out_follow->total_pcm_sample_count = 0;
  out_follow->available = 0;
}

void ClearApiTextFollowData(bag_text_follow_data* out_text_follow) {
  if (out_text_follow == nullptr) {
    return;
  }

  if (out_text_follow->text_tokens_buffer != nullptr &&
      out_text_follow->text_tokens_buffer_size > 0) {
    out_text_follow->text_tokens_buffer[0] = '\0';
  }
  out_text_follow->text_tokens_size = 0;
  out_text_follow->text_tokens_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->text_token_timeline_count = 0;
  out_text_follow->text_token_timeline_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->token_raw_segments_count = 0;
  out_text_follow->token_raw_segments_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->token_raw_display_units_count = 0;
  out_text_follow->token_raw_display_units_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  if (out_text_follow->lyric_lines_buffer != nullptr &&
      out_text_follow->lyric_lines_buffer_size > 0) {
    out_text_follow->lyric_lines_buffer[0] = '\0';
  }
  out_text_follow->lyric_lines_size = 0;
  out_text_follow->lyric_lines_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->lyric_line_timeline_count = 0;
  out_text_follow->lyric_line_timeline_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->line_token_ranges_count = 0;
  out_text_follow->line_token_ranges_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->line_raw_segments_count = 0;
  out_text_follow->line_raw_segments_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->available = 0;
}

void ClearApiEncodeResult(bag_encode_result* out_result) {
  if (out_result == nullptr) {
    return;
  }

  out_result->samples = nullptr;
  out_result->sample_count = 0;
  if (out_result->raw_bytes_hex_buffer != nullptr &&
      out_result->raw_bytes_hex_buffer_size > 0) {
    out_result->raw_bytes_hex_buffer[0] = '\0';
  }
  if (out_result->raw_bits_binary_buffer != nullptr &&
      out_result->raw_bits_binary_buffer_size > 0) {
    out_result->raw_bits_binary_buffer[0] = '\0';
  }
  out_result->raw_bytes_hex_size = 0;
  out_result->raw_bits_binary_size = 0;
  out_result->raw_bytes_hex_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bits_binary_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_payload_available = 0;
  ClearApiFollowData(&out_result->follow_data);
  ClearApiTextFollowData(&out_result->text_follow_data);
}

void ClearApiDecodeResult(bag_decode_result* out_result) {
  if (out_result == nullptr) {
    return;
  }

  if (out_result->text_buffer != nullptr && out_result->text_buffer_size > 0) {
    out_result->text_buffer[0] = '\0';
  }
  if (out_result->raw_bytes_hex_buffer != nullptr &&
      out_result->raw_bytes_hex_buffer_size > 0) {
    out_result->raw_bytes_hex_buffer[0] = '\0';
  }
  if (out_result->raw_bits_binary_buffer != nullptr &&
      out_result->raw_bits_binary_buffer_size > 0) {
    out_result->raw_bits_binary_buffer[0] = '\0';
  }
  out_result->text_size = 0;
  out_result->raw_bytes_hex_size = 0;
  out_result->raw_bits_binary_size = 0;
  out_result->complete = 0;
  out_result->confidence = 0.0f;
  out_result->mode = BAG_TRANSPORT_FLASH;
  out_result->text_decode_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bytes_hex_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bits_binary_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_payload_available = 0;
  ClearApiFollowData(&out_result->follow_data);
  ClearApiTextFollowData(&out_result->text_follow_data);
}

template <typename SourceEntry, typename ApiEntry, typename CopyFn>
bag_decode_content_status CopyTimelineEntries(const std::vector<SourceEntry>& input,
                                              ApiEntry* buffer,
                                              std::size_t buffer_count,
                                              CopyFn&& copy_entry) {
  if (buffer == nullptr) {
    return input.empty() ? BAG_DECODE_CONTENT_STATUS_OK
                         : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  }

  const std::size_t copy_count = std::min(buffer_count, input.size());
  for (std::size_t index = 0; index < copy_count; ++index) {
    copy_entry(input[index], &buffer[index]);
  }
  return copy_count == input.size() ? BAG_DECODE_CONTENT_STATUS_OK
                                    : BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
}

void CopyFollowDataToApi(const bag::PayloadFollowData& follow_data,
                         bag_payload_follow_data* out_follow) {
  if (out_follow == nullptr) {
    return;
  }

  out_follow->payload_begin_sample = follow_data.payload_begin_sample;
  out_follow->payload_sample_count = follow_data.payload_sample_count;
  out_follow->total_pcm_sample_count = follow_data.total_pcm_sample_count;
  out_follow->available = follow_data.available ? 1 : 0;
  out_follow->byte_timeline_count = follow_data.byte_timeline.size();
  out_follow->binary_group_timeline_count =
      follow_data.binary_group_timeline.size();
  if (!follow_data.available) {
    out_follow->byte_timeline_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_follow->binary_group_timeline_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    return;
  }

  out_follow->byte_timeline_status = CopyTimelineEntries(
      follow_data.byte_timeline, out_follow->byte_timeline_buffer,
      out_follow->byte_timeline_buffer_count,
      [](const bag::PayloadFollowByteEntry& source,
         bag_payload_follow_byte_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->byte_index = source.byte_index;
      });
  out_follow->binary_group_timeline_status = CopyTimelineEntries(
      follow_data.binary_group_timeline,
      out_follow->binary_group_timeline_buffer,
      out_follow->binary_group_timeline_buffer_count,
      [](const bag::PayloadFollowBinaryGroupEntry& source,
         bag_payload_follow_binary_group_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->group_index = source.group_index;
        destination->bit_offset = source.bit_offset;
        destination->bit_count = source.bit_count;
      });
}

void CopyTextFollowDataToApi(const bag::TextFollowData& text_follow_data,
                             bag_text_follow_data* out_text_follow) {
  if (out_text_follow == nullptr) {
    return;
  }

  out_text_follow->available = text_follow_data.available ? 1 : 0;
  out_text_follow->text_token_timeline_count =
      text_follow_data.text_token_timeline.size();
  out_text_follow->token_raw_segments_count =
      text_follow_data.token_raw_segments.size();
  out_text_follow->token_raw_display_units_count =
      text_follow_data.token_raw_display_units.size();
  out_text_follow->lyric_line_timeline_count =
      text_follow_data.lyric_line_timeline.size();
  out_text_follow->line_token_ranges_count =
      text_follow_data.line_token_ranges.size();
  out_text_follow->line_raw_segments_count =
      text_follow_data.line_raw_segments.size();
  out_text_follow->text_tokens_size = 0;
  out_text_follow->lyric_lines_size = 0;
  if (!text_follow_data.available) {
    out_text_follow->text_tokens_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->text_token_timeline_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->token_raw_segments_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->token_raw_display_units_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->lyric_lines_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->lyric_line_timeline_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->line_token_ranges_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->line_raw_segments_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    return;
  }

  std::vector<std::size_t> token_offsets(text_follow_data.text_tokens.size(), 0);
  std::vector<std::size_t> token_sizes(text_follow_data.text_tokens.size(), 0);
  std::size_t joined_size = 0;
  for (std::size_t index = 0; index < text_follow_data.text_tokens.size();
       ++index) {
    token_offsets[index] = joined_size;
    token_sizes[index] = text_follow_data.text_tokens[index].size();
    joined_size += token_sizes[index];
    if (index + 1 < text_follow_data.text_tokens.size()) {
      ++joined_size;
    }
  }
  out_text_follow->text_tokens_size = joined_size;
  out_text_follow->text_tokens_status = CopyJoinedTextTokensToApi(
      text_follow_data.text_tokens, out_text_follow->text_tokens_buffer,
      out_text_follow->text_tokens_buffer_size);
  out_text_follow->lyric_lines_size =
      JoinedTextTokensSize(text_follow_data.lyric_lines);
  out_text_follow->lyric_lines_status = CopyJoinedTextTokensToApi(
      text_follow_data.lyric_lines, out_text_follow->lyric_lines_buffer,
      out_text_follow->lyric_lines_buffer_size);
  out_text_follow->text_token_timeline_status = CopyTimelineEntries(
      text_follow_data.text_token_timeline,
      out_text_follow->text_token_timeline_buffer,
      out_text_follow->text_token_timeline_buffer_count,
      [&](const bag::TextFollowTokenTimelineEntry& source,
          bag_text_follow_token_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->token_index = source.token_index;
        destination->text_offset =
            source.token_index < token_offsets.size()
                ? token_offsets[source.token_index]
                : static_cast<std::size_t>(0);
        destination->text_size =
            source.token_index < token_sizes.size()
                ? token_sizes[source.token_index]
                : static_cast<std::size_t>(0);
      });
  out_text_follow->token_raw_segments_status = CopyTimelineEntries(
      text_follow_data.token_raw_segments,
      out_text_follow->token_raw_segments_buffer,
      out_text_follow->token_raw_segments_buffer_count,
      [](const bag::TextFollowRawSegmentEntry& source,
         bag_text_follow_raw_segment_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->token_index = source.token_index;
        destination->byte_offset = source.byte_offset;
        destination->byte_count = source.byte_count;
      });
  out_text_follow->token_raw_display_units_status = CopyTimelineEntries(
      text_follow_data.token_raw_display_units,
      out_text_follow->token_raw_display_units_buffer,
      out_text_follow->token_raw_display_units_buffer_count,
      [](const bag::TextFollowRawDisplayUnitEntry& source,
         bag_text_follow_raw_display_unit_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->token_index = source.token_index;
        destination->byte_index_within_token = source.byte_index_within_token;
        destination->byte_offset = source.byte_offset;
        destination->byte_count = source.byte_count;
      });
  out_text_follow->lyric_line_timeline_status = CopyTimelineEntries(
      text_follow_data.lyric_line_timeline,
      out_text_follow->lyric_line_timeline_buffer,
      out_text_follow->lyric_line_timeline_buffer_count,
      [](const bag::TextFollowLyricLineTimelineEntry& source,
         bag_text_follow_lyric_line_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->line_index = source.line_index;
      });
  out_text_follow->line_token_ranges_status = CopyTimelineEntries(
      text_follow_data.line_token_ranges,
      out_text_follow->line_token_ranges_buffer,
      out_text_follow->line_token_ranges_buffer_count,
      [](const bag::TextFollowLineTokenRangeEntry& source,
         bag_text_follow_line_token_range_entry* destination) {
        destination->line_index = source.line_index;
        destination->token_begin_index = source.token_begin_index;
        destination->token_count = source.token_count;
      });
  out_text_follow->line_raw_segments_status = CopyTimelineEntries(
      text_follow_data.line_raw_segments,
      out_text_follow->line_raw_segments_buffer,
      out_text_follow->line_raw_segments_buffer_count,
      [](const bag::TextFollowLineRawSegmentEntry& source,
         bag_text_follow_line_raw_segment_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->line_index = source.line_index;
        destination->byte_offset = source.byte_offset;
        destination->byte_count = source.byte_count;
      });
}

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

void FillEncodeResultLayout(const bag::EncodedPcmFollowResult& result,
                            bag_encode_result_layout* out_layout) {
  if (out_layout == nullptr) {
    return;
  }

  out_layout->sample_count = result.pcm.size();
  out_layout->raw_bytes_hex_size =
      FormattedRawBytesHexSize(result.follow_data.raw_payload_bytes);
  out_layout->raw_bits_binary_size =
      FormattedRawBitsBinarySize(result.follow_data.raw_payload_bytes);
  out_layout->raw_payload_available = result.follow_data.available ? 1 : 0;
  out_layout->byte_timeline_count = result.follow_data.byte_timeline.size();
  out_layout->binary_group_timeline_count =
      result.follow_data.binary_group_timeline.size();
  out_layout->text_tokens_size =
      JoinedTextTokensSize(result.text_follow_data.text_tokens);
  out_layout->text_token_timeline_count =
      result.text_follow_data.text_token_timeline.size();
  out_layout->token_raw_segments_count =
      result.text_follow_data.token_raw_segments.size();
  out_layout->token_raw_display_units_count =
      result.text_follow_data.token_raw_display_units.size();
  out_layout->lyric_lines_size =
      JoinedTextTokensSize(result.text_follow_data.lyric_lines);
  out_layout->lyric_line_timeline_count =
      result.text_follow_data.lyric_line_timeline.size();
  out_layout->line_token_ranges_count =
      result.text_follow_data.line_token_ranges.size();
  out_layout->line_raw_segments_count =
      result.text_follow_data.line_raw_segments.size();
  out_layout->follow_available = result.follow_data.available ? 1 : 0;
  out_layout->text_follow_available = result.text_follow_data.available ? 1 : 0;
}

bag_error_code CopyStructuredEncodeResultToApi(
    const bag::EncodedPcmFollowResult& result, bag_encode_result* out_result) {
  if (out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  ClearApiEncodeResult(out_result);
  bag_pcm16_result pcm_result{};
  const bag_error_code pcm_code = CopyPcmToApiResult(result.pcm, &pcm_result);
  if (pcm_code != BAG_OK) {
    return pcm_code;
  }
  out_result->samples = pcm_result.samples;
  out_result->sample_count = pcm_result.sample_count;

  out_result->raw_bytes_hex_size =
      FormattedRawBytesHexSize(result.follow_data.raw_payload_bytes);
  out_result->raw_bits_binary_size =
      FormattedRawBitsBinarySize(result.follow_data.raw_payload_bytes);
  out_result->raw_payload_available = result.follow_data.available ? 1 : 0;
  out_result->raw_bytes_hex_status =
      result.follow_data.available
          ? CopyRawBytesHexToApi(result.follow_data.raw_payload_bytes,
                                 out_result->raw_bytes_hex_buffer,
                                 out_result->raw_bytes_hex_buffer_size)
          : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bits_binary_status =
      result.follow_data.available
          ? CopyRawBitsBinaryToApi(result.follow_data.raw_payload_bytes,
                                   out_result->raw_bits_binary_buffer,
                                   out_result->raw_bits_binary_buffer_size)
          : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  CopyFollowDataToApi(result.follow_data, &out_result->follow_data);
  CopyTextFollowDataToApi(result.text_follow_data, &out_result->text_follow_data);
  return BAG_OK;
}

bag_error_code BuildEncodeFollowDataResult(const bag::CoreConfig& core_config,
                                           const std::string& text,
                                           bag::EncodedPcmFollowResult* result) {
  if (result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    bag::PayloadFollowData follow_data{};
    bag::TextFollowData text_follow_data{};
    const bag::ErrorCode follow_code = bag::BuildEncodeFollowData(
        core_config, text, &follow_data, &text_follow_data);
    if (follow_code != bag::ErrorCode::kOk) {
      return ToApiCode(follow_code);
    }

    result->follow_data = std::move(follow_data);
    result->text_follow_data = std::move(text_follow_data);
    return BAG_OK;
  } catch (const std::invalid_argument&) {
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    return BAG_INTERNAL;
  }
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
