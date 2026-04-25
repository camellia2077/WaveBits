from __future__ import annotations

import argparse
import difflib
import json
from collections import defaultdict
from dataclasses import dataclass
import re
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape, unescape

from translation_common import DEFAULT_RES_DIRECTORY

DEFAULT_JSON_PATH = Path(__file__).resolve().with_name("replacements.json")
ANSI_RED = "\033[31m"
ANSI_GREEN = "\033[32m"
ANSI_RESET = "\033[0m"
SUSPICIOUS_XML_ESCAPE_RE = re.compile(r"(\\'|\\\")")


def configure_console_output() -> None:
    # Windows shells may still default to legacy code pages such as GBK.
    # Force UTF-8 so localized diffs can print without crashing after a successful replacement.
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if callable(reconfigure):
            reconfigure(encoding="utf-8", errors="replace")


@dataclass(frozen=True)
class ReplacementEntry:
    original: str
    optimized: str


@dataclass(frozen=True)
class AppliedReplacement:
    xml_path: Path
    string_name: str
    original: str
    optimized: str


@dataclass(frozen=True)
class StringMatch:
    xml_path: Path
    string_name: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Read replacement rules from a JSON file and update Android string XML files.\n"
            "Each rule must include original and optimized.\n"
            "The script searches for a unique matching source text under the Android res directory before replacing it."
        )
    )
    parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    parser.add_argument(
        "--json",
        default=str(DEFAULT_JSON_PATH),
        help=(
            "Path to the replacement JSON file. "
            "Defaults to replacements.json in the same directory as this script."
        ),
    )
    return parser.parse_args()


def load_entries(json_path: Path) -> list[ReplacementEntry]:
    raw_items = json.loads(json_path.read_text(encoding="utf-8"))
    if not isinstance(raw_items, list):
        raise ValueError("Replacement JSON must be a list.")

    entries: list[ReplacementEntry] = []
    for index, item in enumerate(raw_items, start=1):
        if not isinstance(item, dict):
            raise ValueError(f"Entry #{index} is not an object.")

        missing_fields = [field for field in ("original", "optimized") if field not in item]
        if missing_fields:
            raise ValueError(f"Entry #{index} is missing fields: {', '.join(missing_fields)}")

        suspicious_issues: list[str] = []
        for field in ("original", "optimized"):
            value = str(item[field])
            suspicious_match = SUSPICIOUS_XML_ESCAPE_RE.search(value)
            if suspicious_match is not None:
                suspicious_issues.append(
                    f"{field} contains forbidden Android XML backslash escape {suspicious_match.group(0)!r}: {value}"
                )
        if suspicious_issues:
            raise ValueError(
                f"Entry #{index} contains forbidden Android XML backslash escapes.\n"
                + "\n".join(suspicious_issues)
                + "\nUse the literal punctuation character instead of backslash-style escapes."
            )

        entries.append(
            ReplacementEntry(
                original=str(item["original"]),
                optimized=str(item["optimized"]),
            )
        )
    return entries


STRING_TAG_RE = re.compile(
    r"(?P<open><string\b[^>]*\bname=(?P<quote>['\"])(?P<name>[^'\"]+)(?P=quote)[^>]*>)"
    r"(?P<text>.*?)"
    r"(?P<close></string>)",
    re.DOTALL,
)


def build_colored_character_diff(original_text: str, optimized_text: str) -> str:
    parts: list[str] = []
    matcher = difflib.SequenceMatcher(a=original_text, b=optimized_text)
    for opcode, a_start, a_end, b_start, b_end in matcher.get_opcodes():
        if opcode == "equal":
            parts.append(original_text[a_start:a_end])
        elif opcode == "delete":
            parts.append(f"{ANSI_RED}{original_text[a_start:a_end]}{ANSI_RESET}")
        elif opcode == "insert":
            parts.append(f"{ANSI_GREEN}{optimized_text[b_start:b_end]}{ANSI_RESET}")
        elif opcode == "replace":
            parts.append(f"{ANSI_RED}{original_text[a_start:a_end]}{ANSI_RESET}")
            parts.append(f"{ANSI_GREEN}{optimized_text[b_start:b_end]}{ANSI_RESET}")
    return "".join(parts)


def print_applied_diffs(applied_replacements: list[AppliedReplacement]) -> None:
    if not applied_replacements:
        return

    print("Applied diffs:")
    for replacement in applied_replacements:
        print(f"{replacement.xml_path} | {replacement.string_name}")
        print(f"- OLD: {replacement.original}")
        print(f"+ NEW: {replacement.optimized}")
        print(f"  DIFF: {build_colored_character_diff(replacement.original, replacement.optimized)}")


def validate_string_resource_xml(xml_path: Path, xml_text: str) -> list[str]:
    """
    在局部替换前先确认文件仍然是正常的 Android string resources XML。

    修改理由：
    - 当前脚本故意只替换局部文本，不重写整份 XML
    - 如果输入文件先前已经损坏
    - 继续做局部替换只会把错误结构保留下去，让问题更隐蔽

    所以这里使用 fail-fast：
    发现结构异常就直接报错并跳过该文件。
    """
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError as exc:
        return [f"{xml_path} | invalid XML: {exc}"]

    errors: list[str] = []
    if root.tag != "resources":
        errors.append(f"{xml_path} | unexpected root tag: {root.tag}")
        return errors

    if root.text and root.text.strip():
        errors.append(
            f"{xml_path} | unexpected text directly under <resources>: {root.text.strip()}"
        )

    for child in root:
        if child.tag != "string":
            errors.append(f"{xml_path} | unsupported child tag under <resources>: {child.tag}")
            continue
        if child.tail and child.tail.strip():
            errors.append(
                f"{xml_path} | unexpected trailing text after <string name=\"{child.get('name', '')}\">: {child.tail.strip()}"
            )

    return errors


def apply_replacement_at_match(
    xml_path: Path,
    *,
    string_name: str,
    original_text: str,
    optimized_text: str,
    xml_text: str,
) -> bool:
    validation_errors = validate_string_resource_xml(xml_path, xml_text)
    if validation_errors:
        raise ValueError("\n".join(validation_errors))

    replaced = False

    def replace_match(match: re.Match[str]) -> str:
        nonlocal replaced
        if replaced:
            return match.group(0)
        if match.group("name") != string_name:
            return match.group(0)

        current_text = unescape(match.group("text")).strip()
        if current_text != original_text:
            return match.group(0)

        replaced = True
        return f"{match.group('open')}{escape(optimized_text)}{match.group('close')}"

    updated_xml_text = STRING_TAG_RE.sub(replace_match, xml_text)
    if replaced:
        # 只有替换后的 XML 结构仍然有效，才允许真正写回磁盘。
        updated_validation_errors = validate_string_resource_xml(xml_path, updated_xml_text)
        if updated_validation_errors:
            raise ValueError("\n".join(updated_validation_errors))
        xml_path.write_text(updated_xml_text, encoding="utf-8")
    return replaced


def build_not_found_suggestions(
    original_index: dict[str, list[StringMatch]],
    original_text: str,
    *,
    max_candidates: int = 3,
) -> list[str]:
    suggestions: list[str] = []

    prefix_matches = [
        (text, matches)
        for text, matches in original_index.items()
        if text.startswith(original_text)
    ]
    if prefix_matches:
        suggestions.append(
            "provided original looks like a prefix of a longer resource string. Full candidates:"
        )
        for text, matches in prefix_matches[:max_candidates]:
            first_match = matches[0]
            suggestions.append(
                f"  - {first_match.xml_path} | {first_match.string_name} | full text: {text}"
            )
        return suggestions

    close_texts = difflib.get_close_matches(
        original_text,
        original_index.keys(),
        n=max_candidates,
        cutoff=0.45,
    )
    if close_texts:
        suggestions.append("closest full resource strings:")
        for text in close_texts:
            first_match = original_index[text][0]
            suggestions.append(
                f"  - {first_match.xml_path} | {first_match.string_name} | full text: {text}"
            )
    return suggestions


def find_unique_original_match(
    original_index: dict[str, list[StringMatch]],
    original_text: str,
) -> tuple[StringMatch | None, list[str]]:
    """
    仅根据 original 文本在已建立的索引里查找唯一来源。
    如果找不到或找到多个，都报错并停止该条替换，避免误改。
    """
    matches = original_index.get(original_text, [])
    if not matches:
        errors = [f"original not found under res: {original_text}"]
        errors.extend(build_not_found_suggestions(original_index, original_text))
        return None, errors
    if len(matches) > 1:
        match_lines = "\n".join(f"  - {match.xml_path} | {match.string_name}" for match in matches)
        return None, [f"multiple original matches found under res: {original_text}\n{match_lines}"]
    return matches[0], []


def iter_values_xml_files(res_dir: Path) -> list[Path]:
    xml_files: list[Path] = []
    for child in sorted(res_dir.iterdir()):
        if not child.is_dir():
            continue
        if child.name != "values" and not child.name.startswith("values-"):
            continue
        # 这里只处理示例文本资源。
        # 修改理由：
        # - 本脚本当前服务的是 sample 文案批量替换流程
        # - themes.xml / ic_launcher_colors.xml 这类 values XML 虽然也在 res 下，
        #   但它们不属于示例文本，结构也不以 <string> 为主
        # - 如果继续扫描这些文件，会产生无关的格式报错，干扰真正的 sample 文案替换
        xml_files.extend(sorted(child.glob("audio_samples_*.xml")))
    return xml_files


def build_original_index(
    res_dir: Path,
) -> tuple[dict[str, list[StringMatch]], dict[Path, str], list[str]]:
    """
    一次性扫描 values / values-* 目录，建立：
    - original 文本 -> 命中位置列表
    - xml_path -> 文件文本缓存
    - 结构异常文件的错误列表

    避免每条 replacement 都全量重新扫盘。
    """
    original_index: dict[str, list[StringMatch]] = defaultdict(list)
    xml_text_cache: dict[Path, str] = {}
    invalid_xml_errors: list[str] = []

    for xml_path in iter_values_xml_files(res_dir):
        try:
            xml_text = xml_path.read_text(encoding="utf-8")
        except Exception:
            continue

        validation_errors = validate_string_resource_xml(xml_path, xml_text)
        if validation_errors:
            invalid_xml_errors.extend(validation_errors)
            continue

        xml_text_cache[xml_path] = xml_text
        for match in STRING_TAG_RE.finditer(xml_text):
            string_name = match.group("name")
            current_text = unescape(match.group("text")).strip()
            original_index[current_text].append(StringMatch(xml_path=xml_path, string_name=string_name))

    return dict(original_index), xml_text_cache, invalid_xml_errors


def apply_translation_replacements(
    *,
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    json_path: Path | str = DEFAULT_JSON_PATH,
) -> int:
    configure_console_output()
    res_dir = Path(res_dir)
    json_path = Path(json_path)
    if not res_dir.exists():
        print(f"Error: res directory not found: {res_dir}", file=sys.stderr)
        return 1
    if not json_path.exists():
        print(f"Error: replacement JSON not found: {json_path}", file=sys.stderr)
        return 1

    entries = load_entries(json_path)
    if not entries:
        print("No replacement entries found.")
        return 0

    original_index, xml_text_cache, invalid_xml_errors = build_original_index(res_dir)

    applied_total = 0
    applied_replacements: list[AppliedReplacement] = []
    all_errors: list[str] = list(invalid_xml_errors)

    deduped_entries_by_original: dict[str, ReplacementEntry] = {}
    for entry in entries:
        existing = deduped_entries_by_original.get(entry.original)
        if existing is not None and existing.optimized != entry.optimized:
            all_errors.append(
                "conflicting optimized values for the same original:\n"
                f"  original: {entry.original}\n"
                f"  optimized A: {existing.optimized}\n"
                f"  optimized B: {entry.optimized}"
            )
            continue
        deduped_entries_by_original[entry.original] = entry

    for entry in deduped_entries_by_original.values():
        match, resolve_errors = find_unique_original_match(original_index, entry.original)
        if resolve_errors:
            all_errors.extend(resolve_errors)
            continue

        assert match is not None
        xml_path = match.xml_path
        string_name = match.string_name
        try:
            replaced = apply_replacement_at_match(
                xml_path,
                string_name=string_name,
                original_text=entry.original,
                optimized_text=entry.optimized,
                xml_text=xml_text_cache[xml_path],
            )
        except ValueError as exc:
            all_errors.append(str(exc))
            continue
        if replaced:
            applied_total += 1
            applied_replacements.append(
                AppliedReplacement(
                    xml_path=xml_path,
                    string_name=string_name,
                    original=entry.original,
                    optimized=entry.optimized,
                )
            )
            xml_text_cache[xml_path] = xml_path.read_text(encoding="utf-8")

    print(f"Applied replacements: {applied_total}")
    print_applied_diffs(applied_replacements)
    if all_errors:
        print("Validation errors:")
        for error in all_errors:
            print(error)
        return 2

    print("All replacements applied successfully.")
    return 0


def run() -> int:
    args = parse_args()
    return apply_translation_replacements(
        res_dir=args.res_dir,
        json_path=args.json,
    )


if __name__ == "__main__":
    raise SystemExit(run())
