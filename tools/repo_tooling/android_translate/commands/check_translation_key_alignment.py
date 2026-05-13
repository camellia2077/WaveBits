from __future__ import annotations

from dataclasses import dataclass
import json
from pathlib import Path

from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    DEFAULT_TRANSLATION_KEY_ALIGNMENT_DIRECTORY,
    display_language_tag,
    is_pro_sample_key,
    iter_translation_text_xml_paths,
)
from core.translation_reporting import (
    MinimalMarkdownReportWriter,
    OutputDirectoryManager,
    ReportFileBlock,
    ReportKeyBlock,
)
from core.translation_resources import AndroidStringResourceRepository, ResourceFile
from prompts.language_prompt_profiles import get_locale_prompt_profile
from prompts.translation_review_prompts import build_key_alignment_repair_prompt

DEFAULT_OUTPUT_DIRECTORY = DEFAULT_TRANSLATION_KEY_ALIGNMENT_DIRECTORY


@dataclass(frozen=True)
class TranslationKeyAlignmentResult:
    exit_code: int
    output_dir: Path
    alignment_issue_count: int
    stale_issue_count: int
    report_file_count: int
    task_json_paths: tuple[str, ...]


class TranslationKeyAlignmentChecker:
    def __init__(
        self,
        *,
        res_dir: Path | str = DEFAULT_RES_DIRECTORY,
        output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
    ) -> None:
        self.repository = AndroidStringResourceRepository(res_dir)
        self.output_manager = OutputDirectoryManager(output_dir)
        self.writer = MinimalMarkdownReportWriter()

    def run(
        self,
        *,
        lang: str | None = None,
        fail_on_stale: bool = False,
        quiet: bool = False,
        emit_text: bool = True,
    ) -> TranslationKeyAlignmentResult:
        self.repository.ensure_base_directory()
        base_files = self.repository.load_base_resource_files()
        self.output_manager.reset()

        total_issues = 0
        stale_issues = 0
        written_reports = 0
        task_json_paths: list[str] = []

        normalized_lang = lang.strip() if lang else None
        for lang_code, folder_path in self.repository.iter_localized_directories():
            if normalized_lang and lang_code != normalized_lang:
                continue
            file_blocks, task_entries, stale_count = self._build_language_issue_blocks(lang_code, folder_path, base_files)
            if not file_blocks:
                continue
            profile = get_locale_prompt_profile(lang_code)
            prompt = build_key_alignment_repair_prompt(lang_code, display_language_tag(lang_code))

            output_file = self.output_manager.output_dir / lang_code / f"{lang_code}_translation_tasks.md"
            self.writer.write(
                output_file,
                title=f"{display_language_tag(lang_code)} Translation Tasks",
                section=f"{display_language_tag(lang_code)} vs EN",
                prompt=prompt,
                metadata_lines=(
                    f"TOTAL_ISSUES: {sum(len(block.key_blocks) for block in file_blocks)}",
                    f"DIR: values-{lang_code}",
                    f"LOCALE_PROFILE: {profile.profile_id}",
                    f"LOCALE_MODE: {profile.mode}",
                    f"LOCALE_NOTE: {profile.locale_note}",
                    "NOTE: Pro sample keys that contain '_ascii_' are intentionally excluded. They are fixed ASCII protocol samples and are not translation tasks.",
                ),
                file_blocks=tuple(file_blocks),
            )
            task_json_paths.append(
                self._write_key_alignment_task_json(
                    lang_code=lang_code,
                    prompt=prompt,
                    profile=profile,
                    task_entries=task_entries,
                )
            )
            total_issues += sum(len(block.key_blocks) for block in file_blocks)
            stale_issues += stale_count
            written_reports += 1

        if total_issues == 0:
            output_file = self.output_manager.output_dir / "translation_tasks_ok.md"
            output_file.write_text(
                "# Translation Tasks OK\n\nOK: No missing or extra localized translation keys found.\n",
                encoding="utf-8",
            )
            written_reports = 1

        if emit_text and not quiet:
            print(f"Done. Alignment issues: {total_issues}")
            print(f"Stale issues (localized-only keys/files): {stale_issues}")
            print(f"Reports generated under: {self.output_manager.output_dir} ({written_reports} files)")
        if total_issues == 0:
            exit_code = 0
        elif fail_on_stale:
            exit_code = 2 if stale_issues > 0 else 0
        else:
            exit_code = 2
        return TranslationKeyAlignmentResult(
            exit_code=exit_code,
            output_dir=self.output_manager.output_dir,
            alignment_issue_count=total_issues,
            stale_issue_count=stale_issues,
            report_file_count=written_reports,
            task_json_paths=tuple(task_json_paths),
        )

    def _build_language_issue_blocks(
        self,
        lang_code: str,
        folder_path: Path,
        base_files: dict[str, ResourceFile],
    ) -> tuple[list[ReportFileBlock], list[dict[str, object]], int]:
        localized_xml_paths = iter_translation_text_xml_paths(folder_path)
        localized_by_name = {path.name: path for path in localized_xml_paths}

        file_blocks: list[ReportFileBlock] = []
        task_entries: list[dict[str, object]] = []
        stale_count = 0

        extra_filenames = sorted(name for name in localized_by_name if name not in base_files)
        for filename in extra_filenames:
            stale_count += 1
            file_blocks.append(
                ReportFileBlock(
                    filename=filename,
                    key_blocks=(
                        ReportKeyBlock(
                            key="__extra_file__",
                            fields=(("ISSUE", "localized file has no English base counterpart"),),
                        ),
                    ),
                )
            )
            task_entries.append(
                {
                    "issue": "localized file has no English base counterpart",
                    "dir": f"values-{lang_code}",
                    "xml": f"values-{lang_code}/{filename}",
                    "name": "__extra_file__",
                    "localized": "",
                    "action": "inspect_only",
                }
            )

        for filename, resource_file in sorted(base_files.items()):
            # Pro sample entries are intentionally ASCII-only across every locale in the app.
            # They are protocol-style fixed inputs rather than target-language translation work,
            # so key-alignment should ignore them on both sides:
            # - if a locale omits them, that is not a missing translation
            # - if a locale keeps the same ASCII-only keys, that is not an extra localized key
            # Only non-Pro translation keys participate in the English-subset contract.
            #
            # High Gothic (`values-la`) is also handled here as a localized output
            # rather than an English baseline. Its prose intentionally blends Latin
            # and English for style, but structurally it still has to line up with
            # the real English `values/` resource set like every other locale.
            base_keys = {
                key
                for key in resource_file.strings.keys()
                if not is_pro_sample_key(key)
            }
            if filename not in localized_by_name:
                if not base_keys:
                    continue
                example_keys = sorted(base_keys)[:5]
                file_blocks.append(
                    ReportFileBlock(
                        filename=filename,
                        key_blocks=(
                            ReportKeyBlock(
                                key="__missing_file__",
                                fields=(
                                    ("ISSUE", "localized file is missing for English base counterpart"),
                                    ("MISSING_KEY_COUNT", str(len(base_keys))),
                                    ("EXAMPLE_KEYS", ", ".join(example_keys)),
                                ),
                            ),
                        ),
                    )
                )
                task_entries.append(
                    {
                        "issue": "localized file is missing for English base counterpart",
                        "dir": f"values-{lang_code}",
                        "xml": f"values-{lang_code}/{filename}",
                        "name": "__missing_file__",
                        "missing_key_count": len(base_keys),
                        "example_keys": example_keys,
                        "action": "create_file_if_requested",
                    }
                )
                continue

            localized_strings = self.repository.load_localized_strings(folder_path, filename)
            localized_order = self.repository.load_localized_string_order(folder_path, filename)
            localized_keys = {
                key
                for key in localized_strings.keys()
                if not is_pro_sample_key(key)
            }

            missing_key_set = base_keys - localized_keys
            missing_keys = [key for key in resource_file.strings.keys() if key in missing_key_set]
            extra_keys = sorted(localized_keys - base_keys)
            if not missing_keys and not extra_keys:
                continue

            key_blocks: list[ReportKeyBlock] = []
            previous_missing_keys: set[str] = set()
            for key in missing_keys:
                insert_after = self._find_insert_after_key(key, resource_file, localized_keys | previous_missing_keys)
                key_blocks.append(
                    ReportKeyBlock(
                        key=key,
                        fields=tuple(
                            field
                            for field in (
                                ("ISSUE", "missing localized translation for English base key"),
                                (
                                    "CONTEXT",
                                    resource_file.contexts.get(key),
                                ),
                                ("EN", resource_file.strings[key]),
                            )
                            if field[1]
                        ),
                    )
                )
                task_entries.append(
                    {
                        "issue": "missing localized translation for English base key",
                        "dir": f"values-{lang_code}",
                        "xml": f"values-{lang_code}/{filename}",
                        "name": key,
                        "context": resource_file.contexts.get(key),
                        "en": resource_file.strings[key],
                        "localized": "",
                        "suggested_insert_after": insert_after,
                        "nearby_localized_terms": self._nearby_localized_terms(
                            key=key,
                            resource_file=resource_file,
                            localized_strings=localized_strings,
                            localized_order=localized_order,
                        ),
                        "action": "add_localized_string",
                    }
                )
                previous_missing_keys.add(key)
            for key in extra_keys:
                stale_count += 1
                key_blocks.append(
                    ReportKeyBlock(
                        key=key,
                        fields=(
                            ("ISSUE", "localized key exists but English base key is missing"),
                            ("TR", localized_strings[key]),
                        ),
                    )
                )
                task_entries.append(
                    {
                        "issue": "localized key exists but English base key is missing",
                        "dir": f"values-{lang_code}",
                        "xml": f"values-{lang_code}/{filename}",
                        "name": key,
                        "localized": localized_strings[key],
                        "action": "inspect_only",
                    }
                )
            file_blocks.append(ReportFileBlock(filename=filename, key_blocks=tuple(key_blocks)))

        return file_blocks, task_entries, stale_count

    def _write_key_alignment_task_json(
        self,
        *,
        lang_code: str,
        prompt: str,
        profile,
        task_entries: list[dict[str, object]],
    ) -> str:
        output_path = self.output_manager.output_dir / lang_code / f"{lang_code}_key_alignment.task.json"
        payload = {
            "task_version": 1,
            "task_type": "key_alignment_repair",
            "language": lang_code,
            "language_tag": display_language_tag(lang_code),
            "locale_profile": {
                "id": profile.profile_id,
                "mode": profile.mode,
                "note": profile.locale_note,
                "identity_rule": profile.identity_rule,
                "app_text_rule": profile.app_text_rule,
                "sample_text_rule": profile.sample_text_rule,
                "key_alignment_rule": profile.key_alignment_rule,
            },
            "prompt": prompt,
            "entry_count": len(task_entries),
            "entries": task_entries,
        }
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return str(output_path)

    @staticmethod
    def _find_insert_after_key(
        key: str,
        resource_file: ResourceFile,
        localized_keys: set[str],
    ) -> str | None:
        ordered_keys = list(resource_file.strings.keys())
        try:
            key_index = ordered_keys.index(key)
        except ValueError:
            return None
        for previous_key in reversed(ordered_keys[:key_index]):
            if previous_key in localized_keys:
                return previous_key
        return None

    @staticmethod
    def _nearby_localized_terms(
        *,
        key: str,
        resource_file: ResourceFile,
        localized_strings: dict[str, str],
        localized_order: list[str],
    ) -> list[dict[str, str]]:
        del localized_order
        ordered_keys = list(resource_file.strings.keys())
        try:
            key_index = ordered_keys.index(key)
        except ValueError:
            return []
        nearby: list[dict[str, str]] = []
        for candidate in ordered_keys[max(0, key_index - 3): key_index + 4]:
            if candidate == key or candidate not in localized_strings:
                continue
            nearby.append({"name": candidate, "localized": localized_strings[candidate]})
        return nearby


def run_translation_key_alignment_check(
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
    lang: str | None = None,
    fail_on_stale: bool = False,
    quiet: bool = False,
    emit_text: bool = True,
) -> TranslationKeyAlignmentResult:
    checker = TranslationKeyAlignmentChecker(res_dir=res_dir, output_dir=output_dir)
    return checker.run(
        lang=lang,
        fail_on_stale=fail_on_stale,
        quiet=quiet,
        emit_text=emit_text,
    )
