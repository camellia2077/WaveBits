#include <algorithm>
#include <cmath>
#include <string>
#include <vector>

#include "bag_api.h"
#include "test_framework.h"
#include "test_fs.h"
#include "test_utf8.h"
#include "test_vectors.h"
#include "wav_io.h"

namespace {

struct DecodeOutcome {
    std::string text;
    bag_transport_mode mode = BAG_TRANSPORT_FLASH;
};

bag_encoder_config MakeEncoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH,
                                     bag_flash_signal_profile flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_STEADY,
                                     bag_flash_voicing_flavor flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_STEADY) {
    bag_encoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.flash_signal_profile = flash_signal_profile;
    config.flash_voicing_flavor = flash_voicing_flavor;
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH,
                                     bag_flash_signal_profile flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_STEADY,
                                     bag_flash_voicing_flavor flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_STEADY) {
    bag_decoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.flash_signal_profile = flash_signal_profile;
    config.flash_voicing_flavor = flash_voicing_flavor;
    config.reserved = 0;
    return config;
}

std::vector<int16_t> EncodeToVector(const bag_encoder_config& config, const std::string& text) {
    bag_pcm16_result pcm{};
    const auto encode_code = bag_encode_text(&config, text.c_str(), &pcm);
    test::AssertEq(encode_code, BAG_OK, "Artifact encode should succeed.");

    std::vector<int16_t> out;
    if (pcm.sample_count > 0) {
        out.assign(pcm.samples, pcm.samples + pcm.sample_count);
    }
    bag_free_pcm16_result(&pcm);
    return out;
}

DecodeOutcome DecodeFromVector(const bag_decoder_config& config, const std::vector<int16_t>& pcm) {
    bag_decoder* decoder = nullptr;
    const auto create_code = bag_create_decoder(&config, &decoder);
    test::AssertEq(create_code, BAG_OK, "Artifact decoder creation should succeed.");
    test::AssertTrue(decoder != nullptr, "Artifact decoder should not be null.");

    const auto push_code = bag_push_pcm(decoder, pcm.data(), pcm.size(), 0);
    test::AssertEq(push_code, BAG_OK, "Artifact push should succeed.");

    std::vector<char> text_buffer(4096, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    const auto poll_code = bag_poll_result(decoder, &result);
    test::AssertEq(poll_code, BAG_OK, "Artifact poll should succeed after push.");
    bag_destroy_decoder(decoder);

    DecodeOutcome out{};
    out.text.assign(text_buffer.data(), result.text_size);
    out.mode = result.mode;
    return out;
}

bool IsUtf8ContinuationByte(unsigned char value) {
    return (value & 0xC0U) == 0x80U;
}

bool IsLikelyUtf8CodePointBoundaryAfter(const std::string& text, size_t byte_index) {
    return byte_index + static_cast<size_t>(1) >= text.size() ||
           !IsUtf8ContinuationByte(static_cast<unsigned char>(text[byte_index + static_cast<size_t>(1)]));
}

bool EndsWithUtf8Bytes(const std::string& text,
                       size_t byte_index,
                       unsigned char first,
                       unsigned char second,
                       unsigned char third) {
    if (byte_index < static_cast<size_t>(2)) {
        return false;
    }
    return static_cast<unsigned char>(text[byte_index - static_cast<size_t>(2)]) == first &&
           static_cast<unsigned char>(text[byte_index - static_cast<size_t>(1)]) == second &&
           static_cast<unsigned char>(text[byte_index]) == third;
}

size_t LitanyPauseSlotCountAfterByte(const std::string& text, size_t byte_index) {
    const unsigned char value = static_cast<unsigned char>(text[byte_index]);
    switch (value) {
    case static_cast<unsigned char>(' '):
    case static_cast<unsigned char>('\t'):
        return 3;
    case static_cast<unsigned char>('\n'):
    case static_cast<unsigned char>('\r'):
        return 6;
    case static_cast<unsigned char>(','):
    case static_cast<unsigned char>(';'):
    case static_cast<unsigned char>(':'):
        return 4;
    case static_cast<unsigned char>('.'):
    case static_cast<unsigned char>('!'):
    case static_cast<unsigned char>('?'):
        return 8;
    default:
        break;
    }

    if (EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x8C) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x9B) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x9A)) {
        return 4;
    }
    if (EndsWithUtf8Bytes(text, byte_index, 0xE3, 0x80, 0x82) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x81) ||
        EndsWithUtf8Bytes(text, byte_index, 0xEF, 0xBC, 0x9F)) {
        return 8;
    }
    if (byte_index + static_cast<size_t>(1) >= text.size() ||
        !IsLikelyUtf8CodePointBoundaryAfter(text, byte_index)) {
        return 1;
    }

    const size_t cadence_position = byte_index + static_cast<size_t>(1);
    if ((cadence_position % static_cast<size_t>(12)) == 0) {
        return 5;
    }
    return 1;
}

size_t LitanyPauseSlotCount(const std::string& text) {
    size_t pause_slots = 0;
    for (size_t byte_index = 0; byte_index < text.size(); ++byte_index) {
        pause_slots += static_cast<size_t>(14) + LitanyPauseSlotCountAfterByte(text, byte_index);
    }
    return pause_slots;
}

size_t SecondsToSampleCount(int sample_rate_hz, double seconds) {
    return sample_rate_hz > 0 && seconds > 0.0
               ? static_cast<size_t>(std::lround(static_cast<double>(sample_rate_hz) * seconds))
               : static_cast<size_t>(0);
}

size_t ExpectedPcmSampleCount(const std::string& text,
                              bag_transport_mode mode,
                              const test::ConfigCase& config_case,
                              bag_flash_signal_profile flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_STEADY,
                              bag_flash_voicing_flavor flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_STEADY) {
    if (mode == BAG_TRANSPORT_FLASH) {
        const auto frame_samples =
            config_case.frame_samples > 0 ? static_cast<size_t>(config_case.frame_samples) : static_cast<size_t>(0);
        size_t payload_samples_per_bit = frame_samples;
        if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_STEADY) {
            payload_samples_per_bit =
                std::max(static_cast<size_t>(1),
                         frame_samples * static_cast<size_t>(15) / static_cast<size_t>(16));
        } else if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_LITANY) {
            payload_samples_per_bit = frame_samples * static_cast<size_t>(6);
        } else if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_HOSTILE) {
            payload_samples_per_bit =
                std::max(static_cast<size_t>(1),
                         frame_samples * static_cast<size_t>(7) / static_cast<size_t>(8));
        } else if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_ZEAL) {
            payload_samples_per_bit =
                std::max(static_cast<size_t>(1), frame_samples / static_cast<size_t>(2));
        } else if (flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_VOID) {
            payload_samples_per_bit =
                std::max(static_cast<size_t>(1),
                         frame_samples * static_cast<size_t>(5) / static_cast<size_t>(2));
        }
        const bool uses_litany_pauses = flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_LITANY;
        const auto payload_samples =
            text.size() * 8 * payload_samples_per_bit +
            (uses_litany_pauses ? LitanyPauseSlotCount(text) * frame_samples : static_cast<size_t>(0));
        const auto leading_nonpayload_samples =
            flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_LITANY
                ? SecondsToSampleCount(config_case.sample_rate_hz, 1.35)
                : frame_samples * static_cast<size_t>(3);
        const auto trailing_nonpayload_samples =
            flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_LITANY
                ? SecondsToSampleCount(config_case.sample_rate_hz, 1.15)
                : frame_samples * static_cast<size_t>(3);
        return payload_samples + leading_nonpayload_samples + trailing_nonpayload_samples;
    }

    // At the boundary layer, the current product contract is byte-oriented:
    // flash uses 8 BFSK bits per byte, while pro/ultra use 2 symbols per byte.
    if (mode == BAG_TRANSPORT_PRO || mode == BAG_TRANSPORT_ULTRA) {
        return text.size() * 2 * static_cast<size_t>(config_case.frame_samples);
    }

    test::Fail("Artifact expected-length helper received an unsupported transport mode.");
    return 0;
}

void AssertPcmProperties(const std::vector<int16_t>& pcm,
                         const std::string& text,
                         bag_transport_mode mode,
                         const test::ConfigCase& config_case,
                         bag_flash_signal_profile flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_STEADY,
                         bag_flash_voicing_flavor flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_STEADY) {
    const auto expected_length =
        ExpectedPcmSampleCount(text, mode, config_case, flash_signal_profile, flash_voicing_flavor);
    test::AssertEq(pcm.size(), expected_length, "PCM length should match the selected mode's symbol layout.");

    const auto [min_it, max_it] = std::minmax_element(pcm.begin(), pcm.end());
    test::AssertTrue(min_it != pcm.end(), "PCM should not be empty for non-empty artifact corpus.");
    test::AssertTrue(*min_it >= static_cast<int16_t>(-32767), "Artifact PCM min out of range.");
    test::AssertTrue(*max_it <= static_cast<int16_t>(32767), "Artifact PCM max out of range.");
}

void AssertRoundTripAcrossCorpus(const std::vector<test::CorpusCase>& corpus,
                                 bag_transport_mode mode,
                                 bool include_wav) {
    for (const auto& config_case : test::ConfigCases()) {
        const auto encoder_config = MakeEncoderConfig(config_case, mode);
        const auto decoder_config = MakeDecoderConfig(config_case, mode);

        for (const auto& corpus_case : corpus) {
            const auto pcm = EncodeToVector(encoder_config, corpus_case.text);
            AssertPcmProperties(pcm, corpus_case.text, mode, config_case);

            if (!include_wav) {
                const auto decoded = DecodeFromVector(decoder_config, pcm);
                test::AssertEq(decoded.text, corpus_case.text, "Direct PCM roundtrip should preserve original text.");
                test::AssertEq(decoded.mode, mode, "Decoded mode should match the configured mode.");
                continue;
            }

            const auto dir = test::MakeTempDir("artifact");
            const auto wav_path = dir / (config_case.name + "_" + corpus_case.name + ".wav");
            audio_io::WriteMonoPcm16Wav(wav_path, config_case.sample_rate_hz, pcm);

            const auto wav = audio_io::ReadMonoPcm16Wav(wav_path);
            test::AssertEq(
                wav.sample_rate_hz,
                config_case.sample_rate_hz,
                "WAV sample rate should match the encoding configuration.");

            const auto decoded = DecodeFromVector(decoder_config, wav.mono_pcm);
            test::AssertEq(decoded.text, corpus_case.text, "WAV roundtrip should preserve original text.");
            test::AssertEq(decoded.mode, mode, "Decoded WAV mode should match the configured mode.");
        }
    }
}

void TestArtifactDirectRoundTripAcrossModes() {
    AssertRoundTripAcrossCorpus(test::FlashCorpusCases(), BAG_TRANSPORT_FLASH, false);
    AssertRoundTripAcrossCorpus(test::ProCorpusCases(), BAG_TRANSPORT_PRO, false);
    AssertRoundTripAcrossCorpus(test::UltraCorpusCases(), BAG_TRANSPORT_ULTRA, false);
}

void TestArtifactWavRoundTripAcrossModes() {
    AssertRoundTripAcrossCorpus(test::FlashCorpusCases(), BAG_TRANSPORT_FLASH, true);
    AssertRoundTripAcrossCorpus(test::ProCorpusCases(), BAG_TRANSPORT_PRO, true);
    AssertRoundTripAcrossCorpus(test::UltraCorpusCases(), BAG_TRANSPORT_ULTRA, true);
}

void TestArtifactDecodeUnderGainDrop() {
    struct Case {
        bag_transport_mode mode;
        std::string text;
    };
    const std::vector<Case> cases = {
        {BAG_TRANSPORT_FLASH, "GAIN-TEST"},
        {BAG_TRANSPORT_PRO, "GAIN-TEST"},
        {BAG_TRANSPORT_ULTRA, test::Utf8Literal(u8"增益-测试")},
    };

    for (const auto& config_case : test::ConfigCases()) {
        for (const auto& item : cases) {
            const auto encoder_config = MakeEncoderConfig(config_case, item.mode);
            const auto decoder_config = MakeDecoderConfig(config_case, item.mode);
            auto pcm = EncodeToVector(encoder_config, item.text);

            for (auto& sample : pcm) {
                sample = static_cast<int16_t>(sample / 2);
            }

            const auto decoded = DecodeFromVector(decoder_config, pcm);
            test::AssertEq(decoded.text, item.text, "Decode should survive moderate amplitude attenuation.");
            test::AssertEq(decoded.mode, item.mode, "Gain-drop decode should preserve the selected mode.");
        }
    }
}

void TestArtifactModeSpecificLongRoundTrip() {
    const auto config_case = test::ConfigCases().front();

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);
        const auto text = test::BuildTooLongProCorpus();
        const auto pcm = EncodeToVector(encoder_config, text);
        AssertPcmProperties(pcm, text, BAG_TRANSPORT_PRO, config_case);
        const auto decoded = DecodeFromVector(decoder_config, pcm);
        test::AssertEq(decoded.text, text, "Pro extended ASCII artifact should roundtrip.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_PRO, "Pro extended ASCII artifact should preserve mode.");
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        const auto text = test::BuildTooLongUltraCorpus();
        const auto pcm = EncodeToVector(encoder_config, text);
        AssertPcmProperties(pcm, text, BAG_TRANSPORT_ULTRA, config_case);

        const auto dir = test::MakeTempDir("artifact");
        const auto wav_path = dir / "ultra_extended.wav";
        audio_io::WriteMonoPcm16Wav(wav_path, config_case.sample_rate_hz, pcm);
        const auto wav = audio_io::ReadMonoPcm16Wav(wav_path);

        const auto decoded = DecodeFromVector(decoder_config, wav.mono_pcm);
        test::AssertEq(
            decoded.text,
            text,
            "Ultra extended artifact should preserve UTF-8 text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_ULTRA, "Ultra extended artifact should preserve mode.");
    }
}

void TestArtifactVersionMatchesRelease() {
    const char* version = bag_core_version();
    test::AssertTrue(version != nullptr, "Artifact version pointer should not be null.");
    test::AssertEq(
        std::string(version),
        std::string(test::kExpectedCoreVersion),
        "Artifact layer should observe the current release version.");
}

void TestArtifactFlashLitanyRoundTrip() {
    const auto config_case = test::ConfigCases().front();
    const auto text = std::string("Artifact");
    const auto encoder_config =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_LITANY,
            BAG_FLASH_VOICING_FLAVOR_LITANY);
    const auto decoder_config =
        MakeDecoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_LITANY,
            BAG_FLASH_VOICING_FLAVOR_LITANY);
    const auto pcm = EncodeToVector(encoder_config, text);
    AssertPcmProperties(
        pcm,
        text,
        BAG_TRANSPORT_FLASH,
        config_case,
        BAG_FLASH_SIGNAL_PROFILE_LITANY,
        BAG_FLASH_VOICING_FLAVOR_LITANY);
    const auto decoded = DecodeFromVector(decoder_config, pcm);
    test::AssertEq(decoded.text, text, "Artifact litany flash should roundtrip.");
    test::AssertEq(decoded.mode, BAG_TRANSPORT_FLASH, "Artifact litany flash should preserve mode.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Artifact.DirectRoundTripAcrossModes", TestArtifactDirectRoundTripAcrossModes);
    runner.Add("Artifact.WavRoundTripAcrossModes", TestArtifactWavRoundTripAcrossModes);
    runner.Add("Artifact.ModeSpecificLongRoundTrip", TestArtifactModeSpecificLongRoundTrip);
    runner.Add("Artifact.DecodeUnderGainDrop", TestArtifactDecodeUnderGainDrop);
    runner.Add("Artifact.FlashLitanyRoundTrip", TestArtifactFlashLitanyRoundTrip);
    runner.Add("Artifact.VersionMatchesRelease", TestArtifactVersionMatchesRelease);
    return runner.Run();
}
