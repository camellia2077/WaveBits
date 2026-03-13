#include "test_std_support.h"
#include "test_audio_io.h"
#include "test_framework.h"
#include "test_fs.h"
#include "wav_io.h"

namespace {

void TestWavIoHeaderRoundTripContract() {
    const auto dir = test::MakeTempDir("unit");
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto path = dir / (std::string(test_case.name) + ".wav");
        audio_io::WriteMonoPcm16Wav(path, test_case.sample_rate_hz, test_case.mono_pcm);
        const auto read_back = audio_io::ReadMonoPcm16Wav(path);
        test::AssertAudioIoRoundTripResult(read_back, test_case, "Header audio_io boundary");
    }
}

void TestWavIoHeaderReadMissingFileFails() {
    const auto missing_path = test::MakeTempDir("unit") / "missing.wav";
    test::AssertThrows(
        [&] {
            (void)audio_io::ReadMonoPcm16Wav(missing_path);
        },
        "Header audio_io boundary should throw when the input file does not exist.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Unit.WavIoHeaderRoundTripContract", TestWavIoHeaderRoundTripContract);
    runner.Add("Unit.WavIoHeaderReadMissingFileFails", TestWavIoHeaderReadMissingFileFails);
    return runner.Run();
}

