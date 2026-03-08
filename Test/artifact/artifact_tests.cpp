#include <algorithm>
#include <string>
#include <vector>

#include "bag_api.h"
#include "bag/pro/codec.h"
#include "bag/pro/frame_codec.h"
#include "bag/pro/text_codec.h"
#include "bag/ultra/codec.h"
#include "test_framework.h"
#include "test_fs.h"
#include "test_vectors.h"
#include "wav_io.h"

namespace {

struct DecodeOutcome {
    std::string text;
    bag_transport_mode mode = BAG_TRANSPORT_FLASH;
};

bag_encoder_config MakeEncoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH) {
    bag_encoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH) {
    bag_decoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
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

size_t ExpectedPcmSampleCount(const std::string& text,
                              bag_transport_mode mode,
                              const test::ConfigCase& config_case) {
    if (mode == BAG_TRANSPORT_FLASH) {
        return text.size() * 8 * static_cast<size_t>(config_case.frame_samples);
    }

    if (mode == BAG_TRANSPORT_PRO) {
        std::vector<uint8_t> payload;
        test::AssertEq(
            bag::pro::EncodeProTextToPayload(text, &payload),
            bag::ErrorCode::kOk,
            "Artifact expected-length pro payload encode should succeed.");
        return payload.size() * bag::pro::kSymbolsPerPayloadByte *
               static_cast<size_t>(config_case.frame_samples);
    }

    std::vector<uint8_t> payload;
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(text, &payload),
        bag::ErrorCode::kOk,
        "Artifact expected-length ultra payload encode should succeed.");
    return payload.size() * bag::ultra::kSymbolsPerPayloadByte *
           static_cast<size_t>(config_case.frame_samples);
}

void AssertPcmProperties(const std::vector<int16_t>& pcm,
                         const std::string& text,
                         bag_transport_mode mode,
                         const test::ConfigCase& config_case) {
    const auto expected_length = ExpectedPcmSampleCount(text, mode, config_case);
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
        {BAG_TRANSPORT_ULTRA, u8"增益-测试"},
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

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Artifact.DirectRoundTripAcrossModes", TestArtifactDirectRoundTripAcrossModes);
    runner.Add("Artifact.WavRoundTripAcrossModes", TestArtifactWavRoundTripAcrossModes);
    runner.Add("Artifact.ModeSpecificLongRoundTrip", TestArtifactModeSpecificLongRoundTrip);
    runner.Add("Artifact.DecodeUnderGainDrop", TestArtifactDecodeUnderGainDrop);
    runner.Add("Artifact.VersionMatchesRelease", TestArtifactVersionMatchesRelease);
    return runner.Run();
}
