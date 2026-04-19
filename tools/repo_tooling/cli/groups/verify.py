from __future__ import annotations

import argparse

from ...commands import cmd_verify
from ...commands.verify import format_verify_check_groups
from ...constants import DEFAULT_GENERATOR
from ..common import RAW_FORMATTER, add_common_build_dir_argument


def register_verify_command(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    verify_parser = subparsers.add_parser(
        "verify",
        help="Run static policy checks + configure + build + ctest, then apps/audio_android Gradle :app:assembleDebug by default.",
        description=(
            "Run the full verify pipeline.\n\n"
            f"{format_verify_check_groups()}\n\n"
            "Behavior:\n"
            "- Runs static check groups before configure/build/test.\n"
            "- Can optionally run clang-format --check before configure/build/test.\n"
            "- Builds and tests the selected host build directory.\n"
            "- Runs apps/audio_android assembleDebug unless --skip-android is passed.\n\n"
            "Use --list-checks to print the current static check groups without building."
        ),
        formatter_class=RAW_FORMATTER,
    )
    add_common_build_dir_argument(verify_parser)
    verify_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use for configure. The only supported root-host generator is Ninja.",
    )
    verify_parser.add_argument(
        "--skip-android",
        action="store_true",
        help="Skip the apps/audio_android Gradle :app:assembleDebug step.",
    )
    verify_parser.add_argument(
        "--format-check",
        action="store_true",
        help="Run clang-format --check before configure/build/test.",
    )
    verify_parser.add_argument(
        "--format-scope",
        default="libs",
        choices=["libs", "host-native", "android-native", "all-native"],
        help="Source scope to use when --format-check is enabled. Defaults to libs.",
    )
    verify_parser.add_argument(
        "--list-checks",
        action="store_true",
        help="Print the static check groups verify runs before building, then exit.",
    )
    verify_parser.set_defaults(func=cmd_verify)
