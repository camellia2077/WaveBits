#pragma once

#include <cstdint>
#include <filesystem>
#include <vector>

namespace audio_io::detail {

struct WavIoReadResult {
    int sample_rate_hz = 0;
    std::vector<std::int16_t> mono_pcm;
};

void WriteMonoPcm16WavBackend(const std::filesystem::path& output_path,
                              int sample_rate_hz,
                              const std::vector<std::int16_t>& pcm);
WavIoReadResult ReadMonoPcm16WavBackend(const std::filesystem::path& input_path);

}  // namespace audio_io::detail
