use std::fmt::{Display, Formatter};
use std::path::PathBuf;

#[derive(Debug)]
pub enum CliError {
    Io {
        context: &'static str,
        path: PathBuf,
        source: std::io::Error,
    },
    InvalidArtifact(String),
    Api(String),
}

impl Display for CliError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Io {
                context,
                path,
                source,
            } => write!(f, "{context}: {} ({source})", path.display()),
            Self::InvalidArtifact(message) => f.write_str(message),
            Self::Api(message) => f.write_str(message),
        }
    }
}

impl std::error::Error for CliError {}
