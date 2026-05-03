#include <algorithm>
#include <chrono>
#include <cmath>
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

std::size_t SecondsToSampleCount(int sample_rate_hz, double seconds) {
    return sample_rate_hz > 0 && seconds > 0.0
               ? static_cast<std::size_t>(std::lround(static_cast<double>(sample_rate_hz) * seconds))
               : static_cast<std::size_t>(0);
}

bool IsUtf8ContinuationByte(unsigned char value) {
    return (value & 0xC0U) == 0x80U;
}

bool IsLikelyUtf8CodePointBoundaryAfter(const std::string& text, std::size_t byte_index) {
    return byte_index + static_cast<std::size_t>(1) >= text.size() ||
           !IsUtf8ContinuationByte(static_cast<unsigned char>(text[byte_index + static_cast<std::size_t>(1)]));
}

bool EndsWithUtf8Bytes(const std::string& text,
                       std::size_t byte_index,
                       unsigned char first,
                       unsigned char second,
                       unsigned char third) {
    if (byte_index < static_cast<std::size_t>(2)) {
        return false;
    }
    return static_cast<unsigned char>(text[byte_index - static_cast<std::size_t>(2)]) == first &&
           static_cast<unsigned char>(text[byte_index - static_cast<std::size_t>(1)]) == second &&
           static_cast<unsigned char>(text[byte_index]) == third;
}

std::size_t LitanyPauseSlotCountAfterByte(const std::string& text, std::size_t byte_index) {
    const unsigned char value = static_cast<unsigned char>(text[byte_index]);
    switch (value) {
    case static_cast<unsigned char>(' '):
    case static_cast<unsigned char>('\t'):
        return 3;
    case static_cast<unsigned char>('\n'):
    case static_cast<unsigned char>('\r'):
        return 6;
    case static_cast<unsigned char>(','):
    case static_cast<unsigned char>(';'):
    case static_cast<unsigned char>(':'):
        return 4;
    case static_cast<unsigned char>('.'):
    case static_cast<unsigned char>('!'):
    case static_cast<unsigned char>('?'):
        return 8;
    default:
        break;
    }

    if (EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x8C) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x9B) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x9A)) {
        return 4;
    }
    if (EndsWithUtf8Bytes(text, byte_index, 0xE3, 0x80, 0x82) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x81) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x9F)) {
        return 8;
    }
    if (byte_index + static_cast<std::size_t>(1) >= text.size() ||
        !IsLikelyUtf8CodePointBoundaryAfter(text, byte_index)) {
        return 1;
    }

    const std::size_t cadence_position = byte_index + static_cast<std::size_t>(1);
    if ((cadence_position % static_cast<std::size_t>(12)) == 0) {
        return 5;
    }
    return 1;
}

std::size_t LitanyPauseSlotCount(const std::string& text) {
    std::size_t pause_slots = 0;
    for (std::size_t byte_index = 0; byte_index < text.size(); ++byte_index) {
        pause_slots += static_cast<std::size_t>(14) + LitanyPauseSlotCountAfterByte(text, byte_index);
    }
    return pause_slots;
}

std::size_t LitanyPauseSampleCount(const std::string& text, std::size_t silence_slot_samples) {
    return LitanyPauseSlotCount(text) * silence_slot_samples;
}

std::size_t ZealBitSampleCount(std::size_t frame_samples, std::size_t bit_position, bool force_burst_after_pause) {
    if (frame_samples == 0) {
        return 0;
    }
    if (force_burst_after_pause) {
        return std::max(static_cast<std::size_t>(1), frame_samples / static_cast<std::size_t>(2));
    }
    switch (bit_position % static_cast<std::size_t>(16)) {
    case 0:
    case 1:
    case 3:
    case 5:
    case 8:
    case 9:
    case 11:
    case 14:
        return std::max(static_cast<std::size_t>(1), frame_samples / static_cast<std::size_t>(2));
    case 2:
    case 6:
    case 10:
    case 13:
        return std::max(static_cast<std::size_t>(1),
                        frame_samples * static_cast<std::size_t>(5) / static_cast<std::size_t>(8));
    case 4:
    case 15:
        return std::max(static_cast<std::size_t>(1),
                        frame_samples * static_cast<std::size_t>(3) / static_cast<std::size_t>(4));
    case 7:
    case 12:
    default:
        return std::max(static_cast<std::size_t>(1), frame_samples);
    }
}

bool IsZealStrongPauseByte(const std::string& text, std::size_t byte_index) {
    const unsigned char value = static_cast<unsigned char>(text[byte_index]);
    switch (value) {
    case static_cast<unsigned char>('\n'):
    case static_cast<unsigned char>('\r'):
    case static_cast<unsigned char>('.'):
    case static_cast<unsigned char>('!'):
    case static_cast<unsigned char>('?'):
        return true;
    default:
        break;
    }
    return EndsWithUtf8Bytes(text, byte_index, 0xE3, 0x80, 0x82) ||
           EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x81) ||
           EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x9F);
}

std::size_t ZealPauseSlotCountAfterByte(const std::string& text, std::size_t byte_index) {
    if (!IsLikelyUtf8CodePointBoundaryAfter(text, byte_index)) {
        return 0;
    }
    const unsigned char value = static_cast<unsigned char>(text[byte_index]);
    switch (value) {
    case static_cast<unsigned char>('\n'):
    case static_cast<unsigned char>('\r'):
        return 5;
    case static_cast<unsigned char>('.'):
    case static_cast<unsigned char>('!'):
    case static_cast<unsigned char>('?'):
        return 4;
    case static_cast<unsigned char>(','):
    case static_cast<unsigned char>(';'):
    case static_cast<unsigned char>(':'):
        return 1;
    default:
        break;
    }
    if (EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x8C) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x9B) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x9A)) {
        return 1;
    }
    if (IsZealStrongPauseByte(text, byte_index)) {
        return 4;
    }
    return 0;
}

std::size_t ZealPayloadSampleCount(const std::string& text, std::size_t frame_samples) {
    std::size_t sample_count = 0;
    bool force_burst_after_pause = false;
    for (std::size_t byte_index = 0; byte_index < text.size(); ++byte_index) {
        for (int bit_index = 0; bit_index < 8; ++bit_index) {
            const std::size_t bit_position =
                byte_index * static_cast<std::size_t>(8) + static_cast<std::size_t>(bit_index);
            sample_count += ZealBitSampleCount(frame_samples, bit_position, force_burst_after_pause);
            force_burst_after_pause = false;
        }
        const std::size_t pause_slots = ZealPauseSlotCountAfterByte(text, byte_index);
        sample_count += pause_slots * frame_samples;
        if (pause_slots > 0) {
            force_burst_after_pause = true;
        }
    }
    return sample_count;
}

std::size_t ExpectedFlashSampleCount(const std::string& text,
                                     const test::ConfigCase& config_case,
                                     bag_flash_signal_profile flash_signal_profile,
                                     bag_flash_voicing_flavor flash_voicing_flavor) {
    const std::size_t frame_samples =
        config_case.frame_samples > 0 ? static_cast<std::size_t>(config_case.frame_samples) : static_cast<std::size_t>(0);
    std::size_t payload_samples_per_bit = frame_samples;
    if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_STEADY) {
        payload_samples_per_bit = std::max(static_cast<std::size_t>(1),
                                           frame_samples * static_cast<std::size_t>(15) /
                                               static_cast<std::size_t>(16));
    } else if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_LITANY) {
        payload_samples_per_bit = RoundHalfUpFrameScale(config_case.frame_samples, 6, 1);
    } else if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_HOSTILE) {
        payload_samples_per_bit = std::max(static_cast<std::size_t>(1),
                                           frame_samples * static_cast<std::size_t>(7) /
                                               static_cast<std::size_t>(8));
    } else if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_ZEAL) {
        payload_samples_per_bit = std::max(static_cast<std::size_t>(1),
                                           frame_samples / static_cast<std::size_t>(2));
    } else if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_VOID) {
        payload_samples_per_bit = std::max(static_cast<std::size_t>(1),
                                           frame_samples * static_cast<std::size_t>(5) /
                                               static_cast<std::size_t>(2));
    }
    const std::size_t payload_samples_per_litany_silence_slot =
        flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_LITANY
            ? frame_samples
            : payload_samples_per_bit;
    const std::size_t leading_nonpayload_samples =
        flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_LITANY
            ? SecondsToSampleCount(config_case.sample_rate_hz, 1.35)
            : frame_samples * static_cast<std::size_t>(3);
    const std::size_t trailing_nonpayload_samples =
        flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_LITANY
            ? SecondsToSampleCount(config_case.sample_rate_hz, 1.15)
            : frame_samples * static_cast<std::size_t>(3);
    const bool uses_litany_pauses = flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_LITANY;
    const bool uses_zeal_layout = flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_ZEAL;
    return (uses_zeal_layout
                ? ZealPayloadSampleCount(text, frame_samples)
                : text.size() * static_cast<std::size_t>(8) * payload_samples_per_bit) +
           (uses_litany_pauses
                ? LitanyPauseSampleCount(text, payload_samples_per_litany_silence_slot)
                : static_cast<std::size_t>(0)) +
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
    std::vector<char> raw_bytes_hex_buffer(4096, '\0');
    std::vector<char> raw_bits_binary_buffer(32768, '\0');
    bag_decode_result result{};
    result.text_buffer = text_buffer.data();
    result.text_buffer_size = text_buffer.size();
    result.raw_bytes_hex_buffer = raw_bytes_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_bytes_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_binary_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_binary_buffer.size();

    const auto poll_code = bag_poll_decode_result(decoder, &result);
    bag_destroy_decoder(decoder);

    DecodeResult out{};
    out.code = poll_code;
    out.text.assign(text_buffer.data(), result.text_size);
    out.raw_bytes_hex.assign(raw_bytes_hex_buffer.data(), result.raw_bytes_hex_size);
    out.raw_bits_binary.assign(raw_bits_binary_buffer.data(), result.raw_bits_binary_size);
    out.text_status = result.text_decode_status;
    out.raw_payload_available = result.raw_payload_available != 0;
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
    std::vector<char> raw_bytes_hex_buffer(4096, '\0');
    std::vector<char> raw_bits_binary_buffer(32768, '\0');
    bag_decode_result result{};
    result.text_buffer = text_buffer.data();
    result.text_buffer_size = text_buffer.size();
    result.raw_bytes_hex_buffer = raw_bytes_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_bytes_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_binary_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_binary_buffer.size();

    const auto poll_code = bag_poll_decode_result(decoder, &result);
    bag_destroy_decoder(decoder);

    DecodeResult out{};
    out.code = poll_code;
    out.text.assign(text_buffer.data(), result.text_size);
    out.raw_bytes_hex.assign(raw_bytes_hex_buffer.data(), result.raw_bytes_hex_size);
    out.raw_bits_binary.assign(raw_bits_binary_buffer.data(), result.raw_bits_binary_size);
    out.text_status = result.text_decode_status;
    out.raw_payload_available = result.raw_payload_available != 0;
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
