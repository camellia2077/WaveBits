audio_io_wav_status audio_io_encode_mono_pcm16_wav(
    int sample_rate_hz,
    const int16_t* pcm,
    size_t sample_count,
    audio_io_byte_buffer* out_wav_bytes) {
  if (out_wav_bytes == nullptr) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  out_wav_bytes->data = nullptr;
  out_wav_bytes->size = 0;

  if (sample_rate_hz <= 0 || (sample_count > 0 && pcm == nullptr)) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  try {
    const std::vector<std::int16_t> pcm_samples =
        sample_count > 0
            ? std::vector<std::int16_t>(pcm, pcm + sample_count)
            : std::vector<std::int16_t>{};
    const auto wav_bytes =
        audio_io::SerializeMonoPcm16Wav(sample_rate_hz, pcm_samples);
    if (wav_bytes.empty()) {
      return AUDIO_IO_WAV_INVALID_ARGUMENT;
    }

    auto* buffer = new (std::nothrow) uint8_t[wav_bytes.size()];
    if (buffer == nullptr) {
      return AUDIO_IO_WAV_INTERNAL;
    }
    std::copy(wav_bytes.begin(), wav_bytes.end(), buffer);
    out_wav_bytes->data = buffer;
    out_wav_bytes->size = wav_bytes.size();
    return AUDIO_IO_WAV_OK;
  } catch (...) {
    return AUDIO_IO_WAV_INTERNAL;
  }
}

audio_io_wav_status audio_io_encode_mono_pcm16_wav_with_metadata(
    int sample_rate_hz,
    const int16_t* pcm,
    size_t sample_count,
    const audio_io_metadata_view* metadata,
    audio_io_byte_buffer* out_wav_bytes) {
  if (metadata == nullptr || out_wav_bytes == nullptr) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  out_wav_bytes->data = nullptr;
  out_wav_bytes->size = 0;

  if (sample_rate_hz <= 0 || (sample_count > 0 && pcm == nullptr)) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  try {
    audio_io::FlipBitsAudioMetadata native_metadata{};
    if (!ToNativeMetadata(metadata, &native_metadata)) {
      return AUDIO_IO_WAV_INVALID_ARGUMENT;
    }

    const std::vector<std::int16_t> pcm_samples =
        sample_count > 0
            ? std::vector<std::int16_t>(pcm, pcm + sample_count)
            : std::vector<std::int16_t>{};
    const auto wav_bytes = audio_io::SerializeMonoPcm16WavWithMetadata(
        sample_rate_hz, pcm_samples, native_metadata);
    if (wav_bytes.empty()) {
      return AUDIO_IO_WAV_INVALID_ARGUMENT;
    }

    auto* buffer = new (std::nothrow) uint8_t[wav_bytes.size()];
    if (buffer == nullptr) {
      return AUDIO_IO_WAV_INTERNAL;
    }
    std::copy(wav_bytes.begin(), wav_bytes.end(), buffer);
    out_wav_bytes->data = buffer;
    out_wav_bytes->size = wav_bytes.size();
    return AUDIO_IO_WAV_OK;
  } catch (...) {
    return AUDIO_IO_WAV_INTERNAL;
  }
}

audio_io_wav_status audio_io_decode_mono_pcm16_wav(
    const uint8_t* wav_bytes,
    size_t wav_byte_count,
    audio_io_decoded_wav* out_result) {
  if (out_result == nullptr) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  ResetDecodedWav(out_result);

  if (wav_byte_count == 0 || wav_bytes == nullptr) {
    out_result->metadata_status = AUDIO_IO_METADATA_INVALID_ARGUMENT;
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  try {
    const auto parsed_metadata =
        audio_io::ParseFlipBitsAudioMetadata(wav_bytes, wav_byte_count);
    out_result->metadata_status = ToMetadataApiStatus(parsed_metadata.status);
    if (parsed_metadata.status == audio_io::FlipBitsAudioMetadataStatus::kOk &&
        !FillApiMetadata(parsed_metadata.metadata, &out_result->metadata)) {
      out_result->metadata_status = AUDIO_IO_METADATA_INTERNAL;
    }

    const auto parsed_wav = audio_io::ParseMonoPcm16Wav(wav_bytes, wav_byte_count);
    if (parsed_wav.status != audio_io::WavPcm16Status::kOk) {
      return ToApiStatus(parsed_wav.status);
    }

    const auto& pcm_samples = parsed_wav.wav.mono_pcm;
    auto* buffer = new (std::nothrow) int16_t[pcm_samples.size()];
    if (buffer == nullptr && !pcm_samples.empty()) {
      audio_io_free_metadata(&out_result->metadata);
      out_result->metadata_status = AUDIO_IO_METADATA_INTERNAL;
      return AUDIO_IO_WAV_INTERNAL;
    }
    if (!pcm_samples.empty()) {
      std::copy(pcm_samples.begin(), pcm_samples.end(), buffer);
    }

    out_result->sample_rate_hz = parsed_wav.wav.sample_rate_hz;
    out_result->channels = parsed_wav.wav.channels;
    out_result->samples = buffer;
    out_result->sample_count = pcm_samples.size();
    return AUDIO_IO_WAV_OK;
  } catch (...) {
    audio_io_free_decoded_wav(out_result);
    return AUDIO_IO_WAV_INTERNAL;
  }
}

audio_io_wav_status audio_io_probe_mono_pcm16_wav(
    const uint8_t* wav_bytes,
    size_t wav_byte_count,
    audio_io_wav_info* out_info) {
  if (out_info == nullptr) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }
  ResetWavInfo(out_info);
  if (wav_byte_count == 0 || wav_bytes == nullptr) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  try {
    const auto parsed = audio_io::ProbeMonoPcm16Wav(wav_bytes, wav_byte_count);
    if (parsed.status != audio_io::WavPcm16Status::kOk) {
      return ToApiStatus(parsed.status);
    }
    out_info->sample_rate_hz = parsed.info.sample_rate_hz;
    out_info->channels = parsed.info.channels;
    out_info->bits_per_sample = parsed.info.bits_per_sample;
    out_info->pcm_sample_count = parsed.info.pcm_sample_count;
    out_info->data_byte_count = parsed.info.data_byte_count;
    out_info->file_byte_count = parsed.info.file_byte_count;
    out_info->duration_ms = parsed.info.duration_ms;
    return AUDIO_IO_WAV_OK;
  } catch (...) {
    ResetWavInfo(out_info);
    return AUDIO_IO_WAV_INTERNAL;
  }
}
