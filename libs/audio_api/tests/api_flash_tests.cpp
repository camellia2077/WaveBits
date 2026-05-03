#include <string>

#include "api_test_support.h"

namespace {

using namespace api_tests;

void TestApiFlashConfigAffectsLengthAndRoundTrip() {
    const std::string text = "Length";

    for (const auto& config_case : test::ConfigCases()) {
        const auto steady_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                BAG_FLASH_VOICING_FLAVOR_STEADY);
        const auto steady_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                BAG_FLASH_VOICING_FLAVOR_STEADY);
        const auto litany_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_LITANY,
                BAG_FLASH_VOICING_FLAVOR_LITANY);
        const auto litany_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_LITANY,
                BAG_FLASH_VOICING_FLAVOR_LITANY);
        const auto hostile_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_HOSTILE,
                BAG_FLASH_VOICING_FLAVOR_HOSTILE);
        const auto hostile_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_HOSTILE,
                BAG_FLASH_VOICING_FLAVOR_HOSTILE);
        const auto zeal_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_ZEAL,
                BAG_FLASH_VOICING_FLAVOR_ZEAL);
        const auto zeal_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_ZEAL,
                BAG_FLASH_VOICING_FLAVOR_ZEAL);
        const auto void_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_VOID,
                BAG_FLASH_VOICING_FLAVOR_VOID);
        const auto void_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_VOID,
                BAG_FLASH_VOICING_FLAVOR_VOID);

        bag_pcm16_result steady_pcm{};
        bag_pcm16_result litany_pcm{};
        bag_pcm16_result hostile_pcm{};
        bag_pcm16_result zeal_pcm{};
        bag_pcm16_result void_pcm{};
        test::AssertEq(
            bag_encode_text(&steady_encoder, text.c_str(), &steady_pcm),
            BAG_OK,
            "steady flash encode should succeed through the C API.");
        test::AssertEq(
            bag_encode_text(&litany_encoder, text.c_str(), &litany_pcm),
            BAG_OK,
            "litany flash encode should succeed through the C API.");
        test::AssertEq(
            bag_encode_text(&hostile_encoder, text.c_str(), &hostile_pcm),
            BAG_OK,
            "hostile flash encode should succeed through the C API.");
        test::AssertEq(
            bag_encode_text(&zeal_encoder, text.c_str(), &zeal_pcm),
            BAG_OK,
            "zeal flash encode should succeed through the C API.");
        test::AssertEq(
            bag_encode_text(&void_encoder, text.c_str(), &void_pcm),
            BAG_OK,
            "void flash encode should succeed through the C API.");
        test::AssertEq(
            steady_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                BAG_FLASH_VOICING_FLAVOR_STEADY),
            "steady C API flash length should stay on the baseline explicit configuration.");
        test::AssertEq(
            litany_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_LITANY,
                BAG_FLASH_VOICING_FLAVOR_LITANY),
            "litany C API flash length should include the longer timing and shell configuration.");
        test::AssertTrue(
            litany_pcm.sample_count > steady_pcm.sample_count,
            "litany flash output should be longer than steady for the same text.");
        test::AssertEq(
            hostile_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_HOSTILE,
                BAG_FLASH_VOICING_FLAVOR_HOSTILE),
            "hostile C API flash length should use the faster command timing with its own shell.");
        test::AssertTrue(
            hostile_pcm.sample_count < steady_pcm.sample_count,
            "hostile flash output should be shorter than steady for the same text.");
        test::AssertEq(
            zeal_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_ZEAL,
                BAG_FLASH_VOICING_FLAVOR_ZEAL),
            "zeal C API flash length should use the conservative faster signal timing.");
        test::AssertTrue(
            zeal_pcm.sample_count < steady_pcm.sample_count,
            "zeal flash output should be shorter than steady for the same text.");
        test::AssertEq(
            void_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_VOID,
                BAG_FLASH_VOICING_FLAVOR_VOID),
            "void C API flash length should use the slower signal timing.");
        test::AssertTrue(
            void_pcm.sample_count > steady_pcm.sample_count,
            "void flash output should be longer than steady for the same text.");

        const auto steady_decoded = DecodeViaApi(steady_decoder, steady_pcm);
        const auto litany_decoded = DecodeViaApi(litany_decoder, litany_pcm);
        const auto hostile_decoded = DecodeViaApi(hostile_decoder, hostile_pcm);
        const auto zeal_decoded = DecodeViaApi(zeal_decoder, zeal_pcm);
        const auto void_decoded = DecodeViaApi(void_decoder, void_pcm);
        test::AssertEq(steady_decoded.code, BAG_OK, "steady flash decode should succeed.");
        test::AssertEq(litany_decoded.code, BAG_OK, "litany flash decode should succeed.");
        test::AssertEq(hostile_decoded.code, BAG_OK, "hostile flash decode should succeed.");
        test::AssertEq(zeal_decoded.code, BAG_OK, "zeal flash decode should succeed.");
        test::AssertEq(void_decoded.code, BAG_OK, "void flash decode should succeed.");
        test::AssertEq(steady_decoded.text, text, "steady flash decode should preserve text.");
        test::AssertEq(litany_decoded.text, text, "litany flash decode should preserve text.");
        test::AssertEq(hostile_decoded.text, text, "hostile flash decode should preserve text.");
        test::AssertEq(zeal_decoded.text, text, "zeal flash decode should preserve text.");
        test::AssertEq(void_decoded.text, text, "void flash decode should preserve text.");

        bag_free_pcm16_result(&steady_pcm);
        bag_free_pcm16_result(&litany_pcm);
        bag_free_pcm16_result(&hostile_pcm);
        bag_free_pcm16_result(&zeal_pcm);
        bag_free_pcm16_result(&void_pcm);
    }
}

void TestApiFlashVoicingEmotionValuesRoundTrip() {
    struct EmotionCase {
        bag_flash_voicing_flavor flavor;
        const char* name;
    };
    const EmotionCase cases[] = {
        {BAG_FLASH_VOICING_FLAVOR_STEADY, "steady"},
        {BAG_FLASH_VOICING_FLAVOR_LITANY, "litany"},
        {BAG_FLASH_VOICING_FLAVOR_HOSTILE, "hostile"},
        {BAG_FLASH_VOICING_FLAVOR_COLLAPSE, "collapse"},
        {BAG_FLASH_VOICING_FLAVOR_ZEAL, "zeal"},
        {BAG_FLASH_VOICING_FLAVOR_VOID, "void"},
    };
    const auto config_case = test::ConfigCases().front();
    const std::string text = "Emotion";

    for (const auto& item : cases) {
        const auto encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                item.flavor);
        const auto decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                item.flavor);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_validate_encode_request(&encoder, text.c_str()),
            BAG_VALIDATION_OK,
            std::string(item.name) + " flash voicing flavor should validate.");
        test::AssertEq(
            bag_encode_text(&encoder, text.c_str(), &pcm),
            BAG_OK,
            std::string(item.name) + " flash voicing flavor should encode.");
        const auto decoded = DecodeViaApi(decoder, pcm);
        test::AssertEq(
            decoded.code,
            BAG_OK,
            std::string(item.name) + " flash voicing flavor should decode.");
        test::AssertEq(
            decoded.text,
            text,
            std::string(item.name) + " flash voicing flavor should preserve text.");
        bag_free_pcm16_result(&pcm);
    }
}

void TestApiFlashVoicingOnlyHostileKeepsSteadyLengthAndCollapseUsesVariableSilence() {
    const auto config_case = test::ConfigCases().front();
    const std::string text = "Shell collapse";
    const auto steady_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_STEADY);
    const auto hostile_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_HOSTILE);
    const auto collapse_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_COLLAPSE);
    const auto collapse_decoder =
        MakeDecoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_COLLAPSE);

    bag_pcm16_result steady_pcm{};
    bag_pcm16_result hostile_pcm{};
    bag_pcm16_result collapse_pcm{};
    test::AssertEq(bag_encode_text(&steady_encoder, text.c_str(), &steady_pcm), BAG_OK,
                   "steady flash encode should succeed.");
    test::AssertEq(bag_encode_text(&hostile_encoder, text.c_str(), &hostile_pcm), BAG_OK,
                   "hostile flash encode should succeed.");
    test::AssertEq(bag_encode_text(&collapse_encoder, text.c_str(), &collapse_pcm), BAG_OK,
                   "collapse flash encode should succeed.");
    test::AssertEq(
        hostile_pcm.sample_count,
        steady_pcm.sample_count,
        "hostile voicing alone should keep steady signal timing when the signal profile stays steady.");
    test::AssertTrue(
        collapse_pcm.sample_count > steady_pcm.sample_count,
        "collapse should include variable hesitation silence in its payload length.");
    const auto collapse_decoded = DecodeViaApi(collapse_decoder, collapse_pcm);
    test::AssertEq(collapse_decoded.code, BAG_OK, "collapse flash decode should skip variable silence.");
    test::AssertEq(collapse_decoded.text, text, "collapse variable silence should preserve text.");
    bag_free_pcm16_result(&steady_pcm);
    bag_free_pcm16_result(&hostile_pcm);
    bag_free_pcm16_result(&collapse_pcm);
}

void TestApiFlashDecodeRequiresMatchingConfig() {
    const auto config_case = test::ConfigCases().front();
    const auto ritual_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_LITANY,
            BAG_FLASH_VOICING_FLAVOR_LITANY);
    const auto wrong_decoder =
        MakeDecoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_STEADY);
    const std::string text = "Mismatch";

    bag_pcm16_result ritual_pcm{};
    test::AssertEq(
        bag_encode_text(&ritual_encoder, text.c_str(), &ritual_pcm),
        BAG_OK,
        "litany flash encode should succeed before wrong-style decode validation.");

    const auto decoded = DecodeViaApi(wrong_decoder, ritual_pcm);
    test::AssertTrue(
        decoded.code != BAG_OK || decoded.text != text,
        "Decoding litany flash PCM with steady signal/flavor should not look like a valid roundtrip.");
    bag_free_pcm16_result(&ritual_pcm);

    const auto zeal_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_ZEAL,
            BAG_FLASH_VOICING_FLAVOR_ZEAL);
    const std::string zeal_text = "ZealMismatch";
    bag_pcm16_result zeal_pcm{};
    test::AssertEq(
        bag_encode_text(&zeal_encoder, zeal_text.c_str(), &zeal_pcm),
        BAG_OK,
        "zeal flash encode should succeed before wrong-style decode validation.");

    const auto zeal_decoded = DecodeViaApi(wrong_decoder, zeal_pcm);
    test::AssertTrue(
        zeal_decoded.code != BAG_OK || zeal_decoded.text != zeal_text,
        "Decoding zeal flash PCM with steady signal/flavor should not look like a valid roundtrip.");
    bag_free_pcm16_result(&zeal_pcm);

    const auto void_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_VOID,
            BAG_FLASH_VOICING_FLAVOR_VOID);
    const std::string void_text = "VoidMismatch";
    bag_pcm16_result void_pcm{};
    test::AssertEq(
        bag_encode_text(&void_encoder, void_text.c_str(), &void_pcm),
        BAG_OK,
        "void flash encode should succeed before wrong-style decode validation.");

    const auto void_decoded = DecodeViaApi(wrong_decoder, void_pcm);
    test::AssertTrue(
        void_decoded.code != BAG_OK || void_decoded.text != void_text,
        "Decoding void flash PCM with steady signal/flavor should not look like a valid roundtrip.");
    bag_free_pcm16_result(&void_pcm);
}

void TestApiFlashZealVariableCadenceRoundTrip() {
    const auto config_case = test::ConfigCases().front();
    const auto zeal_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_ZEAL,
            BAG_FLASH_VOICING_FLAVOR_ZEAL);
    const auto zeal_decoder =
        MakeDecoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_ZEAL,
            BAG_FLASH_VOICING_FLAVOR_ZEAL);
    const std::string text = "Zeal!Burst";

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&zeal_encoder, text.c_str(), &pcm),
        BAG_OK,
        "zeal variable-cadence flash encode should succeed.");
    test::AssertEq(
        pcm.sample_count,
        ExpectedFlashSampleCount(
            text,
            config_case,
            BAG_FLASH_SIGNAL_PROFILE_ZEAL,
            BAG_FLASH_VOICING_FLAVOR_ZEAL),
        "zeal variable-cadence flash length should include deterministic punctuation pauses.");

    const auto decoded = DecodeViaApi(zeal_decoder, pcm);
    test::AssertEq(decoded.code, BAG_OK, "zeal variable-cadence decode should succeed.");
    test::AssertEq(decoded.text, text, "zeal variable-cadence decode should preserve text.");
    bag_free_pcm16_result(&pcm);
}

void TestApiFlashSignalInfoDerivedFromLayout() {
    const auto config_case = test::ConfigCases().front();
    const auto steady_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_STEADY);
    std::array<char, 64> low_buffer{};
    std::array<char, 64> high_buffer{};
    std::array<char, 128> bit_buffer{};
    std::array<char, 128> silence_buffer{};
    std::array<char, 128> decode_buffer{};
    bag_flash_signal_info steady_info{};
    steady_info.low_carrier_hz_buffer = low_buffer.data();
    steady_info.low_carrier_hz_buffer_size = low_buffer.size();
    steady_info.high_carrier_hz_buffer = high_buffer.data();
    steady_info.high_carrier_hz_buffer_size = high_buffer.size();
    steady_info.bit_duration_samples_buffer = bit_buffer.data();
    steady_info.bit_duration_samples_buffer_size = bit_buffer.size();
    steady_info.payload_silence_buffer = silence_buffer.data();
    steady_info.payload_silence_buffer_size = silence_buffer.size();
    steady_info.decode_path_buffer = decode_buffer.data();
    steady_info.decode_path_buffer_size = decode_buffer.size();

    test::AssertEq(
        bag_describe_flash_signal(&steady_encoder, "Info", &steady_info),
        BAG_OK,
        "steady flash signal info should be derived from the core layout.");
    test::AssertTrue(steady_info.available != 0, "steady flash signal info should be available.");
    test::AssertEq(std::string(low_buffer.data(), steady_info.low_carrier_hz_size), "300",
                   "steady low carrier info should match the rendered layout.");
    test::AssertEq(std::string(high_buffer.data(), steady_info.high_carrier_hz_size), "600",
                   "steady high carrier info should match the rendered layout.");
    test::AssertEq(std::string(bit_buffer.data(), steady_info.bit_duration_samples_size),
                   std::to_string((static_cast<std::size_t>(config_case.frame_samples) *
                                   static_cast<std::size_t>(15)) /
                                  static_cast<std::size_t>(16)),
                   "steady bit duration info should match the conservative faster timing.");
    test::AssertEq(std::string(silence_buffer.data(), steady_info.payload_silence_size), "none",
                   "steady silence info should report no payload silence.");
    test::AssertEq(std::string(decode_buffer.data(), steady_info.decode_path_size), "fixed low/high window",
                   "steady decode path info should report fixed-window decode.");

    const auto zeal_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_ZEAL,
            BAG_FLASH_VOICING_FLAVOR_ZEAL);
    low_buffer.fill('\0');
    high_buffer.fill('\0');
    bit_buffer.fill('\0');
    silence_buffer.fill('\0');
    decode_buffer.fill('\0');
    bag_flash_signal_info zeal_info{};
    zeal_info.low_carrier_hz_buffer = low_buffer.data();
    zeal_info.low_carrier_hz_buffer_size = low_buffer.size();
    zeal_info.high_carrier_hz_buffer = high_buffer.data();
    zeal_info.high_carrier_hz_buffer_size = high_buffer.size();
    zeal_info.bit_duration_samples_buffer = bit_buffer.data();
    zeal_info.bit_duration_samples_buffer_size = bit_buffer.size();
    zeal_info.payload_silence_buffer = silence_buffer.data();
    zeal_info.payload_silence_buffer_size = silence_buffer.size();
    zeal_info.decode_path_buffer = decode_buffer.data();
    zeal_info.decode_path_buffer_size = decode_buffer.size();

    test::AssertEq(
        bag_describe_flash_signal(&zeal_encoder, "A!B", &zeal_info),
        BAG_OK,
        "zeal flash signal info should include variable layout data.");
    test::AssertEq(std::string(low_buffer.data(), zeal_info.low_carrier_hz_size),
                   "560 / 660 / 760 / 900",
                   "zeal low carrier info should expose all rendered low carriers.");
    test::AssertEq(std::string(high_buffer.data(), zeal_info.high_carrier_hz_size),
                   "1120 / 1320 / 1520 / 1800",
                   "zeal high carrier info should expose all rendered high carriers.");
    test::AssertEq(std::string(bit_buffer.data(), zeal_info.bit_duration_samples_size),
                   "1102 / 1378 / 1653 / 2205",
                   "zeal bit duration info should expose variable sample windows.");
    test::AssertEq(std::string(silence_buffer.data(), zeal_info.payload_silence_size),
                   "4 slot gap",
                   "zeal punctuation silence should be derived from payload layout.");
    test::AssertEq(std::string(decode_buffer.data(), zeal_info.decode_path_size),
                   "variable-window gap-aware",
                   "zeal decode path should report its specialized decoder.");

    const auto void_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_VOID,
            BAG_FLASH_VOICING_FLAVOR_VOID);
    low_buffer.fill('\0');
    high_buffer.fill('\0');
    bit_buffer.fill('\0');
    silence_buffer.fill('\0');
    decode_buffer.fill('\0');
    bag_flash_signal_info void_info{};
    void_info.low_carrier_hz_buffer = low_buffer.data();
    void_info.low_carrier_hz_buffer_size = low_buffer.size();
    void_info.high_carrier_hz_buffer = high_buffer.data();
    void_info.high_carrier_hz_buffer_size = high_buffer.size();
    void_info.bit_duration_samples_buffer = bit_buffer.data();
    void_info.bit_duration_samples_buffer_size = bit_buffer.size();
    void_info.payload_silence_buffer = silence_buffer.data();
    void_info.payload_silence_buffer_size = silence_buffer.size();
    void_info.decode_path_buffer = decode_buffer.data();
    void_info.decode_path_buffer_size = decode_buffer.size();

    test::AssertEq(
        bag_describe_flash_signal(&void_encoder, "Info", &void_info),
        BAG_OK,
        "void flash signal info should be derived from the core layout.");
    test::AssertEq(std::string(low_buffer.data(), void_info.low_carrier_hz_size),
                   "220",
                   "void low carrier info should match the slower low register.");
    test::AssertEq(std::string(high_buffer.data(), void_info.high_carrier_hz_size),
                   "440",
                   "void high carrier info should match the slower low register.");
    test::AssertEq(std::string(bit_buffer.data(), void_info.bit_duration_samples_size),
                   std::to_string((static_cast<std::size_t>(config_case.frame_samples) *
                                   static_cast<std::size_t>(5)) /
                                  static_cast<std::size_t>(2)),
                   "void bit duration info should match the 2.5x signal timing.");
    test::AssertEq(std::string(silence_buffer.data(), void_info.payload_silence_size),
                   "none",
                   "void silence info should report a continuous payload.");
    test::AssertEq(std::string(decode_buffer.data(), void_info.decode_path_size),
                   "fixed low/high window",
                   "void decode path should remain fixed-window for stability.");
}

}  // namespace

namespace api_tests {

void RegisterApiFlashTests(test::Runner& runner) {
    runner.Add("Api.FlashConfigAffectsLengthAndRoundTrip", TestApiFlashConfigAffectsLengthAndRoundTrip);
    runner.Add("Api.FlashVoicingEmotionValuesRoundTrip", TestApiFlashVoicingEmotionValuesRoundTrip);
    runner.Add("Api.FlashVoicingOnlyHostileKeepsSteadyLengthAndCollapseUsesVariableSilence",
               TestApiFlashVoicingOnlyHostileKeepsSteadyLengthAndCollapseUsesVariableSilence);
    runner.Add("Api.FlashDecodeRequiresMatchingConfig", TestApiFlashDecodeRequiresMatchingConfig);
    runner.Add("Api.FlashZealVariableCadenceRoundTrip", TestApiFlashZealVariableCadenceRoundTrip);
    runner.Add("Api.FlashSignalInfoDerivedFromLayout", TestApiFlashSignalInfoDerivedFromLayout);
}

}  // namespace api_tests
