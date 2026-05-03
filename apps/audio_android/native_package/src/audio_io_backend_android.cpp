#include "wav_io_backend.h"

#include <cstdint>
#include <filesystem>
#include <fstream>
#include <iterator>
#include <stdexcept>
#include <vector>

#include "../../../../libs/audio_io/src/wav_io_bytes_impl.inc"

namespace audio_io::detail {

std::vector<std::uint8_t> SerializeMonoPcm16WavBackend(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm) {
  return bytes_impl::SerializeMonoPcm16WavBytes(sample_rate_hz, pcm);
}

std::vector<std::uint8_t> SerializeMonoPcm16WavWithMetadataBackend(
    int sample_rate_hz, const std::vector<std::int16_t>& pcm,
    const FlipBitsAudioMetadata& metadata) {
  return bytes_impl::SerializeMonoPcm16WavBytesWithMetadata(sample_rate_hz, pcm,
                                                            &metadata);
}

WavPcm16ParseResult ParseMonoPcm16WavBackend(const std::uint8_t* wav_bytes,
                                             std::size_t wav_byte_count) {
  return bytes_impl::ParseMonoPcm16WavBytes(wav_bytes, wav_byte_count);
}

WavPcm16InfoParseResult ProbeMonoPcm16WavBackend(const std::uint8_t* wav_bytes,
                                                 std::size_t wav_byte_count) {
  return bytes_impl::ProbeMonoPcm16WavBytes(wav_bytes, wav_byte_count);
}

FlipBitsAudioMetadataParseResult ParseFlipBitsAudioMetadataBackend(
    const std::uint8_t* wav_bytes, std::size_t wav_byte_count) {
  return bytes_impl::ParseFlipBitsAudioMetadataBytes(wav_bytes, wav_byte_count);
}

void WriteMonoPcm16WavBackend(const std::filesystem::path& output_path,
                              int sample_rate_hz,
                              const std::vector<std::int16_t>& pcm) {
  const auto wav_bytes = SerializeMonoPcm16WavBackend(sample_rate_hz, pcm);
  if (wav_bytes.empty()) {
    throw std::runtime_error("Failed to serialize mono PCM16 WAV bytes.");
  }

  const auto parent = output_path.parent_path();
  if (!parent.empty()) {
    std::filesystem::create_directories(parent);
  }

  std::ofstream file(output_path, std::ios::binary | std::ios::trunc);
  if (!file) {
    throw std::runtime_error("Failed to open WAV output file.");
  }
  file.write(reinterpret_cast<const char*>(wav_bytes.data()),
             static_cast<std::streamsize>(wav_bytes.size()));
  if (!file) {
    throw std::runtime_error("Failed to write full WAV data.");
  }
}

WavIoReadResult ReadMonoPcm16WavBackend(
    const std::filesystem::path& input_path) {
  std::ifstream file(input_path, std::ios::binary);
  if (!file) {
    throw std::runtime_error("Failed to open WAV input file.");
  }

  const std::vector<std::uint8_t> wav_bytes{
      std::istreambuf_iterator<char>(file), std::istreambuf_iterator<char>()};
  if (!file.eof() && file.fail()) {
    throw std::runtime_error("Failed to read WAV input file.");
  }

  const auto parsed =
      ParseMonoPcm16WavBackend(wav_bytes.data(), wav_bytes.size());
  if (parsed.status != WavPcm16Status::kOk) {
    throw std::runtime_error("Failed to parse mono PCM16 WAV data.");
  }

  WavIoReadResult result{};
  result.sample_rate_hz = parsed.wav.sample_rate_hz;
  result.channels = parsed.wav.channels;
  result.mono_pcm = parsed.wav.mono_pcm;
  return result;
}

}  // namespace audio_io::detail
