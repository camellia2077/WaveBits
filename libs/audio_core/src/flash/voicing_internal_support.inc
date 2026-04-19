constexpr double kPi = 3.14159265358979323846;
constexpr double kPcmScale = 32767.0;
constexpr double kMaxAttackReleaseRatio = 0.18;
constexpr double kMaxAttackReleaseCombinedRatio = 0.30;
constexpr double kMaxSecondHarmonicGain = 0.12;
constexpr double kMaxThirdHarmonicGain = 0.08;
constexpr double kMaxBoundaryClickGain = 0.05;
constexpr double kMaxAdditiveGainBudget = 0.18;
constexpr std::size_t kRhythmBeatCount = 16;
constexpr std::size_t kRhythmGroupSize = 4;
constexpr double kNibbleBoundaryAccentFactor = 0.24;
constexpr double kSpectralBandMix = 0.14;
constexpr double kSpectralSoftClipMix = 0.10;
constexpr double kSpectralSoftClipDrive = 1.18;
constexpr double kMetallicLayerGain = 0.022;
constexpr double kTremoloDepth = 0.045;
constexpr double kTremoloFreqHz = 7.5;
constexpr double kInvocationSweepGain = 0.14;
constexpr double kInvocationPulseGain = 0.18;
constexpr double kClosurePulseGain = 0.20;
constexpr double kClosureTailGain = 0.11;

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

struct SanitizedVoicingConfig {
  int sample_rate_hz = 0;
  double attack_ratio = 0.0;
  double release_ratio = 0.0;
  double second_harmonic_gain = 0.0;
  double third_harmonic_gain = 0.0;
  double boundary_click_gain = 0.0;
};

enum class FixedShellCadenceProfile {
  kProtocolHandshake = 0,
  kRitualChant = 1,
};

enum class FixedShellTimbreProfile {
  kInvocationClosure = 0,
  kProtocolBoundary = 1,
};

struct CadenceWindow {
  double begin = 0.0;
  double end = 0.0;
};

struct VoicingStyleTraits {
  double nibble_boundary_accent_factor = 0.0;
  double byte_boundary_click_scale = 1.0;
  double preamble_grouped_burst_mix = 1.0;
  double preamble_invocation_shell_mix = 1.0;
  double epilogue_grouped_burst_mix = 1.0;
  double epilogue_closure_shell_mix = 1.0;
  double preamble_rise_ratio = 0.12;
  double preamble_fall_ratio = 0.18;
  double epilogue_rise_ratio = 0.08;
  double epilogue_fall_ratio = 0.26;
  double epilogue_decay_exponent = 2.0;
  double payload_release_scale = 1.0;
  double payload_envelope_floor = 0.0;
  double metallic_layer_gain_scale = 1.0;
  double chant_resonance_gain = 0.0;
  double tremolo_depth_scale = 1.0;
  double tremolo_freq_hz = kTremoloFreqHz;
  double spectral_band_mix_scale = 1.0;
  double spectral_softclip_mix_scale = 1.0;
  FixedShellCadenceProfile preamble_cadence_profile =
      FixedShellCadenceProfile::kProtocolHandshake;
  FixedShellCadenceProfile epilogue_cadence_profile =
      FixedShellCadenceProfile::kProtocolHandshake;
  FixedShellTimbreProfile preamble_timbre_profile =
      FixedShellTimbreProfile::kInvocationClosure;
  FixedShellTimbreProfile epilogue_timbre_profile =
      FixedShellTimbreProfile::kInvocationClosure;
  double preamble_phrase_mask_mix = 0.0;
  double epilogue_phrase_mask_mix = 0.0;
  double preamble_terminal_mute_begin = 1.0;
  double preamble_terminal_mute_end = 1.0;
  double epilogue_terminal_mute_begin = 1.0;
  double epilogue_terminal_mute_end = 1.0;
};

FlashVoicingFlavor NormalizeVoicingFlavor(FlashVoicingFlavor flavor) {
  switch (flavor) {
    case FlashVoicingFlavor::kCodedBurst:
    case FlashVoicingFlavor::kRitualChant:
      return flavor;
    default:
      return FlashVoicingFlavor::kCodedBurst;
  }
}

std::size_t LeadingNonpayloadSamples(const FlashVoicingConfig& config) {
  return config.enable_preamble ? config.preamble_sample_count
                                : static_cast<std::size_t>(0);
}

std::size_t TrailingNonpayloadSamples(const FlashVoicingConfig& config) {
  return config.enable_epilogue ? config.epilogue_sample_count
                                : static_cast<std::size_t>(0);
}

void ValidatePayloadLayout(const std::vector<int16_t>& clean_payload_pcm,
                           const FlashPayloadLayout& payload_layout) {
  if (payload_layout.payload_sample_count != clean_payload_pcm.size()) {
    throw std::invalid_argument("Payload layout sample count mismatch.");
  }

  size_t expected_offset = 0;
  for (const FlashPayloadChunk& chunk : payload_layout.chunks) {
    if (chunk.sample_offset != expected_offset) {
      throw std::invalid_argument("Payload layout offsets must be contiguous.");
    }
    if (chunk.sample_offset + chunk.sample_count >
        payload_layout.payload_sample_count) {
      throw std::invalid_argument("Payload chunk exceeds payload bounds.");
    }
    expected_offset += chunk.sample_count;
  }

  if (expected_offset != payload_layout.payload_sample_count) {
    throw std::invalid_argument("Payload layout must cover the full payload.");
  }
}

SanitizedVoicingConfig SanitizeVoicingConfig(const FlashVoicingConfig& config) {
  SanitizedVoicingConfig sanitized{};
  sanitized.sample_rate_hz = std::max(config.sample_rate_hz, 0);
  sanitized.attack_ratio =
      std::clamp(config.attack_ratio, 0.0, kMaxAttackReleaseRatio);
  sanitized.release_ratio =
      std::clamp(config.release_ratio, 0.0, kMaxAttackReleaseRatio);

  const double combined_attack_release =
      sanitized.attack_ratio + sanitized.release_ratio;
  if (combined_attack_release > kMaxAttackReleaseCombinedRatio &&
      combined_attack_release > 0.0) {
    const double scale =
        kMaxAttackReleaseCombinedRatio / combined_attack_release;
    sanitized.attack_ratio *= scale;
    sanitized.release_ratio *= scale;
  }

  sanitized.second_harmonic_gain =
      std::clamp(config.second_harmonic_gain, 0.0, kMaxSecondHarmonicGain);
  sanitized.third_harmonic_gain =
      std::clamp(config.third_harmonic_gain, 0.0, kMaxThirdHarmonicGain);
  sanitized.boundary_click_gain =
      std::clamp(config.boundary_click_gain, 0.0, kMaxBoundaryClickGain);

  const double additive_gain_sum = sanitized.second_harmonic_gain +
                                   sanitized.third_harmonic_gain +
                                   sanitized.boundary_click_gain;
  if (additive_gain_sum > kMaxAdditiveGainBudget && additive_gain_sum > 0.0) {
    const double scale = kMaxAdditiveGainBudget / additive_gain_sum;
    sanitized.second_harmonic_gain *= scale;
    sanitized.third_harmonic_gain *= scale;
    sanitized.boundary_click_gain *= scale;
  }

  return sanitized;
}

size_t RatioToCount(double ratio, size_t sample_count) {
  if (ratio <= 0.0 || sample_count == 0) {
    return 0;
  }

  const size_t count = static_cast<size_t>(
      std::round(static_cast<double>(sample_count) * ratio));
  return std::clamp(count, static_cast<size_t>(1), sample_count);
}

std::pair<size_t, size_t> FitAttackReleaseCounts(size_t sample_count,
                                                 size_t attack_count,
                                                 size_t release_count) {
  if (sample_count == 0) {
    return {0, 0};
  }
  if (attack_count + release_count <= sample_count) {
    return {attack_count, release_count};
  }

  const double total = static_cast<double>(attack_count + release_count);
  if (total <= 0.0) {
    return {0, 0};
  }

  size_t fitted_attack = static_cast<size_t>(
      std::floor(static_cast<double>(sample_count) *
                 static_cast<double>(attack_count) / total));
  fitted_attack = std::min(fitted_attack, sample_count);
  const size_t fitted_release = sample_count - fitted_attack;
  return {fitted_attack, fitted_release};
}

double ComputeEnvelope(size_t local_index, size_t sample_count,
                       size_t attack_count, size_t release_count) {
  if (sample_count == 0) {
    return 0.0;
  }

  double envelope = 1.0;
  if (attack_count > 0 && local_index < attack_count) {
    envelope = std::min(envelope, static_cast<double>(local_index + 1) /
                                      static_cast<double>(attack_count));
  }

  if (release_count > 0) {
    const size_t release_start =
        sample_count > release_count ? sample_count - release_count : 0;
    if (local_index >= release_start) {
      const size_t remaining = sample_count - local_index;
      envelope = std::min(envelope, static_cast<double>(remaining) /
                                        static_cast<double>(release_count));
    }
  }

  return std::clamp(envelope, 0.0, 1.0);
}

double ComputeHarmonicSample(double gain, int harmonic_multiple,
                             double carrier_freq_hz, size_t local_index,
                             int sample_rate_hz) {
  if (gain <= 0.0 || harmonic_multiple <= 1 || carrier_freq_hz <= 0.0 ||
      sample_rate_hz <= 0) {
    return 0.0;
  }

  const double harmonic_freq_hz =
      carrier_freq_hz * static_cast<double>(harmonic_multiple);
  const double nyquist_hz = static_cast<double>(sample_rate_hz) / 2.0;
  if (harmonic_freq_hz >= nyquist_hz) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  return gain * std::sin(2.0 * kPi * harmonic_freq_hz * time);
}

bool IsByteBoundaryChunk(size_t chunk_index) {
  return chunk_index > 0 && (chunk_index % static_cast<size_t>(8)) == 0;
}

bool IsNibbleBoundaryChunk(size_t chunk_index) {
  return chunk_index > 0 && (chunk_index % static_cast<size_t>(4)) == 0 &&
         !IsByteBoundaryChunk(chunk_index);
}

size_t ComputeClickWindow(size_t chunk_sample_count) {
  if (chunk_sample_count == 0) {
    return 0;
  }
  return std::clamp(chunk_sample_count / static_cast<size_t>(96),
                    static_cast<size_t>(6), static_cast<size_t>(24));
}

size_t ComputeSubsegmentIndex(size_t local_index, size_t sample_count,
                              size_t part_count) {
  if (sample_count == 0 || part_count == 0) {
    return 0;
  }
  return std::min((local_index * part_count) / sample_count,
                  part_count - static_cast<size_t>(1));
}

double ComputeSubsegmentProgress(size_t local_index, size_t sample_count,
                                 size_t part_index, size_t part_count) {
  if (sample_count == 0 || part_count == 0) {
    return 0.0;
  }

  const size_t begin = (sample_count * part_index) / part_count;
  const size_t end = std::max(
      begin + static_cast<size_t>(1),
      (sample_count * (part_index + static_cast<size_t>(1))) / part_count);
  const size_t span = std::max(end - begin, static_cast<size_t>(1));
  const size_t clamped_index =
      std::min(local_index, end - static_cast<size_t>(1));
  return std::clamp(
      static_cast<double>(clamped_index - begin) / static_cast<double>(span),
      0.0, 1.0);
}

double ComputeBurstWindow(double progress, double begin, double end) {
  if (progress <= begin || progress >= end || end <= begin) {
    return 0.0;
  }

  const double local = (progress - begin) / (end - begin);
  return std::sin(kPi * local);
}

double ComputeGroupedBurstPattern(size_t local_index, size_t sample_count) {
  if (sample_count == 0) {
    return 0.0;
  }

  constexpr double kGroupGains[4] = {1.00, 0.82, 0.94, 0.76};
  constexpr double kBeatGains[4] = {1.00, 0.12, 0.78, 0.10};

  const size_t beat_index =
      ComputeSubsegmentIndex(local_index, sample_count, kRhythmBeatCount);
  const size_t group_index =
      std::min(beat_index / kRhythmGroupSize, static_cast<size_t>(3));
  const size_t beat_in_group = beat_index % kRhythmGroupSize;
  const double beat_progress = ComputeSubsegmentProgress(
      local_index, sample_count, beat_index, kRhythmBeatCount);
  const double primary_burst = ComputeBurstWindow(beat_progress, 0.03, 0.31);
  const double secondary_burst =
      0.56 * ComputeBurstWindow(beat_progress, 0.49, 0.71);
  const double beat_gain = kGroupGains[group_index] * kBeatGains[beat_in_group];
  return std::clamp(beat_gain * (primary_burst + secondary_burst), 0.0, 1.0);
}

VoicingStyleTraits MakeFlavorTraits(FlashVoicingFlavor flavor) {
  switch (NormalizeVoicingFlavor(flavor)) {
    case FlashVoicingFlavor::kRitualChant:
      return VoicingStyleTraits{
          .nibble_boundary_accent_factor = 0.12,
          .byte_boundary_click_scale = 0.68,
          .preamble_grouped_burst_mix = 0.26,
          .preamble_invocation_shell_mix = 1.12,
          .epilogue_grouped_burst_mix = 0.22,
          .epilogue_closure_shell_mix = 1.08,
          .preamble_rise_ratio = 0.09,
          .preamble_fall_ratio = 0.15,
          .epilogue_rise_ratio = 0.06,
          .epilogue_fall_ratio = 0.18,
          .epilogue_decay_exponent = 3.4,
          .payload_release_scale = 1.10,
          .payload_envelope_floor = 0.12,
          .metallic_layer_gain_scale = 0.18,
          .chant_resonance_gain = 0.008,
          .tremolo_depth_scale = 0.15,
          .tremolo_freq_hz = 5.4,
          .spectral_band_mix_scale = 0.24,
          .spectral_softclip_mix_scale = 0.20,
          .preamble_cadence_profile = FixedShellCadenceProfile::kRitualChant,
          .epilogue_cadence_profile = FixedShellCadenceProfile::kRitualChant,
          .preamble_phrase_mask_mix = 1.0,
          .epilogue_phrase_mask_mix = 1.0,
          .preamble_terminal_mute_begin = 0.88,
          .preamble_terminal_mute_end = 1.0,
          .epilogue_terminal_mute_begin = 0.84,
          .epilogue_terminal_mute_end = 1.0,
      };
    case FlashVoicingFlavor::kCodedBurst:
    default:
      return VoicingStyleTraits{
          .nibble_boundary_accent_factor = kNibbleBoundaryAccentFactor,
          .byte_boundary_click_scale = 1.0,
          .preamble_grouped_burst_mix = 0.54,
          .preamble_invocation_shell_mix = 1.10,
          .epilogue_grouped_burst_mix = 0.50,
          .epilogue_closure_shell_mix = 1.08,
          .preamble_rise_ratio = 0.05,
          .preamble_fall_ratio = 0.24,
          .epilogue_rise_ratio = 0.05,
          .epilogue_fall_ratio = 0.24,
          .epilogue_decay_exponent = 5.4,
          .payload_release_scale = 1.0,
          .payload_envelope_floor = 0.0,
          .metallic_layer_gain_scale = 1.0,
          .chant_resonance_gain = 0.0,
          .tremolo_depth_scale = 1.0,
          .tremolo_freq_hz = kTremoloFreqHz,
          .spectral_band_mix_scale = 1.0,
          .spectral_softclip_mix_scale = 1.0,
          .preamble_cadence_profile =
              FixedShellCadenceProfile::kProtocolHandshake,
          .epilogue_cadence_profile =
              FixedShellCadenceProfile::kProtocolHandshake,
          .preamble_timbre_profile = FixedShellTimbreProfile::kProtocolBoundary,
          .epilogue_timbre_profile = FixedShellTimbreProfile::kProtocolBoundary,
          .preamble_phrase_mask_mix = 1.0,
          .epilogue_phrase_mask_mix = 1.0,
          .preamble_terminal_mute_begin = 0.76,
          .preamble_terminal_mute_end = 1.0,
          .epilogue_terminal_mute_begin = 0.72,
          .epilogue_terminal_mute_end = 1.0,
      };
  }
}

bool ShouldApplySpectralTexture(const FlashVoicingConfig& config) {
  return config.attack_ratio > 0.0 || config.release_ratio > 0.0 ||
         config.second_harmonic_gain > 0.0 || config.third_harmonic_gain > 0.0;
}

bool ShouldApplyLayeredTimbre(const FlashVoicingConfig& config) {
  return config.second_harmonic_gain > 0.0 ||
         config.third_harmonic_gain > 0.0 || config.attack_ratio > 0.0 ||
         config.release_ratio > 0.0;
}

double ComputeOnePoleAlpha(double cutoff_hz, int sample_rate_hz) {
  if (cutoff_hz <= 0.0 || sample_rate_hz <= 0) {
    return 1.0;
  }

  const double exponent =
      -2.0 * kPi * cutoff_hz / static_cast<double>(sample_rate_hz);
  return std::clamp(1.0 - std::exp(exponent), 0.0, 1.0);
}

double SoftClipSample(double sample, double drive) {
  if (drive <= 0.0) {
    return sample;
  }

  const double scaled = sample * drive;
  const double normalization = std::tanh(drive);
  if (normalization <= 0.0) {
    return sample;
  }
  return std::tanh(scaled) / normalization;
}

double ComputeSecondaryMetallicLayer(double carrier_freq_hz, size_t local_index,
                                     int sample_rate_hz) {
  if (carrier_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double primary_freq_hz = carrier_freq_hz * 4.5;
  const double secondary_freq_hz = carrier_freq_hz * 6.25;
  const double nyquist_hz = static_cast<double>(sample_rate_hz) / 2.0;

  double layer = 0.0;
  if (primary_freq_hz < nyquist_hz) {
    layer += 0.68 * std::sin(2.0 * kPi * primary_freq_hz * time);
  }
  if (secondary_freq_hz < nyquist_hz) {
    layer += 0.32 * std::sin(2.0 * kPi * secondary_freq_hz * time + 0.35);
  }
  return kMetallicLayerGain * layer;
}

double ComputeChantResonanceLayer(double carrier_freq_hz,
                                  size_t payload_sample_index,
                                  int sample_rate_hz, double gain) {
  if (gain <= 0.0 || carrier_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time = static_cast<double>(payload_sample_index) /
                      static_cast<double>(sample_rate_hz);
  const double lower_freq_hz = carrier_freq_hz * 0.75;
  const double upper_freq_hz = carrier_freq_hz * 1.50;
  const double nyquist_hz = static_cast<double>(sample_rate_hz) / 2.0;

  double layer = 0.0;
  if (lower_freq_hz > 0.0 && lower_freq_hz < nyquist_hz) {
    layer += 0.62 * std::sin(2.0 * kPi * lower_freq_hz * time + 0.18);
  }
  if (upper_freq_hz < nyquist_hz) {
    layer += 0.38 * std::sin(2.0 * kPi * upper_freq_hz * time + 0.52);
  }
  return gain * layer;
}
