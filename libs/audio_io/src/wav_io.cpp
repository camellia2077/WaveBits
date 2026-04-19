#include "wav_io.h"

#include "wav_io_backend.h"

namespace audio_io {

std::vector<std::uint8_t> SerializeMonoPcm16Wav(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm) {
  return detail::SerializeMonoPcm16WavBackend(sample_rate_hz, pcm);
}

std::vector<std::uint8_t> SerializeMonoPcm16WavWithMetadata(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm,
    const WaveBitsAudioMetadata& metadata) {
  return detail::SerializeMonoPcm16WavWithMetadataBackend(sample_rate_hz, pcm,
                                                          metadata);
}

WavPcm16ParseResult ParseMonoPcm16Wav(const std::uint8_t* wav_bytes,
                                      std::size_t wav_byte_count) {
  return detail::ParseMonoPcm16WavBackend(wav_bytes, wav_byte_count);
}

WavPcm16ParseResult ParseMonoPcm16Wav(
    const std::vector<std::uint8_t>& wav_bytes) {
  return ParseMonoPcm16Wav(wav_bytes.data(), wav_bytes.size());
}

WaveBitsAudioMetadataParseResult ParseWaveBitsAudioMetadata(
    const std::uint8_t* wav_bytes, std::size_t wav_byte_count) {
  return detail::ParseWaveBitsAudioMetadataBackend(wav_bytes, wav_byte_count);
}

WaveBitsAudioMetadataParseResult ParseWaveBitsAudioMetadata(
    const std::vector<std::uint8_t>& wav_bytes) {
  return ParseWaveBitsAudioMetadata(wav_bytes.data(), wav_bytes.size());
}

void WriteMonoPcm16Wav(const std::filesystem::path& output_path,
                       int sample_rate_hz,
                       const std::vector<std::int16_t>& pcm) {
  detail::WriteMonoPcm16WavBackend(output_path, sample_rate_hz, pcm);
}

WavPcm16 ReadMonoPcm16Wav(const std::filesystem::path& input_path) {
  auto backend_result = detail::ReadMonoPcm16WavBackend(input_path);
  WavPcm16 output{};
  output.sample_rate_hz = backend_result.sample_rate_hz;
  output.channels = backend_result.channels;
  output.mono_pcm.swap(backend_result.mono_pcm);
  return output;
}

}  // namespace audio_io
