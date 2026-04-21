from __future__ import annotations

import argparse
import sys
from typing import Sequence

from ..errors import ToolError
from ..paths import ensure_root
from .common import RAW_FORMATTER
from .groups.android import register_android_group
from .groups.artifact import register_artifact_group
from .groups.build_host import register_build_host_commands
from .groups.cli import register_cli_group
from .groups.clang import register_clang_group
from .groups.file_name import register_file_name_group
from .groups.history import register_history_group
from .groups.message import register_message_group
from .groups.verify import register_verify_command
from .groups.windows import register_windows_group


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="run.py",
        description=(
            "FlipBits developer orchestration tools.\n\n"
            "Primary command groups:\n"
            "- Build / host: `configure`, `build`, `clean`, `test`, `test-lib`, `verify`\n"
            "- Rust CLI: `cli`\n"
            "- Windows delivery: `windows`\n"
            "- Native quality: `clang`\n"
            "- Android delivery: `android`\n"
            "- Naming assistance: `file-name`\n"
            "- Release history: `history`\n"
            "- Git message drafts: `message`\n"
            "- Artifacts / delivery: `artifact`\n\n"
            "Use the top-level help to discover main commands, then drill into a command group with:\n"
            "- `python tools/run.py <command> --help`\n"
            "- `python tools/run.py clang <subcommand> --help`\n"
            "- `python tools/run.py file-name <subcommand> --help`\n"
            "- `python tools/run.py history <subcommand> --help`\n"
            "- `python tools/run.py message <subcommand> --help`\n"
            "- `python tools/run.py artifact <subcommand> --help`"
        ),
        formatter_class=RAW_FORMATTER,
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    register_build_host_commands(subparsers)
    register_cli_group(subparsers)
    register_windows_group(subparsers)
    register_clang_group(subparsers)
    register_android_group(subparsers)
    register_artifact_group(subparsers)
    register_file_name_group(subparsers)
    register_history_group(subparsers)
    register_message_group(subparsers)
    register_verify_command(subparsers)

    return parser


def main(argv: Sequence[str] | None = None) -> int:
    ensure_root()
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        args.func(args)
    except ToolError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1
    return 0
