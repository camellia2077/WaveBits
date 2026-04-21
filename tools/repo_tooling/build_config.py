from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache
from typing import Any

import tomllib

from .constants import BUILD_CONFIG_PATH
from .errors import ToolError


@dataclass(frozen=True)
class CiBuildConfig:
    python: str
    java: str


@dataclass(frozen=True)
class AndroidSdkBuildConfig:
    compile_sdk: int
    target_sdk: int
    build_tools: str
    platform: str
    ndk: str
    cmake: str
    components: tuple[str, ...]


@dataclass(frozen=True)
class BuildConfig:
    ci: CiBuildConfig
    android_sdk: AndroidSdkBuildConfig


def _require_table(data: dict[str, Any], key: str) -> dict[str, Any]:
    value = data.get(key)
    if not isinstance(value, dict):
        raise ToolError(f"{BUILD_CONFIG_PATH} is missing required table [{key}].")
    return value


def _require_str(data: dict[str, Any], key: str) -> str:
    value = data.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ToolError(f"{BUILD_CONFIG_PATH} is missing required string '{key}'.")
    return value


def _require_int(data: dict[str, Any], key: str) -> int:
    value = data.get(key)
    if not isinstance(value, int):
        raise ToolError(f"{BUILD_CONFIG_PATH} is missing required integer '{key}'.")
    return value


def _require_str_list(data: dict[str, Any], key: str) -> tuple[str, ...]:
    value = data.get(key)
    if not isinstance(value, list) or not value:
        raise ToolError(f"{BUILD_CONFIG_PATH} is missing required list '{key}'.")
    if not all(isinstance(item, str) and item.strip() for item in value):
        raise ToolError(f"{BUILD_CONFIG_PATH} contains a non-string item in '{key}'.")
    return tuple(value)


@lru_cache(maxsize=1)
def load_build_config() -> BuildConfig:
    if not BUILD_CONFIG_PATH.exists():
        raise ToolError(f"Missing build config: {BUILD_CONFIG_PATH}")

    with BUILD_CONFIG_PATH.open("rb") as handle:
        raw = tomllib.load(handle)

    ci = _require_table(raw, "ci")
    android = _require_table(raw, "android")
    android_sdk = _require_table(android, "sdk")

    return BuildConfig(
        ci=CiBuildConfig(
            python=_require_str(ci, "python"),
            java=_require_str(ci, "java"),
        ),
        android_sdk=AndroidSdkBuildConfig(
            compile_sdk=_require_int(android_sdk, "compile_sdk"),
            target_sdk=_require_int(android_sdk, "target_sdk"),
            build_tools=_require_str(android_sdk, "build_tools"),
            platform=_require_str(android_sdk, "platform"),
            ndk=_require_str(android_sdk, "ndk"),
            cmake=_require_str(android_sdk, "cmake"),
            components=_require_str_list(android_sdk, "components"),
        ),
    )
