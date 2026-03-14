from __future__ import annotations

import argparse
import sys
from typing import Sequence

from .commands import (
    cmd_android,
    cmd_build,
    cmd_clean,
    cmd_configure,
    cmd_export_apk,
    cmd_roundtrip,
    cmd_smoke,
    cmd_test,
    cmd_verify,
)
from .commands.verify import format_verify_check_groups
from .constants import DEFAULT_BUILD_DIR, DEFAULT_GENERATOR, ROOT_DIR
from .errors import ToolError
from .paths import ensure_root


def add_common_build_dir_argument(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--build-dir",
        default=str(DEFAULT_BUILD_DIR.relative_to(ROOT_DIR)),
        help="Build directory relative to repo root or absolute path.",
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="run.py",
        description="WaveBits developer orchestration tools.",
        epilog="Use `python tools/run.py <command> --help` to view detailed options for a specific command.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    configure_parser = subparsers.add_parser("configure", help="Configure the root CMake build.")
    add_common_build_dir_argument(configure_parser)
    configure_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use. Defaults to Ninja. The only supported root-host generator is Ninja.",
    )
    configure_parser.set_defaults(func=cmd_configure)

    build_parser_cmd = subparsers.add_parser("build", help="Build the configured CMake tree.")
    add_common_build_dir_argument(build_parser_cmd)
    build_parser_cmd.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use if auto-configuring. The only supported root-host generator is Ninja.",
    )
    build_parser_cmd.add_argument(
        "--target",
        action="append",
        help="Optional build target. Repeat to pass multiple targets.",
    )
    build_parser_cmd.add_argument(
        "--no-configure-if-missing",
        dest="configure_if_missing",
        action="store_false",
        help="Do not auto-run configure when the build directory is missing CMakeCache.txt.",
    )
    build_parser_cmd.set_defaults(configure_if_missing=True, func=cmd_build)

    clean_parser = subparsers.add_parser(
        "clean",
        help="Remove generated host, Android, and artifact outputs.",
        description=(
            "Remove generated outputs.\n\n"
            "Scopes:\n"
            "- host: remove the selected CMake build directory.\n"
            "- android: run the root Gradle wrapper with `clean`.\n"
            "- artifacts: remove build/test-artifacts and dist/android.\n"
            "- python: remove repository-local Python cache directories and bytecode.\n"
            "- all: expand to host + android + artifacts + python.\n\n"
            "Examples:\n"
            "- python tools/run.py clean\n"
            "- python tools/run.py clean --scope artifacts --scope python\n"
            "- python tools/run.py clean --scope all --dry-run"
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    add_common_build_dir_argument(clean_parser)
    clean_parser.add_argument(
        "--scope",
        action="append",
        choices=["host", "android", "artifacts", "python", "all"],
        help="Clean scope to run. Repeat to combine scopes. Defaults to host.",
    )
    clean_parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the planned removals without deleting anything.",
    )
    clean_parser.set_defaults(func=cmd_clean)

    test_parser = subparsers.add_parser("test", help="Run ctest for the configured build.")
    add_common_build_dir_argument(test_parser)
    test_parser.add_argument(
        "--no-output-on-failure",
        dest="output_on_failure",
        action="store_false",
        help="Do not pass --output-on-failure to ctest.",
    )
    test_parser.add_argument(
        "--tests-regex",
        help="Optional ctest regex filter passed via -R.",
    )
    test_parser.add_argument(
        "--report-dir",
        help=(
            "Optional report output directory. Defaults to "
            "build/test-artifacts/reports/<timestamp>/."
        ),
    )
    test_parser.add_argument(
        "--no-report",
        dest="write_report",
        action="store_false",
        help="Do not write summary.json and run.log for this ctest run.",
    )
    test_parser.set_defaults(output_on_failure=True, write_report=True, func=cmd_test)

    android_parser = subparsers.add_parser(
        "android",
        help="Run the standard Android Gradle tasks from the repo root.",
        description=(
            "Run Android Gradle tasks from the repository root.\n\n"
            "Behavior:\n"
            "- Uses the root Gradle wrapper, not apps/audio_android as a separate Gradle root.\n"
            "- Resolves the action to the matching :app Gradle task.\n"
            "- Optionally prepends `clean` before the selected task.\n"
            "- `modules-smoke` enables the opt-in Android named-modules smoke target for the Phase 3A direct-owner shift.\n"
            "- It does not claim Android host-style `import std;` readiness."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    android_parser.add_argument(
        "action",
        choices=["assemble-debug", "assemble-release", "native-debug", "modules-smoke"],
        help="Android build action.",
    )
    android_parser.add_argument(
        "--clean",
        action="store_true",
        help="Prepend Gradle clean before the selected task.",
    )
    android_parser.set_defaults(func=cmd_android)

    export_apk_parser = subparsers.add_parser(
        "export-apk",
        help="Copy a built Android APK from Gradle outputs into dist/android.",
        description=(
            "Export a built Android APK into a stable delivery directory.\n\n"
            "Behavior:\n"
            "- Reads Gradle APK metadata from apps/audio_android/app/build/outputs/apk/<variant>/.\n"
            "- Copies the selected APK into dist/android by default.\n"
            "- Can optionally build the APK first with --assemble-if-missing."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
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

    verify_parser = subparsers.add_parser(
        "verify",
        help="Run static policy checks + configure + build + ctest, then root Gradle :app:assembleDebug by default.",
        description=(
            "Run the full verify pipeline.\n\n"
            f"{format_verify_check_groups()}\n\n"
            "Behavior:\n"
            "- Runs static check groups before configure/build/test.\n"
            "- Builds and tests the selected host build directory.\n"
            "- Runs root Android assembleDebug unless --skip-android is passed.\n\n"
            "Use --list-checks to print the current static check groups without building."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    add_common_build_dir_argument(verify_parser)
    verify_parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use for configure. The only supported root-host generator is Ninja.",
    )
    verify_parser.add_argument(
        "--skip-android",
        action="store_true",
        help="Skip the root Gradle :app:assembleDebug step.",
    )
    verify_parser.add_argument(
        "--list-checks",
        action="store_true",
        help="Print the static check groups verify runs before building, then exit.",
    )
    verify_parser.set_defaults(func=cmd_verify)

    roundtrip_parser = subparsers.add_parser(
        "roundtrip",
        help="Generate a WAV artifact and decode it back to text via the CLI.",
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

    smoke_parser = subparsers.add_parser(
        "smoke",
        help="Generate visible roundtrip artifacts for representative flash/pro/ultra cases.",
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

    return parser


def main(argv: Sequence[str] | None = None) -> int:
    ensure_root()
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        args.func(args)
    except ToolError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1
    return 0
