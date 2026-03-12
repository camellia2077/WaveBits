#include "test_std_support.h"
#include "test_framework.h"
#include "test_utf8.h"

import audio_io.wav;
import bag.common.error_code;
import bag.common.version;
import bag.flash.codec;
import bag.pro.codec;
import bag.transport.compat.frame_codec;
import bag.ultra.codec;

namespace {

void TestVersionModule() {
    test::AssertEq(std::string(bag::CoreVersion()), std::string("0.3.0"), "Version module should expose core version.");
}

void TestAudioIoModuleRoundTrip() {
    constexpr const char kAudioIoSmokePath[] = "modules_phase2_audio_io_smoke.wav";
    const std::vector<std::int16_t> pcm = {0, 1200, -1200, 3276, -3276};

    std::remove(kAudioIoSmokePath);
    audio_io::WriteMonoPcm16Wav(kAudioIoSmokePath, 44100, pcm);
    const auto wav = audio_io::ReadMonoPcm16Wav(kAudioIoSmokePath);
    std::remove(kAudioIoSmokePath);
    test::AssertEq(wav.sample_rate_hz, 44100, "audio_io module should preserve the sample rate.");
    test::AssertEq(wav.mono_pcm, pcm, "audio_io module should preserve mono PCM content.");
}

void TestFlashCodecModule() {
    const std::string text = test::Utf8Literal(u8"你好，WaveBits");
    std::vector<std::uint8_t> bytes;
    test::AssertEq(
        bag::flash::EncodeTextToBytes(text, &bytes),
        bag::ErrorCode::kOk,
        "Flash codec module should encode UTF-8 bytes.");
    test::AssertEq(
        bytes,
        std::vector<std::uint8_t>(text.begin(), text.end()),
        "Flash codec module should preserve raw byte payload.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodeBytesToText(bytes, &decoded),
        bag::ErrorCode::kOk,
        "Flash codec module should decode raw bytes.");
    test::AssertEq(decoded, text, "Flash codec module should roundtrip UTF-8 text.");
}

void TestProCodecModule() {
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeTextToPayload("ASCII-123", &payload),
        bag::ErrorCode::kOk,
        "Pro codec module should encode ASCII payload.");
    test::AssertEq(
        payload,
        std::vector<std::uint8_t>{'A', 'S', 'C', 'I', 'I', '-', '1', '2', '3'},
        "Pro codec module should keep raw ASCII bytes.");

    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::pro::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "Pro codec module should encode payload symbols.");
    test::AssertEq(
        symbols.size(),
        payload.size() * bag::pro::kSymbolsPerPayloadByte,
        "Pro codec module should emit two symbols per payload byte.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(symbols, &decoded_payload),
        bag::ErrorCode::kOk,
        "Pro codec module should decode symbols back to payload.");
    test::AssertEq(decoded_payload, payload, "Pro codec module should roundtrip payload.");
}

void TestUltraCodecModule() {
    const std::string text = test::Utf8Literal(u8"WaveBits 超级模式 🚀");
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(text, &payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should encode UTF-8 payload.");

    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "Ultra codec module should encode payload symbols.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload(symbols, &decoded_payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should decode symbols back to payload.");
    test::AssertEq(decoded_payload, payload, "Ultra codec module should roundtrip payload symbols.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePayloadToText(decoded_payload, &decoded),
        bag::ErrorCode::kOk,
        "Ultra codec module should decode UTF-8 payload.");
    test::AssertEq(decoded, text, "Ultra codec module should roundtrip UTF-8 text.");
}

void TestCompatFrameCodecModule() {
    const std::vector<std::uint8_t> payload = {'W', 'B', '2'};
    std::vector<std::uint8_t> frame;
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kUltra, payload, &frame),
        bag::ErrorCode::kOk,
        "Compat frame module should encode ultra frame payload.");
    test::AssertEq(
        frame.size(),
        payload.size() + static_cast<std::size_t>(8),
        "Compat frame module should emit header plus CRC bytes around payload.");

    bag::transport::compat::DecodedFrame decoded{};
    test::AssertEq(
        bag::transport::compat::DecodeFrame(frame, &decoded),
        bag::ErrorCode::kOk,
        "Compat frame module should decode a valid frame.");
    test::AssertEq(decoded.mode, bag::TransportMode::kUltra, "Compat frame module should preserve transport mode.");
    test::AssertEq(decoded.payload, payload, "Compat frame module should preserve payload bytes.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("ModulesPhase2.VersionModule", TestVersionModule);
    runner.Add("ModulesPhase2.AudioIoModuleRoundTrip", TestAudioIoModuleRoundTrip);
    runner.Add("ModulesPhase2.FlashCodecModule", TestFlashCodecModule);
    runner.Add("ModulesPhase2.ProCodecModule", TestProCodecModule);
    runner.Add("ModulesPhase2.UltraCodecModule", TestUltraCodecModule);
    runner.Add("ModulesPhase2.CompatFrameCodecModule", TestCompatFrameCodecModule);
    return runner.Run();
}
