mod audio_io_api;
mod bag_api;
mod cli;
mod commands;
mod error;
mod fs_io;
mod util;

#[cfg(test)]
mod tests;

pub use cli::{clap_debug_assert, Cli, Command, DecodeArgs, EncodeArgs, TransportMode};
pub use commands::{run, version_output, RunOutput};
pub use error::CliError;

pub const CLI_VERSION: &str = env!("CARGO_PKG_VERSION");
pub(crate) const DEFAULT_SAMPLE_RATE_HZ: i32 = 44_100;
pub(crate) const DEFAULT_FRAME_RATE_DIVISOR: i32 = 20;
