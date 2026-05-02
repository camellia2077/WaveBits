from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re

from core.translation_paths import DEFAULT_RES_DIRECTORY

STRING_LINE_RE = re.compile(r'^\s*<string\s+name="([^"]+)"')


@dataclass(frozen=True)
class AddTranslationKeyResult:
    exit_code: int
    touched_files: list[Path]
    skipped_files: list[Path]
    errors: list[str]
    localized_fallback_used: bool


def _escape_android_string(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', '\\"')
    )


def _string_names(lines: list[str]) -> set[str]:
    names: set[str] = set()
    for line in lines:
        match = STRING_LINE_RE.match(line)
        if match:
            names.add(match.group(1))
    return names


def _insert_before_resources_end(lines: list[str], new_lines: list[str]) -> list[str]:
    for index in range(len(lines) - 1, -1, -1):
        if lines[index].strip() == "</resources>":
            return lines[:index] + new_lines + lines[index:]
    return lines + new_lines


def _build_key_lines(key: str, value: str, context: str | None) -> list[str]:
    lines: list[str] = []
    if context:
        lines.append(f"    <!-- CONTEXT: {context} -->")
    lines.append(f'    <string name="{key}">{_escape_android_string(value)}</string>')
    return lines


def add_translation_key(
    *,
    filename: str,
    key: str,
    english_value: str,
    localized_value: str | None = None,
    context: str | None = None,
    res_dir: str | Path = DEFAULT_RES_DIRECTORY,
) -> AddTranslationKeyResult:
    res_path = Path(res_dir)
    base_path = res_path / "values" / filename
    if not base_path.exists():
        return AddTranslationKeyResult(
            exit_code=2,
            touched_files=[],
            skipped_files=[],
            errors=[f"Base resource file not found: {base_path}"],
            localized_fallback_used=False,
        )

    touched: list[Path] = []
    skipped: list[Path] = []
    errors: list[str] = []
    target_files = [base_path]
    if localized_value is not None:
        target_files.extend(
            directory / filename
            for directory in sorted(res_path.iterdir())
            if directory.is_dir() and directory.name.startswith("values-") and (directory / filename).exists()
        )

    for path in target_files:
        value = english_value if path == base_path else localized_value
        try:
            lines = path.read_text(encoding="utf-8").splitlines()
        except OSError as exc:
            errors.append(f"Could not read {path}: {exc}")
            continue

        if key in _string_names(lines):
            skipped.append(path)
            continue

        updated_lines = _insert_before_resources_end(
            lines,
            _build_key_lines(key, value, context if path == base_path else None),
        )
        path.write_text("\n".join(updated_lines) + "\n", encoding="utf-8")
        touched.append(path)

    return AddTranslationKeyResult(
        exit_code=1 if errors else 0,
        touched_files=touched,
        skipped_files=skipped,
        errors=errors,
        localized_fallback_used=localized_value is not None,
    )
