from __future__ import annotations

import argparse

from ...commands import cmd_history_prep, cmd_history_validate
from ..common import RAW_FORMATTER


def register_history_group(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    history_parser = subparsers.add_parser(
        "history",
        help="Prepare and validate release-history markdown workflows.",
        description=(
            "Release-history helper commands.\n\n"
            "Use this group when the agent is drafting or checking release-history markdown.\n"
            "- `prep`: collect git + docs context and print a rewriteable scaffold.\n"
            "- `validate`: check markdown structure against repository hard rules."
        ),
        formatter_class=RAW_FORMATTER,
    )
    history_subparsers = history_parser.add_subparsers(dest="history_command", required=True)

    history_prep_parser = history_subparsers.add_parser(
        "prep",
        help="Collect git + docs version hints and print a release-history scaffold.",
        description=(
            "Prepare release-history context for the current working tree.\n\n"
            "Behavior:\n"
            "- Reads `git status --short` to discover changed files.\n"
            "- Scans docs/libs and docs/presentation/* for the latest recorded versions.\n"
            "- Prints a scaffold the agent can rewrite into the final history entry.\n"
            "- This command assists the agent; it does not replace human/agent judgment."
        ),
        formatter_class=RAW_FORMATTER,
    )
    history_prep_parser.add_argument(
        "--format",
        default="markdown",
        choices=["markdown", "json", "plain"],
        help="Output format. Defaults to markdown. Use `plain` for lightweight agent-facing structured text.",
    )
    history_prep_parser.add_argument(
        "--scope",
        action="append",
        help="Optional repo scope filter such as `libs`, `libs/audio_io`, or `apps/audio_android`. Repeat to combine scopes.",
    )
    history_prep_parser.add_argument(
        "--target",
        help="Optional target history markdown file. When provided, the suggested version prefers the target filename such as docs/presentation/cli/v0.2/0.2.0.md -> v0.2.0.",
    )
    history_prep_parser.add_argument(
        "--view",
        default="full",
        choices=["full", "relevant"],
        help="Markdown view mode. `relevant` hides the full changed-file dump and keeps only the summary + draft scaffold.",
    )
    history_prep_parser.add_argument(
        "--out-dir",
        help="Optional output directory. Writes the rendered result under the directory while still printing stdout.",
    )
    history_prep_parser.add_argument(
        "--split-by",
        choices=["bucket"],
        help="Optional split mode. `bucket` writes one file per relevant bucket plus index.json under --out-dir.",
    )
    history_prep_parser.set_defaults(func=cmd_history_prep)

    history_validate_parser = history_subparsers.add_parser(
        "validate",
        help="Validate release-history markdown against repository hard rules.",
        description=(
            "Validate release-history markdown files.\n\n"
            "Behavior:\n"
            "- Parses markdown with markdown-it-py.\n"
            "- Checks release heading format, section names, bullet markers, and version ordering.\n"
            "- Fails if unresolved `TODO(agent)` placeholders remain.\n"
            "- This command validates structure only; it does not judge release-note semantics."
        ),
        formatter_class=RAW_FORMATTER,
    )
    history_validate_parser.add_argument(
        "paths",
        nargs="+",
        help="Markdown history file(s) or directories to validate. Directories are scanned recursively for *.md.",
    )
    history_validate_parser.set_defaults(func=cmd_history_validate)
