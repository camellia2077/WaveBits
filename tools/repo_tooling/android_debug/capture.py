from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
import shlex
import subprocess
import time

from ..constants import ROOT_DIR
from ..errors import ToolError
from ..process import print_command


APP_COMPONENT = "com.bag.audioandroid/.MainActivity"
FLASH_ACTION = "com.bag.audioandroid.DEBUG_FLASH_SCENARIO"
MINI_ACTION = "com.bag.audioandroid.DEBUG_MINI_SCENARIO"
ENCODE_PROGRESS_ACTION = "com.bag.audioandroid.DEBUG_ENCODE_PROGRESS_SCENARIO"

FLASH_LOGCAT_FILTERS = [
    "FlashAutomation:D",
    "FlashHeadless:D",
    "FlipBitsLongAudio:D",
    "GeneratedAudioCache:D",
    "PlaybackAutomation:D",
    "FlashAlignmentPerf:D",
    "FlashVisualPerf:D",
    "FlashLyricsPerf:D",
    "AndroidRuntime:E",
    "libc:E",
    "*:S",
]
MINI_LOGCAT_FILTERS = [
    "MiniAutomation:D",
    "MiniAlignmentPerf:D",
    "FlashLyricsPerf:D",
    "AndroidRuntime:E",
    "libc:E",
    "*:S",
]
ENCODE_PROGRESS_LOGCAT_FILTERS = [
    "EncodeProgressAutomation:D",
    "FlipBitsLongAudio:D",
    "GeneratedAudioCache:D",
    "AndroidRuntime:E",
    "libc:E",
    "*:S",
]


@dataclass(frozen=True)
class CaptureResult:
    raw_log: Path
    summary: Path
    crash_summary: Path


def default_output_dir(prefix: str) -> Path:
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    return ROOT_DIR / "temp" / "android-debug" / f"{stamp}-{prefix}"


def run_adb(args: list[str], *, capture: bool = False, check: bool = True) -> subprocess.CompletedProcess[str]:
    command = ["adb", *args]
    print_command(command, ROOT_DIR)
    completed = subprocess.run(
        command,
        cwd=ROOT_DIR,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if check and completed.returncode != 0:
        detail = ""
        if capture:
            detail = f"\nstdout:\n{completed.stdout}\nstderr:\n{completed.stderr}"
        raise ToolError(f"adb command failed with exit code {completed.returncode}: {' '.join(command)}{detail}")
    return completed


def ensure_device() -> None:
    completed = run_adb(["devices"], capture=True)
    devices = [
        line.split("\t", 1)[0]
        for line in completed.stdout.splitlines()
        if line.strip().endswith("\tdevice")
    ]
    if not devices:
        raise ToolError("No adb device is connected and authorized.")
    print(f"adb device: {devices[0]}")


def device_prep() -> None:
    ensure_device()
    run_adb(["shell", "svc", "power", "stayon", "usb"])
    run_adb(["shell", "input", "keyevent", "KEYCODE_WAKEUP"])
    run_adb(["shell", "wm", "dismiss-keyguard"], check=False)


def bool_extra(command: list[str], key: str, value: bool) -> None:
    command.extend(["--ez", key, "true" if value else "false"])


def string_extra(command: list[str], key: str, value: str | None) -> None:
    if value:
        command.extend(["--es", key, value])


def int_extra(command: list[str], key: str, value: int | None) -> None:
    if value is not None:
        command.extend(["--ei", key, str(value)])


def float_extra(command: list[str], key: str, value: float | None) -> None:
    if value is not None:
        command.extend(["--ef", key, str(value)])


def long_extra(command: list[str], key: str, value: int | None) -> None:
    if value is not None:
        command.extend(["--el", key, str(value)])


def start_activity(action: str, extras: list[str]) -> None:
    remote_command = " ".join(shlex.quote(part) for part in ["am", "start", "-n", APP_COMPONENT, "-a", action, *extras])
    run_adb(["shell", remote_command])


def dump_logcat(path: Path, filters: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    command = ["adb", "logcat", "-d", "-v", "time", *filters]
    print_command(command, ROOT_DIR)
    completed = subprocess.run(
        command,
        cwd=ROOT_DIR,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if completed.returncode != 0:
        raise ToolError(f"adb logcat dump failed with exit code {completed.returncode}:\n{completed.stderr}")
    path.write_text(completed.stdout, encoding="utf-8")
    print(f"Wrote {path}")


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    print(f"Wrote {path}")


def print_result(result: CaptureResult) -> None:
    print("Capture artifacts:")
    print(f"- raw_log: {result.raw_log}")
    print(f"- summary: {result.summary}")
    print(f"- crash_summary: {result.crash_summary}")


def capture_common(
    *,
    output_dir: Path,
    wait_ms: int,
    filters: list[str],
    summary_builder,
    event_parser,
    start,
) -> CaptureResult:
    output_dir.mkdir(parents=True, exist_ok=True)
    raw_log = output_dir / "raw.log"
    summary = output_dir / "summary.md"
    crash_summary_path = output_dir / "crash-summary.txt"

    device_prep()
    run_adb(["logcat", "-c"])
    start()
    print(f"Waiting {wait_ms} ms before dumping logcat...")
    time.sleep(wait_ms / 1000)
    dump_logcat(raw_log, filters)

    text = raw_log.read_text(encoding="utf-8", errors="replace")
    events = [event for line in text.splitlines() if (event := event_parser(line)) is not None]
    write_text(summary, summary_builder(events))

    from . import crash_summary

    lines = crash_summary.read_lines(raw_log)
    blocks = crash_summary.extract_blocks(lines, context=8)
    crash_text = build_crash_text(raw_log, lines, blocks, max_blocks=40)
    write_text(crash_summary_path, crash_text)
    result = CaptureResult(raw_log=raw_log, summary=summary, crash_summary=crash_summary_path)
    print_result(result)
    return result


def build_crash_text(path: Path, lines: list[str], blocks, max_blocks: int) -> str:
    from . import crash_summary

    output: list[str] = []
    output.append(f"file: {path}")
    output.append(f"total_lines: {len(lines)}")
    output.append("pattern_hits:")
    for pattern, count in crash_summary.summarize_patterns(lines):
        output.append(f"  {pattern}: {count}")
    output.append("")
    output.append("extracted_blocks:")
    for block in blocks[:max_blocks]:
        output.append("")
        output.append(f"=== line {block.line_number} trigger={block.trigger} ===")
        output.extend(block.lines)
    output.append("")
    return "\n".join(output)
