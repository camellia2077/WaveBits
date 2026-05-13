from __future__ import annotations

import argparse
from pathlib import Path
import subprocess
import sys


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.android_debug import capture, flash_alignment
from repo_tooling.commands import android_debug


def test_start_activity_quotes_shell_extras_with_spaces(monkeypatch) -> None:
    calls: list[list[str]] = []

    def fake_run_adb(args: list[str], *, capture: bool = False, check: bool = True):
        calls.append(args)
        return subprocess.CompletedProcess(["adb", *args], 0, "", "")

    monkeypatch.setattr(capture, "run_adb", fake_run_adb)

    capture.start_activity(
        capture.FLASH_ACTION,
        ["--es", "wb.input", "flash capture smoke", "--el", "wb.play.ms", "1000"],
    )

    assert calls == [
        [
            "shell",
            "am start -n com.bag.audioandroid/.MainActivity "
            "-a com.bag.audioandroid.DEBUG_FLASH_SCENARIO "
            "--es wb.input 'flash capture smoke' --el wb.play.ms 1000",
        ]
    ]


def test_capture_flash_defaults_ui_play_ms_to_longer_duration(monkeypatch, tmp_path) -> None:
    started: list[tuple[str, list[str]]] = []
    captured: list[dict] = []

    def fake_start_activity(action: str, extras: list[str]) -> None:
        started.append((action, list(extras)))

    def fake_capture_common(**kwargs):
        kwargs["start"]()
        captured.append(kwargs)
        return capture.CaptureResult(
            raw_log=tmp_path / "raw.log",
            summary=tmp_path / "summary.md",
            crash_summary=tmp_path / "crash-summary.txt",
        )

    monkeypatch.setattr(capture, "start_activity", fake_start_activity)
    monkeypatch.setattr(capture, "capture_common", fake_capture_common)

    android_debug.cmd_android_debug(
        argparse.Namespace(
            action="capture-flash",
            output_dir=tmp_path,
            wait_ms=90000,
            scenario="ui",
            style="litany",
            display="mix",
            visual="lanes",
            input_text=None,
            sample_length="long",
            sample_id=None,
            playback_speed=0.1,
            play_ms=None,
            no_encode=False,
            no_play=False,
            max_rows=24,
        )
    )

    assert captured[0]["wait_ms"] == 90000
    assert started[0][0] == capture.FLASH_ACTION
    assert started[0][1][started[0][1].index("wb.display") + 1] == "mix"
    assert started[0][1][started[0][1].index("wb.playback.speed") + 1] == "0.1"
    assert "--el" in started[0][1]
    assert started[0][1][started[0][1].index("wb.play.ms") + 1] == "30000"


def test_capture_flash_defaults_headless_play_ms_to_short_duration(monkeypatch, tmp_path) -> None:
    started: list[list[str]] = []

    def fake_start_activity(_action: str, extras: list[str]) -> None:
        started.append(list(extras))

    def fake_capture_common(**kwargs):
        kwargs["start"]()
        return capture.CaptureResult(
            raw_log=tmp_path / "raw.log",
            summary=tmp_path / "summary.md",
            crash_summary=tmp_path / "crash-summary.txt",
        )

    monkeypatch.setattr(capture, "start_activity", fake_start_activity)
    monkeypatch.setattr(capture, "capture_common", fake_capture_common)

    android_debug.cmd_android_debug(
        argparse.Namespace(
            action="capture-flash",
            output_dir=tmp_path,
            wait_ms=12000,
            scenario="headless",
            style="litany",
            display="lyrics",
            visual="lanes",
            input_text="flash smoke",
            sample_length=None,
            sample_id=None,
            playback_speed=1.0,
            play_ms=None,
            no_encode=False,
            no_play=False,
            max_rows=24,
        )
    )

    assert started[0][started[0].index("wb.play.ms") + 1] == "6000"
    assert started[0][started[0].index("wb.input") + 1] == "flash smoke"


def test_capture_common_writes_summary_and_crash_files_without_device(monkeypatch, tmp_path) -> None:
    started: list[bool] = []
    raw_text = (
        "05-11 15:25:06.660 D/FlashAutomation( 4229): "
        "received scenario=ui style=litany visual=ToneTracks encode=true play=true "
        "playMs=30000 requestId=158 input=text chars=14\n"
        "05-11 15:25:07.156 D/FlashAutomation( 4229): "
        "inputResolved requestId=158 source=text sampleId= chars=14 payloadBytes=14 style=litany\n"
        "05-11 15:25:10.256 D/FlashLyricsPerf( 4229): "
        "playing=true sample=60393 token=0 tokenText=flash tokenStart=59535 tokenEnd=897435 "
        "tokenProgress=0.00 displayLine=0 displayRange=0-3 sourceLine=0 "
        "sourceLineText=flash_ui_smoke byte=0 bit=0 tone=true textTokens=3 lineRanges=1\n"
    )

    monkeypatch.setattr(capture, "device_prep", lambda: None)
    monkeypatch.setattr(capture.time, "sleep", lambda _seconds: None)
    monkeypatch.setattr(capture, "run_adb", lambda *_args, **_kwargs: None)

    def fake_dump_logcat(path: Path, _filters: list[str]) -> None:
        path.write_text(raw_text, encoding="utf-8")

    monkeypatch.setattr(capture, "dump_logcat", fake_dump_logcat)

    result = capture.capture_common(
        output_dir=tmp_path,
        wait_ms=1,
        filters=capture.FLASH_LOGCAT_FILTERS,
        event_parser=flash_alignment.parse_event,
        summary_builder=lambda events: flash_alignment.build_summary(events, max_rows=8),
        start=lambda: started.append(True),
    )

    assert started == [True]
    assert result.raw_log.read_text(encoding="utf-8") == raw_text
    summary = result.summary.read_text(encoding="utf-8")
    assert "Scenario: scenario=ui style=litany" in summary
    assert "Input: source=text" in summary
    assert "Lyrics active-token rows: 1" in summary
    crash_summary = result.crash_summary.read_text(encoding="utf-8")
    assert "total_lines: 3" in crash_summary
    assert "pattern_hits:" in crash_summary
