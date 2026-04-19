from __future__ import annotations

import re
from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError
from .model import ParsedHistoryEntry


_HEADING_RE = re.compile(r"^## \[(v\d+\.\d+\.\d+)\] - (\d{4}-\d{2}-\d{2})$")
_SECTION_RE = re.compile(r"^### .+ \((Added|Changed/Refactor|Fixed)\)$")


def component_name_for_history(path: str) -> str:
    normalized = path.replace("\\", "/")
    if normalized.startswith("docs/presentation/cli/"):
        return "cli-presentation"
    if normalized.startswith("docs/presentation/android/"):
        return "android-presentation"
    if normalized.startswith("docs/libs/"):
        return "libs"
    return Path(normalized).parent.name or "changed-component"


def parse_history_file(path: str) -> ParsedHistoryEntry:
    text = (ROOT_DIR / path).read_text(encoding="utf-8")
    release_version = ""
    release_date = ""
    current_section: str | None = None
    section_bullets: dict[str, list[str]] = {"Added": [], "Changed/Refactor": [], "Fixed": []}

    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            continue

        heading_match = _HEADING_RE.match(line)
        if heading_match:
            release_version, release_date = heading_match.groups()
            continue

        section_match = _SECTION_RE.match(line)
        if section_match:
            current_section = section_match.group(1)
            continue

        if current_section and (line.startswith("* ") or line.startswith("- ")):
            section_bullets[current_section].append(line[2:].strip())

    if not release_version or not release_date:
        raise ToolError(f"History file is missing a valid release heading: {path}")

    return ParsedHistoryEntry(
        path=path,
        release_version=release_version,
        release_date=release_date,
        component_name=component_name_for_history(path),
        added=section_bullets["Added"],
        changed=section_bullets["Changed/Refactor"],
        fixed=section_bullets["Fixed"],
    )
