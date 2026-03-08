from __future__ import annotations

import os
from pathlib import Path

from .constants import ANDROID_DIR, DEFAULT_BUILD_DIR, ROOT_DIR
from .errors import ToolError


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


def gradle_wrapper() -> list[str]:
    if os.name == "nt":
        return [str(ANDROID_DIR / "gradlew.bat")]
    return [str(ANDROID_DIR / "gradlew")]
