#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace android_audio_io {

enum class WavDecodeStatus {
    kOk = 0,
    kInvalidArgument = 1,
    kInvalidHeader = 2,
    kUnsupportedFormat = 3,
    kTruncatedData = 4,
};

enum class WaveBitsMetadataStatus {
    kOk = 0,
    kNotFound = 1,
    kInvalidArgument = 2,
    kInvalidHeader = 3,
    kUnsupportedVersion = 4,
    kInvalidMetadata = 5,
    kTruncatedData = 6,
};

struct EncodedWaveBitsMetadata {
    std::uint8_t version = 0;
    std::uint8_t mode = 0;
    bool has_flash_voicing_style = false;
    std::uint8_t flash_voicing_style = 0;
    std::string created_at_iso_utc;
    std::uint32_t duration_ms = 0;
    std::uint32_t frame_samples = 0;
    std::uint32_t pcm_sample_count = 0;
    std::string app_version;
    std::string core_version;
};

struct DecodedMonoPcm16WavData {
    WavDecodeStatus status = WavDecodeStatus::kOk;
    int sample_rate_hz = 0;
    int channels = 0;
    std::vector<std::int16_t> pcm_samples;
    WaveBitsMetadataStatus metadata_status = WaveBitsMetadataStatus::kNotFound;
    EncodedWaveBitsMetadata metadata{};
};

std::vector<std::uint8_t> EncodeMonoPcm16ToWavBytes(
    int sample_rate_hz,
    const std::vector<std::int16_t>& pcm_samples,
    const EncodedWaveBitsMetadata* metadata);
DecodedMonoPcm16WavData DecodeMonoPcm16WavBytes(const std::vector<std::uint8_t>& wav_bytes);

}  // namespace android_audio_io
