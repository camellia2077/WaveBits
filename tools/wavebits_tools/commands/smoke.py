from __future__ import annotations

import argparse
from dataclasses import dataclass

from .roundtrip import cmd_roundtrip
from ..artifacts import slugify, test_artifacts_root, unique_directory
from ..paths import resolve_build_dir


@dataclass(frozen=True)
class SmokeCase:
    name: str
    mode: str
    text: str


SMOKE_CASES = [
    SmokeCase(name="flash-hello", mode="flash", text="Hello WaveBits"),
    SmokeCase(name="pro-ascii", mode="pro", text="ASCII-123"),
    SmokeCase(name="ultra-utf8", mode="ultra", text="你好，WaveBits"),
]


def cmd_smoke(args: argparse.Namespace) -> None:
    build_dir = resolve_build_dir(args.build_dir)
    root_dir = unique_directory(test_artifacts_root(build_dir) / "smoke" / slugify(args.name, fallback="smoke"))
    print(f"Smoke artifact root: {root_dir}")

    for case in SMOKE_CASES:
        cmd_roundtrip(
            argparse.Namespace(
                build_dir=str(build_dir),
                generator=args.generator,
                experimental_modules=getattr(args, "experimental_modules", False),
                no_modules=getattr(args, "no_modules", False),
                mode=case.mode,
                text=case.text,
                text_file=None,
                case_name=case.name,
                out_dir=str(root_dir / case.name),
            )
        )
