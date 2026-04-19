from __future__ import annotations

import argparse

from .configure import cmd_configure
from ..paths import cmake_cache_exists, configured_build_config, resolve_build_dir
from ..process import run


def cmd_build(args: argparse.Namespace) -> None:
    build_dir = resolve_build_dir(args.build_dir)
    if args.configure_if_missing and not cmake_cache_exists(build_dir):
        cmd_configure(
            argparse.Namespace(
                build_dir=str(build_dir),
                generator=args.generator,
            )
        )

    command = ["cmake", "--build", str(build_dir)]
    configured_build = configured_build_config(build_dir)
    if configured_build:
        command.extend(["--config", configured_build])
    if args.target:
        command.extend(["--target", *args.target])
    run(command)
