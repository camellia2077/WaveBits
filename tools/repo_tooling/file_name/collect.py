from __future__ import annotations

from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError
from ..process import run_capture


def parse_git_status_candidates(status_output: str) -> list[str]:
    candidates: list[str] = []
    for raw_line in status_output.splitlines():
        line = raw_line.rstrip()
        if not line or len(line) < 4:
            continue

        status = line[:2]
        payload = line[3:].strip()
        if status == "??":
            candidates.append(payload)
            continue

        normalized_status = status.replace(" ", "")
        if "R" in normalized_status:
            if " -> " in payload:
                candidates.append(payload.split(" -> ", 1)[1].strip())
            else:
                candidates.append(payload)
            continue

        if "A" in normalized_status:
            candidates.append(payload)

    seen: set[str] = set()
    unique_candidates: list[str] = []
    for candidate in candidates:
        if candidate not in seen:
            seen.add(candidate)
            unique_candidates.append(candidate)
    return unique_candidates


def collect_git_candidates() -> list[str]:
    result = run_capture(["git", "status", "--short", "--untracked-files=all"], cwd=ROOT_DIR, echo=False)
    if result.returncode != 0:
        raise ToolError(result.stderr.strip() or "Failed to read git status.")
    candidates = parse_git_status_candidates(result.stdout)
    return [path for path in candidates if (ROOT_DIR / path).is_file()]


def sibling_files_for(path: str) -> list[str]:
    file_path = ROOT_DIR / path
    parent = file_path.parent
    if not parent.exists():
        return []

    siblings: list[str] = []
    for sibling in parent.iterdir():
        if sibling.is_file() and sibling.name != file_path.name:
            siblings.append(str(sibling.relative_to(ROOT_DIR)).replace("\\", "/"))
    return sorted(siblings)


def representative_siblings(path: str, limit: int = 3) -> list[str]:
    siblings = sibling_files_for(path)
    file_path = Path(path)
    same_extension = [item for item in siblings if Path(item).suffix == file_path.suffix]
    if len(same_extension) >= limit:
        return same_extension[:limit]
    return (same_extension + siblings)[:limit]
