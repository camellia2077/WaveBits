from __future__ import annotations

import re
from collections import Counter
from pathlib import Path

from .collect import sibling_files_for
from .model import CandidateFileReview, FileNameCheck


_BANNED_TOKENS = {"utils", "common", "temp", "misc"}
_SNAKE_CASE_PATTERN = re.compile(r"^[a-z0-9]+(?:_[a-z0-9]+)*$")
_DOMAIN_ROLE_PATTERN = re.compile(r"^[a-z0-9]+(?:_[a-z0-9]+)+$")


def classify_name_style(stem: str) -> str:
    if stem.startswith("i_") and _SNAKE_CASE_PATTERN.match(stem):
        return "snake_case"
    if _SNAKE_CASE_PATTERN.match(stem):
        return "snake_case"
    if re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)+", stem):
        return "kebab-case"
    if re.fullmatch(r"[a-z0-9]+", stem):
        return "flat"
    return "other"


def template_matches(stem: str) -> list[str]:
    matches: list[str] = []
    if re.fullmatch(r"i_[a-z0-9]+(?:_[a-z0-9]+)*", stem):
        matches.append("interface template (`i_<capability>`)")
    for suffix, label in (
        ("_config", "config template"),
        ("_types", "types template"),
        ("_schema", "schema template"),
        ("_registry", "registry template"),
        ("_manifest", "manifest template"),
        ("_aliases", "aliases template"),
        ("_requests", "requests template"),
        ("_responses", "responses template"),
        ("_dto", "dto template"),
        ("_model", "model template"),
        ("_models", "models template"),
    ):
        if stem.endswith(suffix) and len(stem) > len(suffix):
            matches.append(f"{label} (`<domain>{suffix}`)")
    return matches


def sibling_majority_style(path: str) -> str | None:
    counts: Counter[str] = Counter()
    for sibling in sibling_files_for(path):
        sibling_stem = Path(sibling).stem
        sibling_style = classify_name_style(sibling_stem)
        if sibling_style != "other":
            counts[sibling_style] += 1
    if not counts:
        return None
    majority_style, count = counts.most_common(1)[0]
    if count < 2:
        return None
    return majority_style


def build_candidate_review(path: str, suggested_reading: list[str]) -> CandidateFileReview:
    file_path = Path(path)
    stem = file_path.stem
    extension = file_path.suffix
    checks: list[FileNameCheck] = []

    if _SNAKE_CASE_PATTERN.match(stem):
        checks.append(FileNameCheck("snake_case", "pass", "Basename follows snake_case."))
    else:
        checks.append(FileNameCheck("snake_case", "warning", "Basename does not follow snake_case."))

    banned_hits = sorted(token for token in _BANNED_TOKENS if token in stem.split("_"))
    if banned_hits:
        checks.append(
            FileNameCheck(
                "banned_tokens",
                "warning",
                f"Basename contains discouraged generic token(s): {', '.join(banned_hits)}.",
            )
        )
    else:
        checks.append(FileNameCheck("banned_tokens", "pass", "No discouraged generic tokens detected."))

    if _DOMAIN_ROLE_PATTERN.match(stem):
        checks.append(FileNameCheck("domain_role_shape", "pass", "Basename matches a `<domain>_<role>`-like shape."))
    else:
        checks.append(
            FileNameCheck(
                "domain_role_shape",
                "warning",
                "Basename does not clearly match a `<domain>_<role>`-like shape.",
            )
        )

    matches = template_matches(stem)
    if matches:
        checks.append(
            FileNameCheck(
                "template_match",
                "pass",
                f"Detected naming template(s): {', '.join(matches)}.",
            )
        )
    else:
        checks.append(
            FileNameCheck(
                "template_match",
                "pass",
                "No explicit naming template detected; agent should judge whether a template is appropriate.",
            )
        )

    majority_style = sibling_majority_style(path)
    current_style = classify_name_style(stem)
    if majority_style is None:
        checks.append(
            FileNameCheck(
                "sibling_style",
                "pass",
                "Not enough sibling files to infer a strong local naming style.",
            )
        )
    elif current_style == majority_style:
        checks.append(
            FileNameCheck(
                "sibling_style",
                "pass",
                f"Basename aligns with the dominant sibling naming style ({majority_style}).",
            )
        )
    else:
        checks.append(
            FileNameCheck(
                "sibling_style",
                "warning",
                f"Basename differs from the dominant sibling naming style ({majority_style}).",
            )
        )

    agent_judgment = [
        "Confirm the basename actually expresses the file's responsibility, not just a nearby implementation detail.",
        "Check whether the file mixes multiple responsibilities and should be split instead of renamed in place.",
        "Confirm the chosen domain/role words match repository vocabulary and nearby files.",
    ]
    if matches:
        agent_judgment.append("Template matched mechanically; confirm the template does not hide a more precise responsibility name.")
    else:
        agent_judgment.append("No template matched; confirm whether a repository template would make the file easier to scan.")

    return CandidateFileReview(
        path=path,
        basename=file_path.name,
        extension=extension,
        checks=checks,
        agent_judgment=agent_judgment,
        suggested_reading=suggested_reading,
    )
