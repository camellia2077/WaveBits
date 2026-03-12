#include "test_std_support.h"
#include "test_framework.h"
#include "test_utf8.h"

import bag.common.config;
import bag.common.error_code;
import bag.common.types;
import bag.flash.phy_clean;
import bag.fsk.codec;
import bag.pro.phy_compat;
import bag.pro.phy_clean;
import bag.transport.compat.frame_codec;
import bag.ultra.phy_compat;
import bag.ultra.phy_clean;

namespace {

bag::CoreConfig MakeFlashConfig() {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.enable_diagnostics = false;
    config.mode = bag::TransportMode::kFlash;
    return config;
}

bag::CoreConfig MakeProConfig() {
    auto config = MakeFlashConfig();
    config.mode = bag::TransportMode::kPro;
    return config;
}

bag::CoreConfig MakeUltraConfig() {
    auto config = MakeFlashConfig();
    config.mode = bag::TransportMode::kUltra;
    return config;
}

void PushAndPollExpectingText(std::unique_ptr<bag::ITransportDecoder> decoder,
                              const std::vector<std::int16_t>& pcm,
                              bag::TransportMode mode,
                              const std::string& text) {
    test::AssertTrue(decoder != nullptr, "Decoder factory should return a decoder instance.");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 789;
    test::AssertEq(
        decoder->PushPcm(block),
        bag::ErrorCode::kOk,
        "Imported transport decoder contract should accept PCM.");

    bag::TextResult result{};
    test::AssertEq(
        decoder->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Imported transport decoder contract should produce a result.");
    test::AssertEq(result.text, text, "Imported transport decoder contract should preserve decoded text.");
    test::AssertEq(result.mode, mode, "Imported transport decoder contract should preserve mode.");
}

void TestFlashPhyCleanRoundTrip() {
    const auto config = MakeFlashConfig();
    const std::string input = test::Utf8Literal(u8"Phase3");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Flash phy_clean module should encode text to PCM.");
    test::AssertTrue(!pcm.empty(), "Flash phy_clean module should emit PCM.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Flash phy_clean module should decode PCM back to text.");
    test::AssertEq(decoded, input, "Flash phy_clean module should roundtrip text.");
}

void TestFlashDecoderContractRoundTrip() {
    const auto config = MakeFlashConfig();
    const std::string input = "decoder";

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Flash decoder setup encode should succeed.");

    PushAndPollExpectingText(bag::flash::CreateDecoder(config), pcm, bag::TransportMode::kFlash, input);
}

void TestProPhyCleanRoundTrip() {
    const auto config = MakeProConfig();
    const std::string input = "PRO-123";

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::pro::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Pro phy_clean module should encode text to PCM.");
    test::AssertTrue(!pcm.empty(), "Pro phy_clean module should emit PCM.");

    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Pro phy_clean module should decode PCM back to text.");
    test::AssertEq(decoded, input, "Pro phy_clean module should roundtrip text.");
}

void TestProDecoderContractRoundTrip() {
    const auto config = MakeProConfig();
    const std::string input = "decoder-pro";

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::pro::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Pro decoder setup encode should succeed.");

    PushAndPollExpectingText(bag::pro::CreateDecoder(config), pcm, bag::TransportMode::kPro, input);
}

void TestUltraPhyCleanRoundTrip() {
    const auto config = MakeUltraConfig();
    const std::string input = test::Utf8Literal(u8"WaveBits 超级");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Ultra phy_clean module should encode text to PCM.");
    test::AssertTrue(!pcm.empty(), "Ultra phy_clean module should emit PCM.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Ultra phy_clean module should decode PCM back to text.");
    test::AssertEq(decoded, input, "Ultra phy_clean module should roundtrip text.");
}

void TestUltraDecoderContractRoundTrip() {
    const auto config = MakeUltraConfig();
    const std::string input = test::Utf8Literal(u8"decoder-ultra");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Ultra decoder setup encode should succeed.");

    PushAndPollExpectingText(bag::ultra::CreateDecoder(config), pcm, bag::TransportMode::kUltra, input);
}

void TestFskCodecRoundTrip() {
    bag::fsk::FskConfig config{};
    config.sample_rate_hz = 44100;
    config.bit_duration_sec = 0.05;

    const std::string input = "FSK";
    const auto pcm = bag::fsk::EncodeTextToPcm16(input, config);
    test::AssertTrue(!pcm.empty(), "FSK module should emit PCM.");

    const auto decoded = bag::fsk::DecodePcm16ToText(pcm, config);
    test::AssertEq(decoded, input, "FSK module should roundtrip text.");
}

void TestProCompatRoundTrip() {
    const auto config = MakeProConfig();
    const std::string input = "compat-pro";

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::pro::EncodeTextToPcm16Compat(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Pro phy_compat module should encode framed text to PCM.");
    test::AssertTrue(!pcm.empty(), "Pro phy_compat module should emit PCM.");

    PushAndPollExpectingText(bag::pro::CreateCompatDecoder(config), pcm, bag::TransportMode::kPro, input);
}

void TestUltraCompatRoundTrip() {
    const auto config = MakeUltraConfig();
    const std::string input = test::Utf8Literal(u8"compat-ultra");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16Compat(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Ultra phy_compat module should encode framed text to PCM.");
    test::AssertTrue(!pcm.empty(), "Ultra phy_compat module should emit PCM.");

    PushAndPollExpectingText(
        bag::ultra::CreateCompatDecoder(config),
        pcm,
        bag::TransportMode::kUltra,
        input);
}

void TestCompatFrameBytesBridge() {
    const auto config = MakeProConfig();
    const std::vector<std::uint8_t> payload = {'P', '3'};

    std::vector<std::uint8_t> frame_bytes;
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kPro, payload, &frame_bytes),
        bag::ErrorCode::kOk,
        "Compat frame codec setup should encode frame bytes.");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::pro::EncodeFrameBytesToPcm16(config, frame_bytes, &pcm),
        bag::ErrorCode::kOk,
        "Pro phy_compat module should encode raw frame bytes to PCM.");

    std::vector<std::uint8_t> decoded_frame_bytes;
    test::AssertEq(
        bag::pro::DecodePcm16ToFrameBytes(config, pcm, &decoded_frame_bytes),
        bag::ErrorCode::kOk,
        "Pro phy_compat module should decode PCM back to frame bytes.");
    test::AssertEq(decoded_frame_bytes, frame_bytes, "Compat frame bridge should preserve frame bytes.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("ModulesPhase3.FlashPhyCleanRoundTrip", TestFlashPhyCleanRoundTrip);
    runner.Add("ModulesPhase3.FlashDecoderContractRoundTrip", TestFlashDecoderContractRoundTrip);
    runner.Add("ModulesPhase3.ProPhyCleanRoundTrip", TestProPhyCleanRoundTrip);
    runner.Add("ModulesPhase3.ProDecoderContractRoundTrip", TestProDecoderContractRoundTrip);
    runner.Add("ModulesPhase3.UltraPhyCleanRoundTrip", TestUltraPhyCleanRoundTrip);
    runner.Add("ModulesPhase3.UltraDecoderContractRoundTrip", TestUltraDecoderContractRoundTrip);
    runner.Add("ModulesPhase3.FskCodecRoundTrip", TestFskCodecRoundTrip);
    runner.Add("ModulesPhase3.ProCompatRoundTrip", TestProCompatRoundTrip);
    runner.Add("ModulesPhase3.UltraCompatRoundTrip", TestUltraCompatRoundTrip);
    runner.Add("ModulesPhase3.CompatFrameBytesBridge", TestCompatFrameBytesBridge);
    return runner.Run();
}
