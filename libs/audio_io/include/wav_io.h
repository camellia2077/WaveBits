#pragma once

#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <string>
#include <vector>

namespace audio_io {

enum class WavPcm16Status {
  kOk = 0,
  kInvalidArgument = 1,
  kInvalidHeader = 2,
  kUnsupportedFormat = 3,
  kTruncatedData = 4,
};

enum class FlipBitsAudioMetadataMode : std::uint8_t {
  kUnknown = 0,
  kFlash = 1,
  kPro = 2,
  kUltra = 3,
};

enum class FlipBitsAudioMetadataFlashVoicingStyle : std::uint8_t {
  kUnknown = 0,
  kCodedBurst = 1,
  kRitualChant = 2,
  kDeepRitual = 3,
};

enum class FlipBitsAudioMetadataInputSourceKind : std::uint8_t {
  kUnknown = 0,
  kManual = 1,
  kSample = 2,
};

struct WavPcm16 {
  int sample_rate_hz = 0;
  int channels = 1;
  std::vector<std::int16_t> mono_pcm;
};

struct FlipBitsAudioMetadata {
  std::uint8_t version = 0;
  FlipBitsAudioMetadataMode mode = FlipBitsAudioMetadataMode::kUnknown;
  bool has_flash_voicing_style = false;
  FlipBitsAudioMetadataFlashVoicingStyle flash_voicing_style =
      FlipBitsAudioMetadataFlashVoicingStyle::kUnknown;
  // ISO-8601 UTC generation timestamp for the PCM payload.
  std::string created_at_iso_utc;
  std::uint32_t duration_ms = 0;
  // Saved explicitly so library/index surfaces can render audio settings
  // without decoding the full data chunk.
  std::uint32_t sample_rate_hz = 0;
  std::uint32_t frame_samples = 0;
  std::uint32_t pcm_sample_count = 0;
  // Original UTF-8 payload size from the encode request.
  std::uint32_t payload_byte_count = 0;
  // Records whether the payload came from manual text entry or a sample deck.
  FlipBitsAudioMetadataInputSourceKind input_source_kind =
      FlipBitsAudioMetadataInputSourceKind::kUnknown;
  std::uint32_t segment_count = 1;
  std::vector<std::uint32_t> segment_sample_counts;
  std::string app_version;
  std::string core_version;
};

enum class FlipBitsAudioMetadataStatus {
  kOk = 0,
  kNotFound = 1,
  kInvalidArgument = 2,
  kInvalidHeader = 3,
  kUnsupportedVersion = 4,
  kInvalidMetadata = 5,
  kTruncatedData = 6,
};

struct FlipBitsAudioMetadataParseResult {
  FlipBitsAudioMetadataStatus status = FlipBitsAudioMetadataStatus::kNotFound;
  FlipBitsAudioMetadata metadata{};
};

struct WavPcm16ParseResult {
  WavPcm16Status status = WavPcm16Status::kOk;
  WavPcm16 wav{};
};

std::vector<std::uint8_t> SerializeMonoPcm16Wav(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm);
std::vector<std::uint8_t> SerializeMonoPcm16WavWithMetadata(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm,
    const FlipBitsAudioMetadata& metadata);
WavPcm16ParseResult ParseMonoPcm16Wav(const std::uint8_t* wav_bytes,
                                      std::size_t wav_byte_count);
WavPcm16ParseResult ParseMonoPcm16Wav(
    const std::vector<std::uint8_t>& wav_bytes);
FlipBitsAudioMetadataParseResult ParseFlipBitsAudioMetadata(
    const std::uint8_t* wav_bytes, std::size_t wav_byte_count);
FlipBitsAudioMetadataParseResult ParseFlipBitsAudioMetadata(
    const std::vector<std::uint8_t>& wav_bytes);

void WriteMonoPcm16Wav(const std::filesystem::path& output_path,
                       int sample_rate_hz,
                       const std::vector<std::int16_t>& pcm);
WavPcm16 ReadMonoPcm16Wav(const std::filesystem::path& input_path);

}  // namespace audio_io
