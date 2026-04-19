from __future__ import annotations

import argparse

from ...commands import cmd_android
from ..common import RAW_FORMATTER


def register_android_group(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    android_parser = subparsers.add_parser(
        "android",
        help="Run the standard Android Gradle tasks from apps/audio_android.",
        description=(
            "Run Android Gradle tasks from apps/audio_android.\n\n"
            "Behavior:\n"
            "- Uses the Gradle wrapper inside apps/audio_android.\n"
            "- Resolves the action to the matching :app Gradle task.\n"
            "- Optionally prepends `clean` before the selected task.\n"
            "- `ktlint-check` / `ktlint-format` / `detekt` provide Kotlin quality tooling for apps/audio_android.\n"
            "- `quality` is the minimal Android Kotlin quality gate: ktlintCheck + detekt.\n"
            "- `modules-smoke` enables the opt-in Android named-modules smoke target for the Phase 3A direct-owner shift.\n"
            "- It does not claim Android host-style `import std;` readiness."
        ),
        formatter_class=RAW_FORMATTER,
    )
    android_parser.add_argument(
        "action",
        choices=[
            "assemble-debug",
            "assemble-release",
            "native-debug",
            "modules-smoke",
            "ktlint-check",
            "ktlint-format",
            "detekt",
            "quality",
        ],
        help="Android build action.",
    )
    android_parser.add_argument(
        "--clean",
        action="store_true",
        help="Prepend Gradle clean before the selected task.",
    )
    android_parser.set_defaults(func=cmd_android)
