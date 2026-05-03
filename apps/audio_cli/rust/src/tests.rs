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
    let cli = Cli::try_parse_from(["FlipBits", "version"]).unwrap();
    assert!(matches!(cli.command, Command::Version));
}

#[test]
fn parses_licenses_command() {
    let cli = Cli::try_parse_from(["FlipBits", "licenses"]).unwrap();
    assert!(matches!(cli.command, Command::Licenses));
}

#[test]
fn parses_encode_command_with_text() {
    let cli = Cli::try_parse_from([
        "FlipBits",
        "encode",
        "--text",
        "hello",
        "--mode",
        "ultra",
        "--flash-style",
        "litany",
        "--out",
        "out.wav",
    ])
    .unwrap();

    let Command::Encode(args) = cli.command else {
        panic!("expected encode command");
    };
    assert_eq!(args.text.as_deref(), Some("hello"));
    assert_eq!(args.mode, TransportMode::Ultra);
    assert_eq!(args.flash_style, FlashStyle::Litany);
    assert_eq!(args.out, PathBuf::from("out.wav"));
}

#[test]
fn parses_new_flash_styles() {
    for (raw_style, expected_style) in [("zeal", FlashStyle::Zeal), ("void", FlashStyle::Void)] {
        let cli = Cli::try_parse_from([
            "FlipBits",
            "encode",
            "--text",
            "hello",
            "--mode",
            "flash",
            "--flash-style",
            raw_style,
            "--out",
            "out.wav",
        ])
        .unwrap();

        let Command::Encode(args) = cli.command else {
            panic!("expected encode command");
        };
        assert_eq!(args.flash_style, expected_style);
    }
}

#[test]
fn parses_decode_command() {
    let cli = Cli::try_parse_from(["FlipBits", "decode", "--in", "out.wav"]).unwrap();

    let Command::Decode(args) = cli.command else {
        panic!("expected decode command");
    };
    assert_eq!(args.input, PathBuf::from("out.wav"));
}

#[test]
fn rejects_encode_command_without_input() {
    let result = Cli::try_parse_from(["FlipBits", "encode", "--out", "out.wav"]);
    assert!(result.is_err());
}

#[test]
fn version_output_includes_core_version_line() {
    let output = version_output();
    assert!(output.contains("presentation: v0.2.1"));
    assert!(output.contains("core: v"));
}

#[test]
fn licenses_output_mentions_notice_scope() {
    let output = crate::licenses::licenses_output();
    assert!(output.contains("FlipBits third-party notices"));
    assert!(output.contains("runtime: 14"));
    assert!(output.contains("build: 6"));
    assert!(output.contains("test: 3"));
    assert!(output.contains("docs/legal/cli_third_party_notices.md"));
    assert!(output.contains("CLI-only scope"));
    assert!(output.contains("Android is not included"));
    assert!(output.contains("libsndfile is not included"));
}

#[test]
fn wav_bytes_start_with_riff_header() {
    let mut config = bag_api::CodecConfig::for_mode(TransportMode::Ultra);
    config.flash_style = FlashStyle::Litany;
    let pcm_samples = bag_api::encode_text_with_progress(&config, "FlipBits WAV", |_| {}).unwrap();
    let metadata = audio_io_api::FlipBitsMetadata {
        version: 6,
        mode: TransportMode::Ultra,
        flash_voicing_style: None,
        created_at_iso_utc: "1970-01-01T00:00:00Z".to_string(),
        duration_ms: 0,
        sample_rate_hz: config.sample_rate_hz,
        frame_samples: config.frame_samples,
        pcm_sample_count: pcm_samples.len(),
        payload_byte_count: 0,
        app_version: "FlipBits/test".to_string(),
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
fn wav_metadata_roundtrips_new_flash_styles() {
    for style in [FlashStyle::Zeal, FlashStyle::Void] {
        let metadata = audio_io_api::FlipBitsMetadata {
            version: 6,
            mode: TransportMode::Flash,
            flash_voicing_style: Some(style),
            created_at_iso_utc: "1970-01-01T00:00:00Z".to_string(),
            duration_ms: 0,
            sample_rate_hz: DEFAULT_SAMPLE_RATE_HZ,
            frame_samples: DEFAULT_SAMPLE_RATE_HZ / DEFAULT_FRAME_RATE_DIVISOR,
            pcm_sample_count: 4,
            payload_byte_count: 1,
            app_version: "FlipBits/test".to_string(),
            core_version: "test-core".to_string(),
        };
        let wav_bytes = audio_io_api::encode_mono_pcm16_wav_with_metadata(
            DEFAULT_SAMPLE_RATE_HZ,
            &[0, 1, -1, 0],
            &metadata,
        )
        .unwrap();
        let decoded = audio_io_api::decode_mono_pcm16_wav(&wav_bytes).unwrap();

        assert_eq!(decoded.metadata.unwrap().flash_voicing_style, Some(style));
    }
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
    assert!(help.contains("FlipBits command line interface"));
    assert!(help.contains("licenses"));
    assert!(help.contains("encode"));
    assert!(help.contains("Encode text into a FlipBits WAV file"));
    assert!(help.contains("Decode a FlipBits WAV file back into text"));
    assert!(help.contains("Use a subcommand and then `--help` on that subcommand"));
    assert!(!help.contains("requires an explicit --mode"));
    assert!(!help.contains("--flash-style"));
}

#[test]
fn encode_help_mentions_flash_style() {
    let mut command = Cli::command();
    let help = command
        .find_subcommand_mut("encode")
        .expect("encode subcommand")
        .render_long_help()
        .to_string();
    assert!(help.contains("--flash-style"));
    assert!(help.contains("flash  Flexible byte-oriented mode with FSK-like signaling. At the protocol level it is not restricted to ASCII."));
    assert!(help.contains("pro    Telephone-tone-style high/low audio mode. ASCII-only input."));
    assert!(help.contains("ultra  UTF-8 text mode for broader character coverage."));
    assert!(help.contains("mini   Morse-code audio mode. Morse-compatible ASCII input."));
    assert!(help.contains("CLI input boundary:"));
    assert!(help.contains("The current CLI accepts UTF-8 text only:"));
    assert!(help.contains("--text is parsed as a Rust UTF-8 string."));
    assert!(help.contains("--text-file is read as a UTF-8 text file."));
    assert!(help.contains("steady"));
    assert!(help.contains("hostile"));
    assert!(help.contains("litany"));
    assert!(help.contains("collapse"));
    assert!(help.contains("zeal"));
    assert!(help.contains("void"));
}
