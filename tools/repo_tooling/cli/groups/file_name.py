from __future__ import annotations

import argparse

from ...commands import cmd_file_name_prep
from ..common import RAW_FORMATTER


def register_file_name_group(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    file_name_parser = subparsers.add_parser(
        "file-name",
        help="Prepare agent-facing file-name review notes.",
        description=(
            "File-name helper commands.\n\n"
            "Use this group when the agent is creating or renaming files and wants a conservative\n"
            "mechanical review scaffold before making a final naming decision.\n"
            "- `prep`: inspect candidate file names and write an agent-readable draft under `temp/`."
        ),
        formatter_class=RAW_FORMATTER,
    )
    file_name_subparsers = file_name_parser.add_subparsers(dest="file_name_command", required=True)

    file_name_prep_parser = file_name_subparsers.add_parser(
        "prep",
        help="Inspect git candidate file names and write a conservative draft note.",
        description=(
            "Prepare file-name guidance for the current working tree.\n\n"
            "Behavior:\n"
            "- Reads `git status --short` and keeps newly added or renamed files as candidates.\n"
            "- Runs only high-confidence mechanical checks from `.agent/guides/file-name-styles.md`.\n"
            "- Separates semantic naming questions into an agent-judgment section.\n"
            "- Writes the draft note to `temp/file-name/message.txt` using UTF-8."
        ),
        formatter_class=RAW_FORMATTER,
    )
    file_name_prep_parser.set_defaults(func=cmd_file_name_prep)
