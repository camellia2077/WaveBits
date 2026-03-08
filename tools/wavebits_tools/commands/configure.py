from __future__ import annotations

import argparse

from ..constants import ROOT_DIR
from ..paths import resolve_build_dir
from ..process import run


def cmd_configure(args: argparse.Namespace) -> None:
    build_dir = resolve_build_dir(args.build_dir)
    build_dir.parent.mkdir(parents=True, exist_ok=True)
    command = [
        "cmake",
        "-S",
        str(ROOT_DIR),
        "-B",
        str(build_dir),
        "-G",
        args.generator,
    ]
    run(command)
