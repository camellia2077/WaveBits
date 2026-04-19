#include "test_std_support.h"
#include "test_audio_io.h"
#include "test_fs.h"
#include "test_framework.h"
#include "test_vectors.h"

import audio_io.wav;
import bag.common.version;
import bag.flash.signal;
import bag.flash.voicing;

#include "leaf_module_smoke_support.h"

namespace {

void TestVersionModule() {
    test::AssertEq(
        std::string(bag::CoreVersion()),
        std::string(test::kExpectedCoreVersion),
        "Version module should expose core version.");
}

void TestAudioIoModuleRoundTripContract() {
    const auto dir = test::MakeTempDir("modules_leaf");
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto path = dir / (std::string(test_case.name) + ".wav");
        audio_io::WriteMonoPcm16Wav(path, test_case.sample_rate_hz, test_case.mono_pcm);
        const auto wav = audio_io::ReadMonoPcm16Wav(path);
        test::AssertAudioIoRoundTripResult(wav, test_case, "Module audio_io boundary");
    }
}

void TestAudioIoModuleBytesRoundTripContract() {
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
        const auto parsed = audio_io::ParseMonoPcm16Wav(wav_bytes);
        test::AssertEq(
            parsed.status,
            audio_io::WavPcm16Status::kOk,
            "Module bytes route should parse canonical mono PCM16 WAV bytes.");
        test::AssertAudioIoRoundTripResult(parsed.wav, test_case, "Module audio_io bytes boundary");
    }
}

void TestAudioIoModuleReadMissingFileFails() {
    const auto missing_path = test::MakeTempDir("modules_leaf") / "missing.wav";
    test::AssertThrows(
        [&] {
            (void)audio_io::ReadMonoPcm16Wav(missing_path);
        },
        "Module audio_io boundary should throw when the input file does not exist.");
}

void TestAudioIoModuleRejectsInvalidBytes() {
    const std::vector<std::uint8_t> bad_header = {'N', 'O', 'T', 'W', 'A', 'V', 'E'};
    const auto parsed = audio_io::ParseMonoPcm16Wav(bad_header);
    test::AssertEq(
        parsed.status,
        audio_io::WavPcm16Status::kInvalidHeader,
        "Module bytes route should reject invalid RIFF/WAVE bytes.");
}

void TestAudioIoModuleRejectsUnsupportedStereoBytes() {
    const auto test_case = test::AudioIoRoundTripCases().front();
    auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
    wav_bytes[22] = 0x02;
    wav_bytes[23] = 0x00;
    wav_bytes[32] = 0x04;
    wav_bytes[33] = 0x00;
    const auto stereo_byte_rate = static_cast<std::uint32_t>(test_case.sample_rate_hz * 4);
    wav_bytes[28] = static_cast<std::uint8_t>(stereo_byte_rate & 0xFFu);
    wav_bytes[29] = static_cast<std::uint8_t>((stereo_byte_rate >> 8) & 0xFFu);
    wav_bytes[30] = static_cast<std::uint8_t>((stereo_byte_rate >> 16) & 0xFFu);
    wav_bytes[31] = static_cast<std::uint8_t>((stereo_byte_rate >> 24) & 0xFFu);
    const auto parsed = audio_io::ParseMonoPcm16Wav(wav_bytes);
    test::AssertEq(
        parsed.status,
        audio_io::WavPcm16Status::kUnsupportedFormat,
        "Module bytes route should reject stereo WAV input.");
}

void TestAudioIoModuleRejectsTruncatedDataBytes() {
    const auto test_case = test::AudioIoRoundTripCases().front();
    auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
    wav_bytes.pop_back();
    const auto parsed = audio_io::ParseMonoPcm16Wav(wav_bytes);
    test::AssertEq(
        parsed.status,
        audio_io::WavPcm16Status::kTruncatedData,
        "Module bytes route should reject truncated data chunks.");
}

}  // namespace

namespace modules_leaf_smoke {

void RegisterLeafAudioIoTests(test::Runner& runner) {
    runner.Add("ModulesLeaf.VersionModule", TestVersionModule);
    runner.Add("ModulesLeaf.AudioIoModuleRoundTripContract", TestAudioIoModuleRoundTripContract);
    runner.Add("ModulesLeaf.AudioIoModuleBytesRoundTripContract", TestAudioIoModuleBytesRoundTripContract);
    runner.Add("ModulesLeaf.AudioIoModuleReadMissingFileFails", TestAudioIoModuleReadMissingFileFails);
    runner.Add("ModulesLeaf.AudioIoModuleRejectsInvalidBytes", TestAudioIoModuleRejectsInvalidBytes);
    runner.Add("ModulesLeaf.AudioIoModuleRejectsUnsupportedStereoBytes", TestAudioIoModuleRejectsUnsupportedStereoBytes);
    runner.Add("ModulesLeaf.AudioIoModuleRejectsTruncatedDataBytes", TestAudioIoModuleRejectsTruncatedDataBytes);
}

}  // namespace modules_leaf_smoke
