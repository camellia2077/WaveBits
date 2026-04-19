from __future__ import annotations

import json
from dataclasses import asdict
from pathlib import Path

from .model import BucketSummary, HistoryPrepResult
from .render import render_result


def safe_bucket_filename(bucket_name: str, extension: str) -> str:
    sanitized = bucket_name.replace("/", "_").replace("\\", "_").replace(" ", "_").replace(".", "_")
    return f"{sanitized}.{extension}"


def write_output(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def render_bucket_result(parent: HistoryPrepResult, bucket: BucketSummary) -> HistoryPrepResult:
    return HistoryPrepResult(
        scopes=parent.scopes,
        suggested_date=parent.suggested_date,
        suggested_version=parent.suggested_version,
        component_versions=parent.component_versions,
        candidate_topics=bucket.candidate_topics,
        representative_files=bucket.representative_files,
        relevant_summary=bucket.relevant_summary,
        changed_files=bucket.changed_files,
        buckets=[bucket],
    )


def write_split_outputs(
    result: HistoryPrepResult,
    output_format: str,
    relevant_only: bool,
    out_dir: Path,
) -> None:
    # Split output is intended for agent-friendly chunked reading: an index for
    # discovery plus one file per relevant bucket.
    extension = "json" if output_format == "json" else "md" if output_format == "markdown" else "txt"

    index_payload = {
        "scopes": result.scopes,
        "suggested_date": result.suggested_date,
        "suggested_version": result.suggested_version,
        "component_versions": [asdict(item) for item in result.component_versions],
        "candidate_topics": [asdict(item) for item in result.candidate_topics],
        "representative_files": result.representative_files,
        "bucket_files": {
            bucket.name: safe_bucket_filename(bucket.name, extension)
            for bucket in result.buckets
        },
    }
    write_output(out_dir / "index.json", json.dumps(index_payload, ensure_ascii=False, indent=2) + "\n")

    for bucket in result.buckets:
        bucket_result = render_bucket_result(result, bucket)
        bucket_content = render_result(bucket_result, output_format=output_format, relevant_only=relevant_only)
        bucket_file = out_dir / safe_bucket_filename(bucket.name, extension)
        write_output(bucket_file, bucket_content)
