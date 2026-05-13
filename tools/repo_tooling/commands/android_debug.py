from __future__ import annotations

import argparse
from pathlib import Path

from ..constants import ROOT_DIR
from ..android_debug import capture, crash_summary, encode_progress, flash_alignment, mini_alignment


def _write_or_print(summary: str, output: Path | None) -> None:
    if output is None:
        print(summary)
        return
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(summary, encoding="utf-8")
    print(f"Wrote {output}")


def _read_events(log: Path, parser) -> list:
    text = log.read_text(encoding="utf-8", errors="replace")
    return [event for line in text.splitlines() if (event := parser(line)) is not None]


def _output_dir(args: argparse.Namespace, prefix: str) -> Path:
    output_dir = args.output_dir
    if output_dir is None:
        return capture.default_output_dir(prefix)
    if not output_dir.is_absolute():
        output_dir = ROOT_DIR / output_dir
    return output_dir


def cmd_android_debug(args: argparse.Namespace) -> None:
    if args.action == "flash-summary":
        events = _read_events(args.log, flash_alignment.parse_event)
        _write_or_print(flash_alignment.build_summary(events, max_rows=args.max_rows), args.output)
        return
    if args.action == "mini-summary":
        events = _read_events(args.log, mini_alignment.parse_event)
        _write_or_print(mini_alignment.build_summary(events, max_rows=args.max_rows), args.output)
        return
    if args.action == "encode-progress-summary":
        events = _read_events(args.log, encode_progress.parse_event)
        _write_or_print(encode_progress.build_summary(events, max_rows=args.max_rows), args.output)
        return
    if args.action == "crash-summary":
        lines = crash_summary.read_lines(args.log)
        blocks = crash_summary.extract_blocks(lines, args.context)
        crash_summary.print_summary(args.log, lines, blocks, args.max_blocks)
        return
    if args.action == "device-prep":
        capture.device_prep()
        return
    if args.action == "capture-flash":
        extras: list[str] = []
        play_ms = args.play_ms if args.play_ms is not None else (30000 if args.scenario == "ui" else 6000)
        capture.string_extra(extras, "wb.scenario", args.scenario)
        capture.string_extra(extras, "wb.flash.style", args.style)
        capture.string_extra(extras, "wb.display", args.display)
        capture.string_extra(extras, "wb.visual", args.visual)
        capture.string_extra(extras, "wb.input", args.input_text)
        capture.string_extra(extras, "wb.sample.length", args.sample_length)
        capture.string_extra(extras, "wb.sample.id", args.sample_id)
        capture.float_extra(extras, "wb.playback.speed", args.playback_speed)
        capture.bool_extra(extras, "wb.encode", not args.no_encode)
        capture.bool_extra(extras, "wb.play", not args.no_play)
        capture.long_extra(extras, "wb.play.ms", play_ms)
        capture.capture_common(
            output_dir=_output_dir(args, f"flash-{args.scenario}-{args.style}"),
            wait_ms=args.wait_ms,
            filters=capture.FLASH_LOGCAT_FILTERS,
            event_parser=flash_alignment.parse_event,
            summary_builder=lambda events: flash_alignment.build_summary(events, max_rows=args.max_rows),
            start=lambda: capture.start_activity(capture.FLASH_ACTION, extras),
        )
        return
    if args.action == "capture-mini":
        extras = []
        capture.string_extra(extras, "wb.scenario", args.scenario)
        capture.string_extra(extras, "wb.mini.speed", args.speed)
        capture.string_extra(extras, "wb.input", args.input_text)
        capture.bool_extra(extras, "wb.encode", not args.no_encode)
        capture.bool_extra(extras, "wb.play", not args.no_play)
        capture.long_extra(extras, "wb.play.ms", args.play_ms)
        capture.capture_common(
            output_dir=_output_dir(args, f"mini-{args.speed}"),
            wait_ms=args.wait_ms,
            filters=capture.MINI_LOGCAT_FILTERS,
            event_parser=mini_alignment.parse_event,
            summary_builder=lambda events: mini_alignment.build_summary(events, max_rows=args.max_rows),
            start=lambda: capture.start_activity(capture.MINI_ACTION, extras),
        )
        return
    if args.action == "capture-encode-progress":
        extras = []
        capture.string_extra(extras, "wb.mode", args.mode)
        capture.string_extra(extras, "wb.mini.speed", args.speed)
        capture.string_extra(extras, "wb.input", args.input_text)
        capture.string_extra(extras, "wb.sample.length", args.sample_length)
        capture.string_extra(extras, "wb.sample.id", args.sample_id)
        capture.int_extra(extras, "wb.repeat", args.repeat)
        capture.long_extra(extras, "wb.capture.ms", args.capture_ms)
        capture.long_extra(extras, "wb.poll.ms", args.poll_ms)
        capture.bool_extra(extras, "wb.encode", not args.no_encode)
        capture.capture_common(
            output_dir=_output_dir(args, f"encode-progress-{args.mode}"),
            wait_ms=args.wait_ms,
            filters=capture.ENCODE_PROGRESS_LOGCAT_FILTERS,
            event_parser=encode_progress.parse_event,
            summary_builder=lambda events: encode_progress.build_summary(events, max_rows=args.max_rows),
            start=lambda: capture.start_activity(capture.ENCODE_PROGRESS_ACTION, extras),
        )
        return
    raise AssertionError(f"Unhandled android-debug action: {args.action}")
