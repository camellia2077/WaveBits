use crate::{CliError, EncodeArgs};
use std::fs;
use std::path::Path;

pub fn resolve_encode_text(args: &EncodeArgs) -> Result<String, CliError> {
    if let Some(text) = &args.text {
        return Ok(text.clone());
    }
    if let Some(path) = &args.text_file {
        return read_text_file(path);
    }
    Err(CliError::InvalidArtifact(
        "encode requires either --text or --text-file".to_string(),
    ))
}

pub fn read_binary_file(path: &Path) -> Result<Vec<u8>, CliError> {
    fs::read(path).map_err(|source| CliError::Io {
        context: "failed to read input artifact",
        path: path.to_path_buf(),
        source,
    })
}

pub fn write_binary_file(
    path: &Path,
    bytes: &[u8],
    context: &'static str,
) -> Result<(), CliError> {
    ensure_parent_dir(path)?;
    fs::write(path, bytes).map_err(|source| CliError::Io {
        context,
        path: path.to_path_buf(),
        source,
    })
}

pub fn write_text_file(path: &Path, text: &str) -> Result<(), CliError> {
    ensure_parent_dir(path)?;
    fs::write(path, text).map_err(|source| CliError::Io {
        context: "failed to write decoded text",
        path: path.to_path_buf(),
        source,
    })
}

fn read_text_file(path: &Path) -> Result<String, CliError> {
    fs::read_to_string(path).map_err(|source| CliError::Io {
        context: "failed to read text file",
        path: path.to_path_buf(),
        source,
    })
}

fn ensure_parent_dir(path: &Path) -> Result<(), CliError> {
    if let Some(parent) = path.parent() {
        if !parent.as_os_str().is_empty() {
            fs::create_dir_all(parent).map_err(|source| CliError::Io {
                context: "failed to create parent directory",
                path: parent.to_path_buf(),
                source,
            })?;
        }
    }
    Ok(())
}
