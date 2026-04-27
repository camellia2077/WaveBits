from __future__ import annotations

import argparse

from ...commands import cmd_windows
from ...constants import DEFAULT_GENERATOR
from ..common import RAW_FORMATTER, add_common_build_dir_argument


def register_windows_group(
    subparsers: argparse._SubParsersAction[argparse.ArgumentParser],
) -> None:
    windows_parser = subparsers.add_parser(
        "windows",
        help="Build Windows delivery artifacts without host test targets.",
        description=(
            "Windows delivery helper commands.\n\n"
            "Use this group when you want the Windows CLI product artifact without building the full host test set.\n"
            "- Ensures the native static libraries needed by the Rust CLI exist in the selected build tree.\n"
            "- Builds the Rust CLI as a Windows executable.\n"
            "- Leaves the final executable at build/<dir>/bin/FlipBits.exe.\n"
            "- Can optionally copy the executable into another delivery directory.\n"
            "- `probe-msvc-env` bootstraps the Visual Studio build environment and runs a `clang-cl` CMake configure probe."
        ),
        formatter_class=RAW_FORMATTER,
    )
    windows_parser.add_argument(
        "action",
        choices=["build", "probe-msvc-env"],
        help="Windows delivery action.",
    )
    add_common_build_dir_argument(windows_parser)
    windows_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use if auto-configuring the host build. The only supported root-host generator is Ninja.",
    )
    windows_parser.add_argument(
        "--debug",
        action="store_true",
        help="Build the debug CLI executable instead of the default optimized release build.",
    )
    windows_parser.add_argument(
        "--build-tests",
        action="store_true",
        help="Also configure and keep native Test/ executables in this build tree. Default is off for windows delivery builds.",
    )
    windows_parser.add_argument(
        "--out-dir",
        help="Optional export directory. If set, copy FlipBits.exe there after a successful build.",
    )
    windows_parser.set_defaults(func=cmd_windows)
