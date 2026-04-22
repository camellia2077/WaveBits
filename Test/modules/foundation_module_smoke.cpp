#include "test_std_support.h"
#include "test_framework.h"

import audio_io.wav;
import bag.common.config;
import bag.common.error_code;
import bag.common.types;

namespace {

void TestConfigModuleDefaults() {
    bag::CoreConfig config{};
    test::AssertEq(config.sample_rate_hz, 48000, "Module import should expose CoreConfig defaults.");
    test::AssertEq(config.frame_samples, 480, "Module import should preserve frame size defaults.");
    test::AssertEq(config.mode, bag::TransportMode::kFlash, "Module import should preserve mode defaults.");
    test::AssertEq(
        config.flash_signal_profile,
        bag::FlashSignalProfile::kCodedBurst,
        "Module import should default flash signal profile to coded_burst.");
    test::AssertEq(
        config.flash_voicing_flavor,
        bag::FlashVoicingFlavor::kCodedBurst,
        "Module import should default flash voicing flavor to coded_burst.");
    test::AssertTrue(bag::IsValidTransportMode(bag::TransportMode::kUltra), "Imported helper should validate known modes.");
    test::AssertTrue(
        bag::IsValidFlashSignalProfile(bag::FlashSignalProfile::kCodedBurst),
        "Imported helper should validate the coded burst signal profile.");
    test::AssertTrue(
        bag::IsValidFlashVoicingFlavor(bag::FlashVoicingFlavor::kRitualChant),
        "Imported helper should validate the ritual chant voicing flavor.");
    test::AssertTrue(
        bag::IsValidFlashVoicingFlavor(bag::FlashVoicingFlavor::kDeepRitual),
        "Imported helper should validate the deep ritual voicing flavor.");
}

void TestFlashSignalProfilesRemainDistinct() {
    test::AssertTrue(
        bag::IsValidFlashSignalProfile(bag::FlashSignalProfile::kCodedBurst),
        "coded_burst signal profile should remain valid.");
    test::AssertTrue(
        bag::IsValidFlashSignalProfile(bag::FlashSignalProfile::kRitualChant),
        "ritual_chant signal profile should remain valid.");
    test::AssertTrue(
        bag::IsValidFlashSignalProfile(bag::FlashSignalProfile::kDeepRitual),
        "deep_ritual signal profile should remain valid.");
    test::AssertTrue(
        static_cast<int>(bag::FlashSignalProfile::kCodedBurst) !=
            static_cast<int>(bag::FlashSignalProfile::kRitualChant),
        "flash signal profiles should remain distinct enum values.");
    test::AssertTrue(
        static_cast<int>(bag::FlashSignalProfile::kRitualChant) !=
            static_cast<int>(bag::FlashSignalProfile::kDeepRitual),
        "deep_ritual signal profile should remain a distinct enum value.");
}

void TestFlashVoicingFlavorsRemainDistinct() {
    bag::CoreConfig config{};
    config.flash_signal_profile = bag::FlashSignalProfile::kRitualChant;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;

    test::AssertTrue(
        bag::IsValidFlashVoicingFlavor(config.flash_voicing_flavor),
        "Configured flash voicing flavor should remain valid.");
    test::AssertEq(
        config.flash_signal_profile,
        bag::FlashSignalProfile::kRitualChant,
        "CoreConfig should store explicit ritual signal timing directly.");
    test::AssertEq(
        config.flash_voicing_flavor,
        bag::FlashVoicingFlavor::kRitualChant,
        "CoreConfig should store explicit ritual voicing flavor directly.");
    config.flash_signal_profile = bag::FlashSignalProfile::kDeepRitual;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kDeepRitual;
    test::AssertEq(
        config.flash_signal_profile,
        bag::FlashSignalProfile::kDeepRitual,
        "CoreConfig should store explicit deep ritual signal timing directly.");
    test::AssertEq(
        config.flash_voicing_flavor,
        bag::FlashVoicingFlavor::kDeepRitual,
        "CoreConfig should store explicit deep ritual voicing flavor directly.");
}

void TestTypesAndAudioIoModules() {
    bag::TextResult result{};
    result.text = "foundation";
    result.complete = true;
    result.confidence = 1.0f;
    result.mode = bag::TransportMode::kPro;

    audio_io::WavPcm16 wav{};
    wav.sample_rate_hz = 44100;
    wav.mono_pcm = std::vector<std::int16_t>{1, 2, 3, 4};

    test::AssertEq(result.text, std::string("foundation"), "Imported TextResult should be usable.");
    test::AssertTrue(result.complete, "Imported TextResult should expose completion state.");
    test::AssertEq(result.mode, bag::TransportMode::kPro, "Imported TextResult should expose mode.");
    test::AssertEq(wav.sample_rate_hz, 44100, "Imported audio_io module should expose WavPcm16.");
    test::AssertEq(
        wav.mono_pcm.size(),
        static_cast<std::size_t>(4),
        "Imported audio_io module should expose vector payload.");
    test::AssertEq(bag::ErrorCode::kOk, bag::ErrorCode::kOk, "Imported ErrorCode should remain available.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("ModulesFoundation.ConfigModuleDefaults", TestConfigModuleDefaults);
    runner.Add("ModulesFoundation.FlashSignalProfilesRemainDistinct",
               TestFlashSignalProfilesRemainDistinct);
    runner.Add("ModulesFoundation.FlashVoicingFlavorsRemainDistinct",
               TestFlashVoicingFlavorsRemainDistinct);
    runner.Add("ModulesFoundation.TypesAndAudioIoModules", TestTypesAndAudioIoModules);
    return runner.Run();
}
