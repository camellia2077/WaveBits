module;

export module bag.flash.signal;

import std;

export import bag.common.types;

export namespace bag::flash {

struct BfskConfig {
  double low_freq_hz = 400.0;
  double high_freq_hz = 800.0;
  int sample_rate_hz = 44100;
  std::size_t samples_per_bit = 2205;
  double bit_duration_sec = 0.05;
  double amplitude = 0.8;
};

struct FlashPayloadChunk {
  std::uint8_t bit_value = 0;
  std::size_t sample_offset = 0;
  std::size_t sample_count = 0;
  double carrier_freq_hz = 0.0;
};

struct FlashPayloadLayout {
  std::vector<FlashPayloadChunk> chunks;
  std::size_t payload_sample_count = 0;
};

BfskConfig MakeBfskConfig(const CoreConfig& config);
BfskConfig MakeBfskConfigForSignalProfile(const CoreConfig& config,
                                          FlashSignalProfile signal_profile);

FlashPayloadLayout BuildPayloadLayout(const std::vector<std::uint8_t>& bytes,
                                      const BfskConfig& config = {});

std::vector<std::int16_t> EncodeBytesToPcm16(
    const std::vector<std::uint8_t>& bytes, const BfskConfig& config = {},
    const EncodeProgressSink* progress_sink = nullptr,
    float progress_begin = 0.0f, float progress_end = 1.0f);
std::vector<std::uint8_t> DecodePcm16ToBytes(
    const std::vector<std::int16_t>& pcm, const BfskConfig& config = {});

}  // namespace bag::flash
