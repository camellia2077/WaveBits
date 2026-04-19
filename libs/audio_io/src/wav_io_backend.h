#pragma once

#include <cstdint>
#include <filesystem>
#include <vector>

#include "wav_io.h"

namespace audio_io::detail {

struct WavIoReadResult {
  int sample_rate_hz = 0;
  int channels = 1;
  std::vector<std::int16_t> mono_pcm;
};

std::vector<std::uint8_t> SerializeMonoPcm16WavBackend(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm);
std::vector<std::uint8_t> SerializeMonoPcm16WavWithMetadataBackend(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm,
    const WaveBitsAudioMetadata& metadata);
WavPcm16ParseResult ParseMonoPcm16WavBackend(const std::uint8_t* wav_bytes,
                                             std::size_t wav_byte_count);
WaveBitsAudioMetadataParseResult ParseWaveBitsAudioMetadataBackend(
    const std::uint8_t* wav_bytes, std::size_t wav_byte_count);

void WriteMonoPcm16WavBackend(const std::filesystem::path& output_path,
                              int sample_rate_hz,
                              const std::vector<std::int16_t>& pcm);
WavIoReadResult ReadMonoPcm16WavBackend(
    const std::filesystem::path& input_path);

}  // namespace audio_io::detail
