from __future__ import annotations

import os
from pathlib import Path
from typing import Final

from .constants import ANDROID_GRADLE_ROOT, DEFAULT_BUILD_DIR, ROOT_DIR
from .errors import ToolError


DEFAULT_MULTI_CONFIG: Final[str] = "Debug"


def ensure_root() -> None:
    cmake_file = ROOT_DIR / "CMakeLists.txt"
    if not cmake_file.exists():
        raise ToolError(f"Could not locate repository root from {ROOT_DIR}.")


def resolve_build_dir(raw: str | None) -> Path:
    if not raw:
        return DEFAULT_BUILD_DIR
    path = Path(raw)
    if not path.is_absolute():
        path = ROOT_DIR / path
    return path


def cmake_cache_exists(build_dir: Path) -> bool:
    return (build_dir / "CMakeCache.txt").exists()


def read_cmake_cache_value(build_dir: Path, key: str) -> str | None:
    cache_path = build_dir / "CMakeCache.txt"
    if not cache_path.exists():
        return None

    prefix = f"{key}:"
    for line in cache_path.read_text(encoding="utf-8").splitlines():
        if line.startswith(prefix):
            return line.partition("=")[2].strip()
    return None


def configured_generator(build_dir: Path) -> str | None:
    return read_cmake_cache_value(build_dir, "CMAKE_GENERATOR")


def is_multi_config_generator(generator: str | None) -> bool:
    return False


def configured_build_config(build_dir: Path) -> str | None:
    generator = configured_generator(build_dir)
    if not is_multi_config_generator(generator):
        return None
    return DEFAULT_MULTI_CONFIG


def gradle_wrapper() -> list[str]:
    if os.name == "nt":
        return [str(ANDROID_GRADLE_ROOT / "gradlew.bat")]
    return [str(ANDROID_GRADLE_ROOT / "gradlew")]
