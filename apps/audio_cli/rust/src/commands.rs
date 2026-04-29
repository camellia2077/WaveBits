use crate::audio_io_api;
use crate::bag_api;
use crate::fs_io::{read_binary_file, resolve_encode_text, write_binary_file, write_text_file};
use crate::licenses;
use crate::progress::EncodeProgressBar;
use crate::{
    Cli, CliError, Command, DecodeArgs, EncodeArgs, FlashStyle, TransportMode,
    CLI_PRESENTATION_VERSION,
};

const CLI_METADATA_CREATED_AT: &str = "1970-01-01T00:00:00Z";

#[derive(Debug, Eq, PartialEq)]
pub enum RunOutput {
    Message(String),
    DecodedText(String),
}

pub fn run(cli: Cli) -> Result<RunOutput, CliError> {
    match cli.command {
        Command::Version => Ok(RunOutput::Message(version_output())),
        Command::Licenses => Ok(RunOutput::Message(licenses::licenses_output())),
        Command::Encode(args) => encode_command(args),
        Command::Decode(args) => decode_command(args),
    }
}

pub fn version_output() -> String {
    let core_version = bag_api::core_version().unwrap_or_else(|| "unknown".to_string());
    format!(
        "presentation: v{CLI_PRESENTATION_VERSION}\ncore: v{core_version}\nbuild: rust-wav\nstatus: bag_api + audio_io WAV build"
    )
}

fn encode_command(args: EncodeArgs) -> Result<RunOutput, CliError> {
    let text = resolve_encode_text(&args)?;
    let mut config = bag_api::CodecConfig::for_mode(args.mode);
    config.flash_style = flash_style_for_mode(args.mode, args.flash_style);
    let mut progress_bar = EncodeProgressBar::new();
    let pcm_samples = bag_api::encode_text_with_progress(&config, &text, |progress| {
        progress_bar.update(args.mode, progress);
    });
    progress_bar.finish();
    let pcm_samples = pcm_samples.map_err(|error| {
        CliError::Api(format!(
            "failed to encode `{}` text payload: {error}",
            args.mode
        ))
    })?;
    let metadata = build_cli_metadata(args.mode, &config, pcm_samples.len());
    let wav_bytes = audio_io_api::encode_mono_pcm16_wav_with_metadata(
        config.sample_rate_hz,
        &pcm_samples,
        &metadata,
    )
    .map_err(|error| {
        CliError::Api(format!(
            "failed to serialize `{}` PCM into WAV: {error}",
            args.mode
        ))
    })?;
    write_binary_file(&args.out, &wav_bytes, "failed to write WAV output")?;
    Ok(RunOutput::Message(format!(
        "Output WAV: {}\nMode: {}\nFormat: mono PCM16 WAV",
        args.out.display(),
        args.mode
    )))
}

fn decode_command(args: DecodeArgs) -> Result<RunOutput, CliError> {
    let wav_bytes = read_binary_file(&args.input)?;
    let decoded = audio_io_api::decode_mono_pcm16_wav(&wav_bytes)
        .map_err(|error| CliError::Api(format!("failed to parse WAV input: {error}")))?;
    let metadata = decoded.metadata.map_err(|error| {
        CliError::Api(format!(
            "failed to read FlipBits metadata from WAV input: {error}"
        ))
    })?;
    let config = bag_api::CodecConfig {
        sample_rate_hz: decoded.sample_rate_hz,
        frame_samples: metadata.frame_samples,
        mode: metadata.mode,
        flash_style: metadata.flash_voicing_style.unwrap_or(FlashStyle::Steady),
    };
    let text = bag_api::decode_pcm(&config, &decoded.pcm_samples).map_err(|error| {
        CliError::Api(format!(
            "failed to decode WAV payload in `{}` mode: {error}",
            metadata.mode
        ))
    })?;
    if let Some(out_text) = &args.out_text {
        write_text_file(out_text, &text)?;
    }
    Ok(RunOutput::DecodedText(text))
}

fn build_cli_metadata(
    mode: crate::TransportMode,
    config: &bag_api::CodecConfig,
    pcm_sample_count: usize,
) -> audio_io_api::FlipBitsMetadata {
    let duration_ms = ((pcm_sample_count as u64).saturating_mul(1000)
        / config.sample_rate_hz.max(1) as u64) as u32;
    audio_io_api::FlipBitsMetadata {
        version: 6,
        mode,
        flash_voicing_style: if mode == TransportMode::Flash {
            Some(config.flash_style)
        } else {
            None
        },
        created_at_iso_utc: CLI_METADATA_CREATED_AT.to_string(),
        duration_ms,
        sample_rate_hz: config.sample_rate_hz,
        frame_samples: config.frame_samples,
        pcm_sample_count,
        payload_byte_count: 0,
        app_version: format!("FlipBits/{CLI_PRESENTATION_VERSION}"),
        core_version: bag_api::core_version().unwrap_or_else(|| "unknown".to_string()),
    }
}

fn flash_style_for_mode(mode: TransportMode, requested_style: FlashStyle) -> FlashStyle {
    if mode == TransportMode::Flash {
        requested_style
    } else {
        FlashStyle::Steady
    }
}
