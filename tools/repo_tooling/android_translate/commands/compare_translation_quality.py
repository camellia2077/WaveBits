from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

from core.translation_paths import (
    APP_TEXT_GROUPS,
    DEFAULT_RES_DIRECTORY,
    DEFAULT_TRANSLATION_REVIEWS_DIRECTORY,
    FACTIONS,
    TEXT_TYPES,
    display_language_tag,
    get_review_groups_for_text_type,
    humanize_name,
    is_pro_sample_key,
)
from core.translation_agent_tasks import (
    build_agent_task_entry,
    build_agent_task_payload,
    write_agent_task_payload,
)
from core.translation_reporting import (
    MinimalMarkdownReportWriter,
    OutputDirectoryManager,
    ReportFileBlock,
    ReportKeyBlock,
)
from core.translation_resources import AndroidStringResourceRepository, ResourceFile
from prompts.language_prompt_profiles import get_locale_prompt_profile
from prompts.sample_text_profiles import get_sample_text_style_profile
from prompts.translation_review_prompts import (
    build_english_source_review_prompt_for_text_type,
    build_manual_english_source_review_prompt_for_text_type,
    build_manual_translation_review_prompt_for_text_type,
    build_translation_review_prompt_for_text_type,
    get_supported_prompt_modes,
    PROMPT_VERSION,
)

DEFAULT_OUTPUT_DIRECTORY = DEFAULT_TRANSLATION_REVIEWS_DIRECTORY
REVIEW_LANGUAGE_ORDER = (
    "de",
    "es",
    "fr",
    "ja",
    "ko",
    "pt-rBR",
    "ru",
    "uk",
    "zh",
    "zh-rTW",
)


@dataclass(frozen=True)
class CompareGenerationResult:
    exit_code: int
    output_dir: Path
    english_review_files: int
    localized_review_files: int
    localized_languages: int
    english_review_paths: tuple[str, ...]
    localized_review_paths: tuple[str, ...]
    prompt_doc_paths: tuple[str, ...]
    task_json_paths: tuple[str, ...]
    prompt_mode: str
    prompt_version: str
    error: str | None = None


class TranslationQualityReportGenerator:
    def __init__(
        self,
        *,
        res_dir: Path | str = DEFAULT_RES_DIRECTORY,
        output_dir: Path | str | None = DEFAULT_OUTPUT_DIRECTORY,
        lang_filter: str | None = None,
        text_type_filter: str | None = None,
        group_filter: str | None = None,
        prompt_mode: str = "agent_json",
        no_clean: bool = False,
    ) -> None:
        self.repository = AndroidStringResourceRepository(res_dir)
        self.output_manager = OutputDirectoryManager(output_dir or DEFAULT_OUTPUT_DIRECTORY)
        self.writer = MinimalMarkdownReportWriter()
        self.lang_filter = lang_filter.strip() if lang_filter else None
        self.text_type_filter = text_type_filter.strip() if text_type_filter else None
        self.group_filter = group_filter.strip() if group_filter else None
        self.prompt_mode = prompt_mode.strip() if prompt_mode else "agent_json"
        self.no_clean = no_clean
        self.generated_at = datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")

    def run(
        self,
        *,
        quiet: bool = False,
        emit_text: bool = True,
    ) -> CompareGenerationResult:
        self.repository.ensure_base_directory()
        self._validate_filters()
        base_files = self.repository.load_base_resource_files(
            # Pro 的 sample 文本是固定 ASCII 输入，不应该进入翻译质量对比。
            string_filter=lambda key, _text: not is_pro_sample_key(key),
        )
        if not base_files:
            error = f"错误: 英文目录没有可对比的 XML 文本 {self.repository.base_values_dir}"
            if emit_text:
                print(error)
            return CompareGenerationResult(
                exit_code=1,
                output_dir=self.output_manager.output_dir,
                english_review_files=0,
                localized_review_files=0,
                localized_languages=0,
                english_review_paths=(),
                localized_review_paths=(),
                prompt_doc_paths=(),
                task_json_paths=(),
                prompt_mode=self.prompt_mode,
                prompt_version=PROMPT_VERSION,
                error=error,
            )

        if self.no_clean:
            self.output_manager.ensure()
        else:
            self.output_manager.reset()
        if emit_text and not quiet:
            print(f"Output directory: {self.output_manager.output_dir}\n")

        prompt_doc_paths = self._write_shared_prompt_documents(base_files)
        english_written_paths = self._write_english_source_reviews(base_files)
        task_json_paths: list[str] = []
        english_written_count = len(english_written_paths)
        if emit_text and not quiet:
            print(f"Generated {english_written_count} English source review files")

        localized_review_files = 0
        localized_languages = 0
        localized_written_paths: list[str] = []
        for lang_code, folder_path in self._iter_review_directories():
            written_paths = self._write_language_reviews(lang_code, folder_path, base_files)
            task_json_paths.extend(self._write_agent_task_json(lang_code, folder_path, base_files))
            written_count = len(written_paths)
            if written_count > 0:
                localized_review_files += written_count
                localized_languages += 1
                localized_written_paths.extend(written_paths)
                if emit_text and not quiet:
                    print(f"Generated {written_count} review files for {display_language_tag(lang_code)}")

        return CompareGenerationResult(
            exit_code=0,
            output_dir=self.output_manager.output_dir,
            english_review_files=english_written_count,
            localized_review_files=localized_review_files,
            localized_languages=localized_languages,
            english_review_paths=tuple(english_written_paths),
            localized_review_paths=tuple(localized_written_paths),
            prompt_doc_paths=tuple(prompt_doc_paths),
            task_json_paths=tuple(task_json_paths),
            prompt_mode=self.prompt_mode,
            prompt_version=PROMPT_VERSION,
        )

    def _iter_review_directories(self) -> list[tuple[str, Path]]:
        localized_dirs = dict(self.repository.iter_localized_directories())
        if self.lang_filter:
            if self.lang_filter not in localized_dirs:
                return []
            localized_dirs = {self.lang_filter: localized_dirs[self.lang_filter]}
        ordered_lang_codes = [lang_code for lang_code in REVIEW_LANGUAGE_ORDER if lang_code in localized_dirs]
        remaining_lang_codes = sorted(lang_code for lang_code in localized_dirs if lang_code not in REVIEW_LANGUAGE_ORDER)
        return [(lang_code, localized_dirs[lang_code]) for lang_code in ordered_lang_codes + remaining_lang_codes]

    def _validate_filters(self) -> None:
        supported_prompt_modes = get_supported_prompt_modes()
        if self.prompt_mode not in supported_prompt_modes:
            raise ValueError(
                f"Unsupported prompt mode: {self.prompt_mode}. Expected one of: {', '.join(supported_prompt_modes)}"
            )

        available_lang_codes = {lang_code for lang_code, _path in self.repository.iter_localized_directories()}
        if self.lang_filter is not None and self.lang_filter not in available_lang_codes:
            raise ValueError(
                f"Unsupported lang filter: {self.lang_filter}. Expected one of: {', '.join(sorted(available_lang_codes))}"
            )

        if self.text_type_filter is not None and self.text_type_filter not in TEXT_TYPES:
            raise ValueError(
                f"Unsupported text type filter: {self.text_type_filter}. Expected one of: {', '.join(TEXT_TYPES)}"
            )

        valid_groups = set(FACTIONS) | set(APP_TEXT_GROUPS) | {"other"}
        if self.group_filter is not None and self.group_filter not in valid_groups:
            raise ValueError(
                f"Unsupported group filter: {self.group_filter}. Expected one of: {', '.join(sorted(valid_groups))}"
            )

    def _write_english_source_reviews(self, base_files: dict[str, ResourceFile]) -> list[str]:
        english_output_dir = self.output_manager.output_dir / "en"
        written_paths: list[str] = []

        for text_type in TEXT_TYPES:
            if self.text_type_filter is not None and text_type != self.text_type_filter:
                continue
            prompt_path = self._shared_prompt_path("en", text_type)
            for faction in get_review_groups_for_text_type(text_type):
                if self.group_filter is not None and faction != self.group_filter:
                    continue
                file_blocks = self._build_english_file_blocks(base_files, text_type=text_type, faction=faction)
                if not file_blocks:
                    continue

                output_file = english_output_dir / text_type / f"{faction}.md"
                prompt_ref = self._prompt_doc_relative_path(output_file, prompt_path)
                self.writer.write(
                    output_file,
                    title="English Source Review",
                    section=f"{humanize_name(text_type)} / {humanize_name(faction)}",
                    prompt=None,
                    metadata_lines=(
                        "NOTE: Pro sample keys that contain '_ascii_' are intentionally excluded here. They are fixed ASCII protocol samples and are not translation tasks.",
                        f"PROMPT_MODE: {self.prompt_mode}",
                        f"PROMPT_VERSION: {PROMPT_VERSION}",
                        f"GENERATED_AT: {self.generated_at}",
                        f"PROMPT_REF: {prompt_ref}",
                    ),
                    file_blocks=file_blocks,
                )
                written_paths.append(str(output_file))

        return written_paths

    def _write_language_reviews(
        self,
        lang_code: str,
        folder_path: Path,
        base_files: dict[str, ResourceFile],
    ) -> list[str]:
        xml_names = self.repository.localized_xml_names(folder_path, base_files)
        if not xml_names:
            return []

        available_files = {name: base_files[name] for name in xml_names}
        lang_output_dir = self.output_manager.output_dir / lang_code
        language_tag = display_language_tag(lang_code)
        profile = get_locale_prompt_profile(lang_code)
        written_paths: list[str] = []

        for text_type in TEXT_TYPES:
            if self.text_type_filter is not None and text_type != self.text_type_filter:
                continue
            prompt_path = self._shared_prompt_path(lang_code, text_type)
            for faction in get_review_groups_for_text_type(text_type):
                if self.group_filter is not None and faction != self.group_filter:
                    continue
                file_blocks = self._build_translation_file_blocks(
                    available_files,
                    folder_path=folder_path,
                    text_type=text_type,
                    faction=faction,
                    language_tag=language_tag,
                )
                if not file_blocks:
                    continue

                output_file = lang_output_dir / text_type / f"{faction}.md"
                prompt_ref = self._prompt_doc_relative_path(output_file, prompt_path)
                self.writer.write(
                    output_file,
                    title=f"Translation Review EN vs [{language_tag}]",
                    section=f"{humanize_name(text_type)} / {humanize_name(faction)}",
                    prompt=None,
                    metadata_lines=(
                        "NOTE: Pro sample keys that contain '_ascii_' are intentionally excluded here. They are fixed ASCII protocol samples and are not translation tasks.",
                        f"LOCALE_PROFILE: {profile.profile_id}",
                        f"LOCALE_MODE: {profile.mode}",
                        f"LOCALE_NOTE: {profile.locale_note}",
                        f"PROMPT_MODE: {self.prompt_mode}",
                        f"PROMPT_VERSION: {PROMPT_VERSION}",
                        f"GENERATED_AT: {self.generated_at}",
                        f"PROMPT_REF: {prompt_ref}",
                    ),
                    file_blocks=file_blocks,
                )
                written_paths.append(str(output_file))

        return written_paths

    def _write_agent_task_json(
        self,
        lang_code: str,
        folder_path: Path,
        base_files: dict[str, ResourceFile],
    ) -> list[str]:
        xml_names = self.repository.localized_xml_names(folder_path, base_files)
        if not xml_names:
            return []

        available_files = {name: base_files[name] for name in xml_names}
        lang_output_dir = self.output_manager.output_dir / lang_code
        profile = get_locale_prompt_profile(lang_code)
        written_paths: list[str] = []

        for text_type in TEXT_TYPES:
            if self.text_type_filter is not None and text_type != self.text_type_filter:
                continue
            prompt_path = self._shared_prompt_path(lang_code, text_type)
            for faction in get_review_groups_for_text_type(text_type):
                if self.group_filter is not None and faction != self.group_filter:
                    continue
                entries: list[object] = []
                for resource_file in self._iter_matching_files(available_files, text_type=text_type, faction=faction):
                    trans_dict = self.repository.load_localized_strings(folder_path, resource_file.filename)
                    for key, en_text in resource_file.strings.items():
                        entries.append(
                            build_agent_task_entry(
                                folder_name=folder_path.name,
                                resource_file=resource_file,
                                key=key,
                                english_text=en_text,
                                localized_text=trans_dict.get(key, "[MISSING TRANSLATION / 此条目未翻译]"),
                            )
                        )
                if not entries:
                    continue
                output_path = lang_output_dir / text_type / f"{faction}.task.json"
                payload = build_agent_task_payload(
                    output_dir=self.output_manager.output_dir,
                    prompt_mode=self.prompt_mode,
                    prompt_version=PROMPT_VERSION,
                    generated_at=self.generated_at,
                    language_code=lang_code,
                    text_type=text_type,
                    group=faction,
                    prompt_text_type=text_type,
                    prompt_doc_path=prompt_path,
                    locale_profile=profile,
                    style_profile=get_sample_text_style_profile(
                        locale_code=lang_code,
                        text_type=text_type,
                        group=faction,
                    ),
                    entries=entries,
                )
                written_paths.append(write_agent_task_payload(output_path, payload))
        return written_paths

    def _write_shared_prompt_documents(self, base_files: dict[str, ResourceFile]) -> list[str]:
        written_paths: list[str] = []
        for text_type in TEXT_TYPES:
            if self.text_type_filter is not None and text_type != self.text_type_filter:
                continue
            english_prompt_path = self._shared_prompt_path("en", text_type)
            self._write_prompt_document(
                english_prompt_path,
                title="Shared Review Prompt",
                scope=f"EN_SOURCE/{text_type}",
                prompt=self._build_english_prompt(text_type),
            )
            written_paths.append(str(english_prompt_path))

        for lang_code, _folder_path in self._iter_review_directories():
            language_tag = display_language_tag(lang_code)
            profile = get_locale_prompt_profile(lang_code)
            for text_type in TEXT_TYPES:
                if self.text_type_filter is not None and text_type != self.text_type_filter:
                    continue
                prompt_path = self._shared_prompt_path(lang_code, text_type)
                self._write_prompt_document(
                    prompt_path,
                    title="Shared Review Prompt",
                    scope=f"{language_tag}/{text_type}",
                    prompt=self._build_translation_prompt(lang_code, language_tag, text_type),
                    metadata_lines=(
                        f"LOCALE_PROFILE: {profile.profile_id}",
                        f"LOCALE_MODE: {profile.mode}",
                        f"LOCALE_NOTE: {profile.locale_note}",
                    ),
                )
                written_paths.append(str(prompt_path))
        return written_paths

    def _write_prompt_document(
        self,
        output_path: Path,
        *,
        title: str,
        scope: str,
        prompt: str,
        metadata_lines: tuple[str, ...] = (),
    ) -> None:
        output_path.parent.mkdir(parents=True, exist_ok=True)
        lines = [
            f"# {title}",
            "",
            f"PROMPT_MODE: {self.prompt_mode}",
            f"PROMPT_VERSION: {PROMPT_VERSION}",
            f"GENERATED_AT: {self.generated_at}",
            f"SCOPE: {scope}",
        ]
        lines.extend(metadata_lines)
        lines.extend(
            [
            f"PROMPT: {prompt}",
            "",
            ]
        )
        output_path.write_text("\n".join(lines), encoding="utf-8")

    def _shared_prompt_path(self, language_code: str, text_type: str) -> Path:
        return self.output_manager.output_dir / language_code / "_prompts" / f"{self.prompt_mode}_{text_type}.md"

    def _prompt_doc_relative_path(self, review_path: Path, prompt_path: Path) -> str:
        return str(Path("..") / prompt_path.relative_to(review_path.parent.parent))

    def _build_english_prompt(self, text_type: str) -> str:
        if self.prompt_mode == "manual_notes":
            return build_manual_english_source_review_prompt_for_text_type(text_type)
        return build_english_source_review_prompt_for_text_type(text_type)

    def _build_translation_prompt(self, locale_code: str, language_tag: str, text_type: str) -> str:
        if self.prompt_mode == "manual_notes":
            return build_manual_translation_review_prompt_for_text_type(locale_code, language_tag, text_type)
        return build_translation_review_prompt_for_text_type(locale_code, language_tag, text_type)

    def _build_english_file_blocks(
        self,
        base_files: dict[str, ResourceFile],
        *,
        text_type: str,
        faction: str,
    ) -> tuple[ReportFileBlock, ...]:
        file_blocks = []
        for resource_file in self._iter_matching_files(base_files, text_type=text_type, faction=faction):
            key_blocks = tuple(
                ReportKeyBlock(
                    key=key,
                    fields=self._build_english_fields(resource_file, key, en_text),
                )
                for key, en_text in resource_file.strings.items()
            )
            if key_blocks:
                file_blocks.append(ReportFileBlock(filename=resource_file.filename, key_blocks=key_blocks))
        return tuple(file_blocks)

    def _build_translation_file_blocks(
        self,
        base_files: dict[str, ResourceFile],
        *,
        folder_path: Path,
        text_type: str,
        faction: str,
        language_tag: str,
    ) -> tuple[ReportFileBlock, ...]:
        file_blocks = []
        for resource_file in self._iter_matching_files(base_files, text_type=text_type, faction=faction):
            trans_dict = self.repository.load_localized_strings(folder_path, resource_file.filename)
            key_blocks = tuple(
                ReportKeyBlock(
                    key=key,
                    fields=self._build_translation_fields(
                        resource_file,
                        folder_path,
                        key,
                        en_text,
                        trans_dict.get(key, "[MISSING TRANSLATION / 此条目未翻译]"),
                        language_tag,
                    ),
                )
                for key, en_text in resource_file.strings.items()
            )
            if key_blocks:
                file_blocks.append(ReportFileBlock(filename=resource_file.filename, key_blocks=key_blocks))
        return tuple(file_blocks)

    def _iter_matching_files(
        self,
        base_files: dict[str, ResourceFile],
        *,
        text_type: str,
        faction: str,
    ) -> list[ResourceFile]:
        return [
            resource_file
            for resource_file in sorted(base_files.values(), key=lambda item: item.filename)
            if resource_file.text_type == text_type and resource_file.faction == faction
        ]

    def _build_english_fields(
        self,
        resource_file: ResourceFile,
        key: str,
        en_text: str,
    ) -> tuple[tuple[str, str], ...]:
        fields: list[tuple[str, str]] = []
        fields.append(("DIR", "values"))
        fields.append(("XML", f"values/{resource_file.filename}"))
        fields.append(("NAME", key))
        context = resource_file.contexts.get(key)
        if context:
            fields.append(("CONTEXT", context))
        sample_length = resource_file.sample_lengths.get(key)
        if sample_length is not None:
            fields.append(("SAMPLE_LENGTH", sample_length))
        fields.append(("EN", en_text))
        return tuple(fields)

    def _build_translation_fields(
        self,
        resource_file: ResourceFile,
        folder_path: Path,
        key: str,
        en_text: str,
        translated_text: str,
        language_tag: str,
    ) -> tuple[tuple[str, str], ...]:
        fields: list[tuple[str, str]] = []
        fields.append(("DIR", folder_path.name))
        fields.append(("XML", f"{folder_path.name}/{resource_file.filename}"))
        fields.append(("NAME", key))
        context = resource_file.contexts.get(key)
        if context:
            fields.append(("CONTEXT", context))
        sample_length = resource_file.sample_lengths.get(key)
        if sample_length is not None:
            fields.append(("SAMPLE_LENGTH", sample_length))
        fields.append(("EN", en_text))
        fields.append((language_tag, translated_text))
        return tuple(fields)


def generate_comparisons_for_res(
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str | None = DEFAULT_OUTPUT_DIRECTORY,
    lang: str | None = None,
    text_type: str | None = None,
    group: str | None = None,
    prompt_mode: str = "agent_json",
    no_clean: bool = False,
    quiet: bool = False,
    emit_text: bool = True,
) -> CompareGenerationResult:
    generator = TranslationQualityReportGenerator(
        res_dir=res_dir,
        output_dir=output_dir,
        lang_filter=lang,
        text_type_filter=text_type,
        group_filter=group,
        prompt_mode=prompt_mode,
        no_clean=no_clean,
    )
    return generator.run(quiet=quiet, emit_text=emit_text)
