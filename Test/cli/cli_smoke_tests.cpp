#include <filesystem>
#include <string>
#include <vector>

#include "test_framework.h"
#include "test_fs.h"
#include "test_process.h"
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
        "presentation: v0.1.1",
        "`version` output should contain the presentation version.");
    test::AssertContains(
        version_result.output,
        "core: v0.1.1",
        "`version` output should contain the core version.");

    const auto dash_version_result = RunCli(cli_path, {"--version"}, dir);
    test::AssertEq(dash_version_result.exit_code, 0, "`--version` should exit successfully.");
    test::AssertContains(
        dash_version_result.output,
        "presentation: v0.1.1",
        "`--version` output should contain the presentation version.");
    test::AssertContains(
        dash_version_result.output,
        "core: v0.1.1",
        "`--version` output should contain the core version.");
}

void TestFlashEncodeDecodeDirectText(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto wav_path = dir / "flash.wav";

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "flash", "--text", "Smoke-CLI", "--out", wav_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Flash CLI encode should succeed.");
    test::AssertTrue(std::filesystem::exists(wav_path), "Flash encoded WAV file should exist.");
    test::AssertTrue(std::filesystem::file_size(wav_path) > 0, "Flash encoded WAV file should be non-empty.");

    const auto decode_result =
        RunCli(cli_path, {"decode", "--mode", "flash", "--in", wav_path.string()}, dir);
    test::AssertEq(decode_result.exit_code, 0, "Flash CLI decode should succeed.");
    test::AssertContains(
        decode_result.output,
        "Smoke-CLI",
        "Flash CLI decode output should contain the original text.");
}

void TestProEncodeDecodeDirectText(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto wav_path = dir / "pro.wav";

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "pro", "--text", "ASCII-123", "--out", wav_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Pro CLI encode should succeed for ASCII input.");
    test::AssertTrue(std::filesystem::exists(wav_path), "Pro encoded WAV file should exist.");

    const auto decode_result =
        RunCli(cli_path, {"decode", "--mode", "pro", "--in", wav_path.string()}, dir);
    test::AssertEq(decode_result.exit_code, 0, "Pro CLI decode should succeed.");
    test::AssertContains(
        decode_result.output,
        "ASCII-123",
        "Pro CLI decode output should contain the original text.");
}

void TestUltraEncodeTextFileAndDecodeToTextFile(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto input_path = dir / "input.txt";
    const auto wav_path = dir / "ultra.wav";
    const auto output_text_path = dir / "decoded.txt";
    const std::string input = u8"WaveBits 超级模式 🚀";

    test::WriteTextFile(input_path, input);

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "ultra", "--text-file", input_path.string(), "--out", wav_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Ultra CLI encode from text file should succeed.");
    test::AssertTrue(std::filesystem::exists(wav_path), "Ultra CLI should create WAV output.");

    const auto decode_result = RunCli(
        cli_path,
        {"decode", "--mode", "ultra", "--in", wav_path.string(), "--out-text", output_text_path.string()},
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

void TestProMaxSingleFrameTextFileRoundTrip(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto input_path = dir / "pro_max.txt";
    const auto wav_path = dir / "pro_max.wav";
    const auto output_text_path = dir / "pro_max_decoded.txt";

    test::WriteTextFile(input_path, test::BuildMaxProCorpus());

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "pro", "--text-file", input_path.string(), "--out", wav_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Pro CLI should accept the max single-frame ASCII corpus.");

    const auto decode_result = RunCli(
        cli_path,
        {"decode", "--mode", "pro", "--in", wav_path.string(), "--out-text", output_text_path.string()},
        dir);
    test::AssertEq(decode_result.exit_code, 0, "Pro CLI should decode the max single-frame ASCII corpus.");
    test::AssertEq(
        test::ReadTextFile(output_text_path),
        test::BuildMaxProCorpus(),
        "Pro CLI should preserve the max single-frame ASCII corpus.");
}

void TestUltraMaxUtf8TextFileRoundTrip(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto input_path = dir / "ultra_max.txt";
    const auto wav_path = dir / "ultra_max.wav";
    const auto output_text_path = dir / "ultra_max_decoded.txt";

    test::WriteTextFile(input_path, test::BuildMaxUltraCorpus());

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--mode", "ultra", "--text-file", input_path.string(), "--out", wav_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "Ultra CLI should accept the max 512-byte UTF-8 corpus.");

    const auto decode_result = RunCli(
        cli_path,
        {"decode", "--mode", "ultra", "--in", wav_path.string(), "--out-text", output_text_path.string()},
        dir);
    test::AssertEq(decode_result.exit_code, 0, "Ultra CLI should decode the max 512-byte UTF-8 corpus.");
    test::AssertEq(
        test::ReadTextFile(output_text_path),
        test::BuildMaxUltraCorpus(),
        "Ultra CLI should preserve the max 512-byte UTF-8 corpus.");
}

void TestModeSpecificValidation(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto non_ascii_path = dir / "non_ascii.txt";
    const auto pro_too_long_path = dir / "pro_too_long.txt";
    const auto ultra_too_long_path = dir / "ultra_too_long.txt";

    test::WriteTextFile(non_ascii_path, u8"中文");
    test::WriteTextFile(pro_too_long_path, test::BuildTooLongProCorpus());
    test::WriteTextFile(ultra_too_long_path, test::BuildTooLongUltraCorpus());

    const auto non_ascii_result = RunCli(
        cli_path,
        {"encode", "--mode", "pro", "--text-file", non_ascii_path.string(), "--out", (dir / "bad.wav").string()},
        dir);
    test::AssertTrue(non_ascii_result.exit_code != 0, "Pro CLI should reject non-ASCII input.");
    test::AssertContains(
        non_ascii_result.output,
        "ASCII",
        "Pro CLI should explain the ASCII-only restriction.");

    const auto pro_too_long_result = RunCli(
        cli_path,
        {"encode", "--mode", "pro", "--text-file", pro_too_long_path.string(), "--out", (dir / "pro_long.wav").string()},
        dir);
    test::AssertTrue(pro_too_long_result.exit_code != 0, "Pro CLI should reject oversized payloads.");
    test::AssertContains(
        pro_too_long_result.output,
        "512-byte",
        "Pro CLI should mention the single-frame limit.");

    const auto ultra_too_long_result = RunCli(
        cli_path,
        {"encode", "--mode", "ultra", "--text-file", ultra_too_long_path.string(), "--out", (dir / "ultra_long.wav").string()},
        dir);
    test::AssertTrue(ultra_too_long_result.exit_code != 0, "Ultra CLI should reject oversized payloads.");
    test::AssertContains(
        ultra_too_long_result.output,
        "512-byte",
        "Ultra CLI should mention the single-frame limit.");
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
        "Unsupported transport mode",
        "Invalid mode should print a clear error.");

    const auto invalid_result = RunCli(cli_path, {"encode", "--unknown", "value"}, dir);
    test::AssertTrue(invalid_result.exit_code != 0, "Invalid CLI arguments should fail.");
    test::AssertContains(invalid_result.output, "Usage:", "Invalid CLI arguments should print usage.");
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
        "CliSmoke.ProMaxSingleFrameTextFileRoundTrip",
        [&]() { TestProMaxSingleFrameTextFileRoundTrip(cli_path); });
    runner.Add(
        "CliSmoke.UltraMaxUtf8TextFileRoundTrip",
        [&]() { TestUltraMaxUtf8TextFileRoundTrip(cli_path); });
    runner.Add("CliSmoke.ModeSpecificValidation", [&]() { TestModeSpecificValidation(cli_path); });
    runner.Add("CliSmoke.InvalidArgumentsShowUsage", [&]() { TestInvalidArgumentsShowUsage(cli_path); });
    return runner.Run();
}
