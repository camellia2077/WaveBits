audio_io::FlipBitsAudioMetadataMode ToLibraryMode(audio_io_metadata_mode mode) {
  switch (mode) {
    case AUDIO_IO_METADATA_MODE_MINI:
      return audio_io::FlipBitsAudioMetadataMode::kMini;
    case AUDIO_IO_METADATA_MODE_FLASH:
      return audio_io::FlipBitsAudioMetadataMode::kFlash;
    case AUDIO_IO_METADATA_MODE_PRO:
      return audio_io::FlipBitsAudioMetadataMode::kPro;
    case AUDIO_IO_METADATA_MODE_ULTRA:
      return audio_io::FlipBitsAudioMetadataMode::kUltra;
    case AUDIO_IO_METADATA_MODE_UNKNOWN:
    default:
      return audio_io::FlipBitsAudioMetadataMode::kUnknown;
  }
}

audio_io_metadata_mode ToApiMode(audio_io::FlipBitsAudioMetadataMode mode) {
  switch (mode) {
    case audio_io::FlipBitsAudioMetadataMode::kMini:
      return AUDIO_IO_METADATA_MODE_MINI;
    case audio_io::FlipBitsAudioMetadataMode::kFlash:
      return AUDIO_IO_METADATA_MODE_FLASH;
    case audio_io::FlipBitsAudioMetadataMode::kPro:
      return AUDIO_IO_METADATA_MODE_PRO;
    case audio_io::FlipBitsAudioMetadataMode::kUltra:
      return AUDIO_IO_METADATA_MODE_ULTRA;
    case audio_io::FlipBitsAudioMetadataMode::kUnknown:
    default:
      return AUDIO_IO_METADATA_MODE_UNKNOWN;
  }
}

audio_io::FlipBitsAudioMetadataFlashVoicingStyle ToLibraryFlashStyle(
    audio_io_metadata_flash_voicing_style style) {
  switch (style) {
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STEADY:
      return audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kSteady;
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY:
      return audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kLitany;
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILE:
      return audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kHostile;
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE:
      return audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kCollapse;
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL:
      return audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kZeal;
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID:
      return audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kVoid;
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN:
    default:
      return audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kUnknown;
  }
}

audio_io_metadata_flash_voicing_style ToApiFlashStyle(
    audio_io::FlipBitsAudioMetadataFlashVoicingStyle style) {
  switch (style) {
    case audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kSteady:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STEADY;
    case audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kLitany:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY;
    case audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kHostile:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILE;
    case audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kCollapse:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE;
    case audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kZeal:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL;
    case audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kVoid:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID;
    case audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kUnknown:
    default:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
  }
}

audio_io::FlipBitsAudioMetadataInputSourceKind ToLibraryInputSourceKind(
    audio_io_metadata_input_source_kind kind) {
  switch (kind) {
    case AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL:
      return audio_io::FlipBitsAudioMetadataInputSourceKind::kManual;
    case AUDIO_IO_METADATA_INPUT_SOURCE_KIND_SAMPLE:
      return audio_io::FlipBitsAudioMetadataInputSourceKind::kSample;
    case AUDIO_IO_METADATA_INPUT_SOURCE_KIND_UNKNOWN:
    default:
      return audio_io::FlipBitsAudioMetadataInputSourceKind::kUnknown;
  }
}

audio_io_metadata_input_source_kind ToApiInputSourceKind(
    audio_io::FlipBitsAudioMetadataInputSourceKind kind) {
  switch (kind) {
    case audio_io::FlipBitsAudioMetadataInputSourceKind::kManual:
      return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL;
    case audio_io::FlipBitsAudioMetadataInputSourceKind::kSample:
      return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_SAMPLE;
    case audio_io::FlipBitsAudioMetadataInputSourceKind::kUnknown:
    default:
      return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_UNKNOWN;
  }
}

bool IsValidStringView(audio_io_string_view value) {
  return value.size == 0 || value.data != nullptr;
}

std::string ToStdString(audio_io_string_view value) {
  if (value.size == 0) {
    return {};
  }
  return std::string(value.data, value.size);
}

bool FillApiMetadata(const audio_io::FlipBitsAudioMetadata& native_metadata,
                     audio_io_metadata* out_metadata) {
  if (out_metadata == nullptr) {
    return false;
  }

  ResetMetadata(out_metadata);
  out_metadata->version = native_metadata.version;
  out_metadata->mode = ToApiMode(native_metadata.mode);
  out_metadata->has_flash_voicing_style =
      native_metadata.has_flash_voicing_style ? 1u : 0u;
  out_metadata->flash_voicing_style =
      native_metadata.has_flash_voicing_style
          ? ToApiFlashStyle(native_metadata.flash_voicing_style)
          : AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
  out_metadata->duration_ms = native_metadata.duration_ms;
  out_metadata->sample_rate_hz = native_metadata.sample_rate_hz;
  out_metadata->frame_samples = native_metadata.frame_samples;
  out_metadata->pcm_sample_count = native_metadata.pcm_sample_count;
  out_metadata->payload_byte_count = native_metadata.payload_byte_count;
  out_metadata->input_source_kind =
      ToApiInputSourceKind(native_metadata.input_source_kind);
  out_metadata->segment_count = native_metadata.segment_count;
  if (!DuplicateSegmentSampleCounts(native_metadata.segment_sample_counts,
                                    out_metadata)) {
    audio_io_free_metadata(out_metadata);
    return false;
  }

  if (!DuplicateOwnedString(native_metadata.created_at_iso_utc,
                            &out_metadata->created_at_iso_utc) ||
      !DuplicateOwnedString(native_metadata.app_version,
                            &out_metadata->app_version) ||
      !DuplicateOwnedString(native_metadata.core_version,
                            &out_metadata->core_version)) {
    audio_io_free_metadata(out_metadata);
    return false;
  }

  return true;
}

bool ToNativeMetadata(const audio_io_metadata_view* metadata,
                      audio_io::FlipBitsAudioMetadata* out_metadata) {
  if (metadata == nullptr || out_metadata == nullptr) {
    return false;
  }
  if (!IsValidStringView(metadata->created_at_iso_utc) ||
      !IsValidStringView(metadata->app_version) ||
      !IsValidStringView(metadata->core_version)) {
    return false;
  }
  if (metadata->segment_sample_count_count > 0 &&
      metadata->segment_sample_counts == nullptr) {
    return false;
  }

  out_metadata->version = metadata->version;
  out_metadata->mode = ToLibraryMode(metadata->mode);
  out_metadata->has_flash_voicing_style =
      metadata->has_flash_voicing_style != 0u;
  out_metadata->flash_voicing_style =
      out_metadata->has_flash_voicing_style
          ? ToLibraryFlashStyle(metadata->flash_voicing_style)
          : audio_io::FlipBitsAudioMetadataFlashVoicingStyle::kUnknown;
  out_metadata->created_at_iso_utc = ToStdString(metadata->created_at_iso_utc);
  out_metadata->duration_ms = metadata->duration_ms;
  out_metadata->sample_rate_hz = metadata->sample_rate_hz;
  out_metadata->frame_samples = metadata->frame_samples;
  out_metadata->pcm_sample_count = metadata->pcm_sample_count;
  out_metadata->payload_byte_count = metadata->payload_byte_count;
  out_metadata->input_source_kind =
      ToLibraryInputSourceKind(metadata->input_source_kind);
  out_metadata->segment_count = std::max<std::uint32_t>(1u, metadata->segment_count);
  out_metadata->segment_sample_counts.assign(
      metadata->segment_sample_counts,
      metadata->segment_sample_counts + metadata->segment_sample_count_count);
  out_metadata->app_version = ToStdString(metadata->app_version);
  out_metadata->core_version = ToStdString(metadata->core_version);
  return true;
}
