use crate::CLI_PRESENTATION_VERSION;
use clap::{CommandFactory, Parser, Subcommand, ValueEnum};
use std::fmt::{Display, Formatter};
use std::path::PathBuf;

#[derive(Debug, Parser)]
#[command(name = "FlipBits")]
#[command(
    about = "FlipBits command line interface",
    long_about = "FlipBits command line interface for text-to-audio and audio-to-text roundtrips.\n\nUse a subcommand and then `--help` on that subcommand for focused details.",
    version = CLI_PRESENTATION_VERSION
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
        about = "Show third-party license notice coverage for the CLI",
        long_about = "Show the first-pass third-party notice summary for the FlipBits CLI and point to the stable notices document in the repository."
    )]
    Licenses,
    #[command(
        about = "Encode text into a FlipBits WAV file",
        long_about = "Encode text into a mono PCM16 WAV file with embedded FlipBits metadata.",
        after_help = "Mode guide:\n  mini   Morse-code audio mode. Morse-compatible ASCII input.\n  flash  Flexible byte-oriented mode with FSK-like signaling. At the protocol level it is not restricted to ASCII.\n  pro    Telephone-tone-style high/low audio mode. ASCII-only input.\n  ultra  UTF-8 text mode for broader character coverage.\n\nCLI input boundary:\n  The current CLI accepts UTF-8 text only:\n  - --text is parsed as a Rust UTF-8 string.\n  - --text-file is read as a UTF-8 text file.\n  This means flash may be more permissive at the protocol level, but this CLI still accepts text through UTF-8 input only.\n\nFlash style guide:\n  steady    Restrained everyday machine speech.\n  hostile   Aggressive command tone.\n  litany    Slow ceremonial chant.\n  collapse  Fearful broken delivery.\n\nExamples:\n  FlipBits encode --text \"PRAISE THE OMNISSIAH\" --mode mini --out out.wav\n  FlipBits encode --text \"hello\" --out out.wav\n  FlipBits encode --text \"hello\" --mode flash --flash-style litany --out out.wav\n  FlipBits encode --text \"ASCII ONLY\" --mode pro --out out.wav\n  FlipBits encode --text-file input.txt --mode ultra --out out.wav"
    )]
    Encode(EncodeArgs),
    #[command(
        about = "Decode a FlipBits WAV file back into text",
        long_about = "Decode a mono PCM16 WAV file back into text. The CLI reads FlipBits metadata from the WAV artifact and restores the transport mode automatically.",
        after_help = "Examples:\n  FlipBits decode --in out.wav\n  FlipBits decode --in out.wav --out-text decoded.txt"
    )]
    Decode(DecodeArgs),
}

#[derive(Clone, Copy, Debug, Eq, PartialEq, ValueEnum)]
pub enum TransportMode {
    Mini,
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
            Self::Mini => "mini",
        }
    }
}

impl Display for TransportMode {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.as_str())
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq, ValueEnum)]
#[value(rename_all = "snake_case")]
pub enum FlashStyle {
    Steady,
    Hostile,
    Litany,
    Collapse,
}

impl FlashStyle {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::Steady => "steady",
            Self::Hostile => "hostile",
            Self::Litany => "litany",
            Self::Collapse => "collapse",
        }
    }
}

impl Display for FlashStyle {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.as_str())
    }
}

#[derive(Debug, clap::Args)]
pub struct EncodeArgs {
    #[arg(
        long,
        conflicts_with = "text_file",
        required_unless_present = "text_file",
        help = "Inline text to encode"
    )]
    pub text: Option<String>,
    #[arg(
        long,
        conflicts_with = "text",
        required_unless_present = "text",
        help = "Path to a UTF-8 text file to encode"
    )]
    pub text_file: Option<PathBuf>,
    #[arg(long, value_enum, default_value_t = TransportMode::Flash, help = "FlipBits transport mode to use during encoding")]
    pub mode: TransportMode,
    #[arg(
        long,
        value_enum,
        default_value_t = FlashStyle::Steady,
        help = "Flash style for flash mode: steady, hostile, litany, or collapse"
    )]
    pub flash_style: FlashStyle,
    #[arg(long, help = "Output WAV artifact path")]
    pub out: PathBuf,
}

#[derive(Debug, clap::Args)]
pub struct DecodeArgs {
    #[arg(long = "in", help = "Input FlipBits WAV artifact path")]
    pub input: PathBuf,
    #[arg(
        long,
        help = "Optional path to write decoded text instead of only printing it"
    )]
    pub out_text: Option<PathBuf>,
}

pub fn clap_debug_assert() {
    Cli::command().debug_assert();
}
