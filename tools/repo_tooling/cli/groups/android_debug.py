from __future__ import annotations

import argparse

from ...commands.android_debug import cmd_android_debug
from ..common import RAW_FORMATTER


def register_android_debug_group(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    android_debug_parser = subparsers.add_parser(
        "android-debug",
        help="Capture and summarize Android adb/debug logs.",
        description=(
            "Capture and summarize Android adb/debug logs.\n\n"
            "Use `capture-*` commands for stable debug scenario capture on a connected device. "
            "Use `*-summary` commands when you already have an existing logcat file."
        ),
        formatter_class=RAW_FORMATTER,
    )
    action_parsers = android_debug_parser.add_subparsers(dest="action", required=True)

    def add_log_summary(
        name: str,
        help_text: str,
        *,
        default_max_rows: int,
    ) -> None:
        parser = action_parsers.add_parser(name, help=help_text)
        parser.add_argument("log", type=_path_arg, help="Raw adb logcat text file.")
        parser.add_argument("--output", type=_path_arg, help="Optional markdown summary output path.")
        parser.add_argument("--max-rows", type=int, default=default_max_rows, help="Maximum rows to print.")
        parser.set_defaults(func=cmd_android_debug)

    add_log_summary(
        "flash-summary",
        "Summarize Flash visual/readout/Lyrics adb logs.",
        default_max_rows=24,
    )
    add_log_summary(
        "mini-summary",
        "Summarize Mini visual/Lyrics adb logs.",
        default_max_rows=24,
    )
    add_log_summary(
        "encode-progress-summary",
        "Summarize Mini/Pro/Ultra encode progress adb logs.",
        default_max_rows=80,
    )

    crash_parser = action_parsers.add_parser("crash-summary", help="Extract likely crash regions from logcat.")
    crash_parser.add_argument("log", type=_path_arg)
    crash_parser.add_argument("--context", type=int, default=8, help="Surrounding lines before and after each match.")
    crash_parser.add_argument("--max-blocks", type=int, default=40, help="Maximum extracted blocks to print.")
    crash_parser.set_defaults(func=cmd_android_debug)

    device_prep_parser = action_parsers.add_parser("device-prep", help="Prepare the connected adb device.")
    device_prep_parser.set_defaults(func=cmd_android_debug)

    flash_capture = action_parsers.add_parser("capture-flash", help="Run Flash debug scenario and write logs.")
    flash_capture.add_argument("--output-dir", type=_path_arg, help="Directory for raw log and summaries.")
    flash_capture.add_argument("--wait-ms", type=int, default=90000, help="Delay before dumping logcat.")
    flash_capture.add_argument("--scenario", choices=["ui", "headless"], default="ui")
    flash_capture.add_argument("--style", default="litany", help="Flash voicing style.")
    flash_capture.add_argument("--display", choices=["lyrics", "visual", "mix"], default="lyrics")
    flash_capture.add_argument("--visual", default="lanes", help="Flash visual mode.")
    flash_capture.add_argument("--input", dest="input_text", help="Optional text override.")
    flash_capture.add_argument("--sample-length", choices=["short", "long"], help="Built-in sample length.")
    flash_capture.add_argument("--sample-id", help="Built-in sample id.")
    flash_capture.add_argument("--playback-speed", type=float, default=1.0, help="Player playback speed to apply before playback.")
    flash_capture.add_argument(
        "--play-ms",
        type=int,
        default=None,
        help="Playback duration. Defaults to 30000 for ui and 6000 for headless.",
    )
    flash_capture.add_argument("--no-encode", action="store_true", help="Pass wb.encode=false.")
    flash_capture.add_argument("--no-play", action="store_true", help="Pass wb.play=false.")
    flash_capture.add_argument("--max-rows", type=int, default=24, help="Maximum summary rows.")
    flash_capture.set_defaults(func=cmd_android_debug)

    mini_capture = action_parsers.add_parser("capture-mini", help="Run Mini debug scenario and write logs.")
    mini_capture.add_argument("--output-dir", type=_path_arg, help="Directory for raw log and summaries.")
    mini_capture.add_argument("--wait-ms", type=int, default=20000, help="Delay before dumping logcat.")
    mini_capture.add_argument("--scenario", choices=["ui"], default="ui")
    mini_capture.add_argument("--speed", choices=["slow", "standard", "fast"], default="standard")
    mini_capture.add_argument("--input", dest="input_text", help="Optional text override.")
    mini_capture.add_argument("--play-ms", type=int, default=6000)
    mini_capture.add_argument("--no-encode", action="store_true", help="Pass wb.encode=false.")
    mini_capture.add_argument("--no-play", action="store_true", help="Pass wb.play=false.")
    mini_capture.add_argument("--max-rows", type=int, default=24, help="Maximum summary rows.")
    mini_capture.set_defaults(func=cmd_android_debug)

    progress_capture = action_parsers.add_parser(
        "capture-encode-progress",
        help="Run Mini/Pro/Ultra encode-progress debug scenario and write logs.",
    )
    progress_capture.add_argument("--output-dir", type=_path_arg, help="Directory for raw log and summaries.")
    progress_capture.add_argument("--wait-ms", type=int, default=140000, help="Delay before dumping logcat.")
    progress_capture.add_argument("--mode", choices=["mini", "pro", "ultra"], default="mini")
    progress_capture.add_argument("--speed", choices=["slow", "standard", "fast"], default="standard")
    progress_capture.add_argument("--input", dest="input_text", help="Optional text override.")
    progress_capture.add_argument("--sample-length", choices=["short", "long"], help="Built-in sample length.")
    progress_capture.add_argument("--sample-id", help="Built-in sample id.")
    progress_capture.add_argument("--repeat", type=int, default=1)
    progress_capture.add_argument("--capture-ms", type=int, default=120000)
    progress_capture.add_argument("--poll-ms", type=int, default=33)
    progress_capture.add_argument("--no-encode", action="store_true", help="Pass wb.encode=false.")
    progress_capture.add_argument("--max-rows", type=int, default=80, help="Maximum summary rows.")
    progress_capture.set_defaults(func=cmd_android_debug)


def _path_arg(value: str):
    from pathlib import Path

    return Path(value)
