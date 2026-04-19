from __future__ import annotations

import argparse

from ..constants import DEFAULT_BUILD_DIR, ROOT_DIR


RAW_FORMATTER = argparse.RawDescriptionHelpFormatter


def add_common_build_dir_argument(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--build-dir",
        default=str(DEFAULT_BUILD_DIR.relative_to(ROOT_DIR)),
        help="Build directory relative to repo root or absolute path.",
    )
