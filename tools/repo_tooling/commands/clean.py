from __future__ import annotations

import argparse
import shutil
from pathlib import Path
from typing import Iterable

from ..artifacts import test_artifacts_root
from ..constants import ANDROID_DIST_DIR, ANDROID_GRADLE_ROOT, ROOT_DIR
from ..errors import ToolError
from ..paths import cmake_cache_exists, gradle_wrapper, resolve_build_dir
from ..process import quote, run


_DEFAULT_SCOPES = ("host",)
_ALL_SCOPES = ("host", "android", "artifacts", "python")
_PYTHON_CACHE_DIR_NAMES = ("__pycache__", ".pytest_cache", ".mypy_cache", ".ruff_cache")
_PYTHON_CACHE_FILE_SUFFIXES = (".pyc", ".pyo")


def _render_command(command: Iterable[str], cwd: Path) -> str:
    rendered = " ".join(quote(part) for part in command)
    return f"{rendered} (cwd={cwd})"


def _is_within_root(path: Path) -> bool:
    resolved = path.resolve(strict=False)
    root = ROOT_DIR.resolve(strict=False)
    return resolved == root or root in resolved.parents


def _looks_like_host_build_dir(path: Path) -> bool:
    return (
        cmake_cache_exists(path)
        or (path / "CMakeFiles").exists()
        or path.name == "build"
        or path.parent.name == "build"
    )


def _ensure_safe_host_build_dir(build_dir: Path) -> None:
    resolved = build_dir.resolve(strict=False)
    root = ROOT_DIR.resolve(strict=False)
    anchor = Path(resolved.anchor)
    if resolved == root or resolved == anchor:
        raise ToolError(f"Refusing to delete unsafe build directory path: {build_dir}")
    if _is_within_root(build_dir):
        return
    if not _looks_like_host_build_dir(build_dir):
        raise ToolError(
            f"Refusing to delete build directory outside the repository root: {build_dir}. "
            "Pass a repo-local build dir, or clean the external path manually."
        )


def _remove_directory(path: Path, *, dry_run: bool) -> None:
    if not path.exists():
        print(f"Skip missing directory: {path}")
        return
    if not path.is_dir():
        raise ToolError(f"Expected a directory at {path}, but found a file.")
    if dry_run:
        print(f"Would remove directory: {path}")
        return
    shutil.rmtree(path)
    print(f"Removed directory: {path}")


def _remove_file(path: Path, *, dry_run: bool) -> None:
    if not path.exists():
        print(f"Skip missing file: {path}")
        return
    if not path.is_file():
        raise ToolError(f"Expected a file at {path}, but found a directory.")
    if dry_run:
        print(f"Would remove file: {path}")
        return
    path.unlink()
    print(f"Removed file: {path}")


def _collect_python_cache_directories() -> list[Path]:
    directories: set[Path] = set()
    for name in _PYTHON_CACHE_DIR_NAMES:
        directories.update(path for path in ROOT_DIR.rglob(name) if path.is_dir())
    return sorted(directories)


def _collect_python_cache_files(cache_directories: Iterable[Path]) -> list[Path]:
    cache_directory_set = {path.resolve(strict=False) for path in cache_directories}
    files: set[Path] = set()
    for suffix in _PYTHON_CACHE_FILE_SUFFIXES:
        for path in ROOT_DIR.rglob(f"*{suffix}"):
            if not path.is_file():
                continue
            if any(parent.resolve(strict=False) in cache_directory_set for parent in path.parents):
                continue
            files.add(path)
    return sorted(files)


def _resolve_scopes(raw_scopes: list[str] | None) -> list[str]:
    requested = raw_scopes or list(_DEFAULT_SCOPES)
    scopes: list[str] = []
    for scope in requested:
        expanded = _ALL_SCOPES if scope == "all" else (scope,)
        for item in expanded:
            if item not in scopes:
                scopes.append(item)
    return scopes


def _clean_host(build_dir: Path, *, dry_run: bool) -> None:
    _ensure_safe_host_build_dir(build_dir)
    _remove_directory(build_dir, dry_run=dry_run)


def _clean_android(*, dry_run: bool) -> None:
    command = [*gradle_wrapper(), "clean"]
    if dry_run:
        print(f"Would run: {_render_command(command, ANDROID_GRADLE_ROOT)}")
        return
    run(command, cwd=ANDROID_GRADLE_ROOT)


def _clean_artifacts(build_dir: Path, *, dry_run: bool) -> None:
    _remove_directory(test_artifacts_root(build_dir), dry_run=dry_run)
    _remove_directory(ANDROID_DIST_DIR, dry_run=dry_run)


def _clean_python(*, dry_run: bool) -> None:
    cache_directories = _collect_python_cache_directories()
    cache_files = _collect_python_cache_files(cache_directories)
    if not cache_directories and not cache_files:
        print("Skip missing Python cache outputs under repository root.")
        return
    for path in cache_directories:
        _remove_directory(path, dry_run=dry_run)
    for path in cache_files:
        _remove_file(path, dry_run=dry_run)


def cmd_clean(args: argparse.Namespace) -> None:
    scopes = _resolve_scopes(args.scope)
    build_dir = resolve_build_dir(args.build_dir)
    for scope in scopes:
        if scope == "host":
            _clean_host(build_dir, dry_run=args.dry_run)
            continue
        if scope == "android":
            _clean_android(dry_run=args.dry_run)
            continue
        if scope == "artifacts":
            _clean_artifacts(build_dir, dry_run=args.dry_run)
            continue
        if scope == "python":
            _clean_python(dry_run=args.dry_run)
            continue
        raise ToolError(f"Unsupported clean scope: {scope}")
