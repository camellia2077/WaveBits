from __future__ import annotations

from collections import defaultdict
from pathlib import Path

from translation_common import (
    DEFAULT_RES_DIRECTORY,
    FACTIONS,
    AndroidStringResourceRepository,
    ResourceFile,
    humanize_name,
    is_pro_sample_key,
)


def build_faction_counts(base_files: dict[str, ResourceFile]) -> dict[str, dict[str, int]]:
    counts: dict[str, dict[str, int]] = {
        faction: {"SHORT": 0, "LONG": 0}
        for faction in FACTIONS
    }
    counts["other"] = {"SHORT": 0, "LONG": 0}

    for resource_file in base_files.values():
        if resource_file.text_type != "sample_text":
            continue

        faction_counts = counts.setdefault(resource_file.faction, {"SHORT": 0, "LONG": 0})
        for sample_length in resource_file.sample_lengths.values():
            if sample_length in ("SHORT", "LONG"):
                faction_counts[sample_length] += 1

    return counts


def print_faction_counts(counts: dict[str, dict[str, int]]) -> None:
    print("Sample Length Counts")
    print()
    for faction in FACTIONS + ("other",):
        if faction not in counts:
            continue
        short_count = counts[faction]["SHORT"]
        long_count = counts[faction]["LONG"]
        total_count = short_count + long_count
        if total_count == 0:
            continue
        print(f"{humanize_name(faction)}")
        print(f"  SHORT: {short_count}")
        print(f"  LONG:  {long_count}")
        print(f"  TOTAL: {total_count}")
        print()


def run_sample_length_count(
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
) -> None:
    repository = AndroidStringResourceRepository(res_dir)
    repository.ensure_base_directory()
    base_files = repository.load_base_resource_files(
        # Pro 的 sample 文本是固定 ASCII 输入，不计入 short / long themed prose 统计。
        string_filter=lambda key, _text: not is_pro_sample_key(key),
    )
    counts = build_faction_counts(base_files)
    print_faction_counts(counts)


if __name__ == "__main__":
    run_sample_length_count()
