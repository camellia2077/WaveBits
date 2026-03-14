#pragma once

#include "bag/common/version_generated.h"

#if !defined(WAVEBITS_TEST_IMPORT_STD)
#include <string>
#include <vector>
#endif

#include "test_utf8.h"

namespace test {

struct ConfigCase {
    std::string name;
    int sample_rate_hz;
    int frame_samples;
};

struct CorpusCase {
    std::string name;
    std::string text;
};

inline std::string BuildLongCorpus() {
    std::string text;
    const std::string pattern = "WaveBits-Long-Corpus-0123456789|";
    while (text.size() < 128) {
        text += pattern;
    }
    text.resize(128);
    return text;
}

inline const std::vector<ConfigCase>& ConfigCases() {
    static const std::vector<ConfigCase> cases = {
        {"44k1", 44100, 2205},
        {"48k", 48000, 2400},
    };
    return cases;
}

inline const std::vector<CorpusCase>& FlashCorpusCases() {
    static const std::vector<CorpusCase> cases = {
        {"single_char", "A"},
        {"ascii", "Hello-123"},
        {"punctuation", "WaveBits: encode & decode!"},
        {"utf8", test::Utf8Literal(u8"你好，WaveBits")},
        {"long_ascii", BuildLongCorpus()},
    };
    return cases;
}

inline std::string BuildMaxProCorpus() {
    return std::string(170, 'A');
}

inline const std::vector<CorpusCase>& ProCorpusCases() {
    static const std::vector<CorpusCase> cases = {
        {"single_char", "A"},
        {"ascii", "Hello-123"},
        {"punctuation", "WaveBits: encode & decode!"},
        {"long_ascii", BuildLongCorpus()},
        {"max_single_frame_ascii", BuildMaxProCorpus()},
    };
    return cases;
}

inline std::string BuildMaxUltraCorpus() {
    std::string text;
    for (int index = 0; index < 170; ++index) {
        text += test::Utf8Literal(u8"你");
    }
    text += "AB";
    return text;
}

inline const std::vector<CorpusCase>& UltraCorpusCases() {
    static const std::vector<CorpusCase> cases = {
        {"ascii", "Hello-123"},
        {"punctuation", "WaveBits: encode & decode!"},
        {"utf8", test::Utf8Literal(u8"你好，WaveBits")},
        {"emoji", test::Utf8Literal(u8"WaveBits 🚀")},
        {"mixed_utf8", test::Utf8Literal(u8"WaveBits 超级模式 🚀")},
        {"max_single_frame_utf8", BuildMaxUltraCorpus()},
    };
    return cases;
}

inline const std::vector<CorpusCase>& CorpusCases() {
    return FlashCorpusCases();
}

inline std::string BuildTooLongProCorpus() {
    return std::string(171, 'A');
}

inline std::string BuildTooLongUltraCorpus() {
    std::string text;
    for (int index = 0; index < 171; ++index) {
        text += test::Utf8Literal(u8"你");
    }
    return text;
}

inline constexpr char kExpectedCoreVersion[] = WAVEBITS_CORE_VERSION_TEXT;

}  // namespace test
