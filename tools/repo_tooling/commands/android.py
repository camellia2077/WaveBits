from __future__ import annotations

import argparse

from ..constants import ANDROID_GRADLE_ROOT
from ..paths import gradle_wrapper
from ..process import run


ANDROID_ACTIONS = {
    "assemble-debug": {
        "tasks": (":app:assembleDebug",),
        "gradle_args": (),
    },
    "assemble-release": {
        "tasks": (":app:assembleRelease",),
        "gradle_args": (),
    },
    "native-debug": {
        "tasks": (":app:externalNativeBuildDebug",),
        "gradle_args": (),
    },
    "modules-smoke": {
        "tasks": (":app:externalNativeBuildDebug",),
        "gradle_args": ("-Pwavebits.android.modulesSmoke=true",),
    },
    "ktlint-check": {
        "tasks": (":app:ktlintCheck",),
        "gradle_args": (),
    },
    "ktlint-format": {
        "tasks": (":app:ktlintFormat",),
        "gradle_args": (),
    },
    "detekt": {
        "tasks": (":app:detekt",),
        "gradle_args": (),
    },
    "quality": {
        "tasks": (
            ":app:ktlintCheck",
            ":app:detekt",
        ),
        "gradle_args": (),
    },
}


def cmd_android(args: argparse.Namespace) -> None:
    command = gradle_wrapper()
    if args.clean:
        command.append("clean")
    action = ANDROID_ACTIONS[args.action]
    command.extend(action["gradle_args"])
    command.extend(action["tasks"])
    run(command, cwd=ANDROID_GRADLE_ROOT)
