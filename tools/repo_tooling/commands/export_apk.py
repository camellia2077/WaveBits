from __future__ import annotations

import argparse
import json
import re
import shutil
from pathlib import Path
from typing import Any

from ..constants import ANDROID_APP_DIR, ANDROID_DIST_DIR, ROOT_DIR
from ..errors import ToolError
from .android import cmd_android


def _apk_metadata_path(variant: str) -> Path:
    return ANDROID_APP_DIR / "build" / "outputs" / "apk" / variant / "output-metadata.json"


def _resolve_output_directory(raw: str | None) -> Path:
    if not raw:
        return ANDROID_DIST_DIR
    path = Path(raw)
    if not path.is_absolute():
        path = ROOT_DIR / path
    return path


def _load_metadata(metadata_path: Path, variant: str) -> dict[str, Any]:
    try:
        payload = json.loads(metadata_path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise ToolError(
            f"Could not find Android APK metadata at {metadata_path}. "
            f"Run `python tools/run.py android assemble-{variant}` first or pass "
            "`--assemble-if-missing`."
        ) from exc
    except json.JSONDecodeError as exc:
        raise ToolError(f"Failed to parse APK metadata at {metadata_path}: {exc}") from exc
    return payload


def _resolve_single_apk(metadata: dict[str, Any], metadata_path: Path) -> tuple[Path, str]:
    elements = metadata.get("elements")
    if not isinstance(elements, list) or not elements:
        raise ToolError(f"APK metadata at {metadata_path} does not contain any output elements.")
    if len(elements) != 1:
        raise ToolError(
            f"APK metadata at {metadata_path} contains {len(elements)} outputs. "
            "Automatic export currently expects a single APK output."
        )

    element = elements[0]
    if not isinstance(element, dict):
        raise ToolError(f"APK metadata at {metadata_path} has an invalid output element.")

    output_file = element.get("outputFile")
    if not isinstance(output_file, str) or not output_file:
        raise ToolError(f"APK metadata at {metadata_path} is missing `outputFile`.")

    source_apk = metadata_path.parent / output_file
    if not source_apk.exists():
        raise ToolError(f"Expected built APK at {source_apk}, but the file does not exist.")

    version_name = str(element.get("versionName") or "").strip()
    return source_apk, version_name


def _slugify(value: str) -> str:
    compact = re.sub(r"[^A-Za-z0-9._-]+", "-", value.strip().lower())
    return compact.strip("-._")


def _default_filename(variant: str, version_name: str, suffix: str) -> str:
    project_name = _slugify(ROOT_DIR.name) or "wavebits"
    parts = [project_name, "android", variant]
    if version_name:
        parts.append(f"v{_slugify(version_name) or version_name}")
    return "-".join(parts) + suffix


def _maybe_assemble(variant: str) -> None:
    cmd_android(argparse.Namespace(action=f"assemble-{variant}", clean=False))


def cmd_export_apk(args: argparse.Namespace) -> None:
    metadata_path = _apk_metadata_path(args.variant)
    if args.assemble_if_missing and not metadata_path.exists():
        _maybe_assemble(args.variant)

    metadata = _load_metadata(metadata_path, args.variant)
    source_apk, version_name = _resolve_single_apk(metadata, metadata_path)

    output_dir = _resolve_output_directory(args.out_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    filename = args.filename or _default_filename(args.variant, version_name, source_apk.suffix)
    destination = output_dir / filename
    shutil.copy2(source_apk, destination)

    print(f"Source APK: {source_apk}")
    print(f"Exported APK: {destination}")
