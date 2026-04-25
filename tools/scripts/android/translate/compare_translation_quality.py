from __future__ import annotations

from pathlib import Path

from translation_common import (
    DEFAULT_RES_DIRECTORY,
    FACTIONS,
    AndroidStringResourceRepository,
    MinimalMarkdownReportWriter,
    OutputDirectoryManager,
    ReportFileBlock,
    ReportKeyBlock,
    ResourceFile,
    TEXT_TYPES,
    display_language_tag,
    humanize_name,
    is_pro_sample_key,
)

DEFAULT_OUTPUT_DIRECTORY = Path(__file__).resolve().parents[4] / "temp" / "ai_translation_reviews"
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


class TranslationQualityReportGenerator:
    def __init__(
        self,
        *,
        res_dir: Path | str = DEFAULT_RES_DIRECTORY,
        output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
    ) -> None:
        self.repository = AndroidStringResourceRepository(res_dir)
        self.output_manager = OutputDirectoryManager(output_dir)
        self.writer = MinimalMarkdownReportWriter()

    def run(self) -> None:
        self.repository.ensure_base_directory()
        base_files = self.repository.load_base_resource_files(
            # Pro 的 sample 文本是固定 ASCII 输入，不应该进入翻译质量对比。
            string_filter=lambda key, _text: not is_pro_sample_key(key),
        )
        if not base_files:
            print(f"错误: 英文目录没有可对比的 XML 文本 {self.repository.base_values_dir}")
            return

        self.output_manager.reset()
        print(f"Output directory: {self.output_manager.output_dir}\n")

        english_written_count = self._write_english_source_reviews(base_files)
        print(f"Generated {english_written_count} English source review files")

        for lang_code, folder_path in self._iter_review_directories():
            written_count = self._write_language_reviews(lang_code, folder_path, base_files)
            if written_count > 0:
                print(f"Generated {written_count} review files for {display_language_tag(lang_code)}")

    def _iter_review_directories(self) -> list[tuple[str, Path]]:
        localized_dirs = dict(self.repository.iter_localized_directories())
        ordered_lang_codes = [lang_code for lang_code in REVIEW_LANGUAGE_ORDER if lang_code in localized_dirs]
        remaining_lang_codes = sorted(lang_code for lang_code in localized_dirs if lang_code not in REVIEW_LANGUAGE_ORDER)
        return [(lang_code, localized_dirs[lang_code]) for lang_code in ordered_lang_codes + remaining_lang_codes]

    def _write_english_source_reviews(self, base_files: dict[str, ResourceFile]) -> int:
        english_output_dir = self.output_manager.output_dir / "en"
        written_count = 0

        for text_type in TEXT_TYPES:
            for faction in FACTIONS + ("other",):
                file_blocks = self._build_english_file_blocks(base_files, text_type=text_type, faction=faction)
                if not file_blocks:
                    continue

                output_file = english_output_dir / text_type / f"{faction}.md"
                self.writer.write(
                    output_file,
                    title="English Source Review",
                    section=f"{humanize_name(text_type)} / {humanize_name(faction)}",
                    prompt=(
                        "Evaluate whether the English source lines are clear, natural, concise, and tonally consistent. "
                        "Do not compare against a target language. Focus on source wording quality, style, and lineup tone. "
                        "For sample text, preserve the intended length class: SHORT samples should remain short, and LONG samples should remain long. "
                        'These strings will be written into Android XML resources. Do not introduce backslash-style escapes such as \\\' or \\" inside the text; use the literal punctuation character instead. '
                        "Return JSON only. Use this format: "
                        '[{"original":"current source text","optimized":"improved source text","comment":"short reason"}]. '
                        "If no changes are needed, return []."
                    ),
                    file_blocks=file_blocks,
                )
                written_count += 1

        return written_count

    def _write_language_reviews(
        self,
        lang_code: str,
        folder_path: Path,
        base_files: dict[str, ResourceFile],
    ) -> int:
        xml_names = self.repository.localized_xml_names(folder_path, base_files)
        if not xml_names:
            return 0

        available_files = {name: base_files[name] for name in xml_names}
        lang_output_dir = self.output_manager.output_dir / lang_code
        language_tag = display_language_tag(lang_code)
        written_count = 0

        for text_type in TEXT_TYPES:
            for faction in FACTIONS + ("other",):
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
                self.writer.write(
                    output_file,
                    title=f"Translation Review EN vs [{language_tag}]",
                    section=f"{humanize_name(text_type)} / {humanize_name(faction)}",
                    prompt=(
                        f"Evaluate whether the [{language_tag}] lines are natural and suitable for [{language_tag}]. "
                        "Do not require strict word-for-word translation from English. "
                        f"Prefer wording that fits [{language_tag}] sentence structure, grammar, rhythm, pronunciation, and idiom. "
                        "Meaning only needs to be broadly consistent with English while preserving lineup tone. "
                        "For sample text, preserve the intended length class: SHORT samples should remain short, and LONG samples should remain long. "
                        'These strings will be written into Android XML resources. Do not introduce backslash-style escapes such as \\\' or \\" inside the text; use the literal punctuation character instead. '
                        "Return JSON only. Use this format: "
                        '[{"original":"current translated text","optimized":"improved translated text","comment":"short reason"}]. '
                        "If no changes are needed, return []."
                    ),
                    file_blocks=file_blocks,
                )
                written_count += 1

        return written_count

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
        sample_length = resource_file.sample_lengths.get(key)
        if sample_length is not None:
            fields.append(("SAMPLE_LENGTH", sample_length))
        fields.append(("EN", en_text))
        return tuple(fields)

    def _build_translation_fields(
        self,
        resource_file: ResourceFile,
        key: str,
        en_text: str,
        translated_text: str,
        language_tag: str,
    ) -> tuple[tuple[str, str], ...]:
        fields: list[tuple[str, str]] = []
        sample_length = resource_file.sample_lengths.get(key)
        if sample_length is not None:
            fields.append(("SAMPLE_LENGTH", sample_length))
        fields.append(("EN", en_text))
        fields.append((language_tag, translated_text))
        return tuple(fields)


def generate_comparisons_for_res(
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
) -> None:
    TranslationQualityReportGenerator(res_dir=res_dir, output_dir=output_dir).run()
