#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>

#include "test_std_support.h"
#include "test_framework.h"
#include "test_fs.h"
#include "test_utf8.h"
#include "wav_io.h"

#if __cplusplus >= 202002L
import bag.flash.phy_clean;
import bag.pro.codec;
import bag.transport.compat.frame_codec;
#else
#include "bag/legacy/flash/phy_clean.h"
#include "bag/legacy/pro/codec.h"
#include "bag/legacy/transport/compat/frame_codec.h"
#endif

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

void TestEncodeLengthMatchesExpected() {
    const auto config = MakeBfskConfig();
    const std::string text = "A";
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes(text), config);
    const size_t chunk_size =
        static_cast<size_t>(config.sample_rate_hz * config.bit_duration_sec);
    test::AssertEq(
        pcm.size(),
        static_cast<size_t>(8) * chunk_size,
        "Encoded sample count should be 8 bits * chunk size for one byte.");
}

void TestEncodeAmplitudeInRange() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("Hello"), config);
    const auto [min_it, max_it] = std::minmax_element(pcm.begin(), pcm.end());
    test::AssertTrue(min_it != pcm.end(), "PCM should not be empty for non-empty input.");
    test::AssertTrue(*min_it >= static_cast<int16_t>(-32767), "PCM min out of range.");
    test::AssertTrue(*max_it <= static_cast<int16_t>(32767), "PCM max out of range.");
}

void TestDecodeEmptyInputReturnsEmptyText() {
    const auto config = MakeBfskConfig();
    const std::vector<std::int16_t> pcm;
    const auto decoded_bytes = bag::flash::DecodePcm16ToBytes(pcm, config);
    const std::string decoded(decoded_bytes.begin(), decoded_bytes.end());
    test::AssertEq(decoded, std::string(), "Decoding empty input should return empty string.");
}

void TestWavIoMonoRoundTrip() {
    const auto config = MakeBfskConfig();
    const std::string text = "Unit";
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes(text), config);

    const auto dir = test::MakeTempDir("unit");
    const auto path = dir / "mono_roundtrip.wav";
    audio_io::WriteMonoPcm16Wav(path, config.sample_rate_hz, pcm);

    const auto read_back = audio_io::ReadMonoPcm16Wav(path);
    test::AssertEq(
        read_back.sample_rate_hz,
        config.sample_rate_hz,
        "Sample rate should be preserved in wav read/write.");
    test::AssertEq(
        read_back.mono_pcm.size(),
        pcm.size(),
        "Sample count should be preserved in mono wav read/write.");
    test::AssertEq(read_back.mono_pcm, pcm, "PCM content should be identical after roundtrip.");
}

void TestSnapshotFirstSamplesStable() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("A"), config);
    const std::vector<std::int16_t> expected = {
        0, 1493, 2981, 4459, 5924, 7368, 8789, 10182,
        11541, 12863, 14143, 15377, 16561, 17692, 18765, 19777};

    test::AssertTrue(pcm.size() >= expected.size(), "PCM must contain enough samples for snapshot.");
    for (size_t index = 0; index < expected.size(); ++index) {
        if (pcm[index] != expected[index]) {
            test::Fail("Snapshot mismatch at sample index " + std::to_string(index));
        }
    }
}

void TestProCodecRoundTrip() {
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeTextToPayload("A", &payload),
        bag::ErrorCode::kOk,
        "Single-character pro payload encode should succeed.");
    test::AssertEq(
        payload,
        std::vector<std::uint8_t>{static_cast<std::uint8_t>('A')},
        "Pro payload should now store raw ASCII bytes.");

    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::pro::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "Pro payload-to-symbol encode should succeed.");
    test::AssertEq(
        symbols,
        std::vector<std::uint8_t>{0x04, 0x01},
        "Pro symbols should map a byte to high/low nibbles.");

    payload.clear();
    test::AssertEq(
        bag::pro::EncodeTextToPayload("Hello-123", &payload),
        bag::ErrorCode::kOk,
        "ASCII pro payload encode should succeed.");
    test::AssertEq(
        bag::pro::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "ASCII pro symbol encode should succeed.");
    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Pro payload decode should succeed.");
    test::AssertEq(decoded, std::string("Hello-123"), "Pro payload decode should recover the original text.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(symbols, &decoded_payload),
        bag::ErrorCode::kOk,
        "Pro symbol decode should succeed.");
    test::AssertEq(decoded_payload, payload, "Pro symbol decode should recover the original payload.");
}

void TestProCodecRejectsInvalidInput() {
    std::vector<std::uint8_t> payload;
    const auto non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag::pro::EncodeTextToPayload(non_ascii, &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro payload encode should reject non-ASCII input.");

    const std::vector<std::uint8_t> bad_payload = {0x80};
    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToText(bad_payload, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Pro payload decode should reject non-ASCII bytes.");

    const std::vector<std::uint8_t> odd_symbols = {0x04};
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(odd_symbols, &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro symbol decode should reject odd nibble counts.");

    const std::vector<std::uint8_t> out_of_range_symbols = {0x04, 0x10};
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(out_of_range_symbols, &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro symbol decode should reject nibble values outside 0x0..0xF.");
}

void TestFrameCodecRoundTrip() {
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeTextToPayload("Frame", &payload),
        bag::ErrorCode::kOk,
        "Frame test payload encode should succeed.");

    std::vector<std::uint8_t> frame;
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kPro, payload, &frame),
        bag::ErrorCode::kOk,
        "Frame encode should succeed.");

    bag::transport::compat::DecodedFrame decoded_frame{};
    test::AssertEq(
        bag::transport::compat::DecodeFrame(frame, &decoded_frame),
        bag::ErrorCode::kOk,
        "Frame decode should succeed.");
    test::AssertEq(decoded_frame.mode, bag::TransportMode::kPro, "Decoded frame mode should match input.");
    test::AssertEq(decoded_frame.payload, payload, "Decoded frame payload should match input.");
}

void TestFrameCodecRejectsMalformedFrames() {
    const std::string utf8_text = test::Utf8Literal(u8"你好");
    const std::vector<std::uint8_t> payload(utf8_text.begin(), utf8_text.end());

    std::vector<std::uint8_t> frame;
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kUltra, payload, &frame),
        bag::ErrorCode::kOk,
        "Malformed frame setup encode should succeed.");

    bag::transport::compat::DecodedFrame decoded_frame{};

    auto bad_preamble = frame;
    bad_preamble[0] = 0x00;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_preamble, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject bad preamble.");

    auto bad_version = frame;
    bad_version[2] = 0x02;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_version, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject bad version.");

    auto bad_mode = frame;
    bad_mode[3] = 0x00;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_mode, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject bad mode.");

    auto bad_length = frame;
    bad_length[5] = static_cast<uint8_t>(bad_length[5] + 1);
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_length, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject mismatched payload length.");

    auto bad_crc = frame;
    bad_crc.back() ^= 0x01;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_crc, &decoded_frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame decode should reject CRC mismatch.");

    std::vector<std::uint8_t> oversized_payload(
        bag::transport::compat::kMaxFramePayloadBytes + 1,
        static_cast<std::uint8_t>('A'));
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kPro, oversized_payload, &frame),
        bag::ErrorCode::kInvalidArgument,
        "Frame encode should reject payloads above the single-frame limit.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Unit.EncodeLengthMatchesExpected", TestEncodeLengthMatchesExpected);
    runner.Add("Unit.EncodeAmplitudeInRange", TestEncodeAmplitudeInRange);
    runner.Add("Unit.DecodeEmptyInputReturnsEmptyText", TestDecodeEmptyInputReturnsEmptyText);
    runner.Add("Unit.WavIoMonoRoundTrip", TestWavIoMonoRoundTrip);
    runner.Add("Unit.SnapshotFirstSamplesStable", TestSnapshotFirstSamplesStable);
    runner.Add("Unit.ProCodecRoundTrip", TestProCodecRoundTrip);
    runner.Add("Unit.ProCodecRejectsInvalidInput", TestProCodecRejectsInvalidInput);
    runner.Add("Unit.FrameCodecRoundTrip", TestFrameCodecRoundTrip);
    runner.Add("Unit.FrameCodecRejectsMalformedFrames", TestFrameCodecRejectsMalformedFrames);
    return runner.Run();
}

