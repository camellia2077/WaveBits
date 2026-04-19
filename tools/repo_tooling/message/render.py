from __future__ import annotations

from .model import MessagePrepResult


def render_result(result: MessagePrepResult) -> str:
    components = ", ".join(name for name, _ in result.component_versions) or "changed components"
    subject_hint = f"TODO(agent): write concise subject for {components}"

    lines: list[str] = [f"{result.inferred_type}: {subject_hint}", ""]
    lines.append("[Summary]")
    lines.extend(result.summary_lines)
    lines.append("")

    if result.component_versions:
        lines.append("[Component Versions]")
        for name, version in result.component_versions:
            lines.append(f"- {name}: {version}")
        lines.append("")

    if result.added:
        lines.append("[Added]")
        for item in result.added:
            lines.append(f"- {item}")
        lines.append("")

    if result.changed:
        lines.append("[Changed & Refactored]")
        for item in result.changed:
            lines.append(f"- {item}")
        lines.append("")

    if result.fixed:
        lines.append("[Fixed]")
        for item in result.fixed:
            lines.append(f"- {item}")
        lines.append("")

    lines.append("[Verification]")
    lines.append("- TODO(agent): add verification steps for this commit.")
    if result.source_mode == "git-fallback":
        lines.append("- TODO(agent): no history file was selected; confirm the draft against the final history before committing.")
    lines.append("")
    lines.append(f"Release-Version: {result.release_version}")
    lines.append("")
    return "\n".join(lines)
