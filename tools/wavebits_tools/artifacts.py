from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

from .constants import CLI_TARGET_NAME, ROOT_DIR, TEST_ARTIFACTS_DIR_NAME


def build_root_for(build_dir: Path) -> Path:
    if build_dir.parent == ROOT_DIR / "build":
        return build_dir.parent
    if build_dir.parent.name:
        return build_dir.parent
    return ROOT_DIR / "build"


def test_artifacts_root(build_dir: Path) -> Path:
    return build_root_for(build_dir) / TEST_ARTIFACTS_DIR_NAME


def slugify(value: str, fallback: str = "case") -> str:
    compact = value.strip().replace("\n", " ").replace("\r", " ")
    compact = re.sub(r"\s+", "-", compact)
    compact = re.sub(r"[^A-Za-z0-9._-]+", "-", compact)
    compact = compact.strip("-._")
    if not compact:
        return fallback
    return compact[:48]


def unique_directory(path: Path) -> Path:
    if not path.exists():
        path.mkdir(parents=True, exist_ok=False)
        return path

    index = 2
    while True:
        candidate = path.with_name(f"{path.name}-{index}")
        if not candidate.exists():
            candidate.mkdir(parents=True, exist_ok=False)
            return candidate
        index += 1


def ensure_parent(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def write_utf8(path: Path, content: str) -> None:
    ensure_parent(path)
    path.write_text(content, encoding="utf-8")


def write_json(path: Path, payload: dict[str, Any]) -> None:
    ensure_parent(path)
    path.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def cli_binary_candidates(build_dir: Path) -> list[Path]:
    suffix = ".exe" if __import__("os").name == "nt" else ""
    return [
        build_dir / "bin" / f"{CLI_TARGET_NAME}{suffix}",
        build_dir / "apps" / "audio_cli" / "windows" / "cmake" / f"{CLI_TARGET_NAME}{suffix}",
    ]


def find_cli_binary(build_dir: Path) -> Path | None:
    for candidate in cli_binary_candidates(build_dir):
        if candidate.exists():
            return candidate
    return None
