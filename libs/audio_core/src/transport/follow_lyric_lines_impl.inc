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

void AppendLyricLineRange(
    const std::vector<TextTokenSpec>& token_specs,
    const std::vector<TextFollowTokenTimelineEntry>& text_token_timeline,
    const std::vector<TextFollowRawSegmentEntry>& token_raw_segments,
    std::size_t line_begin_token_index, std::size_t line_end_token_index,
    TextFollowData* out_text_follow_data) {
  if (out_text_follow_data == nullptr ||
      line_begin_token_index >= line_end_token_index ||
      line_end_token_index > text_token_timeline.size() ||
      line_end_token_index > token_raw_segments.size()) {
    return;
  }

  const std::size_t line_index = out_text_follow_data->lyric_lines.size();
  const auto& first_token_timeline = text_token_timeline[line_begin_token_index];
  const auto& last_token_timeline =
      text_token_timeline[line_end_token_index - 1];
  const auto& first_raw_segment = token_raw_segments[line_begin_token_index];
  const auto& last_raw_segment = token_raw_segments[line_end_token_index - 1];
  const std::size_t line_start_sample = first_token_timeline.start_sample;
  const std::size_t line_end_sample =
      last_token_timeline.start_sample + last_token_timeline.sample_count;
  const std::size_t line_byte_offset = first_raw_segment.byte_offset;
  const std::size_t line_byte_end =
      last_raw_segment.byte_offset + last_raw_segment.byte_count;

  out_text_follow_data->lyric_lines.push_back(ComposeLyricLineText(
      token_specs, line_begin_token_index, line_end_token_index));
  out_text_follow_data->lyric_line_timeline.push_back(
      TextFollowLyricLineTimelineEntry{
          .start_sample = line_start_sample,
          .sample_count = line_end_sample - line_start_sample,
          .line_index = line_index,
      });
  out_text_follow_data->line_token_ranges.push_back(
      TextFollowLineTokenRangeEntry{
          .line_index = line_index,
          .token_begin_index = line_begin_token_index,
          .token_count = line_end_token_index - line_begin_token_index,
      });
  out_text_follow_data->line_raw_segments.push_back(
      TextFollowLineRawSegmentEntry{
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
