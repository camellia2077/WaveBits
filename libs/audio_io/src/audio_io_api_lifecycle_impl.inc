void ResetOwnedString(audio_io_owned_string* value) {
  if (value == nullptr) {
    return;
  }
  value->data = nullptr;
  value->size = 0;
}

void ResetMetadata(audio_io_metadata* metadata) {
  if (metadata == nullptr) {
    return;
  }
  metadata->version = 0;
  metadata->mode = AUDIO_IO_METADATA_MODE_UNKNOWN;
  metadata->has_flash_voicing_style = 0;
  metadata->flash_voicing_style = AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
  ResetOwnedString(&metadata->created_at_iso_utc);
  metadata->duration_ms = 0;
  metadata->sample_rate_hz = 0;
  metadata->frame_samples = 0;
  metadata->pcm_sample_count = 0;
  metadata->payload_byte_count = 0;
  metadata->input_source_kind = AUDIO_IO_METADATA_INPUT_SOURCE_KIND_UNKNOWN;
  metadata->segment_count = 1;
  metadata->segment_sample_counts = nullptr;
  metadata->segment_sample_count_count = 0;
  ResetOwnedString(&metadata->app_version);
  ResetOwnedString(&metadata->core_version);
}

void ResetDecodedWav(audio_io_decoded_wav* decoded) {
  if (decoded == nullptr) {
    return;
  }
  decoded->sample_rate_hz = 0;
  decoded->channels = 1;
  decoded->samples = nullptr;
  decoded->sample_count = 0;
  decoded->metadata_status = AUDIO_IO_METADATA_NOT_FOUND;
  ResetMetadata(&decoded->metadata);
}

void ResetWavInfo(audio_io_wav_info* info) {
  if (info == nullptr) {
    return;
  }
  info->sample_rate_hz = 0;
  info->channels = 0;
  info->bits_per_sample = 0;
  info->pcm_sample_count = 0;
  info->data_byte_count = 0;
  info->file_byte_count = 0;
  info->duration_ms = 0;
}

bool DuplicateOwnedString(const std::string& input, audio_io_owned_string* out) {
  if (out == nullptr) {
    return false;
  }
  ResetOwnedString(out);
  if (input.empty()) {
    return true;
  }

  auto* buffer = new (std::nothrow) char[input.size() + 1u];
  if (buffer == nullptr) {
    return false;
  }
  std::memcpy(buffer, input.data(), input.size());
  buffer[input.size()] = '\0';
  out->data = buffer;
  out->size = input.size();
  return true;
}

bool DuplicateSegmentSampleCounts(
    const std::vector<std::uint32_t>& input,
    audio_io_metadata* out_metadata) {
  if (out_metadata == nullptr) {
    return false;
  }
  out_metadata->segment_sample_counts = nullptr;
  out_metadata->segment_sample_count_count = 0;
  if (input.empty()) {
    return true;
  }

  auto* buffer = new (std::nothrow) std::uint32_t[input.size()];
  if (buffer == nullptr) {
    return false;
  }
  std::copy(input.begin(), input.end(), buffer);
  out_metadata->segment_sample_counts = buffer;
  out_metadata->segment_sample_count_count = input.size();
  return true;
}
