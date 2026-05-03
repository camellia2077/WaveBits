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

double ComputeLowMetalBellStrike(size_t local_index, int sample_rate_hz,
                                 double strike_time_sec,
                                 double tail_duration_sec, double base_freq_hz,
                                 double gain) {
  if (sample_rate_hz <= 0 || tail_duration_sec <= 0.0 ||
      base_freq_hz <= 0.0 || gain <= 0.0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  if (time < strike_time_sec || time > strike_time_sec + tail_duration_sec) {
    return 0.0;
  }

  const double elapsed = time - strike_time_sec;
  const double decay_progress =
      std::clamp(elapsed / tail_duration_sec, 0.0, 1.0);
  const double tail_envelope = std::exp(-4.15 * decay_progress) *
                               std::cos(decay_progress * kPi * 0.5);
  const double attack_envelope = std::exp(-elapsed * 95.0);
  const double shimmer = 1.0 + 0.055 * std::sin(2.0 * kPi * 4.7 * elapsed) +
                         0.035 * std::sin(2.0 * kPi * 7.3 * elapsed + 0.4);
  const double impact =
      attack_envelope *
      (0.74 * std::sin(2.0 * kPi * 820.0 * elapsed) +
       0.42 * std::sin(2.0 * kPi * 1390.0 * elapsed + 0.31) +
       0.26 * std::sin(2.0 * kPi * 2110.0 * elapsed + 0.62));
  const double bell =
      0.50 * std::sin(2.0 * kPi * base_freq_hz * elapsed) +
      0.28 * std::sin(2.0 * kPi * base_freq_hz * 1.37 * elapsed + 0.19) +
      0.19 * std::sin(2.0 * kPi * base_freq_hz * 2.11 * elapsed + 0.43) +
      0.12 * std::sin(2.0 * kPi * base_freq_hz * 2.86 * elapsed + 0.71) +
      0.07 * std::sin(2.0 * kPi * base_freq_hz * 4.18 * elapsed + 1.02);
  return gain * (tail_envelope * shimmer * bell + 0.34 * impact);
}

// Litany shell timing uses absolute seconds instead of normalized progress so
// the three invocation strikes keep the same ritual spacing as tuning changes.
double ComputeLitanyPreambleBellSequence(size_t local_index,
                                         size_t sample_count,
                                         int sample_rate_hz) {
  constexpr double kStrikeCenters[3] = {0.20, 0.60, 1.00};
  double bell = 0.0;
  (void)sample_count;
  for (const double strike_time_sec : kStrikeCenters) {
    bell += ComputeLowMetalBellStrike(local_index, sample_rate_hz,
                                      strike_time_sec, 0.54, 104.0, 0.74);
  }
  return bell;
}

// The closing rite is deliberately asymmetric: one longer low bell to settle
// the chant, then one shorter strike to mark the protocol boundary.
double ComputeLitanyEpilogueBellSequence(size_t local_index,
                                         size_t sample_count,
                                         int sample_rate_hz) {
  (void)sample_count;
  const double long_bell = ComputeLowMetalBellStrike(
      local_index, sample_rate_hz, 0.20, 0.78, 92.0, 0.82);
  const double short_bell = ComputeLowMetalBellStrike(
      local_index, sample_rate_hz, 0.82, 0.28, 128.0, 0.64);
  return long_bell + short_bell;
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
    case FixedShellCadenceProfile::kLitanyInvocation:
      phrased_mask =
          ComputePhraseMask(progress, 0.11, 0.32, 0.36, 0.57, 0.61, 0.94);
      break;
    case FixedShellCadenceProfile::kHostileChallenge:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 4>{
                                                      CadenceWindow{0.02, 0.10},
                                                      CadenceWindow{0.14, 0.22},
                                                      CadenceWindow{0.28, 0.42},
                                                      CadenceWindow{0.50, 0.60},
                                                  });
      break;
    case FixedShellCadenceProfile::kZealRising:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 4>{
                                                      CadenceWindow{0.04, 0.13},
                                                      CadenceWindow{0.20, 0.30},
                                                      CadenceWindow{0.38, 0.50},
                                                      CadenceWindow{0.62, 0.76},
                                                  });
      break;
    case FixedShellCadenceProfile::kVoidDescent:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 3>{
                                                      CadenceWindow{0.06, 0.22},
                                                      CadenceWindow{0.38, 0.52},
                                                      CadenceWindow{0.70, 0.88},
                                                  });
      break;
    case FixedShellCadenceProfile::kSteadyProtocol:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 3>{
                                                      CadenceWindow{0.06, 0.17},
                                                      CadenceWindow{0.32, 0.42},
                                                      CadenceWindow{0.58, 0.68},
                                                  });
      break;
    case FixedShellCadenceProfile::kCollapseFailure:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 4>{
                                                      CadenceWindow{0.05, 0.16},
                                                      CadenceWindow{0.22, 0.29},
                                                      CadenceWindow{0.39, 0.48},
                                                      CadenceWindow{0.58, 0.64},
                                                  });
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
    case FixedShellCadenceProfile::kLitanyInvocation:
      phrased_mask = std::max(ComputeBurstWindow(progress, 0.10, 0.58),
                              ComputeBurstWindow(progress, 0.58, 0.98));
      break;
    case FixedShellCadenceProfile::kHostileChallenge:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 3>{
                                                      CadenceWindow{0.04, 0.15},
                                                      CadenceWindow{0.20, 0.30},
                                                      CadenceWindow{0.36, 0.47},
                                                  });
      break;
    case FixedShellCadenceProfile::kZealRising:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 3>{
                                                      CadenceWindow{0.08, 0.20},
                                                      CadenceWindow{0.32, 0.45},
                                                      CadenceWindow{0.58, 0.72},
                                                  });
      break;
    case FixedShellCadenceProfile::kVoidDescent:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 2>{
                                                      CadenceWindow{0.10, 0.36},
                                                      CadenceWindow{0.56, 0.90},
                                                  });
      break;
    case FixedShellCadenceProfile::kSteadyProtocol:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 2>{
                                                      CadenceWindow{0.12, 0.29},
                                                      CadenceWindow{0.52, 0.65},
                                                  });
      break;
    case FixedShellCadenceProfile::kCollapseFailure:
      phrased_mask = ComputeCadenceMask(progress, std::array<CadenceWindow, 5>{
                                                      CadenceWindow{0.05, 0.13},
                                                      CadenceWindow{0.19, 0.26},
                                                      CadenceWindow{0.35, 0.42},
                                                      CadenceWindow{0.52, 0.58},
                                                      CadenceWindow{0.66, 0.70},
                                                  });
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
    case FixedShellTimbreProfile::kSteadyBoundary:
      carrier = 0.28 * std::sin(2.0 * kPi * 420.0 * time) +
                0.10 * std::sin(2.0 * kPi * 840.0 * time) +
                0.04 * std::sin(2.0 * kPi * 1260.0 * time);
      shell =
          style_traits.preamble_invocation_shell_mix * 0.62 *
          (ComputeProtocolLeadSweep(local_index, sample_count, sample_rate_hz) +
           0.72 * ComputeProtocolLeadLockPulse(local_index, sample_count,
                                               sample_rate_hz));
      break;
    case FixedShellTimbreProfile::kHostileEdge: {
      const double hard_gate =
          0.72 + 0.28 * std::copysign(
                       1.0, std::sin(2.0 * kPi * 54.0 * time + 0.30));
      carrier =
          0.38 * std::sin(2.0 * kPi * 920.0 * time) +
          0.24 * std::sin(2.0 * kPi * 1840.0 * time + 0.18) +
          0.12 * std::copysign(
                     1.0, std::sin(2.0 * kPi * 2760.0 * time + 0.27));
      shell =
          style_traits.preamble_invocation_shell_mix * hard_gate *
          (1.08 * ComputeProtocolLeadSweep(local_index, sample_count,
                                           sample_rate_hz) +
           1.20 * ComputeProtocolLeadLockPulse(local_index, sample_count,
                                               sample_rate_hz));
      break;
    }
    case FixedShellTimbreProfile::kLitanyInvocation:
      carrier = 0.10 * std::sin(2.0 * kPi * 120.0 * time + 0.10) +
                0.04 * std::sin(2.0 * kPi * 240.0 * time + 0.34);
      shell = style_traits.preamble_invocation_shell_mix *
              ComputeLitanyPreambleBellSequence(local_index, sample_count,
                                                sample_rate_hz);
      break;
    case FixedShellTimbreProfile::kZealRising: {
      const double rise = std::pow(progress, 0.62);
      const double carrier_hz = 520.0 + (1120.0 - 520.0) * rise;
      carrier = 0.34 * std::sin(2.0 * kPi * carrier_hz * time) +
                0.13 * std::sin(2.0 * kPi * carrier_hz * 2.0 * time + 0.18) +
                0.05 * std::sin(2.0 * kPi * carrier_hz * 3.0 * time + 0.31);
      shell =
          style_traits.preamble_invocation_shell_mix * 0.72 *
          (ComputeProtocolLeadSweep(local_index, sample_count, sample_rate_hz) +
           0.84 * ComputeProtocolLeadLockPulse(local_index, sample_count,
                                               sample_rate_hz));
      break;
    }
    case FixedShellTimbreProfile::kVoidDescent: {
      const double sag = std::pow(progress, 0.70);
      const double carrier_hz = 260.0 + (120.0 - 260.0) * sag;
      const double slow_gate =
          0.76 + 0.24 * std::sin(2.0 * kPi * 3.4 * time + 0.20);
      carrier =
          slow_gate * (0.26 * std::sin(2.0 * kPi * carrier_hz * time) +
                       0.08 * std::sin(2.0 * kPi * carrier_hz * 2.0 * time +
                                        0.24));
      shell =
          style_traits.preamble_invocation_shell_mix * 0.48 *
          (0.60 * ComputeProtocolLeadSweep(local_index, sample_count,
                                           sample_rate_hz) +
           ComputeClosureTail(local_index, sample_count, sample_rate_hz));
      break;
    }
    case FixedShellTimbreProfile::kCollapseFailure: {
      const double sag = std::pow(progress, 0.72);
      const double carrier_hz = 360.0 + (160.0 - 360.0) * sag;
      const double shiver = 0.72 + 0.28 * std::sin(2.0 * kPi * 13.0 * time);
      carrier =
          shiver * (0.30 * std::sin(2.0 * kPi * carrier_hz * time) +
                    0.10 * std::sin(2.0 * kPi * carrier_hz * 1.62 * time));
      shell =
          style_traits.preamble_invocation_shell_mix * 0.66 *
          (ComputeClosurePulse(local_index, sample_count, sample_rate_hz) +
           ComputeClosureTail(local_index, sample_count, sample_rate_hz));
      break;
    }
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
  const double drone =
      ComputeChantDroneLayer(local_index, sample_rate_hz,
                             style_traits.chant_drone_gain * 0.76) *
      envelope * phrase_mask * terminal_mute;
  return std::clamp(
      (carrier * pulse_gain + shell) * envelope * phrase_mask * terminal_mute +
          drone,
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
    case FixedShellTimbreProfile::kSteadyBoundary:
      carrier = 0.30 * std::sin(2.0 * kPi * 240.0 * time) +
                0.11 * std::sin(2.0 * kPi * 480.0 * time + 0.18) +
                0.04 * std::sin(2.0 * kPi * 720.0 * time + 0.10);
      shell =
          style_traits.epilogue_closure_shell_mix * 0.60 *
          (ComputeProtocolClosePulse(local_index, sample_count,
                                     sample_rate_hz) +
           0.70 * ComputeProtocolAckChirp(local_index, sample_count,
                                          sample_rate_hz));
      break;
    case FixedShellTimbreProfile::kHostileEdge: {
      const double hard_gate =
          0.68 + 0.32 * std::copysign(
                       1.0, std::sin(2.0 * kPi * 46.0 * time + 0.18));
      carrier =
          0.40 * std::sin(2.0 * kPi * 760.0 * time) +
          0.22 * std::sin(2.0 * kPi * 1520.0 * time + 0.31) +
          0.12 * std::copysign(
                     1.0, std::sin(2.0 * kPi * 2280.0 * time + 0.14));
      shell =
          style_traits.epilogue_closure_shell_mix * hard_gate *
          (1.18 * ComputeProtocolClosePulse(local_index, sample_count,
                                            sample_rate_hz) +
           1.04 * ComputeProtocolAckChirp(local_index, sample_count,
                                          sample_rate_hz));
      break;
    }
    case FixedShellTimbreProfile::kCollapseFailure: {
      const double sag = std::pow(progress, 0.58);
      const double carrier_hz = 280.0 + (82.0 - 280.0) * sag;
      const double shiver =
          0.72 + 0.20 * std::sin(2.0 * kPi * 10.5 * time + 0.12) +
          0.08 * std::sin(2.0 * kPi * 17.0 * time + 0.48);
      carrier =
          shiver * (0.36 * std::sin(2.0 * kPi * carrier_hz * time) +
                    0.10 * std::sin(2.0 * kPi * carrier_hz * 2.0 * time));
      shell =
          style_traits.epilogue_closure_shell_mix * 0.54 *
          (ComputeClosurePulse(local_index, sample_count, sample_rate_hz) +
           0.64 * ComputeClosureTail(local_index, sample_count,
                                     sample_rate_hz));
      break;
    }
    case FixedShellTimbreProfile::kLitanyInvocation:
      carrier = 0.12 * std::sin(2.0 * kPi * 90.0 * time + 0.22) +
                0.05 * std::sin(2.0 * kPi * 180.0 * time + 0.44);
      shell = style_traits.epilogue_closure_shell_mix *
              ComputeLitanyEpilogueBellSequence(local_index, sample_count,
                                                sample_rate_hz);
      break;
    case FixedShellTimbreProfile::kZealRising: {
      const double rise = std::pow(progress, 0.48);
      const double carrier_hz = 660.0 + (1240.0 - 660.0) * rise;
      carrier = 0.34 * std::sin(2.0 * kPi * carrier_hz * time) +
                0.12 * std::sin(2.0 * kPi * carrier_hz * 2.0 * time + 0.22);
      shell =
          style_traits.epilogue_closure_shell_mix * 0.64 *
          (0.86 * ComputeProtocolClosePulse(local_index, sample_count,
                                            sample_rate_hz) +
           ComputeProtocolAckChirp(local_index, sample_count, sample_rate_hz));
      break;
    }
    case FixedShellTimbreProfile::kVoidDescent: {
      const double sag = std::pow(progress, 0.52);
      const double carrier_hz = 220.0 + (68.0 - 220.0) * sag;
      const double slow_gate =
          0.78 + 0.18 * std::sin(2.0 * kPi * 2.8 * time + 0.40) +
          0.04 * std::sin(2.0 * kPi * 5.6 * time + 0.10);
      carrier =
          slow_gate * (0.32 * std::sin(2.0 * kPi * carrier_hz * time) +
                       0.08 * std::sin(2.0 * kPi * carrier_hz * 2.0 * time +
                                        0.16));
      shell =
          style_traits.epilogue_closure_shell_mix * 0.58 *
          (0.42 * ComputeProtocolClosePulse(local_index, sample_count,
                                            sample_rate_hz) +
           ComputeClosureTail(local_index, sample_count, sample_rate_hz));
      break;
    }
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
  const double drone =
      ComputeChantDroneLayer(local_index, sample_rate_hz,
                             style_traits.chant_drone_gain * 0.24) *
      envelope * decay * phrase_mask * terminal_mute;
  return std::clamp((carrier * pulse_gain + shell) * envelope * decay *
                            phrase_mask * terminal_mute +
                        drone,
                    -0.65, 0.65);
}
