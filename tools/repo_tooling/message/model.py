from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class ParsedHistoryEntry:
    path: str
    release_version: str
    release_date: str
    component_name: str
    added: list[str] = field(default_factory=list)
    changed: list[str] = field(default_factory=list)
    fixed: list[str] = field(default_factory=list)


@dataclass(frozen=True)
class MessagePrepResult:
    source_mode: str
    history_paths: list[str]
    changed_files: list[str]
    inferred_type: str
    release_version: str
    component_versions: list[tuple[str, str]]
    summary_lines: list[str]
    added: list[str]
    changed: list[str]
    fixed: list[str]
