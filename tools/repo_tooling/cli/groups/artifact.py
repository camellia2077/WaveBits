from __future__ import annotations

import argparse

from ...commands import cmd_export_apk, cmd_roundtrip, cmd_smoke
from ...constants import DEFAULT_GENERATOR
from ..common import RAW_FORMATTER, add_common_build_dir_argument


def register_artifact_group(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    artifact_parser = subparsers.add_parser(
        "artifact",
        help="Generate or export visible product artifacts.",
        description=(
            "Artifact helper commands.\n\n"
            "Use this group for visible test artifacts and final delivery artifacts.\n"
            "- `roundtrip`: generate one WAV artifact and decode it back to text.\n"
            "- `smoke`: generate a representative flash/pro/ultra artifact batch.\n"
            "- `export-apk`: copy a built Android APK into a stable delivery directory."
        ),
        formatter_class=RAW_FORMATTER,
    )
    artifact_subparsers = artifact_parser.add_subparsers(dest="artifact_command", required=True)

    export_apk_parser = artifact_subparsers.add_parser(
        "export-apk",
        help="Copy a built Android APK from Gradle outputs into dist/android.",
        description=(
            "Export a built Android APK into a stable delivery directory.\n\n"
            "Behavior:\n"
            "- Reads Gradle APK metadata from apps/audio_android/app/build/outputs/apk/<variant>/.\n"
            "- Copies the selected APK into dist/android by default.\n"
            "- Can optionally build the APK first with --assemble-if-missing."
        ),
        formatter_class=RAW_FORMATTER,
    )
    export_apk_parser.add_argument(
        "variant",
        nargs="?",
        default="debug",
        choices=["debug", "release"],
        help="APK variant to export. Defaults to debug.",
    )
    export_apk_parser.add_argument(
        "--out-dir",
        help="Optional export directory. Defaults to dist/android/.",
    )
    export_apk_parser.add_argument(
        "--filename",
        help="Optional exported filename. Defaults to <repo>-android-<variant>-v<version>.apk.",
    )
    export_apk_parser.add_argument(
        "--assemble-if-missing",
        action="store_true",
        help="Run the matching Gradle assemble task if the APK metadata is missing.",
    )
    export_apk_parser.set_defaults(func=cmd_export_apk)

    roundtrip_parser = artifact_subparsers.add_parser(
        "roundtrip",
        help="Generate one WAV artifact and decode it back to text.",
        description=(
            "Generate a single visible roundtrip artifact.\n\n"
            "Behavior:\n"
            "- Builds the CLI if needed.\n"
            "- Encodes the provided text into a WAV artifact.\n"
            "- Decodes the artifact back to text and stores visible outputs under build/test-artifacts/."
        ),
        formatter_class=RAW_FORMATTER,
    )
    add_common_build_dir_argument(roundtrip_parser)
    roundtrip_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use if auto-configuring/building the CLI target. The only supported root-host generator is Ninja.",
    )
    roundtrip_parser.add_argument(
        "--mode",
        default="flash",
        choices=["flash", "pro", "ultra"],
        help="Transport mode to use.",
    )
    roundtrip_parser.add_argument("--text", help="Inline input text.")
    roundtrip_parser.add_argument("--text-file", help="UTF-8 input text file.")
    roundtrip_parser.add_argument("--case-name", help="Optional artifact case directory name.")
    roundtrip_parser.add_argument(
        "--out-dir",
        help="Optional output directory. Defaults to build/test-artifacts/roundtrip/<case>/",
    )
    roundtrip_parser.set_defaults(func=cmd_roundtrip)

    smoke_parser = artifact_subparsers.add_parser(
        "smoke",
        help="Generate a representative flash/pro/ultra artifact batch.",
        description=(
            "Generate a visible smoke artifact batch.\n\n"
            "Behavior:\n"
            "- Builds the CLI if needed.\n"
            "- Produces representative flash / pro / ultra roundtrip artifacts.\n"
            "- Writes outputs under build/test-artifacts/smoke/ for manual inspection."
        ),
        formatter_class=RAW_FORMATTER,
    )
    add_common_build_dir_argument(smoke_parser)
    smoke_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use if auto-configuring/building the CLI target. The only supported root-host generator is Ninja.",
    )
    smoke_parser.add_argument(
        "--name",
        default="default",
        help="Artifact batch name under build/test-artifacts/smoke/.",
    )
    smoke_parser.set_defaults(func=cmd_smoke)
