#include <array>
#include <string>

#include "api_test_support.h"

namespace {

using namespace api_tests;

void TestApiEncodeJobRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case);
    bag_encode_job* job = nullptr;
    bag_encode_job_progress progress{};
    bag_pcm16_result pcm{};

    test::AssertEq(
        bag_start_encode_text_job(nullptr, "A", &job),
        BAG_INVALID_ARGUMENT,
        "Null encoder config should be rejected for async jobs.");
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, nullptr, &job),
        BAG_INVALID_ARGUMENT,
        "Null text should be rejected for async jobs.");
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, "A", nullptr),
        BAG_INVALID_ARGUMENT,
        "Null output job pointer should be rejected for async jobs.");

    auto invalid_config = encoder_config;
    invalid_config.sample_rate_hz = 0;
    test::AssertEq(
        bag_start_encode_text_job(&invalid_config, "A", &job),
        BAG_INVALID_ARGUMENT,
        "Invalid configs should not create async jobs.");
    test::AssertTrue(job == nullptr, "Invalid async job start should not leave a job handle behind.");

    test::AssertEq(
        bag_poll_encode_text_job(nullptr, &progress),
        BAG_INVALID_ARGUMENT,
        "Polling with a null job should be rejected.");
    test::AssertEq(
        bag_poll_encode_text_job(job, nullptr),
        BAG_INVALID_ARGUMENT,
        "Polling with a null output should be rejected.");
    test::AssertEq(
        bag_cancel_encode_text_job(nullptr),
        BAG_INVALID_ARGUMENT,
        "Cancelling a null job should be rejected.");
    test::AssertEq(
        bag_take_encode_text_job_result(nullptr, &pcm),
        BAG_INVALID_ARGUMENT,
        "Taking a result from a null job should be rejected.");
    test::AssertEq(
        bag_take_encode_text_job_result(job, nullptr),
        BAG_INVALID_ARGUMENT,
        "Taking a result into a null PCM result should be rejected.");
}

void TestApiEncodeJobSuccessAndProgressAcrossModes() {
    const auto config_case = test::ConfigCases().front();
    const std::string text = "job-progress";

    const std::array<bag_transport_mode, 3> modes = {
        BAG_TRANSPORT_FLASH,
        BAG_TRANSPORT_PRO,
        BAG_TRANSPORT_ULTRA,
    };
    for (const bag_transport_mode mode : modes) {
        const auto encoder_config = MakeEncoderConfig(config_case, mode);

        bag_encode_job* job = nullptr;
        test::AssertEq(
            bag_start_encode_text_job(&encoder_config, text.c_str(), &job),
            BAG_OK,
            "Starting an async encode job should succeed for each transport mode.");
        const EncodeJobCompletion completion = WaitForEncodeJobTerminal(job);
        test::AssertEq(
            completion.final_progress.state,
            BAG_ENCODE_JOB_SUCCEEDED,
            "Async encode jobs should reach the succeeded terminal state.");
        test::AssertEq(
            completion.final_progress.terminal_code,
            BAG_OK,
            "Successful async encode jobs should publish an OK terminal code.");
        test::AssertEq(
            completion.final_progress.progress_0_to_1,
            1.0f,
            "Successful async encode jobs should finish at 100% progress.");
        test::AssertTrue(
            !completion.saw_phase_regression,
            "Encode job phases should not move backwards while polling.");
        if (mode == BAG_TRANSPORT_FLASH) {
            test::AssertTrue(
                completion.saw_postprocessing,
                "Flash encode jobs should report a postprocessing phase.");
        } else {
            test::AssertTrue(
                !completion.saw_postprocessing,
                "Non-flash encode jobs should not report a postprocessing phase.");
        }

        bag_pcm16_result expected_pcm{};
        bag_pcm16_result actual_pcm{};
        bag_pcm16_result repeated_pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, text.c_str(), &expected_pcm),
            BAG_OK,
            "The synchronous encode baseline should succeed for async-job comparisons.");
        test::AssertEq(
            bag_take_encode_text_job_result(job, &actual_pcm),
            BAG_OK,
            "Succeeded async jobs should expose their PCM result.");
        test::AssertEq(
            bag_take_encode_text_job_result(job, &repeated_pcm),
            BAG_OK,
            "Succeeded async jobs should allow the result to be taken repeatedly.");
        AssertPcmResultsEqual(
            expected_pcm,
            actual_pcm,
            "Async job PCM should match the one-shot encode output.");
        AssertPcmResultsEqual(
            expected_pcm,
            repeated_pcm,
            "Repeated async job result retrieval should stay stable.");

        bag_encode_job_progress repeated_progress{};
        test::AssertEq(
            bag_poll_encode_text_job(job, &repeated_progress),
            BAG_OK,
            "Polling after completion should remain safe.");
        test::AssertEq(
            repeated_progress.state,
            BAG_ENCODE_JOB_SUCCEEDED,
            "Polling after success should stay in the succeeded state.");
        test::AssertEq(
            bag_cancel_encode_text_job(job),
            BAG_OK,
            "Cancelling a completed async encode job should stay idempotently OK.");

        bag_free_pcm16_result(&expected_pcm);
        bag_free_pcm16_result(&actual_pcm);
        bag_free_pcm16_result(&repeated_pcm);
        bag_destroy_encode_text_job(job);
    }
}

void TestApiEncodeJobImmediateCancel() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
            BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);

    bag_encode_job* job = nullptr;
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, BuildLongJobText().c_str(), &job),
        BAG_OK,
        "Starting a long async flash job should succeed before immediate cancel.");
    test::AssertEq(
        bag_cancel_encode_text_job(job),
        BAG_OK,
        "Immediate cancel should succeed.");
    test::AssertEq(
        bag_cancel_encode_text_job(job),
        BAG_OK,
        "Repeated immediate cancel should stay idempotently OK.");

    const EncodeJobCompletion completion = WaitForEncodeJobTerminal(job);
    test::AssertEq(
        completion.final_progress.state,
        BAG_ENCODE_JOB_CANCELLED,
        "Immediately cancelled jobs should report a cancelled terminal state.");
    test::AssertEq(
        completion.final_progress.terminal_code,
        BAG_CANCELLED,
        "Immediately cancelled jobs should expose the cancelled terminal code.");

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_take_encode_text_job_result(job, &pcm),
        BAG_CANCELLED,
        "Cancelled jobs should not expose a PCM result.");
    test::AssertTrue(pcm.samples == nullptr, "Cancelled jobs should not allocate a PCM buffer.");
    test::AssertEq(pcm.sample_count, static_cast<size_t>(0), "Cancelled jobs should report no PCM samples.");

    bag_destroy_encode_text_job(job);
}

void TestApiEncodeJobCancelWhileRunning() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
            BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);

    bag_encode_job* job = nullptr;
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, BuildLongJobText().c_str(), &job),
        BAG_OK,
        "Starting a long async flash job should succeed before running cancel.");

    const EncodeJobCompletion completion = WaitForEncodeJobTerminal(job, true);
    test::AssertTrue(completion.saw_running, "The long async job should have entered the running state.");
    test::AssertEq(
        completion.final_progress.state,
        BAG_ENCODE_JOB_CANCELLED,
        "Cancelling while running should report the cancelled terminal state.");
    test::AssertEq(
        completion.final_progress.terminal_code,
        BAG_CANCELLED,
        "Cancelling while running should publish the cancelled terminal code.");

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_take_encode_text_job_result(job, &pcm),
        BAG_CANCELLED,
        "Running-cancelled jobs should not expose a PCM result.");
    bag_destroy_encode_text_job(job);
}

void TestApiEncodeJobDestroyWhileRunningIsSafe() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
            BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);

    bag_encode_job* job = nullptr;
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, BuildLongJobText().c_str(), &job),
        BAG_OK,
        "Starting a long async flash job should succeed before destroy.");
    bag_destroy_encode_text_job(job);
}

void TestApiEncodeJobPublishesMultipleIntermediateProgressUpdates() {
    const auto config_case = test::ConfigCases().front();
    const std::string long_text(4096, 'P');
    const std::array<bag_transport_mode, 3> modes = {
        BAG_TRANSPORT_FLASH,
        BAG_TRANSPORT_PRO,
        BAG_TRANSPORT_ULTRA,
    };

    for (const bag_transport_mode mode : modes) {
        const auto encoder_config = MakeEncoderConfig(config_case, mode);
        bag_encode_job* job = nullptr;
        test::AssertEq(
            bag_start_encode_text_job(&encoder_config, long_text.c_str(), &job),
            BAG_OK,
            "Starting a long async encode job should succeed for progress sampling tests.");

        const EncodeJobCompletion completion = WaitForEncodeJobTerminal(job);
        test::AssertEq(
            completion.final_progress.state,
            BAG_ENCODE_JOB_SUCCEEDED,
            "Long async encode jobs should complete successfully.");
        test::AssertTrue(
            completion.progress_advance_count >= 3,
            "Long async encode jobs should publish multiple intermediate progress updates.");
        test::AssertTrue(
            !completion.saw_phase_regression,
            "Long async encode jobs should keep phase order monotonic.");
        bag_destroy_encode_text_job(job);
    }
}

}  // namespace

namespace api_tests {

void RegisterApiAsyncTests(test::Runner& runner) {
    runner.Add("Api.EncodeJobRejectsInvalidArguments", TestApiEncodeJobRejectsInvalidArguments);
    runner.Add("Api.EncodeJobSuccessAndProgressAcrossModes", TestApiEncodeJobSuccessAndProgressAcrossModes);
    runner.Add("Api.EncodeJobImmediateCancel", TestApiEncodeJobImmediateCancel);
    runner.Add("Api.EncodeJobCancelWhileRunning", TestApiEncodeJobCancelWhileRunning);
    runner.Add("Api.EncodeJobDestroyWhileRunningIsSafe", TestApiEncodeJobDestroyWhileRunningIsSafe);
    runner.Add("Api.EncodeJobPublishesMultipleIntermediateProgressUpdates",
               TestApiEncodeJobPublishesMultipleIntermediateProgressUpdates);
}

}  // namespace api_tests
