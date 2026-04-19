from __future__ import annotations

import argparse

from ...commands import cmd_format, cmd_tidy
from ...constants import DEFAULT_GENERATOR
from ..common import RAW_FORMATTER, add_common_build_dir_argument


def register_clang_group(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    clang_parser = subparsers.add_parser(
        "clang",
        help="Run clang-based developer tools.",
        description=(
            "Run clang-format / clang-tidy workflows from the repository root.\n\n"
            "Examples:\n"
            "- python tools/run.py clang format --check\n"
            "- python tools/run.py clang tidy --build-dir build/dev"
        ),
        formatter_class=RAW_FORMATTER,
    )
    clang_subparsers = clang_parser.add_subparsers(dest="clang_command", required=True)

    clang_format_parser = clang_subparsers.add_parser(
        "format",
        help="Run clang-format on the native C/C++ source tree.",
        description=(
            "Run clang-format with the repo .clang-format.\n\n"
            "Behavior:\n"
            "- Uses clang-format --style=file from the repository root.\n"
            "- Defaults to the libs/ scope.\n"
            "- Supports check mode via --check.\n"
            "- Supports explicit file or directory selection via repeated --path."
        ),
        formatter_class=RAW_FORMATTER,
    )
    clang_format_parser.add_argument(
        "--scope",
        default="libs",
        choices=["libs", "host-native", "android-native", "all-native"],
        help="Predefined native source scope to format when --path is not provided.",
    )
    clang_format_parser.add_argument(
        "--path",
        action="append",
        help="Optional file or directory to format. Repeat to pass multiple paths.",
    )
    clang_format_parser.add_argument(
        "--check",
        action="store_true",
        help="Run clang-format in dry-run verification mode.",
    )
    clang_format_parser.set_defaults(func=cmd_format)

    tidy_parser = clang_subparsers.add_parser(
        "tidy",
        help="Run minimal clang-tidy for libs sources and split the log into grouped task artifacts.",
        description=(
            "Run the minimal WaveBits clang-tidy flow for libs sources.\n\n"
            "Behavior:\n"
            "- Ensures compile_commands.json exists in the selected build directory.\n"
            "- Runs clang-tidy over translation units under libs/.\n"
            "- Writes the raw merged log plus grouped tasks/patch_XXX logs.\n"
            "- Copies the repo .clang-tidy and .clang-format into the artifact root.\n\n"
            "Output layout:\n"
            "- build/reports/clang-tidy/libs/<build-dir>/clang-tidy.log\n"
            "- build/reports/clang-tidy/libs/<build-dir>/tasks/patch_001/001.log\n"
            "- build/reports/clang-tidy/libs/<build-dir>/tasks/patch_002/011.log"
        ),
        formatter_class=RAW_FORMATTER,
    )
    add_common_build_dir_argument(tidy_parser)
    tidy_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use if compile_commands.json must be regenerated.",
    )
    tidy_parser.add_argument(
        "--jobs",
        type=int,
        help="Number of parallel clang-tidy subprocesses to run. Defaults to 1.",
    )
    tidy_parser.add_argument(
        "--header-filter",
        help="Optional clang-tidy header filter regex. Defaults to the repository libs/ root only.",
    )
    tidy_parser.add_argument(
        "--from-log",
        help="Split an existing clang-tidy log instead of running clang-tidy again.",
    )
    tidy_parser.add_argument(
        "--out-dir",
        help="Optional artifact directory. Defaults to build/reports/clang-tidy/libs/<build-dir>/.",
    )
    tidy_parser.add_argument(
        "--limit",
        type=int,
        help="Optional limit on the number of libs translation units to run.",
    )
    tidy_parser.set_defaults(func=cmd_tidy)
