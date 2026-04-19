from __future__ import annotations

from dataclasses import dataclass
import os
import shlex
import subprocess
import sys
from threading import Thread
from pathlib import Path
from typing import IO, Sequence

from .constants import ROOT_DIR


def quote(arg: str) -> str:
    if os.name == "nt":
        return subprocess.list2cmdline([arg])
    return shlex.quote(arg)


def print_command(command: Sequence[str], cwd: Path | None = None) -> None:
    location = f" (cwd={cwd})" if cwd is not None else ""
    rendered = " ".join(quote(part) for part in command)
    print(f"+ {rendered}{location}", flush=True)


def run(
    command: Sequence[str],
    cwd: Path | None = None,
    env: dict[str, str] | None = None,
) -> None:
    print_command(command, cwd)
    completed = subprocess.run(command, cwd=cwd or ROOT_DIR, env=env)
    if completed.returncode != 0:
        raise SystemExit(completed.returncode)


@dataclass
class CapturedProcessResult:
    command: list[str]
    cwd: Path
    returncode: int
    stdout: str
    stderr: str


def run_capture(
    command: Sequence[str],
    cwd: Path | None = None,
    *,
    echo: bool = True,
) -> CapturedProcessResult:
    effective_cwd = cwd or ROOT_DIR
    if echo:
        print_command(command, effective_cwd)
    completed = subprocess.run(
        command,
        cwd=effective_cwd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=False,
    )
    stdout = completed.stdout.decode("utf-8", errors="replace")
    stderr = completed.stderr.decode("utf-8", errors="replace")
    return CapturedProcessResult(
        command=list(command),
        cwd=effective_cwd,
        returncode=completed.returncode,
        stdout=stdout,
        stderr=stderr,
    )


def _stream_text(pipe: IO[str] | None, sink: IO[str], chunks: list[str]) -> None:
    if pipe is None:
        return

    try:
        for chunk in iter(lambda: pipe.readline(), ""):
            chunks.append(chunk)
            sink.write(chunk)
            sink.flush()
    finally:
        pipe.close()


def run_capture_streaming(command: Sequence[str], cwd: Path | None = None) -> CapturedProcessResult:
    effective_cwd = cwd or ROOT_DIR
    print_command(command, effective_cwd)
    process = subprocess.Popen(
        command,
        cwd=effective_cwd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="replace",
        bufsize=1,
    )

    stdout_chunks: list[str] = []
    stderr_chunks: list[str] = []
    stdout_thread = Thread(
        target=_stream_text,
        args=(process.stdout, sys.stdout, stdout_chunks),
        daemon=True,
    )
    stderr_thread = Thread(
        target=_stream_text,
        args=(process.stderr, sys.stderr, stderr_chunks),
        daemon=True,
    )
    stdout_thread.start()
    stderr_thread.start()

    returncode = process.wait()
    stdout_thread.join()
    stderr_thread.join()

    return CapturedProcessResult(
        command=list(command),
        cwd=effective_cwd,
        returncode=returncode,
        stdout="".join(stdout_chunks),
        stderr="".join(stderr_chunks),
    )


def run_capture_merged_streaming(command: Sequence[str], cwd: Path | None = None) -> CapturedProcessResult:
    effective_cwd = cwd or ROOT_DIR
    print_command(command, effective_cwd)
    process = subprocess.Popen(
        command,
        cwd=effective_cwd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        bufsize=1,
    )

    output_chunks: list[str] = []
    if process.stdout is not None:
        try:
            for chunk in iter(lambda: process.stdout.readline(), ""):
                output_chunks.append(chunk)
                sys.stdout.write(chunk)
                sys.stdout.flush()
        finally:
            process.stdout.close()

    returncode = process.wait()
    return CapturedProcessResult(
        command=list(command),
        cwd=effective_cwd,
        returncode=returncode,
        stdout="".join(output_chunks),
        stderr="",
    )
