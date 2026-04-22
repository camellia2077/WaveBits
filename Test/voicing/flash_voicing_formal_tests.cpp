#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

#include "flash_voicing_test_support.h"

namespace {

using namespace flash_voicing_test;

void TestFormalRitualChantPayloadTailStaysCloseToCodedBurst() {
    const auto config = MakeAndroidSizedCoreConfig();
    const auto signal_profile = bag::FlashSignalProfile::kRitualChant;
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(config, signal_profile);
    const auto payload_layout = bag::flash::BuildPayloadLayout(AsBytes("A"), signal_config);

    std::vector<std::int16_t> coded_burst_pcm;
    std::vector<std::int16_t> ritual_chant_pcm;
    const auto coded_burst_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "A",
            signal_profile,
            bag::FlashVoicingFlavor::kCodedBurst,
            &coded_burst_pcm);
    const auto ritual_chant_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "A",
            signal_profile,
            bag::FlashVoicingFlavor::kRitualChant,
            &ritual_chant_pcm);

    test::AssertEq(coded_burst_encode_code, bag::ErrorCode::kOk, "coded_burst formal encode should succeed.");
    test::AssertEq(ritual_chant_encode_code, bag::ErrorCode::kOk, "ritual_chant formal encode should succeed.");

    const auto coded_trimmed = bag::flash::TrimToPayloadPcm(
        coded_burst_pcm,
        bag::flash::DescribeVoicingOutput(
            coded_burst_pcm.size(),
            bag::flash::MakeFormalVoicingConfigForFlavor(
                config,
                bag::FlashVoicingFlavor::kCodedBurst)));
    const auto ritual_trimmed = bag::flash::TrimToPayloadPcm(
        ritual_chant_pcm,
        bag::flash::DescribeVoicingOutput(
            ritual_chant_pcm.size(),
            bag::flash::MakeFormalVoicingConfigForFlavor(
                config,
                bag::FlashVoicingFlavor::kRitualChant)));

    test::AssertEq(
        coded_trimmed.size(),
        payload_layout.payload_sample_count,
        "coded_burst payload trimming should preserve the ritual signal-profile payload length.");
    test::AssertEq(
        ritual_trimmed.size(),
        payload_layout.payload_sample_count,
        "ritual_chant payload trimming should preserve the ritual signal-profile payload length.");
    test::AssertTrue(
        ritual_trimmed != coded_trimmed,
        "ritual_chant payload should remain distinct from coded_burst under the same signal profile.");

    const auto& first_chunk = payload_layout.chunks.front();
    const auto [body_offset_begin, body_offset_end] =
        FractionalRange(first_chunk.sample_count, 0.25, 0.625);
    const auto [tail_offset_begin, tail_offset_end] =
        FractionalRange(first_chunk.sample_count, 0.875, 1.0);
    const std::size_t body_begin = first_chunk.sample_offset + body_offset_begin;
    const std::size_t body_end = first_chunk.sample_offset + body_offset_end;
    const std::size_t tail_begin = first_chunk.sample_offset + tail_offset_begin;
    const std::size_t tail_end = first_chunk.sample_offset + tail_offset_end;

    const double coded_body_energy =
        AverageAbsoluteSample(coded_trimmed, body_begin, body_end);
    const double ritual_body_energy =
        AverageAbsoluteSample(ritual_trimmed, body_begin, body_end);
    const double coded_tail_energy =
        AverageAbsoluteSample(coded_trimmed, tail_begin, tail_end);
    const double ritual_tail_energy =
        AverageAbsoluteSample(ritual_trimmed, tail_begin, tail_end);
    const double coded_tail_ratio =
        coded_tail_energy / std::max(coded_body_energy, 1.0);
    const double ritual_tail_ratio =
        ritual_tail_energy / std::max(ritual_body_energy, 1.0);
    const double coded_tail_brightness =
        AverageNormalizedFirstDifference(coded_trimmed, tail_begin, tail_end);
    const double ritual_tail_brightness =
        AverageNormalizedFirstDifference(ritual_trimmed, tail_begin, tail_end);

    test::AssertTrue(
        ritual_tail_ratio <= coded_tail_ratio * 1.25,
        "ritual_chant payload tail should stay close to coded_burst instead of blooming into a long sustain.");
    test::AssertTrue(
        ritual_tail_brightness <= coded_tail_brightness * 1.15,
        "ritual_chant payload tail should remain close to coded_burst brightness after the aggressive convergence tuning.");
}

void TestFormalRitualChantHasLongerShellThanCodedBurst() {
    auto coded_config = MakeAndroidSizedCoreConfig();
    auto ritual_config = MakeAndroidSizedCoreConfig();
    ritual_config.flash_signal_profile = bag::FlashSignalProfile::kRitualChant;
    ritual_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    std::vector<std::int16_t> coded_burst_pcm;
    std::vector<std::int16_t> ritual_chant_pcm;

    const auto coded_burst_code = bag::flash::EncodeTextToPcm16(
        coded_config,
        "Length",
        &coded_burst_pcm);
    const auto ritual_chant_code = bag::flash::EncodeTextToPcm16(
        ritual_config,
        "Length",
        &ritual_chant_pcm);

    test::AssertEq(coded_burst_code, bag::ErrorCode::kOk, "coded_burst formal encode should succeed.");
    test::AssertEq(ritual_chant_code, bag::ErrorCode::kOk, "ritual_chant formal encode should succeed.");
    test::AssertTrue(
        ritual_chant_pcm.size() > coded_burst_pcm.size(),
        "ritual_chant should use a longer preamble and epilogue than coded_burst.");
    test::AssertTrue(
        (ritual_chant_pcm.size() - coded_burst_pcm.size()) > static_cast<std::size_t>(coded_config.sample_rate_hz),
        "ritual_chant should exceed coded_burst by more than one second under the Android default frame size so coarse UI timers show a clear difference.");
}

void TestFormalRitualChantDecodesWithConfiguredTrim() {
    auto ritual_config = MakeCoreConfig();
    ritual_config.flash_signal_profile = bag::FlashSignalProfile::kRitualChant;
    ritual_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    std::vector<std::int16_t> ritual_chant_pcm;
    const auto encode_code = bag::flash::EncodeTextToPcm16(
        ritual_config,
        "Decode",
        &ritual_chant_pcm);
    test::AssertEq(encode_code, bag::ErrorCode::kOk, "ritual_chant formal encode should succeed.");

    std::string decoded_text;
    const auto decode_code = bag::flash::DecodePcm16ToText(
        ritual_config,
        ritual_chant_pcm,
        &decoded_text);
    test::AssertEq(decode_code, bag::ErrorCode::kOk, "configured ritual_chant decode should succeed.");
    test::AssertEq(decoded_text, std::string("Decode"), "configured ritual_chant decode should roundtrip text.");
}

void TestFormalDeepRitualHasLongerShellThanRitualChant() {
    auto ritual_config = MakeAndroidSizedCoreConfig();
    auto deep_config = MakeAndroidSizedCoreConfig();
    ritual_config.flash_signal_profile = bag::FlashSignalProfile::kRitualChant;
    ritual_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    deep_config.flash_signal_profile = bag::FlashSignalProfile::kDeepRitual;
    deep_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kDeepRitual;
    std::vector<std::int16_t> ritual_pcm;
    std::vector<std::int16_t> deep_pcm;

    const auto ritual_code = bag::flash::EncodeTextToPcm16(
        ritual_config,
        "Length",
        &ritual_pcm);
    const auto deep_code = bag::flash::EncodeTextToPcm16(
        deep_config,
        "Length",
        &deep_pcm);

    test::AssertEq(ritual_code, bag::ErrorCode::kOk, "ritual_chant formal encode should succeed.");
    test::AssertEq(deep_code, bag::ErrorCode::kOk, "deep_ritual formal encode should succeed.");
    test::AssertTrue(
        deep_pcm.size() > ritual_pcm.size(),
        "deep_ritual should use a longer preamble, epilogue, and payload timing than ritual_chant.");
}

void TestFormalDeepRitualDecodesWithConfiguredTrim() {
    auto deep_config = MakeCoreConfig();
    deep_config.flash_signal_profile = bag::FlashSignalProfile::kDeepRitual;
    deep_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kDeepRitual;
    std::vector<std::int16_t> deep_pcm;
    const auto encode_code = bag::flash::EncodeTextToPcm16(
        deep_config,
        "DeepDecode",
        &deep_pcm);
    test::AssertEq(encode_code, bag::ErrorCode::kOk, "deep_ritual formal encode should succeed.");

    std::string decoded_text;
    const auto decode_code = bag::flash::DecodePcm16ToText(
        deep_config,
        deep_pcm,
        &decoded_text);
    test::AssertEq(decode_code, bag::ErrorCode::kOk, "configured deep_ritual decode should succeed.");
    test::AssertEq(decoded_text, std::string("DeepDecode"), "configured deep_ritual decode should roundtrip text.");
}

void TestExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor() {
    const auto config = MakeAndroidSizedCoreConfig();
    const auto signal_profile = bag::FlashSignalProfile::kCodedBurst;
    const std::string text = "Decouple";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(config, signal_profile);
    const auto payload_layout =
        bag::flash::BuildPayloadLayout(AsBytes(text), signal_config);
    const std::size_t expected_payload_sample_count = payload_layout.payload_sample_count;

    std::vector<std::int16_t> coded_burst_pcm;
    std::vector<std::int16_t> ritual_chant_pcm;
    const auto coded_burst_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kCodedBurst,
            &coded_burst_pcm);
    const auto ritual_chant_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kRitualChant,
            &ritual_chant_pcm);

    test::AssertEq(
        coded_burst_encode_code,
        bag::ErrorCode::kOk,
        "explicit coded_burst encode should succeed with an explicit signal profile.");
    test::AssertEq(
        ritual_chant_encode_code,
        bag::ErrorCode::kOk,
        "explicit ritual_chant encode should succeed with an explicit signal profile.");
    test::AssertEq(
        coded_burst_pcm.size(),
        expected_payload_sample_count +
            FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kCodedBurst) +
            FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kCodedBurst),
        "explicit coded_burst encode should add only the coded_burst shell on top of the shared payload timing.");
    test::AssertEq(
        ritual_chant_pcm.size(),
        expected_payload_sample_count +
            FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kRitualChant) +
            FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kRitualChant),
        "explicit ritual_chant encode should reuse the same payload timing and only change the shell.");
    test::AssertEq(
        ritual_chant_pcm.size() - coded_burst_pcm.size(),
        (FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kRitualChant) +
         FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kRitualChant)) -
            (FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kCodedBurst) +
             FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kCodedBurst)),
        "switching voicing flavor under one explicit signal profile should change only shell samples, not payload timing.");

    std::string coded_burst_decoded;
    std::string ritual_chant_decoded;
    const auto coded_burst_decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            coded_burst_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kCodedBurst,
            &coded_burst_decoded);
    const auto ritual_chant_decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            ritual_chant_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kRitualChant,
            &ritual_chant_decoded);

    test::AssertEq(
        coded_burst_decode_code,
        bag::ErrorCode::kOk,
        "explicit coded_burst decode should succeed with the shared signal profile.");
    test::AssertEq(
        ritual_chant_decode_code,
        bag::ErrorCode::kOk,
        "explicit ritual_chant decode should succeed with the shared signal profile.");
    test::AssertEq(
        coded_burst_decoded,
        text,
        "explicit coded_burst decode should preserve the original text.");
    test::AssertEq(
        ritual_chant_decoded,
        text,
        "explicit ritual_chant decode should preserve the original text under the same signal profile.");
}

void TestExplicitSignalProfileAndFlavorMatchDefaultExplicitPath() {
    auto config = MakeAndroidSizedCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kCodedBurst;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    std::vector<std::int16_t> default_pcm;
    std::vector<std::int16_t> explicit_pcm;

    const auto default_encode_code =
        bag::flash::EncodeTextToPcm16(config, "FlavorPath", &default_pcm);
    const auto explicit_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "FlavorPath",
            bag::FlashSignalProfile::kCodedBurst,
            bag::FlashVoicingFlavor::kRitualChant,
            &explicit_pcm);

    test::AssertEq(default_encode_code, bag::ErrorCode::kOk, "default explicit-flavor encode should succeed.");
    test::AssertEq(explicit_encode_code, bag::ErrorCode::kOk, "explicit signal-profile-and-flavor encode should succeed.");
    test::AssertEq(
        default_pcm,
        explicit_pcm,
        "Default flash encode should match the explicit signal-profile-and-flavor path when explicit components are present.");

    std::string decoded_text;
    const auto decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            explicit_pcm,
            bag::FlashSignalProfile::kCodedBurst,
            bag::FlashVoicingFlavor::kRitualChant,
            &decoded_text);
    test::AssertEq(
        decode_code,
        bag::ErrorCode::kOk,
        "Explicit signal-profile-and-flavor decode should succeed.");
    test::AssertEq(
        decoded_text,
        std::string("FlavorPath"),
        "Explicit signal-profile-and-flavor decode should preserve the original text.");
}

void TestDefaultFormalFlashRemainsCodedBurstBaseline() {
    std::vector<std::int16_t> default_pcm;
    std::vector<std::int16_t> coded_burst_pcm;

    const auto default_encode_code = bag::flash::EncodeTextToPcm16(
        MakeCoreConfig(),
        "Baseline",
        &default_pcm);
    const auto coded_burst_encode_code = bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
        MakeCoreConfig(),
        "Baseline",
        bag::FlashSignalProfile::kCodedBurst,
        bag::FlashVoicingFlavor::kCodedBurst,
        &coded_burst_pcm);
    test::AssertEq(default_encode_code, bag::ErrorCode::kOk, "default formal flash encode should succeed.");
    test::AssertEq(coded_burst_encode_code, bag::ErrorCode::kOk, "coded_burst formal encode should succeed.");
    test::AssertEq(default_pcm, coded_burst_pcm, "default formal flash should remain aligned with coded_burst.");

    std::string decoded_text;
    const auto decode_code = bag::flash::DecodePcm16ToText(MakeCoreConfig(), default_pcm, &decoded_text);
    test::AssertEq(decode_code, bag::ErrorCode::kOk, "default formal flash decode should succeed.");
    test::AssertEq(decoded_text, std::string("Baseline"), "default formal flash decode should remain coded_burst-compatible.");
}

}  // namespace

namespace flash_voicing_test {

void RegisterFlashVoicingFormalTests(test::Runner& runner) {
    runner.Add("FlashVoicing.FormalRitualChantPayloadTailStaysCloseToCodedBurst",
               TestFormalRitualChantPayloadTailStaysCloseToCodedBurst);
    runner.Add("FlashVoicing.FormalRitualChantHasLongerShellThanCodedBurst",
               TestFormalRitualChantHasLongerShellThanCodedBurst);
    runner.Add("FlashVoicing.FormalRitualChantDecodesWithConfiguredTrim",
               TestFormalRitualChantDecodesWithConfiguredTrim);
    runner.Add("FlashVoicing.FormalDeepRitualHasLongerShellThanRitualChant",
               TestFormalDeepRitualHasLongerShellThanRitualChant);
    runner.Add("FlashVoicing.FormalDeepRitualDecodesWithConfiguredTrim",
               TestFormalDeepRitualDecodesWithConfiguredTrim);
    runner.Add("FlashVoicing.ExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor",
               TestExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor);
    runner.Add("FlashVoicing.ExplicitSignalProfileAndFlavorMatchDefaultExplicitPath",
               TestExplicitSignalProfileAndFlavorMatchDefaultExplicitPath);
    runner.Add("FlashVoicing.DefaultFormalFlashRemainsCodedBurstBaseline",
               TestDefaultFormalFlashRemainsCodedBurstBaseline);
}

}  // namespace flash_voicing_test
