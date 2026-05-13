from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    DEFAULT_TRANSLATION_XML_DUMPS_DIRECTORY,
    TEXT_TYPES,
    get_review_groups_for_text_type,
)
from core.translation_resources import AndroidStringResourceRepository

DEFAULT_OUTPUT_DIRECTORY = DEFAULT_TRANSLATION_XML_DUMPS_DIRECTORY


@dataclass(frozen=True)
class DumpXmlMdResult:
    exit_code: int
    output_dir: Path
    files_written: int
    error: str | None = None


class XmlTextDumpGenerator:
    def __init__(
        self,
        *,
        res_dir: Path | str = DEFAULT_RES_DIRECTORY,
        output_dir: Path | str | None = DEFAULT_OUTPUT_DIRECTORY,
        lang_filter: str | None = None,
        text_type_filter: str | None = None,
        group_filter: str | None = None,
        with_en: bool = False,
        no_clean: bool = False,
    ) -> None:
        self.repository = AndroidStringResourceRepository(res_dir)
        self.output_dir = Path(output_dir or DEFAULT_OUTPUT_DIRECTORY)
        self.lang_filter = lang_filter.strip() if lang_filter else None
        self.text_type_filter = text_type_filter.strip() if text_type_filter else None
        self.group_filter = group_filter.strip() if group_filter else None
        self.with_en = with_en
        self.no_clean = no_clean

    def run(self, *, quiet: bool = False, emit_text: bool = True) -> DumpXmlMdResult:
        self.repository.ensure_base_directory()
        if self.text_type_filter and self.text_type_filter not in TEXT_TYPES:
            return DumpXmlMdResult(
                exit_code=1,
                output_dir=self.output_dir,
                files_written=0,
                error=f"Unsupported text type filter: {self.text_type_filter}",
            )

        valid_groups = set(get_review_groups_for_text_type("app_text")) | set(get_review_groups_for_text_type("sample_text"))
        if self.group_filter and self.group_filter not in valid_groups:
            return DumpXmlMdResult(
                exit_code=1,
                output_dir=self.output_dir,
                files_written=0,
                error=f"Unsupported group filter: {self.group_filter}",
            )

        localized_dirs = dict(self.repository.iter_localized_directories())
        if self.lang_filter:
            if self.lang_filter not in localized_dirs:
                return DumpXmlMdResult(
                    exit_code=1,
                    output_dir=self.output_dir,
                    files_written=0,
                    error=f"Unsupported lang filter: {self.lang_filter}",
                )
            localized_dirs = {self.lang_filter: localized_dirs[self.lang_filter]}

        if not self.no_clean and self.output_dir.exists():
            for child in self.output_dir.glob("**/*"):
                if child.is_file():
                    child.unlink()
            for child in sorted(self.output_dir.glob("**/*"), reverse=True):
                if child.is_dir():
                    child.rmdir()

        self.output_dir.mkdir(parents=True, exist_ok=True)

        base_files = self.repository.load_base_resource_files()
        files_written = 0

        # Include values/ baseline by default for direct text inspection.
        files_written += self._write_for_folder("values", self.repository.base_values_dir, base_files)

        for lang_code, folder in sorted(localized_dirs.items(), key=lambda x: x[0]):
            files_written += self._write_for_folder(f"values-{lang_code}", folder, base_files)

        if emit_text and not quiet:
            print(f"Output directory: {self.output_dir}")
            print(f"Generated {files_written} dump files")

        return DumpXmlMdResult(
            exit_code=0,
            output_dir=self.output_dir,
            files_written=files_written,
        )

    def _write_for_folder(self, folder_name: str, folder_path: Path, base_files: dict[str, object]) -> int:
        written = 0
        out_lang_dir = self.output_dir / folder_name
        for xml_name in sorted(name for name in base_files if (folder_path / name).exists()):
            parsed = self.repository.extract_strings_from_xml(folder_path / xml_name)
            if not parsed.strings:
                continue

            # Use baseline file metadata to classify group/text type.
            base_meta = base_files[xml_name]
            if self.text_type_filter and base_meta.text_type != self.text_type_filter:
                continue
            if self.group_filter and base_meta.faction != self.group_filter:
                continue

            out_file = out_lang_dir / base_meta.text_type / f"{xml_name}.md"
            out_file.parent.mkdir(parents=True, exist_ok=True)
            lines: list[str] = []
            lines.append(f"DIR: {folder_name}")
            lines.append(f"XML: {folder_name}/{xml_name}")
            for key, text in parsed.strings.items():
                lines.append("")
                lines.append(f"NAME: {key}")
                sample_length = base_meta.sample_lengths.get(key)
                if sample_length is not None:
                    lines.append(f"SAMPLE_LENGTH: {sample_length}")
                if self.with_en:
                    en_text = base_meta.strings.get(key, "")
                    lines.append(f"EN: {en_text}")
                lines.append(f"TEXT: {text}")
            out_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
            written += 1
        return written


def generate_xml_text_dump(
    *,
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str | None = DEFAULT_OUTPUT_DIRECTORY,
    lang: str | None = None,
    text_type: str | None = None,
    group: str | None = None,
    with_en: bool = False,
    no_clean: bool = False,
    quiet: bool = False,
    emit_text: bool = True,
) -> DumpXmlMdResult:
    generator = XmlTextDumpGenerator(
        res_dir=res_dir,
        output_dir=output_dir,
        lang_filter=lang,
        text_type_filter=text_type,
        group_filter=group,
        with_en=with_en,
        no_clean=no_clean,
    )
    return generator.run(quiet=quiet, emit_text=emit_text)
