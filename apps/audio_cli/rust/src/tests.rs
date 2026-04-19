use crate::audio_io_api;
use crate::bag_api;
use crate::*;
use clap::{CommandFactory, Parser};
use std::path::PathBuf;

#[test]
fn clap_configuration_is_valid() {
    clap_debug_assert();
}

#[test]
fn parses_version_command() {
    let cli = Cli::try_parse_from(["binary_audio_cpp", "version"]).unwrap();
    assert!(matches!(cli.command, Command::Version));
}

#[test]
fn parses_encode_command_with_text() {
    let cli = Cli::try_parse_from([
        "binary_audio_cpp",
        "encode",
        "--text",
        "hello",
        "--mode",
        "ultra",
        "--out",
        "out.wav",
    ])
    .unwrap();

    let Command::Encode(args) = cli.command else {
        panic!("expected encode command");
    };
    assert_eq!(args.text.as_deref(), Some("hello"));
    assert_eq!(args.mode, TransportMode::Ultra);
    assert_eq!(args.out, PathBuf::from("out.wav"));
}

#[test]
fn parses_decode_command() {
    let cli = Cli::try_parse_from(["binary_audio_cpp", "decode", "--in", "out.wav"]).unwrap();

    let Command::Decode(args) = cli.command else {
        panic!("expected decode command");
    };
    assert_eq!(args.input, PathBuf::from("out.wav"));
}

#[test]
fn rejects_encode_command_without_input() {
    let result = Cli::try_parse_from(["binary_audio_cpp", "encode", "--out", "out.wav"]);
    assert!(result.is_err());
}

#[test]
fn version_output_includes_core_version_line() {
    let output = version_output();
    assert!(output.contains("core: v"));
}

#[test]
fn wav_bytes_start_with_riff_header() {
    let config = bag_api::CodecConfig::for_mode(TransportMode::Ultra);
    let pcm_samples = bag_api::encode_text(&config, "WaveBits WAV").unwrap();
    let metadata = audio_io_api::WaveBitsMetadata {
        version: 3,
        mode: TransportMode::Ultra,
        flash_voicing_style: None,
        created_at_iso_utc: "1970-01-01T00:00:00Z".to_string(),
        duration_ms: 0,
        frame_samples: config.frame_samples,
        pcm_sample_count: pcm_samples.len(),
        app_version: "binary_audio_cpp/test".to_string(),
        core_version: "test-core".to_string(),
    };
    let wav_bytes = audio_io_api::encode_mono_pcm16_wav_with_metadata(
        config.sample_rate_hz,
        &pcm_samples,
        &metadata,
    )
    .unwrap();
    assert!(wav_bytes.starts_with(b"RIFF"));
    assert_eq!(&wav_bytes[8..12], b"WAVE");
}

#[test]
fn decode_rejects_invalid_wav_header() {
    let error = audio_io_api::decode_mono_pcm16_wav(b"NOTWAVE").unwrap_err();
    assert!(format!("{error}").contains("Invalid WAV header"));
}

#[test]
fn free_empty_metadata_is_safe() {
    audio_io_api::free_empty_metadata_for_contract_test();
}

#[test]
fn help_mentions_decode_command() {
    let help = Cli::command().render_long_help().to_string();
    assert!(help.contains("WaveBits command line interface"));
    assert!(help.contains("encode   Encode text into a WaveBits WAV file"));
    assert!(help.contains("Decode a WaveBits WAV file back into text"));
    assert!(help.contains("binary_audio_cpp decode --in out.wav"));
    assert!(!help.contains("requires an explicit --mode"));
}
