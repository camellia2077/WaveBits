from __future__ import annotations

import argparse
from pathlib import Path

from ..constants import ROOT_DIR
from ..history.output import write_output
from .infer import build_git_fallback_result, build_history_message_result, resolve_history_inputs
from .render import render_result


DEFAULT_MESSAGE_PATH = ROOT_DIR / "temp" / "message.txt"


def cmd_message_prep(args: argparse.Namespace) -> None:
    history_paths = resolve_history_inputs(args.history)
    if history_paths:
        result = build_history_message_result(history_paths)
    else:
        result = build_git_fallback_result()

    output = render_result(result)
    write_output(Path(DEFAULT_MESSAGE_PATH), output)

    if history_paths:
        joined = ", ".join(history_paths)
        print(f"Wrote commit message draft to temp/message.txt from history: {joined}")
    else:
        print("Wrote commit message draft to temp/message.txt from git fallback context.")
