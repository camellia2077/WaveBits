#!/usr/bin/env python3
from __future__ import annotations

import argparse
import subprocess
from pathlib import Path


REQUIRED_KEYS = (
    "STORE_PASSWORD",
    "KEY_ALIAS",
    "KEY_PASSWORD",
    "KEYSTORE_PATH",
    "DNAME",
)

DEFAULT_KEYSTORE_FILENAME = "flipbits-release.jks"
REPO_ROOT = Path(__file__).resolve().parents[3]
RELEASE_SIGNING_PROPERTIES_PATH = REPO_ROOT / "apps" / "audio_android" / "app" / "release-signing.properties"


def parse_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise ValueError(f"Invalid config line: {raw_line!r}")
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def require_values(values: dict[str, str]) -> dict[str, str]:
    missing = [key for key in REQUIRED_KEYS if not values.get(key)]
    if missing:
        missing_display = ", ".join(missing)
        raise ValueError(f"Missing required config value(s): {missing_display}")
    return {key: values[key] for key in REQUIRED_KEYS}


def resolve_keystore_file(config_path: Path, raw_target: str) -> Path:
    target = Path(raw_target).expanduser()
    if not target.is_absolute():
        target = (config_path.parent / target).resolve()

    if target.suffix.lower() == ".jks":
        return target

    return target / DEFAULT_KEYSTORE_FILENAME


def build_keytool_command(config_path: Path, values: dict[str, str]) -> tuple[list[str], Path]:
    keystore_file = resolve_keystore_file(
        config_path=config_path,
        raw_target=values["KEYSTORE_PATH"],
    )
    keystore_file.parent.mkdir(parents=True, exist_ok=True)

    command = [
        "keytool",
        "-genkeypair",
        "-v",
        "-keystore",
        str(keystore_file),
        "-alias",
        values["KEY_ALIAS"],
        "-storepass",
        values["STORE_PASSWORD"],
        "-keypass",
        values["KEY_PASSWORD"],
        "-keyalg",
        "RSA",
        "-keysize",
        "2048",
        "-validity",
        "36500",
        "-dname",
        values["DNAME"],
    ]
    return command, keystore_file


def build_release_signing_properties(values: dict[str, str], keystore_file: Path) -> str:
    keystore_path = keystore_file.as_posix()
    lines = (
        f"storeFile={keystore_path}",
        f"storePassword={values['STORE_PASSWORD']}",
        f"keyAlias={values['KEY_ALIAS']}",
        f"keyPassword={values['KEY_PASSWORD']}",
    )
    return "\n".join(lines) + "\n"


def write_release_signing_properties(values: dict[str, str], keystore_file: Path) -> None:
    RELEASE_SIGNING_PROPERTIES_PATH.parent.mkdir(parents=True, exist_ok=True)
    RELEASE_SIGNING_PROPERTIES_PATH.write_text(
        build_release_signing_properties(values, keystore_file),
        encoding="utf-8",
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate an Android release keystore from a local KEY=VALUE config file.",
    )
    parser.add_argument(
        "config",
        type=Path,
        help="Path to a local config file such as key.txt.",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Replace an existing .jks file if it already exists.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the resolved output path without running keytool.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    config_path = args.config.resolve()
    values = require_values(parse_env_file(config_path))
    command, keystore_file = build_keytool_command(config_path, values)

    if keystore_file.exists() and not args.overwrite:
        raise SystemExit(
            f"Refusing to overwrite existing keystore: {keystore_file}\n"
            "Pass --overwrite if you want to replace it.",
        )

    if args.dry_run:
        print(f"Resolved keystore output: {keystore_file}")
        print(f"Resolved release signing config: {RELEASE_SIGNING_PROPERTIES_PATH}")
        return 0

    subprocess.run(command, check=True)
    write_release_signing_properties(values, keystore_file)
    print(f"Created keystore: {keystore_file}")
    print(f"Created release signing config: {RELEASE_SIGNING_PROPERTIES_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
