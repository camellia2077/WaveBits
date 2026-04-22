from __future__ import annotations

import argparse
import shutil
import subprocess
from pathlib import Path

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

RELEASE_SIGNING_PROPERTIES_PATH = ANDROID_GRADLE_ROOT / "app" / "release-signing.properties"
RELEASE_SIGNING_DIRECTORY_PATH = ANDROID_GRADLE_ROOT / "app"
RELEASE_APK_OUTPUT_PATH = (
    ANDROID_GRADLE_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"
)


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


def _read_release_signing_properties() -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in RELEASE_SIGNING_PROPERTIES_PATH.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def _resolve_release_keystore_path(store_file_value: str) -> Path:
    keystore_path = Path(store_file_value).expanduser()
    if keystore_path.is_absolute():
        return keystore_path
    return (RELEASE_SIGNING_DIRECTORY_PATH / keystore_path).resolve()


def _ensure_release_signing_config_exists() -> None:
    if not RELEASE_SIGNING_PROPERTIES_PATH.exists():
        raise ToolError(
            "Missing Android release signing file.\n"
            f"Directory: {RELEASE_SIGNING_DIRECTORY_PATH}\n"
            "Please place the required file at:\n"
            f"{RELEASE_SIGNING_PROPERTIES_PATH}"
        )

    signing_properties = _read_release_signing_properties()
    store_file_value = signing_properties.get("storeFile")
    if not store_file_value:
        raise ToolError(
            "Missing 'storeFile' in Android release signing config.\n"
            f"Please update: {RELEASE_SIGNING_PROPERTIES_PATH}"
        )

    resolved_keystore_path = _resolve_release_keystore_path(store_file_value)
    if resolved_keystore_path.exists():
        return

    raise ToolError(
        "Missing Android release keystore file.\n"
        f"Configured by: {RELEASE_SIGNING_PROPERTIES_PATH}\n"
        f"Directory: {RELEASE_SIGNING_DIRECTORY_PATH}\n"
        f"Configured storeFile: {store_file_value}\n"
        f"Resolved keystore path: {resolved_keystore_path}"
    )


def _print_release_apk_path_if_present() -> None:
    if RELEASE_APK_OUTPUT_PATH.exists():
        print(f"Release APK: {RELEASE_APK_OUTPUT_PATH}")


def cmd_android(args: argparse.Namespace) -> None:
    if args.action == "install-sdk":
        _install_android_sdk_components(accept_licenses=args.accept_licenses)
        return

    if args.action == "assemble-release":
        _ensure_release_signing_config_exists()

    command = gradle_wrapper()
    if args.clean:
        command.append("clean")
    action = ANDROID_ACTIONS[args.action]
    command.extend(action["gradle_args"])
    command.extend(action["tasks"])
    run(command, cwd=ANDROID_GRADLE_ROOT)

    if args.action == "assemble-release":
        _print_release_apk_path_if_present()
