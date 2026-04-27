from __future__ import annotations

import argparse
import shutil
from pathlib import Path
import os

from ..constants import CLI_TARGET_NAME, DEFAULT_BUILD_DIR, ROOT_DIR
from ..errors import ToolError
from ..paths import cmake_cache_exists, resolve_build_dir
from ..process import run_capture
from ..windows_env import load_msvc_environment
from .build import cmd_build
from .cli import cmd_cli
from .configure import cmd_configure


def cmd_windows(args: argparse.Namespace) -> None:
    if args.action == "probe-msvc-env":
        _probe_msvc_environment(args)
        return

    build_dir = resolve_build_dir(args.build_dir)
    build_type = "Debug" if args.debug else "Release"
    build_tests = bool(getattr(args, "build_tests", False))
    if args.action != "build":
        raise ToolError(f"Unsupported windows action: {args.action}")

    if not cmake_cache_exists(build_dir):
        cmd_configure(
            argparse.Namespace(
                build_dir=str(build_dir),
                generator=args.generator,
                build_type=build_type,
                build_tests=build_tests,
            )
        )
    else:
        cmd_configure(
            argparse.Namespace(
                build_dir=str(build_dir),
                generator=args.generator,
                build_type=build_type,
                build_tests=build_tests,
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

    if build_tests:
        cmd_build(
            argparse.Namespace(
                build_dir=str(build_dir),
                configure_if_missing=False,
                generator=args.generator,
                target=None,
            )
        )

    cmd_cli(
        argparse.Namespace(
            action="build",
            build_dir=str(build_dir),
            release=not args.debug,
        )
    )

    artifact_path = build_dir / "bin" / _artifact_name()
    copied_dlls = _copy_runtime_dlls(artifact_path)
    if not build_tests:
        removed_tests = _remove_stale_test_binaries(build_dir / "bin")
        if removed_tests:
            print("Removed stale test executables:")
            for test_path in removed_tests:
                print(f"- {test_path}")
    if copied_dlls:
        print("Copied Windows runtime DLLs:")
        for dll_path in copied_dlls:
            print(f"- {dll_path}")

    if args.out_dir:
        source = artifact_path
        if not source.exists():
            raise ToolError(f"Expected built Windows artifact at {source}, but the file does not exist.")
        destination_dir = _resolve_output_directory(args.out_dir)
        destination_dir.mkdir(parents=True, exist_ok=True)
        destination = destination_dir / source.name
        shutil.copy2(source, destination)
        for dll_path in copied_dlls:
            shutil.copy2(dll_path, destination_dir / dll_path.name)
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


def _remove_stale_test_binaries(bin_dir: Path) -> list[Path]:
    if not bin_dir.exists():
        return []

    removed: list[Path] = []
    for candidate in sorted(bin_dir.glob("test_*.exe")):
        if not candidate.is_file():
            continue
        candidate.unlink()
        removed.append(candidate)
    return removed


def _copy_runtime_dlls(artifact_path: Path) -> list[Path]:
    if os.name != "nt":
        return []
    if not artifact_path.exists():
        raise ToolError(f"Expected built Windows artifact at {artifact_path}, but the file does not exist.")

    destination_dir = artifact_path.parent
    queue: list[Path] = [artifact_path]
    visited: set[Path] = set()
    copied: list[Path] = []
    copied_names = {artifact_path.name.lower()}

    while queue:
        current = queue.pop(0)
        current_key = current.resolve()
        if current_key in visited:
            continue
        visited.add(current_key)

        for dll_name in _list_imported_dlls(current):
            dll_key = dll_name.lower()
            if _should_skip_runtime_dll(dll_key):
                continue
            if dll_key in copied_names:
                continue
            source = _resolve_runtime_dll(dll_name)
            if source is None:
                raise ToolError(
                    f"Could not locate required runtime DLL `{dll_name}` for `{current.name}` on PATH."
                )
            destination = destination_dir / source.name
            shutil.copy2(source, destination)
            copied.append(destination)
            copied_names.add(dll_key)
            queue.append(destination)

    return copied


def _list_imported_dlls(binary_path: Path) -> list[str]:
    result = run_capture(["objdump", "-p", str(binary_path)], echo=False)
    if result.returncode != 0:
        raise ToolError(f"Failed to inspect DLL imports for {binary_path}")

    dll_names: list[str] = []
    marker = "DLL Name:"
    for line in result.stdout.splitlines():
        if marker not in line:
            continue
        dll_name = line.split(marker, 1)[1].strip()
        if dll_name:
            dll_names.append(dll_name)
    return dll_names


def _should_skip_runtime_dll(dll_name_lower: str) -> bool:
    if dll_name_lower.startswith("api-ms-win-"):
        return True
    system_dlls = {
        "kernel32.dll",
        "user32.dll",
        "gdi32.dll",
        "winspool.drv",
        "shell32.dll",
        "ole32.dll",
        "oleaut32.dll",
        "uuid.dll",
        "comdlg32.dll",
        "advapi32.dll",
        "ntdll.dll",
        "bcryptprimitives.dll",
        "ws2_32.dll",
        "userenv.dll",
    }
    return dll_name_lower in system_dlls


def _resolve_runtime_dll(dll_name: str) -> Path | None:
    current_path = os.environ.get("PATH", "")
    for raw_entry in current_path.split(os.pathsep):
        if not raw_entry:
            continue
        candidate = Path(raw_entry) / dll_name
        if candidate.exists():
            windows_root = Path(os.environ.get("WINDIR", r"C:\Windows")).resolve()
            try:
                if candidate.resolve().is_relative_to(windows_root):
                    return None
            except ValueError:
                pass
            return candidate
    return None
