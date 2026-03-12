#include "wav_io.h"

#include "wav_io_shared.h"

namespace audio_io {

void WriteMonoPcm16Wav(const std::filesystem::path& output_path,
                       int sample_rate_hz,
                       const std::vector<int16_t>& pcm) {
    detail::WriteMonoPcm16WavShared(output_path, sample_rate_hz, pcm);
}

WavPcm16 ReadMonoPcm16Wav(const std::filesystem::path& input_path) {
    return detail::ReadMonoPcm16WavShared<WavPcm16>(input_path);
}

}  // namespace audio_io
