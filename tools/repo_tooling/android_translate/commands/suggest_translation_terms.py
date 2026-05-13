from __future__ import annotations

from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
import re

from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    display_language_tag,
    get_faction_from_filename,
    get_text_type_from_filename,
)
from core.translation_resources import AndroidStringResourceRepository, ResourceFile


@dataclass(frozen=True)
class TermSuggestionMatch:
    filename: str
    key: str
    english_text: str
    localized_texts: dict[str, str | None]


@dataclass(frozen=True)
class TermSuggestionCandidate:
    value: str
    count: int
    source_keys: tuple[str, ...]


@dataclass(frozen=True)
class TermSuggestionResult:
    exit_code: int
    term: str
    languages: tuple[str, ...]
    text_type: str | None
    group: str | None
    whole_word: bool
    matched_files: int
    matched_keys: int
    matches: tuple[TermSuggestionMatch, ...]
    suggestions: dict[str, tuple[TermSuggestionCandidate, ...]]
    errors: tuple[str, ...]


def _build_matcher(term: str, *, whole_word: bool, case_sensitive: bool) -> re.Pattern[str]:
    flags = 0 if case_sensitive else re.IGNORECASE
    escaped = re.escape(term)
    if whole_word:
        return re.compile(rf"(?<!\w){escaped}(?!\w)", flags)
    return re.compile(escaped, flags)


def _resource_matches_scope(resource: ResourceFile, *, text_type: str | None, group: str | None) -> bool:
    if text_type and resource.text_type != text_type:
        return False
    if group and get_faction_from_filename(resource.filename) != group:
        return False
    return True


def suggest_translation_terms(
    *,
    term: str,
    res_dir: str | Path = DEFAULT_RES_DIRECTORY,
    lang: str | None = None,
    text_type: str | None = None,
    group: str | None = None,
    whole_word: bool = False,
    case_sensitive: bool = False,
) -> TermSuggestionResult:
    normalized_term = term.strip()
    if not normalized_term:
        return TermSuggestionResult(
            exit_code=2,
            term=term,
            languages=(),
            text_type=text_type,
            group=group,
            whole_word=whole_word,
            matched_files=0,
            matched_keys=0,
            matches=(),
            suggestions={},
            errors=("term must be non-empty",),
        )

    repository = AndroidStringResourceRepository(res_dir)
    repository.ensure_base_directory()
    matcher = _build_matcher(normalized_term, whole_word=whole_word, case_sensitive=case_sensitive)
    base_files = repository.load_base_resource_files()

    localized_dirs = repository.iter_localized_directories()
    if lang:
        localized_dirs = [(code, path) for code, path in localized_dirs if code == lang]
        if not localized_dirs:
            return TermSuggestionResult(
                exit_code=2,
                term=normalized_term,
                languages=(),
                text_type=text_type,
                group=group,
                whole_word=whole_word,
                matched_files=0,
                matched_keys=0,
                matches=(),
                suggestions={},
                errors=(f"localized language folder not found: {lang}",),
            )

    localized_cache: dict[tuple[str, str], dict[str, str]] = {}
    for lang_code, folder_path in localized_dirs:
        for filename in base_files:
            file_path = folder_path / filename
            if file_path.exists():
                localized_cache[(lang_code, filename)] = repository.load_localized_strings(folder_path, filename)

    matches: list[TermSuggestionMatch] = []
    candidate_sources: dict[str, list[tuple[str, str]]] = defaultdict(list)
    matched_files: set[str] = set()

    for filename, resource in sorted(base_files.items()):
        if not _resource_matches_scope(resource, text_type=text_type, group=group):
            continue
        for key, english_text in resource.strings.items():
            if not matcher.search(english_text):
                continue
            matched_files.add(filename)
            localized_texts: dict[str, str | None] = {}
            for lang_code, _folder_path in localized_dirs:
                localized_value = localized_cache.get((lang_code, filename), {}).get(key)
                localized_texts[lang_code] = localized_value
                if localized_value and english_text == normalized_term:
                    candidate_sources[lang_code].append((localized_value, key))
            matches.append(
                TermSuggestionMatch(
                    filename=filename,
                    key=key,
                    english_text=english_text,
                    localized_texts=localized_texts,
                )
            )

    suggestions: dict[str, tuple[TermSuggestionCandidate, ...]] = {}
    for lang_code, localized_values in candidate_sources.items():
        grouped_keys: dict[str, list[str]] = defaultdict(list)
        for localized_value, key in localized_values:
            grouped_keys[localized_value].append(key)
        counts = Counter(localized_value for localized_value, _key in localized_values)
        ranked = sorted(
            grouped_keys.items(),
            key=lambda item: (-counts[item[0]], item[0]),
        )
        suggestions[lang_code] = tuple(
            TermSuggestionCandidate(
                value=value,
                count=counts[value],
                source_keys=tuple(sorted(keys)),
            )
            for value, keys in ranked
        )

    return TermSuggestionResult(
        exit_code=0,
        term=normalized_term,
        languages=tuple(lang_code for lang_code, _path in localized_dirs),
        text_type=text_type,
        group=group,
        whole_word=whole_word,
        matched_files=len(matched_files),
        matched_keys=len(matches),
        matches=tuple(matches),
        suggestions=suggestions,
        errors=(),
    )


def build_term_suggestion_payload(result: TermSuggestionResult) -> dict[str, object]:
    return {
        "ok": result.exit_code == 0,
        "command": "term-suggestions",
        "exit_code": result.exit_code,
        "summary": {
            "term": result.term,
            "languages": list(result.languages),
            "text_type": result.text_type,
            "group": result.group,
            "whole_word": result.whole_word,
            "matched_files": result.matched_files,
            "matched_keys": result.matched_keys,
        },
        "matches": [
            {
                "file": match.filename,
                "key": match.key,
                "en": match.english_text,
                "localized": match.localized_texts,
            }
            for match in result.matches
        ],
        "suggestions": {
            lang_code: [
                {
                    "value": candidate.value,
                    "count": candidate.count,
                    "source_keys": list(candidate.source_keys),
                }
                for candidate in candidates
            ]
            for lang_code, candidates in sorted(result.suggestions.items())
        },
        "errors": list(result.errors),
    }


def print_term_suggestion_report(result: TermSuggestionResult) -> None:
    if result.errors:
        for error in result.errors:
            print(error)
        return

    print(
        f"Term `{result.term}` matched {result.matched_keys} keys in {result.matched_files} files "
        f"across {len(result.languages)} language folders."
    )
    if result.matches:
        print("Matches:")
        for match in result.matches:
            print(f"- {match.filename} | {match.key}")
            print(f"  EN: {match.english_text}")
            for lang_code, localized_text in sorted(match.localized_texts.items()):
                value = localized_text if localized_text is not None else "<missing>"
                print(f"  {display_language_tag(lang_code)}: {value}")
    if result.suggestions:
        print("Suggested term candidates:")
        for lang_code, candidates in sorted(result.suggestions.items()):
            print(f"- {display_language_tag(lang_code)}")
            if not candidates:
                print("  <no exact-value candidates>")
                continue
            for candidate in candidates:
                joined_keys = ", ".join(candidate.source_keys)
                print(f"  {candidate.value} (count={candidate.count}; keys={joined_keys})")
