from __future__ import annotations

import argparse

from ..constants import ANDROID_GRADLE_ROOT
from ..paths import gradle_wrapper
from ..process import run


ANDROID_ACTIONS = {
    "assemble-debug": {
        "task": ":app:assembleDebug",
        "gradle_args": (),
    },
    "assemble-release": {
        "task": ":app:assembleRelease",
        "gradle_args": (),
    },
    "native-debug": {
        "task": ":app:externalNativeBuildDebug",
        "gradle_args": (),
    },
    "modules-smoke": {
        "task": ":app:externalNativeBuildDebug",
        "gradle_args": ("-Pwavebits.android.modulesSmoke=true",),
    },
}


def cmd_android(args: argparse.Namespace) -> None:
    command = gradle_wrapper()
    if args.clean:
        command.append("clean")
    action = ANDROID_ACTIONS[args.action]
    command.extend(action["gradle_args"])
    command.append(action["task"])
    run(command, cwd=ANDROID_GRADLE_ROOT)
