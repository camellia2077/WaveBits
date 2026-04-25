from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

REPO_ROOT = Path(__file__).resolve().parents[4]
DEFAULT_RES_DIRECTORY = REPO_ROOT / "apps" / "audio_android" / "app" / "src" / "main" / "res"
TEXT_TYPES = ("app_text", "sample_text")
FACTIONS = (
    "ancient_dynasty",
    "exquisite_fall",
    "immortal_rot",
    "labyrinth_of_mutability",
    "sacred_machine",
    "scarlet_carnage",
)


def get_text_type_from_filename(filename: str) -> str:
    """根据文件名将资源分为普通 app 文案或 sample 文案。"""
    if filename.startswith("audio_samples_"):
        return "sample_text"
    return "app_text"


def get_faction_from_filename(filename: str) -> str:
    """根据文件名识别所属的 sample 阵容。"""
    for faction in FACTIONS:
        if filename.startswith(f"audio_samples_{faction}"):
            return faction
    return "other"


def is_pro_sample_key(key: str) -> bool:
    """Pro 示例使用 ASCII 专用资源，不需要做跨语言翻译质量对比。"""
    return key.startswith("audio_sample_") and "_ascii_" in key


def display_language_tag(lang_code: str) -> str:
    """将 Android values-* 目录名转成提示词中展示的语言标签。"""
    if lang_code == "pt-rBR":
        return "PT-BR"
    return lang_code.upper()


def humanize_name(value: str) -> str:
    return value.replace("_", " ").title()


@dataclass(frozen=True)
class ResourceFile:
    filename: str
    text_type: str
    faction: str
    strings: dict[str, str]
    sample_lengths: dict[str, str | None]


def infer_sample_lengths(filename: str, strings: dict[str, str]) -> dict[str, str | None]:
    """
    sample XML 当前遵循固定结构：
    - 先放 themed short
    - 再放 themed long
    - ascii/pro 项目会在上层按需过滤
    因此这里按 themed key 的出现顺序标记 SHORT / LONG，避免审校时把短文翻成长文。
    """
    sample_lengths = {key: None for key in strings}
    if get_text_type_from_filename(filename) != "sample_text":
        return sample_lengths

    themed_keys = [key for key in strings if key.startswith("audio_sample_") and "_themed_" in key]
    if not themed_keys:
        return sample_lengths

    sample_lengths[themed_keys[0]] = "SHORT"
    for key in themed_keys[1:]:
        sample_lengths[key] = "LONG"
    return sample_lengths


class AndroidStringResourceRepository:
    def __init__(self, res_dir: Path | str = DEFAULT_RES_DIRECTORY) -> None:
        self.res_dir = Path(res_dir)
        self.base_values_dir = self.res_dir / "values"

    def ensure_base_directory(self) -> None:
        if not self.base_values_dir.exists():
            raise FileNotFoundError(f"找不到基础英文目录 {self.base_values_dir}")

    def extract_strings_from_xml(self, xml_path: Path) -> dict[str, str]:
        """解析 XML 文件并返回字典：{ 'name': 'text' }"""
        strings_dict: dict[str, str] = {}
        try:
            tree = ET.parse(xml_path)
            for child in tree.getroot().findall("string"):
                name = child.get("name")
                text = "".join(child.itertext()).strip()
                if name:
                    strings_dict[name] = text
        except Exception as exc:
            print(f"解析出错 {xml_path}: {exc}")
        return strings_dict

    def load_base_resource_files(
        self,
        *,
        string_filter: Callable[[str, str], bool] | None = None,
    ) -> dict[str, ResourceFile]:
        resource_files: dict[str, ResourceFile] = {}
        for xml_path in sorted(self.base_values_dir.glob("*.xml")):
            strings = self.extract_strings_from_xml(xml_path)
            if string_filter is not None:
                strings = {
                    key: text
                    for key, text in strings.items()
                    if string_filter(key, text)
                }
            if not strings:
                continue
            resource_files[xml_path.name] = ResourceFile(
                filename=xml_path.name,
                text_type=get_text_type_from_filename(xml_path.name),
                faction=get_faction_from_filename(xml_path.name),
                strings=strings,
                sample_lengths=infer_sample_lengths(xml_path.name, strings),
            )
        return resource_files

    def iter_localized_directories(self) -> list[tuple[str, Path]]:
        return [
            (path.name.replace("values-", ""), path)
            for path in sorted(self.res_dir.iterdir())
            if path.is_dir() and path.name.startswith("values-")
        ]

    def localized_xml_names(self, folder_path: Path, base_files: dict[str, ResourceFile]) -> list[str]:
        return sorted(path.name for path in folder_path.glob("*.xml") if path.name in base_files)

    def load_localized_strings(self, folder_path: Path, filename: str) -> dict[str, str]:
        return self.extract_strings_from_xml(folder_path / filename)


class OutputDirectoryManager:
    def __init__(self, output_dir: Path | str) -> None:
        self.output_dir = Path(output_dir)

    def reset(self) -> None:
        # 生成前清空旧产物，避免旧版目录结构残留导致新旧输出混杂。
        if self.output_dir.exists():
            shutil.rmtree(self.output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def ensure(self) -> None:
        self.output_dir.mkdir(parents=True, exist_ok=True)


@dataclass(frozen=True)
class ReportKeyBlock:
    key: str
    fields: tuple[tuple[str, str], ...]


@dataclass(frozen=True)
class ReportFileBlock:
    filename: str
    key_blocks: tuple[ReportKeyBlock, ...]


class MinimalMarkdownReportWriter:
    """
    统一输出尽量轻量的 Markdown：
    - 只保留标题层级和必要字段标签
    - 不使用列表、代码块、反引号，尽量减少 token
    """

    def write(
        self,
        output_path: Path | str,
        *,
        title: str,
        section: str,
        prompt: str | None = None,
        metadata_lines: tuple[str, ...] = (),
        file_blocks: tuple[ReportFileBlock, ...],
    ) -> None:
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        lines = [f"# {title}", "", f"## {section}"]
        for metadata_line in metadata_lines:
            lines.append(metadata_line)
        if prompt:
            lines.append(f"PROMPT: {prompt}")

        for file_block in file_blocks:
            if not file_block.key_blocks:
                continue
            lines.extend(("", f"FILE: {file_block.filename}"))
            for key_block in file_block.key_blocks:
                lines.append(f"KEY: {key_block.key}")
                for label, value in key_block.fields:
                    lines.append(f"{label}: {value}")

        output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
