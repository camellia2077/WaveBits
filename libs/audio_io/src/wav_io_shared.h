#pragma once

#include "wav_io_detail.h"

namespace audio_io::detail {

inline void WriteMonoPcm16WavShared(const std::filesystem::path& output_path,
                                    int sample_rate_hz,
                                    const std::vector<int16_t>& pcm) {
    WriteMonoPcm16WavImpl(output_path, sample_rate_hz, pcm);
}

template <typename WavPcm16Like>
inline WavPcm16Like ReadMonoPcm16WavShared(const std::filesystem::path& input_path) {
    auto [sample_rate_hz, mono_pcm] = ReadMonoPcm16WavImpl(input_path);
    WavPcm16Like output{};
    output.sample_rate_hz = sample_rate_hz;
    output.mono_pcm = std::move(mono_pcm);
    return output;
}

}  // namespace audio_io::detail
