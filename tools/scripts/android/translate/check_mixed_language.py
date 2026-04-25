from __future__ import annotations

from pathlib import Path
import re

from translation_common import (
    DEFAULT_RES_DIRECTORY,
    AndroidStringResourceRepository,
    MinimalMarkdownReportWriter,
    OutputDirectoryManager,
    ReportFileBlock,
    ReportKeyBlock,
    ResourceFile,
    display_language_tag,
    is_pro_sample_key,
)

DEFAULT_OUTPUT_DIRECTORY = Path(__file__).resolve().parents[4] / "temp" / "mixed_language_reports"

# 白名单：允许在任何语言中合法保留的英文代号/专有名词（需全小写）
WHITELIST = {
    "led", "ascii", "ping", "ui", "id", "ip", "app", "mac", "hex", "ack", "ok"
}

PLACEHOLDER_RE = re.compile(r"%\d*\$?[sdf]")
NON_LATIN_CHUNK_RE = re.compile(r"[a-zA-Z][a-zA-Z\s\-]*[a-zA-Z]|[a-zA-Z]")
WORD_RE = re.compile(r"\b[a-zA-Z]+\b")
WORD_SPLIT_RE = re.compile(r"[\s\-]+")
DEFAULT_MAX_LATIN_PHRASE_WORDS = 12


def clean_text(text: str) -> str:
    """清理 Android 特殊占位符和转义符，避免影响英文检测"""
    if not text:
        return ""
    # 去除类似于 %s, %d, %1$s, %2$d 等占位符
    text = PLACEHOLDER_RE.sub("", text)
    # 去除换行和反斜杠
    text = text.replace("\\n", " ").replace("\\t", " ").replace("\\", "")
    return text


def is_pro_ascii_context_key(key: str) -> bool:
    """
    Pro 模式本身就是 ASCII/byte/token 可视化语境。
    这些 key 在任何语言下都允许保留 ASCII、byte、Token 等英文协议词，不能按语言混杂处理。
    """
    return key.startswith("audio_pro_") or key == "validation_pro_ascii_only"


def check_ascii_range(target_text: str) -> list[str]:
    """
    Pro 示例文本在所有语言下都必须保持 ASCII。
    因此这里只检查字符范围，不按目标语言检查是否混入英文或其他语言。
    """
    cleaned = clean_text(target_text)
    return sorted({f"U+{ord(char):04X}({char})" for char in cleaned if ord(char) > 0x7F})


def is_non_latin(lang_code: str) -> bool:
    """判断是否为非拉丁语系（中文、日文、韩文、俄文等）"""
    # 西里尔、CJK 等都按非拉丁处理；英文片段混入时更容易直接识别。
    non_latins = ["zh", "ja", "ko", "ru", "uk", "ar", "hi", "th"]
    return any(lang_code.startswith(prefix) for prefix in non_latins)


def check_non_latin(target_text: str) -> list[str]:
    """
    针对中文、日文、俄文等：
    直接寻找译文中的英文字母片段。如果该片段不完全由白名单词组构成，则判定为混杂。
    """
    cleaned = clean_text(target_text)
    # 匹配连续的英文字母，允许中间有空格或连字符（用于抓取 "stolen floor" 这样的词组）
    chunks = NON_LATIN_CHUNK_RE.findall(cleaned)

    suspicious: list[str] = []
    for chunk in chunks:
        chunk = chunk.strip()
        if not chunk:
            continue

        words = WORD_SPLIT_RE.split(chunk)
        # 如果拆分后的单词都在白名单内，或是单个字母（如 A级、X轴），则安全
        is_safe = all(word.lower() in WHITELIST or len(word) == 1 for word in words)
        if not is_safe:
            suspicious.append(chunk)

    return suspicious


def check_latin_longest_phrases(
    source_text: str,
    target_text: str,
    min_words: int = 2,
    max_phrase_words: int = DEFAULT_MAX_LATIN_PHRASE_WORDS,
) -> list[str]:
    """
    针对德语、西语等拉丁语系：
    检查英文原文中是否有连续（>=2）的单词被原封不动地放进了译文里。
    这里用目标文本 n-gram 集合做查找，避免为每个源短语反复构造 regex 搜索。
    """
    cleaned_src = clean_text(source_text)
    cleaned_tgt = clean_text(target_text)

    src_words = WORD_RE.findall(cleaned_src)
    target_words = WORD_RE.findall(cleaned_tgt)
    if len(src_words) < min_words or len(target_words) < min_words:
        return []

    max_len = min(max_phrase_words, len(src_words), len(target_words))
    target_ngrams_by_length: dict[int, set[str]] = {}
    lower_target_words = [word.lower() for word in target_words]
    for length in range(min_words, max_len + 1):
        target_ngrams_by_length[length] = {
            " ".join(lower_target_words[index:index + length])
            for index in range(len(lower_target_words) - length + 1)
        }

    found_phrases: list[str] = []
    found_phrases_lower: list[str] = []
    lower_src_words = [word.lower() for word in src_words]
    # 从最长的短语开始匹配；找到长短语后跳过被它包含的短片段，保持原报告语义。
    for length in range(max_len, min_words - 1, -1):
        target_ngrams = target_ngrams_by_length[length]
        for index in range(len(src_words) - length + 1):
            phrase_words = src_words[index:index + length]

            # 如果全由白名单词汇组成，跳过
            if all(word.lower() in WHITELIST for word in phrase_words):
                continue

            phrase_lower = " ".join(lower_src_words[index:index + length])
            if phrase_lower not in target_ngrams:
                continue

            is_sub_phrase = any(phrase_lower in found for found in found_phrases_lower)
            if not is_sub_phrase:
                found_phrases.append(" ".join(phrase_words))
                found_phrases_lower.append(phrase_lower)

    return found_phrases


class MixedLanguageReportGenerator:
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
        base_files = self.repository.load_base_resource_files()
        self.output_manager.reset()

        total_issues = 0
        written_reports = 0

        for lang_code, folder_path in self.repository.iter_localized_directories():
            category_results = self._build_language_results(lang_code, folder_path, base_files)
            for text_type, result in category_results.items():
                if result["issue_count"] <= 0:
                    continue

                output_file = self.output_manager.output_dir / lang_code / text_type / "mixed_language_report.md"
                self.writer.write(
                    output_file,
                    title=f"Mixed Language Report [{text_type.upper()}][{display_language_tag(lang_code)}]",
                    section=f"{display_language_tag(lang_code)} | {result['strategy_name']}",
                    metadata_lines=(f"TOTAL_ISSUES: {result['issue_count']}",),
                    file_blocks=tuple(result["file_blocks"]),
                )
                total_issues += result["issue_count"]
                written_reports += 1

        if total_issues == 0:
            output_file = self.output_manager.output_dir / "mixed_language_report.md"
            output_file.write_text(
                "# Mixed Language Report\n\nOK: No missing translation or mixed-language issues found by current rules.\n",
                encoding="utf-8",
            )
            written_reports = 1

        print(f"Done. Suspicious issues: {total_issues}")
        print(f"Reports generated under: {self.output_manager.output_dir} ({written_reports} files)")

    def _build_language_results(
        self,
        lang_code: str,
        folder_path: Path,
        base_files: dict[str, ResourceFile],
    ) -> dict[str, dict[str, object]]:
        is_nl_lang = is_non_latin(lang_code)
        lang_strategy = (
            "Non-Latin Check (中文/日文/俄文等)"
            if is_nl_lang
            else "Latin N-gram Check (德语/西语/葡语等)"
        )
        category_results: dict[str, dict[str, object]] = {
            "app_text": {"strategy_name": lang_strategy, "issue_count": 0, "file_blocks": []},
            "sample_text": {"strategy_name": lang_strategy, "issue_count": 0, "file_blocks": []},
        }

        xml_names = self.repository.localized_xml_names(folder_path, base_files)
        for filename in xml_names:
            resource_file = base_files[filename]
            trans_dict = self.repository.load_localized_strings(folder_path, filename)
            key_blocks = self._build_issue_key_blocks(
                resource_file,
                trans_dict=trans_dict,
                is_non_latin_language=is_nl_lang,
            )
            if not key_blocks:
                continue

            category_results[resource_file.text_type]["issue_count"] += len(key_blocks)
            category_results[resource_file.text_type]["file_blocks"].append(
                ReportFileBlock(filename=filename, key_blocks=tuple(key_blocks))
            )

        return category_results

    def _build_issue_key_blocks(
        self,
        resource_file: ResourceFile,
        *,
        trans_dict: dict[str, str],
        is_non_latin_language: bool,
    ) -> list[ReportKeyBlock]:
        key_blocks: list[ReportKeyBlock] = []
        for key, en_text in resource_file.strings.items():
            if key not in trans_dict:
                continue

            trans_text = trans_dict[key]

            # ------ 核心检测逻辑 ------
            if is_pro_sample_key(key):
                # Pro 的示例文本是协议/编码用 ASCII 输入，不是需要翻译的目标语言文案。
                # 所以在这里提前短路，跳过更昂贵的混杂语言检查，只验证 ASCII 范围。
                suspicious = check_ascii_range(trans_text)
                issue_label = "非 ASCII 字符"
            elif is_pro_ascii_context_key(key):
                # Pro UI 描述的是 ASCII/byte/token 编码细节；这些英文协议词跨语言保留是预期行为。
                # 直接跳过混杂语言检查，避免把合法的 ASCII 术语误报为漏翻。
                continue
            elif is_non_latin_language:
                suspicious = check_non_latin(trans_text)
                issue_label = "可疑漏翻/混杂"
            else:
                suspicious = check_latin_longest_phrases(en_text, trans_text)
                issue_label = "可疑漏翻/混杂"

            if not suspicious:
                continue

            key_blocks.append(
                ReportKeyBlock(
                    key=key,
                    fields=self._build_issue_fields(
                        resource_file,
                        key,
                        issue_label,
                        suspicious,
                        trans_text,
                        en_text,
                    ),
                )
            )

        return key_blocks

    def _build_issue_fields(
        self,
        resource_file: ResourceFile,
        key: str,
        issue_label: str,
        suspicious: list[str],
        translated_text: str,
        english_text: str,
    ) -> tuple[tuple[str, str], ...]:
        fields: list[tuple[str, str]] = []
        sample_length = resource_file.sample_lengths.get(key)
        if sample_length is not None:
            fields.append(("SAMPLE_LENGTH", sample_length))
        fields.extend(
            (
                ("ISSUE", f"{issue_label}: {', '.join(suspicious)}"),
                ("TR", translated_text),
                ("EN", english_text),
            )
        )
        return tuple(fields)


def run_mixed_language_check(
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
) -> None:
    MixedLanguageReportGenerator(res_dir=res_dir, output_dir=output_dir).run()
