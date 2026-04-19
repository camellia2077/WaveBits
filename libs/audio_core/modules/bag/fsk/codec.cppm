module;

export module bag.fsk.codec;

import std;

export namespace bag::fsk {

struct FskConfig {
  double low_freq_hz = 400.0;
  double high_freq_hz = 800.0;
  double bit_duration_sec = 0.05;
  int sample_rate_hz = 44100;
  double amplitude = 0.8;
};

std::vector<std::int16_t> EncodeTextToPcm16(const std::string& text,
                                            const FskConfig& config = {});
std::string DecodePcm16ToText(const std::vector<std::int16_t>& pcm,
                              const FskConfig& config = {});

}  // namespace bag::fsk
