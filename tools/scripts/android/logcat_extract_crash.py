#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path


CRASH_PATTERNS = [
    r"FATAL EXCEPTION",
    r"Fatal signal",
    r"SIGABRT",
    r"SIGSEGV",
    r"Abort message",
    r"backtrace:",
    r"tombstone",
    r"OutOfMemoryError",
    r"JNI DETECTED ERROR",
    r"std::bad_alloc",
    r"libaudio_android_jni\.so",
]


@dataclass(frozen=True)
class MatchBlock:
    line_number: int
    trigger: str
    lines: list[str]


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Extract likely crash-relevant regions from an Android logcat dump.",
    )
    parser.add_argument("logfile", type=Path, help="Path to the captured logcat text file.")
    parser.add_argument(
        "--context",
        type=int,
        default=8,
        help="Number of surrounding lines to keep before and after each match.",
    )
    parser.add_argument(
        "--max-blocks",
        type=int,
        default=40,
        help="Maximum number of extracted blocks to print.",
    )
    return parser


def compile_patterns() -> list[re.Pattern[str]]:
    return [re.compile(pattern, re.IGNORECASE) for pattern in CRASH_PATTERNS]


def read_lines(path: Path) -> list[str]:
    return path.read_text(encoding="utf-8", errors="replace").splitlines()


def extract_blocks(lines: list[str], context: int) -> list[MatchBlock]:
    patterns = compile_patterns()
    blocks: list[MatchBlock] = []
    seen_ranges: list[tuple[int, int]] = []

    for index, line in enumerate(lines):
        trigger = next((pattern.pattern for pattern in patterns if pattern.search(line)), None)
        if trigger is None:
            continue

        start = max(0, index - context)
        end = min(len(lines), index + context + 1)
        if any(not (end <= existing_start or start >= existing_end) for existing_start, existing_end in seen_ranges):
            continue

        seen_ranges.append((start, end))
        blocks.append(
            MatchBlock(
                line_number=index + 1,
                trigger=trigger,
                lines=lines[start:end],
            ),
        )

    return blocks


def summarize_patterns(lines: list[str]) -> list[tuple[str, int]]:
    counts: list[tuple[str, int]] = []
    for pattern in compile_patterns():
        count = sum(1 for line in lines if pattern.search(line))
        if count > 0:
            counts.append((pattern.pattern, count))
    counts.sort(key=lambda item: item[1], reverse=True)
    return counts


def print_summary(path: Path, lines: list[str], blocks: list[MatchBlock], max_blocks: int) -> None:
    print(f"file: {path}")
    print(f"total_lines: {len(lines)}")
    print("pattern_hits:")
    for pattern, count in summarize_patterns(lines):
        print(f"  {pattern}: {count}")

    print("\nextracted_blocks:")
    for block in blocks[:max_blocks]:
        print(f"\n=== line {block.line_number} trigger={block.trigger} ===")
        for line in block.lines:
            print(line)


def main() -> int:
    args = build_parser().parse_args()
    lines = read_lines(args.logfile)
    blocks = extract_blocks(lines, args.context)
    print_summary(args.logfile, lines, blocks, args.max_blocks)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
