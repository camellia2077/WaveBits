from __future__ import annotations

import argparse
from pathlib import Path

from .android import cmd_android
from .build import cmd_build
from .configure import cmd_configure
from .test import cmd_test
from ..paths import resolve_build_dir


def run_verify_steps(build_dir: Path, generator: str, skip_android: bool) -> None:
    cmd_configure(argparse.Namespace(build_dir=str(build_dir), generator=generator))
    cmd_build(
        argparse.Namespace(
            build_dir=str(build_dir),
            configure_if_missing=False,
            generator=generator,
            target=None,
        )
    )
    cmd_test(
        argparse.Namespace(
            build_dir=str(build_dir),
            output_on_failure=True,
            tests_regex=None,
            write_report=True,
            report_dir=None,
        )
    )
    if not skip_android:
        cmd_android(argparse.Namespace(action="assemble-debug", clean=False))


def cmd_verify(args: argparse.Namespace) -> None:
    build_dir = resolve_build_dir(args.build_dir)
    run_verify_steps(build_dir=build_dir, generator=args.generator, skip_android=args.skip_android)
