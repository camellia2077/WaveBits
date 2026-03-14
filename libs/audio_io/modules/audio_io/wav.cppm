module;

export module audio_io.wav;

import std;

export namespace audio_io {

struct WavPcm16 {
    int sample_rate_hz = 0;
    std::vector<std::int16_t> mono_pcm;
};

void WriteMonoPcm16Wav(const std::filesystem::path& output_path,
                       int sample_rate_hz,
                       const std::vector<std::int16_t>& pcm);
WavPcm16 ReadMonoPcm16Wav(const std::filesystem::path& input_path);

}  // namespace audio_io
