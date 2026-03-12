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
    test::AssertTrue(bag::IsValidTransportMode(bag::TransportMode::kUltra), "Imported helper should validate known modes.");
}

void TestTypesAndAudioIoModules() {
    bag::TextResult result{};
    result.text = "phase0";
    result.complete = true;
    result.confidence = 1.0f;
    result.mode = bag::TransportMode::kPro;

    audio_io::WavPcm16 wav{};
    wav.sample_rate_hz = 44100;
    wav.mono_pcm = std::vector<std::int16_t>{1, 2, 3, 4};

    test::AssertEq(result.text, std::string("phase0"), "Imported TextResult should be usable.");
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
    runner.Add("ModulesPhase0.ConfigModuleDefaults", TestConfigModuleDefaults);
    runner.Add("ModulesPhase0.TypesAndAudioIoModules", TestTypesAndAudioIoModules);
    return runner.Run();
}
