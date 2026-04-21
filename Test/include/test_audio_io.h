#pragma once

#if !defined(FLIPBITS_TEST_IMPORT_STD)
#include <cstdint>
#include <string>
#include <vector>
#endif

#include "test_framework.h"

namespace test {

struct AudioIoRoundTripCase {
    const char* name;
    int sample_rate_hz;
    std::vector<std::int16_t> mono_pcm;
};

inline const std::vector<AudioIoRoundTripCase>& AudioIoRoundTripCases() {
    static const std::vector<AudioIoRoundTripCase> cases = {
        {"single_sample", 8000, {0}},
        {"short_alternating", 44100, {0, 1200, -1200, 3276, -3276}},
        {"wide_dynamic_range", 48000, {-32767, -16384, -1, 0, 1, 16384, 32767}},
    };
    return cases;
}

template <typename WavLike>
inline void AssertAudioIoRoundTripResult(const WavLike& wav,
                                         const AudioIoRoundTripCase& test_case,
                                         const std::string& route_name) {
    AssertEq(
        wav.sample_rate_hz,
        test_case.sample_rate_hz,
        route_name + " should preserve the sample rate.");
    AssertEq(
        wav.channels,
        1,
        route_name + " should preserve mono channel count.");
    AssertEq(
        wav.mono_pcm,
        test_case.mono_pcm,
        route_name + " should preserve mono PCM content.");
}

}  // namespace test
