from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class FileNameCheck:
    kind: str
    status: str
    message: str


@dataclass(frozen=True)
class CandidateFileReview:
    path: str
    basename: str
    extension: str
    checks: list[FileNameCheck] = field(default_factory=list)
    agent_judgment: list[str] = field(default_factory=list)
    suggested_reading: list[str] = field(default_factory=list)


@dataclass(frozen=True)
class FileNamePrepResult:
    candidates: list[CandidateFileReview]
    candidate_paths: list[str]
    message_path: str
    source_description: str
