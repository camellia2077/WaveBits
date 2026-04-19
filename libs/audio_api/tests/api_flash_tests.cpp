#include <string>

#include "api_test_support.h"

namespace {

using namespace api_tests;

void TestApiFlashConfigAffectsLengthAndRoundTrip() {
    const std::string text = "Length";

    for (const auto& config_case : test::ConfigCases()) {
        const auto coded_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                BAG_FLASH_VOICING_FLAVOR_CODED_BURST);
        const auto coded_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                BAG_FLASH_VOICING_FLAVOR_CODED_BURST);
        const auto ritual_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
                BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);
        const auto ritual_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
                BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);

        bag_pcm16_result coded_pcm{};
        bag_pcm16_result ritual_pcm{};
        test::AssertEq(
            bag_encode_text(&coded_encoder, text.c_str(), &coded_pcm),
            BAG_OK,
            "coded_burst flash encode should succeed through the C API.");
        test::AssertEq(
            bag_encode_text(&ritual_encoder, text.c_str(), &ritual_pcm),
            BAG_OK,
            "ritual_chant flash encode should succeed through the C API.");
        test::AssertEq(
            coded_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                BAG_FLASH_VOICING_FLAVOR_CODED_BURST),
            "coded_burst C API flash length should stay on the baseline explicit configuration.");
        test::AssertEq(
            ritual_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
                BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT),
            "ritual_chant C API flash length should include the longer timing and shell configuration.");
        test::AssertTrue(
            ritual_pcm.sample_count > coded_pcm.sample_count,
            "ritual_chant flash output should be longer than coded_burst for the same text.");

        const auto coded_decoded = DecodeViaApi(coded_decoder, coded_pcm);
        const auto ritual_decoded = DecodeViaApi(ritual_decoder, ritual_pcm);
        test::AssertEq(coded_decoded.code, BAG_OK, "coded_burst flash decode should succeed.");
        test::AssertEq(ritual_decoded.code, BAG_OK, "ritual_chant flash decode should succeed.");
        test::AssertEq(coded_decoded.text, text, "coded_burst flash decode should preserve text.");
        test::AssertEq(ritual_decoded.text, text, "ritual_chant flash decode should preserve text.");

        bag_free_pcm16_result(&coded_pcm);
        bag_free_pcm16_result(&ritual_pcm);
    }
}

void TestApiFlashDecodeRequiresMatchingConfig() {
    const auto config_case = test::ConfigCases().front();
    const auto ritual_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
            BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);
    const auto wrong_decoder =
        MakeDecoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
            BAG_FLASH_VOICING_FLAVOR_CODED_BURST);
    const std::string text = "Mismatch";

    bag_pcm16_result ritual_pcm{};
    test::AssertEq(
        bag_encode_text(&ritual_encoder, text.c_str(), &ritual_pcm),
        BAG_OK,
        "ritual_chant flash encode should succeed before wrong-style decode validation.");

    const auto decoded = DecodeViaApi(wrong_decoder, ritual_pcm);
    test::AssertTrue(
        decoded.code != BAG_OK || decoded.text != text,
        "Decoding ritual flash PCM with coded_burst signal/flavor should not look like a valid roundtrip.");
    bag_free_pcm16_result(&ritual_pcm);
}

}  // namespace

namespace api_tests {

void RegisterApiFlashTests(test::Runner& runner) {
    runner.Add("Api.FlashConfigAffectsLengthAndRoundTrip", TestApiFlashConfigAffectsLengthAndRoundTrip);
    runner.Add("Api.FlashDecodeRequiresMatchingConfig", TestApiFlashDecodeRequiresMatchingConfig);
}

}  // namespace api_tests
