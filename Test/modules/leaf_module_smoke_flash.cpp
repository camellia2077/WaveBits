#include "test_std_support.h"
#include "test_framework.h"
#include "test_utf8.h"

import bag.flash.codec;
import bag.flash.signal;
import bag.flash.voicing;
import bag.flash.phy_clean;

#include "leaf_module_smoke_support.h"

namespace {

using namespace modules_leaf_smoke;

void TestFlashCodecModule() {
    const std::string text = test::Utf8Literal(u8"你好，FlipBits");
    std::vector<std::uint8_t> bytes;
    test::AssertEq(
        bag::flash::EncodeTextToBytes(text, &bytes),
        bag::ErrorCode::kOk,
        "Flash codec module should encode UTF-8 bytes.");
    test::AssertEq(
        bytes,
        std::vector<std::uint8_t>(text.begin(), text.end()),
        "Flash codec module should preserve raw byte payload.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodeBytesToText(bytes, &decoded),
        bag::ErrorCode::kOk,
        "Flash codec module should decode raw bytes.");
    test::AssertEq(decoded, text, "Flash codec module should roundtrip UTF-8 text.");
}

void TestFlashSignalLayoutMatchesExpected() {
    const auto config = MakeBfskConfig();
    const auto layout = bag::flash::BuildPayloadLayout(AsBytes("A"), config);
    const std::size_t chunk_size = config.samples_per_bit;

    test::AssertEq(
        layout.chunks.size(),
        static_cast<std::size_t>(8),
        "Flash signal module should emit one payload chunk per bit.");
    test::AssertEq(
        layout.payload_sample_count,
        static_cast<std::size_t>(8) * chunk_size,
        "Flash signal module payload layout should match the PCM sample budget.");
    test::AssertEq(
        layout.chunks.front().bit_value,
        static_cast<std::uint8_t>(0),
        "Flash signal module should keep the first bit for 'A' as 0.");
    test::AssertEq(
        layout.chunks[1].bit_value,
        static_cast<std::uint8_t>(1),
        "Flash signal module should keep the second bit for 'A' as 1.");
    test::AssertEq(
        layout.chunks[1].sample_offset,
        chunk_size,
        "Flash signal module should advance payload chunk offsets by one chunk size.");
    test::AssertEq(
        layout.chunks.front().carrier_freq_hz,
        config.low_freq_hz,
        "Flash signal module should map 0 bits to the low carrier.");
    test::AssertEq(
        layout.chunks[1].carrier_freq_hz,
        config.high_freq_hz,
        "Flash signal module should map 1 bits to the high carrier.");
}

void TestFlashSignalEncodeLengthMatchesExpected() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("A"), config);
    const std::size_t chunk_size = config.samples_per_bit;
    test::AssertEq(
        pcm.size(),
        static_cast<std::size_t>(8) * chunk_size,
        "Flash signal module should emit 8 bits times chunk size for one byte.");
}

void TestFlashSignalStyleAwareChunkSizeMatchesConfig() {
    const auto steady_config = MakeFlashCoreConfig();
    const auto steady_signal = bag::flash::MakeBfskConfig(steady_config);
    const auto litany_signal = bag::flash::MakeBfskConfig(MakeLitanyFlashCoreConfig());
    const auto hostile_signal = bag::flash::MakeBfskConfig(MakeHostileFlashCoreConfig());
    const auto collapse_signal = bag::flash::MakeBfskConfig(MakeCollapseFlashCoreConfig());
    const auto zeal_signal = bag::flash::MakeBfskConfig(MakeZealFlashCoreConfig());
    const auto steady_frame_samples = static_cast<std::size_t>(steady_config.frame_samples);

    test::AssertEq(
        steady_signal.samples_per_bit,
        (steady_frame_samples * static_cast<std::size_t>(15)) / static_cast<std::size_t>(16),
        "steady flash signal should use the conservative faster timing profile.");
    test::AssertEq(
        steady_signal.low_freq_hz,
        300.0,
        "steady flash signal should use the lower everyday voice low carrier.");
    test::AssertEq(
        steady_signal.high_freq_hz,
        600.0,
        "steady flash signal should use the lower everyday voice high carrier.");
    test::AssertEq(
        litany_signal.samples_per_bit,
        static_cast<std::size_t>(13230),
        "litany flash signal should use the longer 6x bit timing profile.");
    test::AssertEq(
        litany_signal.low_freq_hz,
        220.0,
        "litany flash signal should use the solemn low carrier.");
    test::AssertEq(
        litany_signal.high_freq_hz,
        440.0,
        "litany flash signal should use the solemn high carrier.");
    test::AssertTrue(
        litany_signal.samples_per_bit > steady_signal.samples_per_bit,
        "litany flash signal should use more samples per bit than steady.");
    test::AssertEq(
        hostile_signal.samples_per_bit,
        static_cast<std::size_t>(1929),
        "hostile flash signal should use the faster 7/8 bit timing profile.");
    test::AssertEq(
        hostile_signal.low_freq_hz,
        450.0,
        "hostile flash signal should use the aggressive high-tension low carrier.");
    test::AssertEq(
        hostile_signal.high_freq_hz,
        900.0,
        "hostile flash signal should use the aggressive high-tension high carrier.");
    test::AssertEq(
        collapse_signal.samples_per_bit,
        static_cast<std::size_t>(2205),
        "collapse flash signal should keep one frame per bit.");
    test::AssertEq(
        collapse_signal.low_freq_hz,
        280.0,
        "collapse flash signal should use the lower frightened low carrier.");
    test::AssertEq(
        collapse_signal.high_freq_hz,
        560.0,
        "collapse flash signal should use the lower frightened high carrier.");
    test::AssertEq(
        zeal_signal.samples_per_bit,
        static_cast<std::size_t>(1102),
        "zeal flash signal should expose its shortest burst timing as the nominal bit size.");
    test::AssertEq(
        zeal_signal.samples_per_silence_slot,
        static_cast<std::size_t>(2205),
        "zeal flash signal should keep one frame silence slots for punctuation pauses.");
    test::AssertEq(
        zeal_signal.low_freq_hz,
        900.0,
        "zeal flash signal should expose the burst low carrier.");
    test::AssertEq(
        zeal_signal.high_freq_hz,
        1800.0,
        "zeal flash signal should expose the burst high carrier.");
}

void TestFlashSignalExplicitProfileSelectsEmotionTiming() {
    const auto litany_config = MakeLitanyFlashCoreConfig();
    const auto explicit_steady_signal =
        bag::flash::MakeBfskConfigForSignalProfile(
            litany_config,
            bag::FlashSignalProfile::kSteady);
    const auto explicit_litany_signal =
        bag::flash::MakeBfskConfigForSignalProfile(
            litany_config,
            bag::FlashSignalProfile::kLitany);
    const auto explicit_hostile_signal =
        bag::flash::MakeBfskConfigForSignalProfile(
            litany_config,
            bag::FlashSignalProfile::kHostile);
    const auto explicit_collapse_signal =
        bag::flash::MakeBfskConfigForSignalProfile(
            litany_config,
            bag::FlashSignalProfile::kCollapse);
    const auto explicit_zeal_signal =
        bag::flash::MakeBfskConfigForSignalProfile(
            litany_config,
            bag::FlashSignalProfile::kZeal);

    test::AssertEq(
        explicit_steady_signal.samples_per_bit,
        (static_cast<std::size_t>(litany_config.frame_samples) * static_cast<std::size_t>(15)) /
            static_cast<std::size_t>(16),
        "Explicit steady signal profile should keep steady timing even when litany config is in scope.");
    test::AssertEq(
        explicit_steady_signal.low_freq_hz,
        300.0,
        "Explicit steady signal profile should keep steady carrier tuning.");
    test::AssertEq(
        explicit_litany_signal.samples_per_bit,
        static_cast<std::size_t>(13230),
        "Explicit litany signal profile should keep litany timing when requested.");
    test::AssertEq(
        explicit_litany_signal.low_freq_hz,
        220.0,
        "Explicit litany signal profile should keep litany carrier tuning.");
    test::AssertEq(
        explicit_hostile_signal.samples_per_bit,
        static_cast<std::size_t>(1929),
        "Explicit hostile signal profile should keep the faster hostile timing when requested.");
    test::AssertEq(
        explicit_hostile_signal.low_freq_hz,
        450.0,
        "Explicit hostile signal profile should keep hostile carrier tuning.");
    test::AssertEq(
        explicit_collapse_signal.samples_per_bit,
        static_cast<std::size_t>(2205),
        "Explicit collapse signal profile should keep baseline timing when requested.");
    test::AssertEq(
        explicit_collapse_signal.low_freq_hz,
        280.0,
        "Explicit collapse signal profile should keep collapse carrier tuning.");
    test::AssertEq(
        explicit_zeal_signal.samples_per_bit,
        static_cast<std::size_t>(1102),
        "Explicit zeal signal profile should use the burst timing when requested.");
    test::AssertEq(
        explicit_zeal_signal.low_freq_hz,
        900.0,
        "Explicit zeal signal profile should expose zeal burst carrier tuning.");
}

void TestFlashSignalAmplitudeInRange() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("Hello"), config);
    test::AssertTrue(!pcm.empty(), "Flash signal module PCM should not be empty for non-empty input.");

    std::int16_t min_sample = pcm.front();
    std::int16_t max_sample = pcm.front();
    for (std::int16_t sample : pcm) {
        if (sample < min_sample) {
            min_sample = sample;
        }
        if (sample > max_sample) {
            max_sample = sample;
        }
    }

    test::AssertTrue(min_sample >= static_cast<std::int16_t>(-32767), "Flash signal module PCM min out of range.");
    test::AssertTrue(max_sample <= static_cast<std::int16_t>(32767), "Flash signal module PCM max out of range.");
}

void TestFlashSignalDecodeEmptyInputReturnsEmptyPayload() {
    const auto config = MakeBfskConfig();
    const std::vector<std::int16_t> pcm;
    const auto decoded_bytes = bag::flash::DecodePcm16ToBytes(pcm, config);
    test::AssertEq(
        decoded_bytes,
        std::vector<std::uint8_t>{},
        "Flash signal module should decode empty PCM to an empty byte payload.");
}

void TestFlashSignalSnapshotFirstSamplesStable() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("A"), config);
    const std::vector<std::int16_t> expected = {
        0, 1120, 2238, 3352, 4459, 5559, 6649, 7726,
        8789, 9837, 10866, 11875, 12863, 13827, 14766, 15678};

    test::AssertTrue(
        pcm.size() >= expected.size(),
        "Flash signal module PCM must contain enough samples for snapshot coverage.");
    for (std::size_t index = 0; index < expected.size(); ++index) {
        if (pcm[index] != expected[index]) {
            test::Fail("Flash signal module snapshot mismatch at sample index " + std::to_string(index));
        }
    }
}

void TestFlashPhyCleanTextRoundTrip() {
    const auto config = MakeFlashCoreConfig();
    const std::string text = test::Utf8Literal(u8"你好，FlipBits");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, text, &pcm),
        bag::ErrorCode::kOk,
        "Flash PHY facade should encode UTF-8 text through the extracted signal layer.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Flash PHY facade should decode PCM through the extracted signal layer.");
    test::AssertEq(decoded, text, "Flash PHY facade should preserve roundtrip text behavior.");
}

void TestFlashPhyCleanFormalOutputIncludesPredictableNonpayloadSegments() {
    const auto config = MakeFlashCoreConfig();
    const std::string text = test::Utf8Literal(u8"组合输出");

    std::vector<std::uint8_t> bytes;
    test::AssertEq(
        bag::flash::EncodeTextToBytes(text, &bytes),
        bag::ErrorCode::kOk,
        "Flash codec setup should encode UTF-8 bytes for formal flash output.");

    const auto signal_config = bag::flash::MakeBfskConfig(config);
    const auto clean_payload_pcm = bag::flash::EncodeBytesToPcm16(bytes, signal_config);

    std::vector<std::int16_t> formal_pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, text, &formal_pcm),
        bag::ErrorCode::kOk,
        "Flash PHY facade should encode text through the formal signal+voicing chain.");
    test::AssertEq(
        formal_pcm.size(),
        clean_payload_pcm.size() + FormalFlashLeadingSamples(config) + FormalFlashTrailingSamples(config),
        "Flash PHY facade should add predictable preamble and epilogue sample counts.");
    test::AssertTrue(
        formal_pcm != clean_payload_pcm,
        "Flash PHY facade should apply safe voicing so the formal output differs from the clean signal.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(config, formal_pcm, &decoded),
        bag::ErrorCode::kOk,
        "Flash PHY facade should decode formal output after trimming non-payload segments.");
    test::AssertEq(decoded, text, "Flash PHY facade formal output should preserve roundtrip text behavior.");
}

void TestFlashPhyCleanLitanyUsesLongerTimingAndStillDecodes() {
    const auto steady_config = MakeFlashCoreConfig();
    const auto litany_config = MakeLitanyFlashCoreConfig();
    const std::string text = "Litany";

    std::vector<std::int16_t> steady_pcm;
    std::vector<std::int16_t> litany_pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(steady_config, text, &steady_pcm),
        bag::ErrorCode::kOk,
        "steady flash encode should succeed for timing comparison.");
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(litany_config, text, &litany_pcm),
        bag::ErrorCode::kOk,
        "litany flash encode should succeed for timing comparison.");
    test::AssertTrue(
        litany_pcm.size() > steady_pcm.size(),
        "litany flash encode should be longer than steady after signal timing expansion.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(litany_config, litany_pcm, &decoded),
        bag::ErrorCode::kOk,
        "litany flash decode should succeed when the matching style is configured.");
    test::AssertEq(decoded, text, "litany flash decode should preserve the original text.");
}

void TestFlashPhyCleanWrongStyleDoesNotRoundTrip() {
    const auto steady_config = MakeFlashCoreConfig();
    const auto litany_config = MakeLitanyFlashCoreConfig();
    const std::string text = "Mismatch";

    std::vector<std::int16_t> litany_pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(litany_config, text, &litany_pcm),
        bag::ErrorCode::kOk,
        "litany flash encode should succeed before wrong-style decode validation.");

    std::string decoded;
    const auto decode_code = bag::flash::DecodePcm16ToText(steady_config, litany_pcm, &decoded);
    test::AssertTrue(
        decode_code != bag::ErrorCode::kOk || decoded != text,
        "Decoding litany flash PCM with steady style should not look like a valid roundtrip.");
}

void TestFlashPhyCleanExplicitSignalProfileKeepsPayloadTimingWhenVoicingChanges() {
    const auto config = MakeFlashCoreConfig();
    const auto signal_profile = bag::FlashSignalProfile::kSteady;
    const std::string text = "Decouple";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(config, signal_profile);
    const auto ritual_payload_layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            AsBytes(text),
            signal_config,
            bag::FlashVoicingFlavor::kLitany);
    const std::size_t expected_payload_sample_count =
        ritual_payload_layout.payload_sample_count;
    const std::size_t expected_ritual_shell =
        static_cast<std::size_t>(std::lround(static_cast<double>(config.sample_rate_hz) * 2.50));

    std::vector<std::int16_t> ritual_pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kLitany,
            &ritual_pcm),
        bag::ErrorCode::kOk,
        "Explicit signal profile encode should succeed when litany voicing is layered over steady timing.");
    test::AssertEq(
        ritual_pcm.size(),
        expected_payload_sample_count + expected_ritual_shell,
        "Explicit signal profile encode should use steady signal timing with litany payload pauses and shell.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            ritual_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kLitany,
            &decoded),
        bag::ErrorCode::kOk,
        "Explicit signal profile decode should succeed with the matching litany shell.");
    test::AssertEq(
        decoded,
        text,
        "Explicit signal profile decode should preserve the original text.");
}

void TestFlashPhyCleanSignalProfileAndFlavorApiMatchesConfiguredDefaultPath() {
    auto config = MakeFlashCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kSteady;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    std::vector<std::int16_t> flavor_pcm;
    std::vector<std::int16_t> default_pcm;

    test::AssertEq(
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "FlavorApi",
            bag::FlashSignalProfile::kSteady,
            bag::FlashVoicingFlavor::kLitany,
            &flavor_pcm),
        bag::ErrorCode::kOk,
        "Signal-profile-and-flavor encode should succeed.");
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(
            config,
            "FlavorApi",
            &default_pcm),
        bag::ErrorCode::kOk,
        "Configured default flash encode should succeed.");
    test::AssertEq(
        flavor_pcm,
        default_pcm,
        "Explicit signal-profile-and-flavor encode should match the configured default path.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            flavor_pcm,
            bag::FlashSignalProfile::kSteady,
            bag::FlashVoicingFlavor::kLitany,
            &decoded),
        bag::ErrorCode::kOk,
        "Signal-profile-and-flavor decode should succeed.");
    test::AssertEq(
        decoded,
        std::string("FlavorApi"),
        "Signal-profile-and-flavor decode should preserve the original text.");
}

void TestFlashPhyCleanDefaultPathUsesExplicitFlashComponentsWhenPresent() {
    const auto config = MakeExplicitDecoupledFlashCoreConfig();
    const std::string text = "DefaultPath";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(
            config,
            bag::FlashSignalProfile::kSteady);
    const auto ritual_payload_layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            AsBytes(text),
            signal_config,
            bag::FlashVoicingFlavor::kLitany);
    const std::size_t expected_payload_sample_count =
        ritual_payload_layout.payload_sample_count;
    const std::size_t expected_total_size =
        expected_payload_sample_count +
        static_cast<std::size_t>(std::lround(static_cast<double>(config.sample_rate_hz) * 2.50));

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, text, &pcm),
        bag::ErrorCode::kOk,
        "Default flash encode should succeed when explicit flash components are present.");
    test::AssertEq(
        pcm.size(),
        expected_total_size,
        "Default flash encode should use explicit steady signal timing with litany payload pauses and shell.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Default flash decode should succeed when explicit flash components are present.");
    test::AssertEq(
        decoded,
        text,
        "Default flash decode should preserve the original text when explicit flash components are present.");
}

void TestFlashVoicingNoOpPreservesPayload() {
    const auto signal_config = MakeBfskConfig();
    const auto payload_layout = bag::flash::BuildPayloadLayout(AsBytes("Hi"), signal_config);
    const auto clean_payload_pcm = bag::flash::EncodeBytesToPcm16(AsBytes("Hi"), signal_config);
    const auto voiced = bag::flash::ApplyVoicingToPayload(clean_payload_pcm, payload_layout);

    test::AssertEq(
        voiced.pcm,
        clean_payload_pcm,
        "Flash voicing module should preserve payload PCM in no-op mode.");
    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Flash voicing module should report zero leading non-payload samples in no-op mode.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Flash voicing module should report zero trailing non-payload samples in no-op mode.");
    test::AssertEq(
        voiced.descriptor.payload_sample_count,
        clean_payload_pcm.size(),
        "Flash voicing module should preserve payload sample count in no-op mode.");
}

void TestFlashVoicingStyledOutputKeepsPayloadShape() {
    const auto signal_config = MakeBfskConfig();
    const auto payload_layout = bag::flash::BuildPayloadLayout(AsBytes("Hi"), signal_config);
    const auto clean_payload_pcm = bag::flash::EncodeBytesToPcm16(AsBytes("Hi"), signal_config);
    const auto voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload_pcm, payload_layout, MakeStyledVoicingConfig());

    test::AssertEq(
        voiced.pcm.size(),
        clean_payload_pcm.size(),
        "Flash voicing styled output should keep the payload sample count unchanged.");
    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Flash voicing styled output should keep zero leading non-payload samples.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Flash voicing styled output should keep zero trailing non-payload samples.");
    test::AssertEq(
        voiced.descriptor.payload_sample_count,
        clean_payload_pcm.size(),
        "Flash voicing styled output should report the original payload sample count.");
    test::AssertTrue(
        voiced.pcm != clean_payload_pcm,
        "Flash voicing styled output should differ from the clean payload PCM.");

    const auto [min_it, max_it] = std::minmax_element(voiced.pcm.begin(), voiced.pcm.end());
    test::AssertTrue(min_it != voiced.pcm.end(), "Flash voicing styled output should not be empty.");
    test::AssertTrue(
        *min_it >= static_cast<std::int16_t>(-32767),
        "Flash voicing styled output min sample should remain in PCM16 range.");
    test::AssertTrue(
        *max_it <= static_cast<std::int16_t>(32767),
        "Flash voicing styled output max sample should remain in PCM16 range.");
}

}  // namespace

namespace modules_leaf_smoke {

void RegisterLeafFlashTests(test::Runner& runner) {
    runner.Add("ModulesLeaf.FlashCodecModule", TestFlashCodecModule);
    runner.Add("ModulesLeaf.FlashSignalLayoutMatchesExpected", TestFlashSignalLayoutMatchesExpected);
    runner.Add("ModulesLeaf.FlashSignalEncodeLengthMatchesExpected", TestFlashSignalEncodeLengthMatchesExpected);
    runner.Add("ModulesLeaf.FlashSignalStyleAwareChunkSizeMatchesConfig",
               TestFlashSignalStyleAwareChunkSizeMatchesConfig);
    runner.Add("ModulesLeaf.FlashSignalExplicitProfileSelectsEmotionTiming",
               TestFlashSignalExplicitProfileSelectsEmotionTiming);
    runner.Add("ModulesLeaf.FlashSignalAmplitudeInRange", TestFlashSignalAmplitudeInRange);
    runner.Add("ModulesLeaf.FlashSignalDecodeEmptyInputReturnsEmptyPayload",
               TestFlashSignalDecodeEmptyInputReturnsEmptyPayload);
    runner.Add("ModulesLeaf.FlashSignalSnapshotFirstSamplesStable",
               TestFlashSignalSnapshotFirstSamplesStable);
    runner.Add("ModulesLeaf.FlashPhyCleanTextRoundTrip", TestFlashPhyCleanTextRoundTrip);
    runner.Add("ModulesLeaf.FlashPhyCleanFormalOutputIncludesPredictableNonpayloadSegments",
               TestFlashPhyCleanFormalOutputIncludesPredictableNonpayloadSegments);
    runner.Add("ModulesLeaf.FlashPhyCleanLitanyUsesLongerTimingAndStillDecodes",
               TestFlashPhyCleanLitanyUsesLongerTimingAndStillDecodes);
    runner.Add("ModulesLeaf.FlashPhyCleanWrongStyleDoesNotRoundTrip",
               TestFlashPhyCleanWrongStyleDoesNotRoundTrip);
    runner.Add("ModulesLeaf.FlashPhyCleanExplicitSignalProfileKeepsPayloadTimingWhenVoicingChanges",
               TestFlashPhyCleanExplicitSignalProfileKeepsPayloadTimingWhenVoicingChanges);
    runner.Add("ModulesLeaf.FlashPhyCleanSignalProfileAndFlavorApiMatchesConfiguredDefaultPath",
               TestFlashPhyCleanSignalProfileAndFlavorApiMatchesConfiguredDefaultPath);
    runner.Add("ModulesLeaf.FlashPhyCleanDefaultPathUsesExplicitFlashComponentsWhenPresent",
               TestFlashPhyCleanDefaultPathUsesExplicitFlashComponentsWhenPresent);
    runner.Add("ModulesLeaf.FlashVoicingNoOpPreservesPayload", TestFlashVoicingNoOpPreservesPayload);
    runner.Add("ModulesLeaf.FlashVoicingStyledOutputKeepsPayloadShape",
               TestFlashVoicingStyledOutputKeepsPayloadShape);
}

}  // namespace modules_leaf_smoke
