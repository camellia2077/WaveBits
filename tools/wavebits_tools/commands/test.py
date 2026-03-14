from __future__ import annotations

import argparse
from datetime import datetime
import re
from time import perf_counter
from pathlib import Path
from typing import Any

from ..artifacts import test_artifacts_root, unique_directory, write_json, write_utf8
from ..constants import ROOT_DIR
from ..paths import configured_build_config, resolve_build_dir
from ..process import quote, run_capture_streaming


TEST_RESULT_RE = re.compile(
    r"^\s*(?P<ordinal>\d+)/(?P<total>\d+)\s+Test\s+#(?P<index>\d+):\s+"
    r"(?P<name>.+?)\s+\.{2,}\s+(?P<status>\*{0,3}[A-Za-z]+)\s+"
    r"(?P<duration>[0-9.]+)\s+sec\s*$"
)
SUMMARY_RE = re.compile(
    r"(?P<percent>\d+)% tests passed, (?P<failed>\d+) tests failed out of (?P<total>\d+)"
)
TOTAL_TIME_RE = re.compile(r"Total Test time \(real\) =\s+(?P<duration>[0-9.]+)\s+sec")


def _normalize_status(raw: str) -> str:
    status = raw.strip("*").lower()
    if status == "passed":
        return "passed"
    if status in {"failed", "timeout", "notrun"}:
        return "failed"
    if status == "skipped":
        return "skipped"
    return status


def _parse_ctest_output(stdout: str, returncode: int, measured_duration_sec: float) -> dict[str, Any]:
    suites: list[dict[str, Any]] = []
    for line in stdout.splitlines():
        match = TEST_RESULT_RE.match(line)
        if not match:
            continue
        suites.append(
            {
                "index": int(match.group("index")),
                "name": match.group("name").strip(),
                "status": _normalize_status(match.group("status")),
                "duration_sec": float(match.group("duration")),
            }
        )

    summary_match = SUMMARY_RE.search(stdout)
    if summary_match:
        total = int(summary_match.group("total"))
        failed = int(summary_match.group("failed"))
        passed = max(total - failed, 0)
    else:
        total = len(suites)
        passed = sum(1 for suite in suites if suite["status"] == "passed")
        failed = sum(1 for suite in suites if suite["status"] == "failed")

    skipped = sum(1 for suite in suites if suite["status"] == "skipped")
    total_time_match = TOTAL_TIME_RE.search(stdout)
    duration_sec = (
        float(total_time_match.group("duration"))
        if total_time_match
        else round(measured_duration_sec, 3)
    )
    status = "passed" if returncode == 0 and failed == 0 else "failed"
    return {
        "status": status,
        "total": total,
        "passed": passed,
        "failed": failed,
        "skipped": skipped,
        "duration_sec": duration_sec,
        "suites": suites,
    }


def _default_report_dir(build_dir: Path) -> Path:
    timestamp = datetime.now().strftime("ctest-%Y%m%d-%H%M%S")
    return unique_directory(test_artifacts_root(build_dir) / "reports" / timestamp)


def _resolve_report_dir(raw_report_dir: str | None, build_dir: Path) -> Path:
    if raw_report_dir:
        report_dir = Path(raw_report_dir)
        if not report_dir.is_absolute():
            report_dir = ROOT_DIR / report_dir
        report_dir.mkdir(parents=True, exist_ok=True)
        return report_dir
    return _default_report_dir(build_dir)


def _render_command(command: list[str]) -> str:
    return " ".join(quote(part) for part in command)


def _build_log_content(
    *,
    started_at: datetime,
    finished_at: datetime,
    command_cwd: Path,
    build_dir: Path,
    report_dir: Path,
    command: list[str],
    returncode: int,
    summary: dict[str, Any],
    stdout: str,
    stderr: str,
) -> str:
    lines = [
        "WaveBits ctest report",
        f"started_at: {started_at.isoformat()}",
        f"finished_at: {finished_at.isoformat()}",
        f"duration_sec: {summary['duration_sec']}",
        f"status: {summary['status']}",
        f"exit_code: {returncode}",
        f"cwd: {command_cwd}",
        f"build_dir: {build_dir}",
        f"report_dir: {report_dir}",
        f"command: {_render_command(command)}",
        f"total: {summary['total']}",
        f"passed: {summary['passed']}",
        f"failed: {summary['failed']}",
        f"skipped: {summary['skipped']}",
        "",
        "===== STDOUT =====",
        stdout.rstrip(),
    ]
    if stderr:
        lines.extend(["", "===== STDERR =====", stderr.rstrip()])
    return "\n".join(lines).rstrip() + "\n"


def cmd_test(args: argparse.Namespace) -> None:
    build_dir = resolve_build_dir(args.build_dir)
    command = ["ctest", "--test-dir", str(build_dir)]
    configured_build = configured_build_config(build_dir)
    if configured_build:
        command.extend(["-C", configured_build])
    if args.output_on_failure:
        command.append("--output-on-failure")
    if args.tests_regex:
        command.extend(["-R", args.tests_regex])
    started_at = datetime.now().astimezone()
    timer_started = perf_counter()
    result = run_capture_streaming(command)
    measured_duration_sec = perf_counter() - timer_started
    finished_at = datetime.now().astimezone()

    summary = _parse_ctest_output(result.stdout, result.returncode, measured_duration_sec)
    summary.update(
        {
            "exit_code": result.returncode,
            "started_at": started_at.isoformat(),
            "finished_at": finished_at.isoformat(),
            "build_dir": str(build_dir),
            "command": command,
            "tests_regex": args.tests_regex,
        }
    )

    write_report = getattr(args, "write_report", True)
    report_dir_arg = getattr(args, "report_dir", None)
    if write_report:
        report_dir = _resolve_report_dir(report_dir_arg, build_dir)
        summary_path = report_dir / "summary.json"
        log_path = report_dir / "run.log"
        write_json(summary_path, summary)
        write_utf8(
            log_path,
            _build_log_content(
                started_at=started_at,
                finished_at=finished_at,
                command_cwd=result.cwd,
                build_dir=build_dir,
                report_dir=report_dir,
                command=command,
                returncode=result.returncode,
                summary=summary,
                stdout=result.stdout,
                stderr=result.stderr,
            ),
        )
        print(f"Test report: {summary_path}")
        print(f"Test log: {log_path}")

    if result.returncode != 0:
        raise SystemExit(result.returncode)
