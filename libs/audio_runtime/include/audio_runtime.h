#pragma once

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum audio_runtime_playback_phase {
  AUDIO_RUNTIME_PLAYBACK_IDLE = 0,
  AUDIO_RUNTIME_PLAYBACK_PLAYING = 1,
  AUDIO_RUNTIME_PLAYBACK_PAUSED = 2,
  AUDIO_RUNTIME_PLAYBACK_STOPPED = 3,
  AUDIO_RUNTIME_PLAYBACK_COMPLETED = 4,
  AUDIO_RUNTIME_PLAYBACK_FAILED = 5
} audio_runtime_playback_phase;

typedef struct audio_runtime_playback_session_state {
  audio_runtime_playback_phase phase;
  int played_samples;
  int total_samples;
  int sample_rate_hz;
  int is_scrubbing;
  int scrub_target_samples;
  int resume_after_scrub;
} audio_runtime_playback_session_state;

audio_runtime_playback_session_state audio_runtime_cleared(void);
audio_runtime_playback_session_state audio_runtime_load(int total_samples,
                                                        int sample_rate_hz);
audio_runtime_playback_session_state audio_runtime_play_started(
    audio_runtime_playback_session_state state);
audio_runtime_playback_session_state audio_runtime_paused(
    audio_runtime_playback_session_state state);
audio_runtime_playback_session_state audio_runtime_resumed(
    audio_runtime_playback_session_state state);
audio_runtime_playback_session_state audio_runtime_progress(
    audio_runtime_playback_session_state state, int played_samples);
audio_runtime_playback_session_state audio_runtime_scrub_started(
    audio_runtime_playback_session_state state);
audio_runtime_playback_session_state audio_runtime_scrub_changed(
    audio_runtime_playback_session_state state, int target_samples);
audio_runtime_playback_session_state audio_runtime_scrub_committed(
    audio_runtime_playback_session_state state);
audio_runtime_playback_session_state audio_runtime_scrub_canceled(
    audio_runtime_playback_session_state state);
audio_runtime_playback_session_state audio_runtime_stopped(
    audio_runtime_playback_session_state state);
audio_runtime_playback_session_state audio_runtime_completed(
    audio_runtime_playback_session_state state);
audio_runtime_playback_session_state audio_runtime_failed(
    audio_runtime_playback_session_state state);

float audio_runtime_progress_fraction(
    const audio_runtime_playback_session_state* state);
int audio_runtime_clamp_samples(int total_samples, int sample_index);
int audio_runtime_fraction_to_samples(int total_samples, float fraction);
int64_t audio_runtime_elapsed_ms(
    const audio_runtime_playback_session_state* state);
int64_t audio_runtime_total_ms(
    const audio_runtime_playback_session_state* state);

#ifdef __cplusplus
}
#endif
