from __future__ import annotations

import argparse
from pathlib import Path

from ..constants import ROOT_DIR
from .collect import normalize_target_history_file
from .infer import build_history_prep_result
from .output import write_output, write_split_outputs
from .render import render_result


def cmd_history_prep(args: argparse.Namespace) -> None:
    target_history_file = normalize_target_history_file(args.target) if args.target else None
    result = build_history_prep_result(args.scope or [], target_history_file=target_history_file)
    relevant_only = args.view == "relevant"
    output = render_result(result, output_format=args.format, relevant_only=relevant_only)

    if args.out_dir:
        out_dir = Path(args.out_dir)
        if not out_dir.is_absolute():
            out_dir = ROOT_DIR / out_dir
        out_dir = out_dir.resolve()
        if args.split_by == "bucket":
            write_split_outputs(result, output_format=args.format, relevant_only=relevant_only, out_dir=out_dir)
        else:
            extension = "json" if args.format == "json" else "md" if args.format == "markdown" else "txt"
            write_output(out_dir / f"history_prep.{extension}", output)

    print(output, end="")
