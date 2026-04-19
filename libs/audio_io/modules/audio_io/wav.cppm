module;

export module audio_io.wav;

import std;

export namespace audio_io {

enum class WavPcm16Status {
  kOk = 0,
  kInvalidArgument = 1,
  kInvalidHeader = 2,
  kUnsupportedFormat = 3,
  kTruncatedData = 4,
};

struct WavPcm16 {
  int sample_rate_hz = 0;
  int channels = 1;
  std::vector<std::int16_t> mono_pcm;
};

struct WavPcm16ParseResult {
  WavPcm16Status status = WavPcm16Status::kOk;
  WavPcm16 wav{};
};

std::vector<std::uint8_t> SerializeMonoPcm16Wav(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm);
WavPcm16ParseResult ParseMonoPcm16Wav(const std::uint8_t* wav_bytes,
                                      std::size_t wav_byte_count);
WavPcm16ParseResult ParseMonoPcm16Wav(
    const std::vector<std::uint8_t>& wav_bytes);

void WriteMonoPcm16Wav(const std::filesystem::path& output_path,
                       int sample_rate_hz,
                       const std::vector<std::int16_t>& pcm);
WavPcm16 ReadMonoPcm16Wav(const std::filesystem::path& input_path);

}  // namespace audio_io
