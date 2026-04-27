bag_error_code bag_create_decoder(const bag_decoder_config* config,
                                  bag_decoder** out_decoder) {
  if (config == nullptr || out_decoder == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  if (bag_validate_decoder_config(config) != BAG_VALIDATION_OK) {
    return BAG_INVALID_ARGUMENT;
  }

  auto* decoder = new bag_decoder{};
  decoder->decoder = bag::CreateTransportDecoder(ToCoreConfig(
      config->sample_rate_hz, config->frame_samples, config->enable_diagnostics,
      config->mode, config->flash_signal_profile, config->flash_voicing_flavor,
      config->reserved));
  if (!decoder->decoder) {
    delete decoder;
    return BAG_INTERNAL;
  }

  *out_decoder = decoder;
  return BAG_OK;
}

void bag_destroy_decoder(bag_decoder* decoder) {
  delete decoder;
}

bag_error_code bag_push_pcm(bag_decoder* decoder, const int16_t* samples,
                            size_t sample_count, int64_t timestamp_ms) {
  if (decoder == nullptr || decoder->decoder == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  bag::PcmBlock block{};
  block.samples = samples;
  block.sample_count = sample_count;
  block.timestamp_ms = timestamp_ms;
  return ToApiCode(decoder->decoder->PushPcm(block));
}

bag_error_code bag_poll_decode_result(bag_decoder* decoder,
                                      bag_decode_result* out_result) {
  if (decoder == nullptr || decoder->decoder == nullptr ||
      out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  bag::DecodeResult result{};
  const bag_error_code code =
      ToApiCode(decoder->decoder->PollDecodeResult(&result));
  if (code != BAG_OK) {
    ClearApiDecodeResult(out_result);
    return code;
  }

  const std::string raw_bytes_hex = FormatRawBytesHex(result.raw_payload_bytes);
  const std::string raw_bits_binary =
      FormatRawBitsBinary(result.raw_payload_bytes);
  out_result->text_size = result.text.size();
  out_result->raw_bytes_hex_size = raw_bytes_hex.size();
  out_result->raw_bits_binary_size = raw_bits_binary.size();
  out_result->complete = result.complete ? 1 : 0;
  out_result->confidence = result.confidence;
  out_result->mode = ToApiMode(result.mode);
  out_result->text_decode_status = ToApiDecodeContentStatus(result.text_status);
  out_result->raw_payload_available = result.raw_payload_available ? 1 : 0;
  out_result->raw_bytes_hex_status =
      result.raw_payload_available
          ? CopyStringToApiBuffer(raw_bytes_hex, out_result->raw_bytes_hex_buffer,
                                  out_result->raw_bytes_hex_buffer_size)
          : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bits_binary_status =
      result.raw_payload_available
          ? CopyStringToApiBuffer(raw_bits_binary,
                                  out_result->raw_bits_binary_buffer,
                                  out_result->raw_bits_binary_buffer_size)
          : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  CopyFollowDataToApi(result.follow_data, &out_result->follow_data);
  CopyTextFollowDataToApi(result.text_follow_data, &out_result->text_follow_data);
  const bag_decode_content_status copied_text_status =
      CopyStringToApiBuffer(result.text, out_result->text_buffer,
                            out_result->text_buffer_size);
  if (out_result->text_decode_status == BAG_DECODE_CONTENT_STATUS_OK &&
      copied_text_status == BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL) {
    out_result->text_decode_status = copied_text_status;
  }
  return BAG_OK;
}

bag_error_code bag_poll_result(bag_decoder* decoder,
                               bag_text_result* out_result) {
  if (decoder == nullptr || decoder->decoder == nullptr ||
      out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  bag_decode_result decode_result{};
  decode_result.text_buffer = out_result->buffer;
  decode_result.text_buffer_size = out_result->buffer_size;
  const bag_error_code code = bag_poll_decode_result(decoder, &decode_result);
  if (code != BAG_OK) {
    if (out_result->buffer != nullptr && out_result->buffer_size > 0) {
      out_result->buffer[0] = '\0';
    }
    out_result->text_size = 0;
    out_result->complete = 0;
    out_result->confidence = 0.0f;
    out_result->mode = BAG_TRANSPORT_FLASH;
    return code;
  }

  if (decode_result.text_decode_status != BAG_DECODE_CONTENT_STATUS_OK &&
      decode_result.text_decode_status !=
          BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL) {
    if (out_result->buffer != nullptr && out_result->buffer_size > 0) {
      out_result->buffer[0] = '\0';
    }
    out_result->text_size = 0;
    out_result->complete = 0;
    out_result->confidence = 0.0f;
    out_result->mode = decode_result.mode;
    return BAG_INTERNAL;
  }

  out_result->text_size = decode_result.text_size;
  out_result->complete = decode_result.complete;
  out_result->confidence = decode_result.confidence;
  out_result->mode = decode_result.mode;
  return BAG_OK;
}

void bag_reset(bag_decoder* decoder) {
  if (decoder == nullptr || decoder->decoder == nullptr) {
    return;
  }
  decoder->decoder->Reset();
}
