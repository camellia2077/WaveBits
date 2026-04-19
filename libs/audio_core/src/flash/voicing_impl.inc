namespace bag::flash {

using std::int16_t;
using std::size_t;

namespace {
#include "voicing_internal_support.inc"
#include "voicing_internal_texture.inc"
#include "voicing_internal_shell.inc"

}  // namespace

FlashVoicingConfig MakeFormalVoicingConfigForFlavor(const CoreConfig& config,
                                                    FlashVoicingFlavor flavor) {
  FlashVoicingConfig voicing{};
  voicing.sample_rate_hz = config.sample_rate_hz;
  switch (NormalizeVoicingFlavor(flavor)) {
    case FlashVoicingFlavor::kRitualChant:
      voicing.attack_ratio = 0.05;
      voicing.release_ratio = 0.05;
      voicing.second_harmonic_gain = 0.05;
      voicing.third_harmonic_gain = 0.01;
      voicing.boundary_click_gain = 0.015;
      voicing.enable_preamble = true;
      voicing.enable_epilogue = true;
      voicing.preamble_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(16)
              : static_cast<std::size_t>(0);
      voicing.epilogue_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(8)
              : static_cast<std::size_t>(0);
      break;
    case FlashVoicingFlavor::kCodedBurst:
    default:
      voicing.attack_ratio = 0.08;
      voicing.release_ratio = 0.08;
      voicing.second_harmonic_gain = 0.10;
      voicing.third_harmonic_gain = 0.03;
      voicing.boundary_click_gain = 0.02;
      voicing.enable_preamble = true;
      voicing.enable_epilogue = true;
      voicing.preamble_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      voicing.epilogue_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      break;
  }
  return voicing;
}

FlashVoicingDescriptor DescribeVoicingOutput(std::size_t total_sample_count,
                                             const FlashVoicingConfig& config) {
  const std::size_t leading_nonpayload_samples =
      LeadingNonpayloadSamples(config);
  const std::size_t trailing_nonpayload_samples =
      TrailingNonpayloadSamples(config);
  if (leading_nonpayload_samples + trailing_nonpayload_samples >
      total_sample_count) {
    throw std::invalid_argument("Voicing trim exceeds total sample count.");
  }

  FlashVoicingDescriptor descriptor{};
  descriptor.leading_nonpayload_samples = leading_nonpayload_samples;
  descriptor.trailing_nonpayload_samples = trailing_nonpayload_samples;
  descriptor.payload_sample_count = total_sample_count -
                                    leading_nonpayload_samples -
                                    trailing_nonpayload_samples;
  return descriptor;
}

std::vector<int16_t> TrimToPayloadPcm(
    const std::vector<int16_t>& voiced_pcm,
    const FlashVoicingDescriptor& descriptor) {
  const std::size_t described_total_sample_count =
      descriptor.leading_nonpayload_samples + descriptor.payload_sample_count +
      descriptor.trailing_nonpayload_samples;
  if (described_total_sample_count != voiced_pcm.size()) {
    throw std::invalid_argument(
        "Voicing descriptor does not match PCM sample count.");
  }

  const std::size_t payload_begin = descriptor.leading_nonpayload_samples;
  const std::size_t payload_end =
      payload_begin + descriptor.payload_sample_count;
  return std::vector<int16_t>(
      voiced_pcm.begin() + static_cast<std::ptrdiff_t>(payload_begin),
      voiced_pcm.begin() + static_cast<std::ptrdiff_t>(payload_end));
}

FlashVoicingResult ApplyVoicingToPayload(
    const std::vector<int16_t>& clean_payload_pcm,
    const FlashPayloadLayout& payload_layout, const FlashVoicingConfig& config,
    const EncodeProgressSink* progress_sink, float progress_begin,
    float progress_end) {
  return ApplyVoicingToPayloadWithFlavor(
      clean_payload_pcm, payload_layout, FlashVoicingFlavor::kCodedBurst,
      config, progress_sink, progress_begin, progress_end);
}

FlashVoicingResult ApplyVoicingToPayloadWithFlavor(
    const std::vector<int16_t>& clean_payload_pcm,
    const FlashPayloadLayout& payload_layout, FlashVoicingFlavor flavor,
    const FlashVoicingConfig& config, const EncodeProgressSink* progress_sink,
    float progress_begin, float progress_end) {
  ValidatePayloadLayout(clean_payload_pcm, payload_layout);
  const SanitizedVoicingConfig sanitized = SanitizeVoicingConfig(config);
  const VoicingStyleTraits style_traits = MakeFlavorTraits(flavor);
  const bool use_layered_timbre = ShouldApplyLayeredTimbre(config);
  const FlashVoicingDescriptor descriptor = DescribeVoicingOutput(
      clean_payload_pcm.size() + LeadingNonpayloadSamples(config) +
          TrailingNonpayloadSamples(config),
      config);

  FlashVoicingResult result{};
  result.pcm.resize(descriptor.leading_nonpayload_samples +
                        descriptor.payload_sample_count +
                        descriptor.trailing_nonpayload_samples,
                    0);

  const std::size_t spectral_texture_work = ShouldApplySpectralTexture(config)
                                                ? result.pcm.size()
                                                : static_cast<std::size_t>(0);
  const std::size_t total_postprocess_work =
      descriptor.leading_nonpayload_samples + descriptor.payload_sample_count +
      descriptor.trailing_nonpayload_samples + spectral_texture_work;

  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPostprocessing,
                       progress_begin);
  ThrowIfEncodeCancelled(progress_sink);
  if (total_postprocess_work == 0) {
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPostprocessing,
                         progress_end);
    result.descriptor = descriptor;
    return result;
  }

  std::size_t completed_postprocess_work = 0;
  std::size_t last_reported_postprocess_work = 0;
  for (size_t local_index = 0;
       local_index < descriptor.leading_nonpayload_samples; ++local_index) {
    if (local_index > 0 && local_index % kProgressReportStrideSamples == 0) {
      ThrowIfEncodeCancelled(progress_sink);
    }
    const double sample = RenderFixedPreambleSample(
        local_index, descriptor.leading_nonpayload_samples, style_traits,
        sanitized.sample_rate_hz);
    result.pcm[local_index] =
        static_cast<int16_t>(std::lround(sample * kPcmScale));
    ++completed_postprocess_work;
    MaybeReportCompletedSampleProgress(
        progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin,
        progress_end, completed_postprocess_work, total_postprocess_work,
        &last_reported_postprocess_work);
  }
  MaybeReportCompletedSampleProgress(
      progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin,
      progress_end, completed_postprocess_work, total_postprocess_work,
      &last_reported_postprocess_work, true);

  for (size_t chunk_index = 0; chunk_index < payload_layout.chunks.size();
       ++chunk_index) {
    ThrowIfEncodeCancelled(progress_sink);
    const FlashPayloadChunk& chunk = payload_layout.chunks[chunk_index];
    auto [attack_count, release_count] = FitAttackReleaseCounts(
        chunk.sample_count,
        RatioToCount(sanitized.attack_ratio, chunk.sample_count),
        RatioToCount(
            sanitized.release_ratio * style_traits.payload_release_scale,
            chunk.sample_count));
    const size_t click_window = ComputeClickWindow(chunk.sample_count);

    for (size_t local_index = 0; local_index < chunk.sample_count;
         ++local_index) {
      const size_t payload_sample_index = chunk.sample_offset + local_index;
      const size_t sample_index =
          descriptor.leading_nonpayload_samples + payload_sample_index;
      const double clean_sample =
          static_cast<double>(clean_payload_pcm[payload_sample_index]) /
          kPcmScale;
      const double base_envelope = ComputeEnvelope(
          local_index, chunk.sample_count, attack_count, release_count);
      const double envelope =
          base_envelope +
          style_traits.payload_envelope_floor * (1.0 - base_envelope);
      const double harmonic_mix =
          envelope * ComputeHarmonicSample(sanitized.second_harmonic_gain, 2,
                                           chunk.carrier_freq_hz, local_index,
                                           sanitized.sample_rate_hz) +
          envelope * ComputeHarmonicSample(sanitized.third_harmonic_gain, 3,
                                           chunk.carrier_freq_hz, local_index,
                                           sanitized.sample_rate_hz);
      const double metallic_layer =
          use_layered_timbre ? envelope *
                                   ComputeSecondaryMetallicLayer(
                                       chunk.carrier_freq_hz, local_index,
                                       sanitized.sample_rate_hz) *
                                   style_traits.metallic_layer_gain_scale
                             : 0.0;
      const double chant_resonance =
          use_layered_timbre
              ? envelope * ComputeChantResonanceLayer(
                               chunk.carrier_freq_hz, payload_sample_index,
                               sanitized.sample_rate_hz,
                               style_traits.chant_resonance_gain)
              : 0.0;
      const double boundary_click =
          ComputeBoundaryClick(sanitized.boundary_click_gain, style_traits,
                               chunk_index, local_index, click_window);
      const double tremolo =
          use_layered_timbre
              ? ComputePayloadTremolo(payload_sample_index,
                                      sanitized.sample_rate_hz, style_traits)
              : 1.0;

      const double voiced_sample =
          std::clamp((clean_sample * envelope + harmonic_mix + metallic_layer +
                      chant_resonance) *
                             tremolo +
                         boundary_click,
                     -1.0, 1.0);
      result.pcm[sample_index] =
          static_cast<int16_t>(std::lround(voiced_sample * kPcmScale));
      ++completed_postprocess_work;
      MaybeReportCompletedSampleProgress(
          progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin,
          progress_end, completed_postprocess_work, total_postprocess_work,
          &last_reported_postprocess_work);
    }
    MaybeReportCompletedSampleProgress(
        progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin,
        progress_end, completed_postprocess_work, total_postprocess_work,
        &last_reported_postprocess_work, true);
  }

  for (size_t local_index = 0;
       local_index < descriptor.trailing_nonpayload_samples; ++local_index) {
    if (local_index > 0 && local_index % kProgressReportStrideSamples == 0) {
      ThrowIfEncodeCancelled(progress_sink);
    }
    const double sample = RenderFixedEpilogueSample(
        local_index, descriptor.trailing_nonpayload_samples, style_traits,
        sanitized.sample_rate_hz);
    const size_t sample_index = descriptor.leading_nonpayload_samples +
                                descriptor.payload_sample_count + local_index;
    result.pcm[sample_index] =
        static_cast<int16_t>(std::lround(sample * kPcmScale));
    ++completed_postprocess_work;
    MaybeReportCompletedSampleProgress(
        progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin,
        progress_end, completed_postprocess_work, total_postprocess_work,
        &last_reported_postprocess_work);
  }
  MaybeReportCompletedSampleProgress(
      progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin,
      progress_end, completed_postprocess_work, total_postprocess_work,
      &last_reported_postprocess_work, true);

  result.descriptor = descriptor;
  ThrowIfEncodeCancelled(progress_sink);
  if (ShouldApplySpectralTexture(config)) {
    ApplySpectralTexture(&result.pcm, sanitized.sample_rate_hz, style_traits,
                         progress_sink, progress_begin, progress_end,
                         completed_postprocess_work, total_postprocess_work);
    completed_postprocess_work += spectral_texture_work;
  }
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPostprocessing,
                       progress_end);
  return result;
}

}  // namespace bag::flash
