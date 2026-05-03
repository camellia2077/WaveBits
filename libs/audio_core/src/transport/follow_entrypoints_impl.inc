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
    case TransportMode::kMini:
      return BuildMorsePayloadFollowData(config, raw_payload_bytes);
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
    text_follow_data.available =
        text.empty() && payload_follow_data.raw_payload_bytes.empty();
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
    const auto& last_byte =
        byte_timeline[byte_cursor + token.covered_byte_count -
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
