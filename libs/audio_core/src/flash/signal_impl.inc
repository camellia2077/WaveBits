namespace bag::flash {

using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {

float LerpProgress(float begin, float end, float ratio) {
  return begin + (end - begin) * std::clamp(ratio, 0.0f, 1.0f);
}

void ThrowIfEncodeCancelled(const EncodeProgressSink* progress_sink) {
  if (ShouldCancelEncode(progress_sink)) {
    throw EncodeCancelled{};
  }
}

constexpr std::size_t kProgressReportStrideSamples = 256;

void ReportCompletedSampleProgress(const EncodeProgressSink* progress_sink,
                                   EncodeProgressPhase phase,
                                   float progress_begin, float progress_end,
                                   std::size_t completed_samples,
                                   std::size_t total_samples) {
  const float ratio =
      total_samples == 0
          ? 1.0f
          : static_cast<float>(std::min(completed_samples, total_samples)) /
                static_cast<float>(total_samples);
  ReportEncodeProgress(progress_sink, phase,
                       LerpProgress(progress_begin, progress_end, ratio));
}

void MaybeReportCompletedSampleProgress(
    const EncodeProgressSink* progress_sink, EncodeProgressPhase phase,
    float progress_begin, float progress_end, std::size_t completed_samples,
    std::size_t total_samples, std::size_t* last_reported_samples,
    bool force = false) {
  if (last_reported_samples == nullptr) {
    return;
  }

  const std::size_t clamped_completed_samples =
      total_samples == 0 ? 0 : std::min(completed_samples, total_samples);
  if (!force && clamped_completed_samples < total_samples &&
      clamped_completed_samples - *last_reported_samples <
          kProgressReportStrideSamples) {
    return;
  }

  ReportCompletedSampleProgress(progress_sink, phase, progress_begin,
                                progress_end, clamped_completed_samples,
                                total_samples);
  *last_reported_samples = clamped_completed_samples;
}

std::size_t SamplesPerBitForSignalProfile(FlashSignalProfile signal_profile,
                                          const CoreConfig& config) {
  if (config.frame_samples <= 0) {
    return 0;
  }

  switch (signal_profile) {
    case FlashSignalProfile::kRitualChant:
      return static_cast<std::size_t>(config.frame_samples) *
             static_cast<std::size_t>(3);
    case FlashSignalProfile::kCodedBurst:
    default:
      return static_cast<std::size_t>(config.frame_samples);
  }
}

int ChunkSize(const BfskConfig& config) {
  if (config.samples_per_bit == 0) {
    return 0;
  }
  if (config.samples_per_bit >
      static_cast<std::size_t>(std::numeric_limits<int>::max())) {
    throw std::invalid_argument(
        "BFSK samples_per_bit exceeds supported range.");
  }
  return static_cast<int>(config.samples_per_bit);
}

std::vector<int> BytesToBits(const std::vector<uint8_t>& bytes) {
  std::vector<int> bits;
  bits.reserve(bytes.size() * 8);
  for (uint8_t value : bytes) {
    for (int shift = 7; shift >= 0; --shift) {
      bits.push_back((value >> shift) & 0x01U);
    }
  }
  return bits;
}

std::vector<uint8_t> BitsToBytes(const std::vector<int>& bits) {
  std::vector<uint8_t> bytes;
  bytes.reserve(bits.size() / 8);
  for (size_t index = 0; index + 7 < bits.size(); index += 8) {
    uint8_t value = 0;
    for (int bit = 0; bit < 8; ++bit) {
      value = static_cast<uint8_t>(
          (value << 1) | (bits[index + static_cast<size_t>(bit)] ? 1 : 0));
    }
    bytes.push_back(value);
  }
  return bytes;
}

double GoertzelPower(const int16_t* chunk, int chunk_size, int sample_rate_hz,
                     double target_freq_hz) {
  constexpr double kPi = 3.14159265358979323846;
  const double normalized_freq =
      target_freq_hz / static_cast<double>(sample_rate_hz);
  const double omega = 2.0 * kPi * normalized_freq;
  const double coeff = 2.0 * std::cos(omega);

  double q0 = 0.0;
  double q1 = 0.0;
  double q2 = 0.0;
  for (int index = 0; index < chunk_size; ++index) {
    const double sample =
        static_cast<double>(chunk[static_cast<size_t>(index)]) / 32767.0;
    q0 = coeff * q1 - q2 + sample;
    q2 = q1;
    q1 = q0;
  }
  return q1 * q1 + q2 * q2 - coeff * q1 * q2;
}

}  // namespace

BfskConfig MakeBfskConfigForSignalProfile(const CoreConfig& config,
                                          FlashSignalProfile signal_profile) {
  if (!IsValidFlashSignalProfile(signal_profile)) {
    throw std::invalid_argument("Invalid flash signal profile.");
  }

  BfskConfig bfsk{};
  bfsk.sample_rate_hz = config.sample_rate_hz;
  bfsk.samples_per_bit = SamplesPerBitForSignalProfile(signal_profile, config);
  bfsk.bit_duration_sec = config.sample_rate_hz > 0
                              ? static_cast<double>(bfsk.samples_per_bit) /
                                    static_cast<double>(config.sample_rate_hz)
                              : 0.0;
  return bfsk;
}

BfskConfig MakeBfskConfig(const CoreConfig& config) {
  return MakeBfskConfigForSignalProfile(config, config.flash_signal_profile);
}

FlashPayloadLayout BuildPayloadLayout(const std::vector<uint8_t>& bytes,
                                      const BfskConfig& config) {
  const int chunk_size = ChunkSize(config);
  if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }

  const std::vector<int> bits = BytesToBits(bytes);
  FlashPayloadLayout layout{};
  layout.chunks.reserve(bits.size());

  size_t sample_offset = 0;
  for (int bit : bits) {
    FlashPayloadChunk chunk{};
    chunk.bit_value = static_cast<uint8_t>(bit);
    chunk.sample_offset = sample_offset;
    chunk.sample_count = static_cast<size_t>(chunk_size);
    chunk.carrier_freq_hz = bit == 1 ? config.high_freq_hz : config.low_freq_hz;
    layout.chunks.push_back(chunk);
    sample_offset += chunk.sample_count;
  }
  layout.payload_sample_count = sample_offset;
  return layout;
}

std::vector<int16_t> EncodeBytesToPcm16(const std::vector<uint8_t>& bytes,
                                        const BfskConfig& config,
                                        const EncodeProgressSink* progress_sink,
                                        float progress_begin,
                                        float progress_end) {
  const FlashPayloadLayout layout = BuildPayloadLayout(bytes, config);
  std::vector<int16_t> pcm;
  pcm.reserve(layout.payload_sample_count);

  constexpr double kPi = 3.14159265358979323846;
  const std::size_t total_samples = layout.payload_sample_count;
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_begin);
  ThrowIfEncodeCancelled(progress_sink);
  if (total_samples == 0) {
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                         progress_end);
    return pcm;
  }

  std::size_t rendered_samples = 0;
  std::size_t last_reported_samples = 0;
  for (std::size_t chunk_index = 0; chunk_index < layout.chunks.size();
       ++chunk_index) {
    const FlashPayloadChunk& chunk = layout.chunks[chunk_index];
    ThrowIfEncodeCancelled(progress_sink);
    const double two_pi_f = 2.0 * kPi * chunk.carrier_freq_hz;
    for (size_t index = 0; index < chunk.sample_count; ++index) {
      const double time = static_cast<double>(index) /
                          static_cast<double>(config.sample_rate_hz);
      const double sample = config.amplitude * std::sin(two_pi_f * time);
      const double clamped = std::max(-1.0, std::min(1.0, sample));
      pcm.push_back(static_cast<int16_t>(clamped * 32767.0));
      ++rendered_samples;
      MaybeReportCompletedSampleProgress(
          progress_sink, EncodeProgressPhase::kRenderingPcm, progress_begin,
          progress_end, rendered_samples, total_samples,
          &last_reported_samples);
    }
    MaybeReportCompletedSampleProgress(
        progress_sink, EncodeProgressPhase::kRenderingPcm, progress_begin,
        progress_end, rendered_samples, total_samples, &last_reported_samples,
        true);
  }
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_end);

  return pcm;
}

std::vector<uint8_t> DecodePcm16ToBytes(const std::vector<int16_t>& pcm,
                                        const BfskConfig& config) {
  const int chunk_size = ChunkSize(config);
  if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }
  if (pcm.empty()) {
    return {};
  }

  std::vector<int> bits;
  for (size_t offset = 0;
       offset + static_cast<size_t>(chunk_size) <= pcm.size();
       offset += static_cast<size_t>(chunk_size)) {
    const int16_t* chunk = pcm.data() + offset;
    const double low_power = GoertzelPower(
        chunk, chunk_size, config.sample_rate_hz, config.low_freq_hz);
    const double high_power = GoertzelPower(
        chunk, chunk_size, config.sample_rate_hz, config.high_freq_hz);
    bits.push_back(high_power > low_power ? 1 : 0);
  }

  return BitsToBytes(bits);
}

}  // namespace bag::flash
