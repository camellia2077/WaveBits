namespace bag::flash {

using std::int16_t;
using std::uint8_t;

ErrorCode EncodeTextToPcm16WithSignalProfileAndFlavor(
    const CoreConfig& config, const std::string& text,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::vector<int16_t>* out_pcm);
ErrorCode DecodePcm16ToTextWithSignalProfileAndFlavor(
    const CoreConfig& config, const std::vector<int16_t>& pcm,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::string* out_text);

namespace {

FlashSignalProfile NormalizeFormalSignalProfile(
    FlashSignalProfile signal_profile) {
  switch (signal_profile) {
    case FlashSignalProfile::kSteady:
    case FlashSignalProfile::kLitany:
    case FlashSignalProfile::kHostile:
    case FlashSignalProfile::kCollapse:
    case FlashSignalProfile::kZeal:
    case FlashSignalProfile::kVoid:
      return signal_profile;
    default:
      return FlashSignalProfile::kSteady;
  }
}

FlashVoicingFlavor NormalizeFormalVoicingFlavor(FlashVoicingFlavor flavor) {
  switch (flavor) {
    case FlashVoicingFlavor::kSteady:
    case FlashVoicingFlavor::kLitany:
    case FlashVoicingFlavor::kHostile:
    case FlashVoicingFlavor::kCollapse:
    case FlashVoicingFlavor::kZeal:
    case FlashVoicingFlavor::kVoid:
      return flavor;
    default:
      return FlashVoicingFlavor::kSteady;
  }
}

bool IsFormalVoicingCompatible(const FlashVoicingResult& voiced_payload,
                               std::size_t clean_payload_sample_count,
                               const FlashVoicingConfig& config) {
  return voiced_payload.descriptor.leading_nonpayload_samples ==
             (config.enable_preamble ? config.preamble_sample_count
                                     : static_cast<std::size_t>(0)) &&
         voiced_payload.descriptor.trailing_nonpayload_samples ==
             (config.enable_epilogue ? config.epilogue_sample_count
                                     : static_cast<std::size_t>(0)) &&
         voiced_payload.descriptor.payload_sample_count ==
             clean_payload_sample_count &&
         voiced_payload.pcm.size() ==
             clean_payload_sample_count +
                 voiced_payload.descriptor.leading_nonpayload_samples +
                 voiced_payload.descriptor.trailing_nonpayload_samples;
}

std::vector<int16_t> TrimFormalPayloadPcmForFlavor(
    const CoreConfig& config, FlashVoicingFlavor flavor,
    const std::vector<int16_t>& pcm) {
  const FlashVoicingDescriptor descriptor = DescribeVoicingOutput(
      pcm.size(), MakeFormalVoicingConfigForFlavor(config, flavor));
  return TrimToPayloadPcm(pcm, descriptor);
}

bool UsesSpectralTexture(const FlashVoicingConfig& config) {
  return config.attack_ratio > 0.0 || config.release_ratio > 0.0 ||
         config.second_harmonic_gain > 0.0 || config.third_harmonic_gain > 0.0;
}

std::size_t NonZeroWork(std::size_t work) {
  return std::max(work, static_cast<std::size_t>(1));
}

struct FlashEncodeWorkBudget {
  std::size_t prepare_text_work = 1;
  std::size_t prepare_layout_work = 1;
  std::size_t render_work = 1;
  std::size_t postprocess_work = 1;
  std::size_t finalize_work = 1;
  std::size_t total_work = 5;
};

FlashEncodeWorkBudget BuildFlashEncodeWorkBudget(
    const std::vector<uint8_t>& bytes,
    const FlashPayloadLayout& payload_layout,
    const FlashVoicingConfig& voicing_config) {
  const std::size_t bit_count = bytes.size() * static_cast<std::size_t>(8);
  const std::size_t payload_sample_count = payload_layout.payload_sample_count;
  const std::size_t leading_nonpayload_samples =
      voicing_config.enable_preamble ? voicing_config.preamble_sample_count
                                     : static_cast<std::size_t>(0);
  const std::size_t trailing_nonpayload_samples =
      voicing_config.enable_epilogue ? voicing_config.epilogue_sample_count
                                     : static_cast<std::size_t>(0);
  const std::size_t voiced_total_samples = leading_nonpayload_samples +
                                           payload_sample_count +
                                           trailing_nonpayload_samples;
  const std::size_t spectral_texture_work = UsesSpectralTexture(voicing_config)
                                                ? voiced_total_samples
                                                : static_cast<std::size_t>(0);

  FlashEncodeWorkBudget budget{};
  budget.prepare_text_work = NonZeroWork(bytes.size());
  budget.prepare_layout_work = NonZeroWork(payload_layout.chunks.size());
  budget.render_work = NonZeroWork(payload_sample_count);
  budget.postprocess_work =
      NonZeroWork(voiced_total_samples + spectral_texture_work);
  budget.finalize_work = 1;
  budget.total_work = budget.prepare_text_work + budget.prepare_layout_work +
                      budget.render_work + budget.postprocess_work +
                      budget.finalize_work;
  return budget;
}

std::vector<uint8_t> DecodeFormalPayloadBytes(
    const CoreConfig& config, FlashSignalProfile signal_profile,
    FlashVoicingFlavor flavor, const std::vector<int16_t>& trimmed_payload_pcm) {
  const BfskConfig signal_config =
      MakeBfskConfigForSignalProfile(config, signal_profile);
  switch (NormalizeFormalVoicingFlavor(flavor)) {
    case FlashVoicingFlavor::kLitany:
    case FlashVoicingFlavor::kCollapse:
      return DecodePcm16ToBytesSkippingSilence(trimmed_payload_pcm,
                                               signal_config);
    case FlashVoicingFlavor::kZeal:
      return DecodeZealPcm16ToBytes(trimmed_payload_pcm, signal_config);
    case FlashVoicingFlavor::kSteady:
    case FlashVoicingFlavor::kHostile:
    case FlashVoicingFlavor::kVoid:
    default:
      return DecodePcm16ToBytes(trimmed_payload_pcm, signal_config);
  }
}

class FlashDecoder final : public ITransportDecoder {
 public:
  explicit FlashDecoder(CoreConfig config) : config_(config) {}

  ErrorCode PushPcm(const PcmBlock& block) override {
    if (block.samples == nullptr || block.sample_count == 0) {
      return ErrorCode::kInvalidArgument;
    }
    buffered_pcm_.insert(buffered_pcm_.end(), block.samples,
                         block.samples + block.sample_count);
    has_pending_result_ = true;
    return ErrorCode::kOk;
  }

  ErrorCode PollDecodeResult(DecodeResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    if (!has_pending_result_ || buffered_pcm_.empty()) {
      out_result->text.clear();
      out_result->raw_payload_bytes.clear();
      out_result->raw_payload_available = false;
      out_result->follow_data = PayloadFollowData{};
      out_result->follow_available = false;
      out_result->text_follow_data = TextFollowData{};
      out_result->text_follow_available = false;
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kFlash;
      out_result->text_status = DecodeContentStatus::kUnavailable;
      return ErrorCode::kNotReady;
    }

    try {
      const FlashSignalProfile normalized_signal_profile =
          NormalizeFormalSignalProfile(config_.flash_signal_profile);
      const FlashVoicingFlavor normalized_flavor =
          NormalizeFormalVoicingFlavor(config_.flash_voicing_flavor);
      const std::vector<int16_t> trimmed_payload_pcm =
          TrimFormalPayloadPcmForFlavor(config_, normalized_flavor,
                                        buffered_pcm_);
      const std::vector<uint8_t> bytes = DecodeFormalPayloadBytes(
          config_, normalized_signal_profile, normalized_flavor,
          trimmed_payload_pcm);
      out_result->raw_payload_bytes = bytes;
      out_result->raw_payload_available = true;
      out_result->follow_data = BuildPayloadFollowData(config_, bytes);
      out_result->follow_available = out_result->follow_data.available;
      out_result->text.clear();
      const ErrorCode text_code = DecodeBytesToText(bytes, &out_result->text);
      out_result->text_status =
          text_code == ErrorCode::kOk
              ? DecodeContentStatus::kOk
              : DecodeContentStatus::kInvalidTextPayload;
      if (text_code != ErrorCode::kOk) {
        out_result->text.clear();
      }
      out_result->text_follow_data =
          text_code == ErrorCode::kOk
              ? BuildTextFollowData(out_result->follow_data, out_result->text)
              : TextFollowData{};
      out_result->text_follow_available =
          out_result->text_follow_data.available;
    } catch (...) {
      out_result->text.clear();
      out_result->raw_payload_bytes.clear();
      out_result->raw_payload_available = false;
      out_result->follow_data = PayloadFollowData{};
      out_result->follow_available = false;
      out_result->text_follow_data = TextFollowData{};
      out_result->text_follow_available = false;
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kFlash;
      out_result->text_status = DecodeContentStatus::kInternalError;
      return ErrorCode::kInternal;
    }

    out_result->complete = true;
    out_result->confidence = 1.0f;
    out_result->mode = TransportMode::kFlash;
    has_pending_result_ = false;
    return ErrorCode::kOk;
  }

  ErrorCode PollTextResult(TextResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }

    DecodeResult decode_result{};
    const ErrorCode code = PollDecodeResult(&decode_result);
    if (code != ErrorCode::kOk) {
      out_result->text.clear();
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kFlash;
      return code;
    }

    out_result->text = decode_result.text;
    out_result->complete = decode_result.complete;
    out_result->confidence = decode_result.confidence;
    out_result->mode = decode_result.mode;
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

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink) {
  return EncodeTextToPcm16WithSignalProfileAndFlavor(
      config, text, config.flash_signal_profile, config.flash_voicing_flavor,
      out_pcm, progress_sink);
}

ErrorCode EncodeTextToPcm16WithSignalProfileAndFlavor(
    const CoreConfig& config, const std::string& text,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::vector<int16_t>* out_pcm) {
  return EncodeTextToPcm16WithSignalProfileAndFlavor(
      config, text, signal_profile, flavor, out_pcm, nullptr);
}

ErrorCode EncodeTextToPcm16WithSignalProfileAndFlavor(
    const CoreConfig& config, const std::string& text,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::vector<int16_t>* out_pcm, const EncodeProgressSink* progress_sink) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (!IsValidFlashSignalProfile(signal_profile)) {
    return ErrorCode::kInvalidArgument;
  }
  if (!IsValidFlashVoicingFlavor(flavor)) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<uint8_t> bytes;
  const ErrorCode bytes_code = EncodeTextToBytes(text, &bytes);
  if (bytes_code != ErrorCode::kOk) {
    return bytes_code;
  }

  try {
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                         0.0f);
    if (ShouldCancelEncode(progress_sink)) {
      return ErrorCode::kCancelled;
    }
    const FlashSignalProfile normalized_signal_profile =
        NormalizeFormalSignalProfile(signal_profile);
    const FlashVoicingFlavor normalized_flavor =
        NormalizeFormalVoicingFlavor(flavor);
    const BfskConfig signal_config =
        MakeBfskConfigForSignalProfile(config, normalized_signal_profile);
    const FlashVoicingConfig formal_voicing =
        MakeFormalVoicingConfigForFlavor(config, normalized_flavor);
    const FlashPayloadLayout payload_layout =
        BuildPayloadLayoutForVoicing(bytes, signal_config, normalized_flavor);
    const FlashEncodeWorkBudget work_budget =
        BuildFlashEncodeWorkBudget(bytes, payload_layout, formal_voicing);
    std::size_t completed_work = work_budget.prepare_text_work;
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                         static_cast<float>(completed_work) /
                             static_cast<float>(work_budget.total_work));
    if (ShouldCancelEncode(progress_sink)) {
      return ErrorCode::kCancelled;
    }
    completed_work += work_budget.prepare_layout_work;
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                         static_cast<float>(completed_work) /
                             static_cast<float>(work_budget.total_work));
    if (ShouldCancelEncode(progress_sink)) {
      return ErrorCode::kCancelled;
    }
    const float render_progress_begin =
        static_cast<float>(completed_work) /
        static_cast<float>(work_budget.total_work);
    const float render_progress_end =
        static_cast<float>(completed_work + work_budget.render_work) /
        static_cast<float>(work_budget.total_work);
    const std::vector<int16_t> clean_payload_pcm =
        EncodePayloadLayoutToPcm16(payload_layout, signal_config,
                                   progress_sink, render_progress_begin,
                                   render_progress_end);
    completed_work += work_budget.render_work;
    const float postprocess_progress_begin =
        static_cast<float>(completed_work) /
        static_cast<float>(work_budget.total_work);
    const float postprocess_progress_end =
        static_cast<float>(completed_work + work_budget.postprocess_work) /
        static_cast<float>(work_budget.total_work);
    const FlashVoicingResult voiced_payload = ApplyVoicingToPayloadWithFlavor(
        clean_payload_pcm, payload_layout, normalized_flavor, formal_voicing,
        progress_sink, postprocess_progress_begin, postprocess_progress_end);
    completed_work += work_budget.postprocess_work;

    if (!IsFormalVoicingCompatible(voiced_payload, clean_payload_pcm.size(),
                                   formal_voicing)) {
      return ErrorCode::kInternal;
    }

    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kFinalizing,
                         static_cast<float>(completed_work) /
                             static_cast<float>(work_budget.total_work));
    if (ShouldCancelEncode(progress_sink)) {
      return ErrorCode::kCancelled;
    }
    *out_pcm = voiced_payload.pcm;
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kFinalizing, 1.0f);
    return ErrorCode::kOk;
  } catch (const EncodeCancelled&) {
    return ErrorCode::kCancelled;
  } catch (...) {
    return ErrorCode::kInvalidArgument;
  }
}

ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<int16_t>& pcm,
                            std::string* out_text) {
  return DecodePcm16ToTextWithSignalProfileAndFlavor(
      config, pcm, config.flash_signal_profile, config.flash_voicing_flavor,
      out_text);
}

ErrorCode DecodePcm16ToTextWithSignalProfileAndFlavor(
    const CoreConfig& config, const std::vector<int16_t>& pcm,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::string* out_text) {
  if (out_text == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (!IsValidFlashSignalProfile(signal_profile)) {
    return ErrorCode::kInvalidArgument;
  }
  if (!IsValidFlashVoicingFlavor(flavor)) {
    return ErrorCode::kInvalidArgument;
  }

  try {
    const FlashSignalProfile normalized_signal_profile =
        NormalizeFormalSignalProfile(signal_profile);
    const FlashVoicingFlavor normalized_flavor =
        NormalizeFormalVoicingFlavor(flavor);
    const std::vector<int16_t> trimmed_payload_pcm =
        TrimFormalPayloadPcmForFlavor(config, normalized_flavor, pcm);
    const std::vector<uint8_t> bytes = DecodeFormalPayloadBytes(
        config, normalized_signal_profile, normalized_flavor,
        trimmed_payload_pcm);
    return DecodeBytesToText(bytes, out_text);
  } catch (...) {
    return ErrorCode::kInvalidArgument;
  }
}

std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config) {
  return std::make_unique<FlashDecoder>(config);
}

}  // namespace bag::flash
