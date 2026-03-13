#include "test_std_support.h"
#include "test_audio_io.h"
#include "test_fs.h"
#include "test_framework.h"
#include "test_vectors.h"
#include "test_utf8.h"

import audio_io.wav;
import bag.common.error_code;
import bag.common.version;
import bag.flash.codec;
import bag.flash.phy_clean;
import bag.pro.codec;
import bag.transport.compat.frame_codec;
import bag.ultra.codec;

namespace {

bag::flash::BfskConfig MakeBfskConfig() {
    bag::flash::BfskConfig config{};
    config.sample_rate_hz = 44100;
    config.bit_duration_sec = 0.05;
    return config;
}

std::vector<std::uint8_t> AsBytes(const std::string& text) {
    return std::vector<std::uint8_t>(text.begin(), text.end());
}

void TestVersionModule() {
    test::AssertEq(
        std::string(bag::CoreVersion()),
        std::string(test::kExpectedCoreVersion),
        "Version module should expose core version.");
}

void TestAudioIoModuleRoundTripContract() {
    const auto dir = test::MakeTempDir("modules_phase2");
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto path = dir / (std::string(test_case.name) + ".wav");
        audio_io::WriteMonoPcm16Wav(path, test_case.sample_rate_hz, test_case.mono_pcm);
        const auto wav = audio_io::ReadMonoPcm16Wav(path);
        test::AssertAudioIoRoundTripResult(wav, test_case, "Module audio_io boundary");
    }
}

void TestAudioIoModuleReadMissingFileFails() {
    const auto missing_path = test::MakeTempDir("modules_phase2") / "missing.wav";
    test::AssertThrows(
        [&] {
            (void)audio_io::ReadMonoPcm16Wav(missing_path);
        },
        "Module audio_io boundary should throw when the input file does not exist.");
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

void TestFlashPhyCleanEncodeLengthMatchesExpected() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("A"), config);
    const std::size_t chunk_size =
        static_cast<std::size_t>(config.sample_rate_hz * config.bit_duration_sec);
    test::AssertEq(
        pcm.size(),
        static_cast<std::size_t>(8) * chunk_size,
        "Flash PHY module should emit 8 bits times chunk size for one byte.");
}

void TestFlashPhyCleanAmplitudeInRange() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("Hello"), config);
    test::AssertTrue(!pcm.empty(), "Flash PHY module PCM should not be empty for non-empty input.");

    std::int16_t min_sample = pcm.front();
    std::int16_t max_sample = pcm.front();
    for (std::int16_t sample : pcm) {
        if (sample < min_sample) {
            min_sample = sample;
        }
        if (sample > max_sample) {
            max_sample = sample;
        }
    }

    test::AssertTrue(min_sample >= static_cast<std::int16_t>(-32767), "Flash PHY module PCM min out of range.");
    test::AssertTrue(max_sample <= static_cast<std::int16_t>(32767), "Flash PHY module PCM max out of range.");
}

void TestFlashPhyCleanDecodeEmptyInputReturnsEmptyText() {
    const auto config = MakeBfskConfig();
    const std::vector<std::int16_t> pcm;
    const auto decoded_bytes = bag::flash::DecodePcm16ToBytes(pcm, config);
    const std::string decoded(decoded_bytes.begin(), decoded_bytes.end());
    test::AssertEq(decoded, std::string(), "Flash PHY module should decode empty PCM to empty text.");
}

void TestFlashPhyCleanSnapshotFirstSamplesStable() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("A"), config);
    const std::vector<std::int16_t> expected = {
        0, 1493, 2981, 4459, 5924, 7368, 8789, 10182,
        11541, 12863, 14143, 15377, 16561, 17692, 18765, 19777};

    test::AssertTrue(
        pcm.size() >= expected.size(),
        "Flash PHY module PCM must contain enough samples for snapshot coverage.");
    for (std::size_t index = 0; index < expected.size(); ++index) {
        if (pcm[index] != expected[index]) {
            test::Fail("Flash PHY module snapshot mismatch at sample index " + std::to_string(index));
        }
    }
}

void TestProCodecModule() {
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeTextToPayload("A", &payload),
        bag::ErrorCode::kOk,
        "Single-character pro payload encode should succeed.");
    test::AssertEq(
        payload,
        std::vector<std::uint8_t>{static_cast<std::uint8_t>('A')},
        "Pro codec module should preserve a single ASCII byte.");

    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::pro::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "Single-character pro symbol encode should succeed.");
    test::AssertEq(
        symbols,
        std::vector<std::uint8_t>{0x04, 0x01},
        "Pro codec module should map a byte to high and low nibbles.");

    payload.clear();
    test::AssertEq(
        bag::pro::EncodeTextToPayload("ASCII-123", &payload),
        bag::ErrorCode::kOk,
        "Pro codec module should encode ASCII payload.");
    test::AssertEq(
        payload,
        std::vector<std::uint8_t>{'A', 'S', 'C', 'I', 'I', '-', '1', '2', '3'},
        "Pro codec module should keep raw ASCII bytes.");

    test::AssertEq(
        bag::pro::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "Pro codec module should encode payload symbols.");
    test::AssertEq(
        symbols.size(),
        payload.size() * bag::pro::kSymbolsPerPayloadByte,
        "Pro codec module should emit two symbols per payload byte.");

    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Pro codec module should decode payload bytes back to text.");
    test::AssertEq(decoded, std::string("ASCII-123"), "Pro codec module should roundtrip ASCII text.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(symbols, &decoded_payload),
        bag::ErrorCode::kOk,
        "Pro codec module should decode symbols back to payload.");
    test::AssertEq(decoded_payload, payload, "Pro codec module should roundtrip payload.");
}

void TestProCodecRejectsInvalidInput() {
    std::vector<std::uint8_t> payload;
    const auto non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag::pro::EncodeTextToPayload(non_ascii, &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro codec module should reject non-ASCII input.");

    const std::vector<std::uint8_t> bad_payload = {0x80};
    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToText(bad_payload, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Pro codec module should reject non-ASCII bytes.");

    const std::vector<std::uint8_t> odd_symbols = {0x04};
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(odd_symbols, &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro codec module should reject odd nibble counts.");

    const std::vector<std::uint8_t> out_of_range_symbols = {0x04, 0x10};
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(out_of_range_symbols, &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro codec module should reject nibble values outside 0x0..0xF.");
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

void TestCompatFrameCodecProRoundTrip() {
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeTextToPayload("Frame", &payload),
        bag::ErrorCode::kOk,
        "Compat frame module setup should encode pro payload.");

    std::vector<std::uint8_t> frame;
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kPro, payload, &frame),
        bag::ErrorCode::kOk,
        "Compat frame module should encode pro frame payload.");

    bag::transport::compat::DecodedFrame decoded{};
    test::AssertEq(
        bag::transport::compat::DecodeFrame(frame, &decoded),
        bag::ErrorCode::kOk,
        "Compat frame module should decode a valid pro frame.");
    test::AssertEq(decoded.mode, bag::TransportMode::kPro, "Compat frame module should preserve pro transport mode.");
    test::AssertEq(decoded.payload, payload, "Compat frame module should preserve pro payload bytes.");
}

void TestCompatFrameCodecRejectsMalformedFrames() {
    const std::string utf8_text = test::Utf8Literal(u8"你好");
    const std::vector<std::uint8_t> payload(utf8_text.begin(), utf8_text.end());

    std::vector<std::uint8_t> frame;
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kUltra, payload, &frame),
        bag::ErrorCode::kOk,
        "Compat frame module malformed-frame setup should encode successfully.");

    bag::transport::compat::DecodedFrame decoded{};

    auto bad_preamble = frame;
    bad_preamble[0] = 0x00;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_preamble, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject bad preamble.");

    auto bad_version = frame;
    bad_version[2] = 0x02;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_version, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject bad version.");

    auto bad_mode = frame;
    bad_mode[3] = 0x00;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_mode, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject bad mode.");

    auto bad_length = frame;
    bad_length[5] = static_cast<std::uint8_t>(bad_length[5] + 1);
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_length, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject mismatched payload length.");

    auto bad_crc = frame;
    bad_crc.back() ^= 0x01;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_crc, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject CRC mismatch.");

    std::vector<std::uint8_t> oversized_payload(
        bag::transport::compat::kMaxFramePayloadBytes + 1,
        static_cast<std::uint8_t>('A'));
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kPro, oversized_payload, &frame),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject payloads above the single-frame limit.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("ModulesPhase2.VersionModule", TestVersionModule);
    runner.Add("ModulesPhase2.AudioIoModuleRoundTripContract", TestAudioIoModuleRoundTripContract);
    runner.Add("ModulesPhase2.AudioIoModuleReadMissingFileFails", TestAudioIoModuleReadMissingFileFails);
    runner.Add("ModulesPhase2.FlashCodecModule", TestFlashCodecModule);
    runner.Add("ModulesPhase2.FlashPhyCleanEncodeLengthMatchesExpected", TestFlashPhyCleanEncodeLengthMatchesExpected);
    runner.Add("ModulesPhase2.FlashPhyCleanAmplitudeInRange", TestFlashPhyCleanAmplitudeInRange);
    runner.Add(
        "ModulesPhase2.FlashPhyCleanDecodeEmptyInputReturnsEmptyText",
        TestFlashPhyCleanDecodeEmptyInputReturnsEmptyText);
    runner.Add(
        "ModulesPhase2.FlashPhyCleanSnapshotFirstSamplesStable",
        TestFlashPhyCleanSnapshotFirstSamplesStable);
    runner.Add("ModulesPhase2.ProCodecModule", TestProCodecModule);
    runner.Add("ModulesPhase2.ProCodecRejectsInvalidInput", TestProCodecRejectsInvalidInput);
    runner.Add("ModulesPhase2.UltraCodecModule", TestUltraCodecModule);
    runner.Add("ModulesPhase2.CompatFrameCodecModule", TestCompatFrameCodecModule);
    runner.Add("ModulesPhase2.CompatFrameCodecProRoundTrip", TestCompatFrameCodecProRoundTrip);
    runner.Add(
        "ModulesPhase2.CompatFrameCodecRejectsMalformedFrames",
        TestCompatFrameCodecRejectsMalformedFrames);
    return runner.Run();
}
