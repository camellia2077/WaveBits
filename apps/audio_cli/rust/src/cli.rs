use crate::CLI_VERSION;
use clap::{CommandFactory, Parser, Subcommand, ValueEnum};
use std::fmt::{Display, Formatter};
use std::path::PathBuf;

#[derive(Debug, Parser)]
#[command(name = "binary_audio_cpp")]
#[command(
    about = "WaveBits command line interface",
    long_about = "WaveBits command line interface for text-to-audio and audio-to-text roundtrips.\n\nUse a subcommand to inspect the build, encode text into a WAV artifact, or decode a WaveBits WAV artifact back into text.",
    after_help = "Examples:\n  binary_audio_cpp version\n  binary_audio_cpp encode --text \"hello\" --mode flash --out out.wav\n  binary_audio_cpp decode --in out.wav",
    version = CLI_VERSION
)]
pub struct Cli {
    #[command(subcommand)]
    pub command: Command,
}

#[derive(Debug, Subcommand)]
pub enum Command {
    #[command(about = "Show build and core version information")]
    Version,
    #[command(
        about = "Encode text into a WaveBits WAV file",
        long_about = "Encode text into a mono PCM16 WAV file with embedded WaveBits metadata.",
        after_help = "Examples:\n  binary_audio_cpp encode --text \"hello\" --out out.wav\n  binary_audio_cpp encode --text-file input.txt --mode ultra --out out.wav"
    )]
    Encode(EncodeArgs),
    #[command(
        about = "Decode a WaveBits WAV file back into text",
        long_about = "Decode a mono PCM16 WAV file back into text. The CLI reads WaveBits metadata from the WAV artifact and restores the transport mode automatically.",
        after_help = "Examples:\n  binary_audio_cpp decode --in out.wav\n  binary_audio_cpp decode --in out.wav --out-text decoded.txt"
    )]
    Decode(DecodeArgs),
}

#[derive(Clone, Copy, Debug, Eq, PartialEq, ValueEnum)]
pub enum TransportMode {
    Flash,
    Pro,
    Ultra,
}

impl TransportMode {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::Flash => "flash",
            Self::Pro => "pro",
            Self::Ultra => "ultra",
        }
    }
}

impl Display for TransportMode {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.as_str())
    }
}

#[derive(Debug, clap::Args)]
pub struct EncodeArgs {
    #[arg(long, conflicts_with = "text_file", required_unless_present = "text_file", help = "Inline text to encode")]
    pub text: Option<String>,
    #[arg(long, conflicts_with = "text", required_unless_present = "text", help = "Path to a UTF-8 text file to encode")]
    pub text_file: Option<PathBuf>,
    #[arg(long, value_enum, default_value_t = TransportMode::Flash, help = "WaveBits transport mode to use during encoding")]
    pub mode: TransportMode,
    #[arg(long, help = "Output WAV artifact path")]
    pub out: PathBuf,
}

#[derive(Debug, clap::Args)]
pub struct DecodeArgs {
    #[arg(long = "in", help = "Input WaveBits WAV artifact path")]
    pub input: PathBuf,
    #[arg(long, help = "Optional path to write decoded text instead of only printing it")]
    pub out_text: Option<PathBuf>,
}

pub fn clap_debug_assert() {
    Cli::command().debug_assert();
}
