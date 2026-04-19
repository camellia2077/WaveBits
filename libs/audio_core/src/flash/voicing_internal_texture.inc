double ComputePayloadTremolo(std::size_t payload_sample_index,
                             int sample_rate_hz,
                             const VoicingStyleTraits& style_traits) {
  if (sample_rate_hz <= 0) {
    return 1.0;
  }

  const double time = static_cast<double>(payload_sample_index) /
                      static_cast<double>(sample_rate_hz);
  return 1.0 + (kTremoloDepth * style_traits.tremolo_depth_scale) *
                   std::sin(2.0 * kPi * style_traits.tremolo_freq_hz * time);
}

double ComputeInvocationSweep(size_t local_index, size_t sample_count,
                              int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double begin_hz = 320.0;
  const double end_hz = 1180.0;
  const double sweep_freq_hz =
      begin_hz + (end_hz - begin_hz) * std::pow(progress, 0.72);
  const double envelope = ComputeBurstWindow(progress, 0.04, 0.58);
  return kInvocationSweepGain * std::sin(2.0 * kPi * sweep_freq_hz * time) *
         envelope;
}

double ComputeInvocationPulse(size_t local_index, size_t sample_count,
                              int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double window = ComputeBurstWindow(progress, 0.62, 0.96);
  if (window <= 0.0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double pulse = 0.74 * std::sin(2.0 * kPi * 680.0 * time) +
                       0.26 * std::sin(2.0 * kPi * 1360.0 * time + 0.22);
  return kInvocationPulseGain * pulse * window;
}

double ComputeClosurePulse(size_t local_index, size_t sample_count,
                           int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double window = ComputeBurstWindow(progress, 0.12, 0.54);
  if (window <= 0.0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double pulse = 0.68 * std::sin(2.0 * kPi * 540.0 * time) +
                       0.32 * std::sin(2.0 * kPi * 900.0 * time + 0.44);
  return kClosurePulseGain * pulse * window;
}

double ComputeClosureTail(size_t local_index, size_t sample_count,
                          int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double window = ComputeBurstWindow(progress, 0.54, 0.98);
  if (window <= 0.0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double decay = std::pow(std::max(0.0, 1.0 - progress), 1.6);
  const double tail = 0.72 * std::sin(2.0 * kPi * 280.0 * time) +
                      0.28 * std::sin(2.0 * kPi * 560.0 * time + 0.16);
  return kClosureTailGain * tail * window * decay;
}

double ComputeProtocolLeadSweep(size_t local_index, size_t sample_count,
                                int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double window = ComputeBurstWindow(progress, 0.04, 0.74);
  if (window <= 0.0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double chirp_progress = std::clamp((progress - 0.04) / 0.70, 0.0, 1.0);
  const double sweep_freq_hz =
      760.0 + (1180.0 - 760.0) * std::pow(chirp_progress, 0.58);
  const double sweep =
      0.78 * std::sin(2.0 * kPi * sweep_freq_hz * time) +
      0.22 * std::sin(2.0 * kPi * sweep_freq_hz * 2.0 * time + 0.18);
  return 0.18 * sweep * window;
}

double ComputeProtocolLeadLockPulse(size_t local_index, size_t sample_count,
                                    int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double window = std::max(ComputeBurstWindow(progress, 0.06, 0.22),
                                 ComputeBurstWindow(progress, 0.56, 0.74));
  if (window <= 0.0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double pulse = 0.62 * std::sin(2.0 * kPi * 680.0 * time) +
                       0.26 * std::sin(2.0 * kPi * 1360.0 * time + 0.20) +
                       0.12 * std::sin(2.0 * kPi * 2040.0 * time + 0.08);
  return 0.16 * pulse * window;
}

double ComputeProtocolClosePulse(size_t local_index, size_t sample_count,
                                 int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double window = ComputeBurstWindow(progress, 0.10, 0.36);
  if (window <= 0.0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double pulse = 0.72 * std::sin(2.0 * kPi * 310.0 * time) +
                       0.20 * std::sin(2.0 * kPi * 620.0 * time + 0.24) +
                       0.08 * std::sin(2.0 * kPi * 930.0 * time + 0.10);
  return 0.22 * pulse * window;
}

double ComputeProtocolAckChirp(size_t local_index, size_t sample_count,
                               int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double window = ComputeBurstWindow(progress, 0.52, 0.70);
  if (window <= 0.0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double chirp_progress = std::clamp((progress - 0.52) / 0.18, 0.0, 1.0);
  const double chirp_freq_hz =
      620.0 + (420.0 - 620.0) * std::pow(chirp_progress, 0.84);
  const double chirp =
      0.76 * std::sin(2.0 * kPi * chirp_freq_hz * time) +
      0.24 * std::sin(2.0 * kPi * chirp_freq_hz * 2.0 * time + 0.26);
  return 0.18 * chirp * window;
}

void ApplySpectralTexture(std::vector<int16_t>* pcm, int sample_rate_hz,
                          const VoicingStyleTraits& style_traits,
                          const EncodeProgressSink* progress_sink = nullptr,
                          float progress_begin = 0.0f,
                          float progress_end = 1.0f,
                          std::size_t completed_samples_before = 0,
                          std::size_t total_work_samples = 0) {
  if (pcm == nullptr || pcm->empty() || sample_rate_hz <= 0) {
    return;
  }

  const double low_alpha = ComputeOnePoleAlpha(240.0, sample_rate_hz);
  const double band_alpha = ComputeOnePoleAlpha(2200.0, sample_rate_hz);
  double low_state = 0.0;
  double band_state = 0.0;
  std::size_t last_reported_samples = completed_samples_before;

  for (std::size_t index = 0; index < pcm->size(); ++index) {
    if (index > 0 && index % kProgressReportStrideSamples == 0) {
      ThrowIfEncodeCancelled(progress_sink);
    }
    const double dry = static_cast<double>((*pcm)[index]) / kPcmScale;
    low_state += low_alpha * (dry - low_state);
    const double band_input = dry - low_state;
    band_state += band_alpha * (band_input - band_state);
    const double bandpassed = band_state;
    const double saturated = SoftClipSample(dry, kSpectralSoftClipDrive);
    const double textured =
        dry *
            (1.0 - kSpectralBandMix * style_traits.spectral_band_mix_scale -
             kSpectralSoftClipMix * style_traits.spectral_softclip_mix_scale) +
        bandpassed * (kSpectralBandMix * style_traits.spectral_band_mix_scale) +
        saturated *
            (kSpectralSoftClipMix * style_traits.spectral_softclip_mix_scale);
    (*pcm)[index] = static_cast<int16_t>(
        std::lround(std::clamp(textured, -1.0, 1.0) * kPcmScale));
    MaybeReportCompletedSampleProgress(
        progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin,
        progress_end, completed_samples_before + index + 1, total_work_samples,
        &last_reported_samples);
  }

  MaybeReportCompletedSampleProgress(
      progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin,
      progress_end, completed_samples_before + pcm->size(), total_work_samples,
      &last_reported_samples, true);
}

double ComputeBoundaryClick(double gain, const VoicingStyleTraits& style_traits,
                            size_t chunk_index, size_t local_index,
                            size_t click_window) {
  const double accent_scale =
      IsByteBoundaryChunk(chunk_index)
          ? style_traits.byte_boundary_click_scale
          : (IsNibbleBoundaryChunk(chunk_index)
                 ? style_traits.nibble_boundary_accent_factor
                 : 0.0);
  if (gain <= 0.0 || accent_scale <= 0.0 || click_window == 0 ||
      local_index >= click_window) {
    return 0.0;
  }

  const double phase =
      static_cast<double>(local_index) / static_cast<double>(click_window);
  const double primary_burst = ComputeBurstWindow(phase, 0.00, 0.34);
  const double secondary_burst = 0.54 * ComputeBurstWindow(phase, 0.46, 0.80);
  const double decay = 1.0 - phase;
  const double carrier = std::sin(2.0 * kPi * (3.25 + accent_scale) * phase);
  return gain * accent_scale * carrier * (primary_burst + secondary_burst) *
         decay;
}

double ComputeSegmentEnvelope(size_t local_index, size_t sample_count,
                              double rise_ratio, double fall_ratio) {
  if (sample_count == 0) {
    return 0.0;
  }

  const size_t rise_count = RatioToCount(rise_ratio, sample_count);
  const size_t fall_count = RatioToCount(fall_ratio, sample_count);
  auto [fitted_rise_count, fitted_fall_count] =
      FitAttackReleaseCounts(sample_count, rise_count, fall_count);
  return ComputeEnvelope(local_index, sample_count, fitted_rise_count,
                         fitted_fall_count);
}
