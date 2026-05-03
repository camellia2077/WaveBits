audio_io_wav_status ToApiStatus(audio_io::WavPcm16Status status) {
  switch (status) {
    case audio_io::WavPcm16Status::kOk:
      return AUDIO_IO_WAV_OK;
    case audio_io::WavPcm16Status::kInvalidArgument:
      return AUDIO_IO_WAV_INVALID_ARGUMENT;
    case audio_io::WavPcm16Status::kInvalidHeader:
      return AUDIO_IO_WAV_INVALID_HEADER;
    case audio_io::WavPcm16Status::kUnsupportedFormat:
      return AUDIO_IO_WAV_UNSUPPORTED_FORMAT;
    case audio_io::WavPcm16Status::kTruncatedData:
      return AUDIO_IO_WAV_TRUNCATED_DATA;
  }

  return AUDIO_IO_WAV_INTERNAL;
}

audio_io_metadata_status ToMetadataApiStatus(
    audio_io::FlipBitsAudioMetadataStatus status) {
  switch (status) {
    case audio_io::FlipBitsAudioMetadataStatus::kOk:
      return AUDIO_IO_METADATA_OK;
    case audio_io::FlipBitsAudioMetadataStatus::kNotFound:
      return AUDIO_IO_METADATA_NOT_FOUND;
    case audio_io::FlipBitsAudioMetadataStatus::kInvalidArgument:
      return AUDIO_IO_METADATA_INVALID_ARGUMENT;
    case audio_io::FlipBitsAudioMetadataStatus::kInvalidHeader:
      return AUDIO_IO_METADATA_INVALID_HEADER;
    case audio_io::FlipBitsAudioMetadataStatus::kUnsupportedVersion:
      return AUDIO_IO_METADATA_UNSUPPORTED_VERSION;
    case audio_io::FlipBitsAudioMetadataStatus::kInvalidMetadata:
      return AUDIO_IO_METADATA_INVALID_METADATA;
    case audio_io::FlipBitsAudioMetadataStatus::kTruncatedData:
      return AUDIO_IO_METADATA_TRUNCATED_DATA;
  }

  return AUDIO_IO_METADATA_INTERNAL;
}
