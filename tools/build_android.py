#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build the Android app in debug or release mode."
    )
    parser.add_argument(
        "variant",
        choices=("debug", "release"),
        help="Build variant to assemble.",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Run Gradle clean before assembling.",
    )
    return parser.parse_args()


def gradle_wrapper(root: Path) -> Path:
    return root / ("gradlew.bat" if os.name == "nt" else "gradlew")


def assemble_task(variant: str) -> str:
    return f":app:assemble{variant.capitalize()}"


def apk_path(root: Path, variant: str) -> Path:
    return root / "apps" / "audio_android" / "app" / "build" / "outputs" / "apk" / variant / f"app-{variant}.apk"


def main() -> int:
    args = parse_args()
    root = Path(__file__).resolve().parent.parent
    wrapper = gradle_wrapper(root)
    if not wrapper.exists():
        print(f"Gradle wrapper not found: {wrapper}", file=sys.stderr)
        return 1

    command = [str(wrapper), assemble_task(args.variant)]
    if args.clean:
        command.insert(1, "clean")

    print(f"Building Android {args.variant} variant...")
    print(" ".join(command))
    subprocess.run(command, cwd=root, check=True)

    output_apk = apk_path(root, args.variant)
    if output_apk.exists():
        print(f"APK: {output_apk}")
    else:
        print(f"Build finished, but APK path was not found yet: {output_apk}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
