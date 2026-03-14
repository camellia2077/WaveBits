module;

#include <cstdint>
#include <filesystem>
#include <vector>

#include "../../src/wav_io_backend.h"

module audio_io.wav;

import std;

namespace audio_io {

void WriteMonoPcm16Wav(const std::filesystem::path& output_path,
                       int sample_rate_hz,
                       const std::vector<std::int16_t>& pcm) {
    detail::WriteMonoPcm16WavBackend(output_path, sample_rate_hz, pcm);
}

WavPcm16 ReadMonoPcm16Wav(const std::filesystem::path& input_path) {
    auto backend_result = detail::ReadMonoPcm16WavBackend(input_path);
    WavPcm16 output{};
    output.sample_rate_hz = backend_result.sample_rate_hz;
    output.mono_pcm.swap(backend_result.mono_pcm);
    return output;
}

}  // namespace audio_io
