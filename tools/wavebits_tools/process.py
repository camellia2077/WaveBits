from __future__ import annotations

from dataclasses import dataclass
import os
import shlex
import subprocess
from pathlib import Path
from typing import Sequence

from .constants import ROOT_DIR


def quote(arg: str) -> str:
    if os.name == "nt":
        return subprocess.list2cmdline([arg])
    return shlex.quote(arg)


def print_command(command: Sequence[str], cwd: Path | None = None) -> None:
    location = f" (cwd={cwd})" if cwd is not None else ""
    rendered = " ".join(quote(part) for part in command)
    print(f"+ {rendered}{location}", flush=True)


def run(command: Sequence[str], cwd: Path | None = None) -> None:
    print_command(command, cwd)
    completed = subprocess.run(command, cwd=cwd or ROOT_DIR)
    if completed.returncode != 0:
        raise SystemExit(completed.returncode)


@dataclass
class CapturedProcessResult:
    command: list[str]
    cwd: Path
    returncode: int
    stdout: str
    stderr: str


def run_capture(command: Sequence[str], cwd: Path | None = None) -> CapturedProcessResult:
    effective_cwd = cwd or ROOT_DIR
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
