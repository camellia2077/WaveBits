from __future__ import annotations

import argparse
from pathlib import Path

from ..errors import ToolError
from ..paths import resolve_build_dir
from .test import cmd_test


_LIBRARY_BUILD_DIRS: dict[str, Path] = {
    "audio_runtime": Path("libs") / "audio_runtime",
    "audio_api": Path("libs") / "audio_api",
    "audio_io": Path("libs") / "audio_io",
}


def cmd_test_lib(args: argparse.Namespace) -> None:
    try:
        library_build_suffix = _LIBRARY_BUILD_DIRS[args.library]
    except KeyError as exc:
        raise ToolError(f"Unsupported library test target: {args.library}") from exc

    build_dir = resolve_build_dir(args.build_dir) / library_build_suffix
    delegated_args = argparse.Namespace(
        build_dir=str(build_dir),
        output_on_failure=args.output_on_failure,
        tests_regex=args.tests_regex,
        report_dir=args.report_dir,
        write_report=args.write_report,
    )
    cmd_test(delegated_args)
