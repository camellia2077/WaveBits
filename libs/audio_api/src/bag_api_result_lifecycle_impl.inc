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

void ClearApiFlashSignalInfo(bag_flash_signal_info* out_info) {
  if (out_info == nullptr) {
    return;
  }

  if (out_info->low_carrier_hz_buffer != nullptr &&
      out_info->low_carrier_hz_buffer_size > 0) {
    out_info->low_carrier_hz_buffer[0] = '\0';
  }
  if (out_info->high_carrier_hz_buffer != nullptr &&
      out_info->high_carrier_hz_buffer_size > 0) {
    out_info->high_carrier_hz_buffer[0] = '\0';
  }
  if (out_info->bit_duration_samples_buffer != nullptr &&
      out_info->bit_duration_samples_buffer_size > 0) {
    out_info->bit_duration_samples_buffer[0] = '\0';
  }
  if (out_info->payload_silence_buffer != nullptr &&
      out_info->payload_silence_buffer_size > 0) {
    out_info->payload_silence_buffer[0] = '\0';
  }
  if (out_info->decode_path_buffer != nullptr &&
      out_info->decode_path_buffer_size > 0) {
    out_info->decode_path_buffer[0] = '\0';
  }
  out_info->low_carrier_hz_size = 0;
  out_info->high_carrier_hz_size = 0;
  out_info->bit_duration_samples_size = 0;
  out_info->payload_silence_size = 0;
  out_info->decode_path_size = 0;
  out_info->low_carrier_hz_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_info->high_carrier_hz_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_info->bit_duration_samples_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_info->payload_silence_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_info->decode_path_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_info->available = 0;
}
