from __future__ import annotations

import argparse
import os
import re
import shutil
from pathlib import Path

from ..constants import CLI_RUST_DIR, CLI_TARGET_NAME, RUST_CLI_TARGET_TRIPLE
from ..errors import ToolError
from ..paths import resolve_build_dir
from ..process import run

_SEMVER_RE = re.compile(r"^\d+\.\d+\.\d+$")
_CARGO_PACKAGE_VERSION_RE = re.compile(
    r"(?ms)^(\[package\]\s+.*?^version\s*=\s*)\"[^\"]+\""
)


def cmd_cli(args: argparse.Namespace) -> None:
    if args.action == "bump-version":
        if args.version is None:
            raise ToolError("`cli bump-version` requires a version, for example 0.2.5.")
        bump_cli_version(args.version)
        return
    if args.version is not None:
        raise ToolError("CLI version argument is only valid for `cli bump-version`.")

    build_dir = resolve_build_dir(args.build_dir)
    cargo_env = os.environ.copy()
    base_env = getattr(args, "env", None)
    if base_env:
        cargo_env.update(base_env)
    cargo_env["FLIPBITS_CMAKE_BUILD_DIR"] = str(build_dir)
    cargo_env["CARGO_TARGET_DIR"] = str(_cargo_target_dir(build_dir))

    command = ["cargo", args.action, "--target", RUST_CLI_TARGET_TRIPLE]
    if args.release:
        command.append("--release")

    run(command, cwd=CLI_RUST_DIR, env=cargo_env)

    if args.action == "build":
        cargo_artifact_path = _cargo_artifact_path(build_dir=build_dir, release=args.release)
        final_artifact_path = _final_artifact_path(build_dir)
        final_artifact_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(cargo_artifact_path, final_artifact_path)
        print(f"CLI artifact: {final_artifact_path}", flush=True)


def bump_cli_version(version: str) -> None:
    if _SEMVER_RE.match(version) is None:
        raise ToolError("CLI version must use MAJOR.MINOR.PATCH, for example 0.2.5.")

    cargo_toml_path = CLI_RUST_DIR / "Cargo.toml"
    cargo_toml = cargo_toml_path.read_text(encoding="utf-8")
    updated, replacements = _CARGO_PACKAGE_VERSION_RE.subn(
        rf'\g<1>"{version}"',
        cargo_toml,
        count=1,
    )
    if replacements != 1:
        raise ToolError(f"Could not find [package] version in {cargo_toml_path}.")

    cargo_toml_path.write_text(updated, encoding="utf-8", newline="")
    run(["cargo", "update", "-p", "flipbits"], cwd=CLI_RUST_DIR)
    print(f"Updated CLI package version to {version} and refreshed Cargo.lock.", flush=True)


def _cargo_target_dir(build_dir: Path) -> Path:
    return build_dir / "rust-cli" / "target"


def _cargo_artifact_path(*, build_dir: Path, release: bool) -> Path:
    profile = "release" if release else "debug"
    suffix = ".exe" if os.name == "nt" else ""
    return _cargo_target_dir(build_dir) / RUST_CLI_TARGET_TRIPLE / profile / f"{CLI_TARGET_NAME}{suffix}"


def _final_artifact_path(build_dir: Path) -> Path:
    suffix = ".exe" if os.name == "nt" else ""
    return build_dir / "bin" / f"{CLI_TARGET_NAME}{suffix}"
