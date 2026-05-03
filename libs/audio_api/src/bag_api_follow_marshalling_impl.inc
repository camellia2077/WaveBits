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
