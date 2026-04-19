namespace bag::pro {

using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {

constexpr double kPi = 3.14159265358979323846;

float LerpProgress(float begin, float end, float ratio) {
  return begin + (end - begin) * std::clamp(ratio, 0.0f, 1.0f);
}

constexpr std::size_t kProgressReportStrideSamples = 256;
constexpr std::size_t kSymbolsPerPayloadByteForProgress = 2;

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

std::size_t NonZeroWork(std::size_t work) {
  return std::max(work, static_cast<std::size_t>(1));
}

struct ProEncodeWorkBudget {
  std::size_t prepare_payload_work = 1;
  std::size_t prepare_symbol_work = 1;
  std::size_t render_work = 1;
  std::size_t finalize_work = 1;
  std::size_t total_work = 4;
};

ProEncodeWorkBudget BuildEncodeWorkBudget(std::size_t payload_size,
                                          int symbol_samples) {
  const std::size_t symbol_count =
      payload_size * kSymbolsPerPayloadByteForProgress;

  ProEncodeWorkBudget budget{};
  budget.prepare_payload_work = NonZeroWork(payload_size);
  budget.prepare_symbol_work = NonZeroWork(symbol_count);
  budget.render_work = NonZeroWork(
      symbol_count * static_cast<std::size_t>(std::max(symbol_samples, 0)));
  budget.finalize_work = 1;
  budget.total_work = budget.prepare_payload_work + budget.prepare_symbol_work +
                      budget.render_work + budget.finalize_work;
  return budget;
}

bool IsValidConfig(const DualToneConfig& config) {
  return config.sample_rate_hz > 0 && config.symbol_samples > 0 &&
         config.amplitude > 0.0;
}

double GoertzelPower(const int16_t* chunk, int chunk_size, int sample_rate_hz,
                     double target_freq_hz) {
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

size_t FindStrongestBand(const int16_t* chunk, int chunk_size,
                         int sample_rate_hz,
                         const std::array<double, 4>& freqs_hz) {
  std::array<double, 4> powers{};
  for (size_t index = 0; index < freqs_hz.size(); ++index) {
    powers[index] =
        GoertzelPower(chunk, chunk_size, sample_rate_hz, freqs_hz[index]);
  }
  return static_cast<size_t>(std::distance(
      powers.begin(), std::max_element(powers.begin(), powers.end())));
}

class ProDecoder final : public ITransportDecoder {
 public:
  explicit ProDecoder(CoreConfig config) : config_(config) {}

  ErrorCode PushPcm(const PcmBlock& block) override {
    if (block.samples == nullptr || block.sample_count == 0) {
      return ErrorCode::kInvalidArgument;
    }
    buffered_pcm_.insert(buffered_pcm_.end(), block.samples,
                         block.samples + block.sample_count);
    has_pending_result_ = true;
    return ErrorCode::kOk;
  }

  ErrorCode PollTextResult(TextResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    if (!has_pending_result_ || buffered_pcm_.empty()) {
      out_result->text.clear();
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kPro;
      return ErrorCode::kNotReady;
    }

    std::string decoded_text;
    const ErrorCode decode_code =
        DecodePcm16ToText(config_, buffered_pcm_, &decoded_text);
    if (decode_code != ErrorCode::kOk) {
      out_result->text.clear();
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kPro;
      return ErrorCode::kInternal;
    }

    out_result->text = decoded_text;
    out_result->complete = true;
    out_result->confidence = 1.0f;
    out_result->mode = TransportMode::kPro;
    has_pending_result_ = false;
    return ErrorCode::kOk;
  }

  void Reset() override {
    buffered_pcm_.clear();
    has_pending_result_ = false;
  }

 private:
  CoreConfig config_;
  std::vector<int16_t> buffered_pcm_;
  bool has_pending_result_ = false;
};

}  // namespace

DualToneConfig MakeDualToneConfig(const CoreConfig& config) {
  DualToneConfig out{};
  out.sample_rate_hz = config.sample_rate_hz;
  out.symbol_samples = config.frame_samples;
  return out;
}

ErrorCode EncodeSymbolsToPcm16(const std::vector<uint8_t>& symbols,
                               const DualToneConfig& config,
                               std::vector<int16_t>* out_pcm,
                               const EncodeProgressSink* progress_sink,
                               float progress_begin, float progress_end) {
  if (out_pcm == nullptr || !IsValidConfig(config)) {
    return ErrorCode::kInvalidArgument;
  }

  out_pcm->clear();
  out_pcm->reserve(symbols.size() * static_cast<size_t>(config.symbol_samples));

  const std::size_t total_samples =
      symbols.size() * static_cast<std::size_t>(config.symbol_samples);
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_begin);
  if (total_samples == 0) {
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                         progress_end);
    return ErrorCode::kOk;
  }

  std::size_t rendered_samples = 0;
  std::size_t last_reported_samples = 0;
  for (size_t symbol_index = 0; symbol_index < symbols.size(); ++symbol_index) {
    if (ShouldCancelEncode(progress_sink)) {
      out_pcm->clear();
      return ErrorCode::kCancelled;
    }
    const uint8_t symbol = symbols[symbol_index];
    if (symbol > 0x0F) {
      return ErrorCode::kInvalidArgument;
    }

    const double low_freq_hz =
        config.low_freqs_hz[static_cast<size_t>(symbol / 4)];
    const double high_freq_hz =
        config.high_freqs_hz[static_cast<size_t>(symbol % 4)];
    const double low_omega = 2.0 * kPi * low_freq_hz;
    const double high_omega = 2.0 * kPi * high_freq_hz;

    for (int index = 0; index < config.symbol_samples; ++index) {
      const double time = static_cast<double>(index) /
                          static_cast<double>(config.sample_rate_hz);
      const double low_sample = std::sin(low_omega * time);
      const double high_sample = std::sin(high_omega * time);
      const double sample = config.amplitude * 0.5 * (low_sample + high_sample);
      const double clamped = std::max(-1.0, std::min(1.0, sample));
      out_pcm->push_back(static_cast<int16_t>(clamped * 32767.0));
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
  return ErrorCode::kOk;
}

ErrorCode DecodePcm16ToSymbols(const std::vector<int16_t>& pcm,
                               const DualToneConfig& config,
                               std::vector<uint8_t>* out_symbols) {
  if (out_symbols == nullptr || !IsValidConfig(config)) {
    return ErrorCode::kInvalidArgument;
  }
  if (pcm.empty()) {
    out_symbols->clear();
    return ErrorCode::kOk;
  }
  if (pcm.size() % static_cast<size_t>(config.symbol_samples) != 0) {
    return ErrorCode::kInvalidArgument;
  }

  out_symbols->clear();
  out_symbols->reserve(pcm.size() / static_cast<size_t>(config.symbol_samples));
  for (size_t offset = 0; offset < pcm.size();
       offset += static_cast<size_t>(config.symbol_samples)) {
    const int16_t* chunk = pcm.data() + offset;
    const size_t low_index =
        FindStrongestBand(chunk, config.symbol_samples, config.sample_rate_hz,
                          config.low_freqs_hz);
    const size_t high_index =
        FindStrongestBand(chunk, config.symbol_samples, config.sample_rate_hz,
                          config.high_freqs_hz);
    out_symbols->push_back(static_cast<uint8_t>(low_index * 4 + high_index));
  }

  return ErrorCode::kOk;
}

ErrorCode EncodePayloadToPcm16(const std::vector<uint8_t>& payload,
                               const DualToneConfig& config,
                               std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<uint8_t> symbols;
  const ErrorCode symbols_code = EncodePayloadToSymbols(payload, &symbols);
  if (symbols_code != ErrorCode::kOk) {
    return symbols_code;
  }
  return EncodeSymbolsToPcm16(symbols, config, out_pcm);
}

ErrorCode DecodePcm16ToPayload(const std::vector<int16_t>& pcm,
                               const DualToneConfig& config,
                               std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<uint8_t> symbols;
  const ErrorCode symbols_code = DecodePcm16ToSymbols(pcm, config, &symbols);
  if (symbols_code != ErrorCode::kOk) {
    return symbols_code;
  }
  return DecodeSymbolsToPayload(symbols, out_payload);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                       0.0f);
  if (ShouldCancelEncode(progress_sink)) {
    return ErrorCode::kCancelled;
  }
  std::vector<uint8_t> payload;
  const ErrorCode payload_code = EncodeTextToPayload(text, &payload);
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }
  const ProEncodeWorkBudget work_budget =
      BuildEncodeWorkBudget(payload.size(), config.frame_samples);
  std::size_t completed_work = work_budget.prepare_payload_work;
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                       static_cast<float>(completed_work) /
                           static_cast<float>(work_budget.total_work));
  if (ShouldCancelEncode(progress_sink)) {
    return ErrorCode::kCancelled;
  }
  std::vector<uint8_t> symbols;
  const ErrorCode symbols_code = EncodePayloadToSymbols(payload, &symbols);
  if (symbols_code != ErrorCode::kOk) {
    return symbols_code;
  }
  completed_work += work_budget.prepare_symbol_work;
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                       static_cast<float>(completed_work) /
                           static_cast<float>(work_budget.total_work));
  const float render_progress_begin =
      static_cast<float>(completed_work) /
      static_cast<float>(work_budget.total_work);
  const float render_progress_end =
      static_cast<float>(completed_work + work_budget.render_work) /
      static_cast<float>(work_budget.total_work);
  const ErrorCode encode_code = EncodeSymbolsToPcm16(
      symbols, MakeDualToneConfig(config), out_pcm, progress_sink,
      render_progress_begin, render_progress_end);
  if (encode_code != ErrorCode::kOk) {
    return encode_code;
  }
  completed_work += work_budget.render_work;
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kFinalizing,
                       static_cast<float>(completed_work) /
                           static_cast<float>(work_budget.total_work));
  if (ShouldCancelEncode(progress_sink)) {
    return ErrorCode::kCancelled;
  }
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kFinalizing, 1.0f);
  return ErrorCode::kOk;
}

ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<int16_t>& pcm,
                            std::string* out_text) {
  if (out_text == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<uint8_t> payload;
  const ErrorCode payload_code =
      DecodePcm16ToPayload(pcm, MakeDualToneConfig(config), &payload);
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }
  return DecodePayloadToText(payload, out_text);
}

std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config) {
  return std::make_unique<ProDecoder>(config);
}

}  // namespace bag::pro
