from __future__ import annotations

from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError
from ..process import run_capture


def collect_git_status_paths() -> list[str]:
    result = run_capture(["git", "status", "--short", "--untracked-files=all"], cwd=ROOT_DIR, echo=False)
    if result.returncode != 0:
        raise ToolError(result.stderr.strip() or "Failed to read git status.")

    paths: list[str] = []
    for raw_line in result.stdout.splitlines():
        line = raw_line.rstrip()
        if not line or len(line) < 4:
            continue
        status = line[:2]
        payload = line[3:].strip()
        if status.replace(" ", "").startswith("R") and " -> " in payload:
            paths.append(payload.split(" -> ", 1)[1].strip())
        else:
            paths.append(payload)
    return paths


def is_history_markdown(path: str) -> bool:
    normalized = path.replace("\\", "/")
    if not normalized.endswith(".md"):
        return False
    return normalized.startswith("docs/libs/") or normalized.startswith("docs/presentation/")


def changed_history_paths() -> list[str]:
    seen: set[str] = set()
    histories: list[str] = []
    for path in collect_git_status_paths():
        if is_history_markdown(path) and path not in seen:
            seen.add(path)
            histories.append(path)
    return histories


def normalize_history_paths(raw_paths: list[str]) -> list[str]:
    normalized: list[str] = []
    seen: set[str] = set()
    for raw_path in raw_paths:
        path = Path(raw_path)
        if not path.is_absolute():
            path = ROOT_DIR / path
        try:
            relative = path.resolve().relative_to(ROOT_DIR).as_posix()
        except ValueError as exc:
            raise ToolError(f"History path is outside the repository: {raw_path}") from exc
        if not path.exists():
            raise ToolError(f"History file does not exist: {relative}")
        if not is_history_markdown(relative):
            raise ToolError(f"History file is not under docs/libs or docs/presentation: {relative}")
        if relative not in seen:
            seen.add(relative)
            normalized.append(relative)
    return normalized
