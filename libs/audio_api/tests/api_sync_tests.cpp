#include <algorithm>
#include <array>
#include <string>
#include <vector>

#include "api_test_support.h"

namespace {

using namespace api_tests;

void TestApiFlashRoundTripAcrossCorpusAndConfigs() {
    AssertRoundTripAcrossCorpus(test::FlashCorpusCases(), BAG_TRANSPORT_FLASH);
}

void TestApiProRoundTripAcrossCorpusAndConfigs() {
    AssertRoundTripAcrossCorpus(test::ProCorpusCases(), BAG_TRANSPORT_PRO);
}

void TestApiUltraRoundTripAcrossCorpusAndConfigs() {
    AssertRoundTripAcrossCorpus(test::UltraCorpusCases(), BAG_TRANSPORT_ULTRA);
}

void TestApiEncodeRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case);
    bag_pcm16_result pcm{};

    test::AssertEq(
        bag_encode_text(nullptr, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Null encoder config should be rejected.");
    test::AssertEq(
        bag_encode_text(&encoder_config, nullptr, &pcm),
        BAG_INVALID_ARGUMENT,
        "Null text should be rejected.");
    test::AssertEq(
        bag_encode_text(&encoder_config, "A", nullptr),
        BAG_INVALID_ARGUMENT,
        "Null output PCM result should be rejected.");

    auto invalid_config = encoder_config;
    invalid_config.sample_rate_hz = 0;
    test::AssertEq(
        bag_encode_text(&invalid_config, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Zero sample rate should be rejected.");

    invalid_config = encoder_config;
    invalid_config.frame_samples = 0;
    test::AssertEq(
        bag_encode_text(&invalid_config, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Zero frame size should be rejected.");

    invalid_config = encoder_config;
    invalid_config.mode = static_cast<bag_transport_mode>(99);
    test::AssertEq(
        bag_encode_text(&invalid_config, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Unknown encoder mode should be rejected.");
}

void TestApiCreateDecoderRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto decoder_config = MakeDecoderConfig(config_case);
    bag_decoder* decoder = nullptr;

    test::AssertEq(
        bag_create_decoder(nullptr, &decoder),
        BAG_INVALID_ARGUMENT,
        "Null decoder config should be rejected.");
    test::AssertEq(
        bag_create_decoder(&decoder_config, nullptr),
        BAG_INVALID_ARGUMENT,
        "Null decoder output should be rejected.");

    auto invalid_config = decoder_config;
    invalid_config.sample_rate_hz = 0;
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Zero decoder sample rate should be rejected.");

    invalid_config = decoder_config;
    invalid_config.frame_samples = 0;
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Zero decoder frame size should be rejected.");

    invalid_config = decoder_config;
    invalid_config.mode = static_cast<bag_transport_mode>(99);
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Unknown decoder mode should be rejected.");
}

void TestApiPollAndResetLifecycle() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "RESET", &pcm),
        BAG_OK,
        "Encoding for lifecycle test should succeed.");

    bag_decoder* decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &decoder),
        BAG_OK,
        "Decoder creation for lifecycle test should succeed.");

    std::vector<char> text_buffer(256, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling before push should return not ready.");

    test::AssertEq(
        bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
        BAG_OK,
        "PCM push should succeed before reset.");
    bag_reset(decoder);
    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling after reset should return not ready.");

    bag_destroy_decoder(decoder);
    bag_free_pcm16_result(&pcm);
}

void TestApiModeSpecificValidation() {
    const auto config_case = test::ConfigCases().front();
    bag_pcm16_result pcm{};

    auto pro_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
    const auto pro_non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag_encode_text(&pro_config, pro_non_ascii.c_str(), &pcm),
        BAG_INVALID_ARGUMENT,
        "Pro mode should reject non-ASCII input.");
    test::AssertEq(
        bag_encode_text(&pro_config, test::BuildTooLongProCorpus().c_str(), &pcm),
        BAG_OK,
        "Pro mode should no longer inherit the compat single-frame limit.");
    bag_free_pcm16_result(&pcm);

    auto ultra_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
    test::AssertEq(
        bag_encode_text(&ultra_config, test::BuildTooLongUltraCorpus().c_str(), &pcm),
        BAG_OK,
        "Ultra mode should no longer inherit the compat single-frame limit.");
    bag_free_pcm16_result(&pcm);
}

void TestApiBoundarySuccessCases() {
    const auto config_case = test::ConfigCases().front();

    {
        const auto long_flash_text = std::string(513, 'F');
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, long_flash_text.c_str(), &pcm),
            BAG_OK,
            "Flash mode should not inherit the single-frame payload limit.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Flash long-text decode should succeed.");
        test::AssertEq(decoded.text, long_flash_text, "Flash long-text roundtrip should preserve text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_FLASH, "Flash long-text decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, test::BuildTooLongProCorpus().c_str(), &pcm),
            BAG_OK,
            "Pro mode should accept extended ASCII text beyond the old compat limit.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Pro boundary decode should succeed.");
        test::AssertEq(
            decoded.text,
            test::BuildTooLongProCorpus(),
            "Pro boundary roundtrip should preserve the extended ASCII text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_PRO, "Pro boundary decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, test::BuildTooLongUltraCorpus().c_str(), &pcm),
            BAG_OK,
            "Ultra mode should accept extended UTF-8 text beyond the old compat limit.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Ultra boundary decode should succeed.");
        test::AssertEq(
            decoded.text,
            test::BuildTooLongUltraCorpus(),
            "Ultra boundary roundtrip should preserve the extended UTF-8 text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_ULTRA, "Ultra boundary decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }
}

void TestApiChunkedPushRoundTripAcrossModes() {
    struct ChunkedCase {
        bag_transport_mode mode;
        std::string text;
    };
    const std::array<ChunkedCase, 3> cases = {{
        {BAG_TRANSPORT_FLASH, test::Utf8Literal(u8"Chunk-Flash-你好")},
        {BAG_TRANSPORT_PRO, "Chunk-Pro-123"},
        {BAG_TRANSPORT_ULTRA, test::Utf8Literal(u8"Chunk-Ultra-超级")},
    }};

    for (const auto& config_case : test::ConfigCases()) {
        for (const auto& item : cases) {
            const auto encoder_config = MakeEncoderConfig(config_case, item.mode);
            const auto decoder_config = MakeDecoderConfig(config_case, item.mode);
            bag_pcm16_result pcm{};
            test::AssertEq(
                bag_encode_text(&encoder_config, item.text.c_str(), &pcm),
                BAG_OK,
                "Chunked decode setup encode should succeed.");

            const std::size_t chunk_size =
                std::max<std::size_t>(1, pcm.sample_count / static_cast<std::size_t>(7));
            const auto decoded = DecodeViaApiInChunks(decoder_config, pcm, chunk_size);
            test::AssertEq(decoded.code, BAG_OK, "Chunked API decode should succeed.");
            test::AssertEq(decoded.text, item.text, "Chunked API decode should preserve text.");
            test::AssertEq(decoded.mode, item.mode, "Chunked API decode should preserve mode.");
            bag_free_pcm16_result(&pcm);
        }
    }
}

void TestApiPollResultBufferBoundaries() {
    const auto config_case = test::ConfigCases().front();
    const std::string text = "truncate-check";
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);
    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, text.c_str(), &pcm),
        BAG_OK,
        "Buffer-boundary setup encode should succeed.");

    {
        bag_decoder* decoder = nullptr;
        test::AssertEq(
            bag_create_decoder(&decoder_config, &decoder),
            BAG_OK,
            "Decoder creation should succeed for null-buffer polling.");
        test::AssertEq(
            bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
            BAG_OK,
            "PCM push should succeed before null-buffer polling.");

        bag_text_result result{};
        result.buffer = nullptr;
        result.buffer_size = text.size() + 1;
        test::AssertEq(
            bag_poll_result(decoder, &result),
            BAG_OK,
            "Polling with a null output buffer should still succeed.");
        test::AssertEq(result.text_size, text.size(), "Null-buffer polling should still publish full text size.");
        test::AssertEq(result.complete, 1, "Null-buffer polling should still mark the result complete.");
        test::AssertEq(result.mode, BAG_TRANSPORT_PRO, "Null-buffer polling should still preserve mode.");
        bag_destroy_decoder(decoder);
    }

    {
        bag_decoder* decoder = nullptr;
        test::AssertEq(
            bag_create_decoder(&decoder_config, &decoder),
            BAG_OK,
            "Decoder creation should succeed for zero-size-buffer polling.");
        test::AssertEq(
            bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
            BAG_OK,
            "PCM push should succeed before zero-size-buffer polling.");

        char sentinel[] = "keep";
        bag_text_result result{};
        result.buffer = sentinel;
        result.buffer_size = 0;
        test::AssertEq(
            bag_poll_result(decoder, &result),
            BAG_OK,
            "Polling with a zero-size output buffer should still succeed.");
        test::AssertEq(
            std::string(sentinel),
            std::string("keep"),
            "Zero-size output buffers should remain untouched.");
        test::AssertEq(result.text_size, text.size(), "Zero-size-buffer polling should still publish full text size.");
        test::AssertEq(result.complete, 1, "Zero-size-buffer polling should still mark the result complete.");
        bag_destroy_decoder(decoder);
    }

    {
        bag_decoder* decoder = nullptr;
        test::AssertEq(
            bag_create_decoder(&decoder_config, &decoder),
            BAG_OK,
            "Decoder creation should succeed for truncated-buffer polling.");
        test::AssertEq(
            bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
            BAG_OK,
            "PCM push should succeed before truncated-buffer polling.");

        std::array<char, 5> small_buffer = {'X', 'X', 'X', 'X', 'X'};
        bag_text_result result{};
        result.buffer = small_buffer.data();
        result.buffer_size = small_buffer.size();
        test::AssertEq(
            bag_poll_result(decoder, &result),
            BAG_OK,
            "Polling with a small output buffer should still succeed.");
        test::AssertEq(
            std::string(small_buffer.data()),
            text.substr(0, small_buffer.size() - 1),
            "Small output buffers should receive a stable truncated prefix.");
        test::AssertEq(result.text_size, text.size(), "Small-buffer polling should still publish full text size.");
        test::AssertEq(result.complete, 1, "Small-buffer polling should still mark the result complete.");
        bag_destroy_decoder(decoder);
    }

    bag_free_pcm16_result(&pcm);
}

void TestApiDecoderInputAndRepeatedPollBoundaries() {
    const auto config_case = test::ConfigCases().front();
    const std::string text = "decoder-boundary";
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, text.c_str(), &pcm),
        BAG_OK,
        "Decoder-boundary setup encode should succeed.");

    bag_decoder* decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &decoder),
        BAG_OK,
        "Decoder creation should succeed for decoder-boundary tests.");

    std::array<char, 64> text_buffer{};
    text_buffer[0] = 'X';
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    test::AssertEq(
        bag_poll_result(decoder, nullptr),
        BAG_INVALID_ARGUMENT,
        "Polling with a null text result target should be rejected.");
    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling before any PCM push should return not ready.");
    test::AssertEq(result.text_size, static_cast<std::size_t>(0), "Not-ready polling should clear text size.");
    test::AssertEq(result.complete, 0, "Not-ready polling should clear completion state.");
    test::AssertEq(result.confidence, 0.0f, "Not-ready polling should clear confidence.");
    test::AssertEq(result.mode, BAG_TRANSPORT_FLASH, "Not-ready polling should reset mode to the API default.");
    test::AssertEq(result.buffer[0], '\0', "Not-ready polling should clear the first output byte.");

    test::AssertEq(
        bag_push_pcm(decoder, nullptr, pcm.sample_count, 0),
        BAG_INVALID_ARGUMENT,
        "Null PCM pointers with non-zero sample counts should be rejected.");
    test::AssertEq(
        bag_push_pcm(decoder, pcm.samples, 0, 0),
        BAG_INVALID_ARGUMENT,
        "Zero-length PCM pushes should be rejected.");

    test::AssertEq(
        bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
        BAG_OK,
        "Valid PCM push should succeed.");
    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_OK,
        "Polling after a valid push should succeed.");
    test::AssertEq(
        std::string(result.buffer, result.text_size),
        text,
        "Successful polling should preserve the decoded text.");
    test::AssertEq(result.complete, 1, "Successful polling should mark the result complete.");
    test::AssertEq(result.mode, BAG_TRANSPORT_FLASH, "Successful polling should preserve the configured mode.");

    result.buffer[0] = 'Y';
    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling again after consuming the pending result should return not ready.");
    test::AssertEq(result.text_size, static_cast<std::size_t>(0), "Repeated not-ready polling should clear text size.");
    test::AssertEq(result.complete, 0, "Repeated not-ready polling should clear completion state.");
    test::AssertEq(result.buffer[0], '\0', "Repeated not-ready polling should clear the first output byte.");

    bag_reset(decoder);
    result.buffer[0] = 'Z';
    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling after reset should stay not ready.");
    test::AssertEq(result.buffer[0], '\0', "Polling after reset should clear the first output byte.");

    bag_destroy_decoder(decoder);
    bag_free_pcm16_result(&pcm);
}

void TestApiFreePcmResultIsIdempotent() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "free", &pcm),
        BAG_OK,
        "PCM result should be allocated for a successful encode.");
    test::AssertTrue(pcm.samples != nullptr, "PCM result buffer should be allocated.");
    test::AssertTrue(pcm.sample_count > 0, "PCM result sample count should be non-zero.");

    bag_free_pcm16_result(&pcm);
    test::AssertTrue(pcm.samples == nullptr, "PCM result samples should clear after free.");
    test::AssertEq(pcm.sample_count, static_cast<size_t>(0), "PCM result count should clear after free.");

    bag_free_pcm16_result(&pcm);
    test::AssertTrue(pcm.samples == nullptr, "PCM free should remain safe on a cleared result.");
    test::AssertEq(
        pcm.sample_count,
        static_cast<size_t>(0),
        "PCM sample count should remain zero on repeated free.");
}

void TestApiVersionMatchesRelease() {
    const char* version = bag_core_version();
    test::AssertTrue(version != nullptr, "Version pointer should not be null.");
    test::AssertEq(
        std::string(version),
        std::string(test::kExpectedCoreVersion),
        "C API should expose the current release version.");
}

void TestApiValidationHelpers() {
    bag_transport_mode mode = BAG_TRANSPORT_FLASH;
    test::AssertTrue(
        bag_try_parse_transport_mode("flash", &mode) != 0,
        "Mode parser should accept flash.");
    test::AssertEq(mode, BAG_TRANSPORT_FLASH, "Mode parser should map flash correctly.");
    test::AssertTrue(
        bag_try_parse_transport_mode("pro", &mode) != 0,
        "Mode parser should accept pro.");
    test::AssertEq(mode, BAG_TRANSPORT_PRO, "Mode parser should map pro correctly.");
    test::AssertTrue(
        bag_try_parse_transport_mode("ultra", &mode) != 0,
        "Mode parser should accept ultra.");
    test::AssertEq(mode, BAG_TRANSPORT_ULTRA, "Mode parser should map ultra correctly.");
    test::AssertTrue(
        bag_try_parse_transport_mode("warp", &mode) == 0,
        "Mode parser should reject unsupported values.");

    test::AssertEq(
        std::string(bag_transport_mode_name(BAG_TRANSPORT_PRO)),
        std::string("pro"),
        "Mode name helper should return the stable CLI spelling.");

    const auto config_case = test::ConfigCases().front();
    auto pro_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
    const auto pro_non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag_validate_encode_request(&pro_config, pro_non_ascii.c_str()),
        BAG_VALIDATION_PRO_ASCII_ONLY,
        "Validation helper should expose the pro ASCII-only rule.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_PRO_ASCII_ONLY),
        "ASCII",
        "Validation helper message should explain the ASCII-only rule.");
    test::AssertEq(
        bag_validate_encode_request(&pro_config, test::BuildTooLongProCorpus().c_str()),
        BAG_VALIDATION_OK,
        "Validation helper should reflect that pro no longer inherits the compat frame limit.");

    auto ultra_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
    test::AssertEq(
        bag_validate_encode_request(&ultra_config, test::BuildTooLongUltraCorpus().c_str()),
        BAG_VALIDATION_OK,
        "Validation helper should reflect that ultra no longer inherits the compat frame limit.");

    auto invalid_decoder = MakeDecoderConfig(config_case, static_cast<bag_transport_mode>(99));
    test::AssertEq(
        bag_validate_decoder_config(&invalid_decoder),
        BAG_VALIDATION_INVALID_MODE,
        "Decoder validation helper should reject unknown modes.");

    auto invalid_flash_encoder = MakeEncoderConfig(config_case);
    invalid_flash_encoder.flash_signal_profile = static_cast<bag_flash_signal_profile>(99);
    test::AssertEq(
        bag_validate_encode_request(&invalid_flash_encoder, "flash-style"),
        BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE,
        "Validation helper should reject unsupported flash signal profiles.");
    auto invalid_flash_decoder = MakeDecoderConfig(config_case);
    invalid_flash_decoder.flash_voicing_flavor = static_cast<bag_flash_voicing_flavor>(99);
    test::AssertEq(
        bag_validate_decoder_config(&invalid_flash_decoder),
        BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR,
        "Decoder validation helper should reject unsupported flash voicing flavors.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE),
        "signal profile",
        "Validation helper message should explain the flash signal-profile failure.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR),
        "voicing flavor",
        "Validation helper message should explain the flash voicing-flavor failure.");
    test::AssertContains(
        bag_error_code_message(BAG_INTERNAL),
        "Internal",
        "Error message helper should expose a stable internal-error prompt.");
}

}  // namespace

namespace api_tests {

void RegisterApiSyncTests(test::Runner& runner) {
    runner.Add("Api.FlashRoundTripAcrossCorpusAndConfigs", TestApiFlashRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.ProRoundTripAcrossCorpusAndConfigs", TestApiProRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.UltraRoundTripAcrossCorpusAndConfigs", TestApiUltraRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.EncodeRejectsInvalidArguments", TestApiEncodeRejectsInvalidArguments);
    runner.Add("Api.CreateDecoderRejectsInvalidArguments", TestApiCreateDecoderRejectsInvalidArguments);
    runner.Add("Api.PollAndResetLifecycle", TestApiPollAndResetLifecycle);
    runner.Add("Api.ModeSpecificValidation", TestApiModeSpecificValidation);
    runner.Add("Api.BoundarySuccessCases", TestApiBoundarySuccessCases);
    runner.Add("Api.ChunkedPushRoundTripAcrossModes", TestApiChunkedPushRoundTripAcrossModes);
    runner.Add("Api.PollResultBufferBoundaries", TestApiPollResultBufferBoundaries);
    runner.Add("Api.DecoderInputAndRepeatedPollBoundaries", TestApiDecoderInputAndRepeatedPollBoundaries);
    runner.Add("Api.FreePcmResultIsIdempotent", TestApiFreePcmResultIsIdempotent);
    runner.Add("Api.VersionMatchesRelease", TestApiVersionMatchesRelease);
    runner.Add("Api.ValidationHelpers", TestApiValidationHelpers);
}

}  // namespace api_tests
