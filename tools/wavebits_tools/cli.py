from __future__ import annotations

import argparse
import sys
from typing import Sequence

from .commands import (
    cmd_android,
    cmd_build,
    cmd_configure,
    cmd_roundtrip,
    cmd_smoke,
    cmd_test,
    cmd_verify,
)
from .constants import DEFAULT_BUILD_DIR, DEFAULT_GENERATOR, ROOT_DIR
from .errors import ToolError
from .paths import ensure_root


def add_common_build_dir_argument(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--build-dir",
        default=str(DEFAULT_BUILD_DIR.relative_to(ROOT_DIR)),
        help="Build directory relative to repo root or absolute path.",
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="run.py",
        description="WaveBits developer orchestration tools.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    configure_parser = subparsers.add_parser("configure", help="Configure the root CMake build.")
    add_common_build_dir_argument(configure_parser)
    configure_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use. Defaults to Ninja.",
    )
    configure_parser.set_defaults(func=cmd_configure)

    build_parser_cmd = subparsers.add_parser("build", help="Build the configured CMake tree.")
    add_common_build_dir_argument(build_parser_cmd)
    build_parser_cmd.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use if auto-configuring.",
    )
    build_parser_cmd.add_argument(
        "--target",
        action="append",
        help="Optional build target. Repeat to pass multiple targets.",
    )
    build_parser_cmd.add_argument(
        "--no-configure-if-missing",
        dest="configure_if_missing",
        action="store_false",
        help="Do not auto-run configure when the build directory is missing CMakeCache.txt.",
    )
    build_parser_cmd.set_defaults(configure_if_missing=True, func=cmd_build)

    test_parser = subparsers.add_parser("test", help="Run ctest for the configured build.")
    add_common_build_dir_argument(test_parser)
    test_parser.add_argument(
        "--no-output-on-failure",
        dest="output_on_failure",
        action="store_false",
        help="Do not pass --output-on-failure to ctest.",
    )
    test_parser.add_argument(
        "--tests-regex",
        help="Optional ctest regex filter passed via -R.",
    )
    test_parser.add_argument(
        "--report-dir",
        help=(
            "Optional report output directory. Defaults to "
            "build/test-artifacts/reports/<timestamp>/."
        ),
    )
    test_parser.add_argument(
        "--no-report",
        dest="write_report",
        action="store_false",
        help="Do not write summary.json and run.log for this ctest run.",
    )
    test_parser.set_defaults(output_on_failure=True, write_report=True, func=cmd_test)

    android_parser = subparsers.add_parser("android", help="Run the standard Android Gradle entrypoints.")
    android_parser.add_argument(
        "action",
        choices=["assemble-debug", "assemble-release", "native-debug"],
        help="Android build action.",
    )
    android_parser.add_argument(
        "--clean",
        action="store_true",
        help="Prepend Gradle clean before the selected task.",
    )
    android_parser.set_defaults(func=cmd_android)

    verify_parser = subparsers.add_parser(
        "verify",
        help="Run configure + build + ctest, then Android assembleDebug by default.",
    )
    add_common_build_dir_argument(verify_parser)
    verify_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use for configure.",
    )
    verify_parser.add_argument(
        "--skip-android",
        action="store_true",
        help="Skip the Android assembleDebug step.",
    )
    verify_parser.set_defaults(func=cmd_verify)

    roundtrip_parser = subparsers.add_parser(
        "roundtrip",
        help="Generate a WAV artifact and decode it back to text via the CLI.",
    )
    add_common_build_dir_argument(roundtrip_parser)
    roundtrip_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use if auto-configuring/building the CLI target.",
    )
    roundtrip_parser.add_argument(
        "--mode",
        default="flash",
        choices=["flash", "pro", "ultra"],
        help="Transport mode to use.",
    )
    roundtrip_parser.add_argument("--text", help="Inline input text.")
    roundtrip_parser.add_argument("--text-file", help="UTF-8 input text file.")
    roundtrip_parser.add_argument("--case-name", help="Optional artifact case directory name.")
    roundtrip_parser.add_argument(
        "--out-dir",
        help="Optional output directory. Defaults to build/test-artifacts/roundtrip/<case>/",
    )
    roundtrip_parser.set_defaults(func=cmd_roundtrip)

    smoke_parser = subparsers.add_parser(
        "smoke",
        help="Generate visible roundtrip artifacts for representative flash/pro/ultra cases.",
    )
    add_common_build_dir_argument(smoke_parser)
    smoke_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use if auto-configuring/building the CLI target.",
    )
    smoke_parser.add_argument(
        "--name",
        default="default",
        help="Artifact batch name under build/test-artifacts/smoke/.",
    )
    smoke_parser.set_defaults(func=cmd_smoke)

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
