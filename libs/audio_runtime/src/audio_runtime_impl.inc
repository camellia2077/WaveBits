namespace {

constexpr int kFalse = 0;
constexpr int kTrue = 1;

audio_runtime_playback_phase NormalizePhase(
    audio_runtime_playback_phase phase) {
  switch (phase) {
    case AUDIO_RUNTIME_PLAYBACK_IDLE:
    case AUDIO_RUNTIME_PLAYBACK_PLAYING:
    case AUDIO_RUNTIME_PLAYBACK_PAUSED:
    case AUDIO_RUNTIME_PLAYBACK_STOPPED:
    case AUDIO_RUNTIME_PLAYBACK_COMPLETED:
    case AUDIO_RUNTIME_PLAYBACK_FAILED:
      return phase;
    default:
      return AUDIO_RUNTIME_PLAYBACK_IDLE;
  }
}

int ClampSamplesToTotal(int total_samples, int sample_index) {
  if (total_samples <= 0) {
    return 0;
  }
  return std::clamp(sample_index, 0, total_samples);
}

int DisplaySamples(const audio_runtime_playback_session_state& state) {
  return state.is_scrubbing == kTrue ? state.scrub_target_samples
                                     : state.played_samples;
}

audio_runtime_playback_session_state MakeState(
    audio_runtime_playback_phase phase, int played_samples, int total_samples,
    int sample_rate_hz, int is_scrubbing, int scrub_target_samples,
    int resume_after_scrub) {
  const int normalized_total = std::max(total_samples, 0);
  const int normalized_sample_rate = std::max(sample_rate_hz, 0);
  const int normalized_played =
      ClampSamplesToTotal(normalized_total, played_samples);
  const int normalized_scrubbing =
      normalized_total > 0 && is_scrubbing == kTrue ? kTrue : kFalse;
  const int normalized_scrub_target =
      normalized_scrubbing == kTrue
          ? ClampSamplesToTotal(normalized_total, scrub_target_samples)
          : normalized_played;
  const int normalized_resume_after_scrub =
      normalized_scrubbing == kTrue && resume_after_scrub == kTrue ? kTrue
                                                                   : kFalse;

  if (normalized_total <= 0) {
    return {AUDIO_RUNTIME_PLAYBACK_IDLE,
            0,
            0,
            normalized_sample_rate,
            kFalse,
            0,
            kFalse};
  }

  return {NormalizePhase(phase),
          normalized_played,
          normalized_total,
          normalized_sample_rate,
          normalized_scrubbing,
          normalized_scrub_target,
          normalized_resume_after_scrub};
}

}  // namespace

audio_runtime_playback_session_state audio_runtime_cleared(void) {
  return MakeState(AUDIO_RUNTIME_PLAYBACK_IDLE, 0, 0, 0, kFalse, 0, kFalse);
}

audio_runtime_playback_session_state audio_runtime_load(int total_samples,
                                                        int sample_rate_hz) {
  return MakeState(AUDIO_RUNTIME_PLAYBACK_IDLE, 0, total_samples,
                   sample_rate_hz, kFalse, 0, kFalse);
}

audio_runtime_playback_session_state audio_runtime_play_started(
    audio_runtime_playback_session_state state) {
  const int restart_from =
      state.total_samples > 0 && state.played_samples >= state.total_samples
          ? 0
          : state.played_samples;
  return MakeState(AUDIO_RUNTIME_PLAYBACK_PLAYING, restart_from,
                   state.total_samples, state.sample_rate_hz, kFalse,
                   restart_from, kFalse);
}

audio_runtime_playback_session_state audio_runtime_paused(
    audio_runtime_playback_session_state state) {
  return MakeState(state.total_samples > 0 ? AUDIO_RUNTIME_PLAYBACK_PAUSED
                                           : AUDIO_RUNTIME_PLAYBACK_IDLE,
                   DisplaySamples(state), state.total_samples,
                   state.sample_rate_hz, kFalse, DisplaySamples(state), kFalse);
}

audio_runtime_playback_session_state audio_runtime_resumed(
    audio_runtime_playback_session_state state) {
  return MakeState(state.total_samples > 0 ? AUDIO_RUNTIME_PLAYBACK_PLAYING
                                           : AUDIO_RUNTIME_PLAYBACK_IDLE,
                   DisplaySamples(state), state.total_samples,
                   state.sample_rate_hz, kFalse, DisplaySamples(state), kFalse);
}

audio_runtime_playback_session_state audio_runtime_progress(
    audio_runtime_playback_session_state state, int played_samples) {
  if (state.is_scrubbing == kTrue) {
    return state;
  }
  return MakeState(state.phase, played_samples, state.total_samples,
                   state.sample_rate_hz, kFalse, played_samples, kFalse);
}

audio_runtime_playback_session_state audio_runtime_scrub_started(
    audio_runtime_playback_session_state state) {
  return MakeState(
      state.total_samples > 0 ? AUDIO_RUNTIME_PLAYBACK_PAUSED
                              : AUDIO_RUNTIME_PLAYBACK_IDLE,
      state.played_samples, state.total_samples, state.sample_rate_hz, kTrue,
      DisplaySamples(state),
      state.phase == AUDIO_RUNTIME_PLAYBACK_PLAYING ? kTrue : kFalse);
}

audio_runtime_playback_session_state audio_runtime_scrub_changed(
    audio_runtime_playback_session_state state, int target_samples) {
  const auto current =
      state.is_scrubbing == kTrue ? state : audio_runtime_scrub_started(state);
  return MakeState(current.total_samples > 0 ? AUDIO_RUNTIME_PLAYBACK_PAUSED
                                             : AUDIO_RUNTIME_PLAYBACK_IDLE,
                   current.played_samples, current.total_samples,
                   current.sample_rate_hz, kTrue, target_samples,
                   current.resume_after_scrub);
}

audio_runtime_playback_session_state audio_runtime_scrub_committed(
    audio_runtime_playback_session_state state) {
  if (state.is_scrubbing != kTrue) {
    return state;
  }
  return MakeState(AUDIO_RUNTIME_PLAYBACK_PAUSED, state.scrub_target_samples,
                   state.total_samples, state.sample_rate_hz, kFalse,
                   state.scrub_target_samples, kFalse);
}

audio_runtime_playback_session_state audio_runtime_scrub_canceled(
    audio_runtime_playback_session_state state) {
  if (state.is_scrubbing != kTrue) {
    return state;
  }
  return MakeState(state.resume_after_scrub == kTrue
                       ? AUDIO_RUNTIME_PLAYBACK_PLAYING
                       : AUDIO_RUNTIME_PLAYBACK_PAUSED,
                   state.played_samples, state.total_samples,
                   state.sample_rate_hz, kFalse, state.played_samples, kFalse);
}

audio_runtime_playback_session_state audio_runtime_stopped(
    audio_runtime_playback_session_state state) {
  return MakeState(state.total_samples > 0 ? AUDIO_RUNTIME_PLAYBACK_STOPPED
                                           : AUDIO_RUNTIME_PLAYBACK_IDLE,
                   0, state.total_samples, state.sample_rate_hz, kFalse, 0,
                   kFalse);
}

audio_runtime_playback_session_state audio_runtime_completed(
    audio_runtime_playback_session_state state) {
  return MakeState(state.total_samples > 0 ? AUDIO_RUNTIME_PLAYBACK_COMPLETED
                                           : AUDIO_RUNTIME_PLAYBACK_IDLE,
                   state.total_samples, state.total_samples,
                   state.sample_rate_hz, kFalse, state.total_samples, kFalse);
}

audio_runtime_playback_session_state audio_runtime_failed(
    audio_runtime_playback_session_state state) {
  return MakeState(state.total_samples > 0 ? AUDIO_RUNTIME_PLAYBACK_FAILED
                                           : AUDIO_RUNTIME_PLAYBACK_IDLE,
                   DisplaySamples(state), state.total_samples,
                   state.sample_rate_hz, kFalse, DisplaySamples(state), kFalse);
}

float audio_runtime_progress_fraction(
    const audio_runtime_playback_session_state* state) {
  if (state == nullptr || state->total_samples <= 0) {
    return 0.0f;
  }
  const int displayed_samples = DisplaySamples(*state);
  return static_cast<float>(displayed_samples) /
         static_cast<float>(state->total_samples);
}

int audio_runtime_clamp_samples(int total_samples, int sample_index) {
  return ClampSamplesToTotal(total_samples, sample_index);
}

int audio_runtime_fraction_to_samples(int total_samples, float fraction) {
  if (total_samples <= 0) {
    return 0;
  }
  const float normalized_fraction = std::clamp(fraction, 0.0f, 1.0f);
  return ClampSamplesToTotal(
      total_samples,
      static_cast<int>(std::lround(normalized_fraction * total_samples)));
}

int64_t audio_runtime_elapsed_ms(
    const audio_runtime_playback_session_state* state) {
  if (state == nullptr || state->sample_rate_hz <= 0) {
    return 0;
  }
  const int displayed_samples = DisplaySamples(*state);
  return (static_cast<int64_t>(displayed_samples) * 1000LL) /
         state->sample_rate_hz;
}

int64_t audio_runtime_total_ms(
    const audio_runtime_playback_session_state* state) {
  if (state == nullptr || state->sample_rate_hz <= 0) {
    return 0;
  }
  return (static_cast<int64_t>(state->total_samples) * 1000LL) /
         state->sample_rate_hz;
}
