from __future__ import annotations

import argparse
from pathlib import Path
import shutil

from ..constants import ROOT_DIR
from ..errors import ToolError
from ..process import run_capture_streaming


_CPP_FILE_SUFFIXES = (".c", ".cc", ".cxx", ".cpp", ".cppm", ".h", ".hh", ".hpp", ".inc", ".ixx")
_FORMAT_BATCH_SIZE = 50
_DEFAULT_SCOPE = "libs"
_SCOPE_ROOTS: dict[str, tuple[Path, ...]] = {
    "libs": (ROOT_DIR / "libs",),
    "host-native": (
        ROOT_DIR / "libs",
        ROOT_DIR / "Test",
        ROOT_DIR / "apps" / "audio_cli" / "windows",
    ),
    "android-native": (
        ROOT_DIR / "apps" / "audio_android" / "app" / "src" / "main" / "cpp",
        ROOT_DIR / "apps" / "audio_android" / "native_package",
    ),
    "all-native": (
        ROOT_DIR / "libs",
        ROOT_DIR / "Test",
        ROOT_DIR / "apps" / "audio_cli" / "windows",
        ROOT_DIR / "apps" / "audio_android" / "app" / "src" / "main" / "cpp",
        ROOT_DIR / "apps" / "audio_android" / "native_package",
    ),
}


def cmd_format(args: argparse.Namespace) -> None:
    clang_format = shutil.which("clang-format")
    if clang_format is None:
        raise ToolError("`clang-format` was not found in PATH.")

    files = _resolve_files(args.path, args.scope)
    if not files:
        raise ToolError("No C/C++ files matched the selected format scope.")

    command_mode = "check" if args.check else "apply"
    print(f"--- clang-format mode: {command_mode}")
    print(f"--- clang-format files: {len(files)}")

    return_code = 0
    total_batches = (len(files) + _FORMAT_BATCH_SIZE - 1) // _FORMAT_BATCH_SIZE
    for batch_index, start in enumerate(range(0, len(files), _FORMAT_BATCH_SIZE), start=1):
        batch_files = files[start : start + _FORMAT_BATCH_SIZE]
        print(
            f"--- clang-format batch {batch_index}/{total_batches} "
            f"(files={len(batch_files)})"
        )
        command = [clang_format, "--style=file"]
        if args.check:
            command.extend(["--dry-run", "--Werror"])
        else:
            command.append("-i")
        command.extend(batch_files)
        result = run_capture_streaming(command, cwd=ROOT_DIR)
        if result.returncode != 0:
            return_code = result.returncode

    if return_code != 0:
        raise SystemExit(return_code)


def _resolve_files(raw_paths: list[str] | None, scope: str) -> list[str]:
    files: list[Path] = []
    if raw_paths:
        for raw_path in raw_paths:
            files.extend(_expand_path(Path(raw_path)))
    else:
        for root in _SCOPE_ROOTS[scope]:
            files.extend(_expand_path(root))

    unique_files: list[str] = []
    seen: set[str] = set()
    for file_path in files:
        normalized = str(file_path.resolve()).replace("\\", "/")
        if normalized in seen:
            continue
        seen.add(normalized)
        unique_files.append(normalized)
    unique_files.sort()
    return unique_files


def _expand_path(raw_path: Path) -> list[Path]:
    path = raw_path
    if not path.is_absolute():
        path = ROOT_DIR / path
    if not path.exists():
        raise ToolError(f"Format path does not exist: {path}")
    if path.is_file():
        return [path] if path.suffix.lower() in _CPP_FILE_SUFFIXES else []
    return sorted(
        child
        for child in path.rglob("*")
        if child.is_file() and child.suffix.lower() in _CPP_FILE_SUFFIXES
    )
