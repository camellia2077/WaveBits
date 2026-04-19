from __future__ import annotations

import argparse

from ...commands import cmd_message_prep
from ..common import RAW_FORMATTER


def register_message_group(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    message_parser = subparsers.add_parser(
        "message",
        help="Prepare agent-facing git commit message drafts.",
        description=(
            "Git commit message helper commands.\n\n"
            "Use this group after release-history has been drafted or landed.\n"
            "- `prep`: read history-first context and write a rewriteable commit message draft."
        ),
        formatter_class=RAW_FORMATTER,
    )
    message_subparsers = message_parser.add_subparsers(dest="message_command", required=True)

    message_prep_parser = message_subparsers.add_parser(
        "prep",
        help="Build a commit message draft from history-first context.",
        description=(
            "Prepare a git commit message draft for the current working tree.\n\n"
            "Behavior:\n"
            "- Prefers explicitly provided history markdown files.\n"
            "- Otherwise looks for changed history markdown files in the current git working tree.\n"
            "- Falls back to a conservative git-based scaffold only when no history file can be identified.\n"
            "- Writes the draft to `temp/message.txt` using UTF-8."
        ),
        formatter_class=RAW_FORMATTER,
    )
    message_prep_parser.add_argument(
        "--history",
        action="append",
        help="Optional history markdown file. Repeat to combine multiple history files in one draft.",
    )
    message_prep_parser.set_defaults(func=cmd_message_prep)
