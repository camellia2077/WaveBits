double ComputePhraseMask(double progress, double first_begin, double first_end,
                         double second_begin, double second_end,
                         double third_begin, double third_end) {
  return std::max(
      ComputeBurstWindow(progress, first_begin, first_end),
      std::max(ComputeBurstWindow(progress, second_begin, second_end),
               ComputeBurstWindow(progress, third_begin, third_end)));
}

template <std::size_t N>
double ComputeCadenceMask(double progress,
                          const std::array<CadenceWindow, N>& windows) {
  double mask = 0.0;
  for (const CadenceWindow& window : windows) {
    mask =
        std::max(mask, ComputeBurstWindow(progress, window.begin, window.end));
  }
  return mask;
}

double ComputeTerminalMute(double progress, double begin, double end) {
  if (end <= begin || progress <= begin) {
    return 1.0;
  }
  if (progress >= end) {
    return 0.0;
  }

  const double local = std::clamp((progress - begin) / (end - begin), 0.0, 1.0);
  return std::cos(local * kPi * 0.5);
}

double ComputePreamblePhraseMask(size_t local_index, size_t sample_count,
                                 const VoicingStyleTraits& style_traits) {
  if (sample_count == 0 || style_traits.preamble_phrase_mask_mix <= 0.0) {
    return 1.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  double phrased_mask = 1.0;
  switch (style_traits.preamble_cadence_profile) {
    case FixedShellCadenceProfile::kRitualChant:
      phrased_mask =
          ComputePhraseMask(progress, 0.03, 0.24, 0.31, 0.59, 0.66, 0.95);
      break;
    case FixedShellCadenceProfile::kProtocolHandshake:
    default:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 3>{
                                                      CadenceWindow{0.05, 0.18},
                                                      CadenceWindow{0.30, 0.43},
                                                      CadenceWindow{0.58, 0.70},
                                                  });
      break;
  }
  return (1.0 - style_traits.preamble_phrase_mask_mix) +
         style_traits.preamble_phrase_mask_mix * phrased_mask;
}

double ComputeEpiloguePhraseMask(size_t local_index, size_t sample_count,
                                 const VoicingStyleTraits& style_traits) {
  if (sample_count == 0 || style_traits.epilogue_phrase_mask_mix <= 0.0) {
    return 1.0;
  }

  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  double phrased_mask = 1.0;
  switch (style_traits.epilogue_cadence_profile) {
    case FixedShellCadenceProfile::kRitualChant:
      phrased_mask = std::max(ComputeBurstWindow(progress, 0.08, 0.46),
                              ComputeBurstWindow(progress, 0.56, 0.94));
      break;
    case FixedShellCadenceProfile::kProtocolHandshake:
    default:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 2>{
                                                      CadenceWindow{0.12, 0.32},
                                                      CadenceWindow{0.54, 0.68},
                                                  });
      break;
  }
  return (1.0 - style_traits.epilogue_phrase_mask_mix) +
         style_traits.epilogue_phrase_mask_mix * phrased_mask;
}

double RenderFixedPreambleSample(size_t local_index, size_t sample_count,
                                 const VoicingStyleTraits& style_traits,
                                 int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double envelope = ComputeSegmentEnvelope(
      local_index, sample_count, style_traits.preamble_rise_ratio,
      style_traits.preamble_fall_ratio);
  const double grouped_burst =
      ComputeGroupedBurstPattern(local_index, sample_count);
  const double pulse_gain =
      0.02 + 0.98 * grouped_burst * style_traits.preamble_grouped_burst_mix;
  double carrier = 0.0;
  double shell = 0.0;
  switch (style_traits.preamble_timbre_profile) {
    case FixedShellTimbreProfile::kProtocolBoundary:
      carrier = 0.34 * std::sin(2.0 * kPi * 680.0 * time) +
                0.18 * std::sin(2.0 * kPi * 1360.0 * time) +
                0.08 * std::sin(2.0 * kPi * 2040.0 * time);
      shell =
          style_traits.preamble_invocation_shell_mix *
          (ComputeProtocolLeadSweep(local_index, sample_count, sample_rate_hz) +
           ComputeProtocolLeadLockPulse(local_index, sample_count,
                                        sample_rate_hz));
      break;
    case FixedShellTimbreProfile::kInvocationClosure:
    default:
      carrier = 0.42 * std::sin(2.0 * kPi * 520.0 * time) +
                0.16 * std::sin(2.0 * kPi * 1040.0 * time) +
                0.06 * std::sin(2.0 * kPi * 1560.0 * time);
      shell =
          style_traits.preamble_invocation_shell_mix *
          (ComputeInvocationSweep(local_index, sample_count, sample_rate_hz) +
           ComputeInvocationPulse(local_index, sample_count, sample_rate_hz));
      break;
  }
  const double phrase_mask =
      ComputePreamblePhraseMask(local_index, sample_count, style_traits);
  const double terminal_mute =
      ComputeTerminalMute(progress, style_traits.preamble_terminal_mute_begin,
                          style_traits.preamble_terminal_mute_end);
  return std::clamp(
      (carrier * pulse_gain + shell) * envelope * phrase_mask * terminal_mute,
      -0.75, 0.75);
}

double RenderFixedEpilogueSample(size_t local_index, size_t sample_count,
                                 const VoicingStyleTraits& style_traits,
                                 int sample_rate_hz) {
  if (sample_count == 0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double progress =
      static_cast<double>(local_index) / static_cast<double>(sample_count);
  const double decay = std::pow(std::max(0.0, 1.0 - progress),
                                style_traits.epilogue_decay_exponent);
  const double envelope = ComputeSegmentEnvelope(
      local_index, sample_count, style_traits.epilogue_rise_ratio,
      style_traits.epilogue_fall_ratio);
  const double grouped_burst =
      ComputeGroupedBurstPattern(local_index, sample_count);
  const double pulse_gain =
      0.02 + 0.98 * grouped_burst * style_traits.epilogue_grouped_burst_mix;
  double carrier = 0.0;
  double shell = 0.0;
  switch (style_traits.epilogue_timbre_profile) {
    case FixedShellTimbreProfile::kProtocolBoundary:
      carrier = 0.42 * std::sin(2.0 * kPi * 310.0 * time) +
                0.16 * std::sin(2.0 * kPi * 620.0 * time) +
                0.07 * std::sin(2.0 * kPi * 930.0 * time);
      shell =
          style_traits.epilogue_closure_shell_mix *
          (ComputeProtocolClosePulse(local_index, sample_count,
                                     sample_rate_hz) +
           ComputeProtocolAckChirp(local_index, sample_count, sample_rate_hz));
      break;
    case FixedShellTimbreProfile::kInvocationClosure:
    default:
      carrier = 0.36 * std::sin(2.0 * kPi * 360.0 * time) +
                0.12 * std::sin(2.0 * kPi * 720.0 * time);
      shell = style_traits.epilogue_closure_shell_mix *
              (ComputeClosurePulse(local_index, sample_count, sample_rate_hz) +
               ComputeClosureTail(local_index, sample_count, sample_rate_hz));
      break;
  }
  const double phrase_mask =
      ComputeEpiloguePhraseMask(local_index, sample_count, style_traits);
  const double terminal_mute =
      ComputeTerminalMute(progress, style_traits.epilogue_terminal_mute_begin,
                          style_traits.epilogue_terminal_mute_end);
  return std::clamp((carrier * pulse_gain + shell) * envelope * decay *
                        phrase_mask * terminal_mute,
                    -0.65, 0.65);
}
