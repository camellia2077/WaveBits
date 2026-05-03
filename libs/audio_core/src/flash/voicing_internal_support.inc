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
constexpr double kLitanyPreambleSeconds = 1.35;
constexpr double kLitanyEpilogueSeconds = 1.15;

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
  kSteadyProtocol = 3,
  kHostileChallenge = 4,
  kCollapseFailure = 5,
  kLitanyInvocation = 6,
  kZealRising = 7,
  kVoidDescent = 8,
};

enum class FixedShellTimbreProfile {
  kInvocationClosure = 0,
  kProtocolBoundary = 1,
  kSteadyBoundary = 2,
  kHostileEdge = 3,
  kCollapseFailure = 4,
  kLitanyInvocation = 5,
  kZealRising = 6,
  kVoidDescent = 7,
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
  double chant_drone_gain = 0.0;
  double chant_swell_depth = 0.0;
  double chant_swell_freq_hz = 0.65;
  double mechanical_throat_gain = 0.0;
  double litany_phrase_dip_depth = 0.0;
  double litany_phrase_tail_begin = 1.0;
  double litany_text_pause_tail_begin = 1.0;
  double steady_low_voice_gain = 0.0;
  double steady_low_voice_freq_hz = 120.0;
  double hostile_edge_gain = 0.0;
  double collapse_tremor_depth = 0.0;
  double collapse_hesitation_depth = 0.0;
  double collapse_hesitation_density = 0.0;
  double collapse_hesitation_tail_begin = 1.0;
  double payload_softclip_drive = 1.0;
  double payload_softclip_mix = 0.0;
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

std::size_t LeadingNonpayloadSamples(const FlashVoicingConfig& config) {
  return config.enable_preamble ? config.preamble_sample_count
                                : static_cast<std::size_t>(0);
}

std::size_t TrailingNonpayloadSamples(const FlashVoicingConfig& config) {
  return config.enable_epilogue ? config.epilogue_sample_count
                                : static_cast<std::size_t>(0);
}

std::size_t SecondsToSampleCount(int sample_rate_hz, double seconds) {
  return sample_rate_hz > 0 && seconds > 0.0
             ? static_cast<std::size_t>(
                   std::lround(static_cast<double>(sample_rate_hz) * seconds))
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

bool IsNibblePhraseTailChunk(size_t chunk_index) {
  return (chunk_index % static_cast<size_t>(4)) == static_cast<size_t>(3);
}

bool IsBytePhraseTailChunk(size_t chunk_index) {
  return (chunk_index % static_cast<size_t>(8)) == static_cast<size_t>(7);
}

bool IsByteTailChunk(const FlashPayloadChunk& chunk) {
  return chunk.bit_index_in_byte == static_cast<std::uint8_t>(7);
}

std::uint8_t SourceByteAt(const FlashPayloadLayout& payload_layout,
                          std::size_t byte_index) {
  const std::size_t expected_chunk_index =
      byte_index * static_cast<std::size_t>(8);
  if (expected_chunk_index < payload_layout.chunks.size() &&
      payload_layout.chunks[expected_chunk_index].byte_index == byte_index) {
    return payload_layout.chunks[expected_chunk_index].source_byte;
  }

  for (const FlashPayloadChunk& chunk : payload_layout.chunks) {
    if (chunk.byte_index == byte_index) {
      return chunk.source_byte;
    }
  }
  return static_cast<std::uint8_t>(0);
}

bool EndsWithUtf8Bytes(const FlashPayloadLayout& payload_layout,
                       std::size_t byte_index, std::uint8_t first,
                       std::uint8_t second, std::uint8_t third) {
  if (byte_index < static_cast<std::size_t>(2)) {
    return false;
  }
  return SourceByteAt(payload_layout, byte_index - static_cast<std::size_t>(2)) ==
             first &&
         SourceByteAt(payload_layout, byte_index - static_cast<std::size_t>(1)) ==
             second &&
         SourceByteAt(payload_layout, byte_index) == third;
}

double ComputeLitanyTextPauseWeight(const FlashPayloadLayout& payload_layout,
                                    std::size_t chunk_index) {
  if (chunk_index >= payload_layout.chunks.size()) {
    return 0.0;
  }
  const FlashPayloadChunk& chunk = payload_layout.chunks[chunk_index];
  if (!IsByteTailChunk(chunk)) {
    return 0.0;
  }

  switch (chunk.source_byte) {
    case static_cast<std::uint8_t>(' '):
    case static_cast<std::uint8_t>('\t'):
      return 0.52;
    case static_cast<std::uint8_t>('\n'):
    case static_cast<std::uint8_t>('\r'):
      return 0.66;
    case static_cast<std::uint8_t>(','):
    case static_cast<std::uint8_t>(';'):
    case static_cast<std::uint8_t>(':'):
      return 0.70;
    case static_cast<std::uint8_t>('.'):
    case static_cast<std::uint8_t>('!'):
    case static_cast<std::uint8_t>('?'):
      return 0.86;
    default:
      break;
  }

  const std::size_t byte_index = chunk.byte_index;
  if (EndsWithUtf8Bytes(payload_layout, byte_index, 0xEF, 0xBC, 0x8C) ||
      EndsWithUtf8Bytes(payload_layout, byte_index, 0xEF, 0xBC, 0x9B) ||
      EndsWithUtf8Bytes(payload_layout, byte_index, 0xEF, 0xBC, 0x9A)) {
    return 0.72;
  }
  if (EndsWithUtf8Bytes(payload_layout, byte_index, 0xE3, 0x80, 0x82) ||
      EndsWithUtf8Bytes(payload_layout, byte_index, 0xEF, 0xBC, 0x81) ||
      EndsWithUtf8Bytes(payload_layout, byte_index, 0xEF, 0xBC, 0x9F)) {
    return 0.88;
  }
  return 0.0;
}

double ComputeTailDipArticulation(double pause_weight, double tail_begin,
                                  double min_articulation,
                                  size_t local_index, size_t sample_count) {
  if (pause_weight <= 0.0 || tail_begin >= 1.0 || sample_count == 0) {
    return 1.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  if (progress <= tail_begin) {
    return 1.0;
  }

  const double local =
      std::clamp((progress - tail_begin) / (1.0 - tail_begin), 0.0, 1.0);
  const double eased = 0.5 - 0.5 * std::cos(kPi * local);
  return std::clamp(1.0 - pause_weight * eased, min_articulation, 1.0);
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

double ComputeLitanyPhraseArticulation(size_t chunk_index, size_t local_index,
                                       size_t sample_count,
                                       const VoicingStyleTraits& style_traits) {
  if (style_traits.litany_phrase_dip_depth <= 0.0 || sample_count == 0 ||
      style_traits.litany_phrase_tail_begin >= 1.0) {
    return 1.0;
  }

  const bool byte_tail = IsBytePhraseTailChunk(chunk_index);
  const bool nibble_tail = IsNibblePhraseTailChunk(chunk_index);
  if (!byte_tail && !nibble_tail) {
    return 1.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  if (progress <= style_traits.litany_phrase_tail_begin) {
    return 1.0;
  }

  const double local = std::clamp(
      (progress - style_traits.litany_phrase_tail_begin) /
          (1.0 - style_traits.litany_phrase_tail_begin),
      0.0, 1.0);
  const double eased = 0.5 - 0.5 * std::cos(kPi * local);
  const double depth = style_traits.litany_phrase_dip_depth *
                       (byte_tail ? 1.0 : 0.66);
  return std::clamp(1.0 - depth * eased, 0.34, 1.0);
}

double ComputeLitanyTextPauseArticulation(
    double pause_weight, size_t local_index, size_t sample_count,
    const VoicingStyleTraits& style_traits) {
  return ComputeTailDipArticulation(pause_weight,
                                    style_traits.litany_text_pause_tail_begin,
                                    0.10, local_index, sample_count);
}

std::uint32_t HashCollapseChunk(const FlashPayloadChunk& chunk,
                                std::size_t chunk_index) {
  std::uint32_t value = 2166136261U;
  value ^= static_cast<std::uint32_t>(chunk_index + 0x9E3779B9U);
  value *= 16777619U;
  value ^= static_cast<std::uint32_t>(chunk.byte_index * 131U);
  value *= 16777619U;
  value ^= static_cast<std::uint32_t>(chunk.source_byte);
  value *= 16777619U;
  value ^= static_cast<std::uint32_t>(chunk.bit_index_in_byte);
  value *= 16777619U;
  return value;
}

double ComputeCollapseHesitationWeight(
    const FlashPayloadChunk& chunk, std::size_t chunk_index,
    const VoicingStyleTraits& style_traits) {
  if (style_traits.collapse_hesitation_depth <= 0.0 ||
      style_traits.collapse_hesitation_density <= 0.0) {
    return 0.0;
  }

  const std::uint32_t hash = HashCollapseChunk(chunk, chunk_index);
  const double select =
      static_cast<double>(hash & 0x3FFU) / static_cast<double>(0x3FFU);
  double density = std::clamp(style_traits.collapse_hesitation_density, 0.0, 1.0);
  if (IsByteTailChunk(chunk)) {
    density = std::min(1.0, density + 0.22);
  }
  if (select > density) {
    return 0.0;
  }

  const double depth_variation = static_cast<double>((hash >> 10U) & 0xFFU) /
                                 static_cast<double>(0xFFU);
  const double byte_tail_boost = IsByteTailChunk(chunk) ? 0.12 : 0.0;
  return std::clamp(style_traits.collapse_hesitation_depth *
                        (0.52 + 0.48 * depth_variation) +
                    byte_tail_boost,
                    0.0, 0.92);
}

double ComputeCollapseHesitationArticulation(
    double hesitation_weight, size_t local_index, size_t sample_count,
    const VoicingStyleTraits& style_traits) {
  return ComputeTailDipArticulation(
      hesitation_weight, style_traits.collapse_hesitation_tail_begin, 0.08,
      local_index, sample_count);
}

VoicingStyleTraits MakeFlavorTraits(FlashVoicingFlavor flavor) {
  switch (NormalizeVoicingFlavor(flavor)) {
    case FlashVoicingFlavor::kLitany:
      return VoicingStyleTraits{
          .nibble_boundary_accent_factor = 0.08,
          .byte_boundary_click_scale = 0.18,
          .preamble_grouped_burst_mix = 0.14,
          .preamble_invocation_shell_mix = 1.18,
          .epilogue_grouped_burst_mix = 0.12,
          .epilogue_closure_shell_mix = 1.14,
          .preamble_rise_ratio = 0.10,
          .preamble_fall_ratio = 0.17,
          .epilogue_rise_ratio = 0.07,
          .epilogue_fall_ratio = 0.20,
          .epilogue_decay_exponent = 3.8,
          .payload_release_scale = 1.16,
          .payload_envelope_floor = 0.16,
          .metallic_layer_gain_scale = 0.00,
          .chant_resonance_gain = 0.010,
          .chant_drone_gain = 0.055,
          .chant_swell_depth = 0.10,
          .chant_swell_freq_hz = 0.65,
          .mechanical_throat_gain = 0.018,
          .litany_phrase_dip_depth = 0.58,
          .litany_phrase_tail_begin = 0.62,
          .litany_text_pause_tail_begin = 0.50,
          .tremolo_depth_scale = 0.07,
          .tremolo_freq_hz = 4.8,
          .spectral_band_mix_scale = 0.12,
          .spectral_softclip_mix_scale = 0.08,
          .preamble_cadence_profile =
              FixedShellCadenceProfile::kLitanyInvocation,
          .epilogue_cadence_profile =
              FixedShellCadenceProfile::kLitanyInvocation,
          .preamble_timbre_profile =
              FixedShellTimbreProfile::kLitanyInvocation,
          .epilogue_timbre_profile =
              FixedShellTimbreProfile::kLitanyInvocation,
          .preamble_phrase_mask_mix = 1.0,
          .epilogue_phrase_mask_mix = 1.0,
          .preamble_terminal_mute_begin = 0.95,
          .preamble_terminal_mute_end = 1.0,
          .epilogue_terminal_mute_begin = 0.93,
          .epilogue_terminal_mute_end = 1.0,
      };
    case FlashVoicingFlavor::kHostile:
      return VoicingStyleTraits{
          .nibble_boundary_accent_factor = 0.34,
          .byte_boundary_click_scale = 1.36,
          .preamble_grouped_burst_mix = 0.90,
          .preamble_invocation_shell_mix = 1.28,
          .epilogue_grouped_burst_mix = 0.82,
          .epilogue_closure_shell_mix = 1.22,
          .preamble_rise_ratio = 0.020,
          .preamble_fall_ratio = 0.14,
          .epilogue_rise_ratio = 0.020,
          .epilogue_fall_ratio = 0.14,
          .epilogue_decay_exponent = 5.2,
          .payload_release_scale = 0.62,
          .payload_envelope_floor = 0.03,
          .metallic_layer_gain_scale = 0.54,
          .hostile_edge_gain = 0.052,
          .payload_softclip_drive = 1.52,
          .payload_softclip_mix = 0.34,
          .tremolo_depth_scale = 0.20,
          .tremolo_freq_hz = 10.4,
          .spectral_band_mix_scale = 0.68,
          .spectral_softclip_mix_scale = 0.60,
          .preamble_cadence_profile =
              FixedShellCadenceProfile::kHostileChallenge,
          .epilogue_cadence_profile =
              FixedShellCadenceProfile::kHostileChallenge,
          .preamble_timbre_profile = FixedShellTimbreProfile::kHostileEdge,
          .epilogue_timbre_profile = FixedShellTimbreProfile::kHostileEdge,
          .preamble_phrase_mask_mix = 1.0,
          .epilogue_phrase_mask_mix = 1.0,
          .preamble_terminal_mute_begin = 0.68,
          .preamble_terminal_mute_end = 1.0,
          .epilogue_terminal_mute_begin = 0.60,
          .epilogue_terminal_mute_end = 1.0,
      };
    case FlashVoicingFlavor::kCollapse:
      return VoicingStyleTraits{
          .nibble_boundary_accent_factor = 0.10,
          .byte_boundary_click_scale = 0.58,
          .preamble_grouped_burst_mix = 0.24,
          .preamble_invocation_shell_mix = 0.92,
          .epilogue_grouped_burst_mix = 0.18,
          .epilogue_closure_shell_mix = 0.84,
          .preamble_rise_ratio = 0.12,
          .preamble_fall_ratio = 0.30,
          .epilogue_rise_ratio = 0.04,
          .epilogue_fall_ratio = 0.36,
          .epilogue_decay_exponent = 2.6,
          .payload_release_scale = 1.38,
          .payload_envelope_floor = 0.08,
          .metallic_layer_gain_scale = 0.08,
          .collapse_tremor_depth = 0.115,
          .collapse_hesitation_depth = 0.78,
          .collapse_hesitation_density = 0.28,
          .collapse_hesitation_tail_begin = 0.56,
          .payload_softclip_drive = 1.12,
          .payload_softclip_mix = 0.12,
          .tremolo_depth_scale = 1.15,
          .tremolo_freq_hz = 11.4,
          .spectral_band_mix_scale = 0.24,
          .spectral_softclip_mix_scale = 0.16,
          .preamble_cadence_profile =
              FixedShellCadenceProfile::kProtocolHandshake,
          .epilogue_cadence_profile =
              FixedShellCadenceProfile::kCollapseFailure,
          .preamble_timbre_profile = FixedShellTimbreProfile::kProtocolBoundary,
          .epilogue_timbre_profile = FixedShellTimbreProfile::kCollapseFailure,
          .preamble_phrase_mask_mix = 1.0,
          .epilogue_phrase_mask_mix = 1.0,
          .preamble_terminal_mute_begin = 0.70,
          .preamble_terminal_mute_end = 1.0,
          .epilogue_terminal_mute_begin = 0.58,
          .epilogue_terminal_mute_end = 1.0,
      };
    case FlashVoicingFlavor::kZeal:
      return VoicingStyleTraits{
          .nibble_boundary_accent_factor = 0.22,
          .byte_boundary_click_scale = 0.92,
          .preamble_grouped_burst_mix = 0.68,
          .preamble_invocation_shell_mix = 1.10,
          .epilogue_grouped_burst_mix = 0.60,
          .epilogue_closure_shell_mix = 1.08,
          .preamble_rise_ratio = 0.030,
          .preamble_fall_ratio = 0.18,
          .epilogue_rise_ratio = 0.030,
          .epilogue_fall_ratio = 0.18,
          .epilogue_decay_exponent = 4.2,
          .payload_release_scale = 0.70,
          .payload_envelope_floor = 0.02,
          .metallic_layer_gain_scale = 0.34,
          .hostile_edge_gain = 0.018,
          .payload_softclip_drive = 1.20,
          .payload_softclip_mix = 0.16,
          .tremolo_depth_scale = 0.16,
          .tremolo_freq_hz = 12.6,
          .spectral_band_mix_scale = 0.46,
          .spectral_softclip_mix_scale = 0.32,
          .preamble_cadence_profile =
              FixedShellCadenceProfile::kZealRising,
          .epilogue_cadence_profile =
              FixedShellCadenceProfile::kZealRising,
          .preamble_timbre_profile = FixedShellTimbreProfile::kZealRising,
          .epilogue_timbre_profile = FixedShellTimbreProfile::kZealRising,
          .preamble_phrase_mask_mix = 1.0,
          .epilogue_phrase_mask_mix = 1.0,
          .preamble_terminal_mute_begin = 0.76,
          .preamble_terminal_mute_end = 1.0,
          .epilogue_terminal_mute_begin = 0.72,
          .epilogue_terminal_mute_end = 1.0,
      };
    case FlashVoicingFlavor::kVoid:
      return VoicingStyleTraits{
          .nibble_boundary_accent_factor = 0.035,
          .byte_boundary_click_scale = 0.12,
          .preamble_grouped_burst_mix = 0.14,
          .preamble_invocation_shell_mix = 0.76,
          .epilogue_grouped_burst_mix = 0.07,
          .epilogue_closure_shell_mix = 1.04,
          .preamble_rise_ratio = 0.18,
          .preamble_fall_ratio = 0.34,
          .epilogue_rise_ratio = 0.04,
          .epilogue_fall_ratio = 0.52,
          .epilogue_decay_exponent = 1.35,
          .payload_release_scale = 1.72,
          .payload_envelope_floor = 0.13,
          .metallic_layer_gain_scale = 0.02,
          .chant_resonance_gain = 0.004,
          .chant_drone_gain = 0.040,
          .chant_swell_depth = 0.08,
          .chant_swell_freq_hz = 0.28,
          .steady_low_voice_gain = 0.032,
          .steady_low_voice_freq_hz = 58.0,
          .payload_softclip_drive = 1.05,
          .payload_softclip_mix = 0.05,
          .tremolo_depth_scale = 0.075,
          .tremolo_freq_hz = 2.4,
          .spectral_band_mix_scale = 0.07,
          .spectral_softclip_mix_scale = 0.05,
          .preamble_cadence_profile =
              FixedShellCadenceProfile::kVoidDescent,
          .epilogue_cadence_profile =
              FixedShellCadenceProfile::kVoidDescent,
          .preamble_timbre_profile = FixedShellTimbreProfile::kVoidDescent,
          .epilogue_timbre_profile = FixedShellTimbreProfile::kVoidDescent,
          .preamble_phrase_mask_mix = 1.0,
          .epilogue_phrase_mask_mix = 1.0,
          .preamble_terminal_mute_begin = 0.86,
          .preamble_terminal_mute_end = 1.0,
          .epilogue_terminal_mute_begin = 0.64,
          .epilogue_terminal_mute_end = 1.0,
      };
    case FlashVoicingFlavor::kSteady:
    default:
      return VoicingStyleTraits{
          .nibble_boundary_accent_factor = 0.16,
          .byte_boundary_click_scale = 0.72,
          .preamble_grouped_burst_mix = 0.46,
          .preamble_invocation_shell_mix = 1.02,
          .epilogue_grouped_burst_mix = 0.44,
          .epilogue_closure_shell_mix = 1.00,
          .preamble_rise_ratio = 0.05,
          .preamble_fall_ratio = 0.24,
          .epilogue_rise_ratio = 0.05,
          .epilogue_fall_ratio = 0.24,
          .epilogue_decay_exponent = 5.4,
          .payload_release_scale = 1.0,
          .payload_envelope_floor = 0.0,
          .metallic_layer_gain_scale = 0.06,
          .chant_resonance_gain = 0.0,
          .steady_low_voice_gain = 0.018,
          .steady_low_voice_freq_hz = 120.0,
          .tremolo_depth_scale = 0.04,
          .tremolo_freq_hz = 6.2,
          .spectral_band_mix_scale = 0.12,
          .spectral_softclip_mix_scale = 0.08,
          .preamble_cadence_profile =
              FixedShellCadenceProfile::kSteadyProtocol,
          .epilogue_cadence_profile =
              FixedShellCadenceProfile::kSteadyProtocol,
          .preamble_timbre_profile = FixedShellTimbreProfile::kSteadyBoundary,
          .epilogue_timbre_profile = FixedShellTimbreProfile::kSteadyBoundary,
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

double ComputeChantDroneLayer(std::size_t sample_index, int sample_rate_hz,
                              double gain) {
  if (gain <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(sample_index) / static_cast<double>(sample_rate_hz);
  const double breath =
      0.68 + 0.32 * std::sin(2.0 * kPi * 0.65 * time + 0.35);
  const double drone =
      0.56 * std::sin(2.0 * kPi * 60.0 * time + 0.12) +
      0.29 * std::sin(2.0 * kPi * 120.0 * time + 0.41) +
      0.15 * std::sin(2.0 * kPi * 180.0 * time + 0.73);
  return gain * breath * drone;
}

double ComputeChantSwellGain(std::size_t sample_index, int sample_rate_hz,
                             const VoicingStyleTraits& style_traits) {
  if (style_traits.chant_swell_depth <= 0.0 ||
      style_traits.chant_swell_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 1.0;
  }

  const double time =
      static_cast<double>(sample_index) / static_cast<double>(sample_rate_hz);
  const double depth = std::clamp(style_traits.chant_swell_depth, 0.0, 0.24);
  const double swell =
      std::sin(2.0 * kPi * style_traits.chant_swell_freq_hz * time + 0.35);
  return std::clamp(1.0 + depth * swell, 1.0 - depth, 1.0 + depth);
}

double ComputeMechanicalThroatLayer(std::size_t sample_index,
                                    int sample_rate_hz, double gain) {
  if (gain <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(sample_index) / static_cast<double>(sample_rate_hz);
  const double throat =
      0.48 * std::sin(2.0 * kPi * 160.0 * time + 0.27) +
      0.34 * std::sin(2.0 * kPi * 420.0 * time + 0.61) +
      0.18 * std::sin(2.0 * kPi * 560.0 * time + 1.04);
  const double motor_buzz =
      0.62 + 0.38 * std::sin(2.0 * kPi * 18.0 * time + 0.44);
  return gain * motor_buzz * throat;
}

double ComputeSteadyLowVoiceLayer(std::size_t sample_index, int sample_rate_hz,
                                  const VoicingStyleTraits& style_traits) {
  if (style_traits.steady_low_voice_gain <= 0.0 ||
      style_traits.steady_low_voice_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(sample_index) / static_cast<double>(sample_rate_hz);
  const double phase =
      2.0 * kPi * style_traits.steady_low_voice_freq_hz * time;
  const double square_like =
      (std::sin(phase) + std::sin(3.0 * phase) / 3.0 +
       std::sin(5.0 * phase) / 5.0) /
      (1.0 + 1.0 / 3.0 + 1.0 / 5.0);
  return style_traits.steady_low_voice_gain * square_like;
}

double ComputeHostileEdgeLayer(double carrier_freq_hz, std::size_t local_index,
                               int sample_rate_hz, double gain) {
  if (gain <= 0.0 || carrier_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double nyquist_hz = static_cast<double>(sample_rate_hz) / 2.0;
  double layer = 0.0;
  const double second_freq_hz = carrier_freq_hz * 2.0;
  const double fifth_freq_hz = carrier_freq_hz * 5.0;
  if (second_freq_hz < nyquist_hz) {
    layer += 0.46 * std::sin(2.0 * kPi * second_freq_hz * time + 0.10);
  }
  if (fifth_freq_hz < nyquist_hz) {
    layer += 0.24 * std::sin(2.0 * kPi * fifth_freq_hz * time + 0.38);
  }
  layer += 0.30 * std::copysign(1.0, std::sin(2.0 * kPi * carrier_freq_hz *
                                               time + 0.18));
  return gain * layer;
}

double ApplyPayloadSoftClip(double sample,
                            const VoicingStyleTraits& style_traits) {
  if (style_traits.payload_softclip_mix <= 0.0 ||
      style_traits.payload_softclip_drive <= 1.0) {
    return sample;
  }

  const double clipped =
      SoftClipSample(sample, style_traits.payload_softclip_drive);
  const double mix = std::clamp(style_traits.payload_softclip_mix, 0.0, 1.0);
  return sample * (1.0 - mix) + clipped * mix;
}

double ComputeCollapseTremor(std::size_t payload_sample_index,
                             int sample_rate_hz,
                             const VoicingStyleTraits& style_traits) {
  if (style_traits.collapse_tremor_depth <= 0.0 || sample_rate_hz <= 0) {
    return 1.0;
  }

  const double time = static_cast<double>(payload_sample_index) /
                      static_cast<double>(sample_rate_hz);
  const double primary = std::sin(2.0 * kPi * 8.6 * time + 0.33);
  const double secondary = std::sin(2.0 * kPi * 13.7 * time + 1.10);
  const double tremor =
      style_traits.collapse_tremor_depth * (0.72 * primary + 0.28 * secondary);
  return std::clamp(1.0 + tremor, 0.72, 1.18);
}
