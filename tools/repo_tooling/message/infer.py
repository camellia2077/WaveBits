from __future__ import annotations

from .collect import changed_history_paths, collect_git_status_paths, normalize_history_paths
from .model import MessagePrepResult, ParsedHistoryEntry
from .parse import parse_history_file
from ..errors import ToolError


def infer_message_type(added: list[str], changed: list[str], fixed: list[str]) -> str:
    if added:
        return "feat"
    if fixed and not changed:
        return "fix"
    if changed:
        return "refactor"
    return "chore"


def choose_release_version(entries: list[ParsedHistoryEntry]) -> str:
    versions = sorted({entry.release_version for entry in entries})
    if len(versions) == 1:
        return versions[0]
    return "TODO(agent): choose unified release version"


def build_summary_lines(added: list[str], changed: list[str], fixed: list[str], source_mode: str) -> list[str]:
    summary = (added + changed + fixed)[:3]
    if summary:
        return summary
    if source_mode == "git-fallback":
        return ["TODO(agent): summarize the current git changes in 1-3 concise lines."]
    return ["TODO(agent): summarize the selected history entries in 1-3 concise lines."]


def build_history_message_result(history_paths: list[str]) -> MessagePrepResult:
    entries = [parse_history_file(path) for path in history_paths]
    added = [item for entry in entries for item in entry.added]
    changed = [item for entry in entries for item in entry.changed]
    fixed = [item for entry in entries for item in entry.fixed]
    inferred_type = infer_message_type(added, changed, fixed)
    return MessagePrepResult(
        source_mode="history",
        history_paths=history_paths,
        changed_files=[],
        inferred_type=inferred_type,
        release_version=choose_release_version(entries),
        component_versions=[(entry.component_name, entry.release_version) for entry in entries],
        summary_lines=build_summary_lines(added, changed, fixed, source_mode="history"),
        added=added,
        changed=changed,
        fixed=fixed,
    )


def build_git_fallback_result() -> MessagePrepResult:
    changed_files = collect_git_status_paths()
    preview = changed_files[:5]
    return MessagePrepResult(
        source_mode="git-fallback",
        history_paths=[],
        changed_files=changed_files,
        inferred_type="chore",
        release_version="TODO(agent): set release version",
        component_versions=[],
        summary_lines=build_summary_lines([], [], [], source_mode="git-fallback"),
        added=[],
        changed=[f"Changed file: `{path}`" for path in preview] or ["TODO(agent): review changed files and describe the main refactor or update."],
        fixed=[],
    )


def resolve_history_inputs(raw_history_paths: list[str] | None) -> list[str]:
    if raw_history_paths:
        return normalize_history_paths(raw_history_paths)

    histories = changed_history_paths()
    if not histories:
        return []
    if len(histories) == 1:
        return histories
    raise ToolError(
        "Multiple changed history files were detected. Re-run with explicit `--history <path>` arguments."
    )
