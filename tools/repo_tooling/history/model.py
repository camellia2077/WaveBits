from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ChangedPath:
    status: str
    path: str


@dataclass(frozen=True)
class VersionHint:
    name: str
    source: str
    value: str


@dataclass(frozen=True)
class CandidateTopic:
    title: str
    reason: str
    bucket: str


@dataclass(frozen=True)
class RelevantSummary:
    bucket_counts: dict[str, int]
    changed_bucket_order: list[str]


@dataclass(frozen=True)
class BucketSummary:
    name: str
    changed_files: list[ChangedPath]
    candidate_topics: list[CandidateTopic]
    representative_files: list[str]
    relevant_summary: RelevantSummary


@dataclass(frozen=True)
class HistoryPrepResult:
    # Single intermediate model for history drafting. Renderers and optional
    # file-split outputs consume this shape so business inference stays in one
    # place instead of leaking into each output format.
    scopes: list[str]
    suggested_date: str
    suggested_version: str
    component_versions: list[VersionHint]
    candidate_topics: list[CandidateTopic]
    representative_files: list[str]
    relevant_summary: RelevantSummary
    changed_files: list[ChangedPath]
    buckets: list[BucketSummary]
