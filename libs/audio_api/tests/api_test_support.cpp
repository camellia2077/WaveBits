#include <algorithm>
#include <chrono>
#include <string>
#include <thread>
#include <vector>

#include "api_test_support.h"

namespace api_tests {

bag_encoder_config MakeEncoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode,
                                     bag_flash_signal_profile flash_signal_profile,
                                     bag_flash_voicing_flavor flash_voicing_flavor) {
    bag_encoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.flash_signal_profile = flash_signal_profile;
    config.flash_voicing_flavor = flash_voicing_flavor;
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode,
                                     bag_flash_signal_profile flash_signal_profile,
                                     bag_flash_voicing_flavor flash_voicing_flavor) {
    bag_decoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.flash_signal_profile = flash_signal_profile;
    config.flash_voicing_flavor = flash_voicing_flavor;
    config.reserved = 0;
    return config;
}

std::size_t RoundHalfUpFrameScale(int frame_samples, int numerator, int denominator) {
    return frame_samples > 0
               ? static_cast<std::size_t>((frame_samples * numerator + (denominator / 2)) / denominator)
               : static_cast<std::size_t>(0);
}

std::size_t ExpectedFlashSampleCount(const std::string& text,
                                     const test::ConfigCase& config_case,
                                     bag_flash_signal_profile flash_signal_profile,
                                     bag_flash_voicing_flavor flash_voicing_flavor) {
    const std::size_t frame_samples =
        config_case.frame_samples > 0 ? static_cast<std::size_t>(config_case.frame_samples) : static_cast<std::size_t>(0);
    const std::size_t payload_samples_per_bit =
        flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT
            ? RoundHalfUpFrameScale(config_case.frame_samples, 3, 1)
            : frame_samples;
    const std::size_t leading_nonpayload_samples =
        flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT
            ? frame_samples * static_cast<std::size_t>(16)
            : frame_samples * static_cast<std::size_t>(3);
    const std::size_t trailing_nonpayload_samples =
        flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT
            ? frame_samples * static_cast<std::size_t>(8)
            : frame_samples * static_cast<std::size_t>(3);
    return text.size() * static_cast<std::size_t>(8) * payload_samples_per_bit +
           leading_nonpayload_samples +
           trailing_nonpayload_samples;
}

DecodeResult DecodeViaApi(const bag_decoder_config& config, const bag_pcm16_result& pcm) {
    bag_decoder* decoder = nullptr;
    const auto create_code = bag_create_decoder(&config, &decoder);
    test::AssertEq(create_code, BAG_OK, "Decoder creation should succeed.");
    test::AssertTrue(decoder != nullptr, "Decoder should not be null after creation.");

    const auto push_code = bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0);
    test::AssertEq(push_code, BAG_OK, "PCM push should succeed.");

    std::vector<char> text_buffer(4096, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    const auto poll_code = bag_poll_result(decoder, &result);
    bag_destroy_decoder(decoder);

    DecodeResult out{};
    out.code = poll_code;
    out.text.assign(text_buffer.data(), result.text_size);
    out.mode = result.mode;
    return out;
}

DecodeResult DecodeViaApiInChunks(const bag_decoder_config& config,
                                  const bag_pcm16_result& pcm,
                                  std::size_t chunk_sample_count) {
    bag_decoder* decoder = nullptr;
    const auto create_code = bag_create_decoder(&config, &decoder);
    test::AssertEq(create_code, BAG_OK, "Chunked decoder creation should succeed.");
    test::AssertTrue(decoder != nullptr, "Chunked decoder should not be null after creation.");

    const std::size_t normalized_chunk_sample_count = std::max<std::size_t>(1, chunk_sample_count);
    for (std::size_t offset = 0; offset < pcm.sample_count; offset += normalized_chunk_sample_count) {
        const std::size_t chunk_size =
            std::min(normalized_chunk_sample_count, pcm.sample_count - offset);
        const auto push_code =
            bag_push_pcm(decoder, pcm.samples + offset, chunk_size, static_cast<int64_t>(offset));
        test::AssertEq(push_code, BAG_OK, "Chunked PCM push should succeed.");
    }

    std::vector<char> text_buffer(4096, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    const auto poll_code = bag_poll_result(decoder, &result);
    bag_destroy_decoder(decoder);

    DecodeResult out{};
    out.code = poll_code;
    out.text.assign(text_buffer.data(), result.text_size);
    out.mode = result.mode;
    return out;
}

bool IsEncodeJobTerminal(bag_encode_job_state state) {
    return state == BAG_ENCODE_JOB_SUCCEEDED ||
           state == BAG_ENCODE_JOB_FAILED ||
           state == BAG_ENCODE_JOB_CANCELLED;
}

void AssertPcmResultsEqual(const bag_pcm16_result& lhs,
                           const bag_pcm16_result& rhs,
                           const std::string& message) {
    test::AssertEq(lhs.sample_count, rhs.sample_count, message + " sample count should match.");
    const bool samples_match =
        lhs.sample_count == rhs.sample_count &&
        (lhs.sample_count == 0 ||
         std::equal(lhs.samples, lhs.samples + lhs.sample_count, rhs.samples));
    test::AssertTrue(samples_match, message + " samples should match exactly.");
}

EncodeJobCompletion WaitForEncodeJobTerminal(bag_encode_job* job,
                                            bool cancel_when_running) {
    test::AssertTrue(job != nullptr, "Job handle should not be null while waiting for completion.");

    EncodeJobCompletion completion{};
    float previous_progress = 0.0f;
    bool first_poll = true;
    bool cancel_requested = false;
    bag_encode_job_phase previous_phase = BAG_ENCODE_JOB_PHASE_PREPARING_INPUT;
    for (int attempt = 0; attempt < 20000; ++attempt) {
        bag_encode_job_progress progress{};
        test::AssertEq(
            bag_poll_encode_text_job(job, &progress),
            BAG_OK,
            "Polling an active encode job should succeed.");
        if (!first_poll) {
            test::AssertTrue(
                progress.progress_0_to_1 + 1e-6f >= previous_progress,
                "Encode job progress should stay monotonic.");
            if (progress.progress_0_to_1 > previous_progress + 1e-6f) {
                ++completion.progress_advance_count;
            }
            completion.saw_phase_regression =
                completion.saw_phase_regression ||
                static_cast<int>(progress.phase) < static_cast<int>(previous_phase);
        }
        first_poll = false;
        previous_progress = progress.progress_0_to_1;
        previous_phase = progress.phase;
        completion.saw_running = completion.saw_running || progress.state == BAG_ENCODE_JOB_RUNNING;
        completion.saw_postprocessing =
            completion.saw_postprocessing || progress.phase == BAG_ENCODE_JOB_PHASE_POSTPROCESSING;

        if (cancel_when_running && !cancel_requested && progress.state == BAG_ENCODE_JOB_RUNNING) {
            test::AssertEq(
                bag_cancel_encode_text_job(job),
                BAG_OK,
                "Cancelling a running encode job should succeed.");
            cancel_requested = true;
        }

        if (IsEncodeJobTerminal(progress.state)) {
            completion.final_progress = progress;
            return completion;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }

    test::Fail("Encode job did not reach a terminal state before timeout.");
    return completion;
}

std::string BuildLongJobText() {
    return std::string(256, 'J');
}

void AssertRoundTripAcrossCorpus(const std::vector<test::CorpusCase>& corpus,
                                 bag_transport_mode mode) {
    for (const auto& config_case : test::ConfigCases()) {
        const auto encoder_config = MakeEncoderConfig(config_case, mode);
        const auto decoder_config = MakeDecoderConfig(config_case, mode);

        for (const auto& corpus_case : corpus) {
            bag_pcm16_result pcm{};
            const auto encode_code =
                bag_encode_text(&encoder_config, corpus_case.text.c_str(), &pcm);
            test::AssertEq(encode_code, BAG_OK, "C API encode should succeed across corpus and configs.");

            const auto decoded = DecodeViaApi(decoder_config, pcm);
            test::AssertEq(decoded.code, BAG_OK, "Polling should succeed after valid push.");
            test::AssertEq(decoded.text, corpus_case.text, "API roundtrip should preserve original text.");
            test::AssertEq(decoded.mode, mode, "API result mode should match the configured mode.");
            bag_free_pcm16_result(&pcm);
        }
    }
}

}  // namespace api_tests
