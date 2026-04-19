from __future__ import annotations

from pathlib import Path

from ...constants import ROOT_DIR


_ANDROID_NATIVE_PRIVATE_INCLUDE_ROOT = (
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "private_include"
)
_MODULES_LEAF_SMOKE_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "Test" / "modules" / "leaf_module_smoke.cpp",
    ROOT_DIR / "Test" / "modules" / "leaf_module_smoke_support.cpp",
    ROOT_DIR / "Test" / "modules" / "leaf_module_smoke_audio_io.cpp",
    ROOT_DIR / "Test" / "modules" / "leaf_module_smoke_flash.cpp",
    ROOT_DIR / "Test" / "modules" / "leaf_module_smoke_transport.cpp",
)
_LEGACY_INCLUDE_PREFIX = '#include "bag/legacy/'
_ALLOWED_LEGACY_INCLUDE_ROOTS: tuple[Path, ...] = ()
_ALLOWED_LEGACY_INCLUDE_OWNERS: set[Path] = set()


def iter_code_files() -> list[Path]:
    code_files: set[Path] = set()
    for root in (ROOT_DIR / "libs", ROOT_DIR / "Test", ROOT_DIR / "apps"):
        if not root.is_dir():
            continue
        for pattern in ("*.cpp", "*.h", "*.hpp", "*.cppm", "*.cc", "*.cxx", "*.inc"):
            code_files.update(path for path in root.rglob(pattern) if path.is_file())
    return sorted(code_files)


def iter_android_private_headers() -> list[Path]:
    if not _ANDROID_NATIVE_PRIVATE_INCLUDE_ROOT.is_dir():
        return []
    return sorted(path for path in _ANDROID_NATIVE_PRIVATE_INCLUDE_ROOT.rglob("*.h") if path.is_file())


def is_allowed_legacy_include_owner(path: Path) -> bool:
    if path in _ALLOWED_LEGACY_INCLUDE_OWNERS:
        return True
    return any(path.is_relative_to(root) for root in _ALLOWED_LEGACY_INCLUDE_ROOTS)


def read_modules_leaf_smoke_content() -> str:
    return "\n".join(path.read_text(encoding="utf-8") for path in _MODULES_LEAF_SMOKE_PATHS)


def legacy_include_prefix() -> str:
    return _LEGACY_INCLUDE_PREFIX


def android_native_private_include_root() -> Path:
    return _ANDROID_NATIVE_PRIVATE_INCLUDE_ROOT
