from __future__ import annotations

import argparse

from apply_translation_replacements import (
    DEFAULT_JSON_PATH,
    apply_translation_replacements,
)
from check_mixed_language import run_mixed_language_check
from compare_translation_quality import generate_comparisons_for_res
from translation_common import DEFAULT_RES_DIRECTORY


def run_all_translation_reports() -> None:
    print("[translate] Generating translation quality reviews (de, es, ja, pt-BR, ru, uk, zh, zh-rTW)")
    generate_comparisons_for_res()
    print("[translate] Generating mixed-language reports")
    run_mixed_language_check()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Android translation tooling entrypoint.",
    )
    subparsers = parser.add_subparsers(dest="command")

    subparsers.add_parser(
        "all",
        help="Generate all translation review reports.",
    )
    subparsers.add_parser(
        "compare",
        help="Generate translation quality review reports.",
    )
    subparsers.add_parser(
        "mixed-language",
        help="Generate mixed-language reports.",
    )
    replace_parser = subparsers.add_parser(
        "replace",
        help="Apply replacements from replacements.json into Android sample string XML files.",
    )
    replace_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    replace_parser.add_argument(
        "--json",
        default=str(DEFAULT_JSON_PATH),
        help="Path to the replacement JSON file.",
    )

    return parser.parse_args()


def run() -> int:
    args = parse_args()
    command = args.command or "all"

    if command == "all":
        run_all_translation_reports()
        return 0
    if command == "compare":
        print("[translate] Generating translation quality reviews (de, es, ja, pt-BR, ru, uk, zh, zh-rTW)")
        generate_comparisons_for_res()
        return 0
    if command == "mixed-language":
        print("[translate] Generating mixed-language reports")
        run_mixed_language_check()
        return 0
    if command == "replace":
        return apply_translation_replacements(
            res_dir=args.res_dir,
            json_path=args.json,
        )

    raise ValueError(f"Unsupported command: {command}")


if __name__ == "__main__":
    raise SystemExit(run())
