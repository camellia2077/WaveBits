from __future__ import annotations

import re
from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError
from ..process import run_capture
from .model import ChangedPath, VersionHint


_VERSION_HEADING_RE = re.compile(r"^## \[(v\d+\.\d+\.\d+)\] - (\d{4}-\d{2}-\d{2})$", re.MULTILINE)


def normalize_target_history_file(raw_target: str) -> Path:
    target = Path(raw_target)
    if not target.is_absolute():
        target = ROOT_DIR / target
    return target.resolve()


def parse_porcelain_line(line: str) -> ChangedPath:
    if len(line) < 4:
        raise ToolError(f"Unexpected git status --short line: {line!r}")
    status = line[:2].strip() or "?"
    raw_path = line[3:]
    if " -> " in raw_path:
        raw_path = raw_path.split(" -> ", 1)[1]
    return ChangedPath(status=status, path=raw_path.replace("\\", "/"))


def collect_changed_paths() -> list[ChangedPath]:
    result = run_capture(["git", "status", "--short"], cwd=ROOT_DIR, echo=False)
    if result.returncode != 0:
        raise ToolError(result.stderr.strip() or "git status --short failed")

    changed_paths: list[ChangedPath] = []
    for line in result.stdout.splitlines():
        stripped = line.rstrip()
        if not stripped:
            continue
        changed_paths.append(parse_porcelain_line(stripped))
    return changed_paths


def normalize_scope(raw_scope: str) -> str:
    scope = raw_scope.strip().replace("\\", "/").strip("/")
    if not scope:
        raise ToolError("History scope cannot be empty.")
    return scope


def matches_scope(path: str, scope: str) -> bool:
    return path == scope or path.startswith(f"{scope}/")


def apply_scopes(changed_paths: list[ChangedPath], scopes: list[str]) -> list[ChangedPath]:
    if not scopes:
        return changed_paths
    normalized_scopes = [normalize_scope(scope) for scope in scopes]
    return [
        item for item in changed_paths
        if any(matches_scope(item.path, scope) for scope in normalized_scopes)
    ]


def semantic_version_key(version: str) -> tuple[int, int, int]:
    major, minor, patch = version.removeprefix("v").split(".")
    return (int(major), int(minor), int(patch))


def latest_version_in_tree(root: Path) -> tuple[str, str] | None:
    if not root.is_dir():
        return None

    candidates: list[tuple[tuple[int, int, int], str, str]] = []
    for path in root.rglob("*.md"):
        try:
            content = path.read_text(encoding="utf-8")
        except OSError:
            continue
        match = _VERSION_HEADING_RE.search(content)
        if match is None:
            continue
        version, version_date = match.groups()
        candidates.append((semantic_version_key(version), version, version_date))

    if not candidates:
        return None
    _, version, version_date = max(candidates, key=lambda item: item[0])
    return version, version_date


def collect_version_hints(changed_paths: list[ChangedPath]) -> list[VersionHint]:
    changed_path_strings = [item.path for item in changed_paths]
    hints: list[VersionHint] = []

    def maybe_add(name: str, path_prefix: str, docs_root: Path) -> None:
        if not any(path.startswith(path_prefix) for path in changed_path_strings):
            return
        latest = latest_version_in_tree(docs_root)
        if latest is None:
            hints.append(
                VersionHint(
                    name=name,
                    source=str(docs_root.relative_to(ROOT_DIR)),
                    value="changed",
                )
            )
            return
        version, version_date = latest
        hints.append(
            VersionHint(
                name=name,
                source=f"{docs_root.relative_to(ROOT_DIR)} ({version_date})",
                value=version,
            )
        )

    maybe_add("android-presentation", "apps/audio_android/", ROOT_DIR / "docs" / "presentation" / "android")
    maybe_add("cli-presentation", "apps/audio_cli/", ROOT_DIR / "docs" / "presentation" / "cli")
    maybe_add("libs", "libs/", ROOT_DIR / "docs" / "libs")
    return hints


def bucket_name(path: str) -> str:
    if path.startswith("apps/audio_android/"):
        return "android-app"
    if path.startswith("apps/audio_cli/"):
        return "cli-app"
    if path.startswith("libs/audio_api/"):
        return "libs/audio_api"
    if path.startswith("libs/audio_core/"):
        return "libs/audio_core"
    if path.startswith("libs/audio_io/"):
        return "libs/audio_io"
    if path.startswith("libs/audio_runtime/"):
        return "libs/audio_runtime"
    if path.startswith("libs/"):
        return "libs/other"
    if path.startswith("docs/presentation/android/"):
        return "docs/presentation/android"
    if path.startswith("docs/presentation/cli/"):
        return "docs/presentation/cli"
    if path.startswith("docs/libs/"):
        return "docs/libs"
    if path.startswith("docs/"):
        return "docs/other"
    if path.startswith("tools/"):
        return "tools"
    if path.startswith("Test/"):
        return "tests"
    if path.startswith(".agent/"):
        return "agent-guides"
    return "repo-root/other"


def group_paths(changed_paths: list[ChangedPath]) -> dict[str, list[ChangedPath]]:
    grouped: dict[str, list[ChangedPath]] = {}
    for item in changed_paths:
        grouped.setdefault(bucket_name(item.path), []).append(item)
    return dict(sorted(grouped.items(), key=lambda pair: pair[0]))
