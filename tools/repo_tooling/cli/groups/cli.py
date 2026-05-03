from __future__ import annotations

import argparse

from ...commands import cmd_cli
from ..common import RAW_FORMATTER, add_common_build_dir_argument


def register_cli_group(
    subparsers: argparse._SubParsersAction[argparse.ArgumentParser],
) -> None:
    cli_parser = subparsers.add_parser(
        "cli",
        help="Build or test the Rust CLI from the repo root.",
        description=(
            "Run Rust CLI Cargo workflows from the repo root.\n\n"
            "Behavior:\n"
            "- Always runs in apps/audio_cli/rust.\n"
            "- Automatically sets FLIPBITS_CMAKE_BUILD_DIR for the Rust FFI/link bridge.\n"
            "- Automatically sets CARGO_TARGET_DIR under build/<dir>/rust-cli/target.\n"
            "- Copies the final CLI binary to build/<dir>/bin/.\n"
            "- Always uses the repository-supported target triple x86_64-pc-windows-gnu.\n"
            "- On Windows, uses the GNU-host Rust toolchain stable-x86_64-pc-windows-gnu.\n"
            "- `build` maps to `cargo build`.\n"
            "- `test` maps to `cargo test`.\n\n"
            "Version workflow:\n"
            "- `bump-version <version>` updates apps/audio_cli/rust/Cargo.toml.\n"
            "- Cargo.lock is refreshed by Cargo; do not edit Cargo.lock by hand.\n\n"
            "Examples:\n"
            "- python tools/run.py cli build\n"
            "- python tools/run.py cli build --release\n"
            "- python tools/run.py cli test\n"
            "- python tools/run.py cli test --release --build-dir build/dev\n"
            "- python tools/run.py cli bump-version 0.2.5"
        ),
        formatter_class=RAW_FORMATTER,
    )
    cli_parser.add_argument(
        "action",
        choices=["build", "test", "bump-version"],
        help="Cargo action or CLI maintenance action to run.",
    )
    cli_parser.add_argument(
        "version",
        nargs="?",
        help="New CLI package version for `bump-version`, for example 0.2.5.",
    )
    add_common_build_dir_argument(cli_parser)
    cli_parser.add_argument(
        "--release",
        action="store_true",
        help="Build or test the optimized release profile instead of debug.",
    )
    cli_parser.set_defaults(func=cmd_cli)
