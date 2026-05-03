#include <filesystem>
#include <fstream>
#include <iterator>
#include <string>
#include <vector>

#include "test_framework.h"
#include "test_fs.h"
#include "test_process.h"
#include "test_utf8.h"
#include "test_vectors.h"

namespace {

std::filesystem::path GetCliPath(int argc, char* argv[]) {
    if (argc < 2) {
        test::Fail("CLI smoke tests require the CLI binary path as argv[1].");
    }
    return std::filesystem::path(argv[1]);
}

test::ProcessResult RunCli(const std::filesystem::path& cli_path,
                           const std::vector<std::string>& args,
                           const std::filesystem::path& scratch_dir) {
    return test::RunProcess(cli_path, args, scratch_dir);
}

void TestVersionCommand(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");

    const auto version_result = RunCli(cli_path, {"version"}, dir);
    test::AssertEq(version_result.exit_code, 0, "`version` should exit successfully.");
    test::AssertContains(
        version_result.output,
        std::string("presentation: v") + test::kExpectedCliPresentationVersion,
        "`version` output should contain the presentation version.");
    test::AssertContains(
        version_result.output,
        "build: rust-wav",
        "`version` output should contain the Rust WAV build label.");
    test::AssertContains(
        version_result.output,
        std::string("core: v") + test::kExpectedCoreVersion,
        "`version` output should contain the core version.");

    const auto dash_version_result = RunCli(cli_path, {"--version"}, dir);
    test::AssertEq(dash_version_result.exit_code, 0, "`--version` should exit successfully.");
    test::AssertContains(
        dash_version_result.output,
        test::kExpectedCliPresentationVersion,
        "`--version` output should contain the presentation version.");
}

void TestFlashEncodeDecodeDirectText(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto artifact_path = dir / "flash.wav";

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "flash", "--text", "Smoke-CLI", "--out", artifact_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Flash CLI encode should succeed.");
    test::AssertTrue(std::filesystem::exists(artifact_path), "Flash encoded WAV artifact should exist.");
    test::AssertTrue(std::filesystem::file_size(artifact_path) > 0, "Flash encoded WAV artifact should be non-empty.");

    const auto decode_result =
        RunCli(cli_path, {"decode", "--in", artifact_path.string()}, dir);
    test::AssertEq(decode_result.exit_code, 0, "Flash CLI decode should succeed.");
    test::AssertContains(
        decode_result.output,
        "Smoke-CLI",
        "Flash CLI decode output should contain the original text.");
}

void TestProEncodeDecodeDirectText(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto artifact_path = dir / "pro.wav";

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "pro", "--text", "ASCII-123", "--out", artifact_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Pro CLI encode should succeed for ASCII input.");
    test::AssertTrue(std::filesystem::exists(artifact_path), "Pro encoded WAV artifact should exist.");

    const auto decode_result =
        RunCli(cli_path, {"decode", "--in", artifact_path.string()}, dir);
    test::AssertEq(decode_result.exit_code, 0, "Pro CLI decode should succeed.");
    test::AssertContains(
        decode_result.output,
        "ASCII-123",
        "Pro CLI decode output should contain the original text.");
}

void TestUltraEncodeTextFileAndDecodeToTextFile(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto input_path = dir / "input.txt";
    const auto artifact_path = dir / "ultra.wav";
    const auto output_text_path = dir / "decoded.txt";
    const std::string input = test::Utf8Literal(u8"FlipBits 超级模式 🚀");

    test::WriteTextFile(input_path, input);

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "ultra", "--text-file", input_path.string(), "--out", artifact_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Ultra CLI encode from text file should succeed.");
    test::AssertTrue(std::filesystem::exists(artifact_path), "Ultra CLI should create a WAV artifact output.");

    const auto decode_result = RunCli(
        cli_path,
        {"decode", "--in", artifact_path.string(), "--out-text", output_text_path.string()},
        dir);
    test::AssertEq(decode_result.exit_code, 0, "Ultra CLI decode to text file should succeed.");
    test::AssertTrue(
        std::filesystem::exists(output_text_path),
        "Ultra CLI decode with --out-text should create a text file.");
    test::AssertEq(
        test::ReadTextFile(output_text_path),
        input,
        "Ultra CLI decoded text file content should match the original input.");
}

void TestProExtendedAsciiTextFileRoundTrip(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto input_path = dir / "pro_extended.txt";
    const auto artifact_path = dir / "pro_extended.wav";
    const auto output_text_path = dir / "pro_extended_decoded.txt";

    test::WriteTextFile(input_path, test::BuildTooLongProCorpus());

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "pro", "--text-file", input_path.string(), "--out", artifact_path.string()},
        dir);
    test::AssertEq(
        encode_result.exit_code,
        0,
        "Pro CLI should accept extended ASCII text beyond the old compat limit.");

    const auto decode_result = RunCli(
        cli_path,
        {"decode", "--in", artifact_path.string(), "--out-text", output_text_path.string()},
        dir);
    test::AssertEq(decode_result.exit_code, 0, "Pro CLI should decode the extended ASCII corpus.");
    test::AssertEq(
        test::ReadTextFile(output_text_path),
        test::BuildTooLongProCorpus(),
        "Pro CLI should preserve the extended ASCII corpus.");
}

void TestUltraExtendedUtf8TextFileRoundTrip(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto input_path = dir / "ultra_extended.txt";
    const auto artifact_path = dir / "ultra_extended.wav";
    const auto output_text_path = dir / "ultra_extended_decoded.txt";

    test::WriteTextFile(input_path, test::BuildTooLongUltraCorpus());

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "ultra", "--text-file", input_path.string(), "--out", artifact_path.string()},
        dir);
    test::AssertEq(
        encode_result.exit_code,
        0,
        "Ultra CLI should accept extended UTF-8 text beyond the old compat limit.");

    const auto decode_result = RunCli(
        cli_path,
        {"decode", "--in", artifact_path.string(), "--out-text", output_text_path.string()},
        dir);
    test::AssertEq(decode_result.exit_code, 0, "Ultra CLI should decode the extended UTF-8 corpus.");
    test::AssertEq(
        test::ReadTextFile(output_text_path),
        test::BuildTooLongUltraCorpus(),
        "Ultra CLI should preserve the extended UTF-8 corpus.");
}

void TestModeSpecificValidation(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto invalid_artifact_path = dir / "invalid.wav";

    test::WriteTextFile(invalid_artifact_path, "not-a-wavebits-wav");

    const auto invalid_result = RunCli(
        cli_path,
        {"decode", "--in", invalid_artifact_path.string()},
        dir);
    test::AssertTrue(invalid_result.exit_code != 0, "WAV CLI should reject invalid artifact input.");
    test::AssertContains(
        invalid_result.output,
        "Invalid WAV header",
        "WAV CLI should explain the invalid WAV header.");
}

void TestInvalidArgumentsShowUsage(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");

    const auto no_args_result = RunCli(cli_path, {}, dir);
    test::AssertTrue(no_args_result.exit_code != 0, "Invoking CLI without args should fail.");
    test::AssertContains(no_args_result.output, "Usage:", "CLI without args should print usage.");
    const auto invalid_mode_result =
        RunCli(cli_path, {"encode", "--mode", "warp", "--text", "value", "--out", (dir / "x.wav").string()}, dir);
    test::AssertTrue(invalid_mode_result.exit_code != 0, "Invalid mode should fail.");
    test::AssertContains(
        invalid_mode_result.output,
        "invalid value",
        "Invalid mode should print a clear error.");

    const auto invalid_result = RunCli(cli_path, {"encode", "--unknown", "value"}, dir);
    test::AssertTrue(invalid_result.exit_code != 0, "Invalid CLI arguments should fail.");
    test::AssertContains(invalid_result.output, "Usage:", "Invalid CLI arguments should print usage.");
}

void TestDecodeUsesEmbeddedMetadata(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto artifact_path = dir / "embedded_metadata.wav";

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "flash", "--text", "Mode-Drift", "--out", artifact_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Setup encode should succeed before metadata-driven decode.");

    const auto decode_result =
        RunCli(cli_path, {"decode", "--in", artifact_path.string()}, dir);
    test::AssertEq(decode_result.exit_code, 0, "Decoding with embedded metadata should succeed.");
    test::AssertContains(
        decode_result.output,
        "Mode-Drift",
        "Metadata-driven decode should recover the original text.");
}

void TestDecodeRejectsTruncatedWav(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto artifact_path = dir / "truncated.wav";

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "pro", "--text", "Truncate-Case", "--out", artifact_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Setup encode should succeed before truncation.");

    std::ifstream artifact_stream(artifact_path, std::ios::binary);
    test::AssertTrue(artifact_stream.good(), "Setup WAV should be readable before truncation.");
    std::vector<char> wav_bytes{
        std::istreambuf_iterator<char>(artifact_stream),
        std::istreambuf_iterator<char>()};
    test::AssertTrue(wav_bytes.size() > 1, "Setup WAV should be large enough to truncate.");
    wav_bytes.pop_back();
    std::ofstream truncated_stream(artifact_path, std::ios::binary | std::ios::trunc);
    test::AssertTrue(truncated_stream.good(), "Truncated WAV output should be writable.");
    truncated_stream.write(wav_bytes.data(), static_cast<std::streamsize>(wav_bytes.size()));
    test::AssertTrue(truncated_stream.good(), "Truncated WAV output should write successfully.");

    const auto decode_result =
        RunCli(cli_path, {"decode", "--in", artifact_path.string()}, dir);
    test::AssertTrue(decode_result.exit_code != 0, "Decoding truncated WAV should fail.");
    test::AssertContains(
        decode_result.output,
        "failed to parse WAV input",
        "Truncated WAV failure should surface the parse-stage context.");
    test::AssertContains(
        decode_result.output,
        "Truncated WAV data",
        "Truncated WAV failure should preserve the concrete parse reason.");
}

void TestDecodeRejectsCanonicalWavWithoutFlipBitsMetadata(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto artifact_path = dir / "canonical.wav";
    const std::vector<unsigned char> canonical_wav = {
        'R','I','F','F', 0x28,0x00,0x00,0x00, 'W','A','V','E',
        'f','m','t',' ', 0x10,0x00,0x00,0x00, 0x01,0x00,0x01,0x00,
        0x44,0xAC,0x00,0x00, 0x88,0x58,0x01,0x00, 0x02,0x00,0x10,0x00,
        'd','a','t','a', 0x04,0x00,0x00,0x00, 0x00,0x00,0x01,0x00
    };
    std::ofstream output(artifact_path, std::ios::binary | std::ios::trunc);
    output.write(reinterpret_cast<const char*>(canonical_wav.data()), static_cast<std::streamsize>(canonical_wav.size()));
    test::AssertTrue(output.good(), "Canonical WAV fixture should be writable.");
    output.close();

    const auto decode_result =
        RunCli(cli_path, {"decode", "--in", artifact_path.string()}, dir);
    test::AssertTrue(decode_result.exit_code != 0, "Decoding canonical WAV without metadata should fail.");
    test::AssertContains(
        decode_result.output,
        "failed to read FlipBits metadata from WAV input",
        "Canonical WAV decode failure should surface metadata-stage context.");
    test::AssertContains(
        decode_result.output,
        "not found",
        "Canonical WAV decode failure should preserve the missing-metadata reason.");
}

}  // namespace

int main(int argc, char* argv[]) {
    const auto cli_path = GetCliPath(argc, argv);

    test::Runner runner;
    runner.Add("CliSmoke.VersionCommand", [&]() { TestVersionCommand(cli_path); });
    runner.Add("CliSmoke.FlashEncodeDecodeDirectText", [&]() { TestFlashEncodeDecodeDirectText(cli_path); });
    runner.Add("CliSmoke.ProEncodeDecodeDirectText", [&]() { TestProEncodeDecodeDirectText(cli_path); });
    runner.Add(
        "CliSmoke.UltraEncodeTextFileAndDecodeToTextFile",
        [&]() { TestUltraEncodeTextFileAndDecodeToTextFile(cli_path); });
    runner.Add(
        "CliSmoke.ProExtendedAsciiTextFileRoundTrip",
        [&]() { TestProExtendedAsciiTextFileRoundTrip(cli_path); });
    runner.Add(
        "CliSmoke.UltraExtendedUtf8TextFileRoundTrip",
        [&]() { TestUltraExtendedUtf8TextFileRoundTrip(cli_path); });
    runner.Add("CliSmoke.ModeSpecificValidation", [&]() { TestModeSpecificValidation(cli_path); });
    runner.Add("CliSmoke.InvalidArgumentsShowUsage", [&]() { TestInvalidArgumentsShowUsage(cli_path); });
    runner.Add("CliSmoke.DecodeUsesEmbeddedMetadata", [&]() { TestDecodeUsesEmbeddedMetadata(cli_path); });
    runner.Add("CliSmoke.DecodeRejectsTruncatedWav", [&]() { TestDecodeRejectsTruncatedWav(cli_path); });
    runner.Add(
        "CliSmoke.DecodeRejectsCanonicalWavWithoutFlipBitsMetadata",
        [&]() { TestDecodeRejectsCanonicalWavWithoutFlipBitsMetadata(cli_path); });
    return runner.Run();
}
