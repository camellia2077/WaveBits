from __future__ import annotations

import argparse
import shutil
import subprocess
from pathlib import Path

from ..build_config import load_build_config
from ..constants import ANDROID_GRADLE_ROOT
from ..errors import ToolError
from ..paths import gradle_wrapper
from ..process import print_command, run, run_capture, run_capture_merged_streaming
from .android_kotlin_policy import cmd_android_kotlin_policy


ANDROID_ACTIONS = {
    "assemble-debug": {
        "tasks": (":app:assembleDebug",),
        "gradle_args": (),
    },
    "assemble-staging": {
        "tasks": (":app:assembleStaging",),
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
    "test-debug": {
        "tasks": (":app:testDebugUnitTest",),
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
    ANDROID_GRADLE_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "FlipBits-release.apk"
)
STAGING_APK_OUTPUT_PATH = (
    ANDROID_GRADLE_ROOT / "app" / "build" / "outputs" / "apk" / "staging" / "FlipBits-staging.apk"
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


def _print_staging_apk_path_if_present() -> None:
    if STAGING_APK_OUTPUT_PATH.exists():
        print(f"Staging APK: {STAGING_APK_OUTPUT_PATH}")


def _add_android_string_key(args: argparse.Namespace) -> None:
    missing_args = [
        option
        for option in ("file", "key", "en")
        if not getattr(args, option, None)
    ]
    if missing_args:
        formatted_args = ", ".join(f"--{option}" for option in missing_args)
        raise ToolError(
            "android strings-add requires resource filename, key, and English text.\n"
            f"Missing: {formatted_args}\n"
            "Example:\n"
            "  python tools/run.py android strings-add --file strings_audio.xml "
            "--key sample_key --en \"Sample text\""
        )
    command = [
        "python",
        "tools/scripts/android/translate/run.py",
        "add-key",
        "--file",
        args.file,
        "--key",
        args.key,
        "--en",
        args.en,
    ]
    if args.localized is not None:
        command.extend(["--localized", args.localized])
    if args.context is not None:
        command.extend(["--context", args.context])
    run(command)

    alignment_command = [
        "python",
        "tools/scripts/android/translate/run.py",
        "key-alignment",
        "--json-output",
    ]
    completed = run_capture(alignment_command)
    if completed.returncode == 0:
        print("[android] Translation key alignment is already complete.")
        return
    if completed.returncode == 2:
        print(
            "[android] Translation tasks were generated for missing localized keys.\n"
            "Review: temp/translation_key_alignment_reports/\n"
            "Use the Android translate workflow before relying on a localized build.",
            flush=True,
        )
        return
    print(completed.stdout, end="")
    print(completed.stderr, end="")
    raise SystemExit(completed.returncode)


def cmd_android(args: argparse.Namespace) -> None:
    if getattr(args, "action", None) == "strings-add":
        _add_android_string_key(args)
        return
    if args.action == "install-sdk":
        _install_android_sdk_components(accept_licenses=args.accept_licenses)
        return
    if args.action == "kotlin-policy":
        cmd_android_kotlin_policy()
        return

    if args.action == "assemble-release":
        _ensure_release_signing_config_exists()

    command = gradle_wrapper()
    if args.clean:
        command.append("clean")
    action = ANDROID_ACTIONS[args.action]
    command.extend(action["gradle_args"])
    command.extend(action["tasks"])
    if args.action == "ktlint-check":
        completed = run_capture_merged_streaming(command, cwd=ANDROID_GRADLE_ROOT)
        if completed.returncode != 0:
            print(
                "\n[android] ktlint-check failed. If the reported issues are auto-correctable, run:\n"
                "  python tools/run.py android ktlint-format\n"
                "Then rerun:\n"
                "  python tools/run.py android ktlint-check",
                flush=True,
            )
            raise SystemExit(completed.returncode)
    else:
        run(command, cwd=ANDROID_GRADLE_ROOT)

    if args.action == "assemble-staging":
        _print_staging_apk_path_if_present()
    if args.action == "assemble-release":
        _print_release_apk_path_if_present()
