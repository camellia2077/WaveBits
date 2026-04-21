from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from ..constants import CLI_TARGET_NAME, DEFAULT_BUILD_DIR, ROOT_DIR
from ..errors import ToolError
from ..paths import cmake_cache_exists, resolve_build_dir
from ..windows_env import load_msvc_environment
from .build import cmd_build
from .cli import cmd_cli
from .configure import cmd_configure


def cmd_windows(args: argparse.Namespace) -> None:
    if args.action == "probe-msvc-env":
        _probe_msvc_environment(args)
        return

    build_dir = resolve_build_dir(args.build_dir)
    if args.action != "build":
        raise ToolError(f"Unsupported windows action: {args.action}")

    if not cmake_cache_exists(build_dir):
        cmd_configure(
            argparse.Namespace(
                build_dir=str(build_dir),
                generator=args.generator,
            )
        )

    cmd_build(
        argparse.Namespace(
            build_dir=str(build_dir),
            configure_if_missing=False,
            generator=args.generator,
            target=["FlipBits_rust_cli_native_deps"],
        )
    )

    cmd_cli(
        argparse.Namespace(
            action="build",
            build_dir=str(build_dir),
            release=not args.debug,
        )
    )

    if args.out_dir:
        source = build_dir / "bin" / _artifact_name()
        if not source.exists():
            raise ToolError(f"Expected built Windows artifact at {source}, but the file does not exist.")
        destination_dir = _resolve_output_directory(args.out_dir)
        destination_dir.mkdir(parents=True, exist_ok=True)
        destination = destination_dir / source.name
        shutil.copy2(source, destination)
        print(f"Exported Windows artifact: {destination}")


def _probe_msvc_environment(args: argparse.Namespace) -> None:
    probe_env = load_msvc_environment()
    build_dir = _resolve_probe_build_dir(args.build_dir)
    print(f"MSVC installation: {probe_env.installation_path}")
    print(f"MSVC init script: {probe_env.init_script}")
    print(f"Probe build directory: {build_dir}")

    cmd_configure(
        argparse.Namespace(
            build_dir=str(build_dir),
            generator=args.generator,
            compiler="clang-cl",
            env=probe_env.env,
        )
    )
    print("MSVC environment probe configure succeeded.")


def _artifact_name() -> str:
    return f"{CLI_TARGET_NAME}.exe"


def _resolve_output_directory(raw: str) -> Path:
    path = Path(raw)
    if not path.is_absolute():
        path = ROOT_DIR / path
    return path


def _resolve_probe_build_dir(raw: str) -> Path:
    default_raw = str(DEFAULT_BUILD_DIR.relative_to(ROOT_DIR))
    if raw == default_raw:
        return ROOT_DIR / "build" / "windows-msvc-probe"
    return _resolve_output_directory(raw)
