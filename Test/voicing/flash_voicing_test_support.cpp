#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

#include "flash_voicing_test_support.h"

namespace flash_voicing_test {

bag::flash::BfskConfig MakeSignalConfig() {
    bag::flash::BfskConfig config{};
    config.sample_rate_hz = 44100;
    config.samples_per_bit = 2205;
    config.bit_duration_sec = 0.05;
    return config;
}

bag::CoreConfig MakeCoreConfig() {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 480;
    config.mode = bag::TransportMode::kFlash;
    return config;
}

bag::CoreConfig MakeAndroidSizedCoreConfig() {
    auto config = MakeCoreConfig();
    config.frame_samples = 2205;
    return config;
}

std::size_t FormalPreambleSampleCountForFlavor(const bag::CoreConfig& config,
                                               bag::FlashVoicingFlavor flavor) {
    switch (flavor) {
    case bag::FlashVoicingFlavor::kRitualChant:
        return config.frame_samples > 0
                   ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(16)
                   : static_cast<std::size_t>(0);
    case bag::FlashVoicingFlavor::kCodedBurst:
    default:
        return config.frame_samples > 0
                   ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
                   : static_cast<std::size_t>(0);
    }
}

std::size_t FormalEpilogueSampleCountForFlavor(const bag::CoreConfig& config,
                                               bag::FlashVoicingFlavor flavor) {
    switch (flavor) {
    case bag::FlashVoicingFlavor::kRitualChant:
        return config.frame_samples > 0
                   ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(8)
                   : static_cast<std::size_t>(0);
    case bag::FlashVoicingFlavor::kCodedBurst:
    default:
        return config.frame_samples > 0
                   ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
                   : static_cast<std::size_t>(0);
    }
}

bag::flash::FlashVoicingConfig MakeEnvelopeOnlyConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.attack_ratio = 0.10;
    config.release_ratio = 0.10;
    return config;
}

bag::flash::FlashVoicingConfig MakeHarmonicOnlyConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.second_harmonic_gain = 0.12;
    config.third_harmonic_gain = 0.08;
    return config;
}

bag::flash::FlashVoicingConfig MakeClickOnlyConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.boundary_click_gain = 0.02;
    return config;
}

bag::flash::FlashVoicingConfig MakeStyledConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.attack_ratio = 0.08;
    config.release_ratio = 0.08;
    config.second_harmonic_gain = 0.10;
    config.third_harmonic_gain = 0.03;
    config.boundary_click_gain = 0.02;
    return config;
}

bag::flash::FlashVoicingConfig MakeStyledShellConfig() {
    auto config = MakeStyledConfig();
    config.enable_preamble = true;
    config.enable_epilogue = true;
    config.preamble_sample_count = static_cast<std::size_t>(480);
    config.epilogue_sample_count = static_cast<std::size_t>(240);
    return config;
}

bag::flash::FlashVoicingConfig MakeTrimEnabledConfig(std::size_t preamble_sample_count,
                                                     std::size_t epilogue_sample_count) {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.enable_preamble = preamble_sample_count > 0;
    config.enable_epilogue = epilogue_sample_count > 0;
    config.preamble_sample_count = preamble_sample_count;
    config.epilogue_sample_count = epilogue_sample_count;
    return config;
}

std::vector<std::uint8_t> AsBytes(const std::string& text) {
    return std::vector<std::uint8_t>(text.begin(), text.end());
}

bag::flash::FlashPayloadLayout MakePayloadLayout(const std::string& text) {
    return bag::flash::BuildPayloadLayout(AsBytes(text), MakeSignalConfig());
}

std::vector<std::int16_t> MakeCleanPayload(const std::string& text) {
    return bag::flash::EncodeBytesToPcm16(AsBytes(text), MakeSignalConfig());
}

void AssertPcm16Range(const std::vector<std::int16_t>& pcm, const std::string& context) {
    test::AssertTrue(!pcm.empty(), context + " should not be empty.");
    const auto [min_it, max_it] = std::minmax_element(pcm.begin(), pcm.end());
    test::AssertTrue(
        *min_it >= static_cast<std::int16_t>(-32767),
        context + " min sample should remain in PCM16 range.");
    test::AssertTrue(
        *max_it <= static_cast<std::int16_t>(32767),
        context + " max sample should remain in PCM16 range.");
}

double AverageAbsoluteSample(const std::vector<std::int16_t>& pcm,
                             std::size_t begin,
                             std::size_t end) {
    test::AssertTrue(begin < end, "AverageAbsoluteSample requires a non-empty range.");
    test::AssertTrue(end <= pcm.size(), "AverageAbsoluteSample range must stay within PCM.");

    double sum = 0.0;
    for (std::size_t index = begin; index < end; ++index) {
        sum += std::abs(static_cast<double>(pcm[index]));
    }
    return sum / static_cast<double>(end - begin);
}

double AverageAbsoluteDelta(const std::vector<std::int16_t>& first,
                            const std::vector<std::int16_t>& second,
                            std::size_t begin,
                            std::size_t end) {
    test::AssertEq(first.size(), second.size(), "AverageAbsoluteDelta inputs must have the same size.");
    test::AssertTrue(begin < end, "AverageAbsoluteDelta requires a non-empty range.");
    test::AssertTrue(end <= first.size(), "AverageAbsoluteDelta range must stay within PCM.");

    double sum = 0.0;
    for (std::size_t index = begin; index < end; ++index) {
        sum += std::abs(static_cast<double>(first[index]) - static_cast<double>(second[index]));
    }
    return sum / static_cast<double>(end - begin);
}

double AverageAbsoluteRangeDelta(const std::vector<std::int16_t>& pcm,
                                 std::size_t first_begin,
                                 std::size_t first_end,
                                 std::size_t second_begin,
                                 std::size_t second_end) {
    test::AssertTrue(first_begin < first_end, "AverageAbsoluteRangeDelta requires a non-empty first range.");
    test::AssertTrue(second_begin < second_end, "AverageAbsoluteRangeDelta requires a non-empty second range.");
    test::AssertTrue(first_end <= pcm.size(), "AverageAbsoluteRangeDelta first range must stay within PCM.");
    test::AssertTrue(second_end <= pcm.size(), "AverageAbsoluteRangeDelta second range must stay within PCM.");
    test::AssertEq(
        first_end - first_begin,
        second_end - second_begin,
        "AverageAbsoluteRangeDelta ranges must have the same length.");

    double sum = 0.0;
    const std::size_t length = first_end - first_begin;
    for (std::size_t index = 0; index < length; ++index) {
        sum += std::abs(
            static_cast<double>(pcm[first_begin + index]) -
            static_cast<double>(pcm[second_begin + index]));
    }
    return sum / static_cast<double>(length);
}

double AverageNormalizedFirstDifference(const std::vector<std::int16_t>& pcm,
                                        std::size_t begin,
                                        std::size_t end) {
    test::AssertTrue(begin < end, "AverageNormalizedFirstDifference requires a non-empty range.");
    test::AssertTrue(end <= pcm.size(), "AverageNormalizedFirstDifference range must stay within PCM.");
    if (end - begin < static_cast<std::size_t>(2)) {
        return 0.0;
    }

    double diff_sum = 0.0;
    double amplitude_sum = 0.0;
    for (std::size_t index = begin + static_cast<std::size_t>(1); index < end; ++index) {
        diff_sum += std::abs(
            static_cast<double>(pcm[index]) -
            static_cast<double>(pcm[index - static_cast<std::size_t>(1)]));
        amplitude_sum += std::abs(static_cast<double>(pcm[index]));
    }
    return diff_sum / std::max(amplitude_sum, 1.0);
}

std::pair<std::size_t, std::size_t> FractionalRange(std::size_t sample_count,
                                                    double begin_ratio,
                                                    double end_ratio) {
    const std::size_t begin = static_cast<std::size_t>(
        std::floor(static_cast<double>(sample_count) * begin_ratio));
    const std::size_t end = static_cast<std::size_t>(
        std::ceil(static_cast<double>(sample_count) * end_ratio));
    return {
        std::min(begin, sample_count),
        std::clamp(end, begin + static_cast<std::size_t>(1), sample_count)
    };
}

}  // namespace flash_voicing_test
