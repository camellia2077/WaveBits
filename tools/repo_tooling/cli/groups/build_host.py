from __future__ import annotations

import argparse

from ...commands import cmd_build, cmd_clean, cmd_configure, cmd_test, cmd_test_lib
from ...constants import DEFAULT_GENERATOR
from ..common import RAW_FORMATTER, add_common_build_dir_argument


def register_build_host_commands(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
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
            "- android: run the Gradle wrapper in apps/audio_android with `clean`.\n"
            "- artifacts: remove build/test-artifacts and dist/android.\n"
            "- python: remove repository-local Python cache directories and bytecode.\n"
            "- all: expand to host + android + artifacts + python.\n\n"
            "Examples:\n"
            "- python tools/run.py clean\n"
            "- python tools/run.py clean --scope artifacts --scope python\n"
            "- python tools/run.py clean --scope all --dry-run"
        ),
        formatter_class=RAW_FORMATTER,
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

    test_lib_parser = subparsers.add_parser(
        "test-lib",
        help="Run ctest for a single library test subtree.",
        description=(
            "Run library-scoped ctest from the library build subdirectory.\n\n"
            "Behavior:\n"
            "- Uses build/<dir>/libs/<library>/ as the ctest root.\n"
            "- This is the supported way to run runtime/api/unit library tests directly.\n"
            "- Root-level `ctest -R runtime_tests|api_tests|unit_tests` is no longer the supported workflow.\n\n"
            "Examples:\n"
            "- python tools/run.py test-lib audio_runtime\n"
            "- python tools/run.py test-lib audio_api --tests-regex api_tests\n"
            "- python tools/run.py test-lib audio_io --build-dir build/dev"
        ),
        formatter_class=RAW_FORMATTER,
    )
    add_common_build_dir_argument(test_lib_parser)
    test_lib_parser.add_argument(
        "library",
        choices=["audio_runtime", "audio_api", "audio_io"],
        help="Library test subtree to run.",
    )
    test_lib_parser.add_argument(
        "--no-output-on-failure",
        dest="output_on_failure",
        action="store_false",
        help="Do not pass --output-on-failure to ctest.",
    )
    test_lib_parser.add_argument(
        "--tests-regex",
        help="Optional ctest regex filter passed via -R inside the selected library subtree.",
    )
    test_lib_parser.add_argument(
        "--report-dir",
        help=(
            "Optional report output directory. Defaults to "
            "build/test-artifacts/reports/<timestamp>/."
        ),
    )
    test_lib_parser.add_argument(
        "--no-report",
        dest="write_report",
        action="store_false",
        help="Do not write summary.json and run.log for this ctest run.",
    )
    test_lib_parser.set_defaults(output_on_failure=True, write_report=True, func=cmd_test_lib)
