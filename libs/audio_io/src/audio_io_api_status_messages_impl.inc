const char* audio_io_wav_status_message(audio_io_wav_status status) {
  switch (status) {
    case AUDIO_IO_WAV_OK:
      return "WAV operation completed successfully.";
    case AUDIO_IO_WAV_INVALID_ARGUMENT:
      return "Invalid WAV input argument.";
    case AUDIO_IO_WAV_INVALID_HEADER:
      return "Invalid WAV header.";
    case AUDIO_IO_WAV_UNSUPPORTED_FORMAT:
      return "Unsupported WAV format.";
    case AUDIO_IO_WAV_TRUNCATED_DATA:
      return "Truncated WAV data.";
    case AUDIO_IO_WAV_INTERNAL:
      return "Internal WAV I/O failure.";
  }

  return "Unknown WAV I/O status.";
}

const char* audio_io_metadata_status_message(audio_io_metadata_status status) {
  switch (status) {
    case AUDIO_IO_METADATA_OK:
      return "FlipBits metadata parsed successfully.";
    case AUDIO_IO_METADATA_NOT_FOUND:
      return "FlipBits metadata was not found in the WAV file.";
    case AUDIO_IO_METADATA_INVALID_ARGUMENT:
      return "Invalid FlipBits metadata parse argument.";
    case AUDIO_IO_METADATA_INVALID_HEADER:
      return "Invalid FlipBits metadata header.";
    case AUDIO_IO_METADATA_UNSUPPORTED_VERSION:
      return "Unsupported FlipBits metadata version.";
    case AUDIO_IO_METADATA_INVALID_METADATA:
      return "Invalid FlipBits metadata payload.";
    case AUDIO_IO_METADATA_TRUNCATED_DATA:
      return "Truncated FlipBits metadata payload.";
    case AUDIO_IO_METADATA_INTERNAL:
      return "Internal FlipBits metadata failure.";
  }

  return "Unknown FlipBits metadata status.";
}
