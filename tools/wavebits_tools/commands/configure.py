from __future__ import annotations

import argparse
from pathlib import Path

from ..constants import DEFAULT_CXX_COMPILER, ROOT_DIR
from ..paths import cmake_cache_exists, resolve_build_dir
from ..process import run


def _resolve_host_modules_enabled(args: argparse.Namespace) -> bool:
    if getattr(args, "no_modules", False):
        return False
    if getattr(args, "experimental_modules", False):
        return True
    return True


def _configured_cxx_compiler(build_dir: Path) -> str | None:
    cache_path = build_dir / "CMakeCache.txt"
    if not cache_path.exists():
        return None

    for line in cache_path.read_text(encoding="utf-8").splitlines():
        if line.startswith("CMAKE_CXX_COMPILER:"):
            return line.partition("=")[2].strip()
    return None


def _compiler_matches(configured: str | None, desired: str) -> bool:
    if not configured:
        return False

    configured_name = Path(configured).name.lower()
    desired_name = Path(desired).name.lower()
    return configured_name == desired_name or configured.lower() == desired.lower()


def cmd_configure(args: argparse.Namespace) -> None:
    build_dir = resolve_build_dir(args.build_dir)
    build_dir.parent.mkdir(parents=True, exist_ok=True)
    configured_compiler = _configured_cxx_compiler(build_dir) if cmake_cache_exists(build_dir) else None
    command = [
        "cmake",
    ]
    if configured_compiler and not _compiler_matches(configured_compiler, DEFAULT_CXX_COMPILER):
        command.append("--fresh")
    command.extend([
        "-S",
        str(ROOT_DIR),
        "-B",
        str(build_dir),
        "-G",
        args.generator,
    ])
    command.append(
        f"-DWAVEBITS_HOST_MODULES={'ON' if _resolve_host_modules_enabled(args) else 'OFF'}"
    )
    command.append(f"-DCMAKE_CXX_COMPILER={DEFAULT_CXX_COMPILER}")
    run(command)
