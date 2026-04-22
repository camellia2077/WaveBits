namespace audio_io::detail::bytes_impl {
namespace {

constexpr std::size_t kRiffHeaderSize = 12;
constexpr std::size_t kChunkHeaderSize = 8;
constexpr std::size_t kFmtChunkSize = 16;
constexpr std::size_t kCanonicalWavHeaderSize = 44;
constexpr std::size_t kFlipBitsMetadataIsoUtcByteCount = 20;
constexpr std::size_t kFlipBitsMetadataVersionTextByteCount = 32;
constexpr std::size_t kFlipBitsMetadataVersionOffset = 0;
constexpr std::size_t kFlipBitsMetadataModeOffset = 1;
constexpr std::size_t kFlipBitsMetadataHasFlashStyleOffset = 2;
constexpr std::size_t kFlipBitsMetadataFlashStyleOffset = 3;
constexpr std::size_t kFlipBitsMetadataDurationOffset = 4;
constexpr std::size_t kFlipBitsMetadataCreatedAtOffset = 8;
constexpr std::size_t kFlipBitsMetadataFrameSamplesOffset =
    kFlipBitsMetadataCreatedAtOffset + kFlipBitsMetadataIsoUtcByteCount;
constexpr std::size_t kFlipBitsMetadataPcmSampleCountOffset =
    kFlipBitsMetadataFrameSamplesOffset + 4;
constexpr std::size_t kFlipBitsMetadataAppVersionOffset =
    kFlipBitsMetadataPcmSampleCountOffset + 4;
constexpr std::size_t kFlipBitsMetadataCoreVersionOffset =
    kFlipBitsMetadataAppVersionOffset + kFlipBitsMetadataVersionTextByteCount;
constexpr std::size_t kFlipBitsMetadataV3ChunkSize =
    4 + 4 + kFlipBitsMetadataIsoUtcByteCount + 4 + 4 +
    kFlipBitsMetadataVersionTextByteCount +
    kFlipBitsMetadataVersionTextByteCount;
constexpr std::uint16_t kPcmFormatCode = 1;
constexpr std::uint16_t kMonoChannelCount = 1;
constexpr std::uint16_t kBitsPerSample = 16;
constexpr std::size_t kBytesPerSample = 2;
constexpr std::uint32_t kMaxChunkByteCount = 0xFFFFFFFFu;
constexpr std::uint8_t kFlipBitsMetadataVersionV3 = 3;

bool IsSupportedMode(FlipBitsAudioMetadataMode mode) {
  return mode == FlipBitsAudioMetadataMode::kFlash ||
         mode == FlipBitsAudioMetadataMode::kPro ||
         mode == FlipBitsAudioMetadataMode::kUltra;
}

bool IsSupportedFlashVoicingStyle(
    FlipBitsAudioMetadataFlashVoicingStyle style) {
  return style == FlipBitsAudioMetadataFlashVoicingStyle::kCodedBurst ||
         style == FlipBitsAudioMetadataFlashVoicingStyle::kRitualChant ||
         style == FlipBitsAudioMetadataFlashVoicingStyle::kDeepRitual;
}

bool IsIsoUtcTimestampChar(char value, std::size_t index) {
  switch (index) {
    case 4:
    case 7:
      return value == '-';
    case 10:
      return value == 'T';
    case 13:
    case 16:
      return value == ':';
    case 19:
      return value == 'Z';
    default:
      return value >= '0' && value <= '9';
  }
}

bool IsSupportedMetadataVersion(std::uint8_t version) {
  return version == kFlipBitsMetadataVersionV3;
}

bool IsValidCreatedAtIsoUtc(const std::string& created_at_iso_utc) {
  if (created_at_iso_utc.size() != kFlipBitsMetadataIsoUtcByteCount) {
    return false;
  }
  for (std::size_t index = 0; index < created_at_iso_utc.size(); ++index) {
    if (!IsIsoUtcTimestampChar(created_at_iso_utc[index], index)) {
      return false;
    }
  }
  return true;
}

bool IsValidVersionText(const std::string& version_text) {
  if (version_text.empty() ||
      version_text.size() > kFlipBitsMetadataVersionTextByteCount) {
    return false;
  }
  for (char value : version_text) {
    if (value < 0x20 || value > 0x7Eu) {
      return false;
    }
  }
  return true;
}

std::string ReadPaddedAsciiString(const std::uint8_t* data,
                                  std::size_t byte_count) {
  std::size_t length = 0;
  while (length < byte_count && data[length] != 0u) {
    ++length;
  }
  return std::string(reinterpret_cast<const char*>(data), length);
}

bool HasRange(std::size_t total_size, std::size_t offset,
              std::size_t byte_count) {
  return offset <= total_size && byte_count <= (total_size - offset);
}

std::uint16_t ReadU16Le(const std::uint8_t* data) {
  return static_cast<std::uint16_t>(static_cast<std::uint16_t>(data[0]) |
                                    (static_cast<std::uint16_t>(data[1]) << 8));
}

std::uint32_t ReadU32Le(const std::uint8_t* data) {
  return static_cast<std::uint32_t>(
      static_cast<std::uint32_t>(data[0]) |
      (static_cast<std::uint32_t>(data[1]) << 8) |
      (static_cast<std::uint32_t>(data[2]) << 16) |
      (static_cast<std::uint32_t>(data[3]) << 24));
}

void WriteU16Le(std::uint8_t* data, std::uint16_t value) {
  data[0] = static_cast<std::uint8_t>(value & 0xFFu);
  data[1] = static_cast<std::uint8_t>((value >> 8) & 0xFFu);
}

void WriteU32Le(std::uint8_t* data, std::uint32_t value) {
  data[0] = static_cast<std::uint8_t>(value & 0xFFu);
  data[1] = static_cast<std::uint8_t>((value >> 8) & 0xFFu);
  data[2] = static_cast<std::uint8_t>((value >> 16) & 0xFFu);
  data[3] = static_cast<std::uint8_t>((value >> 24) & 0xFFu);
}

bool MatchesTag(const std::uint8_t* data, const char* expected) {
  return data[0] == static_cast<std::uint8_t>(expected[0]) &&
         data[1] == static_cast<std::uint8_t>(expected[1]) &&
         data[2] == static_cast<std::uint8_t>(expected[2]) &&
         data[3] == static_cast<std::uint8_t>(expected[3]);
}

std::vector<std::uint8_t> BuildFlipBitsMetadataChunk(
    const FlipBitsAudioMetadata& metadata) {
  if (!IsSupportedMetadataVersion(metadata.version) ||
      !IsSupportedMode(metadata.mode)) {
    return {};
  }
  if (metadata.has_flash_voicing_style) {
    if (metadata.mode != FlipBitsAudioMetadataMode::kFlash ||
        !IsSupportedFlashVoicingStyle(metadata.flash_voicing_style)) {
      return {};
    }
  }
  if (!IsValidCreatedAtIsoUtc(metadata.created_at_iso_utc)) {
    return {};
  }
  if (!IsValidVersionText(metadata.app_version) ||
      !IsValidVersionText(metadata.core_version)) {
    return {};
  }

  const std::size_t payload_size = kFlipBitsMetadataV3ChunkSize;
  std::vector<std::uint8_t> chunk(kChunkHeaderSize + payload_size, 0);
  chunk[0] = 'W';
  chunk[1] = 'B';
  chunk[2] = 'A';
  chunk[3] = 'G';
  WriteU32Le(chunk.data() + 4, static_cast<std::uint32_t>(payload_size));
  chunk[kChunkHeaderSize + kFlipBitsMetadataVersionOffset] = metadata.version;
  chunk[kChunkHeaderSize + kFlipBitsMetadataModeOffset] =
      static_cast<std::uint8_t>(metadata.mode);
  chunk[kChunkHeaderSize + kFlipBitsMetadataHasFlashStyleOffset] =
      metadata.has_flash_voicing_style ? 1u : 0u;
  chunk[kChunkHeaderSize + kFlipBitsMetadataFlashStyleOffset] =
      metadata.has_flash_voicing_style
                  ? static_cast<std::uint8_t>(metadata.flash_voicing_style)
                  : static_cast<std::uint8_t>(0u);
  WriteU32Le(chunk.data() + kChunkHeaderSize + kFlipBitsMetadataDurationOffset,
             metadata.duration_ms);
  for (std::size_t index = 0; index < kFlipBitsMetadataIsoUtcByteCount;
       ++index) {
    chunk[kChunkHeaderSize + kFlipBitsMetadataCreatedAtOffset + index] =
        static_cast<std::uint8_t>(metadata.created_at_iso_utc[index]);
  }
  WriteU32Le(
      chunk.data() + kChunkHeaderSize + kFlipBitsMetadataFrameSamplesOffset,
      metadata.frame_samples);
  WriteU32Le(
      chunk.data() + kChunkHeaderSize + kFlipBitsMetadataPcmSampleCountOffset,
      metadata.pcm_sample_count);
  for (std::size_t index = 0; index < metadata.app_version.size(); ++index) {
    chunk[kChunkHeaderSize + kFlipBitsMetadataAppVersionOffset + index] =
        static_cast<std::uint8_t>(metadata.app_version[index]);
  }
  for (std::size_t index = 0; index < metadata.core_version.size(); ++index) {
    chunk[kChunkHeaderSize + kFlipBitsMetadataCoreVersionOffset + index] =
        static_cast<std::uint8_t>(metadata.core_version[index]);
  }
  return chunk;
}

FlipBitsAudioMetadataParseResult ParseFlipBitsMetadataChunk(
    const std::uint8_t* chunk_data, std::size_t chunk_size) {
  if (chunk_data == nullptr && chunk_size > 0) {
    return {FlipBitsAudioMetadataStatus::kInvalidArgument, {}};
  }
  if (chunk_size < kFlipBitsMetadataV3ChunkSize) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }

  FlipBitsAudioMetadata metadata{};
  metadata.version = chunk_data[kFlipBitsMetadataVersionOffset];
  metadata.mode =
      static_cast<FlipBitsAudioMetadataMode>(chunk_data[kFlipBitsMetadataModeOffset]);
  metadata.has_flash_voicing_style =
      chunk_data[kFlipBitsMetadataHasFlashStyleOffset] != 0u;
  metadata.flash_voicing_style =
      static_cast<FlipBitsAudioMetadataFlashVoicingStyle>(
          chunk_data[kFlipBitsMetadataFlashStyleOffset]);

  if (metadata.version != kFlipBitsMetadataVersionV3) {
    return {FlipBitsAudioMetadataStatus::kUnsupportedVersion, {}};
  }
  metadata.duration_ms = ReadU32Le(chunk_data + kFlipBitsMetadataDurationOffset);
  metadata.created_at_iso_utc.assign(
      reinterpret_cast<const char*>(chunk_data + kFlipBitsMetadataCreatedAtOffset),
      kFlipBitsMetadataIsoUtcByteCount);
  if (!IsValidCreatedAtIsoUtc(metadata.created_at_iso_utc)) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }
  metadata.frame_samples =
      ReadU32Le(chunk_data + kFlipBitsMetadataFrameSamplesOffset);
  metadata.pcm_sample_count =
      ReadU32Le(chunk_data + kFlipBitsMetadataPcmSampleCountOffset);
  metadata.app_version = ReadPaddedAsciiString(
      chunk_data + kFlipBitsMetadataAppVersionOffset,
      kFlipBitsMetadataVersionTextByteCount);
  metadata.core_version = ReadPaddedAsciiString(
      chunk_data + kFlipBitsMetadataCoreVersionOffset,
      kFlipBitsMetadataVersionTextByteCount);
  if (!IsValidVersionText(metadata.app_version) ||
      !IsValidVersionText(metadata.core_version)) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }
  if (!IsSupportedMode(metadata.mode)) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }
  if (metadata.has_flash_voicing_style) {
    if (metadata.mode != FlipBitsAudioMetadataMode::kFlash ||
        !IsSupportedFlashVoicingStyle(metadata.flash_voicing_style)) {
      return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
    }
  } else {
    metadata.flash_voicing_style =
        FlipBitsAudioMetadataFlashVoicingStyle::kUnknown;
  }

  return {FlipBitsAudioMetadataStatus::kOk, metadata};
}

}  // namespace

std::vector<std::uint8_t> SerializeMonoPcm16WavBytesWithMetadata(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm,
    const FlipBitsAudioMetadata* metadata);

std::vector<std::uint8_t> SerializeMonoPcm16WavBytes(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm) {
  return SerializeMonoPcm16WavBytesWithMetadata(sample_rate_hz, pcm, nullptr);
}

std::vector<std::uint8_t> SerializeMonoPcm16WavBytesWithMetadata(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm,
    const FlipBitsAudioMetadata* metadata) {
  if (sample_rate_hz <= 0) {
    return {};
  }

  const auto metadata_chunk = metadata != nullptr
                                  ? BuildFlipBitsMetadataChunk(*metadata)
                                  : std::vector<std::uint8_t>{};
  if (metadata != nullptr && metadata_chunk.empty()) {
    return {};
  }
  const std::size_t data_byte_count = pcm.size() * kBytesPerSample;
  const std::size_t total_byte_count =
      kCanonicalWavHeaderSize + metadata_chunk.size() + data_byte_count;
  if (data_byte_count > kMaxChunkByteCount ||
      total_byte_count > (static_cast<std::size_t>(kMaxChunkByteCount) + 8u)) {
    return {};
  }

  std::vector<std::uint8_t> wav_bytes(total_byte_count, 0);
  wav_bytes[0] = 'R';
  wav_bytes[1] = 'I';
  wav_bytes[2] = 'F';
  wav_bytes[3] = 'F';
  WriteU32Le(wav_bytes.data() + 4,
             static_cast<std::uint32_t>(total_byte_count - 8));
  wav_bytes[8] = 'W';
  wav_bytes[9] = 'A';
  wav_bytes[10] = 'V';
  wav_bytes[11] = 'E';
  wav_bytes[12] = 'f';
  wav_bytes[13] = 'm';
  wav_bytes[14] = 't';
  wav_bytes[15] = ' ';
  WriteU32Le(wav_bytes.data() + 16, static_cast<std::uint32_t>(kFmtChunkSize));
  WriteU16Le(wav_bytes.data() + 20, kPcmFormatCode);
  WriteU16Le(wav_bytes.data() + 22, kMonoChannelCount);
  WriteU32Le(wav_bytes.data() + 24, static_cast<std::uint32_t>(sample_rate_hz));
  WriteU32Le(wav_bytes.data() + 28,
             static_cast<std::uint32_t>(sample_rate_hz * kMonoChannelCount *
                                        kBytesPerSample));
  WriteU16Le(wav_bytes.data() + 32,
             static_cast<std::uint16_t>(kMonoChannelCount * kBytesPerSample));
  WriteU16Le(wav_bytes.data() + 34, kBitsPerSample);
  std::size_t chunk_write_offset = 36;
  if (!metadata_chunk.empty()) {
    for (std::size_t index = 0; index < metadata_chunk.size(); ++index) {
      wav_bytes[chunk_write_offset + index] = metadata_chunk[index];
    }
    chunk_write_offset += metadata_chunk.size();
  }
  wav_bytes[chunk_write_offset + 0] = 'd';
  wav_bytes[chunk_write_offset + 1] = 'a';
  wav_bytes[chunk_write_offset + 2] = 't';
  wav_bytes[chunk_write_offset + 3] = 'a';
  WriteU32Le(wav_bytes.data() + chunk_write_offset + 4,
             static_cast<std::uint32_t>(data_byte_count));

  std::size_t write_offset = chunk_write_offset + kChunkHeaderSize;
  for (const std::int16_t sample : pcm) {
    WriteU16Le(wav_bytes.data() + write_offset,
               static_cast<std::uint16_t>(sample));
    write_offset += kBytesPerSample;
  }
  return wav_bytes;
}

WavPcm16ParseResult ParseMonoPcm16WavBytes(const std::uint8_t* wav_bytes,
                                           std::size_t wav_byte_count) {
  if (wav_bytes == nullptr && wav_byte_count > 0) {
    return {WavPcm16Status::kInvalidArgument, {}};
  }
  if (wav_byte_count < kRiffHeaderSize) {
    return {WavPcm16Status::kInvalidHeader, {}};
  }
  if (!MatchesTag(wav_bytes, "RIFF") || !MatchesTag(wav_bytes + 8, "WAVE")) {
    return {WavPcm16Status::kInvalidHeader, {}};
  }

  bool saw_fmt_chunk = false;
  bool saw_data_chunk = false;
  int sample_rate_hz = 0;
  int channels = 0;
  std::size_t data_offset = 0;
  std::size_t data_byte_count = 0;

  std::size_t chunk_offset = kRiffHeaderSize;
  while (HasRange(wav_byte_count, chunk_offset, kChunkHeaderSize)) {
    const std::uint8_t* chunk_header = wav_bytes + chunk_offset;
    const std::uint32_t chunk_size = ReadU32Le(chunk_header + 4);
    const std::size_t chunk_data_offset = chunk_offset + kChunkHeaderSize;
    if (!HasRange(wav_byte_count, chunk_data_offset, chunk_size)) {
      return {WavPcm16Status::kTruncatedData, {}};
    }

    if (MatchesTag(chunk_header, "fmt ")) {
      if (chunk_size < kFmtChunkSize) {
        return {WavPcm16Status::kInvalidHeader, {}};
      }
      const std::uint8_t* fmt = wav_bytes + chunk_data_offset;
      const std::uint16_t format_code = ReadU16Le(fmt);
      const std::uint16_t fmt_channels = ReadU16Le(fmt + 2);
      const std::uint32_t fmt_sample_rate_hz = ReadU32Le(fmt + 4);
      const std::uint16_t block_align = ReadU16Le(fmt + 12);
      const std::uint16_t bits_per_sample = ReadU16Le(fmt + 14);

      if (format_code != kPcmFormatCode || fmt_channels != kMonoChannelCount ||
          bits_per_sample != kBitsPerSample ||
          block_align != (kMonoChannelCount * kBytesPerSample) ||
          fmt_sample_rate_hz == 0u) {
        return {WavPcm16Status::kUnsupportedFormat, {}};
      }

      saw_fmt_chunk = true;
      sample_rate_hz = static_cast<int>(fmt_sample_rate_hz);
      channels = static_cast<int>(fmt_channels);
    } else if (MatchesTag(chunk_header, "data")) {
      if ((chunk_size % kBytesPerSample) != 0u) {
        return {WavPcm16Status::kTruncatedData, {}};
      }
      saw_data_chunk = true;
      data_offset = chunk_data_offset;
      data_byte_count = chunk_size;
    }

    chunk_offset = chunk_data_offset + chunk_size + (chunk_size % 2u);
  }

  if (!saw_fmt_chunk || !saw_data_chunk) {
    return {WavPcm16Status::kInvalidHeader, {}};
  }

  WavPcm16 wav{};
  wav.sample_rate_hz = sample_rate_hz;
  wav.channels = channels;
  wav.mono_pcm.reserve(data_byte_count / kBytesPerSample);
  for (std::size_t offset = 0; offset < data_byte_count;
       offset += kBytesPerSample) {
    wav.mono_pcm.push_back(
        static_cast<std::int16_t>(ReadU16Le(wav_bytes + data_offset + offset)));
  }
  return {WavPcm16Status::kOk, wav};
}

FlipBitsAudioMetadataParseResult ParseFlipBitsAudioMetadataBytes(
    const std::uint8_t* wav_bytes, std::size_t wav_byte_count) {
  if (wav_bytes == nullptr && wav_byte_count > 0) {
    return {FlipBitsAudioMetadataStatus::kInvalidArgument, {}};
  }
  if (wav_byte_count < kRiffHeaderSize) {
    return {FlipBitsAudioMetadataStatus::kInvalidHeader, {}};
  }
  if (!MatchesTag(wav_bytes, "RIFF") || !MatchesTag(wav_bytes + 8, "WAVE")) {
    return {FlipBitsAudioMetadataStatus::kInvalidHeader, {}};
  }

  std::size_t chunk_offset = kRiffHeaderSize;
  while (HasRange(wav_byte_count, chunk_offset, kChunkHeaderSize)) {
    const std::uint8_t* chunk_header = wav_bytes + chunk_offset;
    const std::uint32_t chunk_size = ReadU32Le(chunk_header + 4);
    const std::size_t chunk_data_offset = chunk_offset + kChunkHeaderSize;
    if (MatchesTag(chunk_header, "data")) {
      return {FlipBitsAudioMetadataStatus::kNotFound, {}};
    }
    if (!HasRange(wav_byte_count, chunk_data_offset, chunk_size)) {
      return {FlipBitsAudioMetadataStatus::kTruncatedData, {}};
    }
    if (MatchesTag(chunk_header, "WBAG")) {
      return ParseFlipBitsMetadataChunk(wav_bytes + chunk_data_offset,
                                        chunk_size);
    }
    chunk_offset = chunk_data_offset + chunk_size + (chunk_size % 2u);
  }

  return {FlipBitsAudioMetadataStatus::kNotFound, {}};
}

}  // namespace audio_io::detail::bytes_impl
