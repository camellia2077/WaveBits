#pragma once

#include <cstdint>
#include <filesystem>
#include <stdexcept>
#include <utility>
#include <vector>

#include <sndfile.h>

namespace audio_io::detail {

inline std::vector<int16_t> ToMono(const std::vector<int16_t>& interleaved, int channels) {
    if (channels <= 1) {
        return interleaved;
    }

    const size_t frames = interleaved.size() / static_cast<size_t>(channels);
    std::vector<int16_t> mono(frames, 0);
    for (size_t frame = 0; frame < frames; ++frame) {
        int64_t sum = 0;
        for (int ch = 0; ch < channels; ++ch) {
            sum += interleaved[frame * static_cast<size_t>(channels) + static_cast<size_t>(ch)];
        }
        mono[frame] = static_cast<int16_t>(sum / channels);
    }
    return mono;
}

inline void WriteMonoPcm16WavImpl(const std::filesystem::path& output_path,
                                  int sample_rate_hz,
                                  const std::vector<int16_t>& pcm) {
    auto parent = output_path.parent_path();
    if (!parent.empty()) {
        std::filesystem::create_directories(parent);
    }

    SF_INFO info{};
    info.samplerate = sample_rate_hz;
    info.channels = 1;
    info.format = SF_FORMAT_WAV | SF_FORMAT_PCM_16;

    SNDFILE* file = sf_open(output_path.string().c_str(), SFM_WRITE, &info);
    if (file == nullptr) {
        throw std::runtime_error(sf_strerror(nullptr));
    }

    const sf_count_t written = sf_write_short(file, pcm.data(), static_cast<sf_count_t>(pcm.size()));
    sf_close(file);
    if (written != static_cast<sf_count_t>(pcm.size())) {
        throw std::runtime_error("Failed to write full WAV data.");
    }
}

inline std::pair<int, std::vector<int16_t>> ReadMonoPcm16WavImpl(const std::filesystem::path& input_path) {
    SF_INFO info{};
    SNDFILE* file = sf_open(input_path.string().c_str(), SFM_READ, &info);
    if (file == nullptr) {
        throw std::runtime_error(sf_strerror(nullptr));
    }

    const sf_count_t total_samples = info.frames * info.channels;
    std::vector<int16_t> interleaved(static_cast<size_t>(total_samples), 0);
    const sf_count_t read_count = sf_read_short(file, interleaved.data(), total_samples);
    sf_close(file);

    if (read_count != total_samples) {
        throw std::runtime_error("Failed to read full WAV data.");
    }

    return {info.samplerate, ToMono(interleaved, info.channels)};
}

}  // namespace audio_io::detail
