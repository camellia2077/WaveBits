use assert_cmd::Command;
use predicates::prelude::*;
use std::fs;
use tempfile::tempdir;

use flipbits::CLI_PRESENTATION_VERSION;

#[test]
fn version_command_reports_rust_wav_build() {
    let mut command = Command::cargo_bin("FlipBits").unwrap();
    command
        .arg("version")
        .assert()
        .success()
        .stdout(predicate::str::contains(format!(
            "presentation: v{CLI_PRESENTATION_VERSION}"
        )))
        .stdout(predicate::str::contains("core: v"))
        .stdout(predicate::str::contains("build: rust-wav"))
        .stdout(predicate::str::contains("bag_api + audio_io WAV build"));
}

#[test]
fn licenses_command_reports_notice_summary() {
    let mut command = Command::cargo_bin("FlipBits").unwrap();
    command
        .arg("licenses")
        .assert()
        .success()
        .stdout(predicate::str::contains("FlipBits third-party notices"))
        .stdout(predicate::str::contains("runtime: 14"))
        .stdout(predicate::str::contains("build: 6"))
        .stdout(predicate::str::contains("test: 3"))
        .stdout(predicate::str::contains(
            "docs/legal/cli_third_party_notices.md",
        ))
        .stdout(predicate::str::contains("CLI-only scope"))
        .stdout(predicate::str::contains("Android is not included"))
        .stdout(predicate::str::contains("libsndfile is not included"))
        .stdout(predicate::str::contains("full notices"));
}

#[test]
fn encode_writes_wav_file() {
    let temp = tempdir().unwrap();
    let output_path = temp.path().join("nested").join("artifact.wav");

    let mut command = Command::cargo_bin("FlipBits").unwrap();
    command
        .args([
            "encode",
            "--text",
            "FlipBits stub",
            "--mode",
            "flash",
            "--flash-style",
            "litany",
            "--out",
            output_path.to_str().unwrap(),
        ])
        .assert()
        .success()
        .stdout(predicate::str::contains("Output WAV:"))
        .stdout(predicate::str::contains("Format: mono PCM16 WAV"))
        .stdout(predicate::str::contains("Mode: flash"));

    let artifact = fs::read(output_path).unwrap();
    assert!(artifact.starts_with(b"RIFF"));
    assert_eq!(&artifact[8..12], b"WAVE");
}

#[test]
fn decode_reads_wav_and_writes_text_file() {
    let temp = tempdir().unwrap();
    let artifact_path = temp.path().join("artifact.wav");
    let output_text_path = temp.path().join("decoded.txt");

    let mut encode = Command::cargo_bin("FlipBits").unwrap();
    encode
        .args([
            "encode",
            "--text",
            "Decode me",
            "--mode",
            "flash",
            "--out",
            artifact_path.to_str().unwrap(),
        ])
        .assert()
        .success();

    let mut decode = Command::cargo_bin("FlipBits").unwrap();
    decode
        .args([
            "decode",
            "--in",
            artifact_path.to_str().unwrap(),
            "--out-text",
            output_text_path.to_str().unwrap(),
        ])
        .assert()
        .success()
        .stdout(predicate::str::contains("Decode me"));

    assert_eq!(fs::read_to_string(output_text_path).unwrap(), "Decode me");
}

#[test]
fn decode_missing_input_fails_with_stable_error() {
    let temp = tempdir().unwrap();
    let missing_path = temp.path().join("missing.wav");

    let mut command = Command::cargo_bin("FlipBits").unwrap();
    command
        .args(["decode", "--in", missing_path.to_str().unwrap()])
        .assert()
        .failure()
        .stderr(predicate::str::contains("failed to read input artifact"));
}

#[test]
fn encode_decode_wav_roundtrip_is_happy_path() {
    let temp = tempdir().unwrap();
    let input_text_path = temp.path().join("input.txt");
    let artifact_path = temp.path().join("artifact.wav");
    fs::write(&input_text_path, "Roundtrip text 你好").unwrap();

    let mut encode = Command::cargo_bin("FlipBits").unwrap();
    encode
        .args([
            "encode",
            "--text-file",
            input_text_path.to_str().unwrap(),
            "--mode",
            "ultra",
            "--out",
            artifact_path.to_str().unwrap(),
        ])
        .assert()
        .success();

    let mut decode = Command::cargo_bin("FlipBits").unwrap();
    decode
        .args(["decode", "--in", artifact_path.to_str().unwrap()])
        .assert()
        .success()
        .stdout(predicate::str::contains("Roundtrip text 你好"));
}

#[test]
fn decode_without_mode_uses_wav_metadata() {
    let temp = tempdir().unwrap();
    let artifact_path = temp.path().join("artifact.wav");

    let mut encode = Command::cargo_bin("FlipBits").unwrap();
    encode
        .args([
            "encode",
            "--text",
            "flash only",
            "--mode",
            "flash",
            "--out",
            artifact_path.to_str().unwrap(),
        ])
        .assert()
        .success();

    let mut decode = Command::cargo_bin("FlipBits").unwrap();
    decode
        .args(["decode", "--in", artifact_path.to_str().unwrap()])
        .assert()
        .success()
        .stdout(predicate::str::contains("flash only"));
}

#[test]
fn decode_truncated_wav_fails_with_parse_error() {
    let temp = tempdir().unwrap();
    let artifact_path = temp.path().join("artifact.wav");

    let mut encode = Command::cargo_bin("FlipBits").unwrap();
    encode
        .args([
            "encode",
            "--text",
            "truncate me",
            "--mode",
            "pro",
            "--out",
            artifact_path.to_str().unwrap(),
        ])
        .assert()
        .success();

    let mut bytes = fs::read(&artifact_path).unwrap();
    bytes.truncate(bytes.len().saturating_sub(1));
    fs::write(&artifact_path, bytes).unwrap();

    let mut decode = Command::cargo_bin("FlipBits").unwrap();
    decode
        .args(["decode", "--in", artifact_path.to_str().unwrap()])
        .assert()
        .failure()
        .stderr(predicate::str::contains("failed to parse WAV input"))
        .stderr(predicate::str::contains("Truncated WAV data"));
}

#[test]
fn help_describes_decode_without_mode() {
    let mut command = Command::cargo_bin("FlipBits").unwrap();
    command
        .arg("--help")
        .assert()
        .success()
        .stdout(predicate::str::contains("FlipBits command line interface"))
        .stdout(predicate::str::contains(
            "Decode a FlipBits WAV file back into text",
        ))
        .stdout(predicate::str::contains(
            "licenses  Show third-party license notice coverage for the CLI",
        ))
        .stdout(predicate::str::contains("encode"))
        .stdout(predicate::str::contains("decode"))
        .stdout(predicate::str::contains(
            "Use a subcommand and then `--help` on that subcommand",
        ))
        .stdout(predicate::str::contains("--flash-style").not());
}

#[test]
fn encode_help_mentions_android_aligned_flash_styles() {
    let mut command = Command::cargo_bin("FlipBits").unwrap();
    command
        .args(["encode", "--help"])
        .assert()
        .success()
        .stdout(predicate::str::contains("--flash-style"))
        .stdout(predicate::str::contains("steady"))
        .stdout(predicate::str::contains("hostile"))
        .stdout(predicate::str::contains("litany"))
        .stdout(predicate::str::contains("collapse"))
        .stdout(predicate::str::contains("zeal"))
        .stdout(predicate::str::contains("void"));
}
