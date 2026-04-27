#include <algorithm>
#include <array>
#include <string>
#include <vector>

#include "api_test_support.h"

namespace {

using namespace api_tests;

std::vector<std::string> SplitSpaceSeparatedTokens(const std::string& value) {
    std::vector<std::string> tokens;
    std::string current;
    for (const char ch : value) {
        if (ch == ' ') {
            if (!current.empty()) {
                tokens.push_back(current);
                current.clear();
            }
            continue;
        }
        current.push_back(ch);
    }
    if (!current.empty()) {
        tokens.push_back(current);
    }
    return tokens;
}

std::vector<std::string> SplitLineSeparatedTokens(const std::string& value) {
    std::vector<std::string> tokens;
    std::string current;
    for (const char ch : value) {
        if (ch == '\n') {
            tokens.push_back(current);
            current.clear();
            continue;
        }
        current.push_back(ch);
    }
    if (!current.empty()) {
        tokens.push_back(current);
    }
    return tokens;
}

void AssertFollowTimelineIsContinuous(
    const bag_payload_follow_data& follow_data,
    const std::vector<bag_payload_follow_binary_group_entry>& binary_entries) {
    if (!follow_data.available) {
        test::Fail("Follow data should be available.");
    }
    if (binary_entries.empty()) {
        return;
    }

    test::AssertEq(
        binary_entries.front().start_sample,
        follow_data.payload_begin_sample,
        "Binary timeline should begin at the payload start sample.");
    std::size_t covered_samples = 0;
    std::size_t expected_start = follow_data.payload_begin_sample;
    for (const auto& entry : binary_entries) {
        test::AssertEq(
            entry.start_sample,
            expected_start,
            "Binary timeline entries should be contiguous without gaps.");
        expected_start += entry.sample_count;
        covered_samples += entry.sample_count;
    }
    test::AssertEq(
        covered_samples,
        follow_data.payload_sample_count,
        "Binary timeline should cover the full payload sample range.");
    test::AssertTrue(
        follow_data.payload_begin_sample + follow_data.payload_sample_count <=
            follow_data.total_pcm_sample_count,
        "Payload follow data should stay within the rendered PCM sample range.");
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

void TestApiDecodeResultPublishesRawPayloadAcrossModes() {
    struct RawCase {
        bag_transport_mode mode;
        std::string text;
        bag_flash_signal_profile signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST;
        bag_flash_voicing_flavor voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST;
    };
    const std::array<RawCase, 6> cases = {{
        {BAG_TRANSPORT_FLASH, "FlashRaw"},
        {BAG_TRANSPORT_FLASH,
         test::Utf8Literal(u8"Flash深仪"),
         BAG_FLASH_SIGNAL_PROFILE_DEEP_RITUAL,
         BAG_FLASH_VOICING_FLAVOR_DEEP_RITUAL},
        {BAG_TRANSPORT_FLASH,
         "MidRitual",
         BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
         BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT},
        {BAG_TRANSPORT_PRO, "PRO-ASCII-123"},
        {BAG_TRANSPORT_ULTRA, "UltraRaw"},
        {BAG_TRANSPORT_ULTRA, test::Utf8Literal(u8"Ultra原始")},
    }};

    for (const auto& config_case : test::ConfigCases()) {
        for (const auto& item : cases) {
            const auto encoder_config = MakeEncoderConfig(
                config_case,
                item.mode,
                item.signal_profile,
                item.voicing_flavor);
            const auto decoder_config = MakeDecoderConfig(
                config_case,
                item.mode,
                item.signal_profile,
                item.voicing_flavor);
            bag_pcm16_result pcm{};
            test::AssertEq(
                bag_encode_text(&encoder_config, item.text.c_str(), &pcm),
                BAG_OK,
                "Raw-payload setup encode should succeed.");
            const auto decoded = DecodeViaApi(decoder_config, pcm);
            test::AssertEq(decoded.code, BAG_OK, "Raw-payload decode should succeed.");
            test::AssertTrue(decoded.raw_payload_available, "Raw payload should be available after decode.");
            test::AssertEq(decoded.text_status, BAG_DECODE_CONTENT_STATUS_OK,
                           "Structured decode should keep successful text status.");
            test::AssertTrue(!decoded.raw_bytes_hex.empty() || item.text.empty(),
                             "Raw bytes hex should be populated for non-empty payloads.");
            test::AssertTrue(!decoded.raw_bits_binary.empty() || item.text.empty(),
                             "Raw bits binary should be populated for non-empty payloads.");
            test::AssertEq(decoded.mode, item.mode, "Raw-payload decode should preserve mode.");
            bag_free_pcm16_result(&pcm);
        }
    }
}

void TestApiDecodeResultBufferBoundaries() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);
    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "RAW", &pcm),
        BAG_OK,
        "Raw-boundary setup encode should succeed.");

    bag_decoder* decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &decoder),
        BAG_OK,
        "Decoder creation should succeed for raw-boundary polling.");
    test::AssertEq(
        bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
        BAG_OK,
        "PCM push should succeed before raw-boundary polling.");

    std::array<char, 2> text_buffer = {'X', 'X'};
    std::array<char, 4> raw_hex_buffer = {'Y', 'Y', 'Y', 'Y'};
    std::array<char, 6> raw_bits_buffer = {'Z', 'Z', 'Z', 'Z', 'Z', 'Z'};
    bag_decode_result result{};
    result.text_buffer = text_buffer.data();
    result.text_buffer_size = text_buffer.size();
    result.raw_bytes_hex_buffer = raw_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_buffer.size();

    test::AssertEq(
        bag_poll_decode_result(decoder, &result),
        BAG_OK,
        "Polling structured decode result should succeed.");
    test::AssertEq(
        result.text_decode_status,
        BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL,
        "Small text buffers should report buffer-too-small.");
    test::AssertEq(
        result.raw_bytes_hex_status,
        BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL,
        "Small raw hex buffers should report buffer-too-small.");
    test::AssertEq(
        result.raw_bits_binary_status,
        BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL,
        "Small raw binary buffers should report buffer-too-small.");
    test::AssertTrue(
        result.text_size == std::string("RAW").size(),
        "Structured decode should publish the full text size.");
    test::AssertTrue(
        result.raw_bytes_hex_size >= static_cast<std::size_t>(8),
        "Structured decode should publish the full raw hex size.");
    test::AssertTrue(
        result.raw_bits_binary_size >= static_cast<std::size_t>(26),
        "Structured decode should publish the full raw binary size.");

    bag_destroy_decoder(decoder);
    bag_free_pcm16_result(&pcm);
}

void TestApiStructuredEncodePublishesFollowAcrossModes() {
    struct FollowCase {
        bag_transport_mode mode;
        std::string text;
        bag_flash_signal_profile signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST;
        bag_flash_voicing_flavor voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST;
    };
    const std::array<FollowCase, 5> cases = {{
        {BAG_TRANSPORT_FLASH, "Flash"},
        {BAG_TRANSPORT_FLASH, "Deep", BAG_FLASH_SIGNAL_PROFILE_DEEP_RITUAL, BAG_FLASH_VOICING_FLAVOR_DEEP_RITUAL},
        {BAG_TRANSPORT_PRO, "PRO-123"},
        {BAG_TRANSPORT_ULTRA, "Ultra"},
        {BAG_TRANSPORT_ULTRA, test::Utf8Literal(u8"超频")},
    }};

    for (const auto& config_case : test::ConfigCases()) {
        for (const auto& item : cases) {
            const auto encoder_config =
                MakeEncoderConfig(config_case, item.mode, item.signal_profile, item.voicing_flavor);
            std::array<char, 4096> raw_hex_buffer{};
            std::array<char, 32768> raw_bits_buffer{};
            std::array<bag_payload_follow_byte_entry, 2048> byte_entries{};
            std::array<bag_payload_follow_binary_group_entry, 4096> binary_entries{};
            bag_encode_result result{};
            result.raw_bytes_hex_buffer = raw_hex_buffer.data();
            result.raw_bytes_hex_buffer_size = raw_hex_buffer.size();
            result.raw_bits_binary_buffer = raw_bits_buffer.data();
            result.raw_bits_binary_buffer_size = raw_bits_buffer.size();
            result.follow_data.byte_timeline_buffer = byte_entries.data();
            result.follow_data.byte_timeline_buffer_count = byte_entries.size();
            result.follow_data.binary_group_timeline_buffer = binary_entries.data();
            result.follow_data.binary_group_timeline_buffer_count = binary_entries.size();

            test::AssertEq(
                bag_encode_text_with_follow(&encoder_config, item.text.c_str(), &result),
                BAG_OK,
                "Structured encode should succeed.");
            test::AssertTrue(result.raw_payload_available != 0, "Structured encode should publish raw payload.");
            test::AssertTrue(result.follow_data.available != 0, "Structured encode should publish follow data.");
            test::AssertTrue(result.sample_count > 0, "Structured encode should return PCM samples.");

            const auto hex_tokens =
                SplitSpaceSeparatedTokens(std::string(raw_hex_buffer.data(), result.raw_bytes_hex_size));
            const auto expected_binary_group_count =
                item.mode == BAG_TRANSPORT_FLASH
                    ? hex_tokens.size() * static_cast<std::size_t>(8)
                    : hex_tokens.size() * static_cast<std::size_t>(2);
            test::AssertEq(
                result.follow_data.byte_timeline_count,
                hex_tokens.size(),
                "Byte timeline count should match payload byte count.");
            test::AssertEq(
                result.follow_data.binary_group_timeline_count,
                expected_binary_group_count,
                "Binary group timeline count should match the transport-specific grouping.");
            AssertFollowTimelineIsContinuous(
                result.follow_data,
                std::vector<bag_payload_follow_binary_group_entry>(
                    binary_entries.begin(),
                    binary_entries.begin() + result.follow_data.binary_group_timeline_count));

            bag_free_encode_result(&result);
        }
    }
}

void TestApiEncodeAndDecodeFollowStayAligned() {
    const std::array<bag_transport_mode, 3> modes = {
        BAG_TRANSPORT_FLASH,
        BAG_TRANSPORT_PRO,
        BAG_TRANSPORT_ULTRA,
    };

    for (const auto& config_case : test::ConfigCases()) {
        for (const auto mode : modes) {
            const auto encoder_config = MakeEncoderConfig(config_case, mode);
            const auto decoder_config = MakeDecoderConfig(config_case, mode);
            std::array<char, 4096> encode_raw_hex_buffer{};
            std::array<char, 32768> encode_raw_bits_buffer{};
            std::array<bag_payload_follow_byte_entry, 2048> encode_byte_entries{};
            std::array<bag_payload_follow_binary_group_entry, 4096> encode_binary_entries{};
            bag_encode_result encode_result{};
            encode_result.raw_bytes_hex_buffer = encode_raw_hex_buffer.data();
            encode_result.raw_bytes_hex_buffer_size = encode_raw_hex_buffer.size();
            encode_result.raw_bits_binary_buffer = encode_raw_bits_buffer.data();
            encode_result.raw_bits_binary_buffer_size = encode_raw_bits_buffer.size();
            encode_result.follow_data.byte_timeline_buffer = encode_byte_entries.data();
            encode_result.follow_data.byte_timeline_buffer_count = encode_byte_entries.size();
            encode_result.follow_data.binary_group_timeline_buffer = encode_binary_entries.data();
            encode_result.follow_data.binary_group_timeline_buffer_count = encode_binary_entries.size();
            test::AssertEq(
                bag_encode_text_with_follow(&encoder_config, "FOLLOW", &encode_result),
                BAG_OK,
                "Structured encode should succeed before decode follow comparison.");

            bag_decoder* decoder = nullptr;
            test::AssertEq(bag_create_decoder(&decoder_config, &decoder), BAG_OK, "Decoder creation should succeed.");
            test::AssertEq(
                bag_push_pcm(decoder, encode_result.samples, encode_result.sample_count, 0),
                BAG_OK,
                "PCM push should succeed before structured decode follow comparison.");

            std::array<char, 4096> decode_text_buffer{};
            std::array<char, 4096> decode_raw_hex_buffer{};
            std::array<char, 32768> decode_raw_bits_buffer{};
            std::array<bag_payload_follow_byte_entry, 2048> decode_byte_entries{};
            std::array<bag_payload_follow_binary_group_entry, 4096> decode_binary_entries{};
            bag_decode_result decode_result{};
            decode_result.text_buffer = decode_text_buffer.data();
            decode_result.text_buffer_size = decode_text_buffer.size();
            decode_result.raw_bytes_hex_buffer = decode_raw_hex_buffer.data();
            decode_result.raw_bytes_hex_buffer_size = decode_raw_hex_buffer.size();
            decode_result.raw_bits_binary_buffer = decode_raw_bits_buffer.data();
            decode_result.raw_bits_binary_buffer_size = decode_raw_bits_buffer.size();
            decode_result.follow_data.byte_timeline_buffer = decode_byte_entries.data();
            decode_result.follow_data.byte_timeline_buffer_count = decode_byte_entries.size();
            decode_result.follow_data.binary_group_timeline_buffer = decode_binary_entries.data();
            decode_result.follow_data.binary_group_timeline_buffer_count = decode_binary_entries.size();
            test::AssertEq(
                bag_poll_decode_result(decoder, &decode_result),
                BAG_OK,
                "Structured decode should succeed for follow comparison.");
            bag_destroy_decoder(decoder);

            test::AssertEq(
                std::string(encode_raw_hex_buffer.data(), encode_result.raw_bytes_hex_size),
                std::string(decode_raw_hex_buffer.data(), decode_result.raw_bytes_hex_size),
                "Encode-side and decode-side raw hex should match.");
            test::AssertEq(
                encode_result.follow_data.payload_begin_sample,
                decode_result.follow_data.payload_begin_sample,
                "Encode-side and decode-side payload begin sample should match.");
            test::AssertEq(
                encode_result.follow_data.payload_sample_count,
                decode_result.follow_data.payload_sample_count,
                "Encode-side and decode-side payload sample count should match.");
            test::AssertEq(
                encode_result.follow_data.binary_group_timeline_count,
                decode_result.follow_data.binary_group_timeline_count,
                "Encode-side and decode-side binary group counts should match.");

            bag_free_encode_result(&encode_result);
        }
    }
}

void TestApiBuildEncodeFollowDataMatchesStructuredEncode() {
    const std::array<bag_transport_mode, 3> modes = {
        BAG_TRANSPORT_FLASH,
        BAG_TRANSPORT_PRO,
        BAG_TRANSPORT_ULTRA,
    };
    const std::string text = "FOLLOW";

    for (const auto& config_case : test::ConfigCases()) {
        for (const auto mode : modes) {
            const auto encoder_config = MakeEncoderConfig(config_case, mode);
            std::array<char, 4096> structured_raw_hex_buffer{};
            std::array<char, 32768> structured_raw_bits_buffer{};
            std::array<char, 256> structured_text_tokens_buffer{};
            std::array<char, 256> structured_lyric_lines_buffer{};
            std::array<bag_payload_follow_byte_entry, 2048> structured_byte_entries{};
            std::array<bag_payload_follow_binary_group_entry, 4096> structured_binary_entries{};
            std::array<bag_text_follow_token_entry, 64> structured_text_entries{};
            std::array<bag_text_follow_raw_segment_entry, 64> structured_text_raw_segments{};
            std::array<bag_text_follow_raw_display_unit_entry, 256> structured_text_raw_display_units{};
            std::array<bag_text_follow_lyric_line_entry, 64> structured_lyric_line_entries{};
            std::array<bag_text_follow_line_token_range_entry, 64> structured_line_token_ranges{};
            std::array<bag_text_follow_line_raw_segment_entry, 64> structured_line_raw_segments{};
            bag_encode_result structured_result{};
            structured_result.raw_bytes_hex_buffer = structured_raw_hex_buffer.data();
            structured_result.raw_bytes_hex_buffer_size = structured_raw_hex_buffer.size();
            structured_result.raw_bits_binary_buffer = structured_raw_bits_buffer.data();
            structured_result.raw_bits_binary_buffer_size = structured_raw_bits_buffer.size();
            structured_result.text_follow_data.text_tokens_buffer = structured_text_tokens_buffer.data();
            structured_result.text_follow_data.text_tokens_buffer_size = structured_text_tokens_buffer.size();
            structured_result.text_follow_data.lyric_lines_buffer = structured_lyric_lines_buffer.data();
            structured_result.text_follow_data.lyric_lines_buffer_size = structured_lyric_lines_buffer.size();
            structured_result.text_follow_data.text_token_timeline_buffer = structured_text_entries.data();
            structured_result.text_follow_data.text_token_timeline_buffer_count = structured_text_entries.size();
            structured_result.text_follow_data.token_raw_segments_buffer = structured_text_raw_segments.data();
            structured_result.text_follow_data.token_raw_segments_buffer_count = structured_text_raw_segments.size();
            structured_result.text_follow_data.token_raw_display_units_buffer = structured_text_raw_display_units.data();
            structured_result.text_follow_data.token_raw_display_units_buffer_count = structured_text_raw_display_units.size();
            structured_result.text_follow_data.lyric_line_timeline_buffer = structured_lyric_line_entries.data();
            structured_result.text_follow_data.lyric_line_timeline_buffer_count = structured_lyric_line_entries.size();
            structured_result.text_follow_data.line_token_ranges_buffer = structured_line_token_ranges.data();
            structured_result.text_follow_data.line_token_ranges_buffer_count = structured_line_token_ranges.size();
            structured_result.text_follow_data.line_raw_segments_buffer = structured_line_raw_segments.data();
            structured_result.text_follow_data.line_raw_segments_buffer_count = structured_line_raw_segments.size();
            structured_result.follow_data.byte_timeline_buffer = structured_byte_entries.data();
            structured_result.follow_data.byte_timeline_buffer_count = structured_byte_entries.size();
            structured_result.follow_data.binary_group_timeline_buffer = structured_binary_entries.data();
            structured_result.follow_data.binary_group_timeline_buffer_count = structured_binary_entries.size();

            std::array<char, 4096> follow_only_raw_hex_buffer{};
            std::array<char, 32768> follow_only_raw_bits_buffer{};
            std::array<char, 256> follow_only_text_tokens_buffer{};
            std::array<char, 256> follow_only_lyric_lines_buffer{};
            std::array<bag_payload_follow_byte_entry, 2048> follow_only_byte_entries{};
            std::array<bag_payload_follow_binary_group_entry, 4096> follow_only_binary_entries{};
            std::array<bag_text_follow_token_entry, 64> follow_only_text_entries{};
            std::array<bag_text_follow_raw_segment_entry, 64> follow_only_text_raw_segments{};
            std::array<bag_text_follow_raw_display_unit_entry, 256> follow_only_text_raw_display_units{};
            std::array<bag_text_follow_lyric_line_entry, 64> follow_only_lyric_line_entries{};
            std::array<bag_text_follow_line_token_range_entry, 64> follow_only_line_token_ranges{};
            std::array<bag_text_follow_line_raw_segment_entry, 64> follow_only_line_raw_segments{};
            bag_encode_result follow_only_result{};
            follow_only_result.raw_bytes_hex_buffer = follow_only_raw_hex_buffer.data();
            follow_only_result.raw_bytes_hex_buffer_size = follow_only_raw_hex_buffer.size();
            follow_only_result.raw_bits_binary_buffer = follow_only_raw_bits_buffer.data();
            follow_only_result.raw_bits_binary_buffer_size = follow_only_raw_bits_buffer.size();
            follow_only_result.text_follow_data.text_tokens_buffer = follow_only_text_tokens_buffer.data();
            follow_only_result.text_follow_data.text_tokens_buffer_size = follow_only_text_tokens_buffer.size();
            follow_only_result.text_follow_data.lyric_lines_buffer = follow_only_lyric_lines_buffer.data();
            follow_only_result.text_follow_data.lyric_lines_buffer_size = follow_only_lyric_lines_buffer.size();
            follow_only_result.text_follow_data.text_token_timeline_buffer = follow_only_text_entries.data();
            follow_only_result.text_follow_data.text_token_timeline_buffer_count = follow_only_text_entries.size();
            follow_only_result.text_follow_data.token_raw_segments_buffer = follow_only_text_raw_segments.data();
            follow_only_result.text_follow_data.token_raw_segments_buffer_count = follow_only_text_raw_segments.size();
            follow_only_result.text_follow_data.token_raw_display_units_buffer = follow_only_text_raw_display_units.data();
            follow_only_result.text_follow_data.token_raw_display_units_buffer_count = follow_only_text_raw_display_units.size();
            follow_only_result.text_follow_data.lyric_line_timeline_buffer = follow_only_lyric_line_entries.data();
            follow_only_result.text_follow_data.lyric_line_timeline_buffer_count = follow_only_lyric_line_entries.size();
            follow_only_result.text_follow_data.line_token_ranges_buffer = follow_only_line_token_ranges.data();
            follow_only_result.text_follow_data.line_token_ranges_buffer_count = follow_only_line_token_ranges.size();
            follow_only_result.text_follow_data.line_raw_segments_buffer = follow_only_line_raw_segments.data();
            follow_only_result.text_follow_data.line_raw_segments_buffer_count = follow_only_line_raw_segments.size();
            follow_only_result.follow_data.byte_timeline_buffer = follow_only_byte_entries.data();
            follow_only_result.follow_data.byte_timeline_buffer_count = follow_only_byte_entries.size();
            follow_only_result.follow_data.binary_group_timeline_buffer = follow_only_binary_entries.data();
            follow_only_result.follow_data.binary_group_timeline_buffer_count = follow_only_binary_entries.size();

            test::AssertEq(
                bag_encode_text_with_follow(&encoder_config, text.c_str(), &structured_result),
                BAG_OK,
                "Structured encode should succeed before follow-only comparison.");
            test::AssertEq(
                bag_build_encode_follow_data(&encoder_config, text.c_str(), &follow_only_result),
                BAG_OK,
                "Follow-only encode metadata build should succeed.");
            test::AssertEq(
                follow_only_result.sample_count,
                static_cast<std::size_t>(0),
                "Follow-only metadata build should not allocate PCM output.");
            test::AssertEq(
                std::string(structured_raw_hex_buffer.data(), structured_result.raw_bytes_hex_size),
                std::string(follow_only_raw_hex_buffer.data(), follow_only_result.raw_bytes_hex_size),
                "Follow-only metadata build should match structured encode raw hex.");
            test::AssertEq(
                std::string(structured_raw_bits_buffer.data(), structured_result.raw_bits_binary_size),
                std::string(follow_only_raw_bits_buffer.data(), follow_only_result.raw_bits_binary_size),
                "Follow-only metadata build should match structured encode raw bits.");
            test::AssertEq(
                structured_result.follow_data.payload_begin_sample,
                follow_only_result.follow_data.payload_begin_sample,
                "Follow-only metadata build should preserve payload begin sample.");
            test::AssertEq(
                structured_result.follow_data.payload_sample_count,
                follow_only_result.follow_data.payload_sample_count,
                "Follow-only metadata build should preserve payload sample count.");
            test::AssertEq(
                structured_result.follow_data.byte_timeline_count,
                follow_only_result.follow_data.byte_timeline_count,
                "Follow-only metadata build should preserve byte timeline count.");
            test::AssertEq(
                structured_result.text_follow_data.text_token_timeline_count,
                follow_only_result.text_follow_data.text_token_timeline_count,
                "Follow-only metadata build should preserve text token timeline count.");
            test::AssertEq(
                structured_result.text_follow_data.token_raw_display_units_count,
                follow_only_result.text_follow_data.token_raw_display_units_count,
                "Follow-only metadata build should preserve raw display units.");

            bag_free_encode_result(&structured_result);
            bag_free_encode_result(&follow_only_result);
        }
    }
}

void TestApiFlashFollowTimingRespectsStyleRules() {
    struct StyleCase {
        bag_flash_signal_profile signal_profile;
        bag_flash_voicing_flavor voicing_flavor;
        std::size_t expected_payload_multiplier;
        std::size_t expected_payload_begin_multiplier;
    };
    const std::array<StyleCase, 3> cases = {{
        {BAG_FLASH_SIGNAL_PROFILE_CODED_BURST, BAG_FLASH_VOICING_FLAVOR_CODED_BURST, 1, 3},
        {BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT, BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT, 3, 16},
        {BAG_FLASH_SIGNAL_PROFILE_DEEP_RITUAL, BAG_FLASH_VOICING_FLAVOR_DEEP_RITUAL, 5, 24},
    }};
    const auto config_case = test::ConfigCases().front();

    for (const auto& item : cases) {
        const auto encoder_config =
            MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH, item.signal_profile, item.voicing_flavor);
        std::array<char, 1024> raw_hex_buffer{};
        std::array<char, 8192> raw_bits_buffer{};
        std::array<bag_payload_follow_byte_entry, 512> byte_entries{};
        std::array<bag_payload_follow_binary_group_entry, 4096> binary_entries{};
        bag_encode_result result{};
        result.raw_bytes_hex_buffer = raw_hex_buffer.data();
        result.raw_bytes_hex_buffer_size = raw_hex_buffer.size();
        result.raw_bits_binary_buffer = raw_bits_buffer.data();
        result.raw_bits_binary_buffer_size = raw_bits_buffer.size();
        result.follow_data.byte_timeline_buffer = byte_entries.data();
        result.follow_data.byte_timeline_buffer_count = byte_entries.size();
        result.follow_data.binary_group_timeline_buffer = binary_entries.data();
        result.follow_data.binary_group_timeline_buffer_count = binary_entries.size();

        test::AssertEq(
            bag_encode_text_with_follow(&encoder_config, "AB", &result),
            BAG_OK,
            "Structured flash encode should succeed for timing assertions.");
        test::AssertEq(
            result.follow_data.payload_begin_sample,
            static_cast<std::size_t>(config_case.frame_samples) * item.expected_payload_begin_multiplier,
            "Flash follow payload begin should stay style-aware.");
        test::AssertTrue(
            result.follow_data.binary_group_timeline_count > 0,
            "Flash follow data should contain binary groups.");
        test::AssertEq(
            binary_entries.front().sample_count,
            static_cast<std::size_t>(config_case.frame_samples) * item.expected_payload_multiplier,
            "Flash follow bit duration should match the style signal multiplier.");

        bag_free_encode_result(&result);
    }
}

void TestApiStructuredEncodePublishesWordLevelTextFollow() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
    std::array<char, 256> text_tokens_buffer{};
    std::array<char, 256> lyric_lines_buffer{};
    std::array<bag_text_follow_token_entry, 32> text_entries{};
    std::array<bag_text_follow_raw_segment_entry, 32> text_raw_segments{};
    std::array<bag_text_follow_raw_display_unit_entry, 64> text_raw_display_units{};
    std::array<bag_text_follow_lyric_line_entry, 32> lyric_line_entries{};
    std::array<bag_text_follow_line_token_range_entry, 32> line_token_ranges{};
    std::array<bag_text_follow_line_raw_segment_entry, 32> line_raw_segments{};
    std::array<char, 256> raw_hex_buffer{};
    std::array<char, 2048> raw_bits_buffer{};
    std::array<bag_payload_follow_byte_entry, 128> byte_entries{};
    std::array<bag_payload_follow_binary_group_entry, 256> binary_entries{};
    bag_encode_result result{};
    result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
    result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
    result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
    result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
    result.text_follow_data.text_token_timeline_buffer = text_entries.data();
    result.text_follow_data.text_token_timeline_buffer_count = text_entries.size();
    result.text_follow_data.token_raw_segments_buffer = text_raw_segments.data();
    result.text_follow_data.token_raw_segments_buffer_count = text_raw_segments.size();
    result.text_follow_data.token_raw_display_units_buffer = text_raw_display_units.data();
    result.text_follow_data.token_raw_display_units_buffer_count = text_raw_display_units.size();
    result.text_follow_data.lyric_line_timeline_buffer = lyric_line_entries.data();
    result.text_follow_data.lyric_line_timeline_buffer_count = lyric_line_entries.size();
    result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
    result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();
    result.text_follow_data.line_raw_segments_buffer = line_raw_segments.data();
    result.text_follow_data.line_raw_segments_buffer_count = line_raw_segments.size();
    result.raw_bytes_hex_buffer = raw_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_buffer.size();
    result.follow_data.byte_timeline_buffer = byte_entries.data();
    result.follow_data.byte_timeline_buffer_count = byte_entries.size();
    result.follow_data.binary_group_timeline_buffer = binary_entries.data();
    result.follow_data.binary_group_timeline_buffer_count = binary_entries.size();

    test::AssertEq(
        bag_encode_text_with_follow(&encoder_config, "ASH BELL", &result),
        BAG_OK,
        "Structured encode should succeed for word-level text follow.");
    test::AssertTrue(result.text_follow_data.available != 0, "Encode should publish text follow data.");
    test::AssertEq(
        result.text_follow_data.text_tokens_status,
        BAG_DECODE_CONTENT_STATUS_OK,
        "Encode should publish text-follow tokens.");
    test::AssertEq(
        result.text_follow_data.text_token_timeline_status,
        BAG_DECODE_CONTENT_STATUS_OK,
        "Encode should publish text-follow timeline entries.");
    test::AssertEq(
        result.text_follow_data.token_raw_segments_status,
        BAG_DECODE_CONTENT_STATUS_OK,
        "Encode should publish token raw-segment mappings.");
    test::AssertEq(
        result.text_follow_data.token_raw_display_units_status,
        BAG_DECODE_CONTENT_STATUS_OK,
        "Encode should publish token raw display units.");
    test::AssertEq(
        result.text_follow_data.lyric_lines_status,
        BAG_DECODE_CONTENT_STATUS_OK,
        "Encode should publish lyric lines.");
    test::AssertEq(
        result.text_follow_data.lyric_line_timeline_status,
        BAG_DECODE_CONTENT_STATUS_OK,
        "Encode should publish lyric line timeline entries.");
    test::AssertEq(
        result.text_follow_data.line_token_ranges_status,
        BAG_DECODE_CONTENT_STATUS_OK,
        "Encode should publish line token ranges.");
    test::AssertEq(
        result.text_follow_data.line_raw_segments_status,
        BAG_DECODE_CONTENT_STATUS_OK,
        "Encode should publish line raw-segment mappings.");
    const auto tokens =
        SplitLineSeparatedTokens(std::string(text_tokens_buffer.data(), result.text_follow_data.text_tokens_size));
    const auto lyric_lines =
        SplitLineSeparatedTokens(std::string(lyric_lines_buffer.data(), result.text_follow_data.lyric_lines_size));
    test::AssertEq(tokens.size(), static_cast<std::size_t>(2), "Space-delimited text should tokenize by word.");
    test::AssertEq(tokens[0], std::string("ASH"), "First token should preserve the first word.");
    test::AssertEq(tokens[1], std::string("BELL"), "Second token should preserve the second word.");
    test::AssertEq(
        result.text_follow_data.text_token_timeline_count,
        tokens.size(),
        "Word-level text follow should align one timeline entry per token.");
    test::AssertEq(
        text_entries.front().start_sample,
        result.follow_data.payload_begin_sample,
        "Text follow should begin at the payload start sample.");
    test::AssertTrue(
        text_entries.front().sample_count > 0 && text_entries[1].sample_count > 0,
        "Each text token should span a non-empty sample range.");
    test::AssertEq(
        result.text_follow_data.token_raw_segments_count,
        tokens.size(),
        "Word-level text follow should publish one raw segment per token.");
    test::AssertEq(
        result.text_follow_data.lyric_line_timeline_count,
        lyric_lines.size(),
        "Lyric lines should align one timeline entry per line.");
    test::AssertEq(
        lyric_lines.size(),
        static_cast<std::size_t>(1),
        "Short space-delimited text should stay on a single lyric line.");
    test::AssertEq(
        lyric_lines.front(),
        std::string("ASH BELL"),
        "Single lyric line should preserve word spacing.");
    test::AssertEq(
        result.text_follow_data.line_token_ranges_count,
        lyric_lines.size(),
        "Lyric lines should publish one token-range entry per line.");
    test::AssertEq(
        result.text_follow_data.line_raw_segments_count,
        lyric_lines.size(),
        "Lyric lines should publish one raw-segment entry per line.");
    test::AssertEq(
        result.text_follow_data.token_raw_display_units_count,
        static_cast<std::size_t>(8),
        "Word-level text follow should publish one display unit per payload byte.");
    test::AssertEq(
        line_token_ranges[0].token_begin_index,
        static_cast<std::size_t>(0),
        "First lyric line should begin at the first token.");
    test::AssertEq(
        line_token_ranges[0].token_count,
        static_cast<std::size_t>(2),
        "Single lyric line should cover both word tokens.");
    test::AssertEq(
        line_raw_segments[0].byte_count,
        static_cast<std::size_t>(8),
        "Single lyric line raw segment should cover the full payload.");
    test::AssertEq(text_raw_segments[0].byte_offset, static_cast<std::size_t>(0),
                   "First token should start at the first payload byte.");
    test::AssertEq(text_raw_segments[0].byte_count, static_cast<std::size_t>(4),
                   "First word should cover its bytes plus the separating space.");
    test::AssertEq(text_raw_segments[1].byte_offset, static_cast<std::size_t>(4),
                   "Second token should begin after the first token byte span.");
    test::AssertEq(text_raw_segments[1].byte_count, static_cast<std::size_t>(4),
                   "Second token should cover the remaining payload bytes.");
    test::AssertEq(text_raw_display_units[0].token_index, static_cast<std::size_t>(0),
                   "First display unit should belong to the first token.");
    test::AssertEq(text_raw_display_units[0].byte_index_within_token, static_cast<std::size_t>(0),
                   "First display unit should start at the first token byte.");
    test::AssertEq(text_raw_display_units[3].byte_index_within_token, static_cast<std::size_t>(3),
                   "First token display units should advance one byte at a time.");
    test::AssertEq(text_raw_display_units[4].token_index, static_cast<std::size_t>(1),
                   "Display units should switch token ownership at the token boundary.");
    test::AssertEq(text_raw_display_units[4].byte_index_within_token, static_cast<std::size_t>(0),
                   "Second token display units should reset their local byte index.");

    bag_free_encode_result(&result);
}

void TestApiStructuredEncodePublishesCjkTextFollow() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
    std::array<char, 256> text_tokens_buffer{};
    std::array<char, 256> lyric_lines_buffer{};
    std::array<bag_text_follow_token_entry, 32> text_entries{};
    std::array<bag_text_follow_raw_segment_entry, 32> text_raw_segments{};
    std::array<bag_text_follow_raw_display_unit_entry, 32> text_raw_display_units{};
    std::array<bag_text_follow_lyric_line_entry, 32> lyric_line_entries{};
    std::array<bag_text_follow_line_token_range_entry, 32> line_token_ranges{};
    std::array<bag_text_follow_line_raw_segment_entry, 32> line_raw_segments{};
    std::array<char, 256> raw_hex_buffer{};
    std::array<char, 2048> raw_bits_buffer{};
    std::array<bag_payload_follow_byte_entry, 128> byte_entries{};
    std::array<bag_payload_follow_binary_group_entry, 256> binary_entries{};
    bag_encode_result result{};
    result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
    result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
    result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
    result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
    result.text_follow_data.text_token_timeline_buffer = text_entries.data();
    result.text_follow_data.text_token_timeline_buffer_count = text_entries.size();
    result.text_follow_data.token_raw_segments_buffer = text_raw_segments.data();
    result.text_follow_data.token_raw_segments_buffer_count = text_raw_segments.size();
    result.text_follow_data.token_raw_display_units_buffer = text_raw_display_units.data();
    result.text_follow_data.token_raw_display_units_buffer_count = text_raw_display_units.size();
    result.text_follow_data.lyric_line_timeline_buffer = lyric_line_entries.data();
    result.text_follow_data.lyric_line_timeline_buffer_count = lyric_line_entries.size();
    result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
    result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();
    result.text_follow_data.line_raw_segments_buffer = line_raw_segments.data();
    result.text_follow_data.line_raw_segments_buffer_count = line_raw_segments.size();
    result.raw_bytes_hex_buffer = raw_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_buffer.size();
    result.follow_data.byte_timeline_buffer = byte_entries.data();
    result.follow_data.byte_timeline_buffer_count = byte_entries.size();
    result.follow_data.binary_group_timeline_buffer = binary_entries.data();
    result.follow_data.binary_group_timeline_buffer_count = binary_entries.size();

    test::AssertEq(
        bag_encode_text_with_follow(&encoder_config, test::Utf8Literal(u8"神机").c_str(), &result),
        BAG_OK,
        "Structured encode should succeed for CJK text follow.");
    const auto tokens =
        SplitLineSeparatedTokens(std::string(text_tokens_buffer.data(), result.text_follow_data.text_tokens_size));
    const auto lyric_lines =
        SplitLineSeparatedTokens(std::string(lyric_lines_buffer.data(), result.text_follow_data.lyric_lines_size));
    test::AssertEq(tokens.size(), static_cast<std::size_t>(2), "CJK text should tokenize by character.");
    test::AssertEq(tokens[0], test::Utf8Literal(u8"神"), "First CJK token should preserve the first character.");
    test::AssertEq(tokens[1], test::Utf8Literal(u8"机"), "Second CJK token should preserve the second character.");
    test::AssertEq(
        result.text_follow_data.text_token_timeline_count,
        tokens.size(),
        "CJK text follow should align one timeline entry per character token.");
    test::AssertEq(
        result.text_follow_data.token_raw_segments_count,
        tokens.size(),
        "CJK text follow should publish one raw segment per character token.");
    test::AssertEq(
        result.text_follow_data.token_raw_display_units_count,
        static_cast<std::size_t>(6),
        "CJK text follow should publish one display unit per UTF-8 payload byte.");
    test::AssertEq(
        lyric_lines.size(),
        static_cast<std::size_t>(1),
        "Short CJK text should stay on a single lyric line.");
    test::AssertEq(
        lyric_lines.front(),
        test::Utf8Literal(u8"神机"),
        "Lyric line should preserve compact CJK formatting.");
    test::AssertEq(
        line_token_ranges[0].token_count,
        static_cast<std::size_t>(2),
        "Single CJK lyric line should cover both character tokens.");
    test::AssertEq(text_raw_segments[0].byte_count, static_cast<std::size_t>(3),
                   "Each UTF-8 CJK token should map to its three-byte payload span.");
    test::AssertEq(text_raw_segments[1].byte_offset, static_cast<std::size_t>(3),
                   "Second UTF-8 CJK token should start after the first byte span.");
    test::AssertEq(text_raw_display_units[2].token_index, static_cast<std::size_t>(0),
                   "First CJK token should own its third byte-sized display unit.");
    test::AssertEq(text_raw_display_units[3].token_index, static_cast<std::size_t>(1),
                   "Second CJK token display units should begin at the next byte.");
    test::AssertEq(text_raw_display_units[3].byte_index_within_token, static_cast<std::size_t>(0),
                   "New token display units should reset their local byte index.");

    bag_free_encode_result(&result);
}

void TestApiStructuredEncodeDetachesPunctuationAcrossScripts() {
    const auto config_case = test::ConfigCases().front();

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
        std::array<char, 256> text_tokens_buffer{};
        std::array<char, 256> lyric_lines_buffer{};
        std::array<bag_text_follow_token_entry, 32> text_entries{};
        std::array<bag_text_follow_line_token_range_entry, 32> line_token_ranges{};
        bag_encode_result result{};
        result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
        result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
        result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
        result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
        result.text_follow_data.text_token_timeline_buffer = text_entries.data();
        result.text_follow_data.text_token_timeline_buffer_count = text_entries.size();
        result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
        result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();

        test::AssertEq(
            bag_encode_text_with_follow(&encoder_config, "HELLO, WORLD!", &result),
            BAG_OK,
            "Structured encode should succeed for Latin punctuation splitting.");
        const auto tokens = SplitLineSeparatedTokens(
            std::string(text_tokens_buffer.data(), result.text_follow_data.text_tokens_size));
        const auto lyric_lines = SplitLineSeparatedTokens(
            std::string(lyric_lines_buffer.data(), result.text_follow_data.lyric_lines_size));
        test::AssertEq(tokens.size(), static_cast<std::size_t>(4),
                       "Latin punctuation should be detached into standalone tokens.");
        test::AssertEq(tokens[0], std::string("HELLO"),
                       "Latin word token should preserve text before punctuation.");
        test::AssertEq(tokens[1], std::string(","),
                       "Comma should become its own token.");
        test::AssertEq(tokens[2], std::string("WORLD"),
                       "Latin word token should preserve text after punctuation.");
        test::AssertEq(tokens[3], std::string("!"),
                       "Exclamation mark should become its own token.");
        test::AssertEq(lyric_lines.front(), std::string("HELLO, WORLD!"),
                       "Lyric line text should preserve original punctuation placement.");
        test::AssertEq(line_token_ranges[0].token_count, static_cast<std::size_t>(4),
                       "Lyric line token range should include detached punctuation tokens.");
        bag_free_encode_result(&result);
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        std::array<char, 256> text_tokens_buffer{};
        std::array<char, 256> lyric_lines_buffer{};
        std::array<bag_text_follow_token_entry, 32> text_entries{};
        std::array<bag_text_follow_line_token_range_entry, 32> line_token_ranges{};
        bag_encode_result result{};
        result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
        result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
        result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
        result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
        result.text_follow_data.text_token_timeline_buffer = text_entries.data();
        result.text_follow_data.text_token_timeline_buffer_count = text_entries.size();
        result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
        result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();

        const auto input = test::Utf8Literal(u8"「神机」，启动。");
        test::AssertEq(
            bag_encode_text_with_follow(&encoder_config, input.c_str(), &result),
            BAG_OK,
            "Structured encode should succeed for CJK punctuation splitting.");
        const auto tokens = SplitLineSeparatedTokens(
            std::string(text_tokens_buffer.data(), result.text_follow_data.text_tokens_size));
        const auto lyric_lines = SplitLineSeparatedTokens(
            std::string(lyric_lines_buffer.data(), result.text_follow_data.lyric_lines_size));
        test::AssertEq(tokens.size(), static_cast<std::size_t>(8),
                       "CJK punctuation should also be detached into standalone tokens.");
        test::AssertEq(tokens[0], test::Utf8Literal(u8"「"),
                       "Opening quote should become its own token.");
        test::AssertEq(tokens[1], test::Utf8Literal(u8"神"),
                       "CJK character tokens should remain character-sized.");
        test::AssertEq(tokens[2], test::Utf8Literal(u8"机"),
                       "CJK character tokens should remain character-sized.");
        test::AssertEq(tokens[3], test::Utf8Literal(u8"」"),
                       "Closing quote should become its own token.");
        test::AssertEq(tokens[4], test::Utf8Literal(u8"，"),
                       "CJK comma should become its own token.");
        test::AssertEq(tokens[5], test::Utf8Literal(u8"启"),
                       "CJK text should continue to tokenize by character between punctuation marks.");
        test::AssertEq(tokens[6], test::Utf8Literal(u8"动"),
                       "CJK text should continue to tokenize by character between punctuation marks.");
        test::AssertEq(tokens[7], test::Utf8Literal(u8"。"),
                       "CJK full stop should become its own token.");
        test::AssertEq(lyric_lines.front(), input,
                       "Lyric line text should preserve original CJK punctuation placement.");
        test::AssertEq(line_token_ranges[0].token_count, static_cast<std::size_t>(8),
                       "Lyric line token range should include detached CJK punctuation tokens.");
        bag_free_encode_result(&result);
    }
}

void TestApiStructuredDecodePublishesTextFollowAndKeepsRawOnlyFallback() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);
    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "RITUAL. WORD", &pcm),
        BAG_OK,
        "Decode text-follow setup encode should succeed.");

    bag_decoder* decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &decoder),
        BAG_OK,
        "Decoder creation should succeed for structured text follow.");
    test::AssertEq(
        bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
        BAG_OK,
        "PCM push should succeed for structured text follow.");
    std::array<char, 256> text_buffer{};
    std::array<char, 256> raw_hex_buffer{};
    std::array<char, 2048> raw_bits_buffer{};
    std::array<char, 256> text_tokens_buffer{};
    std::array<char, 256> lyric_lines_buffer{};
    std::array<bag_text_follow_token_entry, 32> text_entries{};
    std::array<bag_text_follow_raw_segment_entry, 32> text_raw_segments{};
    std::array<bag_text_follow_raw_display_unit_entry, 64> text_raw_display_units{};
    std::array<bag_text_follow_lyric_line_entry, 32> lyric_line_entries{};
    std::array<bag_text_follow_line_token_range_entry, 32> line_token_ranges{};
    std::array<bag_text_follow_line_raw_segment_entry, 32> line_raw_segments{};
    std::array<bag_payload_follow_byte_entry, 128> byte_entries{};
    std::array<bag_payload_follow_binary_group_entry, 512> binary_entries{};
    bag_decode_result result{};
    result.text_buffer = text_buffer.data();
    result.text_buffer_size = text_buffer.size();
    result.raw_bytes_hex_buffer = raw_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_buffer.size();
    result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
    result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
    result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
    result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
    result.text_follow_data.text_token_timeline_buffer = text_entries.data();
    result.text_follow_data.text_token_timeline_buffer_count = text_entries.size();
    result.text_follow_data.token_raw_segments_buffer = text_raw_segments.data();
    result.text_follow_data.token_raw_segments_buffer_count = text_raw_segments.size();
    result.text_follow_data.token_raw_display_units_buffer = text_raw_display_units.data();
    result.text_follow_data.token_raw_display_units_buffer_count = text_raw_display_units.size();
    result.text_follow_data.lyric_line_timeline_buffer = lyric_line_entries.data();
    result.text_follow_data.lyric_line_timeline_buffer_count = lyric_line_entries.size();
    result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
    result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();
    result.text_follow_data.line_raw_segments_buffer = line_raw_segments.data();
    result.text_follow_data.line_raw_segments_buffer_count = line_raw_segments.size();
    result.follow_data.byte_timeline_buffer = byte_entries.data();
    result.follow_data.byte_timeline_buffer_count = byte_entries.size();
    result.follow_data.binary_group_timeline_buffer = binary_entries.data();
    result.follow_data.binary_group_timeline_buffer_count = binary_entries.size();

    test::AssertEq(
        bag_poll_decode_result(decoder, &result),
        BAG_OK,
        "Structured decode should succeed for text follow.");
    bag_destroy_decoder(decoder);
    const auto tokens =
        SplitLineSeparatedTokens(std::string(text_tokens_buffer.data(), result.text_follow_data.text_tokens_size));
    const auto lyric_lines =
        SplitLineSeparatedTokens(std::string(lyric_lines_buffer.data(), result.text_follow_data.lyric_lines_size));
    test::AssertTrue(result.text_follow_data.available != 0, "Structured decode should publish text follow.");
    test::AssertEq(tokens.size(), static_cast<std::size_t>(2), "Decoded text should reuse the tokenized text follow.");
    test::AssertEq(
        lyric_lines.size(),
        static_cast<std::size_t>(2),
        "Decoded text with short-phrase punctuation should split into lyric lines.");
    test::AssertEq(
        result.text_follow_data.text_token_timeline_count,
        tokens.size(),
        "Decoded text follow should publish one entry per token.");
    test::AssertEq(
        result.text_follow_data.token_raw_segments_count,
        tokens.size(),
        "Decoded text follow should publish one raw segment per token.");
    test::AssertEq(
        result.text_follow_data.token_raw_display_units_status,
        BAG_DECODE_CONTENT_STATUS_OK,
        "Structured decode should publish token raw display units.");
    for (std::size_t index = 0; index < result.text_follow_data.text_token_timeline_count; ++index) {
        test::AssertTrue(
            text_entries[index].start_sample >= result.follow_data.payload_begin_sample,
            "Text-follow entries should stay within the payload sample range.");
        test::AssertTrue(
            text_entries[index].start_sample + text_entries[index].sample_count <=
                result.follow_data.payload_begin_sample + result.follow_data.payload_sample_count,
            "Text-follow entries should not extend beyond the payload sample range.");
        test::AssertEq(
            text_raw_segments[index].start_sample,
            text_entries[index].start_sample,
            "Token raw segments should share the token follow start sample.");
        test::AssertEq(
            text_raw_segments[index].sample_count,
            text_entries[index].sample_count,
            "Token raw segments should share the token follow sample count.");
    }
    std::size_t display_unit_cursor = 0;
    for (std::size_t index = 0; index < result.text_follow_data.token_raw_segments_count; ++index) {
        for (std::size_t byte_index = 0; byte_index < text_raw_segments[index].byte_count; ++byte_index) {
            test::AssertEq(
                text_raw_display_units[display_unit_cursor].token_index,
                index,
                "Decoded display units should stay attached to the owning token.");
            test::AssertEq(
                text_raw_display_units[display_unit_cursor].byte_index_within_token,
                byte_index,
                "Decoded display units should expose the byte index within the token.");
            ++display_unit_cursor;
        }
    }
    test::AssertEq(
        display_unit_cursor,
        result.text_follow_data.token_raw_display_units_count,
        "Decoded display units should exactly cover all token byte spans.");
    test::AssertEq(
        result.text_follow_data.lyric_line_timeline_count,
        lyric_lines.size(),
        "Structured decode should publish one timeline entry per lyric line.");
    test::AssertEq(
        result.text_follow_data.line_token_ranges_count,
        lyric_lines.size(),
        "Structured decode should publish one token range per lyric line.");
    test::AssertEq(
        result.text_follow_data.line_raw_segments_count,
        lyric_lines.size(),
        "Structured decode should publish one raw segment per lyric line.");
    bag_free_pcm16_result(&pcm);

    bag_decoder* raw_only_decoder = nullptr;
    const auto raw_only_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);
    test::AssertEq(
        bag_create_decoder(&raw_only_config, &raw_only_decoder),
        BAG_OK,
        "Raw-only decoder creation should succeed.");
    const std::array<std::int16_t, 2> invalid_pcm = {0, 0};
    test::AssertEq(
        bag_push_pcm(raw_only_decoder, invalid_pcm.data(), invalid_pcm.size(), 0),
        BAG_OK,
        "Raw-only decoder should accept the synthetic PCM block.");
    bag_decode_result raw_only_result{};
    raw_only_result.text_buffer = text_buffer.data();
    raw_only_result.text_buffer_size = text_buffer.size();
    raw_only_result.raw_bytes_hex_buffer = raw_hex_buffer.data();
    raw_only_result.raw_bytes_hex_buffer_size = raw_hex_buffer.size();
    raw_only_result.raw_bits_binary_buffer = raw_bits_buffer.data();
    raw_only_result.raw_bits_binary_buffer_size = raw_bits_buffer.size();
    raw_only_result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
    raw_only_result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
    raw_only_result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
    raw_only_result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
    raw_only_result.text_follow_data.text_token_timeline_buffer = text_entries.data();
    raw_only_result.text_follow_data.text_token_timeline_buffer_count = text_entries.size();
    raw_only_result.text_follow_data.token_raw_segments_buffer = text_raw_segments.data();
    raw_only_result.text_follow_data.token_raw_segments_buffer_count = text_raw_segments.size();
    raw_only_result.text_follow_data.token_raw_display_units_buffer = text_raw_display_units.data();
    raw_only_result.text_follow_data.token_raw_display_units_buffer_count = text_raw_display_units.size();
    raw_only_result.text_follow_data.lyric_line_timeline_buffer = lyric_line_entries.data();
    raw_only_result.text_follow_data.lyric_line_timeline_buffer_count = lyric_line_entries.size();
    raw_only_result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
    raw_only_result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();
    raw_only_result.text_follow_data.line_raw_segments_buffer = line_raw_segments.data();
    raw_only_result.text_follow_data.line_raw_segments_buffer_count = line_raw_segments.size();
    raw_only_result.follow_data.byte_timeline_buffer = byte_entries.data();
    raw_only_result.follow_data.byte_timeline_buffer_count = byte_entries.size();
    raw_only_result.follow_data.binary_group_timeline_buffer = binary_entries.data();
    raw_only_result.follow_data.binary_group_timeline_buffer_count = binary_entries.size();
    const auto raw_only_code = bag_poll_decode_result(raw_only_decoder, &raw_only_result);
    bag_destroy_decoder(raw_only_decoder);

    if (raw_only_code == BAG_OK &&
        raw_only_result.raw_payload_available != 0 &&
        raw_only_result.text_decode_status == BAG_DECODE_CONTENT_STATUS_INVALID_TEXT_PAYLOAD) {
        test::AssertTrue(
            raw_only_result.text_follow_data.available == 0,
            "Raw-only structured decode should keep text follow unavailable.");
    }
}

void TestApiStructuredEncodePublishesShortPhraseLyricLines() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
    std::array<char, 512> lyric_lines_buffer{};
    std::array<bag_text_follow_lyric_line_entry, 32> lyric_line_entries{};
    std::array<bag_text_follow_line_token_range_entry, 32> line_token_ranges{};
    std::array<bag_text_follow_line_raw_segment_entry, 32> line_raw_segments{};
    bag_encode_result result{};
    result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
    result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
    result.text_follow_data.lyric_line_timeline_buffer = lyric_line_entries.data();
    result.text_follow_data.lyric_line_timeline_buffer_count = lyric_line_entries.size();
    result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
    result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();
    result.text_follow_data.line_raw_segments_buffer = line_raw_segments.data();
    result.text_follow_data.line_raw_segments_buffer_count = line_raw_segments.size();

    const std::string input =
        "ASH BELL.\nKEEP THE WESTERN GATE SILENT UNTIL DAWN RETURNS";
    test::AssertEq(
        bag_encode_text_with_follow(&encoder_config, input.c_str(), &result),
        BAG_OK,
        "Structured encode should succeed for lyric-line tests.");

    const auto lyric_lines =
        SplitLineSeparatedTokens(std::string(lyric_lines_buffer.data(), result.text_follow_data.lyric_lines_size));
    test::AssertTrue(
        lyric_lines.size() >= static_cast<std::size_t>(2),
        "Lyric line builder should honor explicit newlines and short-phrase breaks.");
    test::AssertEq(
        lyric_lines.front(),
        std::string("ASH BELL."),
        "Lyric line builder should keep the explicit first short phrase intact.");
    test::AssertEq(
        result.text_follow_data.lyric_line_timeline_count,
        lyric_lines.size(),
        "Lyric line timeline should align with serialized lyric lines.");
    test::AssertEq(
        result.text_follow_data.line_token_ranges_count,
        lyric_lines.size(),
        "Lyric line token ranges should align with serialized lyric lines.");
    test::AssertEq(
        result.text_follow_data.line_raw_segments_count,
        lyric_lines.size(),
        "Lyric line raw segments should align with serialized lyric lines.");
    for (std::size_t index = 0; index < result.text_follow_data.lyric_line_timeline_count; ++index) {
        test::AssertTrue(
            lyric_line_entries[index].sample_count > 0,
            "Each lyric line should span a non-empty sample range.");
        test::AssertTrue(
            line_raw_segments[index].byte_count > 0,
            "Each lyric line should map to a non-empty byte segment.");
    }

    bag_free_encode_result(&result);
}

void TestApiLegacyTextPollingStaysCompatibleWhenStructuredDecodeHasRawOnlyValue() {
    const auto config_case = test::ConfigCases().front();
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);

    bag_decoder* structured_decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &structured_decoder),
        BAG_OK,
        "Structured decoder creation should succeed.");
    const std::array<std::int16_t, 2> invalid_pcm = {0, 0};
    test::AssertEq(
        bag_push_pcm(structured_decoder, invalid_pcm.data(), invalid_pcm.size(), 0),
        BAG_OK,
        "Structured decoder should accept the synthetic PCM block.");
    std::vector<char> text_buffer(64, '\0');
    std::vector<char> raw_hex_buffer(64, '\0');
    std::vector<char> raw_bits_buffer(128, '\0');
    bag_decode_result decode_result{};
    decode_result.text_buffer = text_buffer.data();
    decode_result.text_buffer_size = text_buffer.size();
    decode_result.raw_bytes_hex_buffer = raw_hex_buffer.data();
    decode_result.raw_bytes_hex_buffer_size = raw_hex_buffer.size();
    decode_result.raw_bits_binary_buffer = raw_bits_buffer.data();
    decode_result.raw_bits_binary_buffer_size = raw_bits_buffer.size();
    const auto structured_code = bag_poll_decode_result(structured_decoder, &decode_result);
    bag_destroy_decoder(structured_decoder);

    bag_decoder* legacy_decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &legacy_decoder),
        BAG_OK,
        "Legacy decoder creation should succeed.");
    test::AssertEq(
        bag_push_pcm(legacy_decoder, invalid_pcm.data(), invalid_pcm.size(), 0),
        BAG_OK,
        "Legacy decoder should accept the synthetic PCM block.");
    std::array<char, 64> legacy_text_buffer{};
    bag_text_result text_result{};
    text_result.buffer = legacy_text_buffer.data();
    text_result.buffer_size = legacy_text_buffer.size();
    const auto legacy_code = bag_poll_result(legacy_decoder, &text_result);
    bag_destroy_decoder(legacy_decoder);

    if (structured_code == BAG_OK &&
        decode_result.raw_payload_available != 0 &&
        decode_result.text_decode_status == BAG_DECODE_CONTENT_STATUS_INVALID_TEXT_PAYLOAD) {
        test::AssertEq(
            legacy_code,
            BAG_INTERNAL,
            "Legacy text polling should continue reporting an internal failure for raw-only results.");
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
    runner.Add("Api.DecodeResultPublishesRawPayloadAcrossModes", TestApiDecodeResultPublishesRawPayloadAcrossModes);
    runner.Add("Api.DecodeResultBufferBoundaries", TestApiDecodeResultBufferBoundaries);
    runner.Add("Api.StructuredEncodePublishesFollowAcrossModes", TestApiStructuredEncodePublishesFollowAcrossModes);
    runner.Add("Api.EncodeAndDecodeFollowStayAligned", TestApiEncodeAndDecodeFollowStayAligned);
    runner.Add("Api.BuildEncodeFollowDataMatchesStructuredEncode",
               TestApiBuildEncodeFollowDataMatchesStructuredEncode);
    runner.Add("Api.FlashFollowTimingRespectsStyleRules", TestApiFlashFollowTimingRespectsStyleRules);
    runner.Add("Api.StructuredEncodePublishesWordLevelTextFollow", TestApiStructuredEncodePublishesWordLevelTextFollow);
    runner.Add("Api.StructuredEncodePublishesCjkTextFollow", TestApiStructuredEncodePublishesCjkTextFollow);
    runner.Add("Api.StructuredEncodeDetachesPunctuationAcrossScripts",
               TestApiStructuredEncodeDetachesPunctuationAcrossScripts);
    runner.Add("Api.StructuredEncodePublishesShortPhraseLyricLines",
               TestApiStructuredEncodePublishesShortPhraseLyricLines);
    runner.Add("Api.StructuredDecodePublishesTextFollowAndKeepsRawOnlyFallback",
               TestApiStructuredDecodePublishesTextFollowAndKeepsRawOnlyFallback);
    runner.Add("Api.LegacyTextPollingStaysCompatibleWhenStructuredDecodeHasRawOnlyValue",
               TestApiLegacyTextPollingStaysCompatibleWhenStructuredDecodeHasRawOnlyValue);
    runner.Add("Api.PollResultBufferBoundaries", TestApiPollResultBufferBoundaries);
    runner.Add("Api.DecoderInputAndRepeatedPollBoundaries", TestApiDecoderInputAndRepeatedPollBoundaries);
    runner.Add("Api.FreePcmResultIsIdempotent", TestApiFreePcmResultIsIdempotent);
    runner.Add("Api.VersionMatchesRelease", TestApiVersionMatchesRelease);
    runner.Add("Api.ValidationHelpers", TestApiValidationHelpers);
}

}  // namespace api_tests
