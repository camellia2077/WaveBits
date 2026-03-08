from __future__ import annotations

import argparse

from ..constants import ANDROID_DIR
from ..paths import gradle_wrapper
from ..process import run


ANDROID_TASKS = {
    "assemble-debug": ":app:assembleDebug",
    "assemble-release": ":app:assembleRelease",
    "native-debug": ":app:externalNativeBuildDebug",
}


def cmd_android(args: argparse.Namespace) -> None:
    command = gradle_wrapper()
    if args.clean:
        command.append("clean")
    command.append(ANDROID_TASKS[args.action])
    run(command, cwd=ANDROID_DIR)
