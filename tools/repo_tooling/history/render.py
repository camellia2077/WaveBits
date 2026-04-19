from __future__ import annotations

import json
from dataclasses import asdict

from .model import HistoryPrepResult


def render_draft_entry(result: HistoryPrepResult) -> list[str]:
    lines: list[str] = []
    lines.append("## Draft Entry")
    lines.append("")
    lines.append("```md")
    lines.append(f"## [{result.suggested_version}] - {result.suggested_date}")
    lines.append("")
    lines.append("### 新增功能 (Added)")
    lines.append("* TODO(agent): 仅在本次确实新增了用户可感知或工程上重要的新能力时保留。")
    lines.append("")
    lines.append("### 技术改进/重构 (Changed/Refactor)")
    lines.append("* TODO(agent): 归纳这次真正重要的结构调整、边界变化或工作流改造。")
    lines.append("")
    lines.append("### 修复 (Fixed)")
    lines.append("* TODO(agent): 仅保留明确的 bug、回归或契约修复。")
    lines.append("")
    lines.append("### 安全性 (Security)")
    lines.append("* TODO(agent): 若无安全相关改动，删除整个分类。")
    lines.append("")
    lines.append("### 弃用/删除 (Deprecated/Removed)")
    lines.append("* TODO(agent): 若无弃用或删除项，删除整个分类。")
    lines.append("```")
    return lines


def render_relevant_summary_markdown(result: HistoryPrepResult) -> list[str]:
    lines: list[str] = []
    lines.append("## Relevant Summary")
    if result.candidate_topics:
        lines.append("- Candidate history topics:")
        for topic in result.candidate_topics:
            lines.append(f"  - `{topic.title}`: {topic.reason}")
    else:
        lines.append("- No candidate topics were inferred from the current filtered changes.")

    if result.buckets:
        lines.append("- Relevant buckets:")
        for bucket in result.buckets:
            lines.append(f"  - `{bucket.name}`: {len(bucket.changed_files)} changed path(s)")
    else:
        lines.append("- No changed files matched the requested scope.")
    if result.representative_files:
        lines.append("- Representative files:")
        for path in result.representative_files:
            lines.append(f"  - `{path}`")
    lines.append("")
    return lines


def render_markdown(result: HistoryPrepResult, relevant_only: bool) -> str:
    lines: list[str] = []
    lines.append("# History Prep")
    lines.append("")
    lines.append("## Agent Notes")
    lines.append("- This output is a scaffold for the agent, not a final history entry.")
    lines.append("- Keep, rewrite, merge, or drop any suggested bullets based on the actual change intent.")
    lines.append("")
    lines.append("## Release Hints")
    lines.append(f"- Suggested version placeholder: `{result.suggested_version}`")
    lines.append(f"- Suggested date: `{result.suggested_date}`")
    if result.scopes:
        lines.append(f"- Applied scope filter: `{', '.join(result.scopes)}`")
    if result.component_versions:
        for hint in result.component_versions:
            lines.append(f"- {hint.name}: `{hint.value}` from `{hint.source}`")
    else:
        lines.append("- No component version hints were inferred from the current changed paths.")
    lines.append("")
    lines.extend(render_relevant_summary_markdown(result))

    if not relevant_only:
        lines.append("## Changed Files")
        if result.buckets:
            for bucket in result.buckets:
                lines.append(f"### {bucket.name}")
                for item in bucket.changed_files:
                    lines.append(f"- `{item.status}` `{item.path}`")
                lines.append("")
        else:
            lines.append("- No changed files detected in `git status --short`.")
            lines.append("")

    lines.extend(render_draft_entry(result))
    return "\n".join(lines).rstrip() + "\n"


def render_plain(result: HistoryPrepResult, relevant_only: bool) -> str:
    lines: list[str] = []
    lines.append("HISTORY PREP")
    lines.append("")
    lines.append(f"suggested_version: {result.suggested_version}")
    lines.append(f"suggested_date: {result.suggested_date}")
    lines.append(f"scopes: {', '.join(result.scopes) if result.scopes else '(all changed files)'}")
    lines.append("")

    lines.append("component_versions:")
    if result.component_versions:
        for hint in result.component_versions:
            lines.append(f"- {hint.name}: {hint.value} [{hint.source}]")
    else:
        lines.append("- none")
    lines.append("")

    lines.append("candidate_topics:")
    if result.candidate_topics:
        for topic in result.candidate_topics:
            lines.append(f"- {topic.title} | bucket={topic.bucket} | {topic.reason}")
    else:
        lines.append("- none")
    lines.append("")

    lines.append("bucket_counts:")
    if result.buckets:
        for bucket in result.buckets:
            lines.append(f"- {bucket.name}: {len(bucket.changed_files)}")
    else:
        lines.append("- none")
    lines.append("")

    lines.append("representative_files:")
    if result.representative_files:
        for path in result.representative_files:
            lines.append(f"- {path}")
    else:
        lines.append("- none")
    lines.append("")

    if not relevant_only:
        lines.append("changed_files:")
        if result.buckets:
            for bucket in result.buckets:
                lines.append(f"[{bucket.name}]")
                for item in bucket.changed_files:
                    lines.append(f"- {item.status} {item.path}")
        else:
            lines.append("- none")
        lines.append("")

    lines.append("draft_entry:")
    lines.append(f"## [{result.suggested_version}] - {result.suggested_date}")
    lines.append("### 新增功能 (Added)")
    lines.append("* TODO(agent)")
    lines.append("### 技术改进/重构 (Changed/Refactor)")
    lines.append("* TODO(agent)")
    lines.append("### 修复 (Fixed)")
    lines.append("* TODO(agent)")
    lines.append("### 安全性 (Security)")
    lines.append("* TODO(agent or remove section)")
    lines.append("### 弃用/删除 (Deprecated/Removed)")
    lines.append("* TODO(agent or remove section)")
    return "\n".join(lines).rstrip() + "\n"


def result_to_json_payload(result: HistoryPrepResult) -> dict[str, object]:
    return {
        "scopes": result.scopes,
        "suggested_date": result.suggested_date,
        "suggested_version": result.suggested_version,
        "component_versions": [asdict(item) for item in result.component_versions],
        "candidate_topics": [asdict(item) for item in result.candidate_topics],
        "representative_files": result.representative_files,
        "relevant_summary": asdict(result.relevant_summary),
        "changed_files": [asdict(item) for item in result.changed_files],
        "buckets": [
            {
                "name": bucket.name,
                "changed_files": [asdict(item) for item in bucket.changed_files],
                "candidate_topics": [asdict(item) for item in bucket.candidate_topics],
                "representative_files": bucket.representative_files,
                "relevant_summary": asdict(bucket.relevant_summary),
            }
            for bucket in result.buckets
        ],
    }


def render_json(result: HistoryPrepResult) -> str:
    return json.dumps(result_to_json_payload(result), ensure_ascii=False, indent=2) + "\n"


def render_result(result: HistoryPrepResult, output_format: str, relevant_only: bool) -> str:
    # Renderers only project the shared model into different reading surfaces;
    # they should not invent new summary logic.
    if output_format == "json":
        return render_json(result)
    if output_format == "plain":
        return render_plain(result, relevant_only=relevant_only)
    return render_markdown(result, relevant_only=relevant_only)
