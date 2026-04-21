from __future__ import annotations

import argparse
import shutil
import subprocess

from ..build_config import load_build_config
from ..constants import ANDROID_GRADLE_ROOT
from ..errors import ToolError
from ..paths import gradle_wrapper
from ..process import print_command, run


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
        "gradle_args": ("-Pflipbits.android.modulesSmoke=true",),
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


def _resolve_sdkmanager() -> str:
    sdkmanager = shutil.which("sdkmanager")
    if sdkmanager is None:
        raise ToolError("Could not find 'sdkmanager' on PATH.")
    return sdkmanager


def _accept_android_sdk_licenses(sdkmanager: str) -> None:
    command = [sdkmanager, "--licenses"]
    print_command(command)
    completed = subprocess.run(
        command,
        input="y\n" * 128,
        text=True,
    )
    if completed.returncode != 0:
        raise SystemExit(completed.returncode)


def _install_android_sdk_components(*, accept_licenses: bool) -> None:
    config = load_build_config()
    sdkmanager = _resolve_sdkmanager()
    if accept_licenses:
        _accept_android_sdk_licenses(sdkmanager)
    run([sdkmanager, *config.android_sdk.components])


def cmd_android(args: argparse.Namespace) -> None:
    if args.action == "install-sdk":
        _install_android_sdk_components(accept_licenses=args.accept_licenses)
        return

    command = gradle_wrapper()
    if args.clean:
        command.append("clean")
    action = ANDROID_ACTIONS[args.action]
    command.extend(action["gradle_args"])
    command.extend(action["tasks"])
    run(command, cwd=ANDROID_GRADLE_ROOT)
