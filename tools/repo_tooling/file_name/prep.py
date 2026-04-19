from __future__ import annotations

import argparse
from pathlib import Path

from ..constants import ROOT_DIR
from ..history.output import write_output
from .analyze import build_candidate_review
from .collect import collect_git_candidates, representative_siblings
from .model import FileNamePrepResult
from .render import render_result


DEFAULT_MESSAGE_PATH = ROOT_DIR / "temp" / "file-name" / "message.txt"


def build_file_name_prep_result() -> FileNamePrepResult:
    candidate_paths = collect_git_candidates()
    reviews = []
    for path in candidate_paths:
        reading = ["`.agent/guides/file-name-styles.md`"]
        reading.extend(f"`{item}`" for item in representative_siblings(path))
        reviews.append(build_candidate_review(path, suggested_reading=reading))

    return FileNamePrepResult(
        candidates=reviews,
        candidate_paths=candidate_paths,
        message_path=str(DEFAULT_MESSAGE_PATH.relative_to(ROOT_DIR)).replace("\\", "/"),
        source_description="git status --short (added + renamed files)",
    )


def cmd_file_name_prep(args: argparse.Namespace) -> None:
    _ = args
    result = build_file_name_prep_result()
    output = render_result(result)
    write_output(Path(DEFAULT_MESSAGE_PATH), output)
    print(
        f"Wrote file-name prep draft to {result.message_path} "
        f"for {len(result.candidate_paths)} candidate file(s)."
    )
