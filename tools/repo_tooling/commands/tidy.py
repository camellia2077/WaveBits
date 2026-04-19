from __future__ import annotations

import argparse
import math
from concurrent.futures import ThreadPoolExecutor, as_completed
import json
import re
from pathlib import Path
import shutil

from .configure import cmd_configure
from ..artifacts import write_json, write_utf8
from ..clang_tidy_tasks import build_tasks, write_task_artifacts
from ..constants import ROOT_DIR
from ..errors import ToolError
from ..paths import resolve_build_dir
from ..process import run, run_capture, run_capture_merged_streaming


_LIB_SOURCE_SUFFIXES = (".cpp", ".cc", ".cxx", ".cppm", ".ixx")
_TIDY_BATCH_SIZE = 25
_LIB_TARGETS = ("bag_core", "bag_api", "audio_io", "audio_runtime")


def cmd_tidy(args: argparse.Namespace) -> None:
    build_dir = resolve_build_dir(args.build_dir)
    output_root = _resolve_output_root(build_dir, args.out_dir)
    output_root.mkdir(parents=True, exist_ok=True)
    _copy_repo_tidy_configs(output_root)
    jobs = _resolve_jobs(args.jobs)
    header_filter = args.header_filter or _default_header_filter()

    if args.from_log:
        log_path = Path(args.from_log)
        if not log_path.is_absolute():
            log_path = (ROOT_DIR / log_path).resolve()
        if not log_path.exists():
            raise ToolError(f"clang-tidy log not found: {log_path}")
        log_content = log_path.read_text(encoding="utf-8", errors="replace")
        return_code = 0
        raw_log_path = output_root / "clang-tidy.log"
        write_utf8(raw_log_path, log_content)
    else:
        _ensure_compile_commands(build_dir, args.generator)
        _prebuild_lib_targets(build_dir)
        compile_commands_path = build_dir / "compile_commands.json"
        files = _load_lib_source_files(compile_commands_path)
        if args.limit is not None:
            if args.limit <= 0:
                raise ToolError("--limit must be > 0")
            files = files[: args.limit]
        if not files:
            raise ToolError("No libs translation units were found in compile_commands.json.")
        log_content, return_code = _run_clang_tidy(
            build_dir,
            files,
            jobs=jobs,
            header_filter=header_filter,
        )
        raw_log_path = output_root / "clang-tidy.log"
        write_utf8(raw_log_path, log_content)

    tasks = build_tasks(log_content)
    task_summary = write_task_artifacts(output_root, tasks)
    summary = {
        "build_dir": str(build_dir),
        "clang_tidy_returncode": return_code,
        "jobs": jobs,
        "header_filter": header_filter,
        "raw_log": "clang-tidy.log",
        "clang_tidy_config": ".clang-tidy",
        "clang_format_config": ".clang-format",
        "task_count": task_summary["task_count"],
        "tasks": task_summary["tasks"],
    }
    write_json(output_root / "run_summary.json", summary)

    print(f"--- clang-tidy output root: {output_root}")
    print(f"--- raw log: {raw_log_path}")
    print(f"--- tasks: {task_summary['task_count']}")
    if return_code != 0:
        print(
            f"--- clang-tidy returned {return_code}, but task artifacts were still generated."
        )


def _resolve_output_root(build_dir: Path, raw_out_dir: str | None) -> Path:
    if raw_out_dir:
        output_root = Path(raw_out_dir)
        if not output_root.is_absolute():
            output_root = ROOT_DIR / output_root
        return output_root
    return ROOT_DIR / "build" / "reports" / "clang-tidy" / "libs" / build_dir.name


def _copy_repo_tidy_configs(output_root: Path) -> None:
    for file_name in (".clang-tidy", ".clang-format"):
        source_path = ROOT_DIR / file_name
        if not source_path.exists():
            continue
        shutil.copy2(source_path, output_root / file_name)


def _resolve_jobs(raw_jobs: int | None) -> int:
    if raw_jobs is None:
        return 1
    if raw_jobs <= 0:
        raise ToolError("--jobs must be > 0")
    return raw_jobs


def _default_header_filter() -> str:
    libs_root = str((ROOT_DIR / "libs").resolve())
    escaped = re.escape(libs_root)
    escaped = escaped.replace(r"\/", r"[\\/]").replace(r"\\", r"[\\/]")
    return rf"^{escaped}[\\/].*"


def _ensure_compile_commands(build_dir: Path, generator: str) -> None:
    compile_commands_path = build_dir / "compile_commands.json"
    if compile_commands_path.exists():
        return

    print(
        f"--- compile_commands.json missing in {build_dir}, re-running configure with export enabled."
    )
    cmd_configure(
        argparse.Namespace(
            build_dir=str(build_dir),
            generator=generator,
        )
    )
    if not compile_commands_path.exists():
        raise ToolError(
            f"configure completed but compile_commands.json is still missing: {compile_commands_path}"
        )


def _load_lib_source_files(compile_commands_path: Path) -> list[str]:
    payload = json.loads(compile_commands_path.read_text(encoding="utf-8"))
    seen: set[str] = set()
    resolved_files: list[str] = []
    for item in payload:
        if not isinstance(item, dict):
            continue
        raw_file = str(item.get("file", "")).strip()
        if not raw_file:
            continue
        file_path = Path(raw_file)
        if not file_path.is_absolute():
            directory = Path(str(item.get("directory", "")).strip() or ".")
            file_path = directory / file_path
        normalized = str(file_path.resolve()).replace("\\", "/")
        if "/libs/" not in normalized:
            continue
        if Path(normalized).suffix.lower() not in _LIB_SOURCE_SUFFIXES:
            continue
        if normalized in seen:
            continue
        seen.add(normalized)
        resolved_files.append(normalized)
    resolved_files.sort()
    return resolved_files


def _prebuild_lib_targets(build_dir: Path) -> None:
    print("--- prebuilding libs targets for module/std import artifacts")
    run(
        [
            "cmake",
            "--build",
            str(build_dir),
            "--target",
            *_LIB_TARGETS,
        ],
        cwd=ROOT_DIR,
    )


def _run_clang_tidy(
    build_dir: Path,
    files: list[str],
    *,
    jobs: int,
    header_filter: str,
) -> tuple[str, int]:
    if shutil.which("clang-tidy") is None:
        raise ToolError("`clang-tidy` was not found in PATH.")

    batches: list[tuple[int, list[str]]] = []
    effective_batch_size = _resolve_batch_size(len(files), jobs)
    total_batches = (len(files) + effective_batch_size - 1) // effective_batch_size
    for batch_index, start in enumerate(range(0, len(files), effective_batch_size), start=1):
        batch_files = files[start : start + effective_batch_size]
        batches.append((batch_index, batch_files))

    print(f"--- clang-tidy jobs: {jobs}")
    print(f"--- clang-tidy batch-size: {effective_batch_size}")
    print(f"--- clang-tidy header-filter: {header_filter}")

    if jobs == 1:
        combined_logs: list[str] = []
        combined_return_code = 0
        for batch_index, batch_files in batches:
            print(
                f"--- clang-tidy batch {batch_index}/{total_batches} "
                f"(files={len(batch_files)})"
            )
            result = run_capture_merged_streaming(
                _build_tidy_command(build_dir, batch_files, header_filter),
                cwd=ROOT_DIR,
            )
            combined_logs.append(result.stdout)
            if result.returncode != 0:
                combined_return_code = result.returncode
        return "".join(combined_logs), combined_return_code

    ordered_logs: dict[int, str] = {}
    combined_return_code = 0
    with ThreadPoolExecutor(max_workers=jobs) as executor:
        future_map = {}
        for batch_index, batch_files in batches:
            print(
                f"--- clang-tidy batch {batch_index}/{total_batches} queued "
                f"(files={len(batch_files)})"
            )
            future = executor.submit(
                run_capture,
                _build_tidy_command(build_dir, batch_files, header_filter),
                ROOT_DIR,
            )
            future_map[future] = (batch_index, len(batch_files))

        for future in as_completed(future_map):
            batch_index, file_count = future_map[future]
            result = future.result()
            ordered_logs[batch_index] = result.stdout
            if result.returncode != 0:
                combined_return_code = result.returncode
            print(
                f"--- clang-tidy batch {batch_index}/{total_batches} finished "
                f"(files={file_count}, exit={result.returncode})"
            )

    combined_logs = [ordered_logs[index] for index, _ in batches]
    return "".join(combined_logs), combined_return_code


def _build_tidy_command(build_dir: Path, batch_files: list[str], header_filter: str) -> list[str]:
    return [
        "clang-tidy",
        "--use-color=false",
        f"--header-filter={header_filter}",
        "-p",
        str(build_dir),
        *batch_files,
    ]


def _resolve_batch_size(file_count: int, jobs: int) -> int:
    if jobs <= 1:
        return _TIDY_BATCH_SIZE
    return max(1, min(_TIDY_BATCH_SIZE, math.ceil(file_count / jobs)))
