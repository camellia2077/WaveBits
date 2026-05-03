void audio_io_free_byte_buffer(audio_io_byte_buffer* buffer) {
  if (buffer == nullptr) {
    return;
  }

  delete[] buffer->data;
  buffer->data = nullptr;
  buffer->size = 0;
}

void audio_io_free_metadata(audio_io_metadata* metadata) {
  if (metadata == nullptr) {
    return;
  }

  delete[] metadata->created_at_iso_utc.data;
  delete[] metadata->segment_sample_counts;
  delete[] metadata->app_version.data;
  delete[] metadata->core_version.data;
  ResetMetadata(metadata);
}

void audio_io_free_decoded_wav(audio_io_decoded_wav* decoded) {
  if (decoded == nullptr) {
    return;
  }

  delete[] decoded->samples;
  decoded->samples = nullptr;
  decoded->sample_count = 0;
  decoded->sample_rate_hz = 0;
  decoded->channels = 1;
  audio_io_free_metadata(&decoded->metadata);
  decoded->metadata_status = AUDIO_IO_METADATA_NOT_FOUND;
}
