#include <string>
#include <vector>

#include "bag_api.h"
#include "test_framework.h"
#include "test_vectors.h"

namespace {

struct DecodeResult {
    bag_error_code code = BAG_INTERNAL;
    std::string text;
    bag_transport_mode mode = BAG_TRANSPORT_FLASH;
};

bag_encoder_config MakeEncoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH) {
    bag_encoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH) {
    bag_decoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.reserved = 0;
    return config;
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
    test::AssertEq(
        bag_encode_text(&pro_config, u8"中文", &pcm),
        BAG_INVALID_ARGUMENT,
        "Pro mode should reject non-ASCII input.");
    test::AssertEq(
        bag_encode_text(&pro_config, test::BuildTooLongProCorpus().c_str(), &pcm),
        BAG_INVALID_ARGUMENT,
        "Pro mode should reject payloads above the single-frame limit.");

    auto ultra_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
    test::AssertEq(
        bag_encode_text(&ultra_config, test::BuildTooLongUltraCorpus().c_str(), &pcm),
        BAG_INVALID_ARGUMENT,
        "Ultra mode should reject payloads above the single-frame limit.");
}

void TestApiBoundarySuccessCases() {
    const auto config_case = test::ConfigCases().front();

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, test::BuildMaxProCorpus().c_str(), &pcm),
            BAG_OK,
            "Pro mode should accept the max single-frame ASCII corpus.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Pro boundary decode should succeed.");
        test::AssertEq(decoded.text, test::BuildMaxProCorpus(), "Pro boundary roundtrip should preserve text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_PRO, "Pro boundary decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, test::BuildMaxUltraCorpus().c_str(), &pcm),
            BAG_OK,
            "Ultra mode should accept the max 512-byte UTF-8 corpus.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Ultra boundary decode should succeed.");
        test::AssertEq(
            decoded.text,
            test::BuildMaxUltraCorpus(),
            "Ultra boundary roundtrip should preserve UTF-8 text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_ULTRA, "Ultra boundary decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }
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
    test::AssertEq(
        bag_validate_encode_request(&pro_config, u8"中文"),
        BAG_VALIDATION_PRO_ASCII_ONLY,
        "Validation helper should expose the pro ASCII-only rule.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_PRO_ASCII_ONLY),
        "ASCII",
        "Validation helper message should explain the ASCII-only rule.");

    auto ultra_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
    test::AssertEq(
        bag_validate_encode_request(&ultra_config, test::BuildTooLongUltraCorpus().c_str()),
        BAG_VALIDATION_PAYLOAD_TOO_LARGE,
        "Validation helper should expose the single-frame size limit.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_PAYLOAD_TOO_LARGE),
        "512-byte",
        "Validation helper message should mention the single-frame size limit.");

    auto invalid_decoder = MakeDecoderConfig(config_case, static_cast<bag_transport_mode>(99));
    test::AssertEq(
        bag_validate_decoder_config(&invalid_decoder),
        BAG_VALIDATION_INVALID_MODE,
        "Decoder validation helper should reject unknown modes.");
    test::AssertContains(
        bag_error_code_message(BAG_INTERNAL),
        "Internal",
        "Error message helper should expose a stable internal-error prompt.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Api.FlashRoundTripAcrossCorpusAndConfigs", TestApiFlashRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.ProRoundTripAcrossCorpusAndConfigs", TestApiProRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.UltraRoundTripAcrossCorpusAndConfigs", TestApiUltraRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.EncodeRejectsInvalidArguments", TestApiEncodeRejectsInvalidArguments);
    runner.Add("Api.CreateDecoderRejectsInvalidArguments", TestApiCreateDecoderRejectsInvalidArguments);
    runner.Add("Api.PollAndResetLifecycle", TestApiPollAndResetLifecycle);
    runner.Add("Api.ModeSpecificValidation", TestApiModeSpecificValidation);
    runner.Add("Api.BoundarySuccessCases", TestApiBoundarySuccessCases);
    runner.Add("Api.FreePcmResultIsIdempotent", TestApiFreePcmResultIsIdempotent);
    runner.Add("Api.VersionMatchesRelease", TestApiVersionMatchesRelease);
    runner.Add("Api.ValidationHelpers", TestApiValidationHelpers);
    return runner.Run();
}
