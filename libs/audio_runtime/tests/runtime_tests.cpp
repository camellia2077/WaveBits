#include "audio_runtime.h"

#include "test_framework.h"

namespace {

void TestRuntimeLoadPlayPauseResumeComplete() {
    auto state = audio_runtime_load(200, 100);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_IDLE, "Load should initialize runtime in idle state.");
    test::AssertEq(state.total_samples, 200, "Load should preserve total samples.");
    test::AssertEq(state.sample_rate_hz, 100, "Load should preserve sample rate.");

    state = audio_runtime_play_started(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PLAYING, "Play should move runtime into playing state.");

    state = audio_runtime_progress(state, 75);
    test::AssertEq(state.played_samples, 75, "Progress should advance played samples.");
    test::AssertTrue(
        audio_runtime_progress_fraction(&state) > 0.37f && audio_runtime_progress_fraction(&state) < 0.38f,
        "Progress fraction should reflect the played sample ratio.");

    state = audio_runtime_paused(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Pause should move runtime into paused state.");

    state = audio_runtime_resumed(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PLAYING, "Resume should return runtime to playing state.");

    state = audio_runtime_completed(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_COMPLETED, "Complete should move runtime into completed state.");
    test::AssertEq(state.played_samples, 200, "Complete should advance playback to the end.");
}

void TestRuntimePausedScrubCommit() {
    auto state = audio_runtime_load(120, 60);
    state = audio_runtime_play_started(state);
    state = audio_runtime_progress(state, 30);
    state = audio_runtime_paused(state);

    state = audio_runtime_scrub_started(state);
    state = audio_runtime_scrub_changed(state, 45);
    test::AssertEq(state.is_scrubbing, 1, "Scrub change should keep runtime in scrubbing mode.");
    test::AssertEq(state.scrub_target_samples, 45, "Scrub change should update preview target.");
    test::AssertEq(state.played_samples, 30, "Scrub preview should not overwrite the committed playback position.");

    state = audio_runtime_scrub_committed(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Commit from paused should stay paused.");
    test::AssertEq(state.is_scrubbing, 0, "Scrub commit should clear scrubbing mode.");
    test::AssertEq(state.played_samples, 45, "Scrub commit should move the committed playback position.");
}

void TestRuntimePlayingScrubCommitResume() {
    auto state = audio_runtime_load(100, 50);
    state = audio_runtime_play_started(state);
    state = audio_runtime_progress(state, 20);

    state = audio_runtime_scrub_started(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Scrub start while playing should pause the runtime state.");
    test::AssertEq(state.resume_after_scrub, 1, "Scrub start while playing should remember auto-resume intent.");

    const auto progress_while_scrubbing = audio_runtime_progress(state, 70);
    test::AssertEq(
        progress_while_scrubbing.played_samples,
        state.played_samples,
        "Backend progress should be ignored while the user is scrubbing.");

    state = audio_runtime_scrub_changed(state, 80);
    state = audio_runtime_scrub_committed(state);
    test::AssertEq(state.resume_after_scrub, 0, "Scrub commit should clear the auto-resume intent latch.");
    test::AssertEq(state.played_samples, 80, "Scrub commit should keep the selected target.");

    state = audio_runtime_resumed(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PLAYING, "Resume after scrub should return to playing state.");
    test::AssertEq(state.played_samples, 80, "Resume after scrub should continue from the committed target.");
}

void TestRuntimeClampAndFractionHelpers() {
    test::AssertEq(audio_runtime_clamp_samples(100, -5), 0, "Clamp should floor negative sample positions.");
    test::AssertEq(audio_runtime_clamp_samples(100, 120), 100, "Clamp should cap sample positions at the end.");
    test::AssertEq(audio_runtime_fraction_to_samples(200, -1.0f), 0, "Fraction helper should floor negative fractions.");
    test::AssertEq(audio_runtime_fraction_to_samples(201, 0.5f), 101, "Fraction helper should round half up consistently.");
    test::AssertEq(audio_runtime_fraction_to_samples(200, 1.0f), 200, "Fraction helper should map 1.0 to the full sample count.");
    test::AssertEq(audio_runtime_fraction_to_samples(200, 2.0f), 200, "Fraction helper should clamp overflow fractions.");
}

void TestRuntimeCompletedScrubBackBecomesPaused() {
    auto state = audio_runtime_load(90, 45);
    state = audio_runtime_completed(state);
    state = audio_runtime_scrub_started(state);
    state = audio_runtime_scrub_changed(state, 12);
    state = audio_runtime_scrub_committed(state);

    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Dragging back from completed should land in paused state.");
    test::AssertEq(state.played_samples, 12, "Dragging back from completed should update the playback position.");
}

void TestRuntimeZeroTotalsStaySafe() {
    auto state = audio_runtime_load(0, 0);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_IDLE, "Zero-length loads should stay idle.");
    test::AssertEq(audio_runtime_progress_fraction(&state), 0.0f, "Zero-length loads should not report progress.");
    test::AssertEq(audio_runtime_elapsed_ms(&state), 0LL, "Zero sample rate should yield zero elapsed time.");
    test::AssertEq(audio_runtime_total_ms(&state), 0LL, "Zero sample rate should yield zero total time.");

    state = audio_runtime_scrub_started(state);
    test::AssertEq(state.is_scrubbing, 0, "Zero-length sessions should not enter scrubbing mode.");
}

void TestRuntimeStoppedAndFailedStateTransitions() {
    {
        auto state = audio_runtime_load(300, 100);
        state = audio_runtime_play_started(state);
        state = audio_runtime_progress(state, 120);
        state = audio_runtime_stopped(state);

        test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_STOPPED, "Stopped sessions should move into stopped state.");
        test::AssertEq(state.played_samples, 0, "Stopped sessions should reset played samples to the start.");
        test::AssertEq(state.is_scrubbing, 0, "Stopped sessions should clear scrubbing state.");
    }

    {
        auto state = audio_runtime_load(300, 100);
        state = audio_runtime_play_started(state);
        state = audio_runtime_progress(state, 90);
        state = audio_runtime_failed(state);

        test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_FAILED, "Failed sessions should move into failed state.");
        test::AssertEq(state.played_samples, 90, "Failed sessions should preserve the current playback position.");
        test::AssertEq(state.is_scrubbing, 0, "Failed sessions should clear scrubbing state.");
    }

    {
        auto state = audio_runtime_load(300, 100);
        state = audio_runtime_play_started(state);
        state = audio_runtime_progress(state, 80);
        state = audio_runtime_scrub_started(state);
        state = audio_runtime_scrub_changed(state, 150);
        state = audio_runtime_failed(state);

        test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_FAILED, "Failure during scrubbing should still move into failed state.");
        test::AssertEq(state.played_samples, 150, "Failure during scrubbing should preserve the displayed scrub target.");
        test::AssertEq(state.is_scrubbing, 0, "Failure during scrubbing should clear scrubbing state.");
    }
}

void TestRuntimeScrubCanceledRespectsResumeIntent() {
    {
        auto state = audio_runtime_load(120, 60);
        state = audio_runtime_play_started(state);
        state = audio_runtime_progress(state, 30);
        state = audio_runtime_scrub_started(state);
        state = audio_runtime_scrub_changed(state, 75);
        state = audio_runtime_scrub_canceled(state);

        test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PLAYING, "Cancelling a playing scrub should resume playback.");
        test::AssertEq(state.played_samples, 30, "Cancelling a playing scrub should restore the committed playback position.");
        test::AssertEq(state.is_scrubbing, 0, "Cancelling a playing scrub should clear scrubbing state.");
    }

    {
        auto state = audio_runtime_load(120, 60);
        state = audio_runtime_play_started(state);
        state = audio_runtime_progress(state, 30);
        state = audio_runtime_paused(state);
        state = audio_runtime_scrub_started(state);
        state = audio_runtime_scrub_changed(state, 75);
        state = audio_runtime_scrub_canceled(state);

        test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Cancelling a paused scrub should stay paused.");
        test::AssertEq(state.played_samples, 30, "Cancelling a paused scrub should restore the committed playback position.");
        test::AssertEq(state.is_scrubbing, 0, "Cancelling a paused scrub should clear scrubbing state.");
    }
}

void TestRuntimeExactTimingHelpers() {
    auto state = audio_runtime_load(1000, 250);
    state = audio_runtime_play_started(state);
    state = audio_runtime_progress(state, 125);

    test::AssertEq(audio_runtime_progress_fraction(&state), 0.125f, "Progress fraction should be exact for simple ratios.");
    test::AssertEq(audio_runtime_elapsed_ms(&state), 500LL, "Elapsed time should follow played samples and sample rate.");
    test::AssertEq(audio_runtime_total_ms(&state), 4000LL, "Total time should follow total samples and sample rate.");

    state = audio_runtime_scrub_started(state);
    state = audio_runtime_scrub_changed(state, 500);
    test::AssertEq(audio_runtime_progress_fraction(&state), 0.5f, "Progress fraction should use the scrub target while scrubbing.");
    test::AssertEq(audio_runtime_elapsed_ms(&state), 2000LL, "Elapsed time should use the scrub target while scrubbing.");
}

void TestRuntimeCompletedScrubCancelConvergesToPausedEnd() {
    auto state = audio_runtime_load(90, 45);
    state = audio_runtime_completed(state);
    state = audio_runtime_scrub_started(state);
    state = audio_runtime_scrub_changed(state, 12);
    state = audio_runtime_scrub_canceled(state);

    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Cancelling a completed scrub should converge to paused state.");
    test::AssertEq(state.played_samples, 90, "Cancelling a completed scrub should restore the committed end position.");
    test::AssertEq(state.is_scrubbing, 0, "Cancelling a completed scrub should clear scrubbing state.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Runtime.LoadPlayPauseResumeComplete", TestRuntimeLoadPlayPauseResumeComplete);
    runner.Add("Runtime.PausedScrubCommit", TestRuntimePausedScrubCommit);
    runner.Add("Runtime.PlayingScrubCommitResume", TestRuntimePlayingScrubCommitResume);
    runner.Add("Runtime.ClampAndFractionHelpers", TestRuntimeClampAndFractionHelpers);
    runner.Add("Runtime.CompletedScrubBackBecomesPaused", TestRuntimeCompletedScrubBackBecomesPaused);
    runner.Add("Runtime.ZeroTotalsStaySafe", TestRuntimeZeroTotalsStaySafe);
    runner.Add("Runtime.StoppedAndFailedStateTransitions", TestRuntimeStoppedAndFailedStateTransitions);
    runner.Add("Runtime.ScrubCanceledRespectsResumeIntent", TestRuntimeScrubCanceledRespectsResumeIntent);
    runner.Add("Runtime.ExactTimingHelpers", TestRuntimeExactTimingHelpers);
    runner.Add("Runtime.CompletedScrubCancelConvergesToPausedEnd", TestRuntimeCompletedScrubCancelConvergesToPausedEnd);
    return runner.Run();
}
