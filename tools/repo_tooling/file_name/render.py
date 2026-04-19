from __future__ import annotations

from .model import CandidateFileReview, FileNamePrepResult


def _render_checks(candidate: CandidateFileReview) -> list[str]:
    lines = [f"- {candidate.path}"]
    for check in candidate.checks:
        lines.append(f"  - [{check.status}] {check.message}")
    return lines


def _render_agent_judgment(candidate: CandidateFileReview) -> list[str]:
    lines = [f"- {candidate.path}"]
    for note in candidate.agent_judgment:
        lines.append(f"  - {note}")
    return lines


def _render_candidate_files(result: FileNamePrepResult) -> list[str]:
    if not result.candidate_paths:
        return ["- No added or renamed files were discovered from current git changes."]
    return [f"- {path}" for path in result.candidate_paths]


def _render_suggested_reading(candidate: CandidateFileReview) -> list[str]:
    lines = [f"- {candidate.path}"]
    for item in candidate.suggested_reading:
        lines.append(f"  - {item}")
    return lines


def render_result(result: FileNamePrepResult) -> str:
    sections: list[str] = []
    sections.append("# File Name Prep")
    sections.append("")
    sections.append(f"Source: {result.source_description}")
    sections.append(f"Output: {result.message_path}")
    sections.append("")
    sections.append("## Mechanizable Checks")
    if result.candidates:
        for candidate in result.candidates:
            sections.extend(_render_checks(candidate))
    else:
        sections.append("- No candidate files required mechanical checks.")
    sections.append("")
    sections.append("## Agent Judgment Needed")
    if result.candidates:
        for candidate in result.candidates:
            sections.extend(_render_agent_judgment(candidate))
    else:
        sections.append("- No candidate files were discovered, so there is no naming judgment to review yet.")
    sections.append("")
    sections.append("## Candidate Files")
    sections.extend(_render_candidate_files(result))
    sections.append("")
    sections.append("## Suggested Reading")
    sections.append("- `.agent/guides/file-name-styles.md`")
    if result.candidates:
        for candidate in result.candidates:
            sections.extend(_render_suggested_reading(candidate))
    else:
        sections.append("- Inspect the next added or renamed file before using this note as naming guidance.")
    sections.append("")
    return "\n".join(sections)
