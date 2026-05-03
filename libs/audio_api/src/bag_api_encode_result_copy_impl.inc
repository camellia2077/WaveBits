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
