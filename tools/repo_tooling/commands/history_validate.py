from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path

from markdown_it import MarkdownIt
from markdown_it.token import Token

from ..constants import ROOT_DIR
from ..errors import ToolError


_VERSION_HEADING_RE = re.compile(r"^\[(v\d+\.\d+\.\d+)\] - (\d{4}-\d{2}-\d{2})$")
_DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
_ALLOWED_SECTION_HEADINGS = [
    "新增功能 (Added)",
    "技术改进/重构 (Changed/Refactor)",
    "修复 (Fixed)",
    "安全性 (Security)",
    "弃用/删除 (Deprecated/Removed)",
]
_ALLOWED_SECTION_SET = set(_ALLOWED_SECTION_HEADINGS)


@dataclass(frozen=True)
class ValidationIssue:
    path: Path
    line: int
    message: str


@dataclass
class SectionState:
    path: Path
    title: str
    line: int
    has_list_item: bool = False


def _resolve_input_paths(raw_paths: list[str]) -> list[Path]:
    resolved_files: list[Path] = []
    seen: set[Path] = set()

    for raw_path in raw_paths:
        path = Path(raw_path)
        if not path.is_absolute():
            path = ROOT_DIR / path
        resolved = path.resolve()

        if resolved.is_dir():
            candidates = sorted(item.resolve() for item in resolved.rglob("*.md"))
            if not candidates:
                raise ToolError(f"No markdown files found under directory: {resolved}")
            for candidate in candidates:
                if candidate not in seen:
                    seen.add(candidate)
                    resolved_files.append(candidate)
            continue

        if not resolved.exists():
            raise ToolError(f"Path does not exist: {resolved}")
        if resolved.suffix.lower() != ".md":
            raise ToolError(f"Expected a markdown file or directory, got: {resolved}")
        if resolved not in seen:
            seen.add(resolved)
            resolved_files.append(resolved)

    return resolved_files


def _semantic_version_key(version: str) -> tuple[int, int, int]:
    major, minor, patch = version.removeprefix("v").split(".")
    return (int(major), int(minor), int(patch))


def _token_line(token: Token) -> int:
    if token.map:
        return token.map[0] + 1
    return 1


def _inline_text(tokens: list[Token], index: int) -> str:
    if index + 1 >= len(tokens):
        return ""
    inline_token = tokens[index + 1]
    if inline_token.type != "inline":
        return ""
    return inline_token.content.strip()


def _validate_file(path: Path) -> list[ValidationIssue]:
    try:
        content = path.read_text(encoding="utf-8")
    except OSError as exc:
        raise ToolError(f"Failed to read {path}: {exc}") from exc

    issues: list[ValidationIssue] = []
    if "TODO(agent)" in content:
        issues.append(ValidationIssue(path, 1, "Found unresolved `TODO(agent)` placeholder."))

    parser = MarkdownIt()
    tokens = parser.parse(content)

    seen_release_versions: list[tuple[str, int]] = []
    current_release_line: int | None = None
    current_section: SectionState | None = None
    saw_release_heading = False

    def close_section() -> None:
        nonlocal current_section
        if current_section is not None and not current_section.has_list_item:
            issues.append(
                ValidationIssue(
                    current_section.path,
                    current_section.line,
                    f"Section `{current_section.title}` is empty; remove the section or add `* ` items.",
                )
            )
        current_section = None

    for index, token in enumerate(tokens):
        if token.type == "heading_open":
            level = token.tag
            text = _inline_text(tokens, index)
            line = _token_line(token)

            if level == "h1":
                close_section()
                if text:
                    issues.append(
                        ValidationIssue(path, line, "History files should not use `#` headings; release entries must start at `##`.")
                    )
                continue

            if level == "h2":
                close_section()
                current_release_line = line
                saw_release_heading = True
                match = _VERSION_HEADING_RE.match(text)
                if match is None:
                    issues.append(
                        ValidationIssue(
                            path,
                            line,
                            "Release heading must match `## [vX.Y.Z] - YYYY-MM-DD`.",
                        )
                    )
                    continue

                version, version_date = match.groups()
                if _DATE_RE.match(version_date) is None:
                    issues.append(ValidationIssue(path, line, "Release date must use ISO 8601 `YYYY-MM-DD`."))
                seen_release_versions.append((version, line))
                continue

            if level == "h3":
                if current_release_line is None:
                    issues.append(
                        ValidationIssue(path, line, "Section heading must appear under a release `## [vX.Y.Z] - YYYY-MM-DD`."),
                    )
                    close_section()
                    continue

                close_section()
                if text not in _ALLOWED_SECTION_SET:
                    issues.append(
                        ValidationIssue(
                            path,
                            line,
                            "Section heading is not allowed. Use one of: "
                            + ", ".join(f"`{name}`" for name in _ALLOWED_SECTION_HEADINGS),
                        )
                    )
                current_section = SectionState(path=path, title=text, line=line)
                continue

            issues.append(ValidationIssue(path, line, "Only `##` release headings and `###` section headings are allowed."))
            continue

        if token.type == "bullet_list_open":
            if token.markup != "*":
                issues.append(
                    ValidationIssue(
                        path,
                        _token_line(token),
                        "History bullet lists must use `* ` instead of `-` or other markers.",
                    )
                )
            continue

        if token.type == "list_item_open":
            if current_section is not None:
                current_section.has_list_item = True
            continue

        if token.type == "paragraph_open" and current_section is None and current_release_line is not None:
            line = _token_line(token)
            paragraph_text = _inline_text(tokens, index)
            if paragraph_text:
                issues.append(
                    ValidationIssue(
                        path,
                        line,
                        "Release content should live inside allowed `###` sections, not as free paragraphs.",
                    )
                )

    close_section()

    if not saw_release_heading:
        issues.append(
            ValidationIssue(path, 1, "No release heading found. Expected at least one `## [vX.Y.Z] - YYYY-MM-DD`."),
        )

    for previous, current in zip(seen_release_versions, seen_release_versions[1:]):
        previous_version, previous_line = previous
        current_version, current_line = current
        if _semantic_version_key(previous_version) <= _semantic_version_key(current_version):
            issues.append(
                ValidationIssue(
                    path,
                    current_line,
                    f"Release order is invalid: `{current_version}` must appear after older versions, not before `{previous_version}`.",
                )
            )
            issues.append(
                ValidationIssue(
                    path,
                    previous_line,
                    "Latest version must appear first, with versions in strictly descending order.",
                )
            )
            break

    return issues


def cmd_history_validate(args: argparse.Namespace) -> None:
    files = _resolve_input_paths(args.paths)
    issues: list[ValidationIssue] = []
    for path in files:
        issues.extend(_validate_file(path))

    if issues:
        for issue in issues:
            try:
                display_path = issue.path.relative_to(ROOT_DIR)
            except ValueError:
                display_path = issue.path
            print(f"{display_path}:{issue.line}: {issue.message}")
        raise ToolError(f"history validation failed for {len(files)} file(s)")

    for path in files:
        try:
            display_path = path.relative_to(ROOT_DIR)
        except ValueError:
            display_path = path
        print(f"OK: {display_path}")
